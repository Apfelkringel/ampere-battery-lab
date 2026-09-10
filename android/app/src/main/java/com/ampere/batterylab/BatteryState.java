package com.ampere.batterylab;

import android.os.BatteryManager;

/** Keeps visible state, background sessions and overlays on one Android-derived rule. */
final class BatteryState {
    private BatteryState() { }

    static boolean isCharging(int status, int plugged) {
        // A CHARGING status without a reported power source is a transient
        // broadcast state on some devices. Do not start a session until both
        // pieces of Android's battery state agree.
        return plugged != 0 && (status == BatteryManager.BATTERY_STATUS_CHARGING
                || status == BatteryManager.BATTERY_STATUS_FULL);
    }
}
