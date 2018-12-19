package io.nessus.ipfs.core;

/*-
 * #%L
 * Nessus :: IPFS
 * %%
 * Copyright (C) 2018 Nessus
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ipfs.multihash.Multihash;
import io.nessus.ipfs.AbstractHandle;
import io.nessus.utils.AssertArgument;

/**
 * A cache that stores IPFS content handles keyed by content id.
 * 
 * Content handles are added to the cache when their respectiv
 * content id is discovered on the blockchain. At this point,
 * the handle is not yet fully available and marked as such.
 * 
 * An asynchromous process, then tries to get the actual content
 * from IPFS and initalises the remaining properties.
 * 
 * FHandle: relative path
 * AHandle: public key
 */
public class IPFSCache {
    
    static final Logger LOG = LoggerFactory.getLogger(IPFSCache.class);

    private final Map<Multihash, AbstractHandle> cache = Collections.synchronizedMap(new LinkedHashMap<>());

	public <T extends AbstractHandle> Set<Multihash> keySet(Class<T> type) {
		
    	Set<Multihash> result = cache.keySet();
    	if (type == null) return result;
    	
    	result = result.stream()
    			.filter(k -> type.isAssignableFrom(cache.get(k).getClass()))
    			.collect(Collectors.toSet());
    	
    	return result;
    }
    
    public void clear() {
        cache.clear();
    }

    @SuppressWarnings("unchecked")
    public <T extends AbstractHandle> T get(Multihash cid, Class<T> type) {
		return (T) cache.get(cid);
    }
    
    @SuppressWarnings("unchecked")
    public <T extends AbstractHandle> T put(AbstractHandle ahandle) {
        AssertArgument.assertNotNull(ahandle.getOwner(), "Null owner");
        AssertArgument.assertNotNull(ahandle.getCid(), "Null cid");
        AssertArgument.assertNotNull(ahandle.getTxId(), "Null txId");
        LOG.debug("Cache put: {}", ahandle);
        return (T) cache.put(ahandle.getCid(), ahandle);
    }
    
    @SuppressWarnings("unchecked")
    public <T extends AbstractHandle> T remove(Multihash cid, Class<T> type) {
        LOG.debug("Cache remove: {}", cid);
        return (T) cache.remove(cid);
    }
}