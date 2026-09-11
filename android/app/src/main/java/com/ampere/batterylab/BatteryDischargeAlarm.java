package com.ampere.batterylab;

/** Safe defaults and hysteresis rules for the low-battery notification. */
final class BatteryDischargeAlarm {
    static final int DEFAULT_THRESHOLD = 15;
    static final int RESET_MARGIN = 3;
    private static final int MIN_THRESHOLD = 5;
    private static final int MAX_THRESHOLD = 50;

    private BatteryDischargeAlarm() { }

    static int normalizeThreshold(int threshold) {
        return Math.max(MIN_THRESHOLD, Math.min(MAX_THRESHOLD, threshold));
    }

    static boolean shouldAlert(int level, boolean charging, int threshold, boolean alreadySent,
                               int previousLevel) {
        int normalized = normalizeThreshold(threshold);
        if (charging || alreadySent || level < 0 || level > normalized) return false;
        // Notify when the level enters the low range. A missing baseline is
        // treated as an initial low reading so a newly enabled monitor does
        // not silently miss an already critical battery.
        return previousLevel < 0 || previousLevel > normalized;
    }

    static boolean shouldReset(int level, boolean charging, int threshold) {
        int normalized = normalizeThreshold(threshold);
        return charging || level < 0 || level > normalized + RESET_MARGIN;
    }
}
