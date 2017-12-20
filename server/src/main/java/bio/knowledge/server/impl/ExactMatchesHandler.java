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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import bio.knowledge.Util;
import bio.knowledge.aggregator.ConceptCliqueService;
import bio.knowledge.aggregator.ConceptTypeService;
import bio.knowledge.aggregator.ConceptTypeUtil;
import bio.knowledge.aggregator.KnowledgeBeaconImpl;
import bio.knowledge.aggregator.KnowledgeBeaconRegistry;
import bio.knowledge.aggregator.KnowledgeBeaconService;
import bio.knowledge.database.repository.aggregator.ConceptCliqueRepository;
import bio.knowledge.model.CURIE;
import bio.knowledge.model.ConceptType;
import bio.knowledge.model.aggregator.ConceptClique;
import bio.knowledge.server.impl.Cache.CacheLocation;

/*
 * RMB September 26 revision: removed 'sessionId' from all calls since 
 * equivalent concept clique resolution is a global operation.
 * 
 * Added a new global application cache to minimize lookups for equivalent concept id's 
 * to ensure that the system doesn't repetitively retrieve the same cliques
 * from the back end data sources more than once in an application session(?)
 *
 *  One concern I have is whether or not the ConceptClique cached is "complete" when
 *  indexed in the cache. The conceptIds are a list of conceptId.. 
 *  Every conceptId in the clique ought to be cached to the same clique...
 *  We may have to tweak the Cache behaviour for non-redundant multi-key caching?
 */
@Service
public class ExactMatchesHandler implements Util, ConceptTypeUtil {
	
	private static Logger _logger = LoggerFactory.getLogger(ExactMatchesHandler.class);
	
	@Autowired private ConceptCliqueRepository conceptCliqueRepository;
	
	@Autowired private KnowledgeBeaconRegistry registry;

	@Autowired private KnowledgeBeaconService kbs;
	
	@Autowired private ConceptTypeService conceptTypeService;
	
	@Autowired private ConceptCliqueService conceptCliqueService;
	
	@Autowired @Qualifier("Global")
	private Cache cache;

	public ConceptClique getClique(String cliqueId) {
		
		// Check in-memory cache first...
		CacheLocation cacheLocation = 
				cache.searchForEntity( "ConceptClique", cliqueId, new String[]{ cliqueId } );
		
		ConceptClique theClique = (ConceptClique)cacheLocation.getEntity();
		
		if( theClique == null ) {
			
			// ... then check the remote database...
			
			theClique = conceptCliqueRepository.getConceptCliqueById(cliqueId);
			
			if( theClique != null ) {
				
				/*
				 * Patch to ensure that legacy 
				 * Semantic Groups are properly
				 * set coming from the database?
				 */
				String conceptType = theClique.getConceptType();
				
				List<ConceptType> types = 
						conceptTypeService.lookUpByIdentifier(conceptType);

				theClique.setConceptType(curieList(types));
				
				// put fetched result to in-memory cache for future reference?
				cacheLocation.setEntity(theClique);
				
				_logger.info("ConceptClique retrieved from the database and placed in the cache");
				
			} else {
				
				/*
				 *  "silent" failure - probably shouldn't normally happen since 
				 *  all cliqueIds are only published by the beaconAggregator
				 *  for ConceptClique objects normally saved to the database?
				 */
				_logger.info("ConceptClique for clique id '"+
				 cliqueId+"' could NOT be retrieved from the database?");
			}

		} else {
			
			_logger.trace("ConceptClique fetched by cliqueId from cached data");
		}
		
		return theClique; // may still be null
	}
	
	public static boolean notDisjoint(ConceptClique clique1, ConceptClique clique2) {
		return ! Collections.disjoint(clique1.getConceptIds(), clique2.getConceptIds());
	}
	

	public static Set<String> unionOfConceptIds(Collection<ConceptClique> cliques) {
		return cliques.stream().map(
				clique -> { return clique.getConceptIds(); }
		).flatMap(List::stream).collect(Collectors.toSet());
	}

	private <T> List<T> listOfOne(T item) {
		List<T> list = new ArrayList<T>();
		list.add(item);
		return list;
	}
	
	/*
	 * Due diligence saving to avoid Clique duplication in the database
	 */
	private ConceptClique archive( ConceptClique theClique ) {
	
		/*
		 *  Sanity check for an existing node in the database
		 *  with the same accession identifier
		 */
		Long theDbId = theClique.getDbId();
		
		/*
		 * Check if there is an existing clique 
		 * with the same clique id in the database...
		 */		
		ConceptClique dbClique = 
				conceptCliqueRepository.getConceptCliqueById( theClique.getId() );
		
		/*
		 * Conservatively, I only need to merge cliques if 
		 * they are two distinct persisted clique records
		 */
		if( dbClique != null ) {
			conceptCliqueService.mergeConceptCliques( dbClique, theClique );
			
		} else {
			// Just save the clique that was handed into you
			dbClique = theClique ;
		}
		
		/*
		 * Now save whichever clique 
		 * you have, to the database
		 */
		dbClique = conceptCliqueRepository.save(dbClique);
		
		return dbClique;
	}
	

	
	/*
	 * Merge a list of cliques deemed equivalent into one clique.
	 * Purge the old cliques from the database along the way?
	 */
	private ConceptClique mergeCliques(List<ConceptClique> cliques, List<ConceptType> types) {
		
		ConceptClique theClique = cliques.get(0);
		
		if( theClique.getDbId() != null ) {
			conceptCliqueRepository.delete(theClique.getDbId());
			theClique.setDbId(null);
		}
		
		theClique.setConceptType(curieList(types));
		
		for(int i = 1 ; i < cliques.size() ; i++ ) {
			
			ConceptClique other = cliques.get(i);
			
			conceptCliqueService.mergeConceptCliques(theClique,other);
			
			if( other.getDbId() != null ) 
				conceptCliqueRepository.delete(other);
		}
		
		// Refresh the accession identifier
		conceptCliqueService.assignAccessionId(theClique);
		
		return theClique;
	}
	
	private void checkForSymbols(String conceptName, List<ConceptClique> cliques) {
		
		// ignore flanking whitespace, if any...
		conceptName = conceptName.trim();
		
		/*
		 *  Crude filter: 
		 *  the interior of symbol names should 
		 *  not have blanks or commas; 
		 *  Some beacons may violate this (e.g. Biolink?)
		 *  but their beacon wrappers need to fix the issue somehow?
		 */
		if(  
			conceptName.indexOf(" ")!=-1 ||
			conceptName.indexOf(",")!=-1
			
		) return;
		
		/*
		 *  Special deep search for Genes that 
		 *  have names that are potential CURIE object ids
		 *  e.g. HGNC.SYMBOL, GENECARD, etc. symbols?
		 *  TODO: Are there other namespaces to be probed for symbols?
		 */
		for(String prefix : new String[] {"HGNC.SYMBOL","genecards"}) {
			
			String testCurie = prefix+":"+conceptName;
			
			/*
			 *  Shallow search for an existing clique that matches? 
			 *  This may not always work the first time, but once 
			 *  the symbol is registered legitimately by some other 
			 *  clique, then it will likely be merged here.
			 */
			ConceptClique testClique = getClique(testCurie) ;
			
			if( testClique != null) { 
				cliques.add(testClique) ;
			}
		}		
	}
	
	public ConceptClique updateAndRefresh(ConceptClique theClique) {
		// Update the remote database
		theClique = archive(theClique) ;

		// putting fetched result into the in-memory cache
		
		/*
		 *  ... cached by every subclique concept id 
		 *  (may overwrite previously partial cliques cached against an identifier)
		 */
		cache.setMultiCachedEntity( "ConceptClique", theClique.getConceptIds(), theClique ) ;

		// .. cached by resulting clique id...
		CacheLocation cliqueIdCacheLocation = 
				cache.searchForEntity( "ConceptClique", theClique.getId(), new String[]{ theClique.getId() } );

		cliqueIdCacheLocation.setEntity(theClique);			
		
		_logger.debug( "Concept Clique "+theClique.getId()+
				" archived in the database and cached in memory?");
		
		return theClique;
	}
	
	/**
	 * Builds up concept cliques for each conceptId in {@code c}, and then merges them into a single
	 * set of conceptIds and returns this set.
	 * @param string 
	 * @param conceptIds
	 * @param sources 
	 * @return
	 */
	/*
	 *  RMB (Sept 2017) - Revised this function to return a merged ConceptClique object every time.
	 *  TODO: This function should be reviewed for "complete" (once only) clique construction and 
	 *  indexing, that is, the first time *any* conceptId member of the clique is encountered, 
	 *  *all* clique associated concept identifiers should be used to index the resulting ConceptClique
	 */
	public ConceptClique getExactMatches( 
			KnowledgeBeaconImpl beacon, 
			String conceptId, 
			String conceptName,
			List<ConceptType> types, 
			String taxon
	) {

		final String beaconId = beacon.getId();
		
		Boolean updateCache = true ;

		CacheLocation conceptIdsCacheLocation = 
				cache.searchForEntity( "ConceptClique", conceptId, new String[]{conceptId} );

		ConceptClique cacheResult = (ConceptClique)conceptIdsCacheLocation.getEntity() ;

		ConceptClique theClique = null;
		
		List<ConceptClique> cliques = new ArrayList<ConceptClique>();
		
		if( cacheResult != null ) {
				
			theClique = cacheResult;
			
			_logger.debug("Concept Clique fetched by conceptIds from cached data. No additional symbol matches (yet)");

			updateCache = false;
		}
		
		/*
		 * Iterative time-based learning: Re-check if some other 
		 * clique more recently registered a symbol match, 
		 * e.g. GENE symbol; If so, we should (re-)merge the cliques
		 */
		
		 /* 
		  * NOTE: Oddly enough, some beacons such as Biolink return a 
		  * comma-separated list of gene symbols!?!??
		  * 		  * 
		  *	String[] candidates = conceptName.split(",");
		  *	for( String symbol : candidates ) {
		  *		checkForSymbols( symbol, cliques ) ;
		  *	}
		  *
		  * NOV 11, 2017 Observation: 
		  * But, parsing these out may cause more
		  * spurious merges between concepts with names
		  * that contain embedded gene symbols that
		  * are NOT a simple enumeration of symbols?
		  *
		  * MAYBE BETTER TO FIX THE BIOLINK BEACON DIRECTLY
		  * AND ONLY CHECK FOR SINGULAR SYMBOLS IN THE CONCEPT NAME?
		  */
		checkForSymbols( conceptName, cliques ) ;
		
		if( ! cliques.isEmpty() ) {
			
			/*
			 *  Cached result not enough now? 
			 *  Also wanna update with new information...
			 */
			if( theClique != null ) cliques.add( theClique ) ;
			
			updateCache = true; 
		}
			
		if( updateCache ) {
			
			/*  
			 * Try to construct or retrieve additional cliques 
			 * with other criteria from some other location...
			 * 
			 *  First, try the shortcut of checking the 
			 *  special case of whether the given conceptId CURIE
			 *  can be normalized then interpreted as a cliqueId
			 */

			theClique = getClique( CURIE.makeNormalizedCurie(conceptId) ) ;

			if( theClique != null) {
				
				_logger.debug("Existing Concept Clique '"+theClique.getId()+"' fetched from the database?");
				
				cliques.add(theClique); // may still need to merge with fresh symbol data?

			}  else {

				// Otherwise, broaden the search

				// Directly by the CURIE...
				List<Map<String, Object>> l = 
						conceptCliqueRepository.getConceptCliques(listOfOne(conceptId));

				List<String> unmatchedConceptIds = listOfOne(conceptId);

				for (Map<String, Object> m : l) {

					List<String> matchedConceptIds = Arrays.asList((String[]) m.get("matchedConceptIds"));
					ConceptClique clique = (ConceptClique) m.get("clique");

					unmatchedConceptIds.removeAll(matchedConceptIds);

					cliques.add(clique);
				}

				if ( ! unmatchedConceptIds.isEmpty() ) {

					// Attempt additional matches of each singular unmatched concept id...
					List<ConceptClique> foundCliques = 
							unmatchedConceptIds.stream()
							.map( id -> findAggregatedExactMatches( beaconId, id, types ) )
							.collect(Collectors.toList());

					for(ConceptClique clique : foundCliques) {
						unmatchedConceptIds.removeAll(clique.getConceptIds()) ;
					}

					cliques.addAll(foundCliques);
				}

				/*
				 *  If some identifier(s) are still not matched,
				 *  then put them in their own clique?
				 */
				if ( cliques.isEmpty() ) {
					ConceptClique orphanClique = new ConceptClique(curieList(types));
					orphanClique.addConceptIds( beaconId, unmatchedConceptIds );
					conceptCliqueService.assignAccessionId(orphanClique);
					cliques.add(orphanClique);
				}

				if(cliques.isEmpty()) 
					throw new RuntimeException(
							"getExactMatches() ERROR: empty clique impossible here? "
									+ "should have at least an orphan clique?)"
							);
			}
			
			/*
			 * Select the sole clique to process further or 
			 * flatten a set of multiply discovered cliques into one
			 */
			if(cliques.size()==1)
				theClique = cliques.get(0) ;
				
			else
				// Two or more cliques to merge?
				theClique = mergeCliques(cliques,types);

			_logger.debug("Canonical Concept Clique assembled from database and/or beacon information");

			/*
			 * (Re-)add the seed conceptId to the clique, just in case
			 * the original identifier is a letter case variant or it
			 * didn't somehow get added in the various beacon queries.
			 */
			theClique.addConceptId( beacon.getId(), conceptId );
			
			/*
			 * Set the Taxon if not yet known?
			 */
			if( theClique.getTaxon().isEmpty() && !nullOrEmpty(taxon) )
				theClique.setTaxon(taxon);
			
			// fresh the id and semantic group, just in case
			conceptCliqueService.assignAccessionId(theClique); 

			theClique = updateAndRefresh(theClique);
		}

		return theClique;
	}

	/*
	 * Polls all the beacons to find exact matches and aggregate them into a single clique.
	 * The 'sourceBeaconId' is the original authority for the CURIE concept id which seeds the clique assembly.
	 */
	private ConceptClique findAggregatedExactMatches(
			String sourceBeaconId, 
			String conceptId, 
			Boolean testCurie, 
			List<ConceptType> types 
		) {
		
		ConceptClique clique = new ConceptClique(curieList(types));
		
		Set<String> matches = new HashSet<String>() ;
		matches.add(conceptId);
		
		int size;
		
		do {
			size = matches.size();
			
			CompletableFuture<Map<KnowledgeBeaconImpl, List<String>>> future = 
						kbs.getExactMatchesToConceptList( new ArrayList<String>(matches), registry.getBeaconIds() ) ;
			
			try {
				Map<KnowledgeBeaconImpl, List<String>> aggregatedMatches = 
						future.get(
								/*
								 *  Try scaling the timeout up proportionately 
								 *  to the number of concept ids being matched?
								 */
								matches.size()*KnowledgeBeaconService.BEACON_TIMEOUT_DURATION,  
								KnowledgeBeaconService.BEACON_TIMEOUT_UNIT 
						);

				for(KnowledgeBeaconImpl beacon : aggregatedMatches.keySet()) {
					
					List<String> beaconMatches = aggregatedMatches.get(beacon);
					
					/* 
					 * Subtle challenge here: if the beacon reports new matches,
					 * then that implies that it recognized at least one of the
					 * input concept ids, which means that these are also part
					 * of the equivalent concept subclique... but which one of the
					 * input ids was specifically recognized may be obscure?
					 * 
					 * 1.0.14 API needs to be changed to also return the input 
					 * identifiers used to match the new identifiers, i.e. to
					 * truly return the full 'subclique' of identifiers known
					 * by a given beacon.
					 * 
					 */
					// Only record non-empty subcliques for beacons
					if(!nullOrEmpty(beaconMatches) ) {
						clique.addConceptIds( beacon.getId(), beaconMatches );
						matches.addAll(beaconMatches);
					}
				}
				
			} catch (InterruptedException | ExecutionException | TimeoutException e) {
				e.printStackTrace();
			}
			
		} while (size < matches.size());
		
		if(clique.getConceptIds().isEmpty()) {
			
			/*
			 *  If this was just a guess about 
			 *  an equivalent CURIE for the concept
			 *  then ignore
			 */
			if( testCurie ) return null;
		
			/*
			 *  Otherwise, treat as a clique of one: that
			 *  'matches' only has the original conceptId?
			 */
			clique.addConceptIds( sourceBeaconId, listOfOne(conceptId) );
		}
		
		/*
		 *  With available equivalent concept id's gathered, 
		 *  choose the accessionId then return the clique!
		 */
		
		conceptCliqueService.assignAccessionId(clique);
		
		return clique;
	}
	
	// Ordinary search for equivalent concept clique?
	private ConceptClique findAggregatedExactMatches( String sourceBeaconId, String conceptId, List<ConceptType> types ) {
		return findAggregatedExactMatches(  sourceBeaconId, conceptId, false, types ) ;
	}

	/**
	 * 
	 * @param identifiers
	 * @return
	 */
	public ConceptClique getConceptClique(String[] identifiers) {
		return conceptCliqueRepository.getConceptClique(identifiers);
	}

	public ConceptClique[] splitClique(
			ConceptClique theClique, 
			String beaconId, 
			String conceptId, 
			String taxon
	) {
		// Remove from the given clique 
		theClique.removeConceptId(beaconId,conceptId);
		
		// may have to reassess the primary clique id?
		conceptCliqueService.assignAccessionId(theClique);
		
		// Then update the cache and database
		updateAndRefresh(theClique);
		
		/*
		 * Create a new clique with the 
		 * removed concept id as the initial cliqueId
		 * but with identical concept type assumed
		 * and specified taxon
		 */
		ConceptClique newClique = 
				new ConceptClique(conceptId,theClique.getConceptType());
		
		newClique.setTaxon(taxon);
		newClique.addConceptId( beaconId, conceptId );
		newClique = updateAndRefresh(newClique);
		
		return new ConceptClique[] { theClique, newClique };
	}
}
