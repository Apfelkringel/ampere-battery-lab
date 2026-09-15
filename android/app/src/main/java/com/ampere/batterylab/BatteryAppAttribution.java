package com.ampere.batterylab;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Keeps per-app drain attribution bounded by the observed battery energy. */
final class BatteryAppAttribution {
    private BatteryAppAttribution() { }

    static String sourceLabel(boolean hasAppTelemetry) {
        return hasAppTelemetry
                ? "Quelle: zugeordnete Akku-Telemetrie (Schätzung)"
                : "Quelle: anteilig nach Vordergrundzeit (Schätzung)";
    }

    /** Uses the whole-device energy from the same window used for foreground attribution. */
    static int observedWindowMah(int telemetryWindowMah, boolean sinceFullWindow,
                                 int sinceFullMah, int dischargeSessionMah) {
        if (telemetryWindowMah > 0) return telemetryWindowMah;
        int fallback = sinceFullWindow ? sinceFullMah : dischargeSessionMah;
        return Math.max(0, fallback);
    }

    static int estimateMah(int directMah, int directTotalMah, int observedTotalMah,
                           long foregroundMs, long totalForegroundMs) {
        if (directMah <= 0) {
            return estimateFallbackMah(observedTotalMah, directTotalMah,
                    foregroundMs, totalForegroundMs);
        }
        if (observedTotalMah <= 0) return 0;
        if (directTotalMah > observedTotalMah) {
            // Floor proportional shares: rounding every app independently can
            // make their sum exceed the measured whole-device energy.
            return Math.max(0, (int) Math.floor(
                    (double) directMah * observedTotalMah / directTotalMah));
        }
        return Math.min(directMah, observedTotalMah);
    }

    static int estimateFallbackMah(int observedTotalMah, int directAssignedMah,
                                   long foregroundMs, long totalForegroundMs) {
        if (observedTotalMah <= 0 || foregroundMs <= 0L || totalForegroundMs <= 0L) return 0;
        int remainingMah = Math.max(0, observedTotalMah - Math.max(0, directAssignedMah));
        // As above, independently rounded app shares can collectively exceed
        // the unassigned energy when several apps have similar foreground time.
        return Math.max(0, Math.min(remainingMah, (int) Math.floor(
                (double) remainingMah * foregroundMs / totalForegroundMs)));
    }

    /** Proportionally scales direct app readings while preserving the observed total exactly. */
    static Map<String, Integer> scaleDirectMah(Map<String, Integer> measured, int observedMah) {
        Map<String, Long> weights = new HashMap<>();
        if (measured != null) {
            for (Map.Entry<String, Integer> entry : measured.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null && entry.getValue() > 0) {
                    weights.put(entry.getKey(), entry.getValue().longValue());
                }
            }
        }
        long total = sumWeights(weights);
        int budget = (int) Math.min(Math.max(0L, observedMah), total);
        return apportion(budget, weights);
    }

    static int allocatedMah(Map<String, Integer> allocations, String key) {
        Integer value = allocations == null ? null : allocations.get(key);
        return value == null ? 0 : value;
    }

    static void addForegroundTime(Map<String, Long> totals, String packageName, long foregroundMs) {
        if (totals == null || packageName == null || packageName.isEmpty() || foregroundMs <= 0L) return;
        Long previous = totals.get(packageName);
        long current = previous == null ? 0L : previous;
        totals.put(packageName, current > Long.MAX_VALUE - foregroundMs
                ? Long.MAX_VALUE : current + foregroundMs);
    }

    /** Divides a remaining energy budget by non-negative foreground-time weights. */
    static Map<String, Integer> apportion(int budgetMah, Map<String, Long> weights) {
        Map<String, Integer> result = new HashMap<>();
        if (weights == null || weights.isEmpty() || budgetMah <= 0) return result;
        List<String> keys = new ArrayList<>();
        for (Map.Entry<String, Long> entry : weights.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null && entry.getValue() > 0L) {
                keys.add(entry.getKey());
            }
        }
        Collections.sort(keys);
        long totalWeight = 0L;
        for (String key : keys) {
            long weight = weights.get(key);
            totalWeight = totalWeight > Long.MAX_VALUE - weight
                    ? Long.MAX_VALUE : totalWeight + weight;
        }
        if (totalWeight <= 0L) return result;

        List<Share> shares = new ArrayList<>();
        int assigned = 0;
        for (String key : keys) {
            double exact = budgetMah * (weights.get(key) / (double) totalWeight);
            int whole = (int) Math.floor(exact);
            assigned += whole;
            result.put(key, whole);
            shares.add(new Share(key, exact - whole));
        }
        Collections.sort(shares, new Comparator<Share>() {
            @Override public int compare(Share left, Share right) {
                int fractionOrder = Double.compare(right.fraction, left.fraction);
                return fractionOrder != 0 ? fractionOrder : left.key.compareTo(right.key);
            }
        });
        int remainder = budgetMah - assigned;
        for (int i = 0; i < remainder && i < shares.size(); i++) {
            String key = shares.get(i).key;
            result.put(key, result.get(key) + 1);
        }
        return result;
    }

    private static long sumWeights(Map<String, Long> weights) {
        long total = 0L;
        for (Long value : weights.values()) {
            if (value != null && value > 0L) {
                total = total > Long.MAX_VALUE - value ? Long.MAX_VALUE : total + value;
            }
        }
        return total;
    }

    private static final class Share {
        final String key;
        final double fraction;

        Share(String key, double fraction) {
            this.key = key;
            this.fraction = fraction;
        }
    }

    static int sampleMah(int currentMa, long intervalMs, long maxIntervalMs) {
        if (currentMa <= 0 || intervalMs <= 0L || maxIntervalMs <= 0L) return 0;
        long boundedIntervalMs = Math.min(intervalMs, maxIntervalMs);
        return Math.max(0, Math.round(currentMa * boundedIntervalMs / 3600000f));
    }

    /** Converts an app's estimated charge loss into a foreground-time rate. */
    static int rateMahPerHour(int estimatedMah, long foregroundMs) {
        if (estimatedMah <= 0 || foregroundMs <= 0L) return 0;
        long boundedMs = Math.min(foregroundMs, 30L * 24L * 60L * 60L * 1000L);
        return Math.max(0, Math.round(estimatedMah * 3600000f / boundedMs));
    }

    /** Only direct app telemetry can support a genuinely app-specific rate. */
    static int appRateMahPerHour(int estimatedMah, long foregroundMs,
                                 boolean hasDirectAppTelemetry) {
        return hasDirectAppTelemetry ? rateMahPerHour(estimatedMah, foregroundMs) : 0;
    }

    static String appRateLabel(int rateMahPerHour, boolean hasDirectAppTelemetry) {
        return hasDirectAppTelemetry && rateMahPerHour > 0
                ? "~" + rateMahPerHour + " mAh/h" : "Rate n/v";
    }
}
