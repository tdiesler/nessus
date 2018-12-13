package io.nessus.ipfs.core;

import java.nio.ByteBuffer;
import java.util.Arrays;

import io.ipfs.multihash.Multihash;
import io.nessus.ipfs.FHandle;

class BCData {
    
    static final byte OP_ADDR_DATA = 0x10;
    static final byte OP_FILE_DATA = 0x20;
    static final byte OP_RETURN = 0x6A; 
    
    final String OP_PREFIX;
    
    BCData(FHeaderValues fhid) {
        OP_PREFIX = fhid.PREFIX;
    }

    byte[] createAddrData(Multihash cid) {
        byte[] bytes = cid.toBytes();
        return buffer(OP_ADDR_DATA, bytes.length + 1).put((byte) bytes.length).put(bytes).array();
    }

    Multihash extractAddrData(byte[] txdata) {
        if (extractOpCode(txdata) != OP_ADDR_DATA)
            return null;
        byte[] data = extractData(txdata);
        int len = data[0];
        data = Arrays.copyOfRange(data, 1, 1 + len);
        return new Multihash(data);
    }

    byte[] createFileData(FHandle fhandle) {
        byte[] fid = fhandle.getCid().toBase58().getBytes();
        return buffer(OP_FILE_DATA, fid.length + 1).put((byte) fid.length).put(fid).array();
    }

    byte[] extractFileData(byte[] txdata) {
        if (extractOpCode(txdata) != OP_FILE_DATA)
            return null;
        byte[] data = extractData(txdata);
        int len = data[0];
        data = Arrays.copyOfRange(data, 1, 1 + len);
        return data;
    }

    boolean isOurs(byte[] txdata) {
        byte[] prefix = OP_PREFIX.getBytes();
        if (txdata[0] != OP_RETURN) return false;
        if (txdata[1] != txdata.length - 2) return false;
        byte[] aux = Arrays.copyOfRange(txdata, 2, 2 + prefix.length);
        return Arrays.equals(prefix, aux);
    }

    byte extractOpCode(byte[] txdata) {
        if (!isOurs(txdata)) return -1;
        byte[] prefix = OP_PREFIX.getBytes();
        byte opcode = txdata[prefix.length + 2];
        return opcode;
    }

    private byte[] extractData(byte[] txdata) {
        if (!isOurs(txdata)) return null;
        byte[] prefix = OP_PREFIX.getBytes();
        return Arrays.copyOfRange(txdata, 2 + prefix.length + 1, txdata.length);
    }

    private ByteBuffer buffer(byte op, int dlength) {
        byte[] prefix = OP_PREFIX.getBytes();
        ByteBuffer buffer = ByteBuffer.allocate(prefix.length + 1 + dlength);
        buffer.put(prefix);
        buffer.put(op);
        return buffer;
    }
}