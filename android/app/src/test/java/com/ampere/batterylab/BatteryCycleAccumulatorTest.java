package com.ampere.batterylab;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BatteryCycleAccumulatorTest {

    @Test public void dischargeStepAddsDifference() {
        assertEquals(70f, BatteryCycleAccumulator.addDischargePercent(50f, 80, 60, false), 0.01f);
    }

    @Test public void chargeStepDoesNotChangeCumulative() {
        assertEquals(50f, BatteryCycleAccumulator.addDischargePercent(50f, 80, 90, true), 0.01f);
    }

    @Test public void flatOrChargingStreakDoesNotChangeCumulative() {
        assertEquals(50f, BatteryCycleAccumulator.addDischargePercent(50f, 80, 80, false), 0.01f);
    }

    @Test public void missingBaselineDoesNotChangeCumulative() {
        assertEquals(50f, BatteryCycleAccumulator.addDischargePercent(50f, -1, 60, false), 0.01f);
    }

    @Test public void normalizesNegativeAccumulatorState() {
        assertEquals(15f, BatteryCycleAccumulator.addDischargePercent(-10f, 80, 65, false), 0.01f);
    }

    @Test public void completedCyclesRoundsDown() {
        assertEquals(0, BatteryCycleAccumulator.completedCycles(99.9f));
        assertEquals(1, BatteryCycleAccumulator.completedCycles(100f));
        assertEquals(1, BatteryCycleAccumulator.completedCycles(199.9f));
        assertEquals(2, BatteryCycleAccumulator.completedCycles(200f));
    }

    @Test public void completedCyclesNeverNegative() {
        assertEquals(0, BatteryCycleAccumulator.completedCycles(-5f));
        assertEquals(0, BatteryCycleAccumulator.completedCycles(Float.NaN));
    }

    @Test public void completedCyclesHandlesExactlyHundred() {
        assertEquals(1, BatteryCycleAccumulator.completedCycles(100f));
    }
}
