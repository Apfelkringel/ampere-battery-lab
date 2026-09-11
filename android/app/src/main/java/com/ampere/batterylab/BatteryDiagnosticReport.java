package com.ampere.batterylab;

import java.util.Locale;

/** Builds a deterministic, human-readable local report from telemetry diagnostics. */
final class BatteryDiagnosticReport {
    private BatteryDiagnosticReport() { }

    static String build(String serialized, long samplingIntervalMs, long generatedAtMs) {
        BatteryTelemetryDiagnostics.Summary summary = BatteryTelemetryDiagnostics.analyze(
                serialized, samplingIntervalMs);
        StringBuilder report = new StringBuilder();
        report.append("Ampere Battery Lab — Diagnosebericht\n");
        report.append("=================================\n");
        report.append("Erstellt (Unix ms): ").append(Math.max(0L, generatedAtMs)).append('\n');
        report.append("Messwerte: ").append(summary.sampleCount).append('\n');
        report.append("Entlade-Messwerte: ").append(summary.dischargeSamples).append('\n');
        report.append('\n');

        if (!summary.hasSamples()) {
            report.append("Noch keine auswertbaren Telemetriedaten vorhanden.\n");
            return report.toString();
        }

        report.append("MESSDIAGNOSE\n");
        report.append("-------------\n");
        if (summary.maxTemperatureTenths > 0) {
            report.append(String.format(Locale.GERMANY, "Max. Akkutemperatur: %.1f °C\n",
                    summary.maxTemperatureTenths / 10f));
        } else {
            report.append("Max. Akkutemperatur: nicht verfügbar\n");
        }
        if (summary.hasVoltageData()) {
            report.append(String.format(Locale.GERMANY, "Min. Entladespannung: %.3f V bei %d %%\n",
                    summary.minDischargeVoltageMv / 1000f, summary.minDischargeVoltageLevel));
        } else {
            report.append("Min. Entladespannung: nicht verfügbar\n");
        }
        report.append("Spitzen-Entladestrom: ").append(summary.peakDischargeMa > 0
                ? summary.peakDischargeMa + " mA" : "nicht verfügbar").append('\n');
        report.append("Größte Messlücke: ").append(formatDuration(summary.largestGapMs)).append('\n');
        report.append('\n');

        report.append("HINWEISE\n");
        report.append("--------\n");
        boolean flagged = false;
        if (summary.hasHighTemperature()) {
            report.append("WARNUNG: Akku erreichte mindestens 48 °C.\n");
            flagged = true;
        }
        if (summary.samplingGap) {
            report.append("HINWEIS: Es existiert mindestens eine längere Sampling-Lücke; "
                    + "Raten über diese Lücke werden nicht als Messung gewertet.\n");
            flagged = true;
        }
        if (!flagged) report.append("Keine konservative Auffälligkeit erkannt.\n");
        report.append("Spannung wird angezeigt, aber nicht mit einer festen 2S-Pack-Grenze "
                + "als Spannungssag bewertet.\n");
        report.append("Ein laufender Entladevorgang wird nicht als Early Cutoff bewertet.\n");
        return report.toString();
    }

    private static String formatDuration(long durationMs) {
        if (durationMs <= 0L) return "nicht verfügbar";
        long minutes = durationMs / 60000L;
        if (minutes < 60L) return minutes + " Min.";
        return (minutes / 60L) + " Std. " + (minutes % 60L) + " Min.";
    }
}
