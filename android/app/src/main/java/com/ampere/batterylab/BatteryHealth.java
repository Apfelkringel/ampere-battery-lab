package com.ampere.batterylab;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Build;

/** Converts a measured capacity into a bounded battery-health percentage. */
final class BatteryHealth {
    private static final int MIN_CAPACITY_MAH = 500;
    private static final int MAX_CAPACITY_MAH = 30000;

    private BatteryHealth() { }

    static int percent(int measuredMah, int designMah) {
        if (!isPlausibleCapacity(measuredMah) || !isPlausibleCapacity(designMah)) return 0;
        return Math.max(1, Math.min(100, Math.round(measuredMah * 100f / designMah)));
    }

    /** Uses Android's system-reported SoH when the running platform exposes it. */
    static int percent(Context context, SharedPreferences prefs, int designMah) {
        int reported = reportedStateOfHealth(context);
        return reported > 0 ? reported : percent(measurementMah(context, prefs), designMah);
    }

    static boolean isPlausibleCapacity(int mah) {
        return mah >= MIN_CAPACITY_MAH && mah <= MAX_CAPACITY_MAH;
    }

    static int reportedPercentValue(int value) {
        return value >= 1 && value <= 100 ? value : 0;
    }

    static int reportedStateOfHealth(Context context) {
        if (Build.VERSION.SDK_INT < 36) return 0;
        try {
            // The field is rollout-gated on some Android 16 builds. Reflection
            // keeps Android 14/15 compatible and falls back safely when absent.
            int property = BatteryManager.class
                    .getField("BATTERY_PROPERTY_STATE_OF_HEALTH").getInt(null);
            BatteryManager manager = context.getSystemService(BatteryManager.class);
            if (manager == null) return 0;
            return reportedPercentValue(manager.getIntProperty(property));
        } catch (Exception ignored) {
            return 0;
        }
    }

    /**
     * Reads the same measured-capacity hierarchy for the dashboard and the
     * background notification. Invalid legacy samples are ignored rather than
     * changing the result or being reported as a real measurement.
     */
    static int measurementMah(Context context, SharedPreferences prefs) {
        int measured = averageRecentSamples(prefs.getString("healthSamples", ""));
        if (!isPlausibleCapacity(measured)) measured = prefs.getInt("benchmarkCapacityMah", 0);
        if (!isPlausibleCapacity(measured)) measured = BatteryCapacity.fullChargeCapacityMah(context);
        return isPlausibleCapacity(measured) ? measured : 0;
    }

    static int averageRecentSamples(String serialized) {
        if (serialized == null || serialized.trim().isEmpty()) return 0;
        String[] values = serialized.split(",", -1);
        int count = Math.min(5, values.length);
        long total = 0L;
        int valid = 0;
        for (int i = values.length - count; i < values.length; i++) {
            try {
                int sample = Integer.parseInt(values[i].trim());
                if (!isPlausibleCapacity(sample)) continue;
                total += sample;
                valid++;
            } catch (NumberFormatException ignored) { }
        }
        return valid > 0 ? Math.round(total / (float) valid) : 0;
    }

    static int estimatedCapacityMah(Context context, SharedPreferences prefs, int designMah) {
        int measured = measurementMah(context, prefs);
        if (isPlausibleCapacity(measured)) return designMah > 0 ? Math.min(measured, designMah) : measured;
        int reported = reportedStateOfHealth(context);
        return reported > 0 && isPlausibleCapacity(designMah)
                ? Math.round(designMah * reported / 100f) : 0;
    }
}
