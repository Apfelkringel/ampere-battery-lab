package com.ampere.batterylab;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;

/**
 * Local persistence seam for battery data.
 *
 * The current adapter deliberately remains SharedPreferences-backed so existing
 * installs and backups keep working. Callers no longer need to know the
 * preference names or the normalization rules for telemetry records.
 */
public final class BatteryDataRepository {
    public static final String DATA_PREFS = "ampere-data";
    public static final String TELEMETRY_PREFS = "ampere-telemetry";
    private static final Object LOCK = new Object();

    private BatteryDataRepository() { }

    public static SharedPreferences data(Context context) {
        return context.getSharedPreferences(DATA_PREFS, Context.MODE_PRIVATE);
    }

    public static SharedPreferences telemetry(Context context) {
        return context.getSharedPreferences(TELEMETRY_PREFS, Context.MODE_PRIVATE);
    }

    public static String readTelemetry(Context context) {
        synchronized (LOCK) {
            return BatteryExportRules.normalizeTelemetry(
                    telemetry(context).getString("telemetrySamples", ""));
        }
    }

    public static void replaceTelemetry(Context context, String raw) {
        String normalized = BatteryExportRules.normalizeTelemetry(raw == null ? "" : raw);
        telemetry(context).edit().putString("telemetrySamples", normalized).apply();
    }

    /** Append one already encoded sample and retain the configured history. */
    public static void appendTelemetry(Context context, String row, int retentionSamples) {
        if (row == null || row.isEmpty() || retentionSamples < 1) return;
        synchronized (LOCK) {
            String current = readTelemetry(context);
            ArrayList<String> rows = BatteryExportRules.validTelemetryRows(current);
            rows.add(row);
            int first = Math.max(0, rows.size() - retentionSamples);
            StringBuilder output = new StringBuilder();
            for (int i = first; i < rows.size(); i++) {
                if (output.length() > 0) output.append('\n');
                output.append(rows.get(i));
            }
            telemetry(context).edit().putString("telemetrySamples", output.toString()).apply();
        }
    }

    public static void migrate(Context context) {
        SharedPreferences oldPrefs = data(context);
        SharedPreferences newPrefs = telemetry(context);
        if (!newPrefs.contains("telemetrySamples") && oldPrefs.contains("telemetrySamples")) {
            SharedPreferences.Editor migration = newPrefs.edit()
                    .putString("telemetrySamples", oldPrefs.getString("telemetrySamples", ""));
            if (oldPrefs.contains("telemetryLastSampleAt")) {
                migration.putLong("telemetryLastSampleAt", oldPrefs.getLong("telemetryLastSampleAt", 0L));
            }
            if (migration.commit()) {
                oldPrefs.edit().remove("telemetrySamples").remove("telemetryLastSampleAt").commit();
            }
        }
        String saved = newPrefs.getString("telemetrySamples", "");
        String normalized = BatteryExportRules.normalizeTelemetry(saved);
        if (!saved.equals(normalized)) newPrefs.edit().putString("telemetrySamples", normalized).commit();
    }
}
