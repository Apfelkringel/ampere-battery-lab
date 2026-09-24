package com.ampere.batterylab;

/** Pure sampling policy shared by the monitor and scenario tests. */
public final class BatterySamplingPolicy {
    private static final int RETENTION_DAYS = 30;

    private BatterySamplingPolicy() { }

    static int retentionDays() {
        return RETENTION_DAYS;
    }

    public static int normalizeMinutes(int minutes) {
        return minutes == 5 || minutes == 15 || minutes == 30 || minutes == 60 ? minutes : 15;
    }

    public static long intervalMs(int minutes) {
        return normalizeMinutes(minutes) * 60L * 1000L;
    }

    public static int retentionSamples(int minutes) {
        long retentionMs = retentionDays() * 24L * 60L * 60L * 1000L;
        return Math.max(1, (int) Math.ceil(retentionMs / (double) intervalMs(minutes)));
    }

    public static boolean shouldSample(long previousAt, long now, long intervalMs) {
        return previousAt <= 0L || now < previousAt || now - previousAt >= intervalMs;
    }
}
