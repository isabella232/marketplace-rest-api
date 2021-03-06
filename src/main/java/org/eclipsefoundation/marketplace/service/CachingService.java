/* Copyright (c) 2019 Eclipse Foundation and others.
 * This program and the accompanying materials are made available
 * under the terms of the Eclipse Public License 2.0
 * which is available at http://www.eclipse.org/legal/epl-v20.html,
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipsefoundation.marketplace.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.commons.lang3.StringUtils;
import org.eclipsefoundation.marketplace.model.RequestWrapper;

/**
 * Interface defining the caching service to be used within the application.
 * 
 * @author Martin Lowe
 * @param <T> the type of object to be stored in the cache.
 */
public interface CachingService<T> {

	/**
	 * Returns an Optional object of type T, returning a cached object if available,
	 * otherwise using the callable to generate a value to be stored in the cache
	 * and returned.
	 * 
	 * @param id       the ID of the object to be stored in cache
	 * @param wrapper  the query parameters for the current request
	 * @param params   parameters to use in place of wrapper parameters when set
	 * @param callable a runnable that returns an object of type T
	 * @return the cached result
	 */
	Optional<T> get(String id, RequestWrapper wrapper, Map<String, List<String>> params,
			Callable<? extends T> callable);

	/**
	 * Returns the expiration date in millis since epoch.
	 * 
	 * @param id     the ID of the object to be stored in cache
	 * @param params the query parameters for the current request
	 * @return an Optional expiration date for the current object if its set. If
	 *         there is no underlying data, then empty would be returned
	 */
	Optional<Long> getExpiration(String id, RequestWrapper params);

	/**
	 * @return the max age of cache entries
	 */
	long getMaxAge();

	/**
	 * Retrieves a set of cache keys available to the current cache.
	 * 
	 * @return unmodifiable set of cache entry keys.
	 */
	Set<String> getCacheKeys();

	/**
	 * Removes cache entry for given cache entry key.
	 * 
	 * @param key cache entry key
	 */
	void remove(String key);

	/**
	 * Removes all cache entries.
	 */
	void removeAll();

	/**
	 * Generates a unique key based on the id of the item/set of items to be stored,
	 * as well as any passed parameters.
	 * 
	 * @param id      identity string of the item to cache
	 * @param wrapper parameters associated with the request for information
	 * @param params  parameters to use in place of wrapper parameters when set
	 * @return the unique cache key for the request.
	 */
	default String getCacheKey(String id, RequestWrapper wrapper, Map<String, List<String>> params) {
		StringBuilder sb = new StringBuilder();
		sb.append('[').append(wrapper.getEndpoint()).append(']');
		sb.append("id:").append(id);

		// get the used set of parameters for filtering data
		Map<String, List<String>> actual = params == null ? wrapper.asMap() : params;
		// join all the non-empty params to the key to create distinct entries for
		// filtered values
		actual.entrySet().stream().filter(e -> !e.getValue().isEmpty())
				.map(e -> e.getKey() + '=' + StringUtils.join(e.getValue(), ','))
				.forEach(s -> sb.append('|').append(s));

		return sb.toString();
	}
}
