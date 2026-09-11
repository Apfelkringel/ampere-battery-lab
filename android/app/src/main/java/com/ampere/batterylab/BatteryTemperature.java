package com.ampere.batterylab;

/** Validates Android's battery temperature, reported in tenths of a degree Celsius. */
final class BatteryTemperature {
    private static final int MAX_TENTHS_CELSIUS = 1000;

    private BatteryTemperature() { }

    /** Returns zero for missing, sentinel, or physically implausible readings. */
    static int normalizeTenths(int rawTenths) {
        return rawTenths > 0 && rawTenths <= MAX_TENTHS_CELSIUS ? rawTenths : 0;
    }
}
