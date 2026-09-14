package com.ampere.batterylab;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BatteryChargingTransitionTest {
    @Test public void matchingSnapshotClearsPendingState() {
        assertTrue(BatteryChargingTransition.agreesWithStable(true, true));
        assertFalse(BatteryChargingTransition.agreesWithStable(true, false));
    }

    @Test public void changedPendingValueStartsANewConfirmationWindow() {
        assertTrue(BatteryChargingTransition.startsNewPending(false, false, true));
        assertTrue(BatteryChargingTransition.startsNewPending(true, false, true));
        assertFalse(BatteryChargingTransition.startsNewPending(true, true, true));
    }

    @Test public void confirmationRequiresMonotonicElapsedTime() {
        assertFalse(BatteryChargingTransition.confirmationElapsed(999, 1000, 2500));
        assertFalse(BatteryChargingTransition.confirmationElapsed(3499, 1000, 2500));
        assertTrue(BatteryChargingTransition.confirmationElapsed(3500, 1000, 2500));
    }
}
