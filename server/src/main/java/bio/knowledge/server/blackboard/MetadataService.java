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

import java.util.*;
import java.util.stream.Collectors;

import bio.knowledge.server.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import bio.knowledge.Util;
import bio.knowledge.aggregator.KnowledgeBeacon;
import bio.knowledge.aggregator.KnowledgeBeaconRegistry;
import bio.knowledge.aggregator.KnowledgeBeaconService;
import bio.knowledge.aggregator.LogEntry;

/**
 * This class manages a cache of Knowledge Beacon network metadata 
 * such as concept data types, predicate relations and the
 * "Knowledge Map" subject type-predicate-object type combinations thereof.
 * 
 * This class also wraps access to global beacon metadata and 
 * (session indexed) API call system logs.
 * 
 * @author richard
 *
 */
@Service
public class MetadataService implements Util {

	@Autowired private KnowledgeBeaconRegistry registry;
	@Autowired private KnowledgeBeaconService kbs;
	@Autowired private BeaconHarvestService beaconHarvestService;
	@Autowired private MetadataRegistry metadataRegistry;

/************************** Beacon Descriptions **************************/

	public List<ServerKnowledgeBeacon> getKnowledgeBeacons()  throws BlackboardException {
		
		List<ServerKnowledgeBeacon> responses = new ArrayList<ServerKnowledgeBeacon>();
		
		try {
			
			List<KnowledgeBeacon> beacons = 
					registry.getKnowledgeBeacons();
			
			for (KnowledgeBeacon beacon : beacons) {
				responses.add(Translator.translate(beacon));
			}
			
		} catch (Exception e) {
			throw new BlackboardException(e);
		}
		
		return responses;
	}
	
/************************** Error tracking **************************/
	
	/**
	 * 
	 * @param sessionId
	 * @param beacon
	 * @param query
	 * @param message
	 */
	public void logError(String sessionId, Integer beacon, String query, String message) {
		kbs.logError(sessionId, beacon, query, message);
	}

	/**
	 * 
	 * @param queryId
	 * @return
	 */
	public List<ServerLogEntry> getErrorLog(String queryId) throws BlackboardException {
		
		List<ServerLogEntry> responses = new ArrayList<>();
		
		try {
			List<LogEntry> entries = kbs.getErrorLog(queryId);
			
			for (LogEntry entry : entries) {
				if (entry != null) {
					responses.add(ModelConverter.convert(entry, ServerLogEntry.class));
				}
			}
		} catch (Exception e) {
			throw new BlackboardException(e);
		}

		return responses;
	}
	
/************************** Concept Type Cache **************************/

	/**
	 * TODO: We don't currently filter out nor log the Concept Types retrieval (beacons and sessionId parameters ignored)
	 * 
	 * @param beacons
	 * @param sessionId
	 * @return Server Concept Type records
	 */
	public Collection<? extends ServerConceptCategory> 
			getConceptTypes( List<Integer> beacons )  throws BlackboardException {

		Collection<? extends ServerConceptCategory> categories = null;
		
		try {
			
			Map<String,ServerConceptCategory> conceptTypes = metadataRegistry.getConceptCategoriesMap();
					
			// Sanity check: is the Server Concept Type cache loaded?
			if(conceptTypes.isEmpty()) 
				beaconHarvestService.loadConceptTypes();
	
			categories = conceptTypes.values();
			
		} catch(Exception e) {
			throw new BlackboardException(e);
		}
		
		return categories;
	}

/************************** Predicate Registry **************************/

	/**
	 * Returns the (Reasoner API formatted) Map of Predicates
	 *
	 * @return Predicate Map
	 * @throws BlackboardException
	 */
	public Map<String, Map<String, List<String>>> getPredicates() throws BlackboardException {
		try {
			/*
			 * Use the KBA Knowledge Map (all beacons)
			 * to compile the Reasoner API predicate map
			 */

			List<ServerKnowledgeMap> responses = getKnowledgeMap(new ArrayList<>());

			Map<String, Map<String,List<String>>> subject_map = new HashMap<>();

			for(ServerKnowledgeMap item:responses) {
				List<ServerKnowledgeMapStatement> statements = item.getStatements();
				for(ServerKnowledgeMapStatement statement:statements) {

					ServerKnowledgeMapSubject subject = statement.getSubject();
					String subject_category = subject.getCategory();

					if(!subject_map.containsKey(subject_category)) {
						subject_map.put(subject_category,new HashMap<>());
					}
					Map<String,List<String>> object_map = subject_map.get(subject_category);

					ServerKnowledgeMapObject object = statement.getObject();
					String object_category = object.getCategory();

					if(!object_map.containsKey(object_category)) {
						object_map.put(object_category, new ArrayList<>());
					}
					List<String> predicate_list = object_map.get(object_category);

					ServerKnowledgeMapPredicate predicate = statement.getPredicate();
					String predicate_label = predicate.getEdgeLabel();

					predicate_list.add(predicate_label);
				}
			}

			/*
			 * Here, we want to ensure that the predicate lists are not duplicated across beacons
			 * therefore, we iterate across the whole catalog of predicates that were compiled above.
			 */
			for( String subject_category : subject_map.keySet() ) {
				Map<String,List<String>> object_map = subject_map.get(subject_category);
				for(String object_category : object_map.keySet() ) {
					List<String> predicate_list = object_map.get(object_category);
					object_map.put(
							object_category,
							predicate_list.stream().
									distinct().
									collect(Collectors.toList()));


				}
			}

			return subject_map;

		} catch (Exception e) {
			throw new BlackboardException(
					"ERROR: Couldn't serialize response for content type application/json: "+
							e.getMessage()
			);
		}
	}
	/**
	 * TODO: We don't currently filter out nor log the
	 * Predicates retrieval (beacons and sessionId parameters ignored)
	 * 
	 * @param beacons
	 * @return Server Predicate entries
	 */
	public List<ServerPredicate> getPredicatesDetails(List<Integer> beacons) throws BlackboardException {
		
		List<ServerPredicate> response = null;
		
		try {
			
			Map<String,ServerPredicate> predicates = metadataRegistry.getPredicatesMap() ;
			
			// Sanity check: is the Server Predicate cache loaded?
			if(predicates.isEmpty()) 
				beaconHarvestService.loadPredicates();
	
			response =  new ArrayList<>(predicates.values());
			
		} catch(Exception e) {
			throw new BlackboardException(e);
		}
	
		return response;
	}

/************************** Knowledge Map **************************/
			
	/**
	 * 
	 * @param beacons
	 * @param sessionId
	 * @return
	 */
	public List<ServerKnowledgeMap> getKnowledgeMap(List<Integer> beacons) throws BlackboardException { 
		
		List<ServerKnowledgeMap> kmaps = null;
		
		try {
			
			kmaps = beaconHarvestService.getKnowledgeMap(beacons);
			
			/*
			 * TODO: need to cache the knowledge map here?
			 */

		} catch(Exception e) {
			throw new BlackboardException(e);
		}
		
		return kmaps;
	}

}
