/*-------------------------------------------------------------------------------
 * The MIT License (MIT)
 *
 * Copyright (c) 2015-17 STAR Informatics / Delphinai Corporation (Canada) - Dr. Richard Bruskiewich
 * Copyright (c) 2017    NIH National Center for Advancing Translational Sciences (NCATS)
 * Copyright (c) 2015-16 Scripps Institute (USA) - Dr. Benjamin Good
 *                       
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *-------------------------------------------------------------------------------
 */
package bio.knowledge.aggregator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.gson.JsonSyntaxException;
import com.squareup.okhttp.OkHttpClient;

import bio.knowledge.client.ApiException;
import bio.knowledge.client.api.ConceptsApi;
import bio.knowledge.client.api.EvidenceApi;
import bio.knowledge.client.api.ExactmatchesApi;
import bio.knowledge.client.api.PredicatesApi;
import bio.knowledge.client.api.StatementsApi;
import bio.knowledge.client.api.SummaryApi;
import bio.knowledge.client.impl.ApiClient;
import bio.knowledge.client.model.BeaconAnnotation;
import bio.knowledge.client.model.BeaconConcept;
import bio.knowledge.client.model.BeaconConceptWithDetails;
import bio.knowledge.client.model.BeaconPredicate;
import bio.knowledge.client.model.BeaconStatement;
import bio.knowledge.client.model.BeaconSummary;
import bio.knowledge.model.aggregator.ConceptClique;

/**
 * 
 * @author Lance Hannestad
 * 
 *         It may seem wasteful to instantiate a new {@code ConceptApi} (or
 *         other API classes) within each {@code ListSupplier<T>}, but in fact
 *         it is necessary because we're asynchronously setting their ApiClient
 *         objects (which encapsulate the URI to be queried) in
 *         {@code GenericDataService}.
 *         <br><br>
 *         The methods in this class are ugly and confusing.. But it's somewhat
 *         unavoidable. Take a look at how they are used in
 *         {@code GenericKnowledgeService}. A SupplierBuilder builds a
 *         ListSupplier which extends a Supplier, which is used to generate
 *         CompletableFutures.
 *         
 *  @author Meera Godden
 *  
 *  		Most queries return a map associating beacons to their results.
 *  		Queries that use exactmatches try to handle internal errors in beacons
 *  		by trying to give the beacon fewer exactmatches at a time.
 *
 */
@Service
public class KnowledgeBeaconService {

	private static Logger _logger = LoggerFactory.getLogger(KnowledgeBeaconService.class);

	// This works because {@code GenericKnowledgeService} is extended by {@code
	// KnowledgeBeaconService}, which is a Spring service.
	@Autowired KnowledgeBeaconRegistry registry;
	
	private Map<String, List<LogEntry>> errorLog = new HashMap<>();
	
	private boolean nullOrEmpty(String str) {
		return str == null || str.isEmpty();
	}
	
	private void clearError(String sessionId) {
		if (nullOrEmpty(sessionId)) return;
		errorLog.put(sessionId, new ArrayList<>());
	}
	
	public void logError(String sessionId, String beacon, String query, String message) {
		
		if (nullOrEmpty(sessionId)||nullOrEmpty(message)) return;
		
		LogEntry entry = new LogEntry(beacon, query, message);
		errorLog.putIfAbsent(sessionId, new ArrayList<>());
		errorLog.get(sessionId).add(entry);
	}
	
	public List<LogEntry> getErrors(String sessionId) {
		return errorLog.getOrDefault(sessionId, new ArrayList<>());
	}
	
	/**
	 * Creates a {@code CompletableFuture} that completes when every beacon has completed.
	 * Currently, old {@code errorLog} entries are cleared at the before the querying starts.
	 * 
	 * @param builder
	 * @param sources
	 * @param sessionId
	 * @return
	 */
	private <T> CompletableFuture<List<T>>[] query(SupplierBuilder<T> builder, List<String> sources, String sessionId) {
		clearError(sessionId);
		
		List<CompletableFuture<List<T>>> futures = new ArrayList<CompletableFuture<List<T>>>();
				
		for (KnowledgeBeaconImpl beacon : registry.filterKnowledgeBeaconsById(sources)) {
			if (beacon.isEnabled()) {
				ListSupplier<T> supplier = builder.build(beacon);
				CompletableFuture<List<T>> future = CompletableFuture.supplyAsync(supplier);
				futures.add(future);
			}
		}
		
		@SuppressWarnings("unchecked")
		CompletableFuture<List<T>>[] futureArray = futures.toArray(new CompletableFuture[futures.size()]);
		return futureArray;
	}

	
	protected <T> CompletableFuture<Map<KnowledgeBeaconImpl, List<T>>> queryForMap(SupplierBuilder<T> builder, List<String> beacons, String sessionId) {
		CompletableFuture<Map<KnowledgeBeaconImpl, List<T>>> combinedFuture = combineFuturesIntoMap(registry.filterKnowledgeBeaconsById(beacons), query(builder, beacons, sessionId));
		return combinedFuture;
	}
	
	protected <T> CompletableFuture<List<T>> queryForList(SupplierBuilder<T> builder, String sessionId) {
		CompletableFuture<List<T>> combinedFuture = combineFuturesIntoList(query(builder, new ArrayList<>(), sessionId));
		return combinedFuture;
	}

	/**
	 * Here we take all of the CompletableFuture objects in futures, and combine
	 * them into a single CompletableFuture object. This combined future is of
	 * type Void, so we need thenApply() to get the proper sort of
	 * CompletableFuture. Also this combinedFuture completes exceptionally if
	 * any of the items in {@code futures} completes exceptionally. Because of
	 * this, we also need to tell it what to do if it completes exceptionally,
	 * which is done with exceptionally().
	 * 
	 * @param <T>
	 * @param futures
	 * @return
	 */
	private <T> CompletableFuture<List<T>> combineFuturesIntoList(CompletableFuture<List<T>>[] futures) {
		return CompletableFuture.allOf(futures).thenApply(x -> {

			List<T> combinedResults = new ArrayList<T>();

			for (CompletableFuture<List<T>> f : futures) {
				List<T> results = f.join();
				if (results != null) {
					for (T c : results) {
						_logger.debug(c.toString());
					}
					combinedResults.addAll(results);
				}
			}

			return combinedResults;
		}).exceptionally((error) -> {
			List<T> combinedResults = new ArrayList<T>();

			for (CompletableFuture<List<T>> f : futures) {
				if (!f.isCompletedExceptionally()) {
					List<T> results = f.join();
					if (results != null) {
						combinedResults.addAll(results);
					}
				}
			}
			return combinedResults;
		});
	}
	
	private <T> CompletableFuture<Map<KnowledgeBeaconImpl, List<T>>> combineFuturesIntoMap(List<KnowledgeBeaconImpl> beacons, CompletableFuture<List<T>>[] futures) {
		return CompletableFuture.allOf(futures).thenApply(x -> {
		
			Map<KnowledgeBeaconImpl, List<T>> combinedResults = new HashMap<>();

			for (int i = 0; i < futures.length; i++) {
				
				KnowledgeBeaconImpl beacon = beacons.get(i);
				CompletableFuture<List<T>> f = futures[i];
				
				List<T> results = f.join();
				if (results != null) {
					combinedResults.put(beacon, results);
				}
			}
			
			return combinedResults;
		}).exceptionally((error) -> {
			
			Map<KnowledgeBeaconImpl, List<T>> combinedResults = new HashMap<>();

			for (int i = 0; i < futures.length; i++) {
				
				KnowledgeBeaconImpl beacon = beacons.get(i);
				CompletableFuture<List<T>> f = futures[i];
				
				if (!f.isCompletedExceptionally()) {
					List<T> results = f.join();
					if (results != null) {
						combinedResults.put(beacon, results);
					}
				}
			}
			return combinedResults;
		});
	}

	/**
	 * Wraps {@code wraps Supplier<List<T>>}, used for the sake of generic
	 * queries in {@code GenericKnowledgeService}. The {@code get()} method
	 * <b>must</b> return a List. It may not return {@code null} or throw an exception
	 * (so that nothing is returned). The list that it returns is concatenated
	 * with the lists returned by other suppliers, and so if there is no data to
	 * return simply return an empty list.
	 * 
	 * @author Lance Hannestad
	 *
	 * @param <T>
	 */
	public abstract class ListSupplier<T> implements Supplier<List<T>> {
		
		/**
		 * The {@code get()} method <b>must</b> return a List, otherwise the
		 * CompletableFuture combining in {@code combineFutures()}. will not
		 * work. To ensure that get() will never return null, I have wrapped it
		 * another method that will be overridden by extended classes. Now even
		 * if the author of those extended classes makes a mistake and allows
		 * for {@code null} to be returned or exceptions be thrown, it should
		 * get caught here and not harm the combining of completable futures.
		 */
		@Override
		public List<T> get() {
			try {
				List<T> result = getList();
				if (result != null) {
					return result;
				} else {
					return new ArrayList<T>();
				}
			} catch (Exception e) {
				return new ArrayList<T>();
			}
		}
		
		public abstract List<T> getList();
	}
	
	/**
	 * A class that builds custom ListSupplier objects, for the use of
	 * generating CompletableFutures within {@code GenericKnowledgeService.query()}.
	 * 
	 * @author Lance Hannestad
	 *
	 * @param <T>
	 */
	public abstract class SupplierBuilder<T> {
		public abstract ListSupplier<T> build(KnowledgeBeaconImpl beacon);
	}
	
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
	
	private <T> List<T> list(T item) {
		List<T> list = new ArrayList<>();
		list.add(item);
		return list;
	}
	
	/**
	 * Semi-arbitrary way of checking whether the beacon was unable to handle the particular input it was given.
	 * 
	 * @param e
	 * @return
	 */
	private boolean isInternalError(Exception e) {
		return e.getMessage().toUpperCase().equals("INTERNAL SERVER ERROR");
	}
	
	private void logError(String sessionId, ApiClient apiClient, Exception e) {
		
		String message = e.getMessage();
		
		if (e instanceof JsonSyntaxException) {
		        message += " PROBLEM WITH DESERIALIZING SERVER RESPONSE";
		}

		if(message!=null) _logger.error(message);
		
		logError(sessionId, apiClient.getBeaconId(), apiClient.getQuery(), message);
	}
	
	/*********************************************************************************************************/
	
	public static final long     BEACON_TIMEOUT_DURATION = 1;
	public static final TimeUnit BEACON_TIMEOUT_UNIT = TimeUnit.MINUTES;

	/**
	 * Dynamically compute adjustment to query timeouts proportionately to 
	 * the number of beacons and pageSize
	 * @param beacons
	 * @param pageSize
	 * @return
	 */
	public long weightedTimeout( List<String> beacons, Integer pageSize ) {
		long timescale;
		if(!(beacons==null || beacons.isEmpty())) 
			timescale = beacons.size();
		else
			timescale = registry.countAllBeacons();
		
		timescale *= Math.max(1,pageSize/10) ;
		
		return timescale*BEACON_TIMEOUT_DURATION;
	}
	
	/**
	 * Timeout simply weighted by total number of beacons and pagesize
	 * @return
	 */
	public long weightedTimeout(Integer pageSize) {
		return weightedTimeout(null, pageSize); // 
	}
	
	/**
	 * Timeout simply weighted by number of beacons
	 * @return
	 */
	public long weightedTimeout() {
		return weightedTimeout(null, 0); // 
	}
	
	/*
	 *  ApiClient timeout weightings here are in milliseconds
	 *  These are used below alongside beacon number and pagesizes 
	 *  to set some reasonable timeouts for various queries
	 */
	public static final int DEFAULT_TIMEOUT_WEIGHTING            = 5000;
	public static final int CONCEPTS_QUERY_TIMEOUT_WEIGHTING     = 10000;
	public static final int EXACTMATCHES_QUERY_TIMEOUT_WEIGHTING = 40000; 
	public static final int STATEMENTS_QUERY_TIMEOUT_WEIGHTING   = 60000; 
	public static final int EVIDENCE_QUERY_TIMEOUT_WEIGHTING     = 40000; 
	public static final int TYPES_QUERY_TIMEOUT_WEIGHTING        = 20000; 
	
	public int apiWeightedTimeout( Integer timeOutWeighting, List<String> beacons, Integer pageSize ) {
		int numberOfBeacons = beacons!=null ? beacons.size() : registry.countAllBeacons() ;
		_logger.debug("apiWeightedTimeout parameters: timeout weight: "+ timeOutWeighting + ", # beacons: "+ numberOfBeacons +", data page size: "+ pageSize);
		return timeOutWeighting*(int)weightedTimeout(beacons,pageSize);
	}
	
	public int apiWeightedTimeout(Integer timeOutWeighting, Integer pageSize) {
		return apiWeightedTimeout(timeOutWeighting, null, pageSize );
	}
	
	public int apiWeightedTimeout(Integer timeOutWeighting) {
		return apiWeightedTimeout(timeOutWeighting, 0);
	}
	
	public int apiWeightedTimeout() {
		return apiWeightedTimeout(DEFAULT_TIMEOUT_WEIGHTING);
	}
	
	private ApiClient timedApiClient( 
			String apiName, 
			ApiClient apiClient, 
			Integer timeOutWeighting, 
			List<String> beacons, 
			Integer pageSize 
	) {
		
		// Adjust ApiClient connection timeout
		apiClient.setConnectTimeout(  
				apiWeightedTimeout(
						timeOutWeighting, 
						beacons, 
						pageSize
				)
		);

		_logger.debug(apiName+": ApiClient connection timeout is currently set to  '"+new Integer(apiClient.getConnectTimeout())+"' milliseconds");

		// Adjust ApiClient read timeout
		OkHttpClient httpClient = apiClient.getHttpClient();
		httpClient.setReadTimeout( 
				apiWeightedTimeout(
						timeOutWeighting, 
						beacons, 
						pageSize
				), 
				TimeUnit.MILLISECONDS
		);

		_logger.debug(apiName+": HTTP client socket read timeout is currently set to '"+new Long(httpClient.getReadTimeout())+"' milliseconds");
		
		return apiClient;
	}

	private ApiClient timedApiClient( 
			String apiName, 
			ApiClient apiClient, 
			Integer timeOutWeighting, 
			List<String> beacons
	) {
		return timedApiClient(
				apiName,
				apiClient,
				timeOutWeighting,
				beacons,
				0
		);
	}

	private ApiClient timedApiClient( 
			String apiName, 
			ApiClient apiClient, 
			Integer timeOutWeighting, 
			Integer pageSize 
	) {
		return timedApiClient(
				apiName,
				apiClient,
				timeOutWeighting,
				null,
				pageSize
		);
	}

	private ApiClient timedApiClient( 
			String apiName, 
			ApiClient apiClient, 
			Integer timeOutWeighting
	) {
		return timedApiClient(
				apiName,
				apiClient,
				timeOutWeighting,
				0
		);
	}

	private ApiClient timedApiClient(String apiName, ApiClient apiClient) {
		return timedApiClient(
				apiName,
				apiClient,
				DEFAULT_TIMEOUT_WEIGHTING
		);
	}
	
	/**
	 * Gets a list of concepts satisfying a query with the given parameters.
	 * @param keywords
	 * @param conceptTypes
	 * @param pageNumber
	 * @param pageSize
	 * @param beacons 
	 * @return a {@code CompletableFuture} of all the concepts from all the
	 *         knowledge sources in the {@code KnowledgeBeaconRegistry} that
	 *         satisfy a query with the given parameters.
	 */
	public CompletableFuture<Map<KnowledgeBeaconImpl, List<BeaconConcept>>> getConcepts(String keywords,
			String conceptTypes,
			int pageNumber,
			int pageSize,
			List<String> beacons,
			String sessionId
	) {
		final String sg = conceptTypes;
		
		SupplierBuilder<BeaconConcept> builder = new SupplierBuilder<BeaconConcept>() {

			@Override
			public ListSupplier<BeaconConcept> build(KnowledgeBeaconImpl beacon) {
				return new ListSupplier<BeaconConcept>() {

					@Override
					public List<BeaconConcept> getList() {
												
						String beaconId = beacon.getId();
						
						_logger.debug("kbs.getConcepts(): accessing beacon '"+beaconId+"'");
						
						ConceptsApi conceptsApi = 
								new ConceptsApi(
										timedApiClient(
												beacon.getName()+".getConcepts",
												beacon.getApiClient(),
												CONCEPTS_QUERY_TIMEOUT_WEIGHTING,
												beacons,
												pageSize
										)
									);

						try {
							List<BeaconConcept> responses = 
									conceptsApi.getConcepts(
											urlEncode(keywords),
											urlEncode(sg),
											pageNumber,
											pageSize
									);
							
							_logger.debug("kbs.getConcepts(): '"+responses.size()+"' results found for beacon '"+beaconId+"'");
							return responses;
							
						} catch (Exception e) {
							
							_logger.error("kbs.getConcepts() ERROR: accessing beacon '"+beaconId+"', Exception thrown: "+e.getMessage());
							
							logError(sessionId, beacon.getApiClient(), e);
							return new ArrayList<BeaconConcept>();
						}
					}
					
				};
			}
			
		};
		return queryForMap(builder, beacons, sessionId);
	}


	public CompletableFuture<Map<KnowledgeBeaconImpl, List<BeaconPredicate>>> getAllPredicates() {
		SupplierBuilder<BeaconPredicate> builder = new SupplierBuilder<BeaconPredicate>() {

			@Override
			public ListSupplier<BeaconPredicate> build(KnowledgeBeaconImpl beacon) {
				
				return new ListSupplier<BeaconPredicate>() {

					@Override
					public List<BeaconPredicate> getList() {
						
						PredicatesApi predicateApi =
								new PredicatesApi(
									timedApiClient(
											beacon.getName()+".getAllPredicates",
											beacon.getApiClient()
									)
								);
						
						try {
							return predicateApi.getPredicates();
							
						} catch (ApiException e) {
							logError("getAllPredicates", beacon.getApiClient(), e);
							return new ArrayList<BeaconPredicate>();
						}
					}
				};
			}
			
		};
		return queryForMap(builder, new ArrayList<String>() , "getAllPredicates");
	}
	
	public CompletableFuture<
								Map< 
									KnowledgeBeaconImpl, 
								    List<BeaconConceptWithDetails>
								   >
					       > getConceptDetails(
										    	ConceptClique clique, 
												List<String> beacons,
												String sessionId
											  ) {
		
		SupplierBuilder<BeaconConceptWithDetails> builder = 
				new SupplierBuilder<BeaconConceptWithDetails>() { 

			@Override
			public ListSupplier<BeaconConceptWithDetails> build(KnowledgeBeaconImpl beacon) {
				
				return new ListSupplier<BeaconConceptWithDetails>() {

					@Override
					public List<BeaconConceptWithDetails> getList() {
						
						// Retrieve the beacon specific subclique list of concept identifiers...
						String beaconId = beacon.getId();
						
						_logger.debug("getConceptDetails() accessing beacon '"+beaconId+"'");
						
						List<String> conceptIds;
						
						if(clique.hasConceptIds(beaconId)) {
							/*
							 * TODO: try to figure out why the beacon-specific concept list
							 * - e.g. from Garbanzo - doesn't always retrieve results?
							 */
							conceptIds = clique.getConceptIds(beaconId);
							_logger.debug("Calling getConceptDetails() with concept details '"+String.join(",",conceptIds)+"'");
							
						} else { //.. don't look any further if the list is empty...
							_logger.debug("Returning from getConceptDetails() ... no concept ids available?");
							return new ArrayList<BeaconConceptWithDetails>();
						}
						
						String beaconTag = beacon.getName()+".getConceptDetails";
						ApiClient beaconApi = beacon.getApiClient();
						
						ConceptsApi conceptsApi = 
								new ConceptsApi(
									timedApiClient(
											beaconTag,
											beaconApi,
											CONCEPTS_QUERY_TIMEOUT_WEIGHTING,
											beacons
									)
								);
						
						List<BeaconConceptWithDetails> results = new ArrayList<>();
						
						for ( String id : conceptIds ) {
							try {
								
								id = urlEncode(id);
								
								List<BeaconConceptWithDetails> conceptWithDetails = 
																	conceptsApi.getConceptDetails( id );
								results.addAll(conceptWithDetails);
								
							} catch (Exception e) {
								
								logError(sessionId, beaconApi, e);
								break;
							}
						}
						_logger.debug("getConceptDetails() accessing beacon '"+results.size()+
								  "' results found for beacon '"+beaconId+"'");
						return results;
					}
					
				};
			}
			
		};
		return queryForMap(builder, beacons, sessionId);
	}

	public CompletableFuture<List<String>> getExactMatchesToConcept(String conceptId) {
		SupplierBuilder<String> builder = new SupplierBuilder<String>() {

			@Override
			public ListSupplier<String> build(KnowledgeBeaconImpl beacon) {
				return new ListSupplier<String>() {

					@Override
					public List<String> getList() {
						
						ExactmatchesApi exactmatchesApi =
								new ExactmatchesApi(
										timedApiClient(
												beacon.getName()+".getExactMatchesToConcept",
												beacon.getApiClient(),
												EXACTMATCHES_QUERY_TIMEOUT_WEIGHTING
										)
									);
						try {
							return exactmatchesApi.getExactMatchesToConcept(conceptId);
								
						} catch (Exception e1) {
							logError("Equivalent Concept Clique", beacon.getApiClient(), e1);
							return new ArrayList<>();
						}
					}
					
				};
			}
			
		};
		return queryForList(builder,"Equivalent Concept Clique");
	}
		
	/**
	 * 
	 * @param conceptIds
	 * @param beacons
	 * @return
	 */
	public CompletableFuture<Map<KnowledgeBeaconImpl, List<String>>> 
				getExactMatchesToConceptList( List<String> conceptIds, List<String> beacons ) {
		
		SupplierBuilder<String> builder = new SupplierBuilder<String>() {

			@Override
			public ListSupplier<String> build(KnowledgeBeaconImpl beacon) {
				
				return new ListSupplier<String>() {

					@Override
					public List<String> getList() { 
						
						ExactmatchesApi exactmatchesApi = 
								new ExactmatchesApi(
										timedApiClient(
												beacon.getName()+".getExactMatchesToConceptList",
												beacon.getApiClient(),
												EXACTMATCHES_QUERY_TIMEOUT_WEIGHTING,
												beacons
										)
									);
						try {
							
							List<String> results = exactmatchesApi.getExactMatchesToConceptList(conceptIds);
							return results;
								
						} catch (Exception e1) {
							
							_logger.debug("KBS.getExactMatchesToConceptList() exception from ExactMatchesApi call: "+e1.toString());
							
							logError("Equivalent Concept Clique", beacon.getApiClient(), e1);
							
							List<String> curieList = new ArrayList<>();

							if (isInternalError(e1)) {
								// try asking about CURIEs individually
																
								for (String id : conceptIds) {
									
									try {
										List<String> matches = exactmatchesApi.getExactMatchesToConcept(id);
										curieList.addAll(matches);
									
									} catch (Exception e2) {
										
										logError("Equivalent Concept Clique", beacon.getApiClient(), e2);
										
										if (!isInternalError(e2)) {
											// there is some other problem
											break;
										}
									}
								}
							}
							return curieList;
						}
					}
					
				};
			}
		};
		
		return queryForMap(builder, beacons, "Equivalent Concept Clique");
	}

	public CompletableFuture<
								Map<
									KnowledgeBeaconImpl, 
									List<BeaconStatement>
									>
							> getStatements(
									
									ConceptClique sourceClique,
									String relations, 
									ConceptClique targetClique,
									String keywords,
									String conceptTypes,
									int pageNumber,
									int pageSize,
									List<String> beacons,
									String sessionId
									
								) {
		
		SupplierBuilder<BeaconStatement> builder = 
				new SupplierBuilder<BeaconStatement>() {

			@Override
			public ListSupplier<BeaconStatement> build(KnowledgeBeaconImpl beacon) {
				
				return new ListSupplier<BeaconStatement>() {

					@Override
					public List<BeaconStatement> getList() {
						
						// Retrieve the beacon specific subclique list of concept identifiers...
						String beaconId = beacon.getId();
						
						_logger.debug("getStatements() accessing beacon '"+beaconId+"'");
						
						List<String> sourceConceptIds ;
						
						if(sourceClique.hasConceptIds(beaconId)) {
							/*
							 * Safer for now to take all the known concept identifiers here  
							 * TODO: try to figure out why the beacon-specific concept list - e.g. from Garbanzo - doesn't always retrieve results? Should perhaps only send beacon-specific list in the future?
							 */
							sourceConceptIds = sourceClique.getConceptIds(beaconId);
							
							_logger.debug("Calling getStatements() with source concept identifiers '"+String.join(",",sourceConceptIds)+"'");
							
						} else { //.. don't look any further if the list is empty...
							_logger.debug("Returning from getStatements() ... no concept ids available?");
							return new ArrayList<BeaconStatement>();
						}
						
						List<String> targetConceptIds = null ;
						
						if(targetClique != null && targetClique.hasConceptIds(beaconId)) {
							/*
							 * Safer for now to take all the known concept identifiers here  
							 * TODO: try to figure out why the beacon-specific concept list - e.g. from Garbanzo - doesn't always retrieve results? Should perhaps only send beacon-specific list in the future?
							 */
							targetConceptIds = targetClique.getConceptIds();
							
							_logger.debug("Calling getStatements() with target concept identifiers '"+String.join(",",targetConceptIds)+"'");
							
						} else {
							_logger.debug("Calling getStatements() without any target concept identifiers?");
						}
						
						String beaconTag = beacon.getName()+".getConceptDetails";
						ApiClient beaconApi = beacon.getApiClient();
						
						StatementsApi statementsApi = 
								new StatementsApi(
										timedApiClient(
												beaconTag,
												beaconApi,
												STATEMENTS_QUERY_TIMEOUT_WEIGHTING,
												beacons,
												pageSize
										)
									);
						try {
							List<BeaconStatement> results = 
									statementsApi.getStatements(
														sourceConceptIds, 
														relations,
														targetConceptIds,
														keywords, 
														conceptTypes,
														pageNumber, 
														pageSize
													);
							_logger.debug("getStatements() '"+results.size()+"' results found for beacon '"+beaconId+"'");
							return results;
								
						} catch (Exception e1) {
							
							logError(sessionId, beaconApi, e1);
							
							List<BeaconStatement> statementList = new ArrayList<>();

							if (isInternalError(e1)) {
								
								// try asking about CURIEs individually
																
								for (String sourceConceptId : sourceClique.getConceptIds(beaconId)) {
									
									try {
										List<BeaconStatement> results = 
												statementsApi.getStatements(
														list(sourceConceptId), 
														relations, 
														targetConceptIds, 
														keywords, 
														conceptTypes,
														pageNumber, 
														pageSize
													);
										statementList.addAll(results);
									
									} catch (Exception e2) {
										
										logError(sessionId, beaconApi, e2);
										
										if (!isInternalError(e2)) {
											// there is some other problem
											break;
										}
									}
								}
							}
							_logger.debug("getStatements() accessing beacon '"+statementList.size()+
									  "' results found for beacon '"+beaconId+"'");
							return statementList;
						}
					}
				};
			}
		};
		return queryForMap(builder, beacons, sessionId);
	}
	
	/**
	 * In our project, Evidences really play this role of evidence.
	 * @param beacons 
	 */
	public CompletableFuture<Map<KnowledgeBeaconImpl, List<BeaconAnnotation>>> getEvidence(
			String statementId,
			String keywords,
			int pageNumber,
			int pageSize,
			List<String> beacons,
			String sessionId
	) {
		SupplierBuilder<BeaconAnnotation> builder = new SupplierBuilder<BeaconAnnotation>() {

			@Override
			public ListSupplier<BeaconAnnotation> build(KnowledgeBeaconImpl beacon) {
				return new ListSupplier<BeaconAnnotation>() {

					@Override
					public List<BeaconAnnotation> getList() {
						EvidenceApi evidenceApi = 
								new EvidenceApi(
										timedApiClient(
												beacon.getName()+".getEvidence",
												beacon.getApiClient(),
												EVIDENCE_QUERY_TIMEOUT_WEIGHTING,
												beacons,
												pageSize
										)
									);
						try {
							List<BeaconAnnotation> responses = 
									evidenceApi.getEvidence(
										urlEncode(statementId),
										urlEncode(keywords),
										pageNumber,
										pageSize
								);
							
							return responses;
							
						} catch (Exception e) {
							logError(sessionId, beacon.getApiClient(), e);
							return new ArrayList<BeaconAnnotation>();
						}
					}
					
				};
			}
			
		};
		return queryForMap(builder, beacons, sessionId);
	}

	public CompletableFuture<Map<KnowledgeBeaconImpl, List<BeaconSummary>>> linkedTypes(List<String> beacons, String sessionId) {
		
		SupplierBuilder<BeaconSummary> builder = new SupplierBuilder<BeaconSummary>() {

			@Override
			public ListSupplier<BeaconSummary> build(KnowledgeBeaconImpl beacon) {
				return new ListSupplier<BeaconSummary>() {

					@Override
					public List<BeaconSummary> getList() {
						
						SummaryApi summaryApi = 
								new SummaryApi(
										timedApiClient(
												beacon.getName()+".linkedTypes",
												beacon.getApiClient(),
												TYPES_QUERY_TIMEOUT_WEIGHTING
										)
									);
						try {
							return summaryApi.linkedTypes();
						} catch (ApiException e) {
							logError(sessionId, beacon.getApiClient(), e);
							return new ArrayList<BeaconSummary>();
						}
					}
					
				};
			}
		};
		return queryForMap(builder, beacons, sessionId);
	}
}