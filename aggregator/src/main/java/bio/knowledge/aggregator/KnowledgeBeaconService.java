package bio.knowledge.aggregator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.springframework.stereotype.Service;

import com.google.gson.JsonSyntaxException;

import bio.knowledge.client.ApiClient;
import bio.knowledge.client.ApiException;
import bio.knowledge.client.api.ConceptsApi;
import bio.knowledge.client.api.EvidenceApi;
import bio.knowledge.client.api.ExactmatchesApi;
import bio.knowledge.client.api.StatementsApi;
import bio.knowledge.client.api.SummaryApi;
import bio.knowledge.client.model.InlineResponse200;
import bio.knowledge.client.model.InlineResponse2001;
import bio.knowledge.client.model.InlineResponse2002;
import bio.knowledge.client.model.InlineResponse2003;
import bio.knowledge.client.model.InlineResponse2004;

/**
 * 
 * @author Lance Hannestad
 * 
 *         It may seem wasteful to instantiate a new {@code ConceptApi} (or
 *         other API classes) within each {@code ListSupplier<T>}, but in fact
 *         it is necessary because we're asynchrounously setting their ApiClient
 *         objects (which encapsulate the URI to be queried) in
 *         {@code GenericDataService}.
 *         <br><br>
 *         The methods in this class are ugly and confusing.. But it's somewhat
 *         unavoidable. Take a look at how they are used in
 *         {@code GenericKnowledgeService}. A SupplierBuilder builds a
 *         ListSupplier which extends a Supplier, which is used to generate
 *         CompletableFutures.
 *
 */
@Service
public class KnowledgeBeaconService extends GenericKnowledgeService {
	
	/**
	 * Periods sometimes drop out of queries if they are not URL encoded. This
	 * is <b>not</b> a complete URL encoding. I have only encoded those few
	 * characters that might be problematic. We may have to revisit this in
	 * the future, and implement a proper encoder.
	 */
	private String urlEncode(String string) {
//		if (string != null) {
//			return string.replace(".", "%2E").replace(" ", "%20").replace(":", "%3A");
//		} else {
//			return null;
//		}
		return string;
	}
	
	private void printError(ApiClient apiClient, Exception e) {
		System.err.println("Error Querying:   " + apiClient.getBasePath());
		System.err.println("Error message:    " + e.getMessage());
		if (e instanceof JsonSyntaxException) {
			System.err.println("PROBLEM WITH DESERIALIZING SERVER RESPONSE");
		}
	}

	/**
	 * Gets a list of concepts satisfying a query with the given parameters.
	 * @param keywords
	 * @param semgroups
	 * @param pageNumber
	 * @param pageSize
	 * @return a {@code CompletableFuture} of all the concepts from all the
	 *         knowledge sources in the {@code KnowledgeBeaconRegistry} that
	 *         satisfy a query with the given parameters.
	 */
	public CompletableFuture<List<InlineResponse2002>> getConcepts(String keywords,
			String semgroups,
			int pageNumber,
			int pageSize
	) {
		final String sg = semgroups;
		
		SupplierBuilder<InlineResponse2002> builder = new SupplierBuilder<InlineResponse2002>() {

			@Override
			public ListSupplier<InlineResponse2002> build(ApiClient apiClient) {
				return new ListSupplier<InlineResponse2002>() {

					@Override
					public List<InlineResponse2002> getList() {
						ConceptsApi conceptsApi = new ConceptsApi(apiClient);
						
						try {
							List<InlineResponse2002> responses = conceptsApi.getConcepts(
									urlEncode(keywords),
									urlEncode(sg),
									pageNumber,
									pageSize
							);
							
							return responses;
							
						} catch (Exception e) {
							printError(apiClient, e);
							return new ArrayList<InlineResponse2002>();
						}
					}
					
				};
			}
			
		};
		
		return query(builder);
	}
	
	public CompletableFuture<List<InlineResponse2001>> getConceptDetails(String conceptId) {
		SupplierBuilder<InlineResponse2001> builder = new SupplierBuilder<InlineResponse2001>() {

			@Override
			public ListSupplier<InlineResponse2001> build(ApiClient apiClient) {
				return new ListSupplier<InlineResponse2001>() {

					@Override
					public List<InlineResponse2001> getList() {
						ConceptsApi conceptsApi = new ConceptsApi(apiClient);
						
						try {
							List<InlineResponse2001> responses = conceptsApi.getConceptDetails(
									urlEncode(conceptId)
							);
							
							return responses;
							
						} catch (Exception e) {
							printError(apiClient, e);
							return new ArrayList<InlineResponse2001>();
						}
					}
					
				};
			}
			
		};
		return query(builder);
	}
	
	public CompletableFuture<List<InlineResponse2003>> getStatements(
			List<String> c,
			String keywords,
			String semgroups,
			int pageNumber,
			int pageSize
	) {
		SupplierBuilder<InlineResponse2003> builder = new SupplierBuilder<InlineResponse2003>() {

			@Override
			public ListSupplier<InlineResponse2003> build(ApiClient apiClient) {
				return new ListSupplier<InlineResponse2003>() {

					@Override
					public List<InlineResponse2003> getList() {
						StatementsApi statementsApi = new StatementsApi(apiClient);
						ExactmatchesApi exactmatchesApi = new ExactmatchesApi(apiClient);
						
						try {
							Set<String> curieSet = new HashSet<String>();
							curieSet.addAll(c);
							
							for (String curie : c) {
								List<String> exactMatches = exactmatchesApi.getExactMatchesToConcept(curie);
								if (exactMatches != null) {
									curieSet.addAll(exactMatches);
								}
							}
							
							List<String> curieList = new ArrayList<String>();
							curieList.addAll(curieSet);
							
							List<InlineResponse2003> responses = statementsApi.getStatements(
									curieList,
									pageNumber,
									pageSize,
									keywords,
									semgroups
							);
							
							return responses;
							
						} catch (Exception e) {
							printError(apiClient, e);
							return new ArrayList<InlineResponse2003>();
						}
					}
					
				};
			}
			
		};
		return query(builder);
	}
	
	/**
	 * In our project, annotations really play this role of evidence.
	 */
	public CompletableFuture<List<InlineResponse2004>> getEvidences(
			String statementId,
			String keywords,
			int pageNumber,
			int pageSize
	) {
		SupplierBuilder<InlineResponse2004> builder = new SupplierBuilder<InlineResponse2004>() {

			@Override
			public ListSupplier<InlineResponse2004> build(ApiClient apiClient) {
				return new ListSupplier<InlineResponse2004>() {

					@Override
					public List<InlineResponse2004> getList() {
						EvidenceApi evidenceApi = new EvidenceApi(apiClient);
						
						try {
							List<InlineResponse2004> responses = evidenceApi.getEvidence(
									urlEncode(statementId),
									urlEncode(keywords),
									pageNumber,
									pageSize
							);
							
							return responses;
							
						} catch (Exception e) {
							printError(apiClient, e);
							return new ArrayList<InlineResponse2004>();
						}
					}
					
				};
			}
			
		};
		return query(builder);
	}

	public CompletableFuture<List<InlineResponse200>> linkedTypes() {
		SupplierBuilder<InlineResponse200> builder = new SupplierBuilder<InlineResponse200>() {

			@Override
			public ListSupplier<InlineResponse200> build(ApiClient apiClient) {
				return new ListSupplier<InlineResponse200>() {

					@Override
					public List<InlineResponse200> getList() {
						SummaryApi summaryApi = new SummaryApi(apiClient);
						
						try {
							return summaryApi.linkedTypes();
						} catch (ApiException e) {
							e.printStackTrace();
							return new ArrayList<InlineResponse200>();
						}
					}
					
				};
			}
			
		};
		
		return query(builder);
	}
	
}