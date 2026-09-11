package com.ampere.batterylab;

import android.os.SystemClock;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;

/** Reads optional OEM charge thresholds without writing or requiring root. */
final class BatteryChargeControl {
    private static final long CACHE_MS = 30_000L;
    private static final Object CACHE_LOCK = new Object();
    private static volatile long cachedAt;
    private static volatile Reading cached = Reading.unavailable();

    private BatteryChargeControl() { }

    static Reading read() {
        long now = SystemClock.elapsedRealtime();
        if (cachedAt > 0L && now >= cachedAt && now - cachedAt < CACHE_MS) return cached;
        synchronized (CACHE_LOCK) {
            long current = SystemClock.elapsedRealtime();
            if (cachedAt > 0L && current >= cachedAt && current - cachedAt < CACHE_MS) return cached;
            Reading result = scan();
            cached = result;
            cachedAt = current;
            return result;
        }
    }

    static int normalizeThreshold(long raw) {
        return raw >= 1L && raw <= 100L ? (int) raw : 0;
    }

    private static Reading scan() {
        try {
            File root = new File("/sys/class/power_supply");
            File[] supplies = root.listFiles();
            if (supplies == null) return Reading.unavailable();
            Arrays.sort(supplies, (left, right) -> Integer.compare(
                    BatterySupplyRules.rank(left), BatterySupplyRules.rank(right)));
            for (File supply : supplies) {
                if (!supply.isDirectory() || !BatterySupplyRules.isBatteryNode(supply)) continue;
                int end = normalizeThreshold(readLong(new File(supply,
                        "charge_control_end_threshold")));
                if (end <= 0) continue;
                int start = normalizeThreshold(readLong(new File(supply,
                        "charge_control_start_threshold")));
                return new Reading(end, start, supply.getName());
            }
        } catch (RuntimeException ignored) {
            // OEMs may expose the directory but deny individual attributes.
        }
        return Reading.unavailable();
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

    static final class Reading {
        final int endThresholdPercent;
        final int startThresholdPercent;
        final String source;

        Reading(int endThresholdPercent, int startThresholdPercent, String source) {
            this.endThresholdPercent = endThresholdPercent;
            this.startThresholdPercent = startThresholdPercent;
            this.source = source == null ? "" : source;
        }

        static Reading unavailable() { return new Reading(0, 0, ""); }

        boolean isAvailable() { return endThresholdPercent > 0; }

        String label() {
            if (!isAvailable()) return "";
            if (startThresholdPercent > 0 && startThresholdPercent <= endThresholdPercent) {
                return "OEM-Ladefenster " + startThresholdPercent + "–" + endThresholdPercent + "%";
            }
            return "OEM-Limit " + endThresholdPercent + "%";
        }
    }
}
