package com.ampere.batterylab;

/** Stable, font-independent labels for the live overlay. */
final class BatteryOverlayText {
    private BatteryOverlayText() { }

    static String header(String level, String current) {
        return "Akku " + safe(level) + "   " + safe(current);
    }

    private static String safe(String value) {
        return value == null || value.isEmpty() ? "—" : value;
    }
}
