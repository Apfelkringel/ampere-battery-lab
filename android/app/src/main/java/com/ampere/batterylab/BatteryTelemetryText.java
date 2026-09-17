package com.ampere.batterylab;

import java.util.Locale;

/** Shared direction-aware labels for the small battery output surfaces. */
final class BatteryTelemetryText {
    private BatteryTelemetryText() { }

    static String current(int currentMa, boolean charging, boolean spacedUnit) {
        return current(currentMa, charging, spacedUnit, Locale.GERMANY);
    }

    static String current(int currentMa, boolean charging, boolean spacedUnit, Locale locale) {
        if (currentMa <= 0) return "—";
        if (currentMa >= 1000) {
            return (charging ? "+" : "−")
                    + String.format(locale, "%.1f", currentMa / 1000f)
                    + (spacedUnit ? " A" : "A");
        }
        return (charging ? "+" : "−") + currentMa + (spacedUnit ? " mA" : "mA");
    }

    static String power(int currentMa, int voltageMv, boolean charging) {
        return power(currentMa, voltageMv, charging, Locale.GERMANY);
    }

    static String power(int currentMa, int voltageMv, boolean charging, Locale locale) {
        int powerMw = BatteryPower.milliWatts(charging ? currentMa : -currentMa, voltageMv);
        if (powerMw <= 0) return "—";
        return (charging ? "+" : "−")
                + String.format(locale, "%.1f W", powerMw / 1000f);
    }
}
