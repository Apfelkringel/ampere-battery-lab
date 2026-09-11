package com.ampere.batterylab;

/** Pure temperature-alarm rules with a three-degree hysteresis band. */
final class BatteryTemperatureAlarm {
    static final int DEFAULT_THRESHOLD_TENTHS = 450;
    private static final int MIN_THRESHOLD_TENTHS = 350;
    private static final int MAX_THRESHOLD_TENTHS = 600;
    private static final int HYSTERESIS_TENTHS = 30;

    private BatteryTemperatureAlarm() { }

    static int normalizeThreshold(int tenths) {
        return tenths >= MIN_THRESHOLD_TENTHS && tenths <= MAX_THRESHOLD_TENTHS
                ? tenths : DEFAULT_THRESHOLD_TENTHS;
    }

    static boolean shouldAlert(int temperatureTenths, int thresholdTenths,
                               boolean enabled, boolean alreadySent) {
        return enabled && !alreadySent && temperatureTenths > 0
                && temperatureTenths >= normalizeThreshold(thresholdTenths);
    }

    static boolean shouldReset(int temperatureTenths, int thresholdTenths) {
        return temperatureTenths <= 0
                || temperatureTenths <= normalizeThreshold(thresholdTenths) - HYSTERESIS_TENTHS;
    }
}
