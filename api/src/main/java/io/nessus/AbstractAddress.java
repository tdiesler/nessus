package io.nessus;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.nessus.Wallet.Address;

public abstract class AbstractAddress implements Address {

    private final String address;
    private final List<String> labels = new ArrayList<String>();
    
    private Boolean watchOnly;

    public AbstractAddress(String address) {
    	this(address, Collections.emptyList());
    }

    public AbstractAddress(String address, List<String> labels) {
        this.address = address;
        this.labels.addAll(labels);
    }

    @Override
    public String getAddress() {
        return address;
    }

    @Override
	public String getPrivKey() {
		return null;
	}

	@Override
	public Address setLabels(List<String> labels) {
		throw new UnsupportedOperationException();
	}

	@Override
    public boolean isWatchOnly() {
		if (watchOnly == null) {
			watchOnly = getPrivKey() == null;
		}
        return watchOnly;
    }

    @Override
    public List<String> getLabels() {
        return Collections.unmodifiableList(labels);
    }

    @Override
    public int hashCode() {
        return address.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (!(obj instanceof Address)) return false;
        Address other = (Address) obj;
        return address.equals(other.getAddress());
    }

    @Override
    public String toString() {
		boolean wonly = isWatchOnly();
		return String.format("[addr=%s, wo=%b, labels=%s]", address, wonly, labels);
    }
}
