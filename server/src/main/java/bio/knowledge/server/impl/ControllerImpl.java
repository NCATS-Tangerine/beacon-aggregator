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
package bio.knowledge.server.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import bio.knowledge.Util;
import bio.knowledge.aggregator.ConceptCliqueService;
import bio.knowledge.aggregator.ConceptTypeService;
import bio.knowledge.aggregator.ConceptTypeUtil;
import bio.knowledge.aggregator.KnowledgeBeaconImpl;
import bio.knowledge.aggregator.KnowledgeBeaconRegistry;
import bio.knowledge.aggregator.KnowledgeBeaconService;
import bio.knowledge.client.model.BeaconAnnotation;
import bio.knowledge.client.model.BeaconConcept;
import bio.knowledge.client.model.BeaconConceptWithDetails;
import bio.knowledge.client.model.BeaconPredicate;
import bio.knowledge.client.model.BeaconStatement;
import bio.knowledge.client.model.BeaconSummary;
import bio.knowledge.model.BioNameSpace;
import bio.knowledge.model.ConceptType;
import bio.knowledge.model.aggregator.ConceptClique;
import bio.knowledge.model.umls.Category;
import bio.knowledge.server.model.ServerAnnotation;
import bio.knowledge.server.model.ServerCliqueIdentifier;
import bio.knowledge.server.model.ServerConcept;
import bio.knowledge.server.model.ServerConceptBeaconEntry;
import bio.knowledge.server.model.ServerConceptDetail;
import bio.knowledge.server.model.ServerConceptWithDetails;
import bio.knowledge.server.model.ServerKnowledgeBeacon;
import bio.knowledge.server.model.ServerLogEntry;
import bio.knowledge.server.model.ServerPredicate;
import bio.knowledge.server.model.ServerStatement;
import bio.knowledge.server.model.ServerStatementSubject;
import bio.knowledge.server.model.ServerSummary;

@Service
public class ControllerImpl implements Util, ConceptTypeUtil {

	private static Logger _logger = LoggerFactory.getLogger(ControllerImpl.class);

	Map<String, HashSet<String>> cache = new HashMap<String, HashSet<String>>();
	
	@Autowired private KnowledgeBeaconRegistry registry;

	@Autowired private KnowledgeBeaconService kbs;
		
	@Autowired private ExactMatchesHandler exactMatchesHandler;
	
	@Autowired private ConceptTypeService conceptTypeService;
	
	@Autowired private ConceptCliqueService conceptCliqueService;
	
	@Autowired private PredicatesRegistry predicatesRegistry;

	private Integer fixInteger(Integer i) {
		return i != null && i >= 1 ? i : 1;
	}

	private String fixString(String str) {
		return str != null ? str : "";
	}
	
	private List<String> fixString(List<String> l) {
		if (l == null) return new ArrayList<>();
		
		for (int i = 0; i < l.size(); i++) {
			l.set(i, fixString(l.get(i)));
		}
		
		return l;
	}

	
	/**
	 * 
	 * @param request
	 * @return url used to make the request
	 */
	private String getUrl(HttpServletRequest request) {
		String query = request.getQueryString();
		query = (query == null)? "" : "?" + query;
		return request.getRequestURL() + query;
	}
	
	private void logError(String sessionId, Exception e) {
		
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
		
		String message = e.getMessage();
		if(message!=null) _logger.error(sessionId+": "+message);
		
		kbs.logError(sessionId, "aggregator", getUrl(request), e.getMessage());
	}
	
	
	private <T> Map<KnowledgeBeaconImpl, List<T>> waitFor(CompletableFuture<Map<KnowledgeBeaconImpl, List<T>>> future) {
		return waitFor(
				future,
				// Scale the timeout proportionately to the number of beacons?
				registry.countAllBeacons()*KnowledgeBeaconService.BEACON_TIMEOUT_DURATION
		) ; 
	}
	 
	/*
	 * Waits {@code TIMEOUT} {@code TIMEUNIT} for the future to complete, throwing a runtime exception otherwise.
	 * @param future
	 * @return
	 */
	private <T> Map<KnowledgeBeaconImpl, List<T>> 
		waitFor(
				CompletableFuture<Map<KnowledgeBeaconImpl, List<T>>> future,
				long timeout
		) {
		try {
			return future.get(timeout, KnowledgeBeaconService.BEACON_TIMEOUT_UNIT);
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			throw new RuntimeException(e.getMessage(), e.getCause());
		}
	}

	public ResponseEntity<List<ServerKnowledgeBeacon>> getBeacons() {
		
		List<ServerKnowledgeBeacon> beacons = new ArrayList<>();
		for (Object beacon : registry.getKnowledgeBeacons()) {
			beacons.add(ModelConverter.convert(beacon, ServerKnowledgeBeacon.class));
		}
		
		return ResponseEntity.ok(beacons);
	}

	public ResponseEntity<List<ServerLogEntry>> getErrors(String sessionId) {
		sessionId = fixString(sessionId);

		List<ServerLogEntry> responses = new ArrayList<>();
		for (Object entry : kbs.getErrors(sessionId)) {
			if (entry != null) {
				responses.add(ModelConverter.convert(entry, ServerLogEntry.class));
			}
		}

		return ResponseEntity.ok(responses);
	}
	
	public ResponseEntity<List<ServerConcept>> getConcepts(String keywords, String conceptTypes, Integer pageNumber,
			Integer pageSize, List<String> beacons, String sessionId) {
		
		try {
			
			pageNumber = fixInteger(pageNumber);
			pageSize = fixInteger(pageSize);
			keywords = fixString(keywords);
			conceptTypes = fixString(conceptTypes);
			beacons = fixString(beacons);
			sessionId = fixString(sessionId);
	
			CompletableFuture<Map<KnowledgeBeaconImpl, List<BeaconConcept>>>
				future = kbs.getConcepts(keywords, conceptTypes, pageNumber, pageSize, beacons, sessionId);
			
			Map<KnowledgeBeaconImpl, List<BeaconConcept>> map = 
					waitFor(
							future,
							kbs.weightedTimeout(beacons,pageSize)
					);
			
			Map<String,ServerConcept> responses = new HashMap<String,ServerConcept>();

			for (KnowledgeBeaconImpl beacon : map.keySet()) {
				
				for (BeaconConcept response : map.get(beacon)) {
					
					ServerConcept translation = Translator.translate(response);

					// First iteration, from beacons, is the Concept semantic type?
					String conceptType = translation.getType();
					
					List<ConceptType> types = 
							conceptTypeService.lookUpByIdentifier(conceptType);
					
					ConceptClique ecc = 
							exactMatchesHandler.getExactMatches(
										beacon,
										response.getId(),
										translation.getName(),
										types,
										translation.getTaxon()
									);
					
					String taxon = translation.getTaxon();
					if(taxon==null) {
						/*
						 * If the beacon doesn't know for sure
						 * then ask the clique about their taxon
						 */
						translation.setTaxon(ecc.getTaxon());
					}
					
					String cliqueId = ecc.getId();
					
					if(!responses.containsKey(cliqueId)) {
						
						translation.setClique(cliqueId);
						
						// fix the concept type if necessary
						translation.setType(
								conceptCliqueService.fixConceptType(ecc, translation.getType())
						);
						
						responses.put(cliqueId,translation);
					}
				}
			}
				
			return ResponseEntity.ok(new ArrayList<ServerConcept>(responses.values()));
		
		} catch (Exception e) {
			logError(sessionId, e);
			return ResponseEntity.ok(new ArrayList<ServerConcept>()); // empty list if exception thrown?
		}
	}

	public ResponseEntity<List<ServerPredicate>> getPredicates() {
		try {
			CompletableFuture<
				Map<KnowledgeBeaconImpl, 
				List<BeaconPredicate>>
			> future = kbs.getAllPredicates();

			Map<KnowledgeBeaconImpl, List<BeaconPredicate>> map = waitFor( future );

			for (KnowledgeBeaconImpl beacon : map.keySet()) {
				for (BeaconPredicate response : map.get(beacon)) {
					/*
					 *  No "conversion" here, but response 
					 *  handled by the indexPredicate function
					 */
					predicatesRegistry.indexPredicate(response,beacon.getId());
				}
			}
			
			List<ServerPredicate> responses = 
					new ArrayList<ServerPredicate>(predicatesRegistry.values());
			
			return ResponseEntity.ok(responses);

		} catch (Exception e) {
			logError("Predicates", e);
			return ResponseEntity.ok(new ArrayList<>());
		}
	}
	
	static final Set TaxonTags = new HashSet<String>(); 
	static {
		TaxonTags.add("taxon");   // Biolink?
		TaxonTags.add("wd:p703"); // from Wikidata
	}
	
	private String findTaxon(List<ServerConceptDetail> details) {
		Optional<ServerConceptDetail> taxonOpt = 
				details.
					stream().
						filter( d -> TaxonTags.contains(d.getTag().toLowerCase()) ).
							findFirst();
		if(taxonOpt.isPresent()) {
			ServerConceptDetail taxon = taxonOpt.get();
			return taxon.getValue();
		}
		return "";
	}

	public ResponseEntity<ServerConceptWithDetails> getConceptDetails(
			String cliqueId, 
			List<String> 
			beacons, 
			String sessionId
	) {
		try {
		
			cliqueId  = fixString(cliqueId);
			beacons   = fixString(beacons);
			sessionId = fixString(sessionId);

			ConceptClique ecc = exactMatchesHandler.getClique(cliqueId);
			
			if(ecc==null) 
				throw new RuntimeException("getConceptDetails(): '"+cliqueId+"' could not be found?") ;

			CompletableFuture<
								Map<KnowledgeBeaconImpl, 
								List<BeaconConceptWithDetails>>
							> future = kbs.getConceptDetails(ecc, beacons, sessionId);

			Map<
				KnowledgeBeaconImpl, List<BeaconConceptWithDetails>
			> map = waitFor(
					future,
					kbs.weightedTimeout(beacons,1)
			);  // Scale timeout proportionately to the number of beacons only?
			
			ServerConceptWithDetails conceptDetails = new ServerConceptWithDetails();
			
			conceptDetails.setClique(cliqueId);
			
			/* 
			 * Defer name setting below; 
			 * clique name seems to be the 
			 * same as the cliqueId right now... 
			 * not sure if that is correct?
			 * 
			 * conceptDetails.setName(ecc.getName()); 
			 */
			conceptDetails.setType(ecc.getConceptType());
			conceptDetails.setAliases(ecc.getConceptIds());
			conceptDetails.setTaxon(ecc.getTaxon());
			
			List<ServerConceptBeaconEntry> entries = conceptDetails.getEntries();
			
			for (KnowledgeBeaconImpl beacon : map.keySet()) {
				
				for (BeaconConceptWithDetails response : map.get(beacon)) {
					
					/*
					 * Simple heuristic to set the name to something sensible.
					 * Since beacon-to-beacon names may diverge, may not always
					 * give the "best" name (if such a thing exists...)
					 */
					if( conceptDetails.getName() == null )
						conceptDetails.setName(response.getName());
					
					ServerConceptBeaconEntry entry = Translator.translate(response);
					entry.setBeacon(beacon.getId());
					
					/*
					 * Heuristic to set and/or dynamically 
					 * repair the clique taxon assignment taxon
					 */
					String cliqueTaxon = conceptDetails.getTaxon();
					// Scrounge among the entry details
					String reportedTaxon = findTaxon(entry.getDetails());
					
					if(cliqueTaxon.isEmpty()) {
						
						if(!reportedTaxon.isEmpty()) {
							conceptDetails.setTaxon(reportedTaxon);
							
							// Opportunistically update the clique taxon along the way?
							ecc.setTaxon(reportedTaxon);
							ecc = exactMatchesHandler.updateAndRefresh(ecc);
						}
					} else if(!reportedTaxon.equals(cliqueTaxon)) {
						conceptDetails.setTaxon(reportedTaxon);
						/*
						 * Oops! This concept clique needs to be 
						 * split along interspecific lines!
						 * It is hard to say what the correct
						 * course of action is at this point,
						 * but after splitting the clique,
						 * we ignore the entry that had the
						 * different taxon (as being from the
						 * wrong species). This decision may 
						 * backfire occasionally if the user
						 * was really interested in the
						 * given identifier...
						 */
						ConceptClique[] cliquePair = 
								exactMatchesHandler.splitClique( 
										ecc, 
										beacon.getId(), 
										response.getId(), 
										reportedTaxon 
								);
						
						// Update the clique of the record to be returned(?)
						ecc = cliquePair[0];
						conceptDetails.setClique(ecc.getId());
						conceptDetails.setAliases(ecc.getConceptIds());
						
						continue;
					}
					
					// If you made it this far, capture the entry..
					entries.add(entry);
				}
			}
	
			return ResponseEntity.ok(conceptDetails);
			
		} catch (Exception e) {
			logError(sessionId, e);
			return ResponseEntity.ok(null);
		}
	}
	
	private Boolean matchToList(String conceptId, String conceptName, List<String> identifiers ) {
		
		String idPattern = "(?i:"+conceptId+")";
		
		/*
		 *  Special test for the presence of 
		 *  Human Gene Nomenclature Consortium (and geneCards) symbols.
		 *  Case insensitive match to non-human species symbols
		 *  which may have difference letter case?
		 */
		String hgncSymbolPattern = "HGNC.SYMBOL:(?i:"+conceptName.toUpperCase()+")";
		String genecardsPattern = "GENECARDS:(?i:"+conceptName.toUpperCase()+")";
		String umlsPattern = "UMLS:(?i:"+conceptName.toUpperCase()+")";
		
		for(String id : identifiers) {
			
			if(id.matches(idPattern)) 
				return true;
			
			if(id.matches(hgncSymbolPattern)) 
				return true;
			
			if(id.matches(genecardsPattern)) 
				return true;
			
			if(id.matches(umlsPattern)) 
				return true;
		}
		return false;
	}
	
	/**
	 * 
	 * @param source
	 * @param relations
	 * @param target
	 * @param keywords
	 * @param conceptTypes
	 * @param pageNumber
	 * @param pageSize
	 * @param beacons
	 * @param sessionId
	 * @return
	 */
	public ResponseEntity<List<ServerStatement>> getStatements(
			String source,
			String relations,
			String target,
			String keywords,
			String conceptTypes,
			Integer pageNumber, 
			Integer pageSize, 
			List<String> beacons, 
			String sessionId
	) {
		try {
			
			source = fixString(source);
			relations = fixString(relations);
			target = fixString(target);
			keywords = fixString(keywords);
			conceptTypes = fixString(conceptTypes);
			pageNumber = fixInteger(pageNumber);
			pageSize = fixInteger(pageSize);
			beacons = fixString(beacons);
			sessionId = fixString(sessionId);
			
			if(source.isEmpty()) {
				_logger.error("ControllerImpl.getStatements(): empty source clique string encountered?") ;
				return ResponseEntity.ok(new ArrayList<>());
			}
			
			ConceptClique sourceClique = exactMatchesHandler.getClique(source);
			if(sourceClique==null) {
				_logger.warn("ControllerImpl.getStatements(): source clique '"+source+"' could not be found?") ;
				return ResponseEntity.ok(new ArrayList<>());
			}
			
			/* 
			 * If the beacon aggregator client is attempting relation filtering
			 * then we should ensure that the PredicateRegistry is initialized
			 */
			if(relations!=null && predicatesRegistry.isEmpty()) getPredicates();

			ConceptClique targetClique = null;
			if(!target.isEmpty()) {
				targetClique = exactMatchesHandler.getClique(target);
				if(targetClique==null) {
					_logger.warn("ControllerImpl.getStatements(): target clique '"+target+"' could not be found?") ;
					return ResponseEntity.ok(new ArrayList<>());
				}
			}
			
			CompletableFuture<Map<KnowledgeBeaconImpl, List<BeaconStatement>>> future = 
					kbs.getStatements( sourceClique, relations, targetClique, keywords, conceptTypes, pageNumber, pageSize, beacons, sessionId );
			
			List<ServerStatement> responses = new ArrayList<ServerStatement>();
			Map<
				KnowledgeBeaconImpl, 
				List<BeaconStatement>
			> map = waitFor(future,kbs.weightedTimeout(beacons, pageSize));

			for (KnowledgeBeaconImpl beacon : map.keySet()) {
				
				String beaconId = beacon.getId();
				
				_logger.debug("ctrl.getStatements(): processing beacon '"+beaconId+"'...");
			
				for ( BeaconStatement response : map.get(beacon)) {
					
					/*
					 * Sanity check: to get around the fact that some beacons 
					 * (like Biolink) will sometimes send back statements
					 *  with a null *%$@?!?!!! subject or object 
					 */
					if( response.getSubject()==null || response.getObject() == null ) continue;
										
					ServerStatement translation = Translator.translate(response);
					translation.setBeacon(beaconId);
					
					// Heuristic: need to somehow tag the equivalent concept here?
					ServerStatementSubject subject  = translation.getSubject();
					String subjectId = subject.getId();
					String subjectName = subject.getName();
					
					/*
					 * The existing beacons may not send the semantic group 
					 * back as a CURIE, thus coerce it accordingly
					 */
					String subjectTypeId = subject.getType();
					
					List<ConceptType> subjectTypes = 
							conceptTypeService.lookUpByIdentifier(subjectTypeId);
					
					subject.setType(curieList(subjectTypes));
					
					ConceptClique subjectEcc = 
							exactMatchesHandler.getExactMatches(
													beacon,
													subjectId,
													subjectName,
													subjectTypes,
													"" // don't currently expect the statements to return taxonomic info?
												);
					
					bio.knowledge.server.model.ServerStatementObject object = translation.getObject();
					String objectId = object.getId();
					String objectName = object.getName();
					
					/*
					 * The existing beacons may not send the semantic group 
					 * back as a CURIE, thus coerce it accordingly
					 */
					String objectTypeId = object.getType();
					
					List<ConceptType> objectTypes = 
							conceptTypeService.lookUpByIdentifier(objectTypeId);

					object.setType(curieList(objectTypes));
					
					ConceptClique objectEcc = 
							exactMatchesHandler.getExactMatches(
													beacon,
													objectId,
													objectName,
													objectTypes,
													"" // don't currently expect the statements to return taxonomic info?
												);
					
					/*
					 * Need to refresh the ecc clique in case either 
					 * subject or object id was discovered to belong 
					 * to it during the exact matches operations above?
					 */
					sourceClique = exactMatchesHandler.getClique(source);
					
					List<String> conceptIds = sourceClique.getConceptIds(beaconId);
					
					_logger.debug("ctrl.getStatements(): processing statement '"+translation.getId()
								+ " from beacon '"+beaconId + "' "
								+ "with subject id '"+subjectId + "' "
								+ "and object id '"+objectId+"'"
								+ " matched against conceptIds: '"+String.join(",",conceptIds)+"'"
							);
					
					if( matchToList( subjectId, subjectName, conceptIds ) ) {
						
						subject.setClique(sourceClique.getId());
						/*
						 * Temporary workaround for beacons not yet 
						 * setting their statement subject semantic groups?
						 */
						String ssg = subject.getType();
						if( ( nullOrEmpty(ssg) || ssg.equals(Category.DEFAULT_SEMANTIC_GROUP)) && sourceClique != null )
							subject.setType(sourceClique.getConceptType());
						
						object.setClique(objectEcc.getId());
						/*
						 * Temporary workaround for beacons not yet 
						 * setting their statement object semantic groups?
						 */
						String osg = object.getType();
						if( ( nullOrEmpty(osg) || osg.equals(Category.DEFAULT_SEMANTIC_GROUP)) && objectEcc != null )
							object.setType(objectEcc.getConceptType());
						
					} else if( matchToList( objectId, objectName, conceptIds ) ) {
						
						object.setClique(sourceClique.getId()) ;
						/*
						 * Temporary workaround for beacons not yet 
						 * setting their statement object semantic groups?
						 */
						String objectConceptType = object.getType();
						if( (nullOrEmpty( objectConceptType) || 
							  objectConceptType.equals(Category.DEFAULT_SEMANTIC_GROUP)) && sourceClique != null
						)
							object.setType(sourceClique.getConceptType());
						
						subject.setClique(subjectEcc.getId());
						/*
						 * Temporary workaround for beacons not yet 
						 * setting their statement subject semantic groups?
						 */
						String subjectConceptType = subject.getType();
						if( ( nullOrEmpty(subjectConceptType) || 
							  subjectConceptType.equals(Category.DEFAULT_SEMANTIC_GROUP) ) && subjectEcc != null 
						)
							object.setType(subjectEcc.getConceptType());	
						
					} else {
						
						_logger.warn("ctrl.getStatements() WARNING: "
								+ "clique is unknown (null) "
								+ "for statement '"+translation.getId()
								+ "from beacon '"+beaconId
								+"' with subject '"+subject.getName()
								+"' ["+subjectId+"]"
								+ " and object '"+object.getName()
								+"' ["+objectId+"]"
								+ " matched against conceptIds: '"+
								String.join(",",conceptIds)+"'"
						);
						continue;
					}
					
					/*
					 *  Heuristic workaround for beacons which have not yet properly 
					 *  implemented the tagging of semantic groups of concepts,
					 *  to try to set their semantic group type
					 */
					if( subject.getClique() != null && 
						subject.getType() == null) {
							
							subject.setType(
										BioNameSpace.defaultConceptType( subject.getClique() ).getCurie()
									);
					}
					
					if( object.getClique() != null && 
						object.getType() == null) {
							
							object.setType(
										BioNameSpace.defaultConceptType( object.getClique() ).getCurie()
									);
					}
					responses.add(translation);
				}
			}
			
			if( ! relations.isEmpty() ) {
				final String relationFilter = relations;
				responses = responses.stream()
						.filter(
							s -> s.getPredicate().getId().equals(relationFilter) ? true : false 
				).collect(Collectors.toList());
			}
			
			return ResponseEntity.ok(responses);
		} catch (Exception e) {
			
			logError(sessionId, e);
			return ResponseEntity.ok(new ArrayList<>());
		}
	}
	
	public ResponseEntity<List<ServerAnnotation>> getEvidence(String statementId, String keywords, Integer pageNumber, Integer pageSize, List<String> beacons, String sessionId) {
		try {
		
			pageNumber = fixInteger(pageNumber);
			pageSize = fixInteger(pageSize);
			keywords = fixString(keywords);
			statementId = fixString(statementId);
			beacons = fixString(beacons);
			sessionId = fixString(sessionId);
			
			CompletableFuture<Map<KnowledgeBeaconImpl, List<BeaconAnnotation>>> future = 
					kbs.getEvidence(statementId, keywords, pageNumber, pageSize, beacons, sessionId);
			
			List<ServerAnnotation> responses = new ArrayList<ServerAnnotation>();
			Map<
				KnowledgeBeaconImpl, 
				List<BeaconAnnotation>
			> map = waitFor(future,kbs.weightedTimeout(beacons, pageSize));
					
			for (KnowledgeBeaconImpl beacon : map.keySet()) {
				for (Object response : map.get(beacon)) {
					ServerAnnotation translation = ModelConverter.convert(response, ServerAnnotation.class);
					translation.setBeacon(beacon.getId());
					responses.add(translation);
				}
			}
			
			return ResponseEntity.ok(responses);
			
		} catch (Exception e) {
			logError(sessionId, e);
			return ResponseEntity.ok(new ArrayList<>());
		}
	}
	
	public ResponseEntity<List<ServerSummary>> linkedTypes(List<String> beacons, String sessionId) {
		try {
			
			beacons = fixString(beacons);
			sessionId = fixString(sessionId);
		
			CompletableFuture<Map<KnowledgeBeaconImpl, List<BeaconSummary>>>
				future = kbs.linkedTypes(beacons, sessionId);
			
			List<ServerSummary> responses = new ArrayList<ServerSummary>();
			Map<
				KnowledgeBeaconImpl, 
				List<BeaconSummary>
			> map = waitFor(future);
			
			for (KnowledgeBeaconImpl beacon : map.keySet()) {
				for (Object summary : map.get(beacon)) {
					ServerSummary translation = ModelConverter.convert(summary, ServerSummary.class);
					translation.setId(
							conceptCliqueService.fixConceptType(null, translation.getId())
					);
					translation.setBeacon(beacon.getId());	
					responses.add(translation);
				}
			}
	
			return ResponseEntity.ok(responses);
		
		} catch (Exception e) {
			logError(sessionId, e);
			return ResponseEntity.ok(new ArrayList<>());
		}
    }

	/**
	 * 
	 * @param identifier
	 * @param sessionId
	 * @return
	 */
	public ResponseEntity<ServerCliqueIdentifier> getClique(String identifier, String sessionId) {
		ConceptClique clique = 
				exactMatchesHandler.getConceptClique(new String[] { identifier });
		if(clique!=null) {
			ServerCliqueIdentifier cliqueId = new ServerCliqueIdentifier();
			cliqueId.setCliqueId(clique.getId());
			return ResponseEntity.ok(cliqueId);
		} else
			return ResponseEntity.ok(null);
	}
	
}

