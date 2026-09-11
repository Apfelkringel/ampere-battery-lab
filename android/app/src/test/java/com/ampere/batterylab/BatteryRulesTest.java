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

    @Test public void powerBroadcastHintExpiresAfterSynchronizationWindow() {
        assertTrue(BatteryState.isPowerHintFresh(true, 0, 2500));
        assertTrue(BatteryState.isPowerHintFresh(true, 0, 5000));
        assertFalse(BatteryState.isPowerHintFresh(true, 0, 5001));
        assertFalse(BatteryState.isPowerHintFresh(false, 0, 2500));
        assertFalse(BatteryState.isPowerHintFresh(true, 3000, 2500));
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
        assertEquals(0, BatteryHealth.reportedPercentValue(101));
        assertEquals(0, BatteryHealth.reportedPercentValue(110));
        assertEquals(0, BatteryHealth.reportedPercentValue(1000));
        assertEquals(0, BatteryHealth.reportedPercentValue(-1));
        assertEquals(6600, BatteryHealth.averageRecentSamples("6600,6600,6600,12000"));
        assertEquals(5940, BatteryHealth.capacityFromReportedPercent(90, 6600));
        assertEquals(0, BatteryHealth.capacityFromReportedPercent(110, 6600));
        assertEquals(0, BatteryHealth.capacityFromReportedPercent(90, 0));
    }

    @Test public void displayedHealthRejectsImpossibleValues() {
        assertEquals(1, BatteryHealth.displayPercent(1));
        assertEquals(100, BatteryHealth.displayPercent(100));
        assertEquals(0, BatteryHealth.displayPercent(0));
        assertEquals(0, BatteryHealth.displayPercent(101));
        assertEquals(0, BatteryHealth.displayPercent(110));
    }

    @Test public void automaticHealthSamplesRequireAStableNearFullCharge() {
        assertTrue(BatteryHealthSampleRules.isEligible(30, 96, 4200, 20));
        assertFalse(BatteryHealthSampleRules.isEligible(30, 94, 4200, 20));
        assertFalse(BatteryHealthSampleRules.isEligible(30, 96, 4200, 26));
        assertFalse(BatteryHealthSampleRules.isEligible(30, 96, 4200, 0));
        assertEquals(6364, BatteryHealthSampleRules.estimateCapacityMah(30, 96, 4200, 20));
        assertEquals(0, BatteryHealthSampleRules.estimateCapacityMah(30, 94, 4200, 20));
    }

    @Test public void healthSampleUsesLatestUsableChargingCurrent() {
        assertEquals(20, BatteryHealthSampleRules.latestUsableCurrent(0, 20));
        assertEquals(300, BatteryHealthSampleRules.latestUsableCurrent(20, 300));
        assertEquals(300, BatteryHealthSampleRules.latestUsableCurrent(300, 0));
    }

    @Test public void persistedPercentagesRejectInvalidPhaseValues() {
        assertTrue(BatteryPercentage.isValidPhase(0f));
        assertTrue(BatteryPercentage.isValidPhase(100f));
        assertFalse(BatteryPercentage.isValidPhase(110f));
        assertFalse(BatteryPercentage.isValidPhase(-1f));
        assertFalse(BatteryPercentage.isValidPhase(Float.NaN));
        assertFalse(BatteryPercentage.isValidPhase(Float.POSITIVE_INFINITY));
        assertEquals(0f, BatteryPercentage.normalizePhase(110f), 0.001f);
        assertEquals(125f, BatteryPercentage.normalizeCumulative(125f), 0.001f);
        assertEquals(0f, BatteryPercentage.normalizeCumulative(Float.NaN), 0.001f);
    }

    @Test public void temperatureRejectsMissingAndImplausibleValues() {
        assertEquals(250, BatteryTemperature.normalizeTenths(250));
        assertEquals(1, BatteryTemperature.normalizeTenths(1));
        assertEquals(0, BatteryTemperature.normalizeTenths(0));
        assertEquals(0, BatteryTemperature.normalizeTenths(-1));
        assertEquals(0, BatteryTemperature.normalizeTenths(1001));
    }

    @Test public void voltageRejectsMissingAndImplausibleValues() {
        assertEquals(4200, BatteryVoltage.normalizeMilliVolts(4200));
        assertEquals(1000, BatteryVoltage.normalizeMilliVolts(1000));
        assertEquals(0, BatteryVoltage.normalizeMilliVolts(0));
        assertEquals(0, BatteryVoltage.normalizeMilliVolts(-1));
        assertEquals(0, BatteryVoltage.normalizeMilliVolts(10001));
    }

    @Test public void batteryLevelRejectsImpossibleRawPairs() {
        assertEquals(46, BatteryLevel.percent(46, 100));
        assertEquals(100, BatteryLevel.percent(100, 100));
        assertEquals(0, BatteryLevel.percent(0, 100));
        assertEquals(-1, BatteryLevel.percent(101, 100));
        assertEquals(-1, BatteryLevel.percent(-1, 100));
        assertEquals(-1, BatteryLevel.percent(1, 0));
        assertEquals(-1, BatteryLevel.percent(1001, 1001));
        assertEquals(88, BatteryLevel.normalizePercent(88));
        assertEquals(-1, BatteryLevel.normalizePercent(110));
        assertEquals(-1, BatteryLevel.normalizePercent(-1));
    }

    @Test public void storedLevelHistoryIsCanonicalizedBeforeReuse() {
        assertEquals("46,100,0", BatteryLevel.normalizeSerialized("46,110,100,broken,0"));
        assertEquals("", BatteryLevel.normalizeSerialized(null));
    }

    @Test public void researchExportSkipsMalformedTelemetryWithoutInventingValues() {
        String[] valid = {"1700000000000", "46", "1", "900", "25.0", "4.20", "6600", "0", "", "12", "2"};
        assertTrue(BatteryExportRules.isValidTelemetry(valid));
        valid[1] = "110";
        assertFalse(BatteryExportRules.isValidTelemetry(valid));
        valid[1] = "46";
        valid[3] = "not-a-current";
        assertFalse(BatteryExportRules.isValidTelemetry(valid));
        valid[3] = "900";
        valid[2] = "maybe";
        assertFalse(BatteryExportRules.isValidTelemetry(valid));
        assertEquals(Integer.valueOf(7), BatteryExportRules.nonNegativeInt(" 7 "));
        assertEquals(null, BatteryExportRules.nonNegativeInt("-1"));
        assertEquals(null, BatteryExportRules.nonNegativeInt("broken"));
    }

    @Test public void csvTelemetryFilterDropsInvalidRowsBeforeExport() {
        String valid = "1700000000000,46,1,900,25.0,4.20,6600,0,,12,2";
        String invalid = "1700000000001,110,1,900,25.0,4.20,6600,0,,12,2";
        java.util.ArrayList<String> rows = BatteryExportRules.validTelemetryRows(valid + "\n" + invalid);
        assertEquals(1, rows.size());
        assertEquals(valid, rows.get(0));
    }

    @Test public void restoredTelemetryIsImmediatelyNormalized() {
        String valid = "1700000000000,46,1,900,25.0,4.20,6600,0,,12,2";
        String invalid = "1700000000001,110,1,900,25.0,4.20,6600,0,,12,2";
        assertEquals(valid, BatteryExportRules.normalizeTelemetry(valid + "\n" + invalid + "\n"));
        assertEquals("", BatteryExportRules.normalizeTelemetry(invalid));
    }

    @Test public void telemetryRowsAreCanonicalizedChronologically() {
        String newer = "1700000000100,46,1,900,25.0,4.20,6600,0,,12,2";
        String older = "1700000000000,45,1,900,25.0,4.20,6590,0,,12,2";
        assertEquals(older + "\n" + newer, BatteryExportRules.normalizeTelemetry(newer + "\n" + older));
    }

    @Test public void telemetryExportRejectsContradictoryCurrentDirection() {
        String[] charging = {"1700000000000", "46", "1", "-900", "25.0", "4.20", "6600", "0", "", "12", "2"};
        String[] discharging = {"1700000000000", "46", "0", "900", "25.0", "4.20", "6600", "0", "", "12", "0"};
        assertFalse(BatteryExportRules.isValidTelemetry(charging));
        assertFalse(BatteryExportRules.isValidTelemetry(discharging));
    }

    @Test public void chargeCounterRejectsSentinelsAndUnknownUnits() {
        assertEquals(6600000L, BatteryChargeCounter.normalizeMicroampereHours(6600000L));
        assertEquals(6600, BatteryChargeCounter.toMilliampereHours(6600000L));
        assertEquals(0L, BatteryChargeCounter.normalizeMicroampereHours(10000L));
        assertEquals(0L, BatteryChargeCounter.normalizeMicroampereHours(-1L));
        assertEquals(0L, BatteryChargeCounter.normalizeMicroampereHours(30000001L));
    }

    @Test public void sessionHistoryRejectsZeroAndWrongDirectionRowsInEveryFormat() {
        assertTrue(BatterySessionRules.isValid("Charge,+12%,18 Min.,11.09. 12:00"));
        assertTrue(BatterySessionRules.isValid("Discharge,-8%,42 Min.,11.09. 13:00,70,62,300,0.03"));
        assertFalse(BatterySessionRules.isValid("Charge,0%,1 Min.,11.09. 12:00"));
        assertFalse(BatterySessionRules.isValid("Discharge,0%,1 Min.,11.09. 12:00,70,70,0,0"));
        assertFalse(BatterySessionRules.isValid("Charge,-3%,1 Min.,11.09. 12:00"));
        assertFalse(BatterySessionRules.isValid("Charge,+110%,1 Min.,11.09. 12:00"));
        assertFalse(BatterySessionRules.isValid("Discharge,-101%,1 Min.,11.09. 12:00"));
        assertFalse(BatterySessionRules.isValid("Charge,+12%,18 Min.,11.09. 12:00,50,62,800,NaN"));
        assertFalse(BatterySessionRules.isValid("Charge,+12%,18 Min.,11.09. 12:00,50,62,800,Infinity"));
        assertFalse(BatterySessionRules.isValid("Charge,+12%,18 Min.,11.09. 12:00,50,62,800,-0.10"));
        assertTrue(BatterySessionRules.isValid("Charge,+12%,18 Min.,11.09. 12:00,50,62,800,0.12"));
        assertFalse(BatterySessionRules.isValid("Charge,+12%,18 Min.,11.09. 12:00,110,62,800,0.12"));
        assertFalse(BatterySessionRules.isValid("Charge,+12%,18 Min.,11.09. 12:00,50,110,800,0.12"));
        assertFalse(BatterySessionRules.isValid("Discharge,-12%,18 Min.,11.09. 12:00,50,38,800,0.12,1100,20"));
        assertTrue(BatterySessionRules.isValid("Discharge,-12%,18 Min.,11.09. 12:00,50,38,800,0.12,100,20"));
        assertFalse(BatterySessionRules.isValid("Charge,+12%,NaN,11.09. 12:00"));
        assertFalse(BatterySessionRules.isValid("Charge,+12%,-1 Min.,11.09. 12:00"));
        assertFalse(BatterySessionRules.isValid("Charge,+12%,0 Min.,11.09. 12:00"));
        assertFalse(BatterySessionRules.isValid("Charge,+12%,18 Min.,11.09. 12:00,50,62,800,0.12,100,20,5,6,0,Netzteil,not-a-timestamp,1700000000000,0"));
        assertFalse(BatterySessionRules.isValid("Charge,+12%,18 Min.,11.09. 12:00,50,62,800,0.12,100,20,5,6,0,Netzteil,1700000000000,1699999999000,0"));
        assertTrue(BatterySessionRules.isValid("Charge,+12%,18 Min.,11.09. 12:00,50,62,800,0.12,100,20,5,6,0,Netzteil,1700000000000,1700000001000,0"));
        assertTrue(BatterySessionRules.isValid("Charge,+12%,1 Std. 0 Min.,11.09. 12:00"));
        assertTrue(BatterySessionRules.isValidEquivalentCycles("3.0"));
        assertFalse(BatterySessionRules.isValidEquivalentCycles("3.01"));
        assertFalse(BatterySessionRules.isValidEquivalentCycles("NaN"));
        assertFalse(BatterySessionRules.isValidEquivalentCycles("Infinity"));
        assertFalse(BatterySessionRules.isValid("not-a-session"));
    }

    @Test public void storedSessionsAreCanonicalizedBeforeBackgroundAppend() {
        String valid = "Charge,+12%,18 Min.,11.09. 12:00";
        String invalid = "Charge,0%,1 Min.,11.09. 12:00";
        assertEquals(valid, BatterySessionRules.normalizeSerialized(valid + "|" + invalid + "|"));
        assertEquals("", BatterySessionRules.normalizeSerialized(invalid));
    }

    @Test public void clockRollbackCannotCreateBackwardsSession() {
        assertTrue(BatteryTimelineRules.isRollback(2_000L, 1_999L));
        assertFalse(BatteryTimelineRules.isRollback(2_000L, 2_000L));
        assertTrue(BatteryTimelineRules.isForward(2_000L, 2_001L));
        assertFalse(BatteryTimelineRules.isForward(2_000L, 1_999L));
        assertEquals(0L, BatteryTimelineRules.sessionMinutes(2_000L, 1_999L));
        assertEquals(1L, BatteryTimelineRules.sessionMinutes(2_000L, 2_001L));
    }

    @Test public void aClockRollbackIsTheSamplingResetSignal() {
        assertTrue(BatteryTimelineRules.isRollback(1_700_000_000_000L, 1_699_999_999_000L));
        assertFalse(BatteryTimelineRules.isRollback(0L, 1_699_999_999_000L));
    }

    @Test public void sessionChangeUsesMeasuredEnergyWhenLevelSnapshotIsNoisy() {
        assertEquals(10, BatterySessionRules.effectiveChange(0, 660, 6600, true));
        assertEquals(10, BatterySessionRules.effectiveChange(-2, 660, 6600, true));
        assertEquals(-10, BatterySessionRules.effectiveChange(0, 660, 6600, false));
        assertEquals(5, BatterySessionRules.effectiveChange(5, 0, 6600, true));
        assertEquals(0, BatterySessionRules.effectiveChange(0, 0, 6600, true));
        assertEquals(0, BatterySessionRules.effectiveChange(0, 7000, 6600, true));
        assertEquals(10, BatterySessionRules.effectiveChange(110, 660, 6600, true));
        assertEquals(-10, BatterySessionRules.effectiveChange(-110, 660, 6600, false));
    }

    @Test public void shortSinglePercentStatusBlipIsNotASession() {
        assertFalse(BatterySessionRules.shouldRecord(1, 0, 1));
        assertFalse(BatterySessionRules.shouldRecord(-1, 0, 4));
        assertTrue(BatterySessionRules.shouldRecord(1, 0, 5));
        assertTrue(BatterySessionRules.shouldRecord(-1, 12, 1));
        assertTrue(BatterySessionRules.shouldRecord(2, 0, 1));
        assertFalse(BatterySessionRules.shouldRecord(0, 100, 20));
    }

    @Test public void restoredChargeLimitStaysInsideSupportedRange() {
        assertEquals(80, BatteryChargeLimit.normalize(0));
        assertEquals(80, BatteryChargeLimit.normalize(49));
        assertEquals(50, BatteryChargeLimit.normalize(50));
        assertEquals(80, BatteryChargeLimit.normalize(80));
        assertEquals(100, BatteryChargeLimit.normalize(100));
        assertEquals(80, BatteryChargeLimit.normalize(101));
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

    @Test public void sysfsHealthAcceptsOnlyExplicitPercentages() {
        assertEquals(1, BatteryCapacity.normalizeStateOfHealth(1));
        assertEquals(100, BatteryCapacity.normalizeStateOfHealth(100));
        assertEquals(0, BatteryCapacity.normalizeStateOfHealth(0));
        assertEquals(0, BatteryCapacity.normalizeStateOfHealth(101));
        assertEquals(0, BatteryCapacity.normalizeStateOfHealth(110));
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

    @Test public void chargeCounterCycleEstimateRejectsCorruptStoredFractions() {
        assertEquals(0.1f, BatteryCycleEstimator.addChargedFraction(Float.NaN,
                5000000L, 5500000L, true, 5000), 0.001f);
        assertEquals(0.1f, BatteryCycleEstimator.addChargedFraction(Float.POSITIVE_INFINITY,
                5000000L, 5500000L, true, 5000), 0.001f);
        assertEquals(0f, BatteryCycleEstimator.addChargedFraction(1.2f,
                5000000L, 5500000L, false, 5000), 0.001f);
        assertEquals(0, BatteryCycleEstimator.completedCycles(Float.NaN));
        assertEquals(0f, BatteryCycleEstimator.remainder(Float.POSITIVE_INFINITY), 0.001f);
        assertEquals(0.2f, BatteryCycleEstimator.remainder(1.2f), 0.001f);
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
