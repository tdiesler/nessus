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

import java.util.Date;

public final class TimeUtils {

    // Hide ctor
    private TimeUtils() {};
    
    public static Long elapsedTime(Date startTime) {
        return elapsedTime(startTime, new Date());
    }

    public static Long elapsedTime(Date startTime, Date endTime) {
        return endTime.getTime() - startTime.getTime();
    }

    public static String elapsedTimeString(Date startTime) {
        Long elapsed = elapsedTime(startTime, new Date());
        return elapsedTimeString(elapsed);
    }
    
    public static String elapsedTimeString(Long millis) {
        long hours = millis / 3600000;
        long mins = (millis % 3600000) / 60000;
        long secs = (millis % 60000) / 1000;
        long mills = millis % 1000;
        return String.format("%02d:%02d:%02d:%03d", hours, mins, secs, mills);
    }
}
