package com.ampere.batterylab;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

/** Aggregates local telemetry into comparable daily, weekly and monthly buckets. */
final class BatteryHistoryStats {
    private BatteryHistoryStats() { }

    static ArrayList<Bucket> aggregateRows(ArrayList<String> rows, long now, int periodDays,
                                           int capacityMah) {
        int normalizedDays = periodDays == 1 ? 1 : periodDays == 7 ? 7 : 30;
        int bucketCount = normalizedDays == 1 ? 7 : normalizedDays == 7 ? 5 : 6;
        long bucketMs = normalizedDays * 24L * 60L * 60L * 1000L;
        long windowStart = now - bucketCount * bucketMs;
        ArrayList<Bucket> result = new ArrayList<>();
        for (int i = 0; i < bucketCount; i++) {
            long start = windowStart + i * bucketMs;
            result.add(new Bucket(start, label(start, normalizedDays)));
        }
        if (rows == null || rows.isEmpty()) return result;
        ArrayList<String> valid = BatteryExportRules.validTelemetryRows(
                joinRows(rows));
        for (int i = 0; i < valid.size(); i++) {
            String[] parts = valid.get(i).split(",", 11);
            if (parts.length < 4) continue;
            try {
                long timestamp = Long.parseLong(parts[0].trim());
                if (timestamp < windowStart || timestamp >= now) continue;
                int current = Math.abs(Integer.parseInt(parts[3].trim()));
                if (current <= 0) continue;
                long end = now;
                if (i + 1 < valid.size()) {
                    end = Long.parseLong(valid.get(i + 1).split(",", 2)[0].trim());
                }
                end = Math.min(now, Math.min(end, timestamp + 2L * 60L * 60L * 1000L));
                if (end <= timestamp) continue;
                int mah = Math.max(0, Math.round(current * (end - timestamp) / 3600000f));
                int bucket = (int) ((timestamp - windowStart) / bucketMs);
                if (bucket < 0 || bucket >= result.size()) continue;
                if ("1".equals(parts[2])) result.get(bucket).chargedMah += mah;
                else if ("0".equals(parts[2])) result.get(bucket).consumedMah += mah;
            } catch (RuntimeException ignored) { }
        }
        for (Bucket bucket : result) {
            bucket.wearCycles = capacityMah > 0
                    ? bucket.consumedMah / (float) capacityMah : 0f;
            bucket.efficiencyPercent = bucket.consumedMah > 0
                    ? Math.round(bucket.chargedMah * 100f / bucket.consumedMah) : 0;
        }
        return result;
    }

    static Overall overall(ArrayList<Bucket> buckets) {
        Overall overall = new Overall();
        if (buckets == null) return overall;
        for (Bucket bucket : buckets) {
            overall.chargedMah += bucket.chargedMah;
            overall.consumedMah += bucket.consumedMah;
            overall.wearCycles += bucket.wearCycles;
        }
        overall.efficiencyPercent = overall.consumedMah > 0
                ? Math.round(overall.chargedMah * 100f / overall.consumedMah) : 0;
        return overall;
    }

    private static String joinRows(ArrayList<String> rows) {
        StringBuilder result = new StringBuilder();
        for (String row : rows) {
            if (row == null || row.trim().isEmpty()) continue;
            if (result.length() > 0) result.append('\n');
            result.append(row);
        }
        return result.toString();
    }

    private static String label(long timestamp, int periodDays) {
        String pattern = periodDays == 1 ? "EEE" : periodDays == 7 ? "d. MMM" : "MMM";
        return new SimpleDateFormat(pattern, Locale.GERMANY).format(new Date(timestamp));
    }

    static final class Bucket {
        final long start;
        final String label;
        int chargedMah;
        int consumedMah;
        float wearCycles;
        int efficiencyPercent;

        Bucket(long start, String label) {
            this.start = start;
            this.label = label;
        }
    }

    static final class Overall {
        int chargedMah;
        int consumedMah;
        float wearCycles;
        int efficiencyPercent;
    }
}
