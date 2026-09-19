package com.ampere.batterylab;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** Edge-trigger guards for the charge-target alarm. */
public class BatteryChargeAlarmTest {

    @Test public void downbeatsDisabledWhenNotCharging() {
        assertEquals(false, BatteryChargeAlarm.shouldAlert(90, false, 80, false, 75));
    }

    @Test public void downbeatsDisabledOnceAlreadySent() {
        assertEquals(false, BatteryChargeAlarm.shouldAlert(85, true, 80, true, 75));
    }

    @Test public void missingBaselineFiresImmediatelyAtOrAboveTarget() {
        // previousLevel < 0 is treated as a first valid sample above the target.
        assertEquals(true, BatteryChargeAlarm.shouldAlert(80, true, 80, false, -1));
        assertEquals(true, BatteryChargeAlarm.shouldAlert(82, true, 80, false, -1));
    }

    @Test public void missingBaselineBelowTargetStaysSilent() {
        assertEquals(false, BatteryChargeAlarm.shouldAlert(70, true, 80, false, -1));
    }

    @Test public void upwardCrossingFires() {
        assertEquals(true, BatteryChargeAlarm.shouldAlert(81, true, 80, false, 79));
        assertEquals(true, BatteryChargeAlarm.shouldAlert(80, true, 80, false, 79));
    }

    @Test public void alreadyAboveTargetDoesNotRefire() {
        assertEquals(false, BatteryChargeAlarm.shouldAlert(85, true, 80, false, 85));
    }

    @Test public void invalidLevelKeepsAlarmSilent() {
        assertEquals(false, BatteryChargeAlarm.shouldAlert(-1, true, 80, false, 70));
    }

    @Test public void resetFiresWhenNoLongerCharging() {
        assertEquals(true, BatteryChargeAlarm.shouldReset(85, false, 80));
    }

    @Test public void resetFiresBelowHysteresisBand() {
        // HYSTERESIS_PERCENT = 3 -> reset at or below target - 3.
        assertEquals(true, BatteryChargeAlarm.shouldReset(77, true, 80));
        assertEquals(false, BatteryChargeAlarm.shouldReset(78, true, 80));
    }

    @Test public void resetFiresOnInvalidLevel() {
        assertEquals(true, BatteryChargeAlarm.shouldReset(-1, true, 80));
    }
}
