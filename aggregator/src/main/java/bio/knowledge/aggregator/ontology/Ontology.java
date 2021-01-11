package bio.knowledge.aggregator.ontology;

import bio.knowledge.aggregator.KnowledgeBeaconImpl;
import bio.knowledge.aggregator.KnowledgeBeaconRegistry;
import bio.knowledge.ontology.BiolinkClass;
import bio.knowledge.ontology.BiolinkEntity;
import bio.knowledge.ontology.BiolinkSlot;
import bio.knowledge.ontology.mapping.ModelLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Function;

@Component
public class Ontology extends bio.knowledge.ontology.Ontology {
	
	private static final Logger _logger = LoggerFactory.getLogger(Ontology.class);
	
	@Autowired KnowledgeBeaconRegistry registry;
	
	/**
	 * Returns an Optional wrapped Biolink class for a given Beacon (by id) and Biolink category.
	 *
	 * @param beaconId identifier of a beacon
	 * @param category from Biolink
	 * @return Optional<BiolinkClass>
	 */
	@SuppressWarnings("unchecked")
	public Optional<BiolinkClass> lookUpCategoryByBeacon(
			Integer beaconId, 
			String category
	) {
		return (Optional<BiolinkClass>)lookUpByBeacon(beaconId,category,getClassLookup());
	}

	/* *
	 *
	 *  UNUSED FUNCTION?
	 *
	 * Returns an Optional wrapped Biolink slot
	 * for a given Beacon (by id) and Biolink predicate.
	 *
	 * @param beaconId identifier of a beacon
	 * @param predicate from Biolink
	 * @return Optional<BiolinkSlot>
	 * /
	@SuppressWarnings("unchecked")
	public Optional<BiolinkSlot> lookUpPredicateByBeacon(
			Integer beaconId,
			String predicate
	) {
		return (Optional<BiolinkSlot>)lookUpByBeacon(beaconId,predicate,getSlotLookup());
	}
	*/

	/**
	 * Returns an Optional wrapped Biolink Entity (class or slot)
	 * for a given Beacon (by id) and CURIE of an mapped ontology
	 *
	 * @param beaconId identifier of a beacon
	 * @param termId CURIE of an mapped ontology
	 * @return Optional<? extends BiolinkEntity>
	 */
	public Optional<? extends BiolinkEntity> lookUpByBeacon(
			Integer beaconId, 
			String termId,  
			ModelLookup modelLookup
	) {
		KnowledgeBeaconImpl beacon = registry.getBeaconById(beaconId);
		return getMapping( beacon.getUrl(), termId, modelLookup );
	}
	
	/**
	 * Returns the Biolink class object for category, from
	 * a given Beacon (by id), for a given id and/or category name.
	 *
	 * @param beaconId identifier of a beacon
	 * @param id of the category
	 * @param category from Biolink
	 * @return BiolinkClass of a concept category
	 */
	public BiolinkClass lookupCategory( Integer beaconId, String id, String category ) {
		
		BiolinkClass biolinkClass = 
				(BiolinkClass)lookupTerm( 
						beaconId, 
						id, 
						category,
						getClassLookup(),
						this::getClassByName
		) ;
		
		if(biolinkClass==null) {
			
			_logger.warn("Ontology.lookupCategory(category: '"+category+"') has no Biolink Mapping?");

			/*
			 * Not all beacon concept types will 
			 * already be mapped onto Biolink
			 * so we'll tag such types to "NAME_TYPE"
			 */
			biolinkClass = getDefaultCategory();
		}
		
		return biolinkClass;
	}
	
	/**
	 * Returns the Biolink slot object for predicate, from
	 * a given Beacon (by id), for a given id and/or predicate name.
	 *
	 * @param beaconId identifier of a beacon
	 * @param id of the category
	 * @param predicate from Biolink
	 * @return BiolinkSlot of a predicate slot
	 */
	public BiolinkSlot lookupPredicate( Integer beaconId, String id, String predicate ) {
		
		BiolinkSlot biolinkSlot = 
				(BiolinkSlot)lookupTerm( 
						beaconId, 
						id, 
						predicate,
						getSlotLookup(),
						this::getSlotByName
		) ;
		
		if( biolinkSlot == null) {
			
			_logger.warn("Ontology.lookupPredicate(): predicate '"+predicate+"' with id '"+id+"', from beacon '"+beaconId+"' has no Biolink Mapping?");

			/*
			 * Since the Translator community are still
			 * debating the "canonical" version of predicates 
			 * and how to encode them, we will, for now
			 * not reject "missing" predicates but rather
			 * just propagate them directly through.
			 */
			biolinkSlot = getDefaultPredicate();
		}
		
		return biolinkSlot;
	}
	
	/*
	 * Shared code for two Biolink Model ontology lookups above
	 */
	private BiolinkEntity lookupTerm(
			Integer beaconId,
			String id,
			String term,  
			ModelLookup modelLookup,
			Function<String,Optional<? extends BiolinkEntity>> lookupByName
	) {
		
		BiolinkEntity biolinkTerm = null;
		
		Optional<? extends BiolinkEntity> optionalBiolinkTerm = lookupByName.apply( term );
		
		if( ! optionalBiolinkTerm.isPresent()) {
			optionalBiolinkTerm = lookUpByBeacon( beaconId, id, modelLookup );
		}

		if(optionalBiolinkTerm.isPresent())
			biolinkTerm = optionalBiolinkTerm.get();
		
		return biolinkTerm;
	}
}
