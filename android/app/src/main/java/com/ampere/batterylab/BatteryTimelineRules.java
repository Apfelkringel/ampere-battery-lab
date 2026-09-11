package com.ampere.batterylab;

/** Keeps wall-clock session boundaries valid when the device clock changes. */
final class BatteryTimelineRules {
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
}
