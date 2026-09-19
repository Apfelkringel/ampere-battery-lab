package com.ampere.batterylab;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Pure guards for the cycle-count value filter. The sysfs path treats zero as
 * "unavailable" while the BatteryManager API accepts zero, so the two
 * predicates must stay in sync with the runtime contract.
 */
public class BatteryCycleCountTest {

    @Test public void plausibleAcceptsZero() {
        // BatteryManager reports zero for fresh batteries.
        assertTrue(BatteryCycleCount.isPlausible(0));
    }

    @Test public void plausibleAcceptsPositiveValues() {
        assertTrue(BatteryCycleCount.isPlausible(1));
        assertTrue(BatteryCycleCount.isPlausible(500));
        assertTrue(BatteryCycleCount.isPlausible(100000));
    }

    @Test public void plausibleRejectsNegativeAndOutOfRange() {
        assertFalse(BatteryCycleCount.isPlausible(-1));
        assertFalse(BatteryCycleCount.isPlausible(100001));
        assertFalse(BatteryCycleCount.isPlausible(Integer.MAX_VALUE));
    }

    @Test public void sysfsRejectsZero() {
        // sysfs exposes 0 to mean "unavailable"; it must not be used as a value.
        assertFalse(BatteryCycleCount.isSysfsValue(0));
    }

    @Test public void sysfsAcceptsPositiveValues() {
        assertTrue(BatteryCycleCount.isSysfsValue(1));
        assertTrue(BatteryCycleCount.isSysfsValue(500));
        assertTrue(BatteryCycleCount.isSysfsValue(100000));
    }

    @Test public void sysfsRejectsNegativeAndOutOfRange() {
        assertFalse(BatteryCycleCount.isSysfsValue(-1));
        assertFalse(BatteryCycleCount.isSysfsValue(100001));
        assertFalse(BatteryCycleCount.isSysfsValue(Integer.MAX_VALUE));
    }
}
