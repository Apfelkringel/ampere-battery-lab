package com.ampere.batterylab;

import android.os.SystemClock;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;

/** Reads optional kernel fuel-gauge time estimates without requiring root. */
final class BatteryFuelGaugeTime {
    private static final long MAX_SECONDS = 48L * 60L * 60L;
    private static final long CACHE_MS = 30_000L;
    private static final Object CACHE_LOCK = new Object();
    private static volatile long cachedAt;
    private static volatile int cachedToEmptyMinutes;
    private static volatile int cachedToFullMinutes;

    private BatteryFuelGaugeTime() { }

    static int readMinutes(boolean charging) {
        refreshIfNeeded();
        return charging ? cachedToFullMinutes : cachedToEmptyMinutes;
    }

    /** Converts the fuel-gauge's seconds value, rejecting sentinels and long nonsense. */
    static int normalizeSeconds(long seconds) {
        if (seconds <= 0L || seconds > MAX_SECONDS) return 0;
        long minutes = seconds / 60L;
        return minutes > 0L ? (int) minutes : 0;
    }

    /** Prefers a valid instantaneous estimate and falls back to the averaged estimate. */
    static int fullMinutes(long nowSeconds, long averageSeconds) {
        int now = normalizeSeconds(nowSeconds);
        return now > 0 ? now : normalizeSeconds(averageSeconds);
    }

    private static void refreshIfNeeded() {
        long now = SystemClock.elapsedRealtime();
        if (cachedAt > 0L && now >= cachedAt && now - cachedAt < CACHE_MS) return;
        synchronized (CACHE_LOCK) {
            long current = SystemClock.elapsedRealtime();
            if (cachedAt > 0L && current >= cachedAt && current - cachedAt < CACHE_MS) return;
            int emptyMinutes = 0;
            int fullMinutes = 0;
            try {
                File root = new File("/sys/class/power_supply");
                File[] supplies = root.listFiles();
                if (supplies != null) {
                    Arrays.sort(supplies, (left, right) -> Integer.compare(
                            BatterySupplyRules.rank(left), BatterySupplyRules.rank(right)));
                    for (File supply : supplies) {
                        if (!supply.isDirectory() || !BatterySupplyRules.isBatteryNode(supply)) continue;
                        if (emptyMinutes <= 0) {
                            emptyMinutes = normalizeSeconds(readLong(
                                    new File(supply, "time_to_empty_avg")));
                        }
                        if (fullMinutes <= 0) {
                            fullMinutes = fullMinutes(
                                    readLong(new File(supply, "time_to_full_now")),
                                    readLong(new File(supply, "time_to_full_avg")));
                        }
                        if (emptyMinutes > 0 && fullMinutes > 0) break;
                    }
                }
            } catch (RuntimeException ignored) {
                // Locked-down or OEM-specific sysfs remains an honest unavailable value.
            }
            cachedToEmptyMinutes = emptyMinutes;
            cachedToFullMinutes = fullMinutes;
            cachedAt = current;
        }
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
