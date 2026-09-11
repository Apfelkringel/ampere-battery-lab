package com.ampere.batterylab;

/** Validates persisted percentage values before they reach calculations or UI. */
final class BatteryPercentage {
    private BatteryPercentage() { }

    /** A percentage belonging to one charging/discharging phase. */
    static boolean isValidPhase(float value) {
        return Float.isFinite(value) && value >= 0f && value <= 100f;
    }

    static float normalizePhase(float value) {
        return isValidPhase(value) ? value : 0f;
    }

    /** A cumulative consumption value may cross 100% over multiple partial cycles. */
    static boolean isValidCumulative(float value) {
        return Float.isFinite(value) && value >= 0f;
    }

    static float normalizeCumulative(float value) {
        return isValidCumulative(value) ? value : 0f;
    }
}
