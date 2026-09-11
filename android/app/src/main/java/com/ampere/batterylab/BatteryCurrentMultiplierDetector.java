package com.ampere.batterylab;

/**
 * Detects common OEM current-unit scale errors without inventing a value for
 * an unknown or near-zero reading. The detector returns only a magnitude
 * multiplier; direction is owned by the charging state in the caller.
 */
final class BatteryCurrentMultiplierDetector {
    static final int STATUS_CHARGING = 1;
    static final int STATUS_DISCHARGING = 2;
    static final int STATUS_UNPLUGGED = 3;

    private static final double MIN_TYPICAL_CHARGING_MA = 500d;
    private static final double MIN_TYPICAL_DISCHARGING_MA = 100d;
    // A real phone can idle below 10 mA in deep sleep. Below this floor there
    // is no evidence that a unit-scale error exists, so multiplying a single
    // sample would manufacture a large drain value from a plausible reading.
    private static final double MIN_BASE_FOR_SCALE_MA = 10d;
    private static final int CHARGING_TAPER_PERCENT = 90;
    private static final int[] MAGNITUDE_MULTIPLIERS = {1, 10, 100, 1000};

    private BatteryCurrentMultiplierDetector() { }

    static int detect(double milliAmpsAtMultiplierOne, int status, int batteryPercent) {
        if (!Double.isFinite(milliAmpsAtMultiplierOne)
                || milliAmpsAtMultiplierOne == 0d) return 1;
        if (status != STATUS_CHARGING && status != STATUS_DISCHARGING
                && status != STATUS_UNPLUGGED) return 1;

        double magnitude = Math.abs(milliAmpsAtMultiplierOne);
        if (magnitude < MIN_BASE_FOR_SCALE_MA) return 1;
        double minimum = status == STATUS_CHARGING
                ? MIN_TYPICAL_CHARGING_MA : MIN_TYPICAL_DISCHARGING_MA;
        if (status == STATUS_CHARGING && batteryPercent >= CHARGING_TAPER_PERCENT
                && magnitude < minimum) return 1;
        for (int multiplier : MAGNITUDE_MULTIPLIERS) {
            if (magnitude * multiplier >= minimum) return multiplier;
        }
        return 1;
    }
}
