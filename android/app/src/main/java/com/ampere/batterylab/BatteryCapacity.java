package com.ampere.batterylab;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Locale;

/**
 * Local battery design-capacity lookup with an explicit unavailable state.
 *
 * Android/OEMs expose the nominal capacity through different layers. Keep the
 * order deterministic and retain the source label so a displayed number is
 * never mistaken for a measured health result.
 */
final class BatteryCapacity {
    private static final int MIN_MAH = 500;
    private static final int MAX_MAH = 30000;
    private static final long CACHE_REFRESH_MS = 15L * 60L * 1000L;
    private static volatile Reading cachedReading;
    private static volatile Reading cachedFullChargeReading;
    private static volatile long cachedReadingAt;
    private static volatile long cachedFullChargeReadingAt;

    private BatteryCapacity() { }

    static final class Reading {
        final int mah;
        final String source;

        Reading(int mah, String source) {
            this.mah = mah;
            this.source = source;
        }

        boolean isAvailable() { return valid(mah); }
    }

    static int designCapacityMah(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("ampere-data", Context.MODE_PRIVATE);
        int override = prefs.getInt("designCapacityMah", 0);
        if (valid(override)) return override;
        Reading detected = automaticReading(context);
        return detected.isAvailable() ? detected.mah : 0;
    }

    static boolean hasManualOverride(Context context) {
        int override = context.getSharedPreferences("ampere-data", Context.MODE_PRIVATE)
                .getInt("designCapacityMah", 0);
        return valid(override);
    }

    static boolean hasAutomaticValue(Context context) {
        return automaticReading(context).isAvailable();
    }

    static String automaticSource(Context context) {
        Reading reading = automaticReading(context);
        return reading.isAvailable() ? reading.source : "Nicht verfügbar";
    }

    /** Current full-charge capacity reported by an OEM battery driver, if exposed. */
    static int fullChargeCapacityMah(Context context) {
        Reading reading = automaticFullChargeReading();
        return reading.isAvailable() ? reading.mah : 0;
    }

    static String fullChargeCapacitySource(Context context) {
        Reading reading = automaticFullChargeReading();
        return reading.isAvailable() ? reading.source : "Nicht verfügbar";
    }

    private static boolean valid(int mah) { return mah >= MIN_MAH && mah <= MAX_MAH; }

    private static Reading automaticReading(Context context) {
        Reading cached = cachedReading;
        long now = SystemClock.elapsedRealtime();
        if (cached != null && isCacheFresh(cachedReadingAt, now)) return cached;
        synchronized (BatteryCapacity.class) {
            now = SystemClock.elapsedRealtime();
            if (cachedReading == null || !isCacheFresh(cachedReadingAt, now)) {
                cachedReading = detect(context);
                cachedReadingAt = now;
            }
            return cachedReading;
        }
    }

    private static Reading detect(Context context) {
        try {
            File root = new File("/sys/class/power_supply");
            File[] supplies = root.listFiles();
            if (supplies != null) {
                // Prefer the battery/BMS nodes, then inspect every OEM node.
                java.util.Arrays.sort(supplies, (left, right) -> {
                    boolean leftBattery = isBatteryNode(left);
                    boolean rightBattery = isBatteryNode(right);
                    return Boolean.compare(!leftBattery, !rightBattery);
                });
                for (File supply : supplies) {
                    if (!supply.isDirectory()) continue;
                    Reading charge = readChargeCapacity(supply);
                    if (charge != null) return charge;
                    Reading energy = readEnergyCapacity(supply);
                    if (energy != null) return energy;
                }
            }
        } catch (Exception ignored) { }

        // OEM power-profile XML is a nominal value, not a health measurement.
        Reading profile = readPowerProfile(context);
        if (profile != null) return profile;
        return new Reading(0, "Nicht verfügbar");
    }

    private static Reading automaticFullChargeReading() {
        Reading cached = cachedFullChargeReading;
        long now = SystemClock.elapsedRealtime();
        if (cached != null && isCacheFresh(cachedFullChargeReadingAt, now)) return cached;
        synchronized (BatteryCapacity.class) {
            now = SystemClock.elapsedRealtime();
            if (cachedFullChargeReading == null || !isCacheFresh(cachedFullChargeReadingAt, now)) {
                // Full-charge capacity is learned by the fuel gauge and can
                // change after a charge cycle; do not freeze it for the
                // lifetime of the foreground monitor process.
                cachedFullChargeReading = detectFullCharge();
                cachedFullChargeReadingAt = now;
            }
            return cachedFullChargeReading;
        }
    }

    static boolean isCacheFresh(long cachedAt, long now) {
        return cachedAt > 0L && now >= cachedAt && now - cachedAt < CACHE_REFRESH_MS;
    }

    private static Reading detectFullCharge() {
        try {
            File root = new File("/sys/class/power_supply");
            File[] supplies = root.listFiles();
            if (supplies != null) {
                java.util.Arrays.sort(supplies, (left, right) -> {
                    boolean leftBattery = isBatteryNode(left);
                    boolean rightBattery = isBatteryNode(right);
                    return Boolean.compare(!leftBattery, !rightBattery);
                });
                for (File supply : supplies) {
                    if (!supply.isDirectory()) continue;
                    Reading reading = readFullChargeCapacity(supply);
                    if (reading != null) return reading;
                }
            }
        } catch (Exception ignored) { }
        return new Reading(0, "Nicht verfügbar");
    }

    private static boolean isBatteryNode(File supply) {
        String name = supply.getName().toLowerCase(Locale.US);
        return name.contains("battery") || name.contains("bms") || name.contains("maxfg") || name.contains("max170");
    }

    private static Reading readChargeCapacity(File supply) {
        String[] names = {
                "charge_full_design", "charge_full_design_uah", "full_design_capacity",
                "nominal_full_capacity", "design_capacity"
        };
        for (String name : names) {
            long raw = readLong(new File(supply, name));
            if (raw <= 0) continue;
            // Linux power_supply nodes are normally µAh, while some OEM nodes
            // already expose mAh. Normalize by magnitude so both forms work.
            long mah = normalizeCapacity(raw);
            if (validLong(mah)) return new Reading((int) mah, "Batterie-Treiber");
        }
        return null;
    }

    private static Reading readFullChargeCapacity(File supply) {
        String[] names = {
                "charge_full", "charge_full_uah", "full_charge_capacity",
                "full_capacity", "battery_full_capacity"
        };
        for (String name : names) {
            long raw = readLong(new File(supply, name));
            if (raw <= 0) continue;
            long mah = normalizeCapacity(raw);
            if (validLong(mah)) return new Reading((int) mah, "Batterie-Treiber (Full Charge)");
        }
        return null;
    }

    private static Reading readEnergyCapacity(File supply) {
        long energy = readLong(new File(supply, "energy_full_design"));
        long voltage = readLong(new File(supply, "voltage_max_design"));
        if (energy <= 0 || voltage <= 0) return null;
        // energy is usually µWh and voltage µV: µWh / V = mWh = mAh at 1 V.
        long mah = Math.round(energy * 1000d / voltage);
        return validLong(mah) ? new Reading((int) mah, "Batterie-Treiber (Energie/Spannung)") : null;
    }

    private static Reading readPowerProfile(Context context) {
        if (context == null) return null;
        try {
            Class<?> profileClass = Class.forName("com.android.internal.os.PowerProfile");
            Object profile = profileClass.getConstructor(Context.class).newInstance(context);
            Object value = profileClass.getMethod("getBatteryCapacity").invoke(profile);
            if (!(value instanceof Number)) return null;
            long mah = Math.round(((Number) value).doubleValue());
            return validLong(mah) ? new Reading((int) mah, "Android-PowerProfile") : null;
        } catch (Throwable ignored) {
            // Hidden API access is blocked on some builds; unavailable is valid.
            return null;
        }
    }

    static long normalizeCapacity(long raw) {
        return raw > 100000L ? raw / 1000L : raw;
    }

    private static boolean validLong(long mah) {
        return mah >= MIN_MAH && mah <= MAX_MAH;
    }

    private static long readLong(File file) {
        try {
            if (!file.isFile() || !file.canRead()) return 0;
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String value = reader.readLine();
                return value == null ? 0 : Long.parseLong(value.trim());
            }
        } catch (Exception ignored) {
            return 0;
        }
    }

}
