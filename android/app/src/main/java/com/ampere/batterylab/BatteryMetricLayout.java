package com.ampere.batterylab;

/** Chooses the metric-card composition from the actual label lane width. */
final class BatteryMetricLayout {
    private static final float STACK_THRESHOLD_DP = 160f;
    private static final float LIVE_FLOW_COMPACT_THRESHOLD_DP = 480f;

    private BatteryMetricLayout() { }

    static boolean shouldStack(float cardWidthDp) {
        return cardWidthDp > 0f && cardWidthDp < STACK_THRESHOLD_DP;
    }

    static boolean shouldUseCompactLiveFlow(float cardWidthDp) {
        return cardWidthDp > 0f && cardWidthDp < LIVE_FLOW_COMPACT_THRESHOLD_DP;
    }
}
