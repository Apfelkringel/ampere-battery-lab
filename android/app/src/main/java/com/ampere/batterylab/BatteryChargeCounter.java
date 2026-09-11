package com.ampere.batterylab;

import android.os.BatteryManager;

/** Validates Android's remaining charge counter, whose public unit is microampere-hours. */
final class BatteryChargeCounter {
    private static final long MIN_MICROAMPERE_HOURS = 500_000L;
    private static final long MAX_MICROAMPERE_HOURS = 30_000_000L;

    private BatteryChargeCounter() { }

    static long readMicroampereHours(BatteryManager manager) {
        if (manager == null) return 0L;
        try {
            long value = 0L;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                value = normalizeMicroampereHours(
                        manager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER));
            }
            if (value > 0L) return value;
            return normalizeMicroampereHours(
                    manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER));
        } catch (RuntimeException ignored) {
            return 0L;
        }
    }

    static long normalizeMicroampereHours(long rawMicroampereHours) {
        return rawMicroampereHours >= MIN_MICROAMPERE_HOURS
                && rawMicroampereHours <= MAX_MICROAMPERE_HOURS
                ? rawMicroampereHours : 0L;
    }

    static int toMilliampereHours(long rawMicroampereHours) {
        long normalized = normalizeMicroampereHours(rawMicroampereHours);
        return normalized > 0L ? (int) Math.round(normalized / 1000d) : 0;
    }
}
