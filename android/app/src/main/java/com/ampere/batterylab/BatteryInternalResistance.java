package com.ampere.batterylab;

import android.os.SystemClock;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.Locale;

/** Reads the optional Linux battery ESR node without root or write access. */
final class BatteryInternalResistance {
    private static final long MIN_MICRO_OHMS = 100L;
    private static final long MAX_MICRO_OHMS = 10_000_000L;
    private static final long CACHE_MS = 30_000L;
    private static volatile long cachedAt;
    private static volatile File cachedFile;

    private BatteryInternalResistance() { }

    static Reading read() {
        long now = SystemClock.elapsedRealtime();
        if (cachedAt > 0L && now >= cachedAt && now - cachedAt < CACHE_MS) {
            Reading cached = fromRaw(readLong(cachedFile), "Linux internal_resistance");
            if (cached.isAvailable()) return cached;
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
                File resistance = new File(supply, "internal_resistance");
                Reading result = fromRaw(readLong(resistance),
                        "Linux internal_resistance (" + supply.getName() + ")");
                if (result.isAvailable()) {
                    cachedFile = resistance;
                    cachedAt = now;
                    return result;
                }
            }
        } catch (RuntimeException ignored) {
            // Locked-down or OEM-specific sysfs remains honestly unavailable.
        }
        cachedAt = now;
        return Reading.unavailable();
    }

    static int normalizeMilliOhms(long microOhms) {
        if (microOhms < MIN_MICRO_OHMS || microOhms > MAX_MICRO_OHMS) return 0;
        long milliOhms = Math.round(microOhms / 1000d);
        return milliOhms >= 1L && milliOhms <= MAX_MICRO_OHMS / 1000L
                ? (int) milliOhms : 0;
    }

    static Reading fromRaw(long microOhms, String source) {
        int milliOhms = normalizeMilliOhms(microOhms);
        return milliOhms > 0 ? new Reading(microOhms, milliOhms, source)
                : Reading.unavailable();
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

    static final class Reading {
        final long microOhms;
        final int milliOhms;
        final String source;

        private Reading(long microOhms, int milliOhms, String source) {
            this.microOhms = microOhms;
            this.milliOhms = milliOhms;
            this.source = source == null ? "" : source;
        }

        static Reading unavailable() { return new Reading(0L, 0, ""); }

        boolean isAvailable() { return milliOhms > 0; }

        String label() {
            if (!isAvailable()) return "—";
            if (microOhms % 1000L == 0L) return milliOhms + " mΩ";
            return String.format(Locale.GERMANY, "%.1f mΩ", microOhms / 1000f);
        }
    }
}
