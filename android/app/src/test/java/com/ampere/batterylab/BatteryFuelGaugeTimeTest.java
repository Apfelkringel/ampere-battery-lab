package com.ampere.batterylab;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** Pure-unit guards for the kernel-fuel-gauge normalizer. */
public class BatteryFuelGaugeTimeTest {

    @Test public void normalizeSecondsRejectsZeroAndNegative() {
        assertEquals(0, BatteryFuelGaugeTime.normalizeSeconds(0L));
        assertEquals(0, BatteryFuelGaugeTime.normalizeSeconds(-1L));
    }

    @Test public void normalizeSecondsRejectsSentinelBeyondLimit() {
        // 48h is the largest plausible value; anything above is treated as
        // OEM noise and mapped to 0.
        assertEquals(0, BatteryFuelGaugeTime.normalizeSeconds(48L * 60L * 60L + 1));
    }

    @Test public void normalizeSecondsTruncatesToMinutes() {
        // 90 seconds -> 1 minute (truncation, not rounding).
        assertEquals(1, BatteryFuelGaugeTime.normalizeSeconds(90L));
    }

    @Test public void normalizeSecondsAcceptsValidValues() {
        assertEquals(60, BatteryFuelGaugeTime.normalizeSeconds(60L * 60L));
        assertEquals(48L * 60L, BatteryFuelGaugeTime.normalizeSeconds(48L * 60L * 60L));
        // 120 seconds -> 2 minutes (truncation).
        assertEquals(2, BatteryFuelGaugeTime.normalizeSeconds(120L));
    }

    @Test public void fullMinutesPrefersInstantaneousEstimate() {
        assertEquals(120, BatteryFuelGaugeTime.fullMinutes(7200L, 5L * 3600L));
    }

    @Test public void fullMinutesFallsBackToAverageWhenInstantaneousMissing() {
        assertEquals(120, BatteryFuelGaugeTime.fullMinutes(0L, 7200L));
    }

    @Test public void fullMinutesReturnsZeroWhenBothInvalid() {
        assertEquals(0, BatteryFuelGaugeTime.fullMinutes(0L, 0L));
        assertEquals(0, BatteryFuelGaugeTime.fullMinutes(-1L, 0L));
        assertEquals(0, BatteryFuelGaugeTime.fullMinutes(0L, 48L * 60L * 60L + 1));
    }
}
