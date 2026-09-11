package com.ampere.batterylab;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Locale;

/** Ranks battery-side Linux power-supply nodes without mistaking USB input for a cell. */
final class BatterySupplyRules {
    static final int UNSUPPORTED = Integer.MAX_VALUE;

    private BatterySupplyRules() { }

    static boolean isBatteryNode(File supply) {
        return rank(supply) != UNSUPPORTED;
    }

    static int rank(File supply) {
        if (supply == null || !supply.isDirectory()) return UNSUPPORTED;
        String name = supply.getName().toLowerCase(Locale.US);
        String declaredType = readText(new File(supply, "type"));
        if (declaredType == null) declaredType = readPowerSupplyType(new File(supply, "uevent"));
        return rank(name, declaredType);
    }

    /** Pure form used by tests and shared by all readers. */
    static int rank(String supplyName, String declaredType) {
        String name = supplyName == null ? "" : supplyName.toLowerCase(Locale.US);
        String type = declaredType == null ? "" : declaredType.trim().toLowerCase(Locale.US);
        boolean batteryType = "battery".equals(type);
        boolean fuelGaugeType = "bms".equals(type) || "unknown".equals(type);
        // A declared non-battery type wins over a misleading directory name.
        if (!type.isEmpty() && !batteryType && !fuelGaugeType) return UNSUPPORTED;
        if ("battery".equals(name)) return 0;
        if (batteryType) return 1;
        if ("bms".equals(name)) return 2;
        if (name.contains("battery")) return 3;
        if (name.contains("fuelgauge") || name.contains("fuel-gauge")) return 4;
        if (name.endsWith("_fg") || name.endsWith("-fg")) return 5;
        if (name.contains("maxfg") || name.contains("max170") || name.contains("bms")) return 6;
        return UNSUPPORTED;
    }

    private static String readPowerSupplyType(File uevent) {
        String contents = readText(uevent);
        if (contents == null) return null;
        for (String line : contents.split("\\n")) {
            if (line.startsWith("POWER_SUPPLY_TYPE=")) {
                String value = line.substring("POWER_SUPPLY_TYPE=".length()).trim();
                return value.isEmpty() ? null : value;
            }
        }
        return null;
    }

    private static String readText(File file) {
        try {
            if (!file.isFile() || !file.canRead()) return null;
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    if (result.length() > 0) result.append('\n');
                    result.append(line);
                }
                return result.toString().trim();
            }
        } catch (Exception ignored) {
            return null;
        }
    }
}
