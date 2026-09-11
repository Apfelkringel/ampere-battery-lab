package com.ampere.batterylab;

import java.util.ArrayList;

/**
 * Derives battery-side power from the existing signed current and voltage
 * telemetry. The value is deliberately calculated at read/export time so old
 * 11-column telemetry remains compatible and no second source of truth is
 * persisted.
 */
final class BatteryPowerStats {
    private BatteryPowerStats() { }

    static Summary analyze(String serialized) {
        return analyzeRows(BatteryExportRules.validTelemetryRows(serialized));
    }

    static Summary analyzeRows(ArrayList<String> rows) {
        Mutable charging = new Mutable();
        Mutable discharging = new Mutable();
        if (rows != null) {
            for (String row : rows) {
                String[] parts = row.split(",", -1);
                if (parts.length < 11) continue;
                int powerMw = milliWatts(parts);
                if (powerMw <= 0) continue;
                if ("1".equals(parts[2])) charging.add(powerMw);
                else if ("0".equals(parts[2])) discharging.add(powerMw);
            }
        }
        return new Summary(charging.finish(), discharging.finish());
    }

    /** Returns the same validated battery-side value used by the statistics. */
    static int milliWatts(String[] telemetryParts) {
        if (telemetryParts == null || telemetryParts.length < 6) return 0;
        try {
            int currentMa = Integer.parseInt(telemetryParts[3].trim());
            int voltageMv = Math.round(Float.parseFloat(telemetryParts[5].trim()) * 1000f);
            return BatteryPower.milliWatts(currentMa, voltageMv);
        } catch (RuntimeException ignored) {
            return 0;
        }
    }

    static final class Summary {
        final Range charging;
        final Range discharging;

        private Summary(Range charging, Range discharging) {
            this.charging = charging;
            this.discharging = discharging;
        }

        boolean hasData() { return charging.isAvailable() || discharging.isAvailable(); }
    }

    static final class Range {
        final int minimumMw;
        final int averageMw;
        final int maximumMw;
        final int sampleCount;

        private Range(int minimumMw, int averageMw, int maximumMw, int sampleCount) {
            this.minimumMw = minimumMw;
            this.averageMw = averageMw;
            this.maximumMw = maximumMw;
            this.sampleCount = sampleCount;
        }

        static Range unavailable() { return new Range(0, 0, 0, 0); }

        boolean isAvailable() { return sampleCount > 0; }
    }

    private static final class Mutable {
        int minimum = Integer.MAX_VALUE;
        int maximum = 0;
        long total = 0L;
        int count = 0;

        void add(int powerMw) {
            minimum = Math.min(minimum, powerMw);
            maximum = Math.max(maximum, powerMw);
            total += powerMw;
            count++;
        }

        Range finish() {
            return count == 0
                    ? Range.unavailable()
                    : new Range(minimum, Math.round(total / (float) count), maximum, count);
        }
    }
}
