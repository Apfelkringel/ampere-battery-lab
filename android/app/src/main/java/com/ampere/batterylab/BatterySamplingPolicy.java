package com.ampere.batterylab;

/** Pure sampling policy shared by the monitor and scenario tests. */
public final class BatterySamplingPolicy {
    private BatterySamplingPolicy() { }

    public static int normalizeMinutes(int minutes) {
        return minutes == 5 || minutes == 15 || minutes == 30 || minutes == 60 ? minutes : 15;
    }

    public static long intervalMs(int minutes) {
        return normalizeMinutes(minutes) * 60L * 1000L;
    }

    public static int retentionSamples(int minutes) {
        long thirtyDaysMs = 30L * 24L * 60L * 60L * 1000L;
        return Math.max(1, (int) Math.ceil(thirtyDaysMs / (double) intervalMs(minutes)));
    }

    public static boolean shouldSample(long previousAt, long now, long intervalMs) {
        return previousAt <= 0L || now < previousAt || now - previousAt >= intervalMs;
    }
}
