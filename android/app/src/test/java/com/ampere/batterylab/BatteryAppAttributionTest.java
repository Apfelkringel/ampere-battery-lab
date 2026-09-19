package com.ampere.batterylab;

import org.junit.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Math guards for the per-app attribution helpers. */
public class BatteryAppAttributionTest {

    @Test public void sourceLabelDistinguishesTelemetryAndFallback() {
        assertEquals("Quelle: zugeordnete Akku-Telemetrie (Schätzung)",
                BatteryAppAttribution.sourceLabel(true));
        assertEquals("Quelle: anteilig nach Vordergrundzeit (Schätzung)",
                BatteryAppAttribution.sourceLabel(false));
    }

    @Test public void observedWindowMahPrefersTelemetryWhenAvailable() {
        assertEquals(1500, BatteryAppAttribution.observedWindowMah(1500, true, 4000, 9999));
    }

    @Test public void observedWindowMahFallsBackToSinceFullWhenTelemetryEmpty() {
        assertEquals(2200, BatteryAppAttribution.observedWindowMah(0, true, 2200, 9999));
    }

    @Test public void observedWindowMahFallsBackToDischargeSessionWhenNothingElse() {
        assertEquals(800, BatteryAppAttribution.observedWindowMah(0, false, 0, 800));
    }

    @Test public void observedWindowMahClampsNegativeFallbackToZero() {
        assertEquals(0, BatteryAppAttribution.observedWindowMah(0, false, -5, -10));
    }

    @Test public void estimateMahFallsBackWhenDirectMahIsZero() {
        // 1000 mAh remaining, 50% foreground share -> 500 mAh.
        assertEquals(500, BatteryAppAttribution.estimateMah(0, 0, 1000, 600_000L, 1_200_000L));
    }

    @Test public void estimateMahScalesDownWhenDirectTotalExceedsObserved() {
        // 200 * 1500 / 2000 = 150.
        assertEquals(150, BatteryAppAttribution.estimateMah(200, 2000, 1500, 60_000L, 600_000L));
    }

    @Test public void estimateMahClampsToObservedWhenDirectExceedsObserved() {
        assertEquals(0, BatteryAppAttribution.estimateMah(500, 800, 0, 60_000L, 600_000L));
    }

    @Test public void estimateFallbackMahReturnsZeroWhenInputsInvalid() {
        assertEquals(0, BatteryAppAttribution.estimateFallbackMah(0, 0, 0L, 0L));
        assertEquals(0, BatteryAppAttribution.estimateFallbackMah(1000, 0, 0L, 600_000L));
        assertEquals(0, BatteryAppAttribution.estimateFallbackMah(1000, 0, 60_000L, 0L));
    }

    @Test public void apportionDistributesBudgetAcrossKeys() {
        Map<String, Long> weights = new HashMap<>();
        weights.put("a", 1L);
        weights.put("b", 1L);
        Map<String, Integer> result = BatteryAppAttribution.apportion(60, weights);
        assertEquals(30, result.get("a").intValue());
        assertEquals(30, result.get("b").intValue());
        assertEquals(60, total(result));
    }

    @Test public void apportionHonoursZeroBudget() {
        Map<String, Long> weights = new HashMap<>();
        weights.put("a", 10L);
        Map<String, Integer> result = BatteryAppAttribution.apportion(0, weights);
        assertTrue(result.isEmpty());
    }

    @Test public void apportionIgnoresZeroAndNegativeWeights() {
        Map<String, Long> weights = new HashMap<>();
        weights.put("valid", 5L);
        weights.put("zero", 0L);
        weights.put("negative", -3L);
        Map<String, Integer> result = BatteryAppAttribution.apportion(100, weights);
        assertEquals(1, result.size());
        assertTrue(result.containsKey("valid"));
        assertEquals(100, total(result));
    }

    @Test public void apportionDistributesRoundingRemainderFairly() {
        Map<String, Long> weights = new LinkedHashMap<>();
        weights.put("alpha", 1L);
        weights.put("bravo", 1L);
        weights.put("charlie", 1L);
        Map<String, Integer> result = BatteryAppAttribution.apportion(100, weights);
        assertEquals(100, total(result));
        assertEquals(34, result.get("alpha").intValue());
        assertEquals(33, result.get("bravo").intValue());
        assertEquals(33, result.get("charlie").intValue());
    }

    @Test public void apportNeverExceedsBudget() {
        Map<String, Long> weights = new HashMap<>();
        for (int i = 0; i < 12; i++) weights.put("k" + i, 1L);
        Map<String, Integer> result = BatteryAppAttribution.apportion(7, weights);
        assertEquals(7, total(result));
        for (Integer v : result.values()) {
            assertTrue("each share must fit in budget", v <= 7);
        }
    }

    @Test public void allocatedMahReturnsZeroForUnknownKey() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 100);
        assertEquals(0, BatteryAppAttribution.allocatedMah(map, "b"));
        assertEquals(0, BatteryAppAttribution.allocatedMah(null, "a"));
    }

    @Test public void scaleDirectMahPreservesObservedTotal() {
        Map<String, Integer> measured = new HashMap<>();
        measured.put("a", 600);
        measured.put("b", 300);
        measured.put("c", 100);
        Map<String, Integer> result = BatteryAppAttribution.scaleDirectMah(measured, 500);
        assertEquals(500, total(result));
        assertTrue(result.get("a") >= result.get("b"));
        assertTrue(result.get("b") >= result.get("c"));
    }

    @Test public void scaleDirectMahSkipsZeroAndNegativeEntries() {
        Map<String, Integer> measured = new HashMap<>();
        measured.put("a", 0);
        measured.put("b", -50);
        measured.put("c", 200);
        Map<String, Integer> result = BatteryAppAttribution.scaleDirectMah(measured, 100);
        assertTrue(result.containsKey("c"));
        assertFalse(result.containsKey("a"));
        assertFalse(result.containsKey("b"));
        assertEquals(100, total(result));
    }

    @Test public void sampleMahRequiresAllPositiveInputs() {
        assertEquals(0, BatteryAppAttribution.sampleMah(0, 60_000L, 60_000L));
        assertEquals(0, BatteryAppAttribution.sampleMah(100, 0L, 60_000L));
        assertEquals(0, BatteryAppAttribution.sampleMah(100, 60_000L, 0L));
        assertEquals(0, BatteryAppAttribution.sampleMah(-50, 60_000L, 60_000L));
    }

    @Test public void sampleMahComputesLinearEnergy() {
        assertEquals(1000, BatteryAppAttribution.sampleMah(1000, 3_600_000L, 3_600_000L));
        assertEquals(250, BatteryAppAttribution.sampleMah(500, 1_800_000L, 3_600_000L));
    }

    @Test public void sampleMahClampsToMaxInterval() {
        // 1000 mA for 4 hours but max interval is 1 hour -> 1000 mAh.
        assertEquals(1000, BatteryAppAttribution.sampleMah(1000, 4L * 3_600_000L, 3_600_000L));
    }

    @Test public void rateMahPerHourRequiresPositiveInputs() {
        assertEquals(0, BatteryAppAttribution.rateMahPerHour(0, 60_000L));
        assertEquals(0, BatteryAppAttribution.rateMahPerHour(100, 0L));
        assertEquals(0, BatteryAppAttribution.rateMahPerHour(-10, 60_000L));
    }

    @Test public void rateMahPerHourComputesExpectedRate() {
        assertEquals(500, BatteryAppAttribution.rateMahPerHour(500, 3_600_000L));
    }

    @Test public void appRateMahPerHourReturnsZeroWithoutDirectTelemetry() {
        assertEquals(0, BatteryAppAttribution.appRateMahPerHour(100, 60_000L, false));
        assertEquals(60, BatteryAppAttribution.appRateMahPerHour(60, 3_600_000L, true));
    }

    @Test public void summaryLabelDistinguishesMissingObservedAndAttributed() {
        assertEquals("Noch keine Akku-Messwerte",
                BatteryAppAttribution.summaryLabel(0, 0));
        assertEquals("Keine App-Werte zugeordnet",
                BatteryAppAttribution.summaryLabel(0, 1000));
        assertEquals("~1234 mAh zugeordnet",
                BatteryAppAttribution.summaryLabel(1234, 1234));
    }

    @Test public void summaryNoteMentionsSevenDayVsDayWindow() {
        assertEquals("Letzte 7 Tage · Schätzung; Rest ggf. nicht zuordenbar",
                BatteryAppAttribution.summaryNote(7));
        assertEquals("Letzte 24 Stunden · Schätzung; Rest ggf. nicht zuordenbar",
                BatteryAppAttribution.summaryNote(1));
    }

    @Test public void appRateLabelOnlyWhenDirectAndPositive() {
        assertEquals("~240 mAh/h", BatteryAppAttribution.appRateLabel(240, true));
        assertEquals("Rate nicht verfügbar",
                BatteryAppAttribution.appRateLabel(240, false));
    }

    @Test public void addForegroundTimeAccumulatesPerPackage() {
        Map<String, Long> totals = new HashMap<>();
        BatteryAppAttribution.addForegroundTime(totals, "com.example.app", 1_000L);
        BatteryAppAttribution.addForegroundTime(totals, "com.example.app", 2_500L);
        BatteryAppAttribution.addForegroundTime(totals, "com.example.beta", 500L);
        assertEquals(3_500L, totals.get("com.example.app").longValue());
        assertEquals(500L, totals.get("com.example.beta").longValue());
    }

    @Test public void addForegroundTimeIgnoresInvalidInputs() {
        Map<String, Long> totals = new HashMap<>();
        BatteryAppAttribution.addForegroundTime(totals, "", 1_000L);
        BatteryAppAttribution.addForegroundTime(totals, null, 1_000L);
        BatteryAppAttribution.addForegroundTime(totals, "a", 0L);
        BatteryAppAttribution.addForegroundTime(totals, "a", -100L);
        BatteryAppAttribution.addForegroundTime(null, "a", 1_000L);
        assertTrue(totals.isEmpty());
    }

    private static int total(Map<String, Integer> map) {
        int sum = 0;
        for (Integer v : map.values()) if (v != null) sum += v;
        return sum;
    }
}
