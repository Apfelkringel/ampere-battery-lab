package com.ampere.batterylab;

import android.content.Intent;
import java.util.Locale;

/**
 * Validates Android's optional charger capability pair. The values describe
 * what the attached source supports, not the current flowing into the cell.
 */
final class BatteryChargerCapability {
    private static final String MAX_CURRENT = "max_charging_current";
    private static final String MAX_VOLTAGE = "max_charging_voltage";
    private static final long MIN_CURRENT_UA = 100_000L;
    private static final long MAX_CURRENT_UA = 100_000_000L;
    private static final long MIN_VOLTAGE_UV = 1_000_000L;
    private static final long MAX_VOLTAGE_UV = 30_000_000L;
    private static final int MIN_POWER_MW = 100;
    private static final int MAX_POWER_MW = 500_000;

    private BatteryChargerCapability() { }

    static int maxPowerMilliwatts(Intent battery) {
        if (battery == null) return 0;
        return maxPowerMilliwatts(battery.getIntExtra(MAX_CURRENT, -1),
                battery.getIntExtra(MAX_VOLTAGE, -1));
    }

    static int maxPowerMilliwatts(long currentMicroamps, long voltageMicrovolts) {
        if (currentMicroamps < MIN_CURRENT_UA || currentMicroamps > MAX_CURRENT_UA
                || voltageMicrovolts < MIN_VOLTAGE_UV || voltageMicrovolts > MAX_VOLTAGE_UV) {
            return 0;
        }
        long powerMilliwatts = Math.round(currentMicroamps * voltageMicrovolts / 1_000_000_000d);
        return powerMilliwatts >= MIN_POWER_MW && powerMilliwatts <= MAX_POWER_MW
                ? (int) powerMilliwatts : 0;
    }

    static String label(int powerMilliwatts) {
        return powerMilliwatts > 0
                ? String.format(Locale.GERMANY, "Max. %.1f W", powerMilliwatts / 1000f)
                : "Max. Ladeleistung nicht verfügbar";
    }
}
