package com.ampere.batterylab;

import java.util.ArrayList;
import java.util.Locale;

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

    static String charging(boolean active, String state, String current, String target,
                           String remaining, String estimateSource, String screenOnRate,
                           String screenOffRate, String sessionEnergy, String sessionDuration,
                           String temperature, String voltage, String charger) {
        StringBuilder summary = new StringBuilder();
        append(summary, "Ladestatus", state);
        if (active) append(summary, "Akkustrom", current);
        append(summary, "Ladeziel", target);
        appendEstimate(summary, "Zeit bis Ladeziel", remaining, estimateSource);
        append(summary, "Laderate bei Bildschirm an", screenOnRate);
        append(summary, "Laderate bei Bildschirm aus", screenOffRate);
        append(summary, "Geladene Energie", sessionEnergy);
        append(summary, "Sitzungsdauer", sessionDuration);
        append(summary, "Temperatur", temperature);
        append(summary, "Spannung", voltage);
        append(summary, "Ladequelle", charger);
        return summary.toString();
    }

    static String history(String period, String selectedRange,
                          BatteryHistoryStats.Bucket selected,
                          ArrayList<BatteryHistoryStats.Bucket> buckets,
                          boolean capacityAvailable) {
        StringBuilder summary = new StringBuilder("Verlauf ").append(period);
        append(summary, "Ausgewählter Zeitraum", selectedRange);
        if (selected == null) {
            append(summary, "Statistik", "keine Messdaten");
        } else {
            append(summary, "Aufgeladen", amount(selected.chargedMah));
            append(summary, "Akkuverbrauch", amount(selected.consumedMah));
            append(summary, "Akkuverschleiß", capacityAvailable
                    ? String.format(Locale.GERMANY, "%.2f EFC", selected.wearCycles)
                    : "nicht berechenbar, Kapazität fehlt");
            append(summary, "Effizienz", selected.efficiencyPercent > 0
                    ? selected.efficiencyPercent + " Prozent" : "nicht verfügbar");
        }
        summary.append(". Balkenwerte: ");
        boolean hasBars = false;
        if (buckets != null) {
            for (BatteryHistoryStats.Bucket bucket : buckets) {
                if (bucket.chargedMah <= 0 && bucket.consumedMah <= 0 && bucket.wearCycles <= 0f) continue;
                if (hasBars) summary.append(". ");
                summary.append(bucket.label).append(": aufgeladen ").append(amount(bucket.chargedMah))
                        .append(", verbraucht ").append(amount(bucket.consumedMah));
                if (capacityAvailable) {
                    summary.append(", Verschleiß ")
                            .append(String.format(Locale.GERMANY, "%.2f EFC", bucket.wearCycles));
                }
                hasBars = true;
            }
        }
        if (!hasBars) summary.append("keine Messdaten im Diagramm");
        summary.append(". EFC sind äquivalente Vollzyklen, kein direkt gemessener chemischer Gesundheitsverlust.");
        return summary.toString();
    }

    static String health(String health, String capacity, String designCapacity,
                         String source, String status, String cycles, String wearImpact,
                         String temperature, String voltage) {
        StringBuilder summary = new StringBuilder("Akkugesundheit");
        append(summary, "Gesundheit", health);
        append(summary, "Geschätzte Vollkapazität", capacity);
        append(summary, "Designkapazität", designCapacity);
        append(summary, "Messquelle", source);
        append(summary, "Messstatus", status);
        append(summary, "Ladezyklen", cycles);
        append(summary, "Belastung bis zum Ladeziel", wearImpact);
        append(summary, "Temperatur", temperature);
        append(summary, "Spannung", voltage);
        summary.append(". Kapazität und Belastung sind Schätzungen, keine direkte chemische Messung.");
        return summary.toString();
    }

    private static String amount(int mah) {
        return mah > 0 ? mah + " mAh" : "keine Messdaten";
    }

    private static void appendEstimate(StringBuilder summary, String label,
                                       String value, String source) {
        append(summary, label, value);
        if (isAvailable(value) && !"Erreicht".equals(value) && !"Voll".equals(value)) {
            append(summary, "Datenquelle", source);
        }
    }

    private static void append(StringBuilder summary, String label, String value) {
        if (summary.length() > 0) summary.append(". ");
        summary.append(label).append(": ");
        summary.append(!isAvailable(value)
                ? "nicht verfügbar" : value.trim());
    }

    private static boolean isAvailable(String value) {
        return value != null && !value.trim().isEmpty() && !"—".equals(value.trim());
    }
}
