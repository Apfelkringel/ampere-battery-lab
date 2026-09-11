package com.ampere.batterylab;

/** Converts a measured capacity into a bounded battery-health percentage. */
final class BatteryHealth {
    private static final int MIN_CAPACITY_MAH = 500;
    private static final int MAX_CAPACITY_MAH = 30000;

    private BatteryHealth() { }

    static int percent(int measuredMah, int designMah) {
        if (!isPlausibleCapacity(measuredMah) || !isPlausibleCapacity(designMah)) return 0;
        return Math.max(1, Math.min(100, Math.round(measuredMah * 100f / designMah)));
    }

    static boolean isPlausibleCapacity(int mah) {
        return mah >= MIN_CAPACITY_MAH && mah <= MAX_CAPACITY_MAH;
    }
}
