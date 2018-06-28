package io.nessus;

import java.math.BigDecimal;

public class TxOutput {
    
    private final String address;
    private final BigDecimal amount;
    private final byte[] data;
    
    private String type;
    
    public TxOutput(String address, BigDecimal amount) {
        this(address, amount, null);
    }

    public TxOutput(String address, BigDecimal amount, byte[] data) {
        this.address = address;
        this.amount = amount;
        this.data = data;
    }

    public String getAddress() {
        return address;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public byte[] getData() {
        return data;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}