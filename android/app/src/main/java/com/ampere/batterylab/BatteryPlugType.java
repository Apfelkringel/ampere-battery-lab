package com.ampere.batterylab;

import android.os.BatteryManager;

/** Interprets EXTRA_PLUGGED as a bit field, matching Android/OEM broadcasts. */
final class BatteryPlugType {
    private BatteryPlugType() { }

    static String label(int plugged) {
        if ((plugged & BatteryManager.BATTERY_PLUGGED_WIRELESS) != 0) return "Kabellos";
        if ((plugged & BatteryManager.BATTERY_PLUGGED_AC) != 0) return "Netzteil";
        if ((plugged & BatteryManager.BATTERY_PLUGGED_USB) != 0) return "USB";
        if ((plugged & BatteryManager.BATTERY_PLUGGED_DOCK) != 0) return "Dock";
        return "Externe Stromquelle";
    }
}
