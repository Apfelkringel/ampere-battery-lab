package com.ampere.batterylab;

import android.os.BatteryManager;

/** Pure widget status rules; keeps the visible charging state honest. */
final class BatteryWidgetStatus {
    private BatteryWidgetStatus() { }

    static String label(int status, boolean charging) {
        if (status == BatteryManager.BATTERY_STATUS_FULL) return "Voll geladen";
        if (charging) return "Laden";
        if (status == BatteryManager.BATTERY_STATUS_UNKNOWN) return "Unbekannt";
        return "Akku";
    }

    static String caption(boolean english) {
        return english ? "Battery" : "Akku";
    }

    static boolean accent(int status, boolean charging) {
        return charging && status != BatteryManager.BATTERY_STATUS_FULL;
    }
}
