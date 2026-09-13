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

    /** Caps an integration interval so a stopped monitor cannot invent activity. */
    static long cappedElapsed(long previous, long current, long capMs) {
        if (!isForward(previous, current) || capMs <= 0L) return 0L;
        return Math.min(capMs, current - previous);
    }

    /** Returns true when a chart must leave a gap for missing measurements. */
    static boolean isSamplingGap(long previous, long current, long intervalMs) {
        if (!isForward(previous, current) || intervalMs <= 0L) return false;
        long threshold = Math.max(TWO_HOURS_MS, intervalMs * 2L);
        return current - previous > threshold;
    }

    /**
     * Allows a short visual interpolation without hiding a real outage. The
     * raw samples stay untouched; callers should render this segment as an
     * estimate and never use it for totals or health calculations.
     */
    static boolean shouldInterpolate(long previous, long current, long intervalMs) {
        if (!isForward(previous, current) || intervalMs <= 0L) return false;
        long delta = current - previous;
        long maximum = Math.min(TWO_HOURS_MS, intervalMs * 6L);
        return delta > intervalMs && delta <= maximum;
    }

    /**
     * A long gap means the open session was not continuously observed. Reset
     * only when that session started before the last known sample; a session
     * that began after the sample is still a valid short event.
     */
    static boolean shouldResetSession(long sessionStartedAt, long previousSampleAt,
                                      long now, long intervalMs) {
        return sessionStartedAt > 0L && previousSampleAt > 0L
                && sessionStartedAt <= previousSampleAt
                && isSamplingGap(previousSampleAt, now, intervalMs);
    }
}
