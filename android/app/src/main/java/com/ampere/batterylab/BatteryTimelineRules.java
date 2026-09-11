package com.ampere.batterylab;

/** Keeps wall-clock session boundaries valid when the device clock changes. */
final class BatteryTimelineRules {
    private static final long TWO_HOURS_MS = 2L * 60L * 60L * 1000L;

    private BatteryTimelineRules() { }

    static boolean isRollback(long previous, long current) {
        return previous > 0L && current < previous;
    }

    static boolean isForward(long start, long end) {
        return start > 0L && end >= start;
    }

    /** Returns zero for an invalid boundary instead of inventing a one-minute session. */
    static long sessionMinutes(long start, long end) {
        if (!isForward(start, end)) return 0L;
        return Math.max(1L, (end - start) / 60000L);
    }

    /** Returns true when a chart must leave a gap for missing measurements. */
    static boolean isSamplingGap(long previous, long current, long intervalMs) {
        if (!isForward(previous, current) || intervalMs <= 0L) return false;
        long threshold = Math.max(TWO_HOURS_MS, intervalMs * 2L);
        return current - previous > threshold;
    }
}
