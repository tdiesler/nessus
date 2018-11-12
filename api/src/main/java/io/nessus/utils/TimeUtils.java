package io.nessus.utils;

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