package com.ampere.batterylab;

/** Keeps per-app drain attribution bounded by the observed battery energy. */
final class BatteryAppAttribution {
    private BatteryAppAttribution() { }

    static int estimateMah(int directMah, int directTotalMah, int observedTotalMah,
                           long foregroundMs, long totalForegroundMs) {
        if (directMah <= 0) {
            return estimateFallbackMah(observedTotalMah, directTotalMah,
                    foregroundMs, totalForegroundMs);
        }
        if (observedTotalMah <= 0) return 0;
        if (directTotalMah > observedTotalMah) {
            return Math.max(0, Math.round(directMah * observedTotalMah / (float) directTotalMah));
        }
        return directMah;
    }

    static int estimateFallbackMah(int observedTotalMah, int directAssignedMah,
                                   long foregroundMs, long totalForegroundMs) {
        if (observedTotalMah <= 0 || foregroundMs <= 0L || totalForegroundMs <= 0L) return 0;
        int remainingMah = Math.max(0, observedTotalMah - Math.max(0, directAssignedMah));
        return Math.max(0, Math.round(remainingMah * foregroundMs / (float) totalForegroundMs));
    }

    static int sampleMah(int currentMa, long intervalMs, long maxIntervalMs) {
        if (currentMa <= 0 || intervalMs <= 0L || maxIntervalMs <= 0L) return 0;
        long boundedIntervalMs = Math.min(intervalMs, maxIntervalMs);
        return Math.max(0, Math.round(currentMa * boundedIntervalMs / 3600000f));
    }
}
