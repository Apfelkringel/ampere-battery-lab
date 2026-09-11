package com.ampere.batterylab;

import android.os.SystemClock;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;

/** Reads a complete Linux power-supply battery manufacture date read-only. */
final class BatteryManufactureDate {
    private static final long CACHE_MS = 30_000L;
    private static volatile long cachedAt;
    private static volatile Reading cached = Reading.unavailable();

    private BatteryManufactureDate() { }

    static Reading read() {
        long now = SystemClock.elapsedRealtime();
        if (cachedAt > 0L && now >= cachedAt && now - cachedAt < CACHE_MS) return cached;
        Reading result = scan();
        cached = result;
        cachedAt = now;
        return result;
    }

    static Reading fromParts(long year, long month, long day, String source) {
        if (!isValidDate(year, month, day)) return Reading.unavailable();
        return new Reading((int) year, (int) month, (int) day, source);
    }

    static boolean isValidDate(long year, long month, long day) {
        if (year < 1970L || year > 2100L || month < 1L || month > 12L || day < 1L) return false;
        int maxDay = daysInMonth((int) year, (int) month);
        return day <= maxDay;
    }

    private static int daysInMonth(int year, int month) {
        switch (month) {
            case 2:
                return ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0) ? 29 : 28;
            case 4:
            case 6:
            case 9:
            case 11:
                return 30;
            default:
                return 31;
        }
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
                long year = readLong(new File(supply, "manufacture_year"));
                long month = readLong(new File(supply, "manufacture_month"));
                long day = readLong(new File(supply, "manufacture_day"));
                Reading result = fromParts(year, month, day,
                        "Linux manufacture date (" + supply.getName() + ")");
                if (result.isAvailable()) return result;
            }
        } catch (RuntimeException ignored) {
            // Locked-down or OEM-specific sysfs remains honestly unavailable.
        }
        return Reading.unavailable();
    }

    private static long readLong(File file) {
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
        final int year;
        final int month;
        final int day;
        final String source;

        private Reading(int year, int month, int day, String source) {
            this.year = year;
            this.month = month;
            this.day = day;
            this.source = source == null ? "" : source;
        }

        static Reading unavailable() { return new Reading(0, 0, 0, ""); }

        boolean isAvailable() { return isValidDate(year, month, day); }

        String label() {
            return isAvailable() ? String.format(java.util.Locale.US, "%04d-%02d-%02d", year, month, day) : "—";
        }
    }
}
