package com.ampere.batterylab;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BatteryLargeTextTabsTest {
    @Test public void exposesFiveNavigationDestinationsWithMatchingActions() {
        assertEquals(5, BatteryLargeTextTabs.count());
        assertEquals("Übersicht", BatteryLargeTextTabs.name(0));
        assertEquals("Übersicht anzeigen", BatteryLargeTextTabs.action(0));
        assertEquals("Akku", BatteryLargeTextTabs.name(3));
        assertEquals("Akkugesundheit anzeigen", BatteryLargeTextTabs.action(3));
        assertEquals("", BatteryLargeTextTabs.name(-1));
        assertEquals("", BatteryLargeTextTabs.action(5));
    }

    @Test public void marksOnlyTheSelectedTabInItsAccessibleDescription() {
        assertEquals("Ausgewählt: Laden", BatteryLargeTextTabs.contentDescription(1, 1));
        assertEquals("Verlauf", BatteryLargeTextTabs.contentDescription(4, 1));
        assertEquals("", BatteryLargeTextTabs.contentDescription(-1, -1));
    }

    @Test public void revealsSelectionOnlyOnFirstDisplayOrPageChange() {
        assertTrue(BatteryLargeTextTabs.shouldRevealSelection(-1, 0));
        assertFalse(BatteryLargeTextTabs.shouldRevealSelection(0, 0));
        assertTrue(BatteryLargeTextTabs.shouldRevealSelection(0, 4));
        assertFalse(BatteryLargeTextTabs.shouldRevealSelection(4, 4));
        assertFalse(BatteryLargeTextTabs.shouldRevealSelection(-1, 5));
    }
}
