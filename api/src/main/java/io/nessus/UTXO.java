package io.nessus;

import java.math.BigDecimal;

public class UTXO extends TxInput {
    
    private final String address;
    private final BigDecimal amount;
    
    public UTXO(String txId, Integer vout, String scriptPubKey, String address, BigDecimal amount) {
        super(txId, vout, scriptPubKey);
        this.address = address;
        this.amount = amount;
    }

    public String getAddress() {
        return address;
    }

    public BigDecimal getAmount() {
        return amount;
    }
    
    public String toString() {
        return String.format("%s %d => %.8f", getTxId(), getVout(), amount);
    }
}