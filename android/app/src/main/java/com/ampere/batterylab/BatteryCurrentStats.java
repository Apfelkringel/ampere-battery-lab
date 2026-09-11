package com.ampere.batterylab;

import java.util.List;

/** Computes min/average/max over already validated current magnitudes. */
final class BatteryCurrentStats {
    private BatteryCurrentStats() { }

    static Summary summarize(List<Integer> magnitudes) {
        if (magnitudes == null || magnitudes.isEmpty()) return Summary.unavailable();
        int minimum = Integer.MAX_VALUE;
        int maximum = 0;
        long total = 0L;
        int count = 0;
        for (Integer value : magnitudes) {
            if (value == null || value <= 0) continue;
            minimum = Math.min(minimum, value);
            maximum = Math.max(maximum, value);
            total += value;
            count++;
        }
        return count > 0 ? new Summary(minimum, Math.round(total / (float) count), maximum, count)
                : Summary.unavailable();
    }

    static final class Summary {
        final int minimumMa;
        final int averageMa;
        final int maximumMa;
        final int sampleCount;

        private Summary(int minimumMa, int averageMa, int maximumMa, int sampleCount) {
            this.minimumMa = minimumMa;
            this.averageMa = averageMa;
            this.maximumMa = maximumMa;
            this.sampleCount = sampleCount;
        }

        static Summary unavailable() { return new Summary(0, 0, 0, 0); }

        boolean isAvailable() { return sampleCount > 0; }
    }
}
