package com.ampere.batterylab;

/** Keeps the optional floating telemetry from covering Ampere's own screen. */
final class BatteryOverlayVisibility {
    private BatteryOverlayVisibility() { }

    static boolean shouldShow(boolean overlayEnabled, boolean appActivityVisible) {
        return overlayEnabled && !appActivityVisible;
    }
}
