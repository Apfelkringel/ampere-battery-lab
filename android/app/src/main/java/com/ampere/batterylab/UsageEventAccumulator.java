package com.ampere.batterylab;

import java.util.Map;

/** Pure interval accounting for foreground usage events. */
final class UsageEventAccumulator {
    private UsageEventAccumulator() { }

    static void apply(Map<String, Long> totals, Map<String, Long> activeSince,
                      String packageName, long timestamp, long start, long end,
                      boolean foreground, boolean background) {
        if (packageName == null || packageName.isEmpty() || timestamp > end) return;
        if (foreground) {
            if (!activeSince.containsKey(packageName)) activeSince.put(packageName, Math.max(start, timestamp));
        } else if (background) {
            Long began = activeSince.remove(packageName);
            if (began != null) add(totals, packageName, began, Math.min(end, timestamp));
        }
    }

    static void closeActive(Map<String, Long> totals, Map<String, Long> activeSince, long end) {
        for (Map.Entry<String, Long> entry : activeSince.entrySet()) {
            add(totals, entry.getKey(), entry.getValue(), end);
        }
    }

    private static void add(Map<String, Long> totals, String packageName, long start, long end) {
        if (end <= start) return;
        totals.put(packageName, totals.containsKey(packageName)
                ? totals.get(packageName) + end - start : end - start);
    }
}
