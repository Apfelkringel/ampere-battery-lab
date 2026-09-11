package com.ampere.batterylab;

import android.content.Intent;
import android.os.SystemClock;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
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
    private static final long SYSFS_CACHE_MS = 30_000L;
    private static volatile long cachedSysfsAt;
    private static volatile File cachedSysfsCurrentFile;

    private BatteryChargerCapability() { }

    static Reading read(Intent battery) {
        Reading android = battery == null ? Reading.empty() : Reading.fromRaw(
                battery.getIntExtra(MAX_CURRENT, -1),
                battery.getIntExtra(MAX_VOLTAGE, -1));
        if (android.isAvailable()) return android;
        return fromSysfs();
    }

    static int maxPowerMilliwatts(Intent battery) {
        return read(battery).maxPowerMilliwatts;
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

    private static Reading fromSysfs() {
        long now = SystemClock.elapsedRealtime();
        if (cachedSysfsAt > 0L && now >= cachedSysfsAt && now - cachedSysfsAt < SYSFS_CACHE_MS) {
            Reading cached = fromHardwareCurrent(readLong(cachedSysfsCurrentFile));
            if (cached.isAvailable()) return cached;
            cachedSysfsCurrentFile = null;
        }
        try {
            File root = new File("/sys/class/power_supply");
            File[] supplies = root.listFiles();
            if (supplies == null) return Reading.empty();
            Arrays.sort(supplies, (left, right) -> Integer.compare(
                    BatterySupplyRules.rank(left), BatterySupplyRules.rank(right)));
            for (File supply : supplies) {
                if (!supply.isDirectory() || !BatterySupplyRules.isBatteryNode(supply)) continue;
                File current = new File(supply, "constant_charge_current_max");
                Reading result = fromHardwareCurrent(readLong(current));
                if (result.isAvailable()) {
                    cachedSysfsCurrentFile = current;
                    cachedSysfsAt = now;
                    return result;
                }
            }
        } catch (RuntimeException ignored) {
            // Locked-down or OEM-specific sysfs remains unavailable.
        }
        cachedSysfsAt = now;
        return Reading.empty();
    }

    private static Reading fromHardwareCurrent(long microamps) {
        return Reading.fromHardwareCurrentLimit(microamps);
    }

    private static long readLong(File file) {
        if (file == null) return 0L;
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

    /** Validated, display-ready charger capability values from one battery broadcast. */
    static final class Reading {
        final int maxCurrentMa;
        final int maxVoltageMv;
        final int maxPowerMilliwatts;
        final String source;

        private Reading(int maxCurrentMa, int maxVoltageMv, int maxPowerMilliwatts, String source) {
            this.maxCurrentMa = maxCurrentMa;
            this.maxVoltageMv = maxVoltageMv;
            this.maxPowerMilliwatts = maxPowerMilliwatts;
            this.source = source == null ? "" : source;
        }

        static Reading empty() { return new Reading(0, 0, 0, ""); }

        static Reading fromRaw(long currentMicroamps, long voltageMicrovolts) {
            int currentMa = validCurrent(currentMicroamps)
                    ? (int) Math.round(currentMicroamps / 1000d) : 0;
            int voltageMv = validVoltage(voltageMicrovolts)
                    ? (int) Math.round(voltageMicrovolts / 1000d) : 0;
            return new Reading(currentMa, voltageMv,
                    maxPowerMilliwatts(currentMicroamps, voltageMicrovolts), "Android-Broadcast");
        }

        static Reading fromHardwareCurrentLimit(long currentMicroamps) {
            if (!validCurrent(currentMicroamps)) return empty();
            return new Reading((int) Math.round(currentMicroamps / 1000d), 0, 0,
                    "Sysfs constant_charge_current_max");
        }

        boolean isAvailable() {
            return maxCurrentMa > 0 || maxVoltageMv > 0 || maxPowerMilliwatts > 0;
        }

        String label() {
            if (!isAvailable()) return "";
            StringBuilder value = new StringBuilder(
                    source.startsWith("Sysfs") ? "Ladehardware max. " : "Ladegerät max. ");
            boolean separator = false;
            if (maxCurrentMa > 0) {
                value.append(String.format(Locale.GERMANY, "%.2f A", maxCurrentMa / 1000f));
                separator = true;
            }
            if (maxVoltageMv > 0) {
                if (separator) value.append(" · ");
                value.append(String.format(Locale.GERMANY, "%.2f V", maxVoltageMv / 1000f));
                separator = true;
            }
            if (maxPowerMilliwatts > 0) {
                if (separator) value.append(" · ");
                value.append(String.format(Locale.GERMANY, "%.1f W", maxPowerMilliwatts / 1000f));
            }
            return value.toString();
        }

        private static boolean validCurrent(long microamps) {
            return microamps >= MIN_CURRENT_UA && microamps <= MAX_CURRENT_UA;
        }

        private static boolean validVoltage(long microvolts) {
            return microvolts >= MIN_VOLTAGE_UV && microvolts <= MAX_VOLTAGE_UV;
        }
    }
}
