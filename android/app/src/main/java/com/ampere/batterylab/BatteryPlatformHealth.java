package com.ampere.batterylab;

import android.os.BatteryManager;

/** Maps Android's qualitative battery-health signal without inventing a percentage. */
final class BatteryPlatformHealth {
    // Android 12+ and Android 15+ builds may expose additional qualitative
    // states even though they are not present as public constants on every SDK.
    private static final int BATTERY_HEALTH_FAIR = 8;
    private static final int BATTERY_HEALTH_EXCELLENT = 9;

    private BatteryPlatformHealth() { }

    static String label(int health) {
        switch (health) {
            case BatteryManager.BATTERY_HEALTH_GOOD: return "Gut";
            case BatteryManager.BATTERY_HEALTH_OVERHEAT: return "Überhitzt";
            case BatteryManager.BATTERY_HEALTH_DEAD: return "Kritisch";
            case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE: return "Überspannung";
            case BatteryManager.BATTERY_HEALTH_COLD: return "Zu kalt";
            case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE: return "Fehler";
            case BATTERY_HEALTH_FAIR: return "Akzeptabel";
            case BATTERY_HEALTH_EXCELLENT: return "Sehr gut";
            default: return "Nicht verfügbar";
        }
    }

    static boolean isAvailable(int health) {
        return !"Nicht verfügbar".equals(label(health));
    }
}
