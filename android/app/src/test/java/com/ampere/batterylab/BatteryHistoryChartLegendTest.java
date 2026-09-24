package com.ampere.batterylab;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Locale;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

public class BatteryHistoryChartLegendTest {
    @Test public void showsTheActualScaleForEachColorSeries() {
        ArrayList<BatteryHistoryStats.Bucket> buckets = new ArrayList<>();
        BatteryHistoryStats.Bucket first = new BatteryHistoryStats.Bucket(1L, "Mo.");
        first.chargedMah = 10;
        first.consumedMah = 20;
        first.wearCycles = .02f;
        first.chargeConsumptionRatioPercent = 50;
        buckets.add(first);
        BatteryHistoryStats.Bucket second = new BatteryHistoryStats.Bucket(2L, "Di.");
        second.chargedMah = 30;
        second.consumedMah = 10;
        second.wearCycles = .01f;
        second.chargeConsumptionRatioPercent = 300;
        buckets.add(second);

        assertArrayEquals(new String[]{"Geladen · max 30 mAh", "Verbrauch · max 20 mAh",
                        "Verschleiß · max 0,02 EFC", "Ladequote · max 300%"},
                BatteryHistoryChartLegend.labels(buckets));
    }

    @Test public void missingMeasurementsAreNotPresentedAsZero() {
        ArrayList<BatteryHistoryStats.Bucket> buckets = new ArrayList<>();
        buckets.add(new BatteryHistoryStats.Bucket(1L, "Mo."));

        assertArrayEquals(new String[]{"Geladen · max —", "Verbrauch · max —",
                        "Verschleiß · max —", "Ladequote · max —"},
                BatteryHistoryChartLegend.labels(buckets));
    }

    @Test public void wearScaleUsesRequestedNumberLocale() {
        ArrayList<BatteryHistoryStats.Bucket> buckets = new ArrayList<>();
        BatteryHistoryStats.Bucket bucket = new BatteryHistoryStats.Bucket(1L, "Mo.");
        bucket.wearCycles = .34f;
        buckets.add(bucket);
        assertTrue(BatteryHistoryChartLegend.labels(buckets, Locale.US)[2]
                .contains("0.34 EFC"));
        assertTrue(BatteryHistoryChartLegend.labels(buckets, Locale.GERMANY)[2]
                .contains("0,34 EFC"));
    }
}
