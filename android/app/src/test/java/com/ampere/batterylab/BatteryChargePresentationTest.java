package com.ampere.batterylab;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BatteryChargePresentationTest {
    @Test public void progressIsRelativeToTarget() {
        assertEquals(.5f, BatteryChargePresentation.progressToTarget(40, 80), .0001f);
        assertEquals(1f, BatteryChargePresentation.progressToTarget(80, 80), .0001f);
        assertEquals(1f, BatteryChargePresentation.progressToTarget(100, 80), .0001f);
    }

    @Test public void targetReachedDoesNotClaimChargingStopped() {
        assertTrue(BatteryChargePresentation.targetReached(80, 80));
        assertFalse(BatteryChargePresentation.targetReached(79, 80));
        assertEquals("Ladeziel erreicht · Gerät lädt weiter",
                BatteryChargePresentation.status(80, 80, true, "Erreicht"));
    }

    @Test public void unknownLevelStaysHonest() {
        assertEquals(0f, BatteryChargePresentation.progressToTarget(-1, 80), .0001f);
        assertEquals("Akkustand wird ermittelt",
                BatteryChargePresentation.status(-1, 80, true, "—"));
    }
}
