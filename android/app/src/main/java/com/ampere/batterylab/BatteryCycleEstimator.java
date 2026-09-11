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
        float safeFraction = Math.max(0f, fraction);
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
        return (int) Math.floor(Math.max(0f, fraction));
    }
}
