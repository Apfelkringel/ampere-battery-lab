package com.ampere.batterylab;

import android.content.Context;
import android.os.Build;
import android.os.PowerManager;

/** Reads Android's public device-wide thermal throttling status. */
final class BatteryThermalStatus {
    static final int UNKNOWN = -1;

    private BatteryThermalStatus() { }

    static int read(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || context == null) return UNKNOWN;
        PowerManager power = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (power == null) return UNKNOWN;
        try {
            return normalize(power.getCurrentThermalStatus());
        } catch (RuntimeException ignored) {
            return UNKNOWN;
        }
    }

    static int normalize(int status) {
        return status >= PowerManager.THERMAL_STATUS_NONE
                && status <= PowerManager.THERMAL_STATUS_SHUTDOWN ? status : UNKNOWN;
    }

    static boolean isAvailable(int status) {
        return normalize(status) != UNKNOWN;
    }

    static String label(int status) {
        switch (normalize(status)) {
            case PowerManager.THERMAL_STATUS_NONE: return "Normal";
            case PowerManager.THERMAL_STATUS_LIGHT: return "Leicht erhöht";
            case PowerManager.THERMAL_STATUS_MODERATE: return "Mäßig";
            case PowerManager.THERMAL_STATUS_SEVERE: return "Hoch";
            case PowerManager.THERMAL_STATUS_CRITICAL: return "Kritisch";
            case PowerManager.THERMAL_STATUS_EMERGENCY: return "Notfall";
            case PowerManager.THERMAL_STATUS_SHUTDOWN: return "Abschaltung";
            default: return "Nicht verfügbar";
        }
    }
}
