package com.ampere.batterylab;

import android.os.BatteryManager;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;

/** Reads Android current properties once and rejects sentinels, bad units and spikes. */
final class BatteryCurrent {
    private static final long MIN_MICROAMPS = 1_000L;
    private static final long MAX_MICROAMPS = 100_000_000L;

    private BatteryCurrent() { }

    static int milliAmps(BatteryManager manager) {
        if (manager != null) {
            try {
                int now = fromMicroamps(manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW));
                if (now > 0) return now;
                int average = fromMicroamps(manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE));
                if (average > 0) return average;
            } catch (RuntimeException ignored) {
                // A missing or blocked BatteryManager property is a normal
                // OEM variation; continue with the kernel-backed fallback.
            }
        }
        return fromSysfs();
    }

    /**
     * Applies the conservative OEM scale detector after the normal source
     * hierarchy has produced a magnitude. This keeps all callers on the same
     * validated source path while fixing the common 10x/100x/1000x mismatch.
     */
    static int milliAmps(BatteryManager manager, boolean charging, int batteryPercent) {
        int base = milliAmps(manager);
        if (base <= 0) return 0;
        int status = charging
                ? BatteryCurrentMultiplierDetector.STATUS_CHARGING
                : BatteryCurrentMultiplierDetector.STATUS_DISCHARGING;
        int multiplier = BatteryCurrentMultiplierDetector.detect(base, status, batteryPercent);
        long corrected = (long) base * multiplier;
        return corrected > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) corrected;
    }

    static int fromMicroamps(long raw) {
        long magnitude = Math.abs(raw);
        if (raw == Integer.MIN_VALUE || magnitude < MIN_MICROAMPS || magnitude > MAX_MICROAMPS) return 0;
        return (int) (magnitude / 1_000L);
    }

    /**
     * Reads the standard Linux power_supply current nodes when the Android
     * property is unavailable. Only battery/BMS-like supplies are considered;
     * USB input current is deliberately excluded because it is not battery
     * current and would distort sessions and battery-side power.
     */
    private static int fromSysfs() {
        try {
            File root = new File("/sys/class/power_supply");
            File[] supplies = root.listFiles();
            if (supplies == null) return 0;
            Arrays.sort(supplies, (left, right) -> Integer.compare(
                    BatterySupplyRules.rank(left), BatterySupplyRules.rank(right)));
            for (File supply : supplies) {
                if (!supply.isDirectory() || !BatterySupplyRules.isBatteryNode(supply)) continue;
                int now = fromMicroamps(readLong(new File(supply, "current_now")));
                if (now > 0) return now;
                int average = fromMicroamps(readLong(new File(supply, "current_avg")));
                if (average > 0) return average;
            }
        } catch (RuntimeException ignored) {
            // Treat inaccessible sysfs as an honest unavailable reading.
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
