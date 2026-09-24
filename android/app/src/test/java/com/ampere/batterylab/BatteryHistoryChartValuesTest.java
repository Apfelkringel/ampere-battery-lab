package com.ampere.batterylab;

import org.junit.Test;
import java.util.Locale;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/** Formatting guards for the bucket value labels shown below the history chart. */
public class BatteryHistoryChartValuesTest {

    @Test public void nullBucketAlwaysEmitsDashPlaceholders() {
        assertArrayEquals(new String[]{"—", "—", "—", "—"},
                BatteryHistoryChartValues.forBucket(null, true));
    }

    @Test public void emptyBucketEmitsDashPlaceholders() {
        BatteryHistoryStats.Bucket bucket = new BatteryHistoryStats.Bucket(0, "");
        assertArrayEquals(new String[]{"—", "—", "—", "—"},
                BatteryHistoryChartValues.forBucket(bucket, true));
    }

    @Test public void chargedMahAndConsumedMahRenderAsPlainIntegers() {
        BatteryHistoryStats.Bucket bucket = new BatteryHistoryStats.Bucket(0, "");
        bucket.chargedMah = 1230;
        bucket.consumedMah = 845;
        bucket.wearCycles = 0.34f;
        bucket.chargeConsumptionRatioPercent = 145;
        bucket.measuredIntervals = 1;
        String[] labels = BatteryHistoryChartValues.forBucket(bucket, true);
        assertEquals("1230", labels[0]);
        assertEquals("845", labels[1]);
        assertEquals("0,34", labels[2]);
        assertEquals("145%", labels[3]);
    }

    @Test public void missingWearShowsDashWhenCapacityFlagIsFalse() {
        BatteryHistoryStats.Bucket bucket = new BatteryHistoryStats.Bucket(0, "");
        bucket.chargedMah = 100;
        bucket.consumedMah = 50;
        bucket.wearCycles = 0.5f;
        bucket.chargeConsumptionRatioPercent = 200;
        bucket.measuredIntervals = 1;
        String[] labels = BatteryHistoryChartValues.forBucket(bucket, false);
        assertEquals("—", labels[2]);
        // Consumption still drives the ratio so it must still render.
        assertEquals("200%", labels[3]);
    }

    @Test public void zeroConsumptionHidesRatioLabel() {
        BatteryHistoryStats.Bucket bucket = new BatteryHistoryStats.Bucket(0, "");
        bucket.chargedMah = 50;
        bucket.consumedMah = 0;
        bucket.wearCycles = 0.1f;
        bucket.chargeConsumptionRatioPercent = 99;
        bucket.measuredIntervals = 1;
        String[] labels = BatteryHistoryChartValues.forBucket(bucket, true);
        assertEquals("0,10", labels[2]);
        assertEquals("—", labels[3]);
    }

    @Test public void wearValueUsesRequestedNumberLocale() {
        BatteryHistoryStats.Bucket bucket = new BatteryHistoryStats.Bucket(0, "");
        bucket.chargedMah = 50;
        bucket.consumedMah = 20;
        bucket.wearCycles = .34f;
        bucket.chargeConsumptionRatioPercent = 250;
        bucket.measuredIntervals = 1;
        assertEquals("0.34", BatteryHistoryChartValues.forBucket(bucket, true, Locale.US)[2]);
        assertEquals("0,34", BatteryHistoryChartValues.forBucket(bucket, true, Locale.GERMANY)[2]);
    }
}
