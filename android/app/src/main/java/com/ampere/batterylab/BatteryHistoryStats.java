package com.ampere.batterylab;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Aggregates local telemetry into comparable daily, weekly and monthly buckets. */
final class BatteryHistoryStats {
    private BatteryHistoryStats() { }

    static ArrayList<Bucket> aggregateRows(ArrayList<String> rows, long now, int periodDays,
                                           int capacityMah) {
        int normalizedDays = periodDays == 1 ? 1 : periodDays == 7 ? 7 : 30;
        int bucketCount = normalizedDays == 1 ? 7 : normalizedDays == 7 ? 5 : 6;
        ArrayList<Bucket> result = calendarBuckets(now, normalizedDays, bucketCount,
                TimeZone.getDefault());
        long windowStart = result.get(0).start;
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
                int assignedMah = 0;
                long cursor = timestamp;
                while (cursor < end) {
                    int bucket = bucketAt(result, cursor);
                    if (bucket < 0) break;
                    long nextBoundary = bucket + 1 < result.size()
                            ? result.get(bucket + 1).start : now;
                    long segmentEnd = Math.min(end, nextBoundary);
                    if (segmentEnd <= cursor) break;
                    int segmentMah = segmentEnd == end ? mah - assignedMah
                            : Math.round(mah * (segmentEnd - timestamp) / (float) (end - timestamp))
                            - assignedMah;
                    if ("1".equals(parts[2])) result.get(bucket).chargedMah += segmentMah;
                    else if ("0".equals(parts[2])) result.get(bucket).consumedMah += segmentMah;
                    assignedMah += segmentMah;
                    cursor = segmentEnd;
                }
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

    /** Builds locally aligned calendar days, Monday-based weeks and calendar months. */
    static ArrayList<Bucket> calendarBuckets(long now, int periodDays, int bucketCount,
                                              TimeZone timeZone) {
        int normalizedDays = periodDays == 1 ? 1 : periodDays == 7 ? 7 : 30;
        int safeCount = Math.max(1, bucketCount);
        Calendar current = Calendar.getInstance(timeZone, Locale.GERMANY);
        current.setTimeInMillis(now);
        current.set(Calendar.HOUR_OF_DAY, 0);
        current.set(Calendar.MINUTE, 0);
        current.set(Calendar.SECOND, 0);
        current.set(Calendar.MILLISECOND, 0);
        if (normalizedDays == 7) {
            int daysSinceWeekStart = (current.get(Calendar.DAY_OF_WEEK)
                    - current.getFirstDayOfWeek() + 7) % 7;
            current.add(Calendar.DAY_OF_MONTH, -daysSinceWeekStart);
        } else if (normalizedDays == 30) {
            current.set(Calendar.DAY_OF_MONTH, 1);
        }
        Calendar first = (Calendar) current.clone();
        if (normalizedDays == 1) first.add(Calendar.DAY_OF_MONTH, -(safeCount - 1));
        else if (normalizedDays == 7) first.add(Calendar.DAY_OF_MONTH, -7 * (safeCount - 1));
        else first.add(Calendar.MONTH, -(safeCount - 1));

        ArrayList<Bucket> buckets = new ArrayList<>();
        Calendar cursor = (Calendar) first.clone();
        for (int i = 0; i < safeCount; i++) {
            long start = cursor.getTimeInMillis();
            buckets.add(new Bucket(start, label(start, normalizedDays)));
            if (normalizedDays == 1) cursor.add(Calendar.DAY_OF_MONTH, 1);
            else if (normalizedDays == 7) cursor.add(Calendar.DAY_OF_MONTH, 7);
            else cursor.add(Calendar.MONTH, 1);
        }
        return buckets;
    }

    private static int bucketAt(ArrayList<Bucket> buckets, long timestamp) {
        for (int i = 0; i < buckets.size(); i++) {
            if (timestamp >= buckets.get(i).start
                    && (i + 1 == buckets.size() || timestamp < buckets.get(i + 1).start)) {
                return i;
            }
        }
        return -1;
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
