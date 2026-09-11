package com.ampere.batterylab;

import android.content.Intent;
import android.os.BatteryManager;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;

/** Resolves battery voltage from Android first, then read-only battery sysfs. */
final class BatteryVoltage {
    private static final int MIN_MILLIVOLTS = 500;
    private static final int MAX_MILLIVOLTS = 20000;
    private static volatile File cachedSysfsFile;

    private BatteryVoltage() { }

    /** Returns a broadcast value when valid, otherwise a battery/BMS voltage_now fallback. */
    static int readMilliVolts(Intent battery) {
        int broadcast = battery == null ? 0 : normalizeMilliVolts(
                battery.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0));
        if (broadcast > 0) return broadcast;
        return fromSysfs();
    }

    /** Validates Android's millivolt value without inventing a fallback. */
    static int normalizeMilliVolts(int rawMilliVolts) {
        return rawMilliVolts >= MIN_MILLIVOLTS && rawMilliVolts <= MAX_MILLIVOLTS
                ? rawMilliVolts : 0;
    }

    /** Normalizes Linux voltage_now values, which may be reported in mV or µV. */
    static int normalizeSysfsVoltage(long raw) {
        if (raw <= 0L || raw > MAX_MILLIVOLTS * 1000L) return 0;
        long millivolts = raw > MAX_MILLIVOLTS ? raw / 1000L : raw;
        return millivolts >= MIN_MILLIVOLTS && millivolts <= MAX_MILLIVOLTS
                ? (int) millivolts : 0;
    }

    private static int fromSysfs() {
        File cached = cachedSysfsFile;
        if (cached != null) {
            int value = normalizeSysfsVoltage(readLong(cached));
            if (value > 0) return value;
            cachedSysfsFile = null;
        }
        try {
            File root = new File("/sys/class/power_supply");
            File[] supplies = root.listFiles();
            if (supplies == null) return 0;
            Arrays.sort(supplies, (left, right) -> Integer.compare(
                    BatterySupplyRules.rank(left), BatterySupplyRules.rank(right)));
            for (File supply : supplies) {
                if (!supply.isDirectory() || !BatterySupplyRules.isBatteryNode(supply)) continue;
                File voltage = new File(supply, "voltage_now");
                int value = normalizeSysfsVoltage(readLong(voltage));
                if (value > 0) {
                    cachedSysfsFile = voltage;
                    return value;
                }
            }
        } catch (RuntimeException ignored) {
            // Locked-down or OEM-specific sysfs remains an honest unavailable value.
        }
        return 0;
    }

    private static long readLong(File file) {
        try {
            if (!file.isFile() || !file.canRead()) return 0L;
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String value = reader.readLine();
                return value == null ? 0L : Long.parseLong(value.trim());
            }
        } catch (Exception ignored) {
            return 0L;
        }
    }
}
