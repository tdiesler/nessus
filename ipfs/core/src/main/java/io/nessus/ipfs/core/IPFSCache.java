package io.nessus.ipfs.core;

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
    public <T extends AbstractHandle> T put(AbstractHandle fhandle, Class<T> type) {
    	Multihash cid = fhandle.getCid();
        AssertArgument.assertNotNull(cid, "Null cid");
        LOG.debug("Cache put: {}", fhandle);
        return (T) cache.put(cid, fhandle);
    }
    
    @SuppressWarnings("unchecked")
    public <T extends AbstractHandle> T remove(Multihash cid, Class<T> type) {
        LOG.debug("Cache remove: {}", cid);
        return (T) cache.remove(cid);
    }
}