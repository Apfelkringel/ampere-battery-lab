package com.ampere.batterylab;

/** Builds concise, localized summaries of live dashboard values for screen readers. */
final class BatteryAccessibilitySummary {
    private BatteryAccessibilitySummary() { }

    static String overview(boolean charging, String runtime, String runtimeSource,
                           String current, String temperature, String voltage) {
        StringBuilder summary = new StringBuilder();
        append(summary, "Akkustrom", current);
        append(summary, "Temperatur", temperature);
        append(summary, "Spannung", voltage);
        if (!charging) appendEstimate(summary, "Restlaufzeit bei normaler Nutzung", runtime, runtimeSource);
        return summary.toString();
    }

    static String discharge(String screenOn, String screenOnSource,
                            String screenOff, String screenOffSource,
                            String normal, String normalSource) {
        StringBuilder summary = new StringBuilder();
        appendEstimate(summary, "Restlaufzeit bei dauerhaft eingeschaltetem Bildschirm",
                screenOn, screenOnSource);
        appendEstimate(summary, "Restlaufzeit bei ausgeschaltetem Bildschirm",
                screenOff, screenOffSource);
        appendEstimate(summary, "Restlaufzeit bei normaler Nutzung", normal, normalSource);
        return summary.toString();
    }

    private static void appendEstimate(StringBuilder summary, String label,
                                       String value, String source) {
        append(summary, label, value);
        append(summary, "Datenquelle", source);
    }

    private static void append(StringBuilder summary, String label, String value) {
        if (summary.length() > 0) summary.append(". ");
        summary.append(label).append(": ");
        summary.append(value == null || value.trim().isEmpty() || "—".equals(value)
                ? "nicht verfügbar" : value.trim());
    }
}
