package com.ampere.batterylab;

import android.os.SystemClock;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;

/** Reads the optional fuel-gauge capacity error margin without root or writes. */
final class BatteryCapacityErrorMargin {
    private static final long CACHE_MS = 30_000L;
    private static volatile long cachedAt;
    private static volatile File cachedFile;

    private BatteryCapacityErrorMargin() { }

    static Reading read() {
        long now = SystemClock.elapsedRealtime();
        if (cachedAt > 0L && now >= cachedAt && now - cachedAt < CACHE_MS) {
            int cached = normalize(readLong(cachedFile));
            if (cached >= 0) return new Reading(cached, "Linux capacity_error_margin (cached)");
            cachedFile = null;
        }
        try {
            File root = new File("/sys/class/power_supply");
            File[] supplies = root.listFiles();
            if (supplies == null) return Reading.unavailable();
            Arrays.sort(supplies, (left, right) -> Integer.compare(
                    BatterySupplyRules.rank(left), BatterySupplyRules.rank(right)));
            for (File supply : supplies) {
                if (!supply.isDirectory() || !BatterySupplyRules.isBatteryNode(supply)) continue;
                File margin = new File(supply, "capacity_error_margin");
                int value = normalize(readLong(margin));
                if (value >= 0) {
                    cachedFile = margin;
                    cachedAt = now;
                    return new Reading(value,
                            "Linux capacity_error_margin (" + supply.getName() + ")");
                }
            }
        } catch (RuntimeException ignored) {
            // Locked-down or OEM-specific sysfs remains honestly unavailable.
        }
        cachedAt = now;
        return Reading.unavailable();
    }

    /** Returns -1 for unavailable/out-of-range values; 0 is a valid calibrated margin. */
    static int normalize(long rawPercent) {
        return rawPercent >= 0L && rawPercent <= 100L ? (int) rawPercent : -1;
    }

    static Reading fromRaw(long rawPercent, String source) {
        int value = normalize(rawPercent);
        return value >= 0 ? new Reading(value, source) : Reading.unavailable();
    }

    private static long readLong(File file) {
        if (file == null) return Long.MIN_VALUE;
        try {
            if (!file.isFile() || !file.canRead()) return Long.MIN_VALUE;
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String value = reader.readLine();
                return value == null ? Long.MIN_VALUE : Long.parseLong(value.trim());
            }
        } catch (Exception ignored) {
            return Long.MIN_VALUE;
        }
    }

    static final class Reading {
        final int percent;
        final String source;

        private Reading(int percent, String source) {
            this.percent = percent;
            this.source = source == null ? "" : source;
        }

        static Reading unavailable() { return new Reading(-1, ""); }

        boolean isAvailable() { return percent >= 0; }

        String label() { return isAvailable() ? "±" + percent + " %" : "—"; }
    }
}
