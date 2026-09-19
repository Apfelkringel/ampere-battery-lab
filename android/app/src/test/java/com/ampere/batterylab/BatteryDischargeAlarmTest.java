package com.ampere.batterylab;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** Edge-trigger guards for the low-battery alarm. */
public class BatteryDischargeAlarmTest {

    @Test public void defaultThresholdFallsInsideAllowedRange() {
        assertEquals(15, BatteryDischargeAlarm.normalizeThreshold(BatteryDischargeAlarm.DEFAULT_THRESHOLD));
    }

    @Test public void thresholdClampsToAllowedRange() {
        // Below the allowed minimum the threshold snaps to MIN_THRESHOLD (5).
        assertEquals(5, BatteryDischargeAlarm.normalizeThreshold(0));
        assertEquals(5, BatteryDischargeAlarm.normalizeThreshold(3));
        assertEquals(5, BatteryDischargeAlarm.normalizeThreshold(4));
        // Above the allowed maximum the threshold snaps to 50.
        assertEquals(50, BatteryDischargeAlarm.normalizeThreshold(120));
        assertEquals(50, BatteryDischargeAlarm.normalizeThreshold(Integer.MAX_VALUE));
        // Values inside the allowed range are preserved.
        assertEquals(5, BatteryDischargeAlarm.normalizeThreshold(5));
        assertEquals(15, BatteryDischargeAlarm.normalizeThreshold(15));
        assertEquals(20, BatteryDischargeAlarm.normalizeThreshold(20));
        assertEquals(50, BatteryDischargeAlarm.normalizeThreshold(50));
    }

    @Test public void chargingDeviceSuppressesAlarm() {
        assertEquals(false, BatteryDischargeAlarm.shouldAlert(10, true, 15, false, 25));
    }

    @Test public void alreadySentAlarmIsNotRepeated() {
        assertEquals(false, BatteryDischargeAlarm.shouldAlert(10, false, 15, true, 20));
    }

    @Test public void invalidLevelSuppressesAlarm() {
        assertEquals(false, BatteryDischargeAlarm.shouldAlert(-1, false, 15, false, 25));
    }

    @Test public void levelAboveThresholdSuppressesAlarm() {
        assertEquals(false, BatteryDischargeAlarm.shouldAlert(80, false, 15, false, 90));
    }

    @Test public void firstSampleInLowRangeFires() {
        // No previous reading and a low level must alert so a freshly enabled
        // monitor does not miss an already critical battery.
        assertEquals(true, BatteryDischargeAlarm.shouldAlert(12, false, 15, false, -1));
    }

    @Test public void downwardCrossingFires() {
        assertEquals(true, BatteryDischargeAlarm.shouldAlert(14, false, 15, false, 25));
        assertEquals(true, BatteryDischargeAlarm.shouldAlert(15, false, 15, false, 25));
    }

    @Test public void alreadyBelowThresholdDoesNotRefire() {
        assertEquals(false, BatteryDischargeAlarm.shouldAlert(14, false, 15, false, 14));
    }

    @Test public void resetFiresWhileCharging() {
        assertEquals(true, BatteryDischargeAlarm.shouldReset(10, true, 15));
    }

    @Test public void resetFiresOnInvalidLevel() {
        assertEquals(true, BatteryDischargeAlarm.shouldReset(-1, false, 15));
    }

    @Test public void resetFiresAfterResetMargin() {
        // threshold 15 + RESET_MARGIN 3 = 18 -> reset strictly above 18.
        assertEquals(true, BatteryDischargeAlarm.shouldReset(19, false, 15));
        assertEquals(true, BatteryDischargeAlarm.shouldReset(20, false, 15));
        // The boundary stays armed; only an above-margin level clears it.
        assertEquals(false, BatteryDischargeAlarm.shouldReset(18, false, 15));
        assertEquals(false, BatteryDischargeAlarm.shouldReset(16, false, 15));
    }

    @Test public void resetUsesNormalizedThresholdForClampedInputs() {
        // The user set 120 -> normalized to 50 -> reset margin is strictly
        // greater than 53, so 54 clears the alarm and 53 keeps it armed.
        assertEquals(true, BatteryDischargeAlarm.shouldReset(54, false, 120));
        assertEquals(false, BatteryDischargeAlarm.shouldReset(53, false, 120));
        assertEquals(false, BatteryDischargeAlarm.shouldReset(52, false, 120));
    }
}
