package com.ampere.batterylab;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

public class BatteryHistoryChartValuesTest {
    @Test public void showsExactValuesInLegendSeriesOrder() {
        BatteryHistoryStats.Bucket bucket = new BatteryHistoryStats.Bucket(1L, "Mo.");
        bucket.measuredIntervals = 2;
        bucket.chargedMah = 1250;
        bucket.consumedMah = 500;
        bucket.wearCycles = .25f;
        bucket.chargeConsumptionRatioPercent = 250;

        assertArrayEquals(new String[]{"1250", "500", "0,25", "250%"},
                BatteryHistoryChartValues.forBucket(bucket, true));
    }

    @Test public void distinguishesMissingDataFromZeroAndUnavailableCapacity() {
        BatteryHistoryStats.Bucket missing = new BatteryHistoryStats.Bucket(1L, "Mo.");
        assertArrayEquals(new String[]{"—", "—", "—", "—"},
                BatteryHistoryChartValues.forBucket(missing, true));

        BatteryHistoryStats.Bucket chargeOnly = new BatteryHistoryStats.Bucket(1L, "Di.");
        chargeOnly.measuredIntervals = 1;
        chargeOnly.chargedMah = 80;
        assertArrayEquals(new String[]{"80", "0", "—", "—"},
                BatteryHistoryChartValues.forBucket(chargeOnly, false));
    }
}
