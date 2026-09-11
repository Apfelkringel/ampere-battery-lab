package com.ampere.batterylab;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Converts a measured capacity into a bounded battery-health percentage. */
final class BatteryHealth {
    private static final int MIN_CAPACITY_MAH = 500;
    private static final int MAX_CAPACITY_MAH = 30000;

    private BatteryHealth() { }

    /** Validated health percentage and the capacity that belongs to that source. */
    static final class HealthReading {
        final int percent;
        final int capacityMah;
        final String source;

        HealthReading(int percent, int capacityMah, String source) {
            this.percent = displayPercent(percent);
            this.capacityMah = isPlausibleCapacity(capacityMah) ? capacityMah : 0;
            this.source = source == null ? "" : source;
        }
    }

    /**
     * Resolves all health inputs in one order. An invalid system value such as
     * 110 is discarded before either the percentage or its derived capacity is
     * returned, so the UI cannot mix a rejected percentage with another source.
     */
    static HealthReading resolveReading(int reportedPercent, int measuredMah, int designMah,
                                        String reportedSource, String measuredSource) {
        int reported = displayPercent(reportedPercent);
        if (reported > 0) {
            return new HealthReading(reported, capacityFromReportedPercent(reported, designMah),
                    reportedSource);
        }
        int measured = isPlausibleCapacity(measuredMah) ? measuredMah : 0;
        int measuredPercent = percent(measured, designMah);
        return new HealthReading(measuredPercent, measured,
                measuredPercent > 0 ? measuredSource : "");
    }

    static int percent(int measuredMah, int designMah) {
        if (!isPlausibleCapacity(measuredMah) || !isPlausibleCapacity(designMah)) return 0;
        // A measured capacity can be slightly above the nominal value because
        // the nominal value is rounded. That is a valid measurement, but the
        // health scale itself must still stop at 100%.
        return Math.max(1, Math.min(100, Math.round(measuredMah * 100f / designMah)));
    }

    /** Uses Android's system-reported SoH when the running platform exposes it. */
    static int percent(Context context, SharedPreferences prefs, int designMah) {
        return resolveDisplayPercent(context, prefs, designMah);
    }

    /**
     * Single source of truth for the percentage shown in every UI surface.
     *
     * Keep the final gate at the outside of the complete source hierarchy.
     * This is deliberately redundant with the individual readers: a future
     * OEM/API reader must never be able to make an impossible value visible
     * just because it was added before the existing validation code.
     */
    static int resolveDisplayPercent(Context context, SharedPreferences prefs, int designMah) {
        if (prefs == null) return 0;
        return read(context, prefs, designMah).percent;
    }

    static boolean isPlausibleCapacity(int mah) {
        return mah >= MIN_CAPACITY_MAH && mah <= MAX_CAPACITY_MAH;
    }

    static int reportedPercentValue(int value) {
        // Some OEM battery services have been observed to report a rounded
        // value above the physical 100% ceiling. Do not silently turn an
        // impossible reading such as 110 into a seemingly valid 100: reject
        // it so the caller can use a local measurement or show unavailable.
        return displayPercent(value);
    }

    /**
     * Single output gate for every health percentage shown by the app.
     * Invalid values are unavailable, not rounded into a misleading result.
     */
    static int displayPercent(int value) {
        return value >= 1 && value <= 100 ? value : 0;
    }

    /** Converts a valid system SoH percentage to capacity without accepting impossible input. */
    static int capacityFromReportedPercent(int reportedPercent, int designMah) {
        int reported = reportedPercentValue(reportedPercent);
        if (reported == 0 || !isPlausibleCapacity(designMah)) return 0;
        int capacity = Math.round(designMah * reported / 100f);
        return isPlausibleCapacity(capacity) ? capacity : 0;
    }

    static int reportedStateOfHealth(Context context) {
        int systemValue = batteryManagerStateOfHealth(context);
        int fallback = BatteryCapacity.stateOfHealthPercent();
        return displayPercent(systemValue > 0 ? systemValue : fallback);
    }

    static HealthReading read(Context context, SharedPreferences prefs, int designMah) {
        if (prefs == null) return new HealthReading(0, 0, "");
        int reported = reportedStateOfHealth(context);
        String reportedSource = reported > 0 ? reportedStateOfHealthSource(context) : "";
        int measured = measurementMah(context, prefs);
        return resolveReading(reported, measured, designMah, reportedSource,
                measurementSource(context, prefs));
    }

    private static String measurementSource(Context context, SharedPreferences prefs) {
        if (averageRecentSamples(prefs.getString("healthSamples", "")) > 0) {
            return "lokale Lademessungen";
        }
        if (isPlausibleCapacity(prefs.getInt("benchmarkCapacityMah", 0))) {
            return "manueller Benchmark";
        }
        if (BatteryCapacity.fullChargeCapacityMah(context) > 0) {
            return BatteryCapacity.fullChargeCapacitySource(context);
        }
        return "";
    }

    static String reportedStateOfHealthSource(Context context) {
        if (batteryManagerStateOfHealth(context) > 0) return "Android BatteryManager";
        return BatteryCapacity.stateOfHealthPercent() > 0
                ? BatteryCapacity.stateOfHealthSource() : "";
    }

    private static int batteryManagerStateOfHealth(Context context) {
        if (context == null) return 0;
        try {
            // Android exposes this property behind a feature flag on some
            // releases and OEMs may backport it. Do not hard-code an API-level
            // gate: reflection lets every device advertise its real support,
            // while a missing/blocked field still falls back safely.
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
        ArrayList<Integer> samples = parseSamples(serialized);
        int count = Math.min(5, samples.size());
        if (count == 0) return 0;
        // A single noisy charge session must not pull the health estimate
        // toward an impossible value. Use a robust median over the newest
        // samples while keeping the existing five-sample horizon.
        ArrayList<Integer> recent = new ArrayList<>(
                samples.subList(samples.size() - count, samples.size()));
        Collections.sort(recent);
        int middle = count / 2;
        return count % 2 == 1
                ? recent.get(middle)
                : Math.round((recent.get(middle - 1) + recent.get(middle)) / 2f);
    }

    /** Parses only plausible capacity samples and bounds legacy history size. */
    static ArrayList<Integer> parseSamples(String serialized) {
        ArrayList<Integer> samples = new ArrayList<>();
        if (serialized == null || serialized.trim().isEmpty()) return samples;
        for (String value : serialized.split(",", -1)) {
            try {
                int sample = Integer.parseInt(value.trim());
                if (isPlausibleCapacity(sample)) samples.add(sample);
            } catch (NumberFormatException ignored) { }
        }
        while (samples.size() > 150) samples.remove(0);
        return samples;
    }

    static String serializeSamples(List<Integer> samples) {
        StringBuilder output = new StringBuilder();
        if (samples == null) return "";
        for (Integer sample : samples) {
            if (sample == null || !isPlausibleCapacity(sample)) continue;
            if (output.length() > 0) output.append(',');
            output.append(sample);
        }
        return output.toString();
    }

    static int estimatedCapacityMah(Context context, SharedPreferences prefs, int designMah) {
        HealthReading reading = read(context, prefs, designMah);
        if (reading.capacityMah <= 0) return 0;
        return designMah > 0 ? Math.min(reading.capacityMah, designMah) : reading.capacityMah;
    }
}
