package com.ampere.batterylab;

import android.os.BatteryManager;

/** Stable buckets for charge-rate history; EXTRA_PLUGGED is a bit field. */
final class BatteryChargeSource {
    static final int UNKNOWN = 0;
    static final int AC = 1;
    static final int WIRELESS = 2;
    static final int USB = 3;
    static final int DOCK = 4;

    private BatteryChargeSource() { }

    static int fromPlugged(int plugged) {
        if ((plugged & BatteryManager.BATTERY_PLUGGED_WIRELESS) != 0) return WIRELESS;
        if ((plugged & BatteryManager.BATTERY_PLUGGED_AC) != 0) return AC;
        if ((plugged & BatteryManager.BATTERY_PLUGGED_USB) != 0) return USB;
        if ((plugged & BatteryManager.BATTERY_PLUGGED_DOCK) != 0) return DOCK;
        return UNKNOWN;
    }

    static boolean isKnown(int source) {
        return source != UNKNOWN;
    }
}
