package io.nessus.ipfs;

import java.nio.file.Path;
import java.nio.file.Paths;

import io.ipfs.multihash.Multihash;
import io.nessus.utils.AssertArgument;

public final class CidPath {
	
	private final Multihash cid;
	private final Path path;
	private final String cidSpec;
	
	public CidPath(Multihash cid) {
		this(cid, null);
	}
	
	public CidPath(Multihash cid, Path path) {
		AssertArgument.assertNotNull(cid, "Null cid");
		this.cid = cid;
		this.path = path;
		this.cidSpec = cid.toBase58() + (path != null ? "/" + path : "");
	}
	
	public CidPath append(String apath) {
		AssertArgument.assertNotNull(apath, "Null apath");
		Path xpath = path != null ? path.resolve(apath) : Paths.get(apath);
		return new CidPath(cid, xpath);
	}
	
	public Multihash getCid() {
		return cid;
	}

	public Path getPath() {
		return path;
	}

	public static CidPath parse(String cidPath) {
		CidPath result;
		int idx = cidPath.indexOf('/');
		if (idx > 0) {
			Multihash cid = Multihash.fromBase58(cidPath.substring(0, idx));
			Path path = Paths.get(cidPath.substring(idx));
			result = new CidPath(cid, path);
		} else {
			Multihash cid = Multihash.fromBase58(cidPath);
			result = new CidPath(cid, null);
		}
		return result;
	}
	
	@Override
	public int hashCode() {
		return cidSpec.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof CidPath)) return false;
		CidPath other = (CidPath) obj;
		return cidSpec.equals(other.cidSpec);
	}

	public String toString() {
		return cidSpec;
	}
}