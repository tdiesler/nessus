package io.nessus;

public class TxInput {
    
    private final String txId;
    private final int vout;
    private final String scriptPubKey;
    
    public TxInput(String txId, int vout, String scriptPubKey) {
        this.txId = txId;
        this.vout = vout;
        this.scriptPubKey = scriptPubKey;
    }

    public String getTxId() {
        return txId;
    }

    public int getVout() {
        return vout;
    }

    public String getScriptPubKey() {
        return scriptPubKey;
    }
}