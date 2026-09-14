package com.ampere.batterylab;

/** Pure decision rules for noisy Android cable/status transitions. */
public final class BatteryChargingTransition {
    private BatteryChargingTransition() { }

    public static boolean agreesWithStable(boolean detectedCharging, boolean stableCharging) {
        return detectedCharging == stableCharging;
    }

    public static boolean startsNewPending(boolean hasPending, boolean pendingCharging,
                                           boolean detectedCharging) {
        return !hasPending || pendingCharging != detectedCharging;
    }

    public static boolean confirmationElapsed(long now, long pendingSince, long confirmationMs) {
        return now >= pendingSince && now - pendingSince >= confirmationMs;
    }
}
