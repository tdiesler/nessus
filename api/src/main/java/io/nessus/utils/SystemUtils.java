package io.nessus.utils;

public class SystemUtils {

    // Hide ctor
    private SystemUtils() {
    }

    public static String getenv(String name, String defval) {
        String resval = System.getenv(name);
        if (resval == null || resval.length() == 0) {
            resval = defval;
        }
        return resval;
    }
}
