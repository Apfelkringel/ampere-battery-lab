package com.ampere.batterylab;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BatteryPageStateTest {
    @Test public void restoredPageIndexStaysWithinTheFiveNavigationDestinations() {
        assertEquals(0, BatteryPageState.normalize(-5));
        assertEquals(0, BatteryPageState.normalize(0));
        assertEquals(4, BatteryPageState.normalize(4));
        assertEquals(4, BatteryPageState.normalize(25));
    }
}
