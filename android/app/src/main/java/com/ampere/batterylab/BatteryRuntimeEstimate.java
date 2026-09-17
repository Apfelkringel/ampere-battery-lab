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

    /** Short, actionable copy for a forecast that is correctly unavailable. */
    static String unavailableModeHint(boolean charging, long measuredDurationMs) {
        if (charging) return "Nach dem Abstecken";
        if (measuredDurationMs < 5L * 60L * 1000L) return "Ab 5 Min.";
        return "Mehr Daten";
    }

    static String unavailableNormalHint(boolean charging, boolean hasDischargeHistory) {
        return charging && !hasDischargeHistory ? "Nach dem Abstecken" : "Mehr Daten";
    }

    /** Estimates remaining minutes from a live current only when capacity is known. */
    static long minutesFromCurrent(int levelPercent, int capacityMah, int currentMa) {
        if (levelPercent < 0 || levelPercent > 100 || capacityMah <= 0 || capacityMah > 30_000
                || currentMa < 50) return 0L;
        float remainingMah = capacityMah * levelPercent / 100f;
        float minutes = remainingMah * 60f / currentMa;
        if (!Float.isFinite(minutes) || minutes < 0f) return 0L;
        return Math.max(1L, Math.round(minutes));
    }

    private static boolean isValidRate(float rate) {
        return Float.isFinite(rate) && rate > 0f && rate <= MAX_RATE_PERCENT_PER_HOUR;
    }
}
