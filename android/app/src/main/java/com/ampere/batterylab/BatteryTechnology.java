package com.ampere.batterylab;

import android.content.Intent;
import android.os.BatteryManager;

/** Validates the optional public battery-technology string from Android. */
final class BatteryTechnology {
    private static final int MAX_LENGTH = 32;

    private BatteryTechnology() { }

    static String read(Intent battery) {
        if (battery == null) return "";
        return normalize(battery.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY));
    }

    static String normalize(String value) {
        if (value == null) return "";
        for (int i = 0; i < value.length(); i++) {
            if (Character.isISOControl(value.charAt(i))) return "";
        }
        value = value.trim();
        if (value.isEmpty() || value.length() > MAX_LENGTH) return "";
        return value;
    }
}
