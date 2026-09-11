package com.ampere.batterylab;

import android.os.SystemClock;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.Locale;

/** Reads the Linux power-supply charging behaviour without writing to sysfs. */
final class BatteryChargeBehaviour {
    private static final long CACHE_MS = 30_000L;
    private static volatile long cachedAt;
    private static volatile File cachedFile;

    private BatteryChargeBehaviour() { }

    static Reading read() {
        long now = SystemClock.elapsedRealtime();
        if (cachedAt > 0L && now >= cachedAt && now - cachedAt < CACHE_MS) {
            Reading cached = fromText(readText(cachedFile), "Linux charge_behaviour (cached)");
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
                File behaviourFile = new File(supply, "charge_behaviour");
                Reading result = fromText(readText(behaviourFile),
                        "Linux charge_behaviour (" + supply.getName() + ")");
                if (result.isAvailable()) {
                    cachedFile = behaviourFile;
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

    static String normalize(String raw) {
        if (raw == null) return "";
        String value = raw.trim().toLowerCase(Locale.US);
        if (value.startsWith("[") && value.endsWith("]")) {
            value = value.substring(1, value.length() - 1).trim();
        }
        if ("auto".equals(value)) return "auto";
        if ("inhibit-charge".equals(value)) return "inhibit-charge";
        if ("inhibit-charge-awake".equals(value)) return "inhibit-charge-awake";
        if ("force-discharge".equals(value)) return "force-discharge";
        return "";
    }

    static String label(String behaviour) {
        String normalized = normalize(behaviour);
        if ("auto".equals(normalized)) return "Normalbetrieb";
        if ("inhibit-charge".equals(normalized)) return "Laden gesperrt";
        if ("inhibit-charge-awake".equals(normalized)) return "Laden im Wachzustand gesperrt";
        if ("force-discharge".equals(normalized)) return "Entladung erzwungen";
        return "Nicht verfügbar";
    }

    static Reading fromText(String raw, String source) {
        String behaviour = normalize(raw);
        return behaviour.length() > 0 ? new Reading(behaviour, source) : Reading.unavailable();
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
        final String behaviour;
        final String source;

        private Reading(String behaviour, String source) {
            this.behaviour = behaviour == null ? "" : behaviour;
            this.source = source == null ? "" : source;
        }

        static Reading unavailable() { return new Reading("", ""); }

        boolean isAvailable() { return behaviour.length() > 0; }

        String label() { return BatteryChargeBehaviour.label(behaviour); }
    }
}
