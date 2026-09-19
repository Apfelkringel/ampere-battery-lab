package com.ampere.batterylab;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** Pure-unit guards for the helper that converts raw microamp readings. */
public class BatteryCurrentTest {

    @Test public void zeroMicroampsMapsToZeroMilliamps() {
        assertEquals(0, BatteryCurrent.fromMicroamps(0L));
    }

    @Test public void negativeMicroampsMapsToAbsoluteMagnitude() {
        assertEquals(250, BatteryCurrent.fromMicroamps(-250000L));
    }

    @Test public void positiveMicroampsMapToMagnitude() {
        assertEquals(750, BatteryCurrent.fromMicroamps(750000L));
    }

    @Test public void integerMinValueIsRejected() {
        assertEquals(0, BatteryCurrent.fromMicroamps(Integer.MIN_VALUE));
    }

    @Test public void magnitudeBelowMinimumIsRejected() {
        assertEquals(0, BatteryCurrent.fromMicroamps(999L));
        assertEquals(0, BatteryCurrent.fromMicroamps(-999L));
    }

    @Test public void magnitudeAtMinimumBoundaryPasses() {
        assertEquals(1, BatteryCurrent.fromMicroamps(1000L));
    }

    @Test public void magnitudeAboveMaximumIsRejected() {
        assertEquals(0, BatteryCurrent.fromMicroamps(101000000L));
        assertEquals(0, BatteryCurrent.fromMicroamps(-101000000L));
    }

    @Test public void magnitudeAtMaximumBoundaryPasses() {
        assertEquals(100000, BatteryCurrent.fromMicroamps(100000000L));
    }

    @Test public void truncatesRemainder() {
        assertEquals(1, BatteryCurrent.fromMicroamps(1234L));
        assertEquals(1, BatteryCurrent.fromMicroamps(1999L));
    }
}
