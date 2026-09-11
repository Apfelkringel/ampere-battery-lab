package com.ampere.batterylab;

/** Pure, bounded time-to-charge calculation used after system estimates fail. */
final class BatteryTimeEstimate {
    private static final int MIN_CURRENT_MA = 50;
    private static final int MAX_RATE_MA = 100_000;

    private BatteryTimeEstimate() { }

    /** Returns minutes, or 0 when the input cannot support an honest estimate. */
    static long minutesToTarget(int level, int targetLevel, int capacityMah,
                               float historicalRateMahPerHour, int currentMa) {
        if (level < 0 || targetLevel <= level || targetLevel > 100 || capacityMah <= 0) return 0L;
        int missingMah = Math.round(capacityMah * (targetLevel - level) / 100f);
        if (missingMah <= 0) return 0L;
        boolean historicalUsable = Float.isFinite(historicalRateMahPerHour)
                && historicalRateMahPerHour >= MIN_CURRENT_MA
                && historicalRateMahPerHour <= MAX_RATE_MA;
        boolean currentUsable = currentMa >= MIN_CURRENT_MA && currentMa <= MAX_RATE_MA;
        float rate = historicalUsable ? historicalRateMahPerHour : currentUsable ? currentMa : 0f;
        if (rate <= 0f) return 0L;
        double minutes = missingMah * 60d / rate;
        if (!Double.isFinite(minutes) || minutes <= 0d) return 0L;
        return Math.max(1L, Math.round(minutes));
    }
}
