package com.ampere.batterylab;

/** Validates the user-selected charge target before it reaches UI or alarms. */
final class BatteryChargeLimit {
    static final int DEFAULT = 80;
    private static final int MINIMUM = 50;
    private static final int MAXIMUM = 100;

    private BatteryChargeLimit() { }

    static int normalize(int value) {
        return value >= MINIMUM && value <= MAXIMUM ? value : DEFAULT;
    }
}
