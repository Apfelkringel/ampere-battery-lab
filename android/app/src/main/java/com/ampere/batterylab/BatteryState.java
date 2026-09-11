package com.ampere.batterylab;

import android.os.BatteryManager;

/** Keeps visible state, background sessions and overlays on one Android-derived rule. */
final class BatteryState {
    private static final long POWER_HINT_MAX_AGE_MS = 5000L;

    private BatteryState() { }

    static boolean isCharging(int status, int plugged) {
        // A CHARGING status without a reported power source is a transient
        // broadcast state on some devices. Do not start a session until both
        // pieces of Android's battery state agree.
        return plugged != 0 && (status == BatteryManager.BATTERY_STATUS_CHARGING
                || status == BatteryManager.BATTERY_STATUS_FULL);
    }

    static boolean resolveUiCharging(boolean detectedCharging, boolean hasRecentMonitorSample,
                                     boolean monitorCharging) {
        return hasRecentMonitorSample ? monitorCharging : detectedCharging;
    }

    /** Allows a power edge to bridge Android's short broadcast ordering gap only. */
    static boolean isPowerHintFresh(boolean hasHint, long hintAt, long now) {
        return hasHint && hintAt >= 0L && now >= hintAt
                && now - hintAt <= POWER_HINT_MAX_AGE_MS;
    }
}
