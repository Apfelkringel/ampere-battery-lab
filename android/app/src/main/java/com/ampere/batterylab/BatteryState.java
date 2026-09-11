package com.ampere.batterylab;

import android.os.BatteryManager;

/** Keeps visible state, background sessions and overlays on one Android-derived rule. */
final class BatteryState {
    private static final long POWER_HINT_MAX_AGE_MS = 5000L;

    private BatteryState() { }

    static boolean isCharging(int status, int plugged) {
        return isCharging(status, plugged, true);
    }

    /**
     * Resolves status without confusing a missing OEM field with an explicit unplugged value.
     * A present zero remains authoritative; a missing plug field lets Android's charging/full
     * status speak for itself, matching the robust status-first strategy used by Beam.
     */
    static boolean isCharging(int status, int plugged, boolean plugValuePresent) {
        if (status != BatteryManager.BATTERY_STATUS_CHARGING
                && status != BatteryManager.BATTERY_STATUS_FULL) return false;
        return !plugValuePresent || plugged != 0;
    }

    /**
     * Returns the power-connected signal used by charge baselines. Some OEMs
     * omit EXTRA_PLUGGED even while reporting CHARGING/FULL; the resolved
     * charging state must remain authoritative in that case.
     */
    static boolean isPowerConnected(boolean charging, int plugged) {
        return charging || plugged != 0;
    }

    /**
     * Prefers the current sticky battery broadcast. A persisted monitor sample
     * is only a fallback when Android did not provide a usable status; it must
     * never mask a fresh plug/unplug transition for hours.
     */
    static boolean resolveUiCharging(int status, boolean detectedCharging,
                                     boolean hasRecentMonitorSample, boolean monitorCharging) {
        if (status == BatteryManager.BATTERY_STATUS_UNKNOWN && hasRecentMonitorSample) {
            return monitorCharging;
        }
        return detectedCharging;
    }

    /** Allows a power edge to bridge Android's short broadcast ordering gap only. */
    static boolean isPowerHintFresh(boolean hasHint, long hintAt, long now) {
        return hasHint && hintAt >= 0L && now >= hintAt
                && now - hintAt <= POWER_HINT_MAX_AGE_MS;
    }
}
