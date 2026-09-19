package com.ampere.batterylab;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** Math guards for the bounded minutes-to-target estimate. */
public class BatteryTimeEstimateTest {

    @Test public void invalidInputsReturnZero() {
        assertEquals(0L, BatteryTimeEstimate.minutesToTarget(-1, 80, 3000, 1500f, 1200));
        assertEquals(0L, BatteryTimeEstimate.minutesToTarget(80, 80, 3000, 1500f, 1200));
        assertEquals(0L, BatteryTimeEstimate.minutesToTarget(50, 80, 0, 1500f, 1200));
        // Already at target.
        assertEquals(0L, BatteryTimeEstimate.minutesToTarget(80, 50, 3000, 1500f, 1200));
    }

    @Test public void userPickedUnreasonableTargetReturnsZero() {
        assertEquals(0L, BatteryTimeEstimate.minutesToTarget(50, 150, 3000, 1500f, 1200));
    }

    @Test public void historicalRateWinsWhenAvailable() {
        // 30% of 3000 mAh missing = 900 mAh; 900 mAh at 1500 mA -> 36 minutes.
        assertEquals(36L, BatteryTimeEstimate.minutesToTarget(50, 80, 3000, 1500f, 0));
    }

    @Test public void currentUsableAsFallbackRate() {
        // 900 mAh missing at 1500 mA -> 36 minutes.
        assertEquals(36L, BatteryTimeEstimate.minutesToTarget(50, 80, 3000, 0f, 1500));
    }

    @Test public void unusableBothRatesReturnZero() {
        assertEquals(0L, BatteryTimeEstimate.minutesToTarget(50, 80, 3000, 0f, 0));
        assertEquals(0L, BatteryTimeEstimate.minutesToTarget(50, 80, 3000, 10f, 10));
        // Rate above MAX_RATE_MA is rejected.
        assertEquals(0L, BatteryTimeEstimate.minutesToTarget(50, 80, 3000, 200_000f, 0));
    }

    @Test public void currentBelowMinimumThresholdIsIgnored() {
        // 49 mA is below MIN_CURRENT_MA so historical rate (10 mA) is also too low.
        assertEquals(0L, BatteryTimeEstimate.minutesToTarget(50, 80, 3000, 10f, 49));
    }

    @Test public void nonFiniteHistoricalRateTreatedAsInvalid() {
        // Float.NaN is rejected, current 1500 mA wins -> 36 minutes.
        assertEquals(36L, BatteryTimeEstimate.minutesToTarget(50, 80, 3000, Float.NaN, 1500));
    }

    @Test public void trivialMissingCapacityClampsToOneMinute() {
        // 1% missing of 100 mAh = 1 mAh. 1 mAh at 50 mA -> ~1 minute.
        assertEquals(1L, BatteryTimeEstimate.minutesToTarget(51, 52, 100, 0f, 50));
    }

    @Test public void integerOverflowReturnsZero() {
        // capacityMah * (target - level) overflows long when capacity is huge.
        // The helper must catch this and return 0 instead of a nonsense minute value.
        assertEquals(0L, BatteryTimeEstimate.minutesToTarget(0, 100, Integer.MAX_VALUE, 0f, 1));
    }

    @Test public void shortSessionsRoundUpToOneMinute() {
        // 1 mAh missing at 60 mA -> 1 minute.
        assertEquals(1L, BatteryTimeEstimate.minutesToTarget(99, 100, 100, 0f, 60));
    }
}
