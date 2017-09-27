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
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;

import bio.knowledge.client.impl.ApiClient;

public class GenericKnowledgeService {

	// This works because {@code GenericKnowledgeService} is extended by {@code
	// KnowledgeBeaconService}, which is a Spring service.
	@Autowired
	KnowledgeBeaconRegistry registry;
	
	private Map<String, List<LogEntry>> errorLog = new HashMap<>();
	
	private boolean nullOrEmpty(String str) {
		return str == null || str.isEmpty();
	}
	
	private void clearError(String sessionId) {
		if (nullOrEmpty(sessionId)) return;
		errorLog.put(sessionId, new ArrayList<>());
	}
	
	public void logError(String sessionId, String beacon, String query, String message) {
		if (nullOrEmpty(sessionId)) return;
		
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
				
		for (KnowledgeBeacon beacon : registry.filterKnowledgeBeaconsById(sources)) {
			if (beacon.isEnabled()) {
				ListSupplier<T> supplier = builder.build(beacon.getApiClient());
				CompletableFuture<List<T>> future = CompletableFuture.supplyAsync(supplier);
				futures.add(future);
			}
		}
		
		@SuppressWarnings("unchecked")
		CompletableFuture<List<T>>[] futureArray = futures.toArray(new CompletableFuture[futures.size()]);
		return futureArray;
	}

	
	protected <T> CompletableFuture<Map<KnowledgeBeacon, List<T>>> queryForMap(SupplierBuilder<T> builder, List<String> beacons, String sessionId) {
		CompletableFuture<Map<KnowledgeBeacon, List<T>>> combinedFuture = combineFuturesIntoMap(registry.filterKnowledgeBeaconsById(beacons), query(builder, beacons, sessionId));
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
						System.out.println(c);
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
	
	private <T> CompletableFuture<Map<KnowledgeBeacon, List<T>>> combineFuturesIntoMap(List<KnowledgeBeacon> beacons, CompletableFuture<List<T>>[] futures) {
		return CompletableFuture.allOf(futures).thenApply(x -> {
		
			Map<KnowledgeBeacon, List<T>> combinedResults = new HashMap<>();

			for (int i = 0; i < futures.length; i++) {
				
				KnowledgeBeacon beacon = beacons.get(i);
				CompletableFuture<List<T>> f = futures[i];
				
				List<T> results = f.join();
				if (results != null) {
					combinedResults.put(beacon, results);
				}
			}
			
			return combinedResults;
		}).exceptionally((error) -> {
			
			Map<KnowledgeBeacon, List<T>> combinedResults = new HashMap<>();

			for (int i = 0; i < futures.length; i++) {
				
				KnowledgeBeacon beacon = beacons.get(i);
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
		public abstract ListSupplier<T> build(ApiClient apiClient);
	}
}
