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
        return String.format("[txid=%s, vout=%d, amount=%.8f]", getTxId(), getVout(), amount);
    }
}
