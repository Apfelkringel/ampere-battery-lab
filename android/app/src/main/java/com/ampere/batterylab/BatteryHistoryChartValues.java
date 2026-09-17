package com.ampere.batterylab;

import java.util.Locale;

/** Exact values shown directly below each independently scaled history-chart group. */
final class BatteryHistoryChartValues {
    private BatteryHistoryChartValues() { }

    static String[] forBucket(BatteryHistoryStats.Bucket bucket, boolean hasCapacity) {
        if (bucket == null || bucket.measuredIntervals <= 0) {
            return new String[]{"—", "—", "—", "—"};
        }
        return new String[]{Integer.toString(bucket.chargedMah),
                Integer.toString(bucket.consumedMah),
                hasCapacity ? String.format(Locale.GERMANY, "%.2f", bucket.wearCycles) : "—",
                bucket.consumedMah > 0 ? bucket.chargeConsumptionRatioPercent + "%" : "—"};
    }
}
