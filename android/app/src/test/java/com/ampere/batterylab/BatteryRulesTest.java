package com.ampere.batterylab;

import android.os.BatteryManager;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Pure rules shared by the visible dashboard and background monitor. */
public class BatteryRulesTest {
    @Test public void chargingRequiresAReportedPowerSource() {
        assertFalse(BatteryState.isCharging(BatteryManager.BATTERY_STATUS_CHARGING, 0));
        assertTrue(BatteryState.isCharging(BatteryManager.BATTERY_STATUS_CHARGING, BatteryManager.BATTERY_PLUGGED_USB));
        assertTrue(BatteryState.isCharging(BatteryManager.BATTERY_STATUS_FULL, BatteryManager.BATTERY_PLUGGED_AC));
        assertFalse(BatteryState.isCharging(BatteryManager.BATTERY_STATUS_DISCHARGING, BatteryManager.BATTERY_PLUGGED_AC));
    }

    @Test public void cycleCountRejectsMissingAndImplausibleValues() {
        assertTrue(BatteryCycleCount.isPlausible(0));
        assertTrue(BatteryCycleCount.isPlausible(100000));
        assertFalse(BatteryCycleCount.isPlausible(-1));
        assertFalse(BatteryCycleCount.isPlausible(100001));
    }

    @Test public void healthCannotExceedOneHundredPercent() {
        assertEquals(85, BatteryHealth.percent(8500, 10000));
        assertEquals(100, BatteryHealth.percent(12000, 10000));
        assertEquals(0, BatteryHealth.percent(0, 10000));
    }

    @Test public void sessionHistoryRejectsZeroAndWrongDirectionRowsInEveryFormat() {
        assertTrue(BatterySessionRules.isValid("Charge,+12%,18 Min.,11.09. 12:00"));
        assertTrue(BatterySessionRules.isValid("Discharge,-8%,42 Min.,11.09. 13:00,70,62,300,0.03"));
        assertFalse(BatterySessionRules.isValid("Charge,0%,1 Min.,11.09. 12:00"));
        assertFalse(BatterySessionRules.isValid("Discharge,0%,1 Min.,11.09. 12:00,70,70,0,0"));
        assertFalse(BatterySessionRules.isValid("Charge,-3%,1 Min.,11.09. 12:00"));
        assertFalse(BatterySessionRules.isValid("not-a-session"));
    }

    @Test public void platformHealthStaysQualitative() {
        assertEquals("Gut", BatteryPlatformHealth.label(BatteryManager.BATTERY_HEALTH_GOOD));
        assertEquals("Überhitzt", BatteryPlatformHealth.label(BatteryManager.BATTERY_HEALTH_OVERHEAT));
        assertEquals("Akzeptabel", BatteryPlatformHealth.label(8));
        assertEquals("Sehr gut", BatteryPlatformHealth.label(9));
        assertEquals("Nicht verfügbar", BatteryPlatformHealth.label(BatteryManager.BATTERY_HEALTH_UNKNOWN));
    }

    @Test public void capacityUnitsNormalizeWithoutInventingAValue() {
        assertEquals(6600L, BatteryCapacity.normalizeCapacity(6600000L));
        assertEquals(6600L, BatteryCapacity.normalizeCapacity(6600L));
        assertEquals(100L, BatteryCapacity.normalizeCapacity(100L));
    }
}
