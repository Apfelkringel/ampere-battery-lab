package com.ampere.batterylab;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BatteryAccessibilityLayoutTest {
    @Test public void chartValuesActionIsVisibleAndMeetsFortyEightDpTarget() {
        assertTrue(BatteryAccessibilityLayout.isVisible(BatteryAccessibilityLayout.HISTORY_VALUES, 4));
        assertTrue(BatteryAccessibilityLayout.pageControlsFor(4).length >= 5);
        int[] bounds = BatteryAccessibilityLayout.bounds(BatteryAccessibilityLayout.HISTORY_VALUES,
                0f, 360f, 0f, 0f);
        assertTrue(bounds[2] - bounds[0] >= 48);
        assertEquals(48, bounds[3] - bounds[1]);
        assertEquals("Exakte Werte im Bilanzdiagramm anzeigen",
                BatteryAccessibilityLayout.label(BatteryAccessibilityLayout.HISTORY_VALUES,
                        false, true, false, false));
        assertFalse(BatteryAccessibilityLayout.isVisible(BatteryAccessibilityLayout.HISTORY_VALUES, 3));
    }
}
