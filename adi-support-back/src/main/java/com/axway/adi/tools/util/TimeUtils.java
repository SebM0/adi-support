package com.axway.adi.tools.util;

public class TimeUtils {
    private TimeUtils() {
    }

    public static long computeDuration(long duration, String durationFieldType) {
        switch (durationFieldType) {
            case "months":
                duration *= 30 * 24 * 60 * 60 * 1000;
                break;
            case "weeks":
                duration *= 7;
                // no break;
            case "days":
                duration *= 24;
                // no break;
            case "hours":
                duration *= 60;
                // no break;
            case "minutes":
                duration *= 60;
                // no break;
            case "seconds":
                duration *= 1000;
                break;
        }
        return duration;
    }
}
