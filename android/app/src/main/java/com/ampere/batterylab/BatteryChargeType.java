package com.ampere.batterylab;

import android.os.SystemClock;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.Locale;

/** Reads the Linux power-supply charge algorithm without writing to sysfs. */
final class BatteryChargeType {
    private static final long CACHE_MS = 30_000L;
    private static volatile long cachedAt;
    private static volatile File cachedTypeFile;

    private BatteryChargeType() { }

    static Reading read() {
        long now = SystemClock.elapsedRealtime();
        if (cachedAt > 0L && now >= cachedAt && now - cachedAt < CACHE_MS) {
            Reading cached = fromText(readText(cachedTypeFile), "Linux charge_type (cached)");
            if (cached.isAvailable()) return cached;
            cachedTypeFile = null;
        }
        try {
            File root = new File("/sys/class/power_supply");
            File[] supplies = root.listFiles();
            if (supplies == null) return Reading.unavailable();
            Arrays.sort(supplies, (left, right) -> Integer.compare(
                    BatterySupplyRules.rank(left), BatterySupplyRules.rank(right)));
            for (File supply : supplies) {
                if (!supply.isDirectory() || !BatterySupplyRules.isBatteryNode(supply)) continue;
                File typeFile = new File(supply, "charge_type");
                Reading result = fromText(readText(typeFile),
                        "Linux charge_type (" + supply.getName() + ")");
                if (!result.isAvailable()) {
                    File typesFile = new File(supply, "charge_types");
                    result = fromTypes(readText(typesFile),
                            "Linux charge_types (" + supply.getName() + ")");
                    if (result.isAvailable()) typeFile = typesFile;
                }
                if (result.isAvailable()) {
                    cachedTypeFile = typeFile;
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

    static int normalize(String raw) {
        if (raw == null) return 0;
        String value = raw.trim().toLowerCase(Locale.US).replace('_', ' ');
        if (value.startsWith("[") && value.endsWith("]")) {
            value = value.substring(1, value.length() - 1).trim();
        }
        if ("trickle".equals(value)) return 1;
        if ("fast".equals(value)) return 2;
        if ("standard".equals(value)) return 3;
        if ("adaptive".equals(value)) return 4;
        if ("custom".equals(value)) return 5;
        if ("long life".equals(value)) return 6;
        if ("bypass".equals(value)) return 7;
        return 0;
    }

    static String label(int type) {
        switch (type) {
            case 1: return "Trickle";
            case 2: return "Fast";
            case 3: return "Standard";
            case 4: return "Adaptiv";
            case 5: return "Benutzerdefiniert";
            case 6: return "Long Life";
            case 7: return "Bypass";
            default: return "Nicht verfügbar";
        }
    }

    static Reading fromText(String raw, String source) {
        int type = normalize(raw);
        return type > 0 ? new Reading(type, source) : Reading.unavailable();
    }

    static Reading fromTypes(String raw, String source) {
        if (raw == null) return Reading.unavailable();
        int open = raw.indexOf('[');
        int close = raw.indexOf(']', open + 1);
        if (open < 0 || close <= open) return Reading.unavailable();
        return fromText(raw.substring(open + 1, close), source);
    }

    private static String readText(File file) {
        if (file == null) return null;
        try {
            if (!file.isFile() || !file.canRead()) return null;
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String value = reader.readLine();
                return value == null ? null : value.trim();
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    static final class Reading {
        final int type;
        final String source;

        private Reading(int type, String source) {
            this.type = type;
            this.source = source == null ? "" : source;
        }

        static Reading unavailable() { return new Reading(0, ""); }

        boolean isAvailable() { return type > 0; }

        String label() { return BatteryChargeType.label(type); }
    }
}
