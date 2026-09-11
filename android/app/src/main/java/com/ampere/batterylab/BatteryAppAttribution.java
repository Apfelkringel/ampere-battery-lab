package com.ampere.batterylab;

/** Keeps per-app drain attribution bounded by the observed battery energy. */
final class BatteryAppAttribution {
    private BatteryAppAttribution() { }

    static int estimateMah(int directMah, int directTotalMah, int observedTotalMah,
                           long foregroundMs, long totalForegroundMs) {
        if (directMah <= 0 || observedTotalMah <= 0) {
            if (directMah > 0) return 0;
            if (observedTotalMah <= 0 || foregroundMs <= 0L || totalForegroundMs <= 0L) return 0;
            return Math.max(0, Math.round(observedTotalMah * foregroundMs / (float) totalForegroundMs));
        }
        if (directMah > 0) {
            if (directTotalMah > observedTotalMah && observedTotalMah > 0) {
                return Math.max(0, Math.round(directMah * observedTotalMah / (float) directTotalMah));
            }
            return directMah;
        }
        return 0;
    }

    static int sampleMah(int currentMa, long intervalMs, long maxIntervalMs) {
        if (currentMa <= 0 || intervalMs <= 0L || maxIntervalMs <= 0L) return 0;
        long boundedIntervalMs = Math.min(intervalMs, maxIntervalMs);
        return Math.max(0, Math.round(currentMa * boundedIntervalMs / 3600000f));
    }
}
