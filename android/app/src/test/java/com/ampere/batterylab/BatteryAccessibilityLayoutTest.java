package com.ampere.batterylab;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BatteryAccessibilityLayoutTest {
    @Test public void compactOverviewMeasurementActionHasAVisibleAndAccessibleState() {
        int action = BatteryAccessibilityLayout.OVERVIEW_BENCHMARK;
        assertTrue(BatteryAccessibilityLayout.isVisible(action, 0, true));
        assertFalse(BatteryAccessibilityLayout.isVisible(action, 0, false));
        assertEquals(3, BatteryAccessibilityLayout.pageControlsFor(0, true).length);
        assertEquals(2, BatteryAccessibilityLayout.pageControlsFor(0, false).length);

        int[] bounds = BatteryAccessibilityLayout.bounds(action, 0f, 320f, 0f, 0f, true);
        assertEquals(52, bounds[0]);
        assertEquals(494, bounds[1]);
        assertEquals(268, bounds[2]);
        assertEquals(48, bounds[3] - bounds[1]);
        assertEquals("Messung starten",
                BatteryAccessibilityLayout.overviewBenchmarkLabel(false, false));
        assertEquals("Messung aktualisieren",
                BatteryAccessibilityLayout.overviewBenchmarkLabel(false, true));
        assertEquals("Kapazitätsmessung stoppen",
                BatteryAccessibilityLayout.overviewBenchmarkLabel(true, false));
    }

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

    @Test public void navigationTargetsMeetFortyEightDpAtCompactPhoneWidths() {
        for (float width : new float[]{240f, 250f, 275f, 276f, 320f}) {
            for (int tab = 0; tab < 5; tab++) {
                int[] bounds = BatteryAccessibilityLayout.navigationBounds(tab, width);
                assertTrue("tab " + tab + " must be at least 48 dp wide at " + width,
                        bounds[2] - bounds[0] >= 48);
                assertEquals("tab " + tab + " must be 48 dp tall at " + width,
                        48, bounds[3] - bounds[1]);
                assertEquals(tab, BatteryAccessibilityLayout.navigationTabAt(
                        (bounds[0] + bounds[2]) / 2f, width));
            }
        }
    }

    @Test public void heroIllustrationYieldsItsTextLaneOnNarrowPhones() {
        assertFalse(BatteryEditorialLayout.showSideIllustration(240f));
        assertFalse(BatteryEditorialLayout.showSideIllustration(319f));
        assertTrue(BatteryEditorialLayout.showSideIllustration(320f));
        assertEquals(204f, BatteryEditorialLayout.textRight(240f), 0f);
        assertEquals(190f, BatteryEditorialLayout.textRight(320f), 0f);
    }
}
