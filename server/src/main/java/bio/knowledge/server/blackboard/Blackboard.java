/*-------------------------------------------------------------------------------
 * The MIT License (MIT)
 *
 * Copyright (c) 2015-18 STAR Informatics / Delphinai Corporation (Canada) - Dr. Richard Bruskiewich
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
package bio.knowledge.server.blackboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import bio.knowledge.server.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import bio.knowledge.Util;
import bio.knowledge.aggregator.ConceptCategoryService;
import bio.knowledge.aggregator.Curie;
import bio.knowledge.aggregator.harvest.QueryUtil;
import bio.knowledge.database.repository.ConceptRepository;
import bio.knowledge.database.repository.StatementRepository;
import bio.knowledge.database.repository.beacon.BeaconRepository;
import bio.knowledge.model.aggregator.neo4j.Neo4jConceptClique;
import bio.knowledge.model.aggregator.neo4j.Neo4jKnowledgeBeacon;
import bio.knowledge.model.neo4j.Neo4jConcept;
import bio.knowledge.model.neo4j.Neo4jConceptDetail;
import bio.knowledge.model.neo4j.Neo4jEvidence;
import bio.knowledge.model.neo4j.Neo4jStatement;
import bio.knowledge.ontology.BiolinkTerm;
import bio.knowledge.server.controller.ExactMatchesHandler;

/**
 * This class manages the KBA Blackboard which is, in essence, 
 * accessing a graph database of cached retrieved concepts and relationships.
 * 
 * If requested concepts and relationship statements are not yet detected
 * in the graph database, then a query is triggered to harvest such data 
 * from onto the Knowledge Beacon network.
 * 
 * @author richard
 *
 */
@Service
public class Blackboard implements Curie, QueryUtil, Util {
	
	public static final String NO_CLIQUE_FOUND_WARNING = "WARN: Could not build clique - are you sure inputId exists in at least one of the beacons?";

	@Autowired private QueryRegistry queryRegistry;
	
	@Autowired private ExactMatchesHandler exactMatchesHandler;
	
	@Autowired private BeaconHarvestService beaconHarvestService;
	
	@Autowired private ConceptRepository conceptRepository;
	@Autowired private ConceptCategoryService conceptTypeService;
	
	@Autowired private StatementRepository statementRepository;
	@Autowired private BeaconRepository beaconRepository;

	/**
	 * 
	 * @param queryId
	 * @return
	 */
	public boolean isActiveQuery(String queryId) {
		return queryRegistry.isActiveQuery(queryId);
	}
	
/******************************** CONCEPT Data Access *************************************/
	
	/**
	 * 
	 * @param queryId
	 * @param keywords
	 * @param conceptTypes
	 * @param beacons
	 * @throws BlackboardException
	 */
	public ServerConceptsQuery initiateConceptsQuery(
			List<String> keywords, 
			List<String> conceptTypes, 
			List<Integer> beacons
	) throws BlackboardException {
		
		try {
			ConceptsQuery query = (ConceptsQuery) queryRegistry.createQuery(QueryRegistry.QueryType.CONCEPTS);
			ServerConceptsQuery scq = query.getQuery(keywords, conceptTypes, beacons);
			return scq;
		} catch (Exception e) {
			throw new BlackboardException(e);
		}
	}


	/**
	 * 
	 * @param queryId
	 * @param beacons
	 * @return
	 */
	public ServerConceptsQueryStatus 
					getConceptsQueryStatus(
							String queryId, 
							List<Integer> beacons
	) throws BlackboardException {
		try {
			ConceptsQuery query = (ConceptsQuery) queryRegistry.lookupQuery(queryId);
			ServerConceptsQueryStatus queryStatus = query.getQueryStatus(beacons);
			return queryStatus;

		} catch (Exception e) {
			throw new BlackboardException(e);
		}
	}

	/**
	 * 
	 * @param queryId
	 * @param pageNumber
	 * @param pageSize
	 * @param beacons
	 * @return
	 * @throws BlackboardException
	 */
	public ServerConceptsQueryResult 
					retrieveConceptsQueryResults(
							String queryId, 
							Integer pageNumber, 
							Integer pageSize,
							List<Integer> beacons
	) throws BlackboardException {
		try {
			ConceptsQuery query = (ConceptsQuery) queryRegistry.lookupQuery(queryId);
			ServerConceptsQueryResult results = query.getQueryResults(pageNumber,pageSize,beacons);
			return results;
		} catch(Exception e) {
			throw new BlackboardException(e);
		}
	}
	
	
	public ServerCliquesQuery initiateCliquesQuery(List<String> identifiers, List<Integer> beacons) 
		throws BlackboardException {
		try {
			CliquesQuery query = (CliquesQuery) queryRegistry.createQuery( QueryRegistry.QueryType.CLIQUES);
			ServerCliquesQuery scq = query.getQuery(identifiers, beacons);
			return scq;
		} catch(Exception e) {
			throw new BlackboardException(e);
		}
	}
	
	public ServerCliquesQueryStatus getCliquesQueryStatus(String queryId)
		throws BlackboardException {
		try {
			CliquesQuery query = (CliquesQuery) queryRegistry.lookupQuery(queryId);
			ServerCliquesQueryStatus queryStatus = query.getQueryStatus();
			return queryStatus;
		} catch(Exception e) {
			throw new BlackboardException(e);
		}
	}
	
	public ServerCliquesQueryResult retrieveCliquesQueryResults (String queryId) 
		throws BlackboardException {
		try {
			CliquesQuery query = (CliquesQuery) queryRegistry.lookupQuery(queryId);
			ServerCliquesQueryResult results = query.getQueryResults();
			return results;
		} catch(Exception e) {
			throw new BlackboardException(e);
		}
	}
		


	/**
	 * 
	 * @param identifier
	 * @param queryId
	 * @return
	 */
	public ServerCliqueIdentifier getClique( String identifier ) throws BlackboardException {
		
		ServerCliqueIdentifier cliqueId = null;
		
		try {
			
			Neo4jConceptClique clique = 
					exactMatchesHandler.getConceptCliqueFromDb(new String[] { identifier });
			
			if(clique!=null) {
				cliqueId = new ServerCliqueIdentifier();
				cliqueId.setCliqueId(clique.getId());
				cliqueId.setInputId(identifier);
			}
		
		} catch (Exception e) {
			throw new BlackboardException(e);
		}
		
		return cliqueId;
	}
	
	/**
	 * Builds clique and returns its cliqueId or Optional.empty() if doesn't exist
	 * @param identifier we want to build a clique from
	 * @return a cliqueId of clique built from finding exactmatches on beacons or warning message in the cliqueId if clique
	 * could not be built 
	 */
	public Optional<ServerCliqueIdentifier> buildCliqueFromBeaconsOrCreateErrorResponse(String identifier) {
		Optional<Neo4jConceptClique> optional = 
				exactMatchesHandler.compileConceptCliqueFromBeacons(
						identifier,identifier,BiolinkTerm.NAMED_THING.getLabel()
				);
		
		ServerCliqueIdentifier cliqueId = new ServerCliqueIdentifier();
		cliqueId.setInputId(identifier);
		
		if (optional.isPresent()) {
			Neo4jConceptClique clique = optional.get();
			cliqueId.setCliqueId(clique.getId());

			return Optional.of(cliqueId);
		} else {
			return Optional.empty();
		}
		
	}
	
	/**
	 * 
	 * @param clique
	 * @param beacons
	 * @param queryId
	 * @return
	 */
	public  ServerConceptWithDetails getConceptDetails(
			String cliqueId, 
			List<Integer> beacons
	) throws BlackboardException {
	
		ServerConceptWithDetails concept = null;
		
		try {
			concept = getConceptsWithDetailsFromDatabase(
							cliqueId,
							beacons
			);
			
			if (concept == null) {
				Neo4jConceptClique clique = exactMatchesHandler.getClique(cliqueId);

				if(clique==null) 
					throw new RuntimeException("harvestConceptsBeaconDetails(): clique with ID '"+cliqueId+"' could not be found?") ;
				
				concept = new ServerConceptWithDetails();
				concept.setClique(cliqueId);
				concept.setCategories(clique.getConceptCategories());
				concept.setAliases(clique.getConceptIds());
				concept.setName(clique.getSuperName());
			}
			
	    	if (concept.getEntries().isEmpty()) {
	    		
	    		concept = beaconHarvestService.harvestConceptsBeaconDetails(concept, beacons);

	    		addConceptsWithDetailsToDatabase(concept);

	    	} 		
		} catch (Exception e) {
			throw new BlackboardException(e);
		}
		
		return concept;
	}

	private void addConceptsWithDetailsToDatabase(ServerConceptWithDetails concept) {
			
		Neo4jConcept neo4jConcept = conceptRepository.getByClique(concept.getClique());
		
		if (neo4jConcept == null) {
			neo4jConcept = new Neo4jConcept();
		}
		
		Neo4jConceptClique clique = exactMatchesHandler.getClique(concept.getClique());
		neo4jConcept.setClique(clique);
		
		neo4jConcept.setName(concept.getName());
		
		for (ServerConceptWithDetailsBeaconEntry e : concept.getEntries()) {

			
			
			for (ServerConceptDetail d : e.getDetails()) {
				
				Neo4jConceptDetail detail = new Neo4jConceptDetail();
				detail.setAccessionId(e.getId());
				detail.setDefinition(e.getDefinition());
				detail.setSynonyms(e.getSynonyms());
				
				detail.setKey(d.getTag());
				detail.setValue(d.getValue());
				
				Neo4jKnowledgeBeacon neo4jBeacon = beaconRepository.getBeacon(e.getBeacon());
				
				if (neo4jBeacon == null) {
					neo4jBeacon = new Neo4jKnowledgeBeacon();
					neo4jBeacon.setBeaconId(e.getBeacon());
				}
				
				detail.setSourceBeacon(neo4jBeacon);
				
				neo4jConcept.addDetail(detail);
			}
		}
		
		/*  TODO: Fix concept type setting
		String type = concept.getType();
		if(type!=null) {
			String[] typenames = type.split("\\s");
			List<String> types = new ArrayList<String>();
			// TODO lookup and load types by typenames here?
			entry.setTypes(types);
		}
		*/
		
		neo4jConcept = conceptRepository.save(neo4jConcept);
	}

	private ServerConceptWithDetails getConceptsWithDetailsFromDatabase(String cliqueId, List<Integer> beacons) {
		
		Neo4jConceptClique clique = exactMatchesHandler.getClique(cliqueId);
		if(clique==null) return null; // non-existent concept clique being requested
		
		List<String> aliases = clique.getConceptIds();

		Neo4jConcept neo4jConcept = 
				conceptRepository.getByClique(cliqueId);
		
		if(neo4jConcept == null) return  null; // concept not found?
		
		ServerConceptWithDetails concept = new ServerConceptWithDetails();
		
		concept.setClique(neo4jConcept.getClique().getId());
		concept.setAliases(aliases);
		concept.setName(neo4jConcept.getName());
		
		Set<String> categories = neo4jConcept.getTypes();
		
		if(nullOrEmpty(categories)) {
			for (String category : clique.getConceptCategories())
				categories.add(conceptTypeService.lookUpByIdentifier(category).getName());
		}
		
		concept.setCategories(new ArrayList<>(categories));
		
		List<ServerConceptWithDetailsBeaconEntry> entries = 
				new ArrayList<ServerConceptWithDetailsBeaconEntry>();
		
		Map<Neo4jKnowledgeBeacon, List<ServerConceptDetail>> detailsMap = 
				new HashMap<Neo4jKnowledgeBeacon, List<ServerConceptDetail>>();
		
		List<Map<String, Object>> details = conceptRepository.getDetailsByClique(cliqueId);
		for (Map<String, Object> detailObject : details) {

			Neo4jConceptDetail detail = (Neo4jConceptDetail) detailObject.get("detail");
			Neo4jKnowledgeBeacon beacon = (Neo4jKnowledgeBeacon) detailObject.get("beacon");
			
			ServerConceptDetail serverConceptDetail = new ServerConceptDetail();
			serverConceptDetail.setTag(detail.getKey());
			serverConceptDetail.setValue(detail.getValue());
			
			if (detailsMap.containsKey(beacon)) {
				detailsMap.get(beacon).add(serverConceptDetail);
			} else {
				List<ServerConceptDetail> detailsByBeacon = new ArrayList<ServerConceptDetail>();
				detailsByBeacon.add(serverConceptDetail);
				detailsMap.put(beacon, detailsByBeacon);
			}
		}
		
		for (Neo4jKnowledgeBeacon knowledgeBeacon : detailsMap.keySet()) {
			
			ServerConceptWithDetailsBeaconEntry entry = 
					new ServerConceptWithDetailsBeaconEntry();
			
			Neo4jConceptDetail detail = (Neo4jConceptDetail) details.get(0).get("detail"); 
			
			entry.setId(detail.getAccessionId());
			entry.setDefinition(detail.getDefinition());
			entry.setSynonyms(detail.getSynonyms());
			entry.setBeacon(knowledgeBeacon.getBeaconId());
			entry.setDetails(detailsMap.get(knowledgeBeacon));
			entries.add(entry);
		}
		
		concept.setEntries(entries);
		
		return concept;
	}

/******************************** STATEMENTS Data Access *************************************/

	/**
	 * 
	 * @param queryId
	 * @param source
	 * @param relations
	 * @param target
	 * @param keywords
	 * @param conceptTypes
	 * @param beacons
	 */
	public ServerStatementsQuery initiateStatementsQuery(
			String source, 
			List<String> relations, 
			String target, 
			List<String> keywords,
			List<String> conceptTypes, 
			List<Integer> beacons
	) throws BlackboardException {
		
		try {
			// Create new query instance
			StatementsQuery query = (StatementsQuery)
					queryRegistry.createQuery( QueryRegistry.QueryType.STATEMENTS );
	
			ServerStatementsQuery ssq = 
					query.getQuery(
							source, relations, target,
							keywords, conceptTypes,
							beacons
					);
			
			return ssq;
		
		} catch(Exception e) {
			throw new BlackboardException(e);
		}
	}
	

	/**
	 * 
	 * @param queryId
	 * @param beacons
	 * @return
	 */
	public ServerStatementsQueryStatus 
					getStatementsQueryStatus(
							String queryId, 
							List<Integer> beacons
	) throws BlackboardException {

		try {
			StatementsQuery query = 
					(StatementsQuery) queryRegistry.lookupQuery(queryId);

			ServerStatementsQueryStatus queryStatus = query.getQueryStatus(beacons);

			return queryStatus;

		} catch(Exception e) {
			throw new BlackboardException(e);
		}

	}


	/**
	 * 
	 * @param queryId
	 * @param pageNumber
	 * @param pageSize
	 * @param beacons
	 * @return
	 * @throws BlackboardException
	 */
	public ServerStatementsQueryResult 
					retrieveStatementsQueryResults(
							String queryId, 
							Integer pageNumber,
							Integer pageSize, 
							List<Integer> beacons
							
	) throws BlackboardException {
		
		try {
			StatementsQuery query = 
					(StatementsQuery) queryRegistry.lookupQuery(queryId);
			
			// Create result wrapper
			ServerStatementsQueryResult results = 
					query.getQueryResults(pageNumber,pageSize,beacons);
			
			return results;
			
		} catch(Exception e) {
			throw new BlackboardException(e);
		}
	}

/******************************** Statement Details Data Access *************************************/

	public ServerStatementDetails getStatementDetails(String statementId, List<String> keywords,
			Integer pageSize, Integer pageNumber) throws BlackboardException {
		
		try {
			Neo4jStatement statement = statementRepository.findStatementById(statementId);
			
			if (statement != null) {
				if (statement.getIsDefinedBy() == null) {
					statement = beaconHarvestService.harvestAndSaveEvidence(statement, statementId, keywords, pageSize);
				}
				
				List<Neo4jEvidence> evidence = statementRepository.getEvidenceByStatementId(statementId);
				return statementToDetails(statement, evidence, keywords, pageSize, pageNumber);
				
			} else {
				throw new BlackboardException("GetStatementDetails: could not find the statement in repository. Are you sure the statementId is correct?");
			} 
		} catch (Exception e) {
			throw new BlackboardException(e);
		}
	}

	private ServerStatementDetails statementToDetails(Neo4jStatement statement, List<Neo4jEvidence> evidence,
			List<String> keywords, Integer pageSize, Integer pageNumber) {
		ServerStatementDetails result = new ServerStatementDetails();
		result.setAnnotation(Translator.translateAnnotation(statement.getAnnotationsAsList()));
		result.setId(statement.getId());
		result.setIsDefinedBy(statement.getIsDefinedBy());
		result.setProvidedBy(statement.getProvidedBy());
		result.setKeywords(keywords);
		result.setPageSize(pageSize);
		result.setPageNumber(pageNumber);
		result.setQualifiers(result.getQualifiers());
		result.setEvidence(Translator.translateEvidence(evidence, pageSize, pageNumber));
		return result;
	}

	/******************************** NCATS Reasoner API Query Handler ****************************/

	/**
	 *
	 * @return Reasoner API message
	 */
    public Message query(Map<String, Object> body) throws BlackboardException {
    	throw new BlackboardException("UNIMPLEMENTED: Reasoner API Query");
    }
}
