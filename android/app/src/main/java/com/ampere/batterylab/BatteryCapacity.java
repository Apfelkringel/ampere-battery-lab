package com.ampere.batterylab;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

/** Local battery design-capacity lookup with a user override and safe fallback. */
final class BatteryCapacity {
    private static final int FALLBACK_MAH = 4500;
    private static final int MIN_MAH = 500;
    private static final int MAX_MAH = 30000;
    private static int cachedDetectedMah = -1;

    private BatteryCapacity() { }

    static int designCapacityMah(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("ampere-data", Context.MODE_PRIVATE);
        int override = prefs.getInt("designCapacityMah", 0);
        if (valid(override)) return override;
        int detected = automaticCapacityMah();
        return valid(detected) ? detected : FALLBACK_MAH;
    }

    static boolean hasManualOverride(Context context) {
        int override = context.getSharedPreferences("ampere-data", Context.MODE_PRIVATE)
                .getInt("designCapacityMah", 0);
        return valid(override);
    }

    static boolean hasAutomaticValue() {
        return valid(automaticCapacityMah());
    }

    private static boolean valid(int mah) { return mah >= MIN_MAH && mah <= MAX_MAH; }

    private static int automaticCapacityMah() {
        if (cachedDetectedMah < 0) cachedDetectedMah = detectFromSysfs();
        return cachedDetectedMah;
    }

    private static int detectFromSysfs() {
        try {
            File root = new File("/sys/class/power_supply");
            File[] supplies = root.listFiles();
            if (supplies == null) return 0;
            for (File supply : supplies) {
                if (!supply.getName().toLowerCase(java.util.Locale.US).contains("battery")) continue;
                int charge = readInt(new File(supply, "charge_full_design"));
                if (valid(charge / 1000)) return charge / 1000;
                int energy = readInt(new File(supply, "energy_full_design"));
                int voltage = readInt(new File(supply, "voltage_max_design"));
                if (energy > 0 && voltage > 0) {
                    int estimated = Math.round(energy * 1000f / voltage);
                    if (valid(estimated)) return estimated;
                }
            }
        } catch (Exception ignored) { }
        return 0;
    }

    private static int readInt(File file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String value = reader.readLine();
            return value == null ? 0 : Integer.parseInt(value.trim());
        } catch (Exception ignored) {
            return 0;
        }
    }
}
