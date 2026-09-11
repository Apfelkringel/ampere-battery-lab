package com.ampere.batterylab;

import java.util.Locale;

/** Shared direction-aware labels for the small battery output surfaces. */
final class BatteryTelemetryText {
    private BatteryTelemetryText() { }

    static String current(int currentMa, boolean charging, boolean spacedUnit) {
        if (currentMa <= 0) return "—";
        if (currentMa >= 1000) {
            return (charging ? "+" : "−")
                    + String.format(Locale.GERMANY, "%.1f", currentMa / 1000f)
                    + (spacedUnit ? " A" : "A");
        }
        return (charging ? "+" : "−") + currentMa + (spacedUnit ? " mA" : "mA");
    }

    static String power(int currentMa, int voltageMv, boolean charging) {
        int powerMw = BatteryPower.milliWatts(charging ? currentMa : -currentMa, voltageMv);
        if (powerMw <= 0) return "—";
        return (charging ? "+" : "−")
                + String.format(Locale.GERMANY, "%.1f W", powerMw / 1000f);
    }
}
