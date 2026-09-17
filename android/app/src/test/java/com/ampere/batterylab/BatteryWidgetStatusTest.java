package com.ampere.batterylab;

import android.os.BatteryManager;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BatteryWidgetStatusTest {
    @Test public void fullStatusWinsOverCharging() {
        assertEquals("Voll geladen", BatteryWidgetStatus.label(
                BatteryManager.BATTERY_STATUS_FULL, true));
        assertEquals("Voll geladen", BatteryWidgetStatus.label(
                BatteryManager.BATTERY_STATUS_FULL, false));
    }

    @Test public void chargingWithoutFullStatusStaysCharging() {
        assertEquals("Laden", BatteryWidgetStatus.label(
                BatteryManager.BATTERY_STATUS_CHARGING, true));
        assertTrue(BatteryWidgetStatus.accent(
                BatteryManager.BATTERY_STATUS_CHARGING, true));
    }

    @Test public void fullStatusIsNotChargingAccent() {
        assertFalse(BatteryWidgetStatus.accent(
                BatteryManager.BATTERY_STATUS_FULL, true));
        assertFalse(BatteryWidgetStatus.accent(
                BatteryManager.BATTERY_STATUS_FULL, false));
    }

    @Test public void unknownAndIdleKeepOldLabels() {
        assertEquals("Unbekannt", BatteryWidgetStatus.label(
                BatteryManager.BATTERY_STATUS_UNKNOWN, false));
        assertEquals("Akku", BatteryWidgetStatus.label(
                BatteryManager.BATTERY_STATUS_DISCHARGING, false));
    }
}
