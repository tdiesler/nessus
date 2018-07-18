package io.nessus.utils;

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

public final class AssertArgument {

    // hide ctor
    private AssertArgument() {
    }

    /**
     * Throws an IllegalArgumentException when the given value is null.
     */
    public static <T> T assertNotNull(T value, String name) {
        if (value == null)
            throw new IllegalArgumentException("Null " + name);

        return value;
    }

    /**
     * Throws an IllegalArgumentException when the given value is not true.
     */
    public static Boolean assertTrue(Boolean value, String message) {
        if (!Boolean.valueOf(value))
            throw new IllegalArgumentException(message);
        return value;
    }

    /**
     * Throws an IllegalArgumentException when the given value is not false.
     */
    public static Boolean assertFalse(Boolean value, String message) {
        if (Boolean.valueOf(value))
            throw new IllegalArgumentException(message);
        return value;
    }
}
