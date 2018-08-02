package io.nessus;

/*-
 * #%L
 * Nessus :: API
 * %%
 * Copyright (C) 2018 Nessus
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.math.BigDecimal;

import wf.bitcoin.krotjson.HexCoder;

public class TxOutput {
    
    private final String addr;
    private final BigDecimal amount;
    private final byte[] data;
    
    private String type;
    
    public TxOutput(String addr, BigDecimal amount) {
        this(addr, amount, null);
    }

    public TxOutput(String addr, BigDecimal amount, byte[] data) {
        this.addr = addr;
        this.amount = amount;
        this.data = data;
    }

    public String getAddress() {
        return addr;
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
    
    public String toString() {
        String hex = data != null ? HexCoder.encode(data) : null;
        return String.format("[addr=%s, amnt=%f, data=%s]", addr, amount, hex);
    }
}
