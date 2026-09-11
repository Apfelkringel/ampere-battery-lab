package com.ampere.batterylab;

import android.os.BatteryManager;
import org.junit.Test;
import java.util.HashMap;
import java.util.Map;

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

    @Test public void uiUsesRecentStabilizedMonitorState() {
        assertTrue(BatteryState.resolveUiCharging(false, true, true));
        assertFalse(BatteryState.resolveUiCharging(true, true, false));
        assertTrue(BatteryState.resolveUiCharging(true, false, false));
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
        assertEquals(0, BatteryHealth.percent(30001, 10000));
        assertFalse(BatteryHealth.isPlausibleCapacity(0));
        assertTrue(BatteryHealth.isPlausibleCapacity(6600));
        assertEquals(10000, BatteryHealth.averageRecentSamples("0,12000,8000"));
        assertEquals(0, BatteryHealth.averageRecentSamples("0,30001,-4,broken"));
        assertEquals(100, BatteryHealth.reportedPercentValue(100));
        assertEquals(100, BatteryHealth.reportedPercentValue(110));
        assertEquals(100, BatteryHealth.reportedPercentValue(1000));
        assertEquals(0, BatteryHealth.reportedPercentValue(-1));
    }

    @Test public void sessionHistoryRejectsZeroAndWrongDirectionRowsInEveryFormat() {
        assertTrue(BatterySessionRules.isValid("Charge,+12%,18 Min.,11.09. 12:00"));
        assertTrue(BatterySessionRules.isValid("Discharge,-8%,42 Min.,11.09. 13:00,70,62,300,0.03"));
        assertFalse(BatterySessionRules.isValid("Charge,0%,1 Min.,11.09. 12:00"));
        assertFalse(BatterySessionRules.isValid("Discharge,0%,1 Min.,11.09. 12:00,70,70,0,0"));
        assertFalse(BatterySessionRules.isValid("Charge,-3%,1 Min.,11.09. 12:00"));
        assertFalse(BatterySessionRules.isValid("not-a-session"));
    }

    @Test public void sessionChangeUsesMeasuredEnergyWhenLevelSnapshotIsNoisy() {
        assertEquals(10, BatterySessionRules.effectiveChange(0, 660, 6600, true));
        assertEquals(10, BatterySessionRules.effectiveChange(-2, 660, 6600, true));
        assertEquals(-10, BatterySessionRules.effectiveChange(0, 660, 6600, false));
        assertEquals(5, BatterySessionRules.effectiveChange(5, 0, 6600, true));
        assertEquals(0, BatterySessionRules.effectiveChange(0, 0, 6600, true));
        assertEquals(0, BatterySessionRules.effectiveChange(0, 7000, 6600, true));
    }

    @Test public void platformHealthStaysQualitative() {
        assertEquals("Gut", BatteryPlatformHealth.label(BatteryManager.BATTERY_HEALTH_GOOD));
        assertEquals("Überhitzt", BatteryPlatformHealth.label(BatteryManager.BATTERY_HEALTH_OVERHEAT));
        assertEquals("Akzeptabel", BatteryPlatformHealth.label(8));
        assertEquals("Sehr gut", BatteryPlatformHealth.label(9));
        assertEquals("Nicht verfügbar", BatteryPlatformHealth.label(BatteryManager.BATTERY_HEALTH_UNKNOWN));
    }

    @Test public void capacityLevelStaysSeparateFromHealthPercentage() {
        assertEquals("Kritisch", BatteryCapacityLevel.label(1));
        assertEquals("Normal", BatteryCapacityLevel.label(3));
        assertEquals("Voll", BatteryCapacityLevel.label(5));
        assertTrue(BatteryCapacityLevel.isAvailable(4));
        assertFalse(BatteryCapacityLevel.isAvailable(0));
        assertEquals("Nicht verfügbar", BatteryCapacityLevel.label(-1));
    }

    @Test public void capacityLevelNormalizesOnlyAndroidsQualitativeSignal() {
        assertEquals(4, BatteryCapacityLevel.normalize(4));
        assertEquals(-1, BatteryCapacityLevel.normalize(0));
        assertEquals(-1, BatteryCapacityLevel.normalize(6));
    }

    @Test public void chargingProfileIsSeparateFromCableDetection() {
        assertEquals(5, BatteryChargingState.normalize(5));
        assertEquals(0, BatteryChargingState.normalize(6));
        assertTrue(BatteryChargingState.isSpecial(4));
        assertFalse(BatteryChargingState.isSpecial(1));
        assertEquals("Akkuschonend", BatteryChargingState.label(4));
        assertEquals("Nicht verfügbar", BatteryChargingState.label(0));
    }

    @Test public void chargerCapabilityUsesOnlyPlausiblePowerPairs() {
        assertEquals(7500, BatteryChargerCapability.maxPowerMilliwatts(1_500_000, 5_000_000));
        assertEquals(0, BatteryChargerCapability.maxPowerMilliwatts(0, 5_000_000));
        assertEquals(0, BatteryChargerCapability.maxPowerMilliwatts(1_500_000, 0));
        assertEquals(0, BatteryChargerCapability.maxPowerMilliwatts(100_000_000, 30_000_000));
        assertEquals("Max. 7,5 W", BatteryChargerCapability.label(7500));
    }

    @Test public void batteryPowerUsesValidatedCurrentAndVoltage() {
        assertEquals(4500, BatteryPower.milliWatts(900, 5000));
        assertEquals(4500, BatteryPower.milliWatts(-900, 5000));
        assertEquals(0, BatteryPower.milliWatts(0, 5000));
        assertEquals(0, BatteryPower.milliWatts(900, 0));
        assertEquals(0, BatteryPower.milliWatts(Integer.MIN_VALUE, 5000));
        assertEquals("≈ 4,5 W", BatteryPower.label(4500));
    }

    @Test public void currentParserRejectsSentinelsAndUnrealisticSpikes() {
        assertEquals(900, BatteryCurrent.fromMicroamps(900_000));
        assertEquals(900, BatteryCurrent.fromMicroamps(-900_000));
        assertEquals(0, BatteryCurrent.fromMicroamps(0));
        assertEquals(0, BatteryCurrent.fromMicroamps(Integer.MIN_VALUE));
        assertEquals(0, BatteryCurrent.fromMicroamps(100_000_001));
        assertEquals(0, BatteryCurrent.fromMicroamps(-100_000_001));
    }

    @Test public void temperatureAlarmUsesThresholdAndHysteresis() {
        assertEquals(450, BatteryTemperatureAlarm.normalizeThreshold(0));
        assertTrue(BatteryTemperatureAlarm.shouldAlert(450, 450, true, false));
        assertFalse(BatteryTemperatureAlarm.shouldAlert(449, 450, true, false));
        assertFalse(BatteryTemperatureAlarm.shouldAlert(500, 450, true, true));
        assertFalse(BatteryTemperatureAlarm.shouldReset(430, 450));
        assertTrue(BatteryTemperatureAlarm.shouldReset(420, 450));
        assertFalse(BatteryTemperatureAlarm.shouldAlert(500, 450, false, false));
    }

    @Test public void capacityUnitsNormalizeWithoutInventingAValue() {
        assertEquals(6600L, BatteryCapacity.normalizeCapacity(6600000L));
        assertEquals(6600L, BatteryCapacity.normalizeCapacity(6600L));
        assertEquals(100L, BatteryCapacity.normalizeCapacity(100L));
        assertTrue(BatteryCapacity.isCacheFresh(1000L, 1001L));
        assertFalse(BatteryCapacity.isCacheFresh(1000L, 901001L));
        assertFalse(BatteryCapacity.isCacheFresh(0L, 1001L));
        assertEquals("6600,7000", BatteryHealth.serializeSamples(
                BatteryHealth.parseSamples("0,6600,110,-2,7000,broken")));
        assertEquals(6600, BatteryHealth.averageRecentSamples("0,6600,110,-2,broken"));
    }

    @Test public void localCycleFallbackIgnoresLevelDropsWhileCharging() {
        assertEquals(8f, BatteryCycleAccumulator.addDischargePercent(0f, 80, 72, false), 0.001f);
        assertEquals(0f, BatteryCycleAccumulator.addDischargePercent(0f, 80, 72, true), 0.001f);
        assertEquals(1, BatteryCycleAccumulator.completedCycles(108f));
    }

    @Test public void chargeCounterCycleEstimateCountsOnlyStableChargingIncreases() {
        assertEquals(0.1f, BatteryCycleEstimator.addChargedFraction(0f, 5000000L, 5500000L,
                true, 5000), 0.001f);
        assertEquals(0f, BatteryCycleEstimator.addChargedFraction(0f, 5000000L, 5500000L,
                false, 5000), 0.001f);
        assertEquals(0f, BatteryCycleEstimator.addChargedFraction(0f, 20000000L, 5000000L,
                true, 5000), 0.001f);
        assertEquals(1, BatteryCycleEstimator.completedCycles(1.2f));
    }

    @Test public void usageEventsCountOnlyForegroundIntervalsInsideTheWindow() {
        Map<String, Long> totals = new HashMap<>();
        Map<String, Long> active = new HashMap<>();
        UsageEventAccumulator.apply(totals, active, "app.one", 90L, 100L, 500L, true, false);
        UsageEventAccumulator.apply(totals, active, "app.one", 250L, 100L, 500L, false, true);
        UsageEventAccumulator.apply(totals, active, "app.one", 300L, 100L, 500L, true, false);
        UsageEventAccumulator.closeActive(totals, active, 500L);
        assertEquals(Long.valueOf(350L), totals.get("app.one"));
    }
}
