package com.ampere.batterylab;

import java.util.Locale;

/** Calculates instantaneous battery-side power from validated Android values. */
final class BatteryPower {
    private static final int MIN_CURRENT_MA = 10;
    private static final int MAX_CURRENT_MA = 100_000;
    private static final int MIN_VOLTAGE_MV = 2_000;
    private static final int MAX_VOLTAGE_MV = 30_000;
    private static final int MIN_POWER_MW = 10;
    private static final int MAX_POWER_MW = 500_000;

    private BatteryPower() { }

    /**
     * Returns battery-side milliwatts, not wall-plug power. The current may be
     * signed; direction is irrelevant for the magnitude calculation.
     */
    static int milliWatts(int signedCurrentMa, int voltageMv) {
        long currentMa = Math.abs((long) signedCurrentMa);
        if (currentMa < MIN_CURRENT_MA || currentMa > MAX_CURRENT_MA
                || voltageMv < MIN_VOLTAGE_MV || voltageMv > MAX_VOLTAGE_MV) return 0;
        long powerMw = Math.round(currentMa * voltageMv / 1000d);
        return powerMw >= MIN_POWER_MW && powerMw <= MAX_POWER_MW ? (int) powerMw : 0;
    }

    static String label(int powerMw) {
        return label(powerMw, Locale.GERMANY);
    }

    static String label(int powerMw, Locale locale) {
        return powerMw > 0
                ? String.format(locale == null ? Locale.GERMANY : locale,
                        "≈ %.1f W", powerMw / 1000f)
                : "—";
    }
}
