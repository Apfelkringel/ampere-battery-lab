package com.ampere.batterylab;

import android.content.Intent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

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
            if (valid(value)) return new Reading(value, "Batterie-Treiber");
        }
        return null;
    }

    private static boolean valid(int value) { return value >= 0 && value <= MAX_CYCLES; }

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
