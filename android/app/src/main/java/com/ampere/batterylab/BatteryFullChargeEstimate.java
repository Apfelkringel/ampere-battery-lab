package com.ampere.batterylab;

/** Conservative full-charge estimate from remaining charge and level. */
final class BatteryFullChargeEstimate {
    private static final int MIN_MAH = 500;
    private static final int MAX_MAH = 30000;
    private static final int MIN_LEVEL_PERCENT = 20;

    private BatteryFullChargeEstimate() { }

    static int fromCounter(long counterUah, int rawLevel, int scale) {
        if (scale < 1 || scale > 1000 || rawLevel < 0 || rawLevel > scale) return 0;
        return fromCounterFraction(counterUah, rawLevel / (double) scale);
    }

    static int fromCounterAtLevel(long counterUah, int levelPercent) {
        if (levelPercent < MIN_LEVEL_PERCENT || levelPercent > 100) return 0;
        return fromCounterFraction(counterUah, levelPercent / 100d);
    }

    /** Uses the unrounded Android level fraction to avoid avoidable rounding error. */
    static int fromCounterFraction(long counterUah, double levelFraction) {
        if (counterUah <= 0L || !Double.isFinite(levelFraction)
                || levelFraction < MIN_LEVEL_PERCENT / 100d || levelFraction > 1d) return 0;
        double estimateMah = counterUah / 1000d / levelFraction;
        if (!Double.isFinite(estimateMah)) return 0;
        long rounded = Math.round(estimateMah);
        return rounded >= MIN_MAH && rounded <= MAX_MAH ? (int) rounded : 0;
    }
}
