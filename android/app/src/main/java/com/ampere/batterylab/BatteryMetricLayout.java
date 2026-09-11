package com.ampere.batterylab;

/** Chooses the metric-card composition from the actual label lane width. */
final class BatteryMetricLayout {
    private static final float STACK_THRESHOLD_DP = 160f;

    private BatteryMetricLayout() { }

    static boolean shouldStack(float cardWidthDp) {
        return cardWidthDp > 0f && cardWidthDp < STACK_THRESHOLD_DP;
    }
}
