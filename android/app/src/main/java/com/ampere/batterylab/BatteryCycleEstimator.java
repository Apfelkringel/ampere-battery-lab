package com.ampere.batterylab;

/**
 * Conservative equivalent-full-cycle estimate based on Android's persistent
 * charge counter. The counter is a remaining-charge value, so only upward
 * movement while charging is counted. OEM recalibration and counter resets
 * are ignored instead of being turned into fake battery wear.
 */
final class BatteryCycleEstimator {
    private BatteryCycleEstimator() { }

    static float addChargedFraction(float fraction, long previousUah, long currentUah,
                                    boolean charging, int designMah) {
        // The persisted value is a remainder after completed cycles have
        // already been removed. Anything outside [0, 1) is corrupt state,
        // not evidence for additional battery wear.
        float safeFraction = isValidRemainder(fraction) ? fraction : 0f;
        if (!charging || previousUah <= 0L || currentUah <= previousUah
                || designMah < 500 || designMah > 30000) return safeFraction;
        long designUah = designMah * 1000L;
        long deltaUah = currentUah - previousUah;
        // A larger jump is almost certainly a reset/recalibration or a unit
        // mismatch. Wait for the next stable baseline instead.
        if (deltaUah <= 0L || deltaUah > designUah * 3L) return safeFraction;
        return safeFraction + deltaUah / (float) designUah;
    }

    static int completedCycles(float fraction) {
        if (!Float.isFinite(fraction) || fraction < 0f) return 0;
        return Math.max(0, (int) Math.floor(fraction));
    }

    static float remainder(float fraction) {
        if (!Float.isFinite(fraction) || fraction < 0f) return 0f;
        float remainder = fraction - completedCycles(fraction);
        return isValidRemainder(remainder) ? remainder : 0f;
    }

    private static boolean isValidRemainder(float value) {
        return Float.isFinite(value) && value >= 0f && value < 1f;
    }
}
