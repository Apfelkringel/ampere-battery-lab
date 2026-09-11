package com.ampere.batterylab;

import android.os.BatteryManager;
import java.util.Locale;

/** Reads Android's remaining battery-energy counter without confusing it with charge. */
final class BatteryEnergy {
    private static final long MIN_NANO_WATT_HOURS = 1_000_000L;
    private static final long MAX_NANO_WATT_HOURS = 100_000_000_000_000L;

    private BatteryEnergy() { }

    static long readNanoWattHours(BatteryManager manager) {
        if (manager == null) return 0L;
        try {
            return normalizeNanoWattHours(manager.getLongProperty(
                    BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER));
        } catch (RuntimeException ignored) {
            return 0L;
        }
    }

    static long normalizeNanoWattHours(long raw) {
        return raw >= MIN_NANO_WATT_HOURS && raw <= MAX_NANO_WATT_HOURS ? raw : 0L;
    }

    static double wattHours(long nanoWattHours) {
        long normalized = normalizeNanoWattHours(nanoWattHours);
        return normalized > 0L ? normalized / 1_000_000_000d : 0d;
    }

    static String label(long nanoWattHours) {
        double wattHours = wattHours(nanoWattHours);
        return wattHours > 0d ? String.format(Locale.GERMANY, "%.2f Wh", wattHours) : "—";
    }
}
