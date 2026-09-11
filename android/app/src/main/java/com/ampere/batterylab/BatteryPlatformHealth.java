package com.ampere.batterylab;

import android.os.BatteryManager;

/** Maps Android's qualitative battery-health signal without inventing a percentage. */
final class BatteryPlatformHealth {
    private BatteryPlatformHealth() { }

    static String label(int health) {
        switch (health) {
            case BatteryManager.BATTERY_HEALTH_GOOD: return "Gut";
            case BatteryManager.BATTERY_HEALTH_OVERHEAT: return "Überhitzt";
            case BatteryManager.BATTERY_HEALTH_DEAD: return "Kritisch";
            case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE: return "Überspannung";
            case BatteryManager.BATTERY_HEALTH_COLD: return "Zu kalt";
            case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE: return "Fehler";
            default: return "Nicht verfügbar";
        }
    }

    static boolean isAvailable(int health) {
        return health != BatteryManager.BATTERY_HEALTH_UNKNOWN;
    }
}
