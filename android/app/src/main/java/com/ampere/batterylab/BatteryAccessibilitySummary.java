package com.ampere.batterylab;

import java.util.ArrayList;
import java.util.Locale;

/** Builds concise, localized summaries of live dashboard values for screen readers. */
final class BatteryAccessibilitySummary {
    private BatteryAccessibilitySummary() { }

    static String overview(boolean charging, String runtime, String runtimeSource,
                           String current, String temperature, String voltage) {
        return overview(charging, runtime, runtimeSource, current, temperature, voltage, Locale.GERMANY);
    }

    static String overview(boolean charging, String runtime, String runtimeSource,
                           String current, String temperature, String voltage, Locale locale) {
        boolean english = isEnglish(locale);
        StringBuilder summary = new StringBuilder();
        append(summary, english ? "Battery current" : "Akkustrom", current, english);
        append(summary, english ? "Temperature" : "Temperatur", temperature, english);
        append(summary, english ? "Voltage" : "Spannung", voltage, english);
        if (!charging) appendEstimate(summary,
                english ? "Estimated runtime with normal use" : "Restlaufzeit bei normaler Nutzung",
                runtime, runtimeSource, english);
        return summary.toString();
    }

    static String discharge(String screenOn, String screenOnSource,
                            String screenOff, String screenOffSource,
                            String normal, String normalSource) {
        return discharge(screenOn, screenOnSource, screenOff, screenOffSource,
                normal, normalSource, Locale.GERMANY);
    }

    static String discharge(String screenOn, String screenOnSource,
                            String screenOff, String screenOffSource,
                            String normal, String normalSource, Locale locale) {
        boolean english = isEnglish(locale);
        StringBuilder summary = new StringBuilder();
        appendDischargeEstimate(summary, english ? "Runtime with screen always on"
                : "Restlaufzeit bei dauerhaft eingeschaltetem Bildschirm", screenOn, screenOnSource, english);
        appendDischargeEstimate(summary, english ? "Runtime with screen off"
                : "Restlaufzeit bei ausgeschaltetem Bildschirm", screenOff, screenOffSource, english);
        appendDischargeEstimate(summary, english ? "Runtime with normal use"
                : "Restlaufzeit bei normaler Nutzung", normal, normalSource, english);
        return summary.toString();
    }

    static String dischargeEstimate(String label, String value, String source) {
        return dischargeEstimate(label, value, source, Locale.GERMANY);
    }

    static String dischargeEstimate(String label, String value, String source, Locale locale) {
        boolean english = isEnglish(locale);
        StringBuilder summary = new StringBuilder();
        appendDischargeEstimate(summary, english ? englishLabel(label) : label, value, source, english);
        return summary.toString();
    }

    static String charging(boolean active, String state, String current, String target,
                           String remaining, String estimateSource, String screenOnRate,
                           String screenOffRate, String sessionEnergy, String sessionDuration,
                           String temperature, String voltage, String charger) {
        return charging(active, state, current, target, remaining, estimateSource, screenOnRate,
                screenOffRate, sessionEnergy, sessionDuration, temperature, voltage, charger, Locale.GERMANY);
    }

    static String charging(boolean active, String state, String current, String target,
                           String remaining, String estimateSource, String screenOnRate,
                           String screenOffRate, String sessionEnergy, String sessionDuration,
                           String temperature, String voltage, String charger, Locale locale) {
        boolean english = isEnglish(locale);
        StringBuilder summary = new StringBuilder();
        append(summary, english ? "Charge status" : "Ladestatus", state, english);
        if (active) append(summary, english ? "Battery current" : "Akkustrom", current, english);
        append(summary, english ? "Charge target" : "Ladeziel", target, english);
        appendEstimate(summary, english ? "Time to charge target" : "Zeit bis Ladeziel", remaining, estimateSource, english);
        append(summary, english ? "Charge rate with screen on" : "Laderate bei Bildschirm an", screenOnRate, english);
        append(summary, english ? "Charge rate with screen off" : "Laderate bei Bildschirm aus", screenOffRate, english);
        append(summary, english ? "Energy charged" : "Geladene Energie", sessionEnergy, english);
        append(summary, english ? "Session duration" : "Sitzungsdauer", sessionDuration, english);
        append(summary, english ? "Temperature" : "Temperatur", temperature, english);
        append(summary, english ? "Voltage" : "Spannung", voltage, english);
        append(summary, english ? "Charging source" : "Ladequelle", charger, english);
        return summary.toString();
    }

    static String history(String period, String selectedRange,
                          BatteryHistoryStats.Bucket selected,
                          ArrayList<BatteryHistoryStats.Bucket> buckets,
                          boolean capacityAvailable) {
        return history(period, selectedRange, selected, buckets, capacityAvailable, Locale.GERMANY);
    }

    static String history(String period, String selectedRange,
                          BatteryHistoryStats.Bucket selected,
                          ArrayList<BatteryHistoryStats.Bucket> buckets,
                          boolean capacityAvailable, Locale locale) {
        boolean english = isEnglish(locale);
        String localizedPeriod = english ? englishLabel(period) : period;
        StringBuilder summary = new StringBuilder(english ? "History " : "Verlauf ").append(localizedPeriod);
        append(summary, english ? "Selected period" : "Ausgewählter Zeitraum", selectedRange, english);
        if (selected == null) {
            append(summary, english ? "Statistics" : "Statistik", english ? "no measurements" : "keine Messdaten", english);
        } else {
            append(summary, english ? "Charged" : "Aufgeladen", amount(selected.chargedMah, english), english);
            append(summary, english ? "Battery usage" : "Akkuverbrauch", amount(selected.consumedMah, english), english);
            append(summary, english ? "Battery wear" : "Akkuverschleiß", capacityAvailable
                    ? String.format(locale, "%.2f EFC", selected.wearCycles)
                    : (english ? "not available, design capacity missing" : "nicht berechenbar, Kapazität fehlt"), english);
            append(summary, english ? "Charged-to-used ratio" : "Lade-/Verbrauchsquote (geladen geteilt durch verbraucht)",
                    ratio(selected, english), english);
        }
        summary.append(english
                ? ". The charged-to-used ratio compares energy charged with energy used; it is not measured battery efficiency. Each metric uses its own chart scale: "
                : ". Die Lade-/Verbrauchsquote vergleicht geladene mit verbrauchter Energie und ist keine gemessene Akku-Effizienz. Balkenwerte (jede Kennzahl ist separat skaliert): ");
        boolean hasBars = false;
        if (buckets != null) {
            for (BatteryHistoryStats.Bucket bucket : buckets) {
                if (bucket.chargedMah <= 0 && bucket.consumedMah <= 0 && bucket.wearCycles <= 0f) continue;
                if (hasBars) summary.append(". ");
                summary.append(bucket.label).append(english ? ": charged " : ": aufgeladen ").append(amount(bucket.chargedMah, english))
                        .append(english ? ", used " : ", verbraucht ").append(amount(bucket.consumedMah, english));
                if (capacityAvailable) {
                    summary.append(english ? ", wear " : ", Verschleiß ")
                            .append(String.format(locale, "%.2f EFC", bucket.wearCycles));
                }
                summary.append(english ? ", charged-to-used ratio " : ", Lade-/Verbrauchsquote ")
                        .append(ratio(bucket, english));
                hasBars = true;
            }
        }
        if (!hasBars) summary.append(english ? "no measurements in the chart" : "keine Messdaten im Diagramm");
        if ("Monatlich".equals(period) || "Monthly".equals(period)) {
            summary.append(english
                    ? ". Older months may be blank because local telemetry is retained for about "
                    : ". Ältere Monate können leer sein, da lokale Telemetrie etwa ")
                    .append(BatterySamplingPolicy.retentionDays())
                    .append(english ? " days." : " Tage aufbewahrt wird.");
        }
        summary.append(english
                ? ". EFC means equivalent full cycles; it is not a direct measurement of chemical battery wear."
                : ". EFC sind äquivalente Vollzyklen, kein direkt gemessener chemischer Gesundheitsverlust.");
        return summary.toString();
    }

    static String health(String health, String capacity, String designCapacity,
                         String source, String status, String cycles, String wearImpact,
                         String temperature, String voltage) {
        return health(health, capacity, designCapacity, source, status, cycles, wearImpact,
                temperature, voltage, Locale.GERMANY);
    }

    static String health(String health, String capacity, String designCapacity,
                         String source, String status, String cycles, String wearImpact,
                         String temperature, String voltage, Locale locale) {
        boolean english = isEnglish(locale);
        StringBuilder summary = new StringBuilder(english ? "Battery health" : "Akkugesundheit");
        append(summary, english ? "Health" : "Gesundheit", health, english);
        append(summary, english ? "Estimated full capacity" : "Geschätzte Vollkapazität", capacity, english);
        append(summary, english ? "Design capacity" : "Designkapazität", designCapacity, english);
        append(summary, english ? "Measurement source" : "Messquelle", source, english);
        append(summary, english ? "Measurement status" : "Messstatus", status, english);
        append(summary, english ? "Charge cycles" : "Ladezyklen", cycles, english);
        append(summary, english ? "Wear at charge target" : "Belastung bis zum Ladeziel", wearImpact, english);
        append(summary, english ? "Temperature" : "Temperatur", temperature, english);
        append(summary, english ? "Voltage" : "Spannung", voltage, english);
        summary.append(english
                ? ". Capacity and wear are estimates, not direct chemical measurements."
                : ". Kapazität und Belastung sind Schätzungen, keine direkte chemische Messung.");
        return summary.toString();
    }

    private static String amount(int mah, boolean english) {
        return mah > 0 ? mah + " mAh" : (english ? "no measurements" : "keine Messdaten");
    }

    private static void appendEstimate(StringBuilder summary, String label,
                                       String value, String source, boolean english) {
        append(summary, label, value, english);
        if (isAvailable(value) && !(english ? "Reached" : "Erreicht").equals(value)
                && !(english ? "Full" : "Voll").equals(value)) {
            append(summary, english ? "Data source" : "Datenquelle", source, english);
        }
    }

    private static void appendDischargeEstimate(StringBuilder summary, String label,
                                                String value, String source, boolean english) {
        append(summary, label, value, english);
        if (isAvailable(value)) {
            append(summary, english ? "Data source" : "Datenquelle", source, english);
        } else if (isAvailable(source)) {
            append(summary, english ? "Note" : "Hinweis", source, english);
        }
    }

    private static void append(StringBuilder summary, String label, String value, boolean english) {
        if (summary.length() > 0) summary.append(". ");
        summary.append(label).append(": ");
        summary.append(!isAvailable(value)
                ? (english ? "not available" : "nicht verfügbar")
                : (english ? englishValue(value.trim()) : value.trim()));
    }

    private static String ratio(BatteryHistoryStats.Bucket bucket, boolean english) {
        return bucket.consumedMah > 0
                ? bucket.chargeConsumptionRatioPercent + (english ? "%" : " Prozent")
                : (english ? "not available" : "nicht verfügbar");
    }

    private static String englishLabel(String label) {
        if ("Bildschirm dauerhaft an".equals(label)) return "Runtime with screen always on";
        if ("Bildschirm aus".equals(label)) return "Runtime with screen off";
        if ("Normale Nutzung".equals(label)) return "Runtime with normal use";
        if ("Restlaufzeit bei normaler Nutzung".equals(label)) return "Estimated runtime with normal use";
        if ("Täglich".equals(label)) return "Daily";
        if ("Wöchentlich".equals(label)) return "Weekly";
        if ("Monatlich".equals(label)) return "Monthly";
        if ("Heute".equals(label)) return "Today";
        if ("Diese Woche".equals(label)) return "This week";
        if ("Diesen Monat".equals(label)) return "This month";
        if ("Lokale Telemetrie".equals(label)) return "Local telemetry";
        if ("Historische Schätzung".equals(label)) return "Historical estimate";
        if ("Keine Schätzung möglich".equals(label)) return "No estimate available";
        return label;
    }

    private static String englishValue(String value) {
        switch (value) {
            case "Nicht verfügbar": return "not available";
            case "Nicht gemessen": return "not measured";
            case "Keine Messung":
            case "Keine Messung vorhanden": return "no measurement";
            case "Erreicht": return "Reached";
            case "Voll": return "Full";
            case "Nicht verbunden": return "Not connected";
            case "Lädt schnell": return "Fast charging";
            case "Lädt": return "Charging";
            case "Zu kalt": return "Too cold";
            case "Zu heiß": return "Too hot";
            case "Akkuschonend": return "Battery protection";
            case "Adaptiv": return "Adaptive";
            case "Überhitzt": return "Overheated";
            case "Überspannung": return "Overvoltage";
            case "Akzeptabel": return "Fair";
            case "Sehr gut": return "Very good";
            case "Gut": return "Good";
            case "Kritisch": return "Critical";
            case "Fehler": return "Failure";
            case "Normalbetrieb": return "Normal operation";
            case "Laden gesperrt": return "Charging inhibited";
            case "Laden im Wachzustand gesperrt": return "Charging inhibited while awake";
            case "Entladung erzwungen": return "Forced discharge";
            case "Netzteil": return "Power adapter";
            case "USB-Ladegerät": return "USB charger";
            case "Kabellos": return "Wireless";
            case "Externe Stromquelle": return "External power source";
            case "Keine Schätzung möglich": return "No estimate available";
            case "Letzte Entladephase": return "Last discharge phase";
            case "Aktuelle Sitzung + lokale 7-Tage-Nutzung": return "Current session + local 7-day usage";
            case "Aktuelle Entladephase": return "Current discharge phase";
            case "Basierend auf lokaler 7-Tage-Nutzung": return "Based on local 7-day usage";
            case "Fuel-Gauge-Schätzung": return "Fuel gauge estimate";
            case "Android-Systemschätzung": return "Android system estimate";
            case "Momentanschätzung": return "Instantaneous estimate";
            case "Manuell festgelegt": return "Set manually";
            default:
                return value.replace(" Std.", " hr").replace(" Min.", " min")
                        .replace("Std.", "hr").replace("Min.", "min")
                        .replace(" Prozent", " percent");
        }
    }

    private static boolean isEnglish(Locale locale) {
        return locale != null && Locale.ENGLISH.getLanguage().equals(locale.getLanguage());
    }

    private static boolean isAvailable(String value) {
        return value != null && !value.trim().isEmpty() && !"—".equals(value.trim());
    }
}
