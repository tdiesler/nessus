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
import java.util.Date;
import java.util.List;

public interface Block {
    
    String hash();

    int confirmations();

    int size();

    int height();

    int version();

    String merkleRoot();

    List<String> tx();

    Date time();

    long nonce();

    String bits();

    BigDecimal difficulty();

    String previousHash();

    String nextHash();

    String chainwork();

    Block previous();

    Block next();
}
