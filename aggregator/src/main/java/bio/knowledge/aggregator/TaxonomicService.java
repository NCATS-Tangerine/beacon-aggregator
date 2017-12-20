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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Helper class for resolving taxonomic data.
 * 
 * @author Richard
 *
 */
@Service
public class TaxonomicService {
	
	private static Logger _logger = LoggerFactory.getLogger(TaxonomicService.class);
			
	public TaxonomicService() { }
	
	private static Map<String,String> taxaById = new HashMap<String,String>();
	
	static {
		
		// Taxonomic identifier conversions
		
		/*
		 *  wd: prefixed ids are WikiData id's
		 *  Here we normalize them to NCBITaxon identifier numbers.
		 *  Would be nice to bulk load this at startup?
		 */
		taxaById.put("wd:Q15978631", "NCBITaxon:9606"); // human
		
	}
	
	public static String lookUpByIdentifier(String identifier) {
		
		if( !(identifier == null || identifier.isEmpty())) {
			if(taxaById.containsKey(identifier)) {
				return taxaById.get(identifier);
			}
		}
		_logger.trace("TaxonomicService.lookUpByIdentifier() - identifier not translated: "+identifier);
		return identifier; // return raw identifier .. maybe needs no conversion?
	}
	
	
	static final Set<String> TaxonTags = new HashSet<String>(); 
	static {
		TaxonTags.add("taxon");   // Biolink?
		TaxonTags.add("wd:p703"); // from Wikidata
	}
	
	public static Boolean containTaxonTag(String tag) {
		return TaxonTags.contains(tag);
	}

}
