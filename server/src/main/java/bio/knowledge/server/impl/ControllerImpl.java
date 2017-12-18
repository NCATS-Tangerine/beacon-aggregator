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

import bio.knowledge.aggregator.ConceptCliqueService;
import bio.knowledge.aggregator.ConceptTypeService;
import bio.knowledge.aggregator.ConceptTypeUtil;
import bio.knowledge.aggregator.KnowledgeBeaconImpl;
import bio.knowledge.aggregator.KnowledgeBeaconRegistry;
import bio.knowledge.aggregator.KnowledgeBeaconService;
import bio.knowledge.client.model.BeaconAnnotation;
import bio.knowledge.client.model.BeaconConcept;
import bio.knowledge.client.model.BeaconConceptDetail;
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
import bio.knowledge.server.model.ServerConceptDetail;
import bio.knowledge.server.model.ServerConceptWithDetails;
import bio.knowledge.server.model.ServerKnowledgeBeacon;
import bio.knowledge.server.model.ServerLogEntry;
import bio.knowledge.server.model.ServerPredicate;
import bio.knowledge.server.model.ServerStatement;
import bio.knowledge.server.model.ServerStatementSubject;
import bio.knowledge.server.model.ServerSummary;

@Service
public class ControllerImpl implements ConceptTypeUtil {

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
	
			List<ServerConcept> responses = new ArrayList<ServerConcept>();
			
			Map<KnowledgeBeaconImpl, List<BeaconConcept>> map = 
					waitFor(
							future,
							kbs.weightedTimeout(beacons,pageSize)
					);
			
			for (KnowledgeBeaconImpl beacon : map.keySet()) {
				
				for (Object response : map.get(beacon)) {
					
					ServerConcept translation = ModelConverter.convert(response, ServerConcept.class);

					// First iteration, from beacons, is the Concept semantic type?
					String conceptType = translation.getSemanticGroup();
					
					List<ConceptType> types = 
							conceptTypeService.lookUpByIdentifier(conceptType);
					
					ConceptClique ecc = 
							exactMatchesHandler.getExactMatches(
										beacon,
										translation.getId(),
										translation.getName(),
										types
									);
					
					translation.setClique(ecc.getId());
					translation.setSemanticGroup(
							conceptCliqueService.fixConceptType(ecc, translation.getSemanticGroup())
					);
					translation.setAliases(ecc.getConceptIds());
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

	public ResponseEntity<List<ServerConceptWithDetails>> getConceptDetails(
			String cliqueId, 
			List<String> 
			beacons, 
			String sessionId
	) {
		try {
		
			cliqueId = fixString(cliqueId);
			beacons = fixString(beacons);
			sessionId = fixString(sessionId);

			ConceptClique ecc = exactMatchesHandler.getClique(cliqueId);
			if(ecc==null) throw new RuntimeException("getConceptDetails(): '"+cliqueId+"' could not be found?") ;

			CompletableFuture<
								Map<KnowledgeBeaconImpl, 
								List<BeaconConceptWithDetails>>
							> future = kbs.getConceptDetails(ecc, beacons, sessionId);
	
			List<ServerConceptWithDetails> responses = 
					new ArrayList<ServerConceptWithDetails>();
			Map<
				KnowledgeBeaconImpl, 
				List<BeaconConceptWithDetails>
			> map = waitFor(
					future,
					kbs.weightedTimeout(beacons,1)
			);  // Scale timeout proportionately to the number of beacons only?
					
			for (KnowledgeBeaconImpl beacon : map.keySet()) {
				for (Object response : map.get(beacon)) {
					
					ServerConceptWithDetails translation = 
							ModelConverter.convert(response, ServerConceptWithDetails.class);
					
					translation.setClique(ecc.getId());
					translation.setSemanticGroup(
							conceptCliqueService.fixConceptType(ecc, translation.getSemanticGroup())
					);
					translation.setAliases(ecc.getConceptIds());
					
					/*
					 * Try to retrieve any details returned...
					 *  Note that the details returned by Translator 
					 *  are still the original beacon version of the Java object!
					 */
					@SuppressWarnings({ "unchecked", "rawtypes" })
					List<BeaconConceptDetail> details = 
								(List<BeaconConceptDetail>)(List)translation.getDetails();
					
					List<ServerConceptDetail> translatedDetails = 
													new ArrayList<ServerConceptDetail>();
					
					for(BeaconConceptDetail detail : details) {
						ServerConceptDetail sdt = new ServerConceptDetail();
						String tag   = detail.getTag();
						String value = detail.getValue();
						sdt.setTag(tag);
						sdt.setValue(value);
						translatedDetails.add(sdt);
						_logger.debug("getConceptDetails() details - Tag: '"+tag+
								      "' = Value: '"+value.toString()+"'");
					}
					
					translation.setDetails(translatedDetails);
					
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
										
					ServerStatement translation = ModelConverter.convert(response, ServerStatement.class);
					translation.setBeacon(beaconId);
					
					// Heuristic: need to somehow tag the equivalent concept here?
					ServerStatementSubject subject  = translation.getSubject();
					String subjectId = subject.getId();
					String subjectName = subject.getName();
					
					/*
					 * The existing beacons may not send the semantic group 
					 * back as a CURIE, thus coerce it accordingly
					 */
					String subjectTypeId = subject.getSemanticGroup();
					
					List<ConceptType> subjectTypes = 
							conceptTypeService.lookUpByIdentifier(subjectTypeId);
					
					subject.setSemanticGroup(curieList(subjectTypes));
					
					ConceptClique subjectEcc = 
							exactMatchesHandler.getExactMatches(
													beacon,
													subjectId,
													subjectName,
													subjectTypes
												);
					
					bio.knowledge.server.model.ServerStatementObject object = translation.getObject();
					String objectId = object.getId();
					String objectName = object.getName();
					
					/*
					 * The existing beacons may not send the semantic group 
					 * back as a CURIE, thus coerce it accordingly
					 */
					String objectTypeId = object.getSemanticGroup();
					
					List<ConceptType> objectTypes = 
							conceptTypeService.lookUpByIdentifier(objectTypeId);

					object.setSemanticGroup(curieList(objectTypes));
					
					ConceptClique objectEcc = 
							exactMatchesHandler.getExactMatches(
													beacon,
													objectId,
													objectName,
													objectTypes
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
						String ssg = subject.getSemanticGroup();
						if( ( ssg==null || ssg.isEmpty() || ssg.equals(Category.DEFAULT_SEMANTIC_GROUP)) && sourceClique != null )
							subject.setSemanticGroup(sourceClique.getConceptType());
						
						object.setClique(objectEcc.getId());
						/*
						 * Temporary workaround for beacons not yet 
						 * setting their statement object semantic groups?
						 */
						String osg = object.getSemanticGroup();
						if( ( osg==null || osg.isEmpty() || osg.equals(Category.DEFAULT_SEMANTIC_GROUP)) && objectEcc != null )
							object.setSemanticGroup(objectEcc.getConceptType());
						
					} else if( matchToList( objectId, objectName, conceptIds ) ) {
						
						object.setClique(sourceClique.getId()) ;
						/*
						 * Temporary workaround for beacons not yet 
						 * setting their statement object semantic groups?
						 */
						String objectConceptType = object.getSemanticGroup();
						if( ( objectConceptType==null ||
							  objectConceptType.isEmpty() || 
							  objectConceptType.equals(Category.DEFAULT_SEMANTIC_GROUP)) && sourceClique != null
						)
							object.setSemanticGroup(sourceClique.getConceptType());
						
						subject.setClique(subjectEcc.getId());
						/*
						 * Temporary workaround for beacons not yet 
						 * setting their statement subject semantic groups?
						 */
						String subjectConceptType = subject.getSemanticGroup();
						if( ( subjectConceptType==null || 
							  subjectConceptType.isEmpty() || 
							  subjectConceptType.equals(Category.DEFAULT_SEMANTIC_GROUP)) && subjectEcc != null 
						)
							object.setSemanticGroup(subjectEcc.getConceptType());	
						
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
						subject.getSemanticGroup() == null) {
							
							subject.setSemanticGroup(
										BioNameSpace.defaultConceptType( subject.getClique() ).getCurie()
									);
					}
					
					if( object.getClique() != null && 
						object.getSemanticGroup() == null) {
							
							object.setSemanticGroup(
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
