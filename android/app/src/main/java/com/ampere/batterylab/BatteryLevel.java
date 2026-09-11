package com.ampere.batterylab;

/** Validates and normalizes Android's raw battery level and scale pair. */
final class BatteryLevel {
    private static final int MAX_SCALE = 1000;

    private BatteryLevel() { }

    /** Returns 0..100, or -1 when the supplied pair is impossible. */
    static int percent(int rawLevel, int scale) {
        if (scale < 1 || scale > MAX_SCALE || rawLevel < 0 || rawLevel > scale) return -1;
        return Math.max(0, Math.min(100, Math.round(rawLevel * 100f / scale)));
    }

    /** Validates a percentage already read from local history or telemetry. */
    static int normalizePercent(int value) {
        return value >= 0 && value <= 100 ? value : -1;
    }
}
