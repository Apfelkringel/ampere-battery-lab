package com.ampere.batterylab;

/** Pure rules for the local full-cycle fallback based on observed discharge. */
final class BatteryCycleAccumulator {
    private BatteryCycleAccumulator() { }

    static float addDischargePercent(float accumulated, int previousLevel, int level, boolean charging) {
        float safe = Math.max(0f, accumulated);
        if (charging || previousLevel < 0 || level >= previousLevel) return safe;
        return safe + previousLevel - level;
    }

    static int completedCycles(float dischargePercent) {
        return Math.max(0, (int) Math.floor(Math.max(0f, dischargePercent) / 100f));
    }
}
