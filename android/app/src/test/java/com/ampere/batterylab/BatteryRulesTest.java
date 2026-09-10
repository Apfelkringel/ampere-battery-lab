package com.ampere.batterylab;

import android.os.BatteryManager;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
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
}
