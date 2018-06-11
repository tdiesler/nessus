package io.nessus;

public class TxInput {
    
    private final String txId;
    private final int vout;
    private final String scriptPubKey;
    
    TxInput(String txId, int vout, String scriptPubKey) {
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
    
    public static class TxInputBuilder {
        
        private String txId;
        private int vout;
        private String scriptPubKey;
        
        public TxInput.TxInputBuilder txId(String txId) {
            this.txId = txId;
            return this;
        }
        
        public TxInput.TxInputBuilder vout(int vout) {
            this.vout = vout;
            return this;
        }
        
        public TxInput.TxInputBuilder scriptPubKey(String scriptPubKey) {
            this.scriptPubKey = scriptPubKey;
            return this;
        }
        
        public TxInput build () {
            return new TxInput(txId, vout, scriptPubKey);
        }
    }
}