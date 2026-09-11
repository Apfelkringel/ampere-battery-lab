package com.ampere.batterylab;

import android.content.Intent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;

/** Reads a reported battery cycle count without inventing one when unavailable. */
final class BatteryCycleCount {
    private static final int MAX_CYCLES = 100000;

    private BatteryCycleCount() { }

    static final class Reading {
        final int cycles;
        final String source;

        Reading(int cycles, String source) {
            this.cycles = cycles;
            this.source = source;
        }
    }

    static Reading read(Intent battery) {
        if (battery != null) {
            int broadcast = battery.getIntExtra("android.os.extra.CYCLE_COUNT", -1);
            if (valid(broadcast)) return new Reading(broadcast, "Android-Batterie-API");
        }
        String[] paths = {
                "/sys/class/power_supply/battery/battery_cycle_count",
                "/sys/class/power_supply/battery/battery_cycle",
                "/sys/class/power_supply/battery/cycle_count",
                "/sys/class/power_supply/battery/cycle",
                "/sys/class/power_supply/battery/charge_cycles",
                "/sys/class/power_supply/Battery/cycle_count",
                "/sys/class/power_supply/bms/cycle_count",
                "/sys/class/power_supply/bms/charge_cycles",
                "/sys/class/power_supply/BMS/cycle_count",
                "/sys/class/power_supply/maxfg/cycle_count",
                "/sys/class/power_supply/max170xx/cycle_count",
                "/sys/class/power_supply/max170xx_battery/cycle_count",
                "/sys/class/power_supply/google-battery/cycle_count"
        };
        for (String path : paths) {
            int value = readInt(new File(path));
            if (isSysfsValue(value)) return new Reading(value, "Batterie-Treiber");
        }
        Reading scanned = scanPowerSupplyNodes();
        if (scanned != null) return scanned;
        return null;
    }

    static boolean isPlausible(int value) { return value >= 0 && value <= MAX_CYCLES; }

    /** Linux power_supply uses zero to mean that the cycle count is unavailable. */
    static boolean isSysfsValue(int value) { return value > 0 && value <= MAX_CYCLES; }

    private static boolean valid(int value) { return isPlausible(value); }

    private static Reading scanPowerSupplyNodes() {
        File root = new File("/sys/class/power_supply");
        File[] supplies = root.listFiles();
        if (supplies == null) return null;
        Arrays.sort(supplies, (left, right) -> Boolean.compare(!isBatteryNode(left), !isBatteryNode(right)));
        String[] names = {"cycle_count", "battery_cycle_count", "battery_cycle", "charge_cycles"};
        for (File supply : supplies) {
            if (!supply.isDirectory()) continue;
            for (String name : names) {
                int value = readInt(new File(supply, name));
                if (isSysfsValue(value)) return new Reading(value, "Batterie-Treiber");
            }
        }
        return null;
    }

    private static boolean isBatteryNode(File supply) {
        String name = supply.getName().toLowerCase(java.util.Locale.US);
        return name.contains("battery") || name.contains("bms") || name.contains("maxfg")
                || name.contains("max170") || name.contains("fuelgauge") || name.contains("fuel-gauge");
    }

    private static int readInt(File file) {
        try {
            if (!file.isFile() || !file.canRead()) return -1;
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String raw = reader.readLine();
                return raw == null ? -1 : Integer.parseInt(raw.trim());
            }
        } catch (Exception ignored) {
            return -1;
        }
    }
}
