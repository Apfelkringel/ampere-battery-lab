package com.ampere.batterylab;

/** Validates the visible dashboard destination restored after Android recreates the activity. */
final class BatteryPageState {
    private BatteryPageState() { }

    static int normalize(int page) {
        return Math.max(0, Math.min(4, page));
    }
}
