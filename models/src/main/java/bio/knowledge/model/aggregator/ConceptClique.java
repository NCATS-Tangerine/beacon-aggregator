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
package bio.knowledge.model.aggregator;

import java.util.ArrayList;
import java.util.List;

import org.neo4j.ogm.annotation.NodeEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bio.knowledge.Util;
import bio.knowledge.model.DomainModelException;
import bio.knowledge.model.core.neo4j.Neo4jAbstractIdentifiedEntity;
import bio.knowledge.model.umls.Category;

/**
 * This version of ConceptClique stores beacon subcliques 
 * as a pair of List objects: a list of (case sensitive)
 * exact match concept ids and a corresponding list of
 * strings encoding a comma separated list of beaconIds, 
 * where the identical index position in each list, links
 * the list of beaconids with their corresponding matched
 * concept id (which they recognize exactly)
 * 
 * @author Richard
 *
 */
@NodeEntity(label="ConceptClique")
public class ConceptClique extends Neo4jAbstractIdentifiedEntity implements Util {
	
	private static Logger _logger = LoggerFactory.getLogger(ConceptClique.class);
	
	// delimiter of conceptIds in beacon subcliques
	private static final String QDELIMITER = ";";
	
	/* Translator concept semantic data type
	 * Could be a UMLS Semantic Group, e.g. umls_sg:GENE
	 * or some other more comprehensive
	 * Translator data type ontology term.
	 */
	private String conceptType = Category.DEFAULT_SEMANTIC_GROUP ;

	/* NCBI Taxon Identifier */
	private String taxon = "" ;

	/**
	 * 
	 */
	public ConceptClique() { }
	
	/**
	 * 
	 * @param conceptType
	 */
	public ConceptClique(String conceptType) {
		this.conceptType = conceptType;
	}
	
	/**
	 * 
	 * @param conceptType
	 */
	public ConceptClique(String cliqueId, String conceptType) {
		super(cliqueId,cliqueId,cliqueId+" Clique of type "+conceptType);
		this.conceptType = conceptType;
	}
	
	/**
	 * 
	 * @param primary taxon associated with the Clique
	 */
	public void setTaxon(String taxon) {
		this.taxon = taxon;
	}
	
	/**
	 * 
	 * @return primary taxon associated with the Clique
	 */
	public String getTaxon() {
		return taxon;
	}
	
	/**
	 * 
	 * @param conceptType
	 */
	public void setConceptType(String conceptType) {
		this.conceptType = conceptType;
	}
	
	/**
	 * 
	 * @return CURIE of Concept semantic type for this clique
	 */
	public String getConceptType() {
		return conceptType;
	}
	
	/*
	 * Master list of all identifiers recorded in this clique.
	 * Original concept identifier letter case varians is 
	 * preserved to ease their exact match use to recover 
	 * associated concepts in the beacons which use those variants. 
	 * Thus, duplication of identifiers in the master list
	 * (when viewed from a case insensitive vantage point) 
	 * is not avoided.
	 */
	private List<String> conceptIds  = new ArrayList<String>();
	
	/*
	 * A list of strings encoding a comma-separated list of
	 * integer indices into the master list of concept identifiers, 
	 * corresponding to each beacon which knows about that
	 * subclique of equivalent identifiers from the master list.
	 * 
	 * The index position of each entry in the list is
	 * guaranteed to correspond to the the beacon aggregator
	 * assigned beacon id for a given beacon.
	 */
	private List<String> beaconSubcliques = new ArrayList<String>();
	
	@Override
	public String getName() {
		String name = super.getName();
		if( nullOrEmpty(name))
			return getId();
		return super.getName();
	}
	
	/**
	 * 
	 * @param beaconId of the beacon that asserts the equivalence of these concept identifiers
	 * @param conceptIds concept identifiers to be added
	 * @throws DomainModelException if the beaconId is null or empty
	 */
	public void addConceptIds( String beaconId, List<String> subclique ) {
		
		if( nullOrEmpty(beaconId) ) 
			throw new DomainModelException("ConceptClique() ERROR: null or empty beacon id?");
		
		for(String id : subclique ) {
			int cid = 0;
			if(!conceptIds.contains(id)) {
				conceptIds.add(id);
			}
			cid = conceptIds.indexOf(id);
			
			addToSubClique(beaconId,cid);
		}
	}
	
	/**
	 * 
	 * @param beaconId
	 * @param conceptId
	 */
	public void addConceptId( String beaconId, String conceptId ) {
		
		if( nullOrEmpty(beaconId) ) 
			throw new DomainModelException("ConceptClique() ERROR: null or empty beacon id?");
		
		if(!conceptIds.contains(conceptId)) {
			conceptIds.add(conceptId);
		}
		
		int cid = conceptIds.indexOf(conceptId);
		addToSubClique(beaconId,cid);
	}

	public void removeConceptId(String beaconId, String conceptId) {
		
		if( nullOrEmpty(beaconId) ) 
			throw new DomainModelException("ConceptClique() ERROR: null or empty beacon id?");
		
		int cid = conceptIds.indexOf(conceptId);
		
		if(cid>=0) {
			conceptIds.set(cid, ""); // empty string safer than null!
		}
		
		removeFromSubClique(beaconId,cid);
	}
	
	/*
	 * Add a concept integer index to a beacon subclique
	 */
	private void addToSubClique(String beaconId, Integer cid) {
		List<Integer> subclique = getBeaconSubClique(beaconId) ;
		if(! subclique.contains(cid) ) subclique.add(cid);
		setBeaconSubClique(beaconId,subclique);
	}

	private void removeFromSubClique(String beaconId, int cid) {
		List<Integer> subclique = getBeaconSubClique(beaconId) ;
		if( subclique.contains(cid) ) subclique.remove(cid);
		setBeaconSubClique(beaconId,subclique);
	}

	/*
	 * Private accessor to internal beacon subclique data structure, which expands as needed by beacons provided
	 */
	private List<String> _beaconSubcliques(Integer bid) {
		
		/*
		 *  Expand the beaconSubclique master list 
		 *  if necessary (probably initially then rarely)
		 */
		if( bid >= beaconSubcliques.size()) {
			
			_logger.warn("setBeaconSubClique(): expanding the subcliques array for beaconId '"+bid+"'");
			
			/*
			 * Allocate a larger beaconSubcliques list 
			 * size then copy over the old subcliques
			 */
			List<String> bigger = new ArrayList<String>(bid+1);
			for( Integer i=0 ; i <= bid ; i++ ) {
				// Add empty strings for every beacon...
				bigger.add("");
				if(i<beaconSubcliques.size())
					/*
					 * ... but overwrite the entry with 
					 * the current values of the old subclique 
					 * (may still be empty?)
					 */
					bigger.set(i, beaconSubcliques.get(i));
			}
			beaconSubcliques = bigger;
		}
		
		return beaconSubcliques;
	}

	/*
	 * Access and convert an internal String representation of a beacon subclique, 
	 * into a List of integer indices into the master list of concept identifiers. 
	 * I don't do much explicit type checking here since I (hope to) completely 
	 * control the data integrity of the internal data structure.
	 */ 
	private void setBeaconSubClique(String beaconId, List<Integer> subclique) {
		
		/*
		 *  This had better be a non-null valid beaconId
		 *  otherwise a numeric exception will be thrown!
		 */
		Integer bid = new Integer(beaconId);

		// Rebuild the beacon subclique entry
		String entry = "";
		for(Integer cid : subclique)
			if(entry.isEmpty())
				entry = cid.toString();
			else
				entry += QDELIMITER+cid.toString();
		
		// Reset the beacon subclique to the new entry
		_beaconSubcliques(bid).set(bid,entry);
	}

	/* 
	 * @param beaconId
	 * @return current list of subclique concept ids encoded as a List of integer indices into the master concept id list
	 */
	private List<Integer> getBeaconSubClique(String beaconId) {
		
		/*
		 *  This had better be a non-null valid beaconId
		 *  otherwise a numeric exception will be thrown!
		 */
		Integer bid = new Integer(beaconId);
		
		String entry = _beaconSubcliques(bid).get(bid);
		
		List<Integer> subclique = new ArrayList<Integer>();
		
		if(!entry.isEmpty()) {
			String[] cids = entry.split(QDELIMITER);
			for(String cid : cids) 
				subclique.add(new Integer(cid));
		}
		
		return subclique;
	}
	
	/**
	 * @param beaconId
	 * @return the list of identifiers of concepts deemed equivalent by a specified beacon.
	 */
	public Boolean hasConceptIds(String beaconId) {
		/*
		 *  This had better be a non-null valid beaconId
		 *  otherwise a numeric exception will be thrown!
		 */
		Integer bid = new Integer(beaconId);
		
		String entry = _beaconSubcliques(bid).get(bid);
		
		return !entry.isEmpty() ;
	}
	
	/**
	 * @param beaconId
	 * @return the list of identifiers of concepts deemed equivalent by a specified beacon.
	 */
	public List<String> getConceptIds(String beaconId) {
		List<Integer> subclique = getBeaconSubClique(beaconId);
		return getConceptIds(subclique);
	}
	
	/*
	 * We construct the conceptId list for a given identifier subclique dynamically on the fly
	 * @param subclique list of integer indices to entries in the master List of concept identifiers
	 */
	private List<String> getConceptIds(List<Integer> subclique) {
		List<String> cids = new ArrayList<String>();
		for(Integer cidx : subclique)
			cids.add(conceptIds.get(cidx));
		return cids; // Note: will be empty if the subclique indice list is empty
	}

	/**
	 * This function should not normally be directly called by the code 
	 * except during loading of a well formed ConceptClique record from the database
	 * 
	 * @return set the master list of (exact match, case sensitive) identifiers of all concepts deemed equivalent in this clique.
	 */
	public void setConceptIds(List<String> cids) {
		conceptIds = cids;
	}
	
	/**
	 * 
	 * @return the master list of (exact match, case sensitive) identifiers of all concepts deemed equivalent in this clique.
	 */
	public List<String> getConceptIds() {
		return new ArrayList<String>(conceptIds);
	}
	
	/**
	 * This function should not normally be directly called by the code 
	 * except during loading of a well formed ConceptClique record from the database
	 * 
	 * @return set the master list of beacon subcliques (see above)
	 */
	public void setBeaconSubcliques(List<String> subcliques) {
		this.beaconSubcliques = subcliques;
	}
	
	/**
	 * This function should not normally be directly called by the code 
	 * except during the saving of a well formed ConceptClique record to the database
	 * 
	 * @return get the master list of beacon subcliques (see above)
	 */
	public List<String> getBeaconSubcliques() {
		return new ArrayList<String>(beaconSubcliques);
	}


}
