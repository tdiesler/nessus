package io.nessus;

import java.math.BigDecimal;

public class TxOutput {
    
    public final String address;
    public final BigDecimal amount;
    
    TxOutput(String address, BigDecimal amount) {
        this.address = address;
        this.amount = amount;
    }

    public String getAddress() {
        return address;
    }

    public BigDecimal getAmount() {
        return amount;
    }
    
    static class TxOutputBuilder {
        
        public String address;
        public BigDecimal amount;
        
        public TxOutput.TxOutputBuilder address(String address) {
            this.address = address;
            return this;
        }
        
        public TxOutput.TxOutputBuilder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }
        
        public TxOutput build () {
            return new TxOutput(address, amount);
        }
    }
}