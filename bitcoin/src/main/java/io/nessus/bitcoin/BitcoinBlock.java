package io.nessus.bitcoin;

/*-
 * #%L
 * Nessus :: Bitcoin
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
import java.util.Date;
import java.util.List;

import io.nessus.Block;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient;

public class BitcoinBlock implements Block {

    final BitcoindRpcClient.Block block;
    
    public BitcoinBlock(BitcoindRpcClient.Block block) {
        this.block = block;
    }

    @Override
    public String hash() {
        return block.hash();
    }

    @Override
    public int confirmations() {
        return block.confirmations();
    }

    @Override
    public int size() {
        return block.size();
    }

    @Override
    public int height() {
        return block.height();
    }

    @Override
    public int version() {
        return block.version();
    }

    @Override
    public String merkleRoot() {
        return block.merkleRoot();
    }

    @Override
    public List<String> tx() {
        return block.tx();
    }

    @Override
    public Date time() {
        return block.time();
    }

    @Override
    public long nonce() {
        return block.nonce();
    }

    @Override
    public String bits() {
        return block.bits();
    }

    @Override
    public BigDecimal difficulty() {
        return block.difficulty();
    }

    @Override
    public String previousHash() {
        return block.previousHash();
    }

    @Override
    public String nextHash() {
        return block.nextHash();
    }

    @Override
    public String chainwork() {
        return block.chainwork();
    }

    @Override
    public Block previous() {
        return new BitcoinBlock(block.previous());
    }

    public Block next() {
        return new BitcoinBlock(block.next());
    }
    
    @Override
    public int hashCode() {
        return block.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (!(obj instanceof Block)) return false;
        Block other = (Block) obj;
        return hash().equals(other.hash());
    }
    
    public String toString() {
        return String.format("Block[%d: %s]", height(), hash());
    }
}
