package io.nessus.ipfs;

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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import io.ipfs.multihash.Multihash;
import io.nessus.Wallet.Address;
import io.nessus.utils.AssertArgument;

public class FHandle extends AbstractHandle {
    
    final FHandle parent;
    final Path path;
    final URL furl;
    final String secToken;
    
    final List<FHandle> children = new ArrayList<>();
    
    private FHandle(FHandle parent, Address owner, CidPath cid, Path path, URL furl, String secToken, String txId, boolean available, boolean expired, AtomicBoolean scheduled, int attempt, long elapsed) {
    	super(owner, cid, txId, available, expired, scheduled, attempt, elapsed);
        boolean urlBased = path != null && furl != null;
        AssertArgument.assertTrue(urlBased || cid != null, "Neither url nor cid based");
        
        this.parent = parent;
        this.path = path;
        this.furl = furl;
        this.secToken = secToken;
    }
    
	public FHandle getRoot() {
        FHandle result = this;
        while (result.parent != null) {
            result = result.parent;
        }
        return result;
    }

    public FHandle getParent() {
        return parent;
    }

    public List<FHandle> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public FHandle findChild(Path path) {
        AssertArgument.assertNotNull(path, "Null path");
        return findChildRecursive(getRoot(), path);
    }
    
    public boolean hasChildren() {
        return !children.isEmpty();
    }
    
    public URL getURL() {
        return furl;
    }

    public Path getFilePath() {
        if (furl == null || !furl.getProtocol().equals("file")) return null;
        try {
            return Paths.get(URLDecoder.decode(furl.getPath(), "UTF-8"));
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }

    public Path getPath() {
        return path;
    }
    
    public String getSecretToken() {
        return secToken;
    }

    public boolean isEncrypted() {
        return secToken != null;
    }

    private void addChild(FHandle fhchild) {
        Path chpath = fhchild.getPath();
        Multihash chcid = fhchild.getCid();
        AssertArgument.assertTrue(chcid != null || chpath != null, "Neither cid not path in: " + fhchild);
        if (chpath != null) {
            AssertArgument.assertTrue(chpath.startsWith(path) && !chpath.equals(path), "Invalid child path: " + chpath);
            children.forEach(ch -> {
                AssertArgument.assertTrue(!chpath.equals(ch.getPath()), "Duplicate child path: " + chpath);
            });
        } else {
            // Assume that the cid is unique
        }
        children.add(fhchild);
    }
    
    private FHandle findChildRecursive(FHandle fhandle, Path path) {
        
        if (path.equals(fhandle.path)) 
            return fhandle;
        
        for (FHandle aux : fhandle.children) {
            FHandle auxRes = findChildRecursive(aux, path);
            if (auxRes != null) return auxRes;
        }
        
        return null;
    }
    
    public String toString(boolean verbose) {
        if (!verbose) return toString();
        StringWriter sw = new StringWriter();
        recursiveString(new PrintWriter(sw), 0, this);
        return sw.toString().trim();
    }

    private void recursiveString(PrintWriter pw, int indent, FHandle fh) {
        char[] pad = new char[indent];
        Arrays.fill(pad, ' ');
        pw.println(new String(pad) + fh);
        if (fh.hasChildren()) {
            fh.children.forEach(ch -> recursiveString(pw, indent + 3, ch));
        }
    }

    @Override
    public String toString() {
        String addr = owner.getAddress();
    	return String.format("[addr=%s, cid=%s, path=%s, avl=%d, exp=%d, try=%d, time=%s]", 
    			addr, cid, path, available ? 1 : 0, expired ? 1 : 0, attempt, elapsed);
    }

    public static class FHReference {
        
        private FHandle fhref;

        public FHReference() {
        }

        public FHReference(FHandle fh) {
            this.fhref = fh;
        }

        public synchronized FHandle getFHandle() {
            return fhref;
        }

        public synchronized void setFHandle(FHandle fh) {
            this.fhref = fh;
        }
        
        @Override
        public synchronized String toString() {
            return fhref.toString();
        }
    }
    
    public static class FHWalker {
    	
        public interface Visitor {
            FHandle visit(FHandle fhandle) throws IOException, GeneralSecurityException;
        }
        
        public static FHandle walkTree (FHandle fhandle, Visitor visitor) throws IOException, GeneralSecurityException {
            FHandle fhroot = fhandle.getRoot();
            FHReference fhref = new FHReference(fhroot);
            walkTreeRecursive(fhref, fhroot.getPath(), visitor);
            return fhref.getFHandle();
        }
        
        private static boolean walkTreeRecursive(FHReference fhref, Path path, Visitor visitor) throws IOException, GeneralSecurityException {
            
            FHandle fhroot = fhref.getFHandle();
            FHandle fhchild = fhroot.findChild(path);
            
            FHandle fhres = visitor.visit(fhchild);
            boolean success = fhres != null;
            
            if (success) {
                fhref.setFHandle(fhres.getRoot());
                for (FHandle aux : fhres.getChildren()) {
                    success = walkTreeRecursive(fhref, aux.getPath(), visitor);
                    if (!success) break;
                }
            }
            
            return success;
        }
    }
    
    public static class FHBuilder extends AbstractBuilder<FHBuilder, FHandle> {
        
        private FHandle parent;
        private Path path;
        private URL furl;
        private String secToken;
        private boolean available;
        
        private FHBuilder parentBuilder;
        private Map<Path, FHBuilder> childBuilders = new LinkedHashMap<>();
        
        public FHBuilder(FHandle fhandle) {
        	super(fhandle);
            AssertArgument.assertTrue(fhandle.parent == null, "Cannot rebuild partial trees");
            init(null, fhandle);
        }

        public FHBuilder(Address owner, Path path, URL furl) {
        	super(owner);
            AssertArgument.assertNotNull(path, "Null path");
            AssertArgument.assertNotNull(furl, "Null furl");
            this.path = path;
            this.furl = furl;
        }

        public FHBuilder(Address owner, String txId, Multihash cid) {
        	super(owner, txId, cid);
        }

        private FHBuilder(FHBuilder parentBuilder, FHandle fhandle) {
        	super(fhandle);
            init(parentBuilder, fhandle);
        }

        private void init(FHBuilder parentBuilder, FHandle fhandle) {
            this.parentBuilder = parentBuilder;
            this.owner = fhandle.owner;
            this.cid = fhandle.cid;
            this.path = fhandle.path;
            this.furl = fhandle.furl;
            this.txId = fhandle.txId;
            this.secToken = fhandle.secToken;
            this.available = fhandle.available;
            this.expired = fhandle.expired;
            this.scheduled = fhandle.scheduled;
            this.attempt = fhandle.attempt;
            this.elapsed = fhandle.elapsed;
            
            fhandle.children.forEach(ch -> { 
                FHBuilder cbuilder = new FHBuilder(this, ch);
                childBuilders.put(ch.getPath(), cbuilder); 
            });
        }

        public FHBuilder rootBuilder() {
            FHBuilder rootBuilder = this;
            while (rootBuilder.parentBuilder != null) {
                rootBuilder = rootBuilder.parentBuilder;
            }
            return rootBuilder;
        }

        public FHBuilder findChild(Path path) {
            AssertArgument.assertNotNull(path, "Null path");
            return findChildRecursive(rootBuilder(), path);
        }

        public FHBuilder findChild(Multihash cid) {
            AssertArgument.assertNotNull(cid, "Null cid");
            return findChildRecursive(rootBuilder(), new CidPath(cid));
        }
        
        FHBuilder findChildRecursive(FHBuilder builder, Path path) {
            
            if (path.equals(builder.path)) 
                return builder;
            
            for (FHBuilder aux : builder.childBuilders.values()) {
                FHBuilder auxRes = findChildRecursive(aux, path);
                if (auxRes != null) return auxRes;
            }
            
            return null;
        }

        FHBuilder findChildRecursive(FHBuilder builder, CidPath cid) {
            
            if (cid.equals(builder.cid)) 
                return builder;
            
            for (FHBuilder aux : builder.childBuilders.values()) {
                FHBuilder auxRes = findChildRecursive(aux, cid);
                if (auxRes != null) return auxRes;
            }
            
            return null;
        }

        public FHBuilder parent(FHandle parent) {
            this.parent = parent;
            return this;
        }

        public FHBuilder path(Path path) {
            this.path = path;
            return this;
        }
        
        public FHBuilder url(URL furl) {
            this.furl = furl;
            return this;
        }
        
        public FHBuilder secretToken(String secToken) {
            this.secToken = secToken;
            return this;
        }
        
        public FHBuilder available(boolean available) {
            this.available = available;
            return this;
        }

        @Override
        public FHandle build() {
            
            if (parentBuilder == null)
                return buildInternal();
            
            FHBuilder rootBuilder = rootBuilder(); 
            FHandle rootHandle = rootBuilder.buildInternal();
            FHandle childHandle = rootHandle.findChild(path);
            
            return childHandle;
        }
        
        private FHandle buildInternal() {
        	
        	if (parent != null) {
        		FHandle rootHandle = parent.getRoot();
				Multihash rootCid = rootHandle.getCid();
        		if (rootCid != null) {
        	    	Path rootPath = rootHandle.getPath();
        	    	Path relpath = rootPath.relativize(path);
        			cid = new CidPath(rootCid, relpath);
        		}
        	}
        	
            FHandle fhandle = new FHandle(parent, owner, cid, path, furl, secToken, txId, available, expired, scheduled, attempt, elapsed);
            childBuilders.values().stream().map(cb -> cb.parent(fhandle).buildInternal()).collect(Collectors.toList());
            if (parent != null) parent.addChild(fhandle);
            
            return fhandle;
        }
    }
}
