package com.ampere.batterylab;

/** Confidence-aware blend of a current discharge phase and historical usage. */
final class BatteryRuntimeEstimate {
    static final long MIN_SESSION_MS = 10L * 60L * 1000L;
    private static final long FULL_SESSION_WEIGHT_MS = 30L * 60L * 1000L;
    private static final float MAX_RATE_PERCENT_PER_HOUR = 100f;

    private BatteryRuntimeEstimate() { }

    /**
     * Gradually reaches the advertised 40/60 history/current split only after
     * thirty minutes of usable current-session evidence. Short sessions do not
     * replace a stable history with one noisy percentage transition.
     */
    static float blendRate(float historicalRate, float currentRate, long sessionMs) {
        boolean historyValid = isValidRate(historicalRate);
        boolean currentValid = isValidRate(currentRate);
        if (!historyValid) return currentValid ? currentRate : 0f;
        if (!currentValid || sessionMs < MIN_SESSION_MS) return historicalRate;
        float confidence = Math.min(1f, sessionMs / (float) FULL_SESSION_WEIGHT_MS);
        float currentWeight = .6f * confidence;
        float blended = historicalRate * (1f - currentWeight) + currentRate * currentWeight;
        return isValidRate(blended) ? blended : historicalRate;
    }

    /** Converts observed phase drain to %/h, with charge-counter energy as a fallback. */
    static float rateFromObserved(float observedPercent, int energyMah, int capacityMah,
                                  long durationMs) {
        if (durationMs < MIN_SESSION_MS || durationMs <= 0L) return 0f;
        float percent = Float.isFinite(observedPercent) ? Math.max(0f, observedPercent) : 0f;
        if (energyMah > 0 && capacityMah > 0) {
            percent = Math.max(percent, energyMah * 100f / capacityMah);
        }
        if (percent <= 0f) return 0f;
        float rate = percent * 3_600_000f / durationMs;
        return isValidRate(rate) ? rate : 0f;
    }

    /** Whether a last-discharge runtime can be computed from real local evidence. */
    static boolean hasHistoricalEstimate(int referenceLevel, float observedPercent,
                                         long durationMs, float ratePercentPerHour) {
        if (referenceLevel < 0 || referenceLevel > 100) return false;
        if (Float.isFinite(ratePercentPerHour) && ratePercentPerHour > 0f) return true;
        return Float.isFinite(observedPercent) && observedPercent > 0f
                && durationMs >= 5L * 60L * 1000L;
    }

    private static boolean isValidRate(float rate) {
        return Float.isFinite(rate) && rate > 0f && rate <= MAX_RATE_PERCENT_PER_HOUR;
    }
}
