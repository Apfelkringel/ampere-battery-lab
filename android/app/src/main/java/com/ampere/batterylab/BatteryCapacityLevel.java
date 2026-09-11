package com.ampere.batterylab;

import android.content.Intent;

/**
 * Android 16+ qualitative capacity level. This is a power-management hint,
 * not a state-of-health percentage and must never be used to calculate one.
 */
final class BatteryCapacityLevel {
    private static final String EXTRA = "android.os.extra.CAPACITY_LEVEL";

    private BatteryCapacityLevel() { }

    static int fromIntent(Intent battery) {
        if (battery == null) return -1;
        int value = battery.getIntExtra(EXTRA, -1);
        return normalize(value);
    }

    /** Keeps the validation rule testable without a mocked Android framework. */
    static int normalize(int value) {
        return isAvailable(value) ? value : -1;
    }

    static boolean isAvailable(int value) {
        return value >= 1 && value <= 5;
    }

    static String label(int value) {
        switch (value) {
            case 1: return "Kritisch";
            case 2: return "Niedrig";
            case 3: return "Normal";
            case 4: return "Hoch";
            case 5: return "Voll";
            default: return "Nicht verfügbar";
        }
    }
}
