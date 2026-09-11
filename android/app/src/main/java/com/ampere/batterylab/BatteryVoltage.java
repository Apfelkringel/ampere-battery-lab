package com.ampere.batterylab;

/** Validates Android's battery voltage, reported in millivolts. */
final class BatteryVoltage {
    private static final int MIN_MILLIVOLTS = 1000;
    private static final int MAX_MILLIVOLTS = 10000;

    private BatteryVoltage() { }

    /** Returns zero for missing, sentinel, or implausible battery voltage. */
    static int normalizeMilliVolts(int rawMilliVolts) {
        return rawMilliVolts >= MIN_MILLIVOLTS && rawMilliVolts <= MAX_MILLIVOLTS
                ? rawMilliVolts : 0;
    }
}
