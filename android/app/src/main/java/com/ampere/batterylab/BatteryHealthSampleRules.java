package com.ampere.batterylab;

/** Rules for automatic full-charge capacity samples derived from a charge session. */
final class BatteryHealthSampleRules {
    private static final int MIN_END_LEVEL = 95;
    private static final int MIN_LEVEL_CHANGE = 5;
    private static final int MAX_STABLE_CURRENT_MA = 25;
    private static final int MIN_ESTIMATED_CAPACITY_MAH = 500;
    private static final int MAX_ESTIMATED_CAPACITY_MAH = 20000;

    private BatteryHealthSampleRules() { }

    /** Keeps the latest usable charging reading; an unavailable sample cannot refresh it. */
    static int latestUsableCurrent(int previousCurrentMa, int currentMa) {
        return currentMa > 0 ? currentMa : Math.max(0, previousCurrentMa);
    }

    static boolean isEligible(int startLevel, int endLevel, int energyMah, int currentMa) {
        return startLevel >= 0 && startLevel <= 100
                && endLevel >= MIN_END_LEVEL && endLevel <= 100
                && endLevel - startLevel >= MIN_LEVEL_CHANGE
                && energyMah > 0
                && currentMa > 0 && currentMa <= MAX_STABLE_CURRENT_MA;
    }

    static int estimateCapacityMah(int startLevel, int endLevel, int energyMah, int currentMa) {
        if (!isEligible(startLevel, endLevel, energyMah, currentMa)) return 0;
        long estimate = Math.round((double) energyMah * 100d / (endLevel - startLevel));
        return estimate >= MIN_ESTIMATED_CAPACITY_MAH && estimate <= MAX_ESTIMATED_CAPACITY_MAH
                ? (int) estimate : 0;
    }
}
