package io.nessus;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Tx {
    
    private final String txId;
    private final String blockHash;
    private final Date blockTime;
    private final List<TxInput> inputs; 
    private final List<TxOutput> outputs;

    Tx(String txId, String blockHash, Date blockTime, List<TxInput> inputs, List<TxOutput> outputs) {
        this.txId = txId;
        this.blockHash = blockHash;
        this.blockTime = blockTime;
        this.inputs = new ArrayList<>(inputs);
        this.outputs = new ArrayList<>(outputs);
    }

    public String txId() {
        return txId;
    }

    public String blockHash() {
        return blockHash;
    }

    public Date blockTime() {
        return blockTime;
    }

    public List<TxInput> inputs() {
        return inputs;
    }

    public List<TxOutput> outputs() {
        return outputs;
    } 
    
    public String toString() {
        return String.format("[tx=%s, vin=%d, vout=%d]", txId, inputs.size(), outputs.size());
    }

    public static class TxBuilder {
        
        private final List<TxInput> inputs = new ArrayList<>();
        private final List<TxOutput> outputs = new ArrayList<>();
        
        private String txId;
        private String blockHash;
        private Date blockTime;
        
        public TxBuilder txId(String txId) {
            this.txId = txId;
            return this;
        }
        
        public TxBuilder blockHash(String blockHash) {
            this.blockHash = blockHash;
            return this;
        }
        
        public TxBuilder blockTime(Date blockTime) {
            this.blockTime = blockTime;
            return this;
        }
        
        public TxBuilder unspentInputs(List<UTXO> utxos) {
            this.inputs.addAll(utxos);
            return this;
        }
        
        public TxBuilder inputs(List<TxInput> ins) {
            this.inputs.addAll(ins);
            return this;
        }
        
        public TxBuilder input(TxInput in) {
            this.inputs.add(in);
            return this;
        }
        
        public TxBuilder outputs(List<TxOutput> outs) {
            this.outputs.addAll(outs);
            return this;
        }

        public TxBuilder output(TxOutput out) {
            this.outputs.add(out);
            return this;
        }

        public TxBuilder output(String address, BigDecimal amount) {
            this.outputs.add(new TxOutput(address, amount));
            return this;
        }

        public Tx build () {
            return new Tx(txId, blockHash, blockTime, inputs, outputs);
        }
    }
}