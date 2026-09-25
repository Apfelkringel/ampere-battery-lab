package com.ampere.batterylab;

import java.util.Locale;

/** Builds a deterministic, human-readable local report from telemetry diagnostics. */
final class BatteryDiagnosticReport {
    private BatteryDiagnosticReport() { }

    static String build(String serialized, long samplingIntervalMs, long generatedAtMs) {
        return build(serialized, samplingIntervalMs, generatedAtMs, Locale.GERMANY);
    }

    static String build(String serialized, long samplingIntervalMs, long generatedAtMs, Locale locale) {
        Locale safeLocale = locale == null ? Locale.GERMANY : locale;
        boolean english = !"de".equalsIgnoreCase(safeLocale.getLanguage());
        BatteryTelemetryDiagnostics.Summary summary = BatteryTelemetryDiagnostics.analyze(
                serialized, samplingIntervalMs);
        StringBuilder report = new StringBuilder();
        report.append(english ? "AkkuTakt — Diagnostic report\n"
                : "AkkuTakt — Diagnosebericht\n");
        report.append("=================================\n");
        report.append(english ? "Generated (Unix ms): " : "Erstellt (Unix ms): ")
                .append(Math.max(0L, generatedAtMs)).append('\n');
        report.append(english ? "Readings: " : "Messwerte: ").append(summary.sampleCount).append('\n');
        report.append(english ? "Discharge readings: " : "Entlade-Messwerte: ")
                .append(summary.dischargeSamples).append('\n');
        report.append('\n');

        if (!summary.hasSamples()) {
            report.append(english ? "No usable telemetry data yet.\n"
                    : "Noch keine auswertbaren Telemetriedaten vorhanden.\n");
            return report.toString();
        }

        report.append(english ? "MEASUREMENT DIAGNOSTICS\n" : "MESSDIAGNOSE\n");
        report.append("-------------\n");
        if (summary.maxTemperatureTenths > 0) {
            report.append(String.format(safeLocale, english
                            ? "Max. battery temperature: %.1f °C\n" : "Max. Akkutemperatur: %.1f °C\n",
                    summary.maxTemperatureTenths / 10f));
        } else {
            report.append(english ? "Max. battery temperature: unavailable\n"
                    : "Max. Akkutemperatur: nicht verfügbar\n");
        }
        if (summary.hasVoltageData()) {
            report.append(String.format(safeLocale, english
                            ? "Min. discharge voltage: %.3f V at %d %%\n"
                            : "Min. Entladespannung: %.3f V bei %d %%\n",
                    summary.minDischargeVoltageMv / 1000f, summary.minDischargeVoltageLevel));
        } else {
            report.append(english ? "Min. discharge voltage: unavailable\n"
                    : "Min. Entladespannung: nicht verfügbar\n");
        }
        report.append(english ? "Peak discharge current: " : "Spitzen-Entladestrom: ")
                .append(summary.peakDischargeMa > 0 ? summary.peakDischargeMa + " mA"
                        : (english ? "unavailable" : "nicht verfügbar")).append('\n');
        report.append(english ? "Longest sampling gap: " : "Größte Messlücke: ")
                .append(formatDuration(summary.largestGapMs, english)).append('\n');
        report.append('\n');

        report.append(english ? "NOTES\n" : "HINWEISE\n");
        report.append("--------\n");
        boolean flagged = false;
        if (summary.hasHighTemperature()) {
            report.append(english ? "WARNING: Battery reached at least 48 °C.\n"
                    : "WARNUNG: Akku erreichte mindestens 48 °C.\n");
            flagged = true;
        }
        if (summary.samplingGap) {
            report.append(english
                    ? "NOTE: At least one extended sampling gap exists; rates across this gap are not treated as measurements.\n"
                    : "HINWEIS: Es existiert mindestens eine längere Sampling-Lücke; "
                    + "Raten über diese Lücke werden nicht als Messung gewertet.\n");
            flagged = true;
        }
        if (!flagged) report.append(english ? "No conservative issues detected.\n"
                : "Keine konservative Auffälligkeit erkannt.\n");
        report.append(english
                ? "Voltage is shown but is not assessed for voltage sag against a fixed 2S pack limit.\n"
                : "Spannung wird angezeigt, aber nicht mit einer festen 2S-Pack-Grenze "
                + "als Spannungssag bewertet.\n");
        report.append(english ? "An ongoing discharge is not classified as an early cutoff.\n"
                : "Ein laufender Entladevorgang wird nicht als Early Cutoff bewertet.\n");
        return report.toString();
    }

    private static String formatDuration(long durationMs, boolean english) {
        if (durationMs <= 0L) return english ? "unavailable" : "nicht verfügbar";
        long minutes = durationMs / 60000L;
        if (minutes < 60L) return minutes + (english ? " min" : " Min.");
        return (minutes / 60L) + (english ? " hr " : " Std. ")
                + (minutes % 60L) + (english ? " min" : " Min.");
    }
}
