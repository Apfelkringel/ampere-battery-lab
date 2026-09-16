package com.ampere.batterylab;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BatteryHistoryChartSelectionTest {
    @Test public void mapsTouchesToExactBucketsAndExcludesPlotEdges() {
        assertEquals(-1, BatteryHistoryChartSelection.bucketIndexAt(35.9f, 36f, 284f, 7));
        assertEquals(0, BatteryHistoryChartSelection.bucketIndexAt(36f, 36f, 284f, 7));
        assertEquals(3, BatteryHistoryChartSelection.bucketIndexAt(160f, 36f, 284f, 7));
        assertEquals(6, BatteryHistoryChartSelection.bucketIndexAt(283.9f, 36f, 284f, 7));
        assertEquals(-1, BatteryHistoryChartSelection.bucketIndexAt(284f, 36f, 284f, 7));
    }

    @Test public void handlesMonthlyBucketsWithoutOutOfRangeIndices() {
        assertEquals(29, BatteryHistoryChartSelection.bucketIndexAt(349.9f, 36f, 350f, 30));
        assertEquals(-1, BatteryHistoryChartSelection.bucketIndexAt(100f, 36f, 350f, 0));
    }
}
