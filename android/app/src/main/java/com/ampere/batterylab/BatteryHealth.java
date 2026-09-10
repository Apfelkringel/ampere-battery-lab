package com.ampere.batterylab;

/** Converts a measured capacity into a bounded battery-health percentage. */
final class BatteryHealth {
    private BatteryHealth() { }

    static int percent(int measuredMah, int designMah) {
        if (measuredMah <= 0 || designMah <= 0) return 0;
        return Math.max(1, Math.min(100, Math.round(measuredMah * 100f / designMah)));
    }
}
