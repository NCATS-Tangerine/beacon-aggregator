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

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import bio.knowledge.aggregator.StatementsQueryInterface;
import bio.knowledge.client.model.BeaconStatement;
import bio.knowledge.model.aggregator.ConceptClique;
import bio.knowledge.server.controller.ExactMatchesHandler;
import bio.knowledge.server.model.ServerStatement;
import bio.knowledge.server.model.ServerStatementsQuery;
import bio.knowledge.server.model.ServerStatementsQueryBeaconStatus;
import bio.knowledge.server.model.ServerStatementsQueryResult;
import bio.knowledge.server.model.ServerStatementsQueryStatus;

/**
 * @author richard
 *
 */
public class StatementsQuery 
		extends AbstractQuery<
					StatementsQueryInterface,
					BeaconStatement,
					ServerStatement
				> 
		implements StatementsQueryInterface
{

	private final ServerStatementsQuery query;
	private final ServerStatementsQueryStatus status;
	private final ServerStatementsQueryResult results;
	
	public StatementsQuery(
			BeaconHarvestService beaconHarvestService, 
			StatementsDatabaseInterface statementsDatabaseInterface
	) {
		super(beaconHarvestService,statementsDatabaseInterface);
		
		query = new ServerStatementsQuery();
		query.setQueryId(getQueryId());
		
		status = new ServerStatementsQueryStatus();
		status.setQueryId(getQueryId());
		
		results = new ServerStatementsQueryResult();
		results.setQueryId(getQueryId());
	}
	
	public ServerStatementsQuery getQuery(
			String source, List<String> relations, String target, 
			String keywords, List<String> conceptTypes,
			List<Integer> beacons
	) {
		
		query.setSource(source);
		query.setRelations(relations);
		query.setTarget(target);
		query.setKeywords(keywords);
		query.setTypes(conceptTypes);
		
		setQueryBeacons(beacons);
		
		getHarvestService().initiateBeaconHarvest(this);	
		
		return query;
	}
	
	/**
	 * 
	 */
	@Override
	public StatementsQueryInterface getQuery() {
		return (StatementsQueryInterface)this;
	}

	/**
	 * 
	 */
	public String getSource() {
		return query.getSource();
	}

	/**
	 * 
	 */
	public List<String> getRelations() {
		return query.getRelations();
	}

	/**
	 * 
	 */
	public String getTarget() {
		return query.getTarget();
	}

	/**
	 * 
	 */
	public String getKeywords() {
		return query.getKeywords();
	}

	/**
	 * 
	 */
	public List<String> getConceptTypes() {
		return query.getTypes();
	}


	/**
	 * Returns a query string for a 'Statements' Query harvesting
	 */
	@Override
	public String makeQueryString() {
		return makeQueryString("concepts",getSource(),getRelations(),getTarget(),getKeywords(),getConceptTypes());
	}
	
	/**
	 * 
	 */
	@Override
	protected BeaconStatusInterface createBeaconStatus(Integer beacon) {
		return new ServerStatementsQueryBeaconStatus();
	}

	public ServerStatementsQueryStatus getQueryStatus( List<Integer> beacons ) {

		if(nullOrEmpty(beacons))
			beacons = getQueryBeacons(); // retrieve all beacons if not filtered?
		
		@SuppressWarnings({ "rawtypes", "unchecked" })
		List<BeaconStatusInterface> bsList = 
				(List<BeaconStatusInterface>)(List)status.getStatus();
		bsList.clear();
		
		for( Integer beacon : beacons ) {
			Optional<BeaconStatusInterface> beaconStatus = getBeaconStatus(beacon);
			if(beaconStatus.isPresent())
				bsList.add(beaconStatus.get());
		}
		
		return status;
	}
	
	public ServerStatementsQueryResult getQueryResults(Integer pageNumber, Integer pageSize, List<Integer> beacons) {
		
		// Seems redundant, but...
		setPageNumber(pageNumber);
		setPageSize(pageSize);
		
		// ...Also need to also set the Server DTO sent back
		results.setPageNumber(pageNumber);
		results.setPageSize(pageSize);

		List<ServerStatement> statements = 
				getDatabaseInterface().getDataPage(this,beacons);
		
		results.setResults(statements);
		
		return results;
	}
	

	/*
	 * (non-Javadoc)
	 * @see bio.knowledge.server.blackboard.AbstractQuery#getQueryResultSupplier(java.lang.Integer)
	 */
	@Override
	public Supplier<Integer> getQueryResultSupplier(Integer beacon) {
		return ()->queryBeaconForStatements(beacon);
	}
	
	/*
	 * This method will access the given beacon, 
	 * in a blocking fashion, within the above 
	 * asynchronoous ComputableFuture. Once the
	 * beacon returns its data, this method also 
	 * loads it into the database, then returns 
	 * the list(?).
	 * 
	 * @param beacon
	 * @return
	 */
	private Integer queryBeaconForStatements(Integer beacon) {
		
		BeaconHarvestService bhs = getHarvestService() ;
		ExactMatchesHandler emh = bhs.getExactMatchesHandler();
		
		String source = getSource();
		ConceptClique sourceClique = emh.getClique(source);
		if(sourceClique==null) {
			severeError("queryBeaconForStatements(): source clique '"+source+"' could not be found?") ;
		}

		String target = getTarget();
		ConceptClique targetClique = null;
		if(!target.isEmpty()) {
			targetClique = emh.getClique(target);
			if(targetClique==null) {
				severeError("queryBeaconForStatements(): target clique '"+target+"' could not be found?") ;
			}
		}

		// The legacy Beacon PAI 1.0.17 still has space-delimited relations...
		String relations =  String.join(" ", getRelations()).trim();
		
		// empty relations argument should be set to null here!
		relations = relations.isEmpty()?null:relations; 

		// Deal with empty keywords filters here...
		String keywords =  String.join(" ", getKeywords()).trim();
		
		// empty keywords should be set to null here!
		keywords = keywords.isEmpty()?null:keywords; 

		// The legacy Beacon PAI 1.0.17 still has space-delimited concept types...
		String conceptTypes =  String.join(" ", getConceptTypes()).trim();
		
		// empty concept types should be set to null here!
		conceptTypes = conceptTypes.isEmpty()?null:conceptTypes; 
				
		// Call Beacon
		List<BeaconStatement> results =
				bhs.getKnowledgeBeaconService().
					getStatements(
						sourceClique,
						relations,
						targetClique,
						
						keywords,
						conceptTypes,
						
						/*
						 *  TODO: Abandon data paging at the level of Beacon harvests; 
						 *  replace with a default batch size
						 *  For now, until the Beacon API formalizes this idea, 
						 *  we'll fake things with a huge DEFAULT pageSize request for pageNumber 1
						 *  A tacit assumption is made (should be documented in the API) that
						 *  results will be returned in order of relevance to the submitted query.
						 *  A query may, of course, return fewer items than the default pageSize.
						 */
						1, // getPageNumber(), 
						DEFAULT_BEACON_QUERY_SIZE, // getPageSize(), 
						
						beacon
					);
	
		// Load BeaconStatement results into the blackboard database
		StatementsDatabaseInterface dbi = 
				(StatementsDatabaseInterface)getDatabaseInterface();
		
		dbi.loadData(this,results,beacon);

		return results.size();
	}
	


}