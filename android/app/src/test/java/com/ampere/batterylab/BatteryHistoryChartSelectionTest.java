package com.ampere.batterylab;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** Hit-test guards for the history-chart bucket picker. */
public class BatteryHistoryChartSelectionTest {

    @Test public void zeroBucketsNeverHits() {
        assertEquals(-1, BatteryHistoryChartSelection.bucketIndexAt(50f, 0f, 100f, 0));
    }

    @Test public void negativeBucketsNeverHits() {
        assertEquals(-1, BatteryHistoryChartSelection.bucketIndexAt(50f, 0f, 100f, -3));
    }

    @Test public void touchBeforeLeftEdgeNeverHits() {
        assertEquals(-1, BatteryHistoryChartSelection.bucketIndexAt(-0.1f, 0f, 100f, 4));
    }

    @Test public void touchAtRightEdgeNeverHits() {
        // The last bucket starts strictly before the right edge, so a tap
        // exactly at the edge stays out of range and falls back to the
        // surrounding controls instead of overflowing.
        assertEquals(-1, BatteryHistoryChartSelection.bucketIndexAt(100f, 0f, 100f, 4));
    }

    @Test public void inRangeTouchResolvesToFirstBucket() {
        assertEquals(0, BatteryHistoryChartSelection.bucketIndexAt(10f, 0f, 100f, 4));
    }

    @Test public void inRangeTouchResolvesToMiddleBucket() {
        // 50f / 100f * 4 = 2.0 -> bucket index 2 (third of four).
        assertEquals(2, BatteryHistoryChartSelection.bucketIndexAt(50f, 0f, 100f, 4));
        // 30f / 100f * 4 = 1.2 -> cast to int yields 1 (second bucket).
        assertEquals(1, BatteryHistoryChartSelection.bucketIndexAt(30f, 0f, 100f, 4));
    }

    @Test public void inRangeTouchClipsToLastBucket() {
        // 90f / 100f * 4 = 3.6 -> bucket 3 (last bucket).
        assertEquals(3, BatteryHistoryChartSelection.bucketIndexAt(90f, 0f, 100f, 4));
        // A coordinate just below the right edge must still resolve to a hit.
        assertEquals(3, BatteryHistoryChartSelection.bucketIndexAt(99.9f, 0f, 100f, 4));
    }

    @Test public void nonFiniteInputNeverHits() {
        assertEquals(-1, BatteryHistoryChartSelection.bucketIndexAt(Float.NaN, 0f, 100f, 4));
        assertEquals(-1, BatteryHistoryChartSelection.bucketIndexAt(50f, Float.NaN, 100f, 4));
        assertEquals(-1, BatteryHistoryChartSelection.bucketIndexAt(50f, 0f, Float.NaN, 4));
    }

    @Test public void degenerateRangeNeverHits() {
        // right <= left means there is no bucket geometry; never accept a hit.
        assertEquals(-1, BatteryHistoryChartSelection.bucketIndexAt(50f, 100f, 100f, 4));
        assertEquals(-1, BatteryHistoryChartSelection.bucketIndexAt(50f, 200f, 100f, 4));
    }
}
