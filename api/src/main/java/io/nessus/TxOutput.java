package io.nessus;

import java.math.BigDecimal;

public class TxOutput {
    
    private final String address;
    private final BigDecimal amount;
    
    public TxOutput(String address, BigDecimal amount) {
        this.address = address;
        this.amount = amount;
    }

    public String getAddress() {
        return address;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}