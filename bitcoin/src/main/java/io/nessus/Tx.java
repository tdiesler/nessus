package io.nessus;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class Tx {
    
    private final List<TxInput> inputs; 
    private final List<TxOutput> outputs;

    Tx(List<TxInput> inputs, List<TxOutput> outputs) {
        this.inputs = new ArrayList<>(inputs);
        this.outputs = new ArrayList<>(outputs);
    }

    public List<TxInput> getInputs() {
        return inputs;
    }

    public List<TxOutput> getOutputs() {
        return outputs;
    } 

    public static class TxBuilder {
        
        private final List<TxInput> inputs = new ArrayList<>();
        private final List<TxOutput> outputs = new ArrayList<>();
        
        public Tx.TxBuilder unspentInputs(List<UTXO> utxos) {
            this.inputs.addAll(utxos);
            return this;
        }
        
        public Tx.TxBuilder inputs(List<TxInput> ins) {
            this.inputs.addAll(ins);
            return this;
        }
        
        public Tx.TxBuilder input(String txId, int vout, String scriptPubKey) {
            this.inputs.add(new TxInput(txId, vout, scriptPubKey));
            return this;
        }
        
        public Tx.TxBuilder input(TxInput in) {
            this.inputs.add(in);
            return this;
        }
        
        public Tx.TxBuilder outputs(List<TxOutput> outs) {
            this.outputs.addAll(outs);
            return this;
        }

        public Tx.TxBuilder output(TxOutput out) {
            this.outputs.add(out);
            return this;
        }

        public Tx.TxBuilder output(String address, BigDecimal amount) {
            this.outputs.add(new TxOutput(address, amount));
            return this;
        }

        public Tx build () {
            return new Tx(inputs, outputs);
        }
    }
}