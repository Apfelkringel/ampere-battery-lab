package com.ampere.batterylab;

import java.util.ArrayList;

/**
 * Computes conservative diagnostics from the local telemetry stream.
 *
 * BatteryLog's report is deliberately device-specific: its voltage-sag rule
 * assumes an AYANEO 2S pack and its early-cutoff rule assumes a finished CSV
 * run. Ampere stays device-agnostic and never calls a live, still-running
 * discharge an anomaly merely because the latest sample is above zero.
 */
final class BatteryTelemetryDiagnostics {
    static final int HIGH_TEMPERATURE_TENTHS = 480;

    private BatteryTelemetryDiagnostics() { }

    static Summary analyze(String serialized, long samplingIntervalMs) {
        ArrayList<String> rows = BatteryExportRules.validTelemetryRows(serialized);
        int minTemperatureTenths = 0;
        long temperatureSumTenths = 0L;
        int temperatureSamples = 0;
        int maxTemperatureTenths = 0;
        int minDischargeVoltageMv = 0;
        int minDischargeVoltageLevel = -1;
        int peakDischargeMa = 0;
        long largestGapMs = 0L;
        int dischargeSamples = 0;
        long previousTimestamp = 0L;

        for (String row : rows) {
            String[] parts = row.split(",", 11);
            if (parts.length < 8) continue;
            try {
                long timestamp = Long.parseLong(parts[0].trim());
                int level = BatteryLevel.normalizePercent(Integer.parseInt(parts[1].trim()));
                boolean charging = "1".equals(parts[2]);
                int current = Integer.parseInt(parts[3].trim());
                int temperatureTenths = Math.round(Float.parseFloat(parts[4].trim()) * 10f);
                int voltageMv = Math.round(Float.parseFloat(parts[5].trim()) * 1000f);
                if (temperatureTenths > 0) {
                    if (minTemperatureTenths == 0 || temperatureTenths < minTemperatureTenths) {
                        minTemperatureTenths = temperatureTenths;
                    }
                    temperatureSumTenths += temperatureTenths;
                    temperatureSamples++;
                    if (temperatureTenths > maxTemperatureTenths) maxTemperatureTenths = temperatureTenths;
                }
                if (previousTimestamp > 0L && timestamp >= previousTimestamp) {
                    largestGapMs = Math.max(largestGapMs, timestamp - previousTimestamp);
                }
                previousTimestamp = timestamp;
                if (!charging) {
                    dischargeSamples++;
                    if (voltageMv > 0 && (minDischargeVoltageMv == 0 || voltageMv < minDischargeVoltageMv)) {
                        minDischargeVoltageMv = voltageMv;
                        minDischargeVoltageLevel = level;
                    }
                    if (current < 0) peakDischargeMa = Math.max(peakDischargeMa, Math.abs(current));
                }
            } catch (NumberFormatException ignored) {
                // validTelemetryRows already filters the row; keep this final
                // guard so a future schema extension cannot break the UI.
            }
        }

        boolean samplingGap = false;
        if (rows.size() >= 2 && samplingIntervalMs > 0L) {
            for (int i = 1; i < rows.size(); i++) {
                try {
                    long previous = Long.parseLong(rows.get(i - 1).substring(0, rows.get(i - 1).indexOf(',')));
                    long current = Long.parseLong(rows.get(i).substring(0, rows.get(i).indexOf(',')));
                    if (BatteryTimelineRules.isSamplingGap(previous, current, samplingIntervalMs)) {
                        samplingGap = true;
                        break;
                    }
                } catch (RuntimeException ignored) { }
            }
        }
        int averageTemperatureTenths = temperatureSamples > 0
                ? Math.round(temperatureSumTenths / (float) temperatureSamples) : 0;
        return new Summary(rows.size(), dischargeSamples, minTemperatureTenths,
                averageTemperatureTenths, maxTemperatureTenths,
                minDischargeVoltageMv, minDischargeVoltageLevel, peakDischargeMa,
                largestGapMs, samplingGap, BatteryPowerStats.analyzeRows(rows));
    }

    static final class Summary {
        final int sampleCount;
        final int dischargeSamples;
        final int minTemperatureTenths;
        final int averageTemperatureTenths;
        final int maxTemperatureTenths;
        final int minDischargeVoltageMv;
        final int minDischargeVoltageLevel;
        final int peakDischargeMa;
        final long largestGapMs;
        final boolean samplingGap;
        final BatteryPowerStats.Summary powerStats;

        Summary(int sampleCount, int dischargeSamples, int minTemperatureTenths,
                int averageTemperatureTenths, int maxTemperatureTenths,
                int minDischargeVoltageMv, int minDischargeVoltageLevel, int peakDischargeMa,
                long largestGapMs, boolean samplingGap, BatteryPowerStats.Summary powerStats) {
            this.sampleCount = sampleCount;
            this.dischargeSamples = dischargeSamples;
            this.minTemperatureTenths = minTemperatureTenths;
            this.averageTemperatureTenths = averageTemperatureTenths;
            this.maxTemperatureTenths = maxTemperatureTenths;
            this.minDischargeVoltageMv = minDischargeVoltageMv;
            this.minDischargeVoltageLevel = minDischargeVoltageLevel;
            this.peakDischargeMa = peakDischargeMa;
            this.largestGapMs = largestGapMs;
            this.samplingGap = samplingGap;
            this.powerStats = powerStats;
        }

        boolean hasHighTemperature() {
            return maxTemperatureTenths >= HIGH_TEMPERATURE_TENTHS;
        }

        boolean hasTemperatureData() {
            return minTemperatureTenths > 0 && averageTemperatureTenths > 0 && maxTemperatureTenths > 0;
        }

        boolean hasVoltageData() {
            return minDischargeVoltageMv > 0;
        }

        boolean hasSamples() {
            return sampleCount > 0;
        }
    }
}
