package com.ampere.batterylab;

import android.content.Intent;

/**
 * Android's qualitative charging profile from API 34+. It describes why the
 * charging rate may be limited; it is not used to decide whether a cable is
 * connected or to calculate battery health.
 */
final class BatteryChargingState {
    private static final String EXTRA = "android.os.extra.CHARGING_STATUS";

    private BatteryChargingState() { }

    static int fromIntent(Intent battery) {
        if (battery == null) return 0;
        return normalize(battery.getIntExtra(EXTRA, 0));
    }

    static int normalize(int value) {
        return value >= 1 && value <= 5 ? value : 0;
    }

    static boolean isAvailable(int value) {
        return value >= 1 && value <= 5;
    }

    static boolean isSpecial(int value) {
        return value >= 2 && value <= 5;
    }

    static String label(int value) {
        switch (value) {
            case 1: return "Normal";
            case 2: return "Zu kalt";
            case 3: return "Zu heiß";
            case 4: return "Akkuschonend";
            case 5: return "Adaptiv";
            default: return "Nicht verfügbar";
        }
    }
}
