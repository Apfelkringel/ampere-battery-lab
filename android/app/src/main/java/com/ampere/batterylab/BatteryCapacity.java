package com.ampere.batterylab;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.SystemClock;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

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
    private static volatile PercentReading cachedStateOfHealthReading;
    private static volatile long cachedReadingAt;
    private static volatile long cachedFullChargeReadingAt;
    private static volatile long cachedStateOfHealthReadingAt;

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
        Reading reading = automaticFullChargeReading(context);
        return reading.isAvailable() ? reading.mah : 0;
    }

    static String fullChargeCapacitySource(Context context) {
        Reading reading = automaticFullChargeReading(context);
        return reading.isAvailable() ? reading.source : "Nicht verfügbar";
    }

    /** Reads the standard read-only OEM state-of-health node, if exposed. */
    static int stateOfHealthPercent() {
        PercentReading reading = automaticStateOfHealthReading();
        return reading.isAvailable() ? reading.percent : 0;
    }

    static String stateOfHealthSource() {
        PercentReading reading = automaticStateOfHealthReading();
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
                    return Integer.compare(BatterySupplyRules.rank(left), BatterySupplyRules.rank(right));
                });
                for (File supply : supplies) {
                    if (!supply.isDirectory() || !BatterySupplyRules.isBatteryNode(supply)) continue;
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

    private static Reading automaticFullChargeReading(Context context) {
        Reading cached = cachedFullChargeReading;
        long now = SystemClock.elapsedRealtime();
        if (cached != null && isCacheFresh(cachedFullChargeReadingAt, now)) return cached;
        synchronized (BatteryCapacity.class) {
            now = SystemClock.elapsedRealtime();
            if (cachedFullChargeReading == null || !isCacheFresh(cachedFullChargeReadingAt, now)) {
                // Full-charge capacity is learned by the fuel gauge and can
                // change after a charge cycle; do not freeze it for the
                // lifetime of the foreground monitor process.
                cachedFullChargeReading = detectFullCharge(context);
                cachedFullChargeReadingAt = now;
            }
            return cachedFullChargeReading;
        }
    }

    private static PercentReading automaticStateOfHealthReading() {
        PercentReading cached = cachedStateOfHealthReading;
        long now = SystemClock.elapsedRealtime();
        if (cached != null && isCacheFresh(cachedStateOfHealthReadingAt, now)) return cached;
        synchronized (BatteryCapacity.class) {
            now = SystemClock.elapsedRealtime();
            if (cachedStateOfHealthReading == null || !isCacheFresh(cachedStateOfHealthReadingAt, now)) {
                cachedStateOfHealthReading = detectStateOfHealth();
                cachedStateOfHealthReadingAt = now;
            }
            return cachedStateOfHealthReading;
        }
    }

    static boolean isCacheFresh(long cachedAt, long now) {
        return cachedAt > 0L && now >= cachedAt && now - cachedAt < CACHE_REFRESH_MS;
    }

    private static Reading detectFullCharge(Context context) {
        try {
            File root = new File("/sys/class/power_supply");
            File[] supplies = root.listFiles();
            if (supplies != null) {
                java.util.Arrays.sort(supplies, (left, right) -> {
                    return Integer.compare(BatterySupplyRules.rank(left), BatterySupplyRules.rank(right));
                });
                for (File supply : supplies) {
                    if (!supply.isDirectory() || !BatterySupplyRules.isBatteryNode(supply)) continue;
                    Reading reading = readFullChargeCapacity(supply);
                    if (reading != null) return reading;
                }
            }
        } catch (Exception ignored) { }
        Reading oplus = readVendorFullChargeCapacity();
        if (oplus != null) return oplus;
        Reading counterEstimate = readChargeCounterEstimate(context);
        if (counterEstimate != null) return counterEstimate;
        return new Reading(0, "Nicht verfügbar");
    }

    /**
     * Last-resort estimate used when no fuel-gauge full-capacity node exists.
     * The Android charge counter is remaining charge in µAh; dividing it by
     * the current level fraction gives an approximate full-charge value.
     */
    private static Reading readChargeCounterEstimate(Context context) {
        if (context == null) return null;
        try {
            Intent battery = context.registerReceiver(null,
                    new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (battery == null) return null;
            int rawLevel = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
            BatteryManager manager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
            int estimate = BatteryFullChargeEstimate.fromCounter(
                    BatteryChargeCounter.readMicroampereHours(manager), rawLevel, scale);
            return estimate > 0 ? new Reading(estimate, "Android-Charge-Counter (geschätzt)") : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static PercentReading detectStateOfHealth() {
        try {
            File root = new File("/sys/class/power_supply");
            File[] supplies = root.listFiles();
            if (supplies != null) {
                java.util.Arrays.sort(supplies, (left, right) -> {
                    return Integer.compare(BatterySupplyRules.rank(left), BatterySupplyRules.rank(right));
                });
                for (File supply : supplies) {
                    if (!supply.isDirectory() || !BatterySupplyRules.isBatteryNode(supply)) continue;
                    PercentReading reading = readStateOfHealth(supply);
                    if (reading != null) return reading;
                }
            }
        } catch (Exception ignored) { }
        // OnePlus/Oppo/Realme devices often expose their learned FCC/SoH in
        // the vendor charger tree instead of /sys/class/power_supply. The
        // files are read-only; if the OEM path is hidden by Android, the
        // result remains unavailable rather than being guessed.
        PercentReading oplus = readVendorStateOfHealth();
        if (oplus != null) return oplus;
        return new PercentReading(0, "Nicht verfügbar");
    }

    private static PercentReading readStateOfHealth(File supply) {
        // state_of_health is the standard Linux power_supply spelling. The
        // aliases cover common OEM fuel-gauge nodes. Samsung commonly exposes
        // its aging ratio as fg_asoc; qualitative "health" is intentionally
        // excluded because it means Good/Dead/etc., not a percentage.
        String[] names = {"state_of_health", "battery_state_of_health", "soh", "battery_soh", "fg_asoc"};
        for (String name : names) {
            long raw = readLong(new File(supply, name));
            int percent = normalizeStateOfHealth(raw);
            if (percent > 0) {
                String source = "fg_asoc".equals(name)
                        ? "Samsung-Batterietreiber (ASOC)" : "Batterie-Treiber (SoH)";
                return new PercentReading(percent, source);
            }
        }
        return null;
    }

    /** Accepts only the ABI's integer percentage; 110 remains invalid. */
    static int normalizeStateOfHealth(long raw) {
        return raw >= 1L && raw <= 100L ? (int) raw : 0;
    }

    /** Recognizes explicit SoH/ASOC attributes, never the qualitative health state. */
    static boolean isStateOfHealthAttribute(String name) {
        return "state_of_health".equals(name)
                || "battery_state_of_health".equals(name)
                || "soh".equals(name)
                || "battery_soh".equals(name)
                || "fg_asoc".equals(name);
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

    private static Reading readVendorFullChargeCapacity() {
        String[] paths = {
                "/sys/class/oplus_chg/battery/battery_fcc",
                "/sys/class/oplus_chg/battery/full_charge_capacity"
        };
        for (String path : paths) {
            long raw = readLong(new File(path));
            long mah = normalizeCapacity(raw);
            if (validLong(mah)) {
                return new Reading((int) mah, "OPlus/ColorOS-Batterietreiber (FCC)");
            }
        }
        return null;
    }

    private static PercentReading readVendorStateOfHealth() {
        String[] paths = {
                "/sys/class/oplus_chg/battery/battery_soh",
                "/sys/class/oplus_chg/battery/state_of_health"
        };
        for (String path : paths) {
            int percent = normalizeStateOfHealth(readLong(new File(path)));
            if (percent > 0) {
                return new PercentReading(percent, "OPlus/ColorOS-Batterietreiber (SoH)");
            }
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

    private static final class PercentReading {
        final int percent;
        final String source;

        PercentReading(int percent, String source) {
            this.percent = percent;
            this.source = source;
        }

        boolean isAvailable() { return normalizeStateOfHealth(percent) > 0; }
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
