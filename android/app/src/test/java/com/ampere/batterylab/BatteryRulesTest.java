package com.ampere.batterylab;

import android.os.BatteryManager;
import android.app.DownloadManager;
import org.junit.Test;
import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Pure rules shared by the visible dashboard and background monitor. */
public class BatteryRulesTest {
    @Test public void powerStatsDeriveSeparateChargingAndDischargingRanges() {
        ArrayList<String> rows = new ArrayList<>(Arrays.asList(
                "1000,50,1,1000,25.0,4.0,2000,1,,10,1",
                "2000,51,1,1250,25.0,4.0,2050,1,,10,1",
                "3000,49,0,-500,25.0,4.0,2000,0,,10,0"));
        BatteryPowerStats.Summary summary = BatteryPowerStats.analyzeRows(rows);
        assertEquals(4000, summary.charging.minimumMw);
        assertEquals(4500, summary.charging.averageMw);
        assertEquals(5000, summary.charging.maximumMw);
        assertEquals(1, summary.discharging.sampleCount);
        assertEquals(2000, summary.discharging.averageMw);
        assertEquals(0, BatteryPowerStats.milliWatts(new String[]{"0", "0", "0", "-1", "0", "0"}));
    }

    @Test public void remainingEnergyUsesAndroidNanoWattHourUnitAndRejectsSentinels() {
        assertEquals(2_500_000_000L, BatteryEnergy.normalizeNanoWattHours(2_500_000_000L));
        assertEquals(2.5d, BatteryEnergy.wattHours(2_500_000_000L), 0.0001d);
        assertEquals("2,50 Wh", BatteryEnergy.label(2_500_000_000L));
        assertEquals(0L, BatteryEnergy.normalizeNanoWattHours(0L));
        assertEquals(0L, BatteryEnergy.normalizeNanoWattHours(Long.MIN_VALUE));
        assertEquals(0L, BatteryEnergy.normalizeNanoWattHours(100_000_000_000_001L));
        assertEquals("—", BatteryEnergy.label(-1L));
    }

    @Test public void chargingRequiresAReportedPowerSource() {
        assertFalse(BatteryState.isCharging(BatteryManager.BATTERY_STATUS_CHARGING, 0));
        assertTrue(BatteryState.isCharging(BatteryManager.BATTERY_STATUS_CHARGING, BatteryManager.BATTERY_PLUGGED_USB));
        assertTrue(BatteryState.isCharging(BatteryManager.BATTERY_STATUS_FULL, BatteryManager.BATTERY_PLUGGED_AC));
        assertFalse(BatteryState.isCharging(BatteryManager.BATTERY_STATUS_DISCHARGING, BatteryManager.BATTERY_PLUGGED_AC));
        assertTrue(BatteryState.isCharging(BatteryManager.BATTERY_STATUS_CHARGING, 0, false));
        assertTrue(BatteryState.isCharging(BatteryManager.BATTERY_STATUS_FULL, 0, false));
        assertFalse(BatteryState.isCharging(BatteryManager.BATTERY_STATUS_CHARGING, 0, true));
    }

    @Test public void appDrainRateUsesForegroundTimeAndRejectsInvalidSamples() {
        assertEquals(240, BatteryAppAttribution.rateMahPerHour(120, 30L * 60L * 1000L));
        assertEquals(60, BatteryAppAttribution.rateMahPerHour(60, 60L * 60L * 1000L));
        assertEquals(0, BatteryAppAttribution.rateMahPerHour(0, 60L * 60L * 1000L));
        assertEquals(0, BatteryAppAttribution.rateMahPerHour(60, 0L));
        assertEquals("direct app telemetry supports a per-app rate", 240,
                BatteryAppAttribution.appRateMahPerHour(120, 30L * 60L * 1000L, true));
        assertEquals("foreground-time apportionment must not masquerade as a per-app rate", 0,
                BatteryAppAttribution.appRateMahPerHour(120, 30L * 60L * 1000L, false));
        assertEquals("~240 mAh/h", BatteryAppAttribution.appRateLabel(240, true));
        assertEquals("Rate n/v", BatteryAppAttribution.appRateLabel(240, false));
    }

    @Test public void appAttributionExplainsTelemetryAndForegroundTimeEstimateSources() {
        assertTrue(BatteryAppAttribution.sourceLabel(true)
                .contains("zugeordnete Akku-Telemetrie (Schätzung)"));
        assertTrue(BatteryAppAttribution.sourceLabel(false)
                .contains("anteilig nach Vordergrundzeit (Schätzung)"));
    }

    @Test public void historyStatsAggregateChargeDrainWearAndChargeConsumptionRatioByBucket() {
        long now = 8L * 60L * 60L * 1000L;
        ArrayList<String> rows = new ArrayList<>(Arrays.asList(
                (now - 2L * 60L * 60L * 1000L) + ",50,1,1000,25.0,4.0,100,1,,0,1",
                (now - 1L * 60L * 60L * 1000L) + ",51,0,-500,25.0,4.0,100,1,,0,0"));
        ArrayList<BatteryHistoryStats.Bucket> buckets = BatteryHistoryStats.aggregateRows(rows, now, 1, 1000);
        BatteryHistoryStats.Overall overall = BatteryHistoryStats.overall(buckets);
        assertEquals(1000, overall.chargedMah);
        assertEquals(500, overall.consumedMah);
        assertEquals(200, overall.chargeConsumptionRatioPercent);
        assertEquals(0.5f, overall.wearCycles, 0.001f);
    }

    @Test public void historyRatioKeepsZeroAsAValidMeasuredValue() {
        long now = 8L * 60L * 60L * 1000L;
        ArrayList<String> rows = new ArrayList<>(Arrays.asList(
                (now - 60L * 60L * 1000L) + ",50,0,-500,25.0,4.0,100,1,,0,0"));
        ArrayList<BatteryHistoryStats.Bucket> buckets =
                BatteryHistoryStats.aggregateRows(rows, now, 1, 1000);
        BatteryHistoryStats.Bucket today = buckets.get(buckets.size() - 1);
        assertEquals(500, today.consumedMah);
        assertEquals(0, today.chargeConsumptionRatioPercent);

        String summary = BatteryAccessibilitySummary.history("Täglich", "Heute", today,
                buckets, true);
        assertTrue(summary.contains("Lade-/Verbrauchsquote (geladen geteilt durch verbraucht): 0 Prozent"));
        assertTrue(summary.contains("keine gemessene Akku-Effizienz"));
    }

    @Test public void historyStatsSplitMeasurementEnergyAcrossBucketBoundaries() {
        TimeZone previous = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Berlin"));
        try {
            Calendar nowCalendar = Calendar.getInstance(TimeZone.getDefault(), Locale.GERMANY);
            nowCalendar.clear();
            nowCalendar.set(2026, Calendar.MARCH, 31, 12, 0);
            long now = nowCalendar.getTimeInMillis();
            ArrayList<BatteryHistoryStats.Bucket> empty =
                    BatteryHistoryStats.aggregateRows(new ArrayList<>(), now, 1, 1000);
            long boundary = empty.get(1).start;
            ArrayList<String> rows = new ArrayList<>(Arrays.asList(
                    (boundary - 30L * 60L * 1000L) + ",50,1,1000,25.0,4.0,100,1,,0,1",
                    (boundary + 30L * 60L * 1000L) + ",50,1,0,25.0,4.0,100,1,,0,1"));

            ArrayList<BatteryHistoryStats.Bucket> buckets =
                    BatteryHistoryStats.aggregateRows(rows, now, 1, 1000);

            assertEquals(500, buckets.get(0).chargedMah);
            assertEquals(500, buckets.get(1).chargedMah);
            assertEquals(1000, BatteryHistoryStats.overall(buckets).chargedMah);
        } finally {
            TimeZone.setDefault(previous);
        }
    }

    @Test public void historyBucketsFollowLocalDaysWeeksMonthsAndDaylightSaving() {
        TimeZone berlin = TimeZone.getTimeZone("Europe/Berlin");
        Calendar current = Calendar.getInstance(berlin, Locale.GERMANY);
        current.clear();
        current.set(2026, Calendar.MARCH, 31, 12, 0);
        long now = current.getTimeInMillis();

        ArrayList<BatteryHistoryStats.Bucket> daily =
                BatteryHistoryStats.calendarBuckets(now, 1, 7, berlin);
        assertEquals(7, daily.size());
        Calendar boundary = Calendar.getInstance(berlin, Locale.GERMANY);
        for (BatteryHistoryStats.Bucket bucket : daily) {
            boundary.setTimeInMillis(bucket.start);
            assertEquals(0, boundary.get(Calendar.HOUR_OF_DAY));
            assertEquals(0, boundary.get(Calendar.MINUTE));
        }
        assertEquals(23L * 60L * 60L * 1000L, daily.get(5).start - daily.get(4).start);

        current.set(2026, Calendar.MAY, 20, 12, 0);
        ArrayList<BatteryHistoryStats.Bucket> weekly =
                BatteryHistoryStats.calendarBuckets(current.getTimeInMillis(), 7, 5, berlin);
        assertEquals(5, weekly.size());
        for (BatteryHistoryStats.Bucket bucket : weekly) {
            boundary.setTimeInMillis(bucket.start);
            assertEquals(Calendar.MONDAY, boundary.get(Calendar.DAY_OF_WEEK));
            assertEquals(0, boundary.get(Calendar.HOUR_OF_DAY));
        }

        current.set(2026, Calendar.MARCH, 31, 12, 0);
        ArrayList<BatteryHistoryStats.Bucket> monthly =
                BatteryHistoryStats.calendarBuckets(current.getTimeInMillis(), 30, 6, berlin);
        assertEquals(6, monthly.size());
        for (BatteryHistoryStats.Bucket bucket : monthly) {
            boundary.setTimeInMillis(bucket.start);
            assertEquals(1, boundary.get(Calendar.DAY_OF_MONTH));
            assertEquals(0, boundary.get(Calendar.HOUR_OF_DAY));
        }
    }

    @Test public void historyRangeLabelsMatchCalendarBuckets() {
        assertEquals("Heute · seit Mitternacht", BatteryHistoryStats.rangeLabel(1));
        assertEquals("Diese Woche · Montag bis heute", BatteryHistoryStats.rangeLabel(7));
        assertEquals("Dieser Monat · Monatsanfang bis heute", BatteryHistoryStats.rangeLabel(30));
    }

    @Test public void pageAccessibilityControlsFollowTheActivePage() {
        assertTrue(BatteryAccessibilityLayout.isVisible(
                BatteryAccessibilityLayout.OVERVIEW_7D, 0));
        assertTrue(BatteryAccessibilityLayout.isVisible(
                BatteryAccessibilityLayout.OVERVIEW_30D, 0));
        assertFalse(BatteryAccessibilityLayout.isVisible(
                BatteryAccessibilityLayout.OVERVIEW_7D, 1));
        assertTrue(BatteryAccessibilityLayout.isVisible(
                BatteryAccessibilityLayout.CHARGE_ALARM, 1));
        assertTrue(BatteryAccessibilityLayout.isVisible(
                BatteryAccessibilityLayout.CHARGE_LIMIT, 1));
        assertTrue(BatteryAccessibilityLayout.isVisible(
                BatteryAccessibilityLayout.CHARGE_OVERLAY, 1));
        assertTrue(BatteryAccessibilityLayout.isVisible(
                BatteryAccessibilityLayout.HEALTH_BENCHMARK, 3));
        assertTrue(BatteryAccessibilityLayout.isVisible(
                BatteryAccessibilityLayout.HEALTH_CAPACITY, 3));
        assertTrue(BatteryAccessibilityLayout.isVisible(
                BatteryAccessibilityLayout.DISCHARGE_USAGE, 2));
        assertTrue(BatteryAccessibilityLayout.isVisible(
                BatteryAccessibilityLayout.HISTORY_EXPORT, 4));
    }

    @Test public void screenReaderOverviewReportsLiveValuesAndSkipsDischargeEtaWhileCharging() {
        String discharging = BatteryAccessibilitySummary.overview(false, "8 Std.",
                "Android-Systemschätzung", "−400 mA", "32,0 Grad Celsius", "3,90 Volt");
        assertTrue(discharging.contains("Akkustrom: −400 mA"));
        assertTrue(discharging.contains("Temperatur: 32,0 Grad Celsius"));
        assertTrue(discharging.contains("Spannung: 3,90 Volt"));
        assertTrue(discharging.contains("Restlaufzeit bei normaler Nutzung: 8 Std."));
        assertTrue(discharging.contains("Datenquelle: Android-Systemschätzung"));

        String charging = BatteryAccessibilitySummary.overview(true, "8 Std.",
                "Android-Systemschätzung", "+900 mA", "—", "—");
        assertFalse(charging.contains("Restlaufzeit"));
        assertTrue(charging.contains("Temperatur: nicht verfügbar"));
        assertTrue(charging.contains("Spannung: nicht verfügbar"));
    }

    @Test public void screenReaderDischargeSummaryNamesAllModesAndTheirSources() {
        String summary = BatteryAccessibilitySummary.discharge("5 Std.", "Aktuelle Sitzung",
                "12 Std.", "Standby-Modell", "7 Std.", "Lokale 7-Tage-Nutzung");
        assertTrue(summary.contains("dauerhaft eingeschaltetem Bildschirm: 5 Std."));
        assertTrue(summary.contains("ausgeschaltetem Bildschirm: 12 Std."));
        assertTrue(summary.contains("normaler Nutzung: 7 Std."));
        assertTrue(summary.contains("Datenquelle: Standby-Modell"));
    }

    @Test public void screenReaderChargingSummaryIncludesRateTargetAndSession() {
        String summary = BatteryAccessibilitySummary.charging(true, "Lädt schnell", "+1.200 mA",
                "80 Prozent", "42 Min.", "Lokale 7-Tage-Schätzung", "1.000 mAh/h",
                "700 mAh/h", "350 mAh", "25 Min.", "31,0 Grad Celsius", "4,1 Volt", "USB");
        assertTrue(summary.contains("Ladestatus: Lädt schnell"));
        assertTrue(summary.contains("Akkustrom: +1.200 mA"));
        assertTrue(summary.contains("Ladeziel: 80 Prozent"));
        assertTrue(summary.contains("Zeit bis Ladeziel: 42 Min."));
        assertTrue(summary.contains("Datenquelle: Lokale 7-Tage-Schätzung"));
        assertTrue(summary.contains("Laderate bei Bildschirm aus: 700 mAh/h"));
        assertTrue(summary.contains("Geladene Energie: 350 mAh"));
        assertTrue(summary.contains("Ladequelle: USB"));
    }

    @Test public void screenReaderChargingSummaryDoesNotClaimAnEtaSourceWhenUnavailable() {
        String summary = BatteryAccessibilitySummary.charging(false, "Nicht verbunden", "—",
                "80 Prozent", "—", "Momentanschätzung", "—", "—", "—", "—",
                "—", "—", "Nicht verbunden");
        assertTrue(summary.contains("Zeit bis Ladeziel: nicht verfügbar"));
        assertFalse(summary.contains("Datenquelle:"));
        assertFalse(summary.contains("Akkustrom:"));
    }

    @Test public void pageAccessibilityBoundsStayInsideTheCenteredBody() {
        for (int page = 0; page <= 4; page++) {
            for (int id : BatteryAccessibilityLayout.pageControlsFor(page)) {
                int[] bounds = BatteryAccessibilityLayout.bounds(
                        id, 18f, 360f, 430f, 1280f);
                assertTrue(bounds[0] >= 18);
                assertTrue(bounds[1] >= 0);
                assertTrue(bounds[2] <= 378);
                assertTrue(bounds[3] > bounds[1]);
            }
        }
    }

    @Test public void historyExportBoundsMatchTheFullVisibleButton() {
        int[] bounds = BatteryAccessibilityLayout.bounds(
                BatteryAccessibilityLayout.HISTORY_EXPORT, 0f, 320f, 0f, 500f);
        assertEquals(36, bounds[0]);
        assertEquals(498, bounds[1]);
        assertEquals(284, bounds[2]);
        assertEquals(546, bounds[3]);
    }

    @Test public void healthBenchmarkBoundsMatchTheRenderedPortraitAndWideButtons() {
        int[] portrait = BatteryAccessibilityLayout.bounds(
                BatteryAccessibilityLayout.HEALTH_BENCHMARK, 0f, 320f, 0f, 0f);
        assertEquals(104, portrait[0]);
        assertEquals(704, portrait[1]);
        assertEquals(284, portrait[2]);
        assertEquals(752, portrait[3]);

        int[] wide = BatteryAccessibilityLayout.bounds(
                BatteryAccessibilityLayout.HEALTH_BENCHMARK, 0f, 600f, 0f, 0f);
        assertEquals(384, wide[0]);
        assertEquals(734, wide[1]);
        assertEquals(564, wide[2]);
        assertEquals(782, wide[3]);
    }

    @Test public void chargingControlBoundsMatchTheirVisibleRows() {
        int[] editorialAlarm = BatteryAccessibilityLayout.bounds(
                BatteryAccessibilityLayout.CHARGE_ALARM, 0f, 320f, 0f, 0f, true);
        assertEquals(22, editorialAlarm[0]);
        assertEquals(459, editorialAlarm[1]);
        assertEquals(298, editorialAlarm[2]);
        assertEquals(507, editorialAlarm[3]);

        int[] wideOverlay = BatteryAccessibilityLayout.bounds(
                BatteryAccessibilityLayout.CHARGE_OVERLAY, 0f, 600f, 0f, 0f, false);
        assertEquals(36, wideOverlay[0]);
        assertEquals(507, wideOverlay[1]);
        assertEquals(564, wideOverlay[2]);
        assertEquals(555, wideOverlay[3]);
    }

    @Test public void primaryCanvasControlsProvideAtLeast48DpTouchTargets() {
        int[] controls = {
                BatteryAccessibilityLayout.OVERVIEW_7D,
                BatteryAccessibilityLayout.OVERVIEW_30D,
                BatteryAccessibilityLayout.CHARGE_ALARM,
                BatteryAccessibilityLayout.CHARGE_OVERLAY,
                BatteryAccessibilityLayout.CHARGE_LIMIT,
                BatteryAccessibilityLayout.HEALTH_BENCHMARK,
                BatteryAccessibilityLayout.HISTORY_DAY,
                BatteryAccessibilityLayout.HISTORY_WEEK,
                BatteryAccessibilityLayout.HISTORY_MONTH
        };
        for (int control : controls) {
            int[] bounds = BatteryAccessibilityLayout.bounds(control, 0f, 411f,
                    430f, 1280f, true);
            assertTrue("control " + control + " needs a 48dp-high target",
                    bounds[3] - bounds[1] >= 48);
        }
    }

    @Test public void rangeTapSelectsItsOwnRangeInsteadOfTogglingTheOtherOne() {
        assertEquals(7, BatteryAccessibilityLayout.historyDaysForControl(
                BatteryAccessibilityLayout.OVERVIEW_7D, 30));
        assertEquals(30, BatteryAccessibilityLayout.historyDaysForControl(
                BatteryAccessibilityLayout.OVERVIEW_30D, 7));
    }

    @Test public void pageAccessibilityLabelsExposeCurrentToggleState() {
        assertEquals("7 Tage", BatteryAccessibilityLayout.label(
                BatteryAccessibilityLayout.OVERVIEW_7D, false, true, false, false));
        assertEquals("30 Tage", BatteryAccessibilityLayout.label(
                BatteryAccessibilityLayout.OVERVIEW_30D, true, true, false, false));
        assertEquals("Ladealarm: Aus", BatteryAccessibilityLayout.label(
                BatteryAccessibilityLayout.CHARGE_ALARM, false, false, false, false));
        assertEquals("Ladeziel: 80 Prozent", BatteryAccessibilityLayout.label(
                BatteryAccessibilityLayout.CHARGE_LIMIT, false, true, false, false, 80));
        assertEquals("Live-Anzeige: Aktiv", BatteryAccessibilityLayout.label(
                BatteryAccessibilityLayout.CHARGE_OVERLAY, false, true, true, false));
        assertEquals("Kapazitätsmessung stoppen", BatteryAccessibilityLayout.label(
                BatteryAccessibilityLayout.HEALTH_BENCHMARK, false, true, false, true));
    }

    @Test public void historyPeriodAccessibilitySelectionTracksTheActiveRange() {
        assertTrue(BatteryAccessibilityLayout.isHistoryPeriodSelected(
                BatteryAccessibilityLayout.HISTORY_DAY, 1));
        assertTrue(BatteryAccessibilityLayout.isHistoryPeriodSelected(
                BatteryAccessibilityLayout.HISTORY_WEEK, 7));
        assertTrue(BatteryAccessibilityLayout.isHistoryPeriodSelected(
                BatteryAccessibilityLayout.HISTORY_MONTH, 30));
        assertFalse(BatteryAccessibilityLayout.isHistoryPeriodSelected(
                BatteryAccessibilityLayout.HISTORY_DAY, 30));
    }

    @Test public void historyPeriodTouchTargetsShareGuttersAndMatchAccessibilityBounds() {
        float width = 520f;
        float inset = 140f;
        float column = (width - 84f) / 3f;
        float firstBoundary = 42f + column;
        float secondBoundary = 54f + 2f * column;
        assertEquals(BatteryAccessibilityLayout.HISTORY_DAY,
                BatteryAccessibilityLayout.historyPeriodControlAt(inset + firstBoundary - .1f, inset, width));
        assertEquals(BatteryAccessibilityLayout.HISTORY_WEEK,
                BatteryAccessibilityLayout.historyPeriodControlAt(inset + firstBoundary, inset, width));
        assertEquals(BatteryAccessibilityLayout.HISTORY_WEEK,
                BatteryAccessibilityLayout.historyPeriodControlAt(inset + secondBoundary - .1f, inset, width));
        assertEquals(BatteryAccessibilityLayout.HISTORY_MONTH,
                BatteryAccessibilityLayout.historyPeriodControlAt(inset + secondBoundary, inset, width));
        assertEquals(0, BatteryAccessibilityLayout.historyPeriodControlAt(inset + 35.9f, inset, width));
        assertEquals(0, BatteryAccessibilityLayout.historyPeriodControlAt(inset + width - 36f, inset, width));

        int[] day = BatteryAccessibilityLayout.bounds(
                BatteryAccessibilityLayout.HISTORY_DAY, inset, width, 0f, 0f);
        int[] week = BatteryAccessibilityLayout.bounds(
                BatteryAccessibilityLayout.HISTORY_WEEK, inset, width, 0f, 0f);
        int[] month = BatteryAccessibilityLayout.bounds(
                BatteryAccessibilityLayout.HISTORY_MONTH, inset, width, 0f, 0f);
        assertEquals(day[2], week[0]);
        assertEquals(week[2], month[0]);
        assertEquals(Math.round(inset + 36f), day[0]);
        assertEquals(Math.round(inset + width - 36f), month[2]);
    }

    @Test public void screenReaderHistorySummaryExposesSelectedTotalsAndChartBars() {
        ArrayList<BatteryHistoryStats.Bucket> buckets = new ArrayList<>();
        BatteryHistoryStats.Bucket prior = new BatteryHistoryStats.Bucket(1L, "Montag");
        prior.chargedMah = 300;
        prior.consumedMah = 200;
        prior.wearCycles = .1f;
        buckets.add(prior);
        BatteryHistoryStats.Bucket selected = new BatteryHistoryStats.Bucket(2L, "Dienstag");
        selected.chargedMah = 400;
        selected.consumedMah = 250;
        selected.wearCycles = .125f;
        selected.chargeConsumptionRatioPercent = 160;
        buckets.add(selected);

        String summary = BatteryAccessibilitySummary.history(
                "Täglich", BatteryHistoryStats.rangeLabel(1), selected, buckets, true);
        assertTrue(summary.contains("Verlauf Täglich"));
        assertTrue(summary.contains("Aufgeladen: 400 mAh"));
        assertTrue(summary.contains("Akkuverbrauch: 250 mAh"));
        assertTrue(summary.contains("Akkuverschleiß: 0,13 EFC"));
        assertTrue(summary.contains("Lade-/Verbrauchsquote (geladen geteilt durch verbraucht): 160 Prozent"));
        assertTrue(summary.contains("Montag: aufgeladen 300 mAh, verbraucht 200 mAh"));
        assertTrue(summary.contains("Dienstag: aufgeladen 400 mAh, verbraucht 250 mAh, Verschleiß 0,13 EFC, Lade-/Verbrauchsquote 160 Prozent"));
        assertTrue(summary.contains("jede Kennzahl ist separat skaliert"));
        assertTrue(summary.contains("kein direkt gemessener chemischer Gesundheitsverlust"));
    }

    @Test public void screenReaderHistorySummaryExplainsEmptyAndUnavailableWearData() {
        ArrayList<BatteryHistoryStats.Bucket> buckets = new ArrayList<>();
        BatteryHistoryStats.Bucket empty = new BatteryHistoryStats.Bucket(1L, "Heute");
        buckets.add(empty);
        String summary = BatteryAccessibilitySummary.history(
                "Monatlich", "letzte 30 Tage", empty, buckets, false);
        assertTrue(summary.contains("Aufgeladen: keine Messdaten"));
        assertTrue(summary.contains("Akkuverschleiß: nicht berechenbar, Kapazität fehlt"));
        assertTrue(summary.contains("keine Messdaten im Diagramm"));
    }

    @Test public void screenReaderHealthSummaryDistinguishesEstimateSourceAndMissingValues() {
        String measured = BatteryAccessibilitySummary.health("87 Prozent", "4.350 mAh",
                "5.000 mAh", "Lokale Ladesitzungen", "Wird nach dem Ladevorgang einbezogen",
                "312", "Erreicht", "32,0 Grad Celsius", "3,9 Volt");
        assertTrue(measured.contains("Gesundheit: 87 Prozent"));
        assertTrue(measured.contains("Geschätzte Vollkapazität: 4.350 mAh"));
        assertTrue(measured.contains("Messquelle: Lokale Ladesitzungen"));
        assertTrue(measured.contains("Ladezyklen: 312"));
        assertTrue(measured.contains("Temperatur: 32,0 Grad Celsius"));
        assertTrue(measured.contains("keine direkte chemische Messung"));

        String unavailable = BatteryAccessibilitySummary.health("—", "—", "—",
                "Keine Messung", "Längere Ladevorgänge verbessern die Genauigkeit",
                "—", "—", "—", "—");
        assertTrue(unavailable.contains("Gesundheit: nicht verfügbar"));
        assertTrue(unavailable.contains("Geschätzte Vollkapazität: nicht verfügbar"));
        assertTrue(unavailable.contains("Messquelle: Keine Messung"));
    }

    @Test public void accessibilityChargeLimitUsesTheSameSafeRangeAsTheUi() {
        assertEquals(50, BatteryAccessibilityLayout.normalizeChargeLimit(1));
        assertEquals(80, BatteryAccessibilityLayout.normalizeChargeLimit(80));
        assertEquals(100, BatteryAccessibilityLayout.normalizeChargeLimit(100));
        assertEquals(100, BatteryAccessibilityLayout.normalizeChargeLimit(150));
    }

    @Test public void uiPrefersCurrentKnownBatteryStateOverStaleMonitorState() {
        assertTrue(BatteryState.resolveUiCharging(
                BatteryManager.BATTERY_STATUS_UNKNOWN, false, true, true));
        assertFalse(BatteryState.resolveUiCharging(
                BatteryManager.BATTERY_STATUS_DISCHARGING, false, true, true));
        assertTrue(BatteryState.resolveUiCharging(
                BatteryManager.BATTERY_STATUS_CHARGING, true, true, false));
        assertFalse(BatteryState.resolveUiCharging(
                BatteryManager.BATTERY_STATUS_DISCHARGING, false, true, true));
        assertTrue(BatteryState.resolveUiCharging(
                BatteryManager.BATTERY_STATUS_CHARGING, true, false, false));
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
        assertFalse(BatteryCycleCount.isSysfsValue(0));
        assertTrue(BatteryCycleCount.isSysfsValue(1));
        assertTrue(BatteryCycleCount.isSysfsValue(100000));
        assertFalse(BatteryCycleCount.isSysfsValue(100001));
    }

    @Test public void batterySupplyRankingRejectsInputNodesAndPrefersCellSources() {
        assertEquals(0, BatterySupplyRules.rank("battery", "Battery"));
        assertEquals(2, BatterySupplyRules.rank("bms", "BMS"));
        assertEquals(4, BatterySupplyRules.rank("main-fuelgauge", ""));
        assertEquals(BatterySupplyRules.UNSUPPORTED, BatterySupplyRules.rank("usb", "USB"));
        assertEquals(BatterySupplyRules.UNSUPPORTED, BatterySupplyRules.rank("usb_battery", "USB"));
    }

    @Test public void chargerSourceTreatsAndroidPlugValueAsBitField() {
        assertEquals("Netzteil", BatteryPlugType.label(BatteryManager.BATTERY_PLUGGED_AC));
        assertEquals("USB", BatteryPlugType.label(BatteryManager.BATTERY_PLUGGED_USB));
        assertEquals("Kabellos", BatteryPlugType.label(BatteryManager.BATTERY_PLUGGED_WIRELESS));
        assertEquals("Netzteil", BatteryPlugType.label(
                BatteryManager.BATTERY_PLUGGED_AC | BatteryManager.BATTERY_PLUGGED_USB));
    }

    @Test public void chargeBaselineUsesResolvedChargingWhenPlugFieldIsMissing() {
        assertTrue(BatteryState.isPowerConnected(true, 0));
        assertTrue(BatteryState.isPowerConnected(false, BatteryManager.BATTERY_PLUGGED_USB));
        assertFalse(BatteryState.isPowerConnected(false, 0));
    }

    @Test public void chargeRateHistoryKeepsChargerSourcesSeparate() {
        assertEquals(BatteryChargeSource.AC,
                BatteryChargeSource.fromPlugged(BatteryManager.BATTERY_PLUGGED_AC));
        assertEquals(BatteryChargeSource.USB,
                BatteryChargeSource.fromPlugged(BatteryManager.BATTERY_PLUGGED_USB));
        assertEquals(BatteryChargeSource.WIRELESS,
                BatteryChargeSource.fromPlugged(BatteryManager.BATTERY_PLUGGED_WIRELESS));
        assertEquals(BatteryChargeSource.WIRELESS,
                BatteryChargeSource.fromPlugged(BatteryManager.BATTERY_PLUGGED_WIRELESS
                        | BatteryManager.BATTERY_PLUGGED_AC));
        assertEquals(BatteryChargeSource.UNKNOWN, BatteryChargeSource.fromPlugged(0));
        assertTrue(BatteryChargeSource.isKnown(BatteryChargeSource.DOCK));
        assertFalse(BatteryChargeSource.isKnown(BatteryChargeSource.UNKNOWN));
    }

    @Test public void dailyCycleHistoryKeepsTheStrongestMonotoneReadingPerDay() {
        String history = BatteryCycleHistory.record("", "2026-09-11", 12f, BatteryCycleHistory.ESTIMATED);
        history = BatteryCycleHistory.record(history, "2026-09-11", 11f, BatteryCycleHistory.ESTIMATED);
        assertEquals("2026-09-11|12.000|estimated", history);

        history = BatteryCycleHistory.record(history, "2026-09-11", 8f, BatteryCycleHistory.REPORTED);
        assertEquals("2026-09-11|8.000|reported", history);
        history = BatteryCycleHistory.record(history, "2026-09-11", 7f, BatteryCycleHistory.REPORTED);
        assertEquals("2026-09-11|8.000|reported", history);
    }

    @Test public void dailyCycleHistoryRejectsCorruptRowsAndCapsRetention() {
        String history = "bad;2026-09-11|NaN|reported;2026-09-11|4|reported";
        assertEquals("2026-09-11|4.000|reported", BatteryCycleHistory.serialize(
                BatteryCycleHistory.parse(history)));
        for (int i = 0; i < BatteryCycleHistory.MAX_POINTS + 5; i++) {
            history = BatteryCycleHistory.record(history, String.format("%04d-%02d-01", 2020 + i / 12, (i % 12) + 1),
                    i, BatteryCycleHistory.REPORTED);
        }
        assertEquals(BatteryCycleHistory.MAX_POINTS, BatteryCycleHistory.parse(history).size());
    }

    @Test public void chargeAnchorUsesFullChargeOncePerPlugSession() {
        BatteryChargeAnchor.State empty = new BatteryChargeAnchor.State(0L, -1, "", false);
        BatteryChargeAnchor.State connected = BatteryChargeAnchor.onPowerConnected(empty);
        BatteryChargeAnchor.State full = BatteryChargeAnchor.onBatteryChanged(
                connected, 100, true, true, 1000L);
        BatteryChargeAnchor.State repeated = BatteryChargeAnchor.onBatteryChanged(
                full, 100, true, true, 2000L);
        assertEquals(1000L, repeated.anchorAt);
        assertEquals(BatteryChargeAnchor.FULL, repeated.anchorType);
        assertTrue(repeated.fullReachedThisPlug);

        BatteryChargeAnchor.State unplugged = BatteryChargeAnchor.onPowerDisconnected(
                repeated, 90, 3000L);
        assertEquals(1000L, unplugged.anchorAt);
        assertFalse(unplugged.fullReachedThisPlug);

        BatteryChargeAnchor.State nextPlug = BatteryChargeAnchor.onPowerConnected(unplugged);
        BatteryChargeAnchor.State nextFull = BatteryChargeAnchor.onBatteryChanged(
                nextPlug, 99, true, true, 4000L);
        assertEquals(4000L, nextFull.anchorAt);
        assertEquals(BatteryChargeAnchor.FULL, nextFull.anchorType);
    }

    @Test public void chargeAnchorUsesUnplugPointWhenChargeStopsBeforeFull() {
        BatteryChargeAnchor.State connected = BatteryChargeAnchor.onPowerConnected(
                new BatteryChargeAnchor.State(0L, -1, "", false));
        BatteryChargeAnchor.State unplugged = BatteryChargeAnchor.onPowerDisconnected(
                connected, 76, 5000L);
        assertEquals(5000L, unplugged.anchorAt);
        assertEquals(76, unplugged.anchorLevel);
        assertEquals(BatteryChargeAnchor.UNPLUGGED, unplugged.anchorType);
    }

    @Test public void fullChargeEstimateUsesRemainingCounterAndRejectsUncertainLevels() {
        assertEquals(5000, BatteryFullChargeEstimate.fromCounterAtLevel(2_500_000L, 50));
        assertEquals(5000, BatteryFullChargeEstimate.fromCounter(2_500_000L, 50, 100));
        assertEquals(4950, BatteryFullChargeEstimate.fromCounter(2_500_000L, 505, 1000));
        assertEquals(4941, BatteryFullChargeEstimate.fromCounterFraction(2_500_000L, 0.506));
        assertEquals(0, BatteryFullChargeEstimate.fromCounterAtLevel(500_000L, 10));
        assertEquals(0, BatteryFullChargeEstimate.fromCounter(2_500_000L, 199, 1000));
        assertEquals(0, BatteryFullChargeEstimate.fromCounterAtLevel(50_000_000L, 50));
        assertEquals(0, BatteryFullChargeEstimate.fromCounterAtLevel(2_500_000L, 101));
    }

    @Test public void healthNeverTreatsASingleCounterExtrapolationAsMeasuredCapacity() {
        BatteryHealth.HealthReading reading = BatteryHealth.resolveReading(
                0, 5000, 5000, "", "Android-Charge-Counter (geschätzt)");
        assertEquals(0, reading.percent);
        assertEquals(0, reading.capacityMah);
    }

    @Test public void appAttributionNeverExceedsObservedEnergyWhenDirectTelemetryRunsHot() {
        assertEquals(320, BatteryAppAttribution.estimateMah(400, 1000, 800, 0L, 0L));
        assertEquals(400, BatteryAppAttribution.estimateMah(400, 800, 1000, 0L, 0L));
        assertEquals(0, BatteryAppAttribution.estimateMah(400, 1000, 0, 0L, 0L));
        assertEquals(0, BatteryAppAttribution.estimateMah(-400, 1000, 800, 0L, 0L));
        assertEquals(300, BatteryAppAttribution.estimateMah(0, 0, 1200, 15L, 60L));
        assertEquals(0, BatteryAppAttribution.estimateFallbackMah(1200, 1200, 15L, 60L));
        assertEquals(150, BatteryAppAttribution.estimateFallbackMah(1200, 600, 15L, 60L));
        assertEquals(0, BatteryAppAttribution.estimateMah(0, 0, 0, 15L, 60L));
        assertEquals(500, BatteryAppAttribution.sampleMah(1000, 2L * 60L * 60L * 1000L, 30L * 60L * 1000L));
        assertEquals(0, BatteryAppAttribution.sampleMah(1000, 0L, 30L * 60L * 1000L));
    }

    @Test public void appAttributionUsesEnergyFromTheSameSelectedWindow() {
        assertEquals(420, BatteryAppAttribution.observedWindowMah(420, true, 900, 250));
        assertEquals(900, BatteryAppAttribution.observedWindowMah(0, true, 900, 250));
        assertEquals(250, BatteryAppAttribution.observedWindowMah(0, false, 900, 250));
        assertEquals(0, BatteryAppAttribution.observedWindowMah(0, true, -1, 250));
    }

    @Test public void appAttributionApportionsRemaindersWithoutExceedingObservedEnergy() {
        Map<String, Integer> measured = new HashMap<>();
        measured.put("app.c", 1);
        measured.put("app.a", 1);
        measured.put("app.b", 1);
        Map<String, Integer> direct = BatteryAppAttribution.scaleDirectMah(measured, 2);
        assertEquals(2, direct.get("app.a") + direct.get("app.b") + direct.get("app.c"));
        assertEquals(1, direct.get("app.a").intValue());
        assertEquals(1, direct.get("app.b").intValue());
        assertEquals(0, direct.get("app.c").intValue());

        Map<String, Long> foreground = new HashMap<>();
        foreground.put("app.c", 10L);
        foreground.put("app.a", 10L);
        foreground.put("app.b", 10L);
        Map<String, Integer> fallback = BatteryAppAttribution.apportion(2, foreground);
        assertEquals(2, fallback.get("app.a") + fallback.get("app.b") + fallback.get("app.c"));
        assertEquals(1, fallback.get("app.a").intValue());
        assertEquals(1, fallback.get("app.b").intValue());
        assertEquals(0, fallback.get("app.c").intValue());
    }

    @Test public void bucketedUsageIntervalsAreMergedPerPackageBeforeAttribution() {
        Map<String, Long> foreground = new HashMap<>();
        BatteryAppAttribution.addForegroundTime(foreground, "app.a", 40L * 60L * 1000L);
        BatteryAppAttribution.addForegroundTime(foreground, "app.b", 20L * 60L * 1000L);
        BatteryAppAttribution.addForegroundTime(foreground, "app.a", 30L * 60L * 1000L);
        BatteryAppAttribution.addForegroundTime(foreground, "app.c", -1L);
        assertEquals(70L * 60L * 1000L, foreground.get("app.a").longValue());
        assertEquals(20L * 60L * 1000L, foreground.get("app.b").longValue());
        assertFalse(foreground.containsKey("app.c"));
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

    @Test public void healthSnapshotFallsBackAtomicallyWhenSystemSohIsInvalid() {
        BatteryHealth.HealthReading reading = BatteryHealth.resolveReading(
                110, 5940, 6600, "Android BatteryManager", "lokale Lademessungen");
        assertEquals(90, reading.percent);
        assertEquals(5940, reading.capacityMah);
        assertEquals("lokale Lademessungen", reading.source);
    }

    @Test public void healthSnapshotKeepsSystemSohAndDerivedCapacityTogether() {
        BatteryHealth.HealthReading reading = BatteryHealth.resolveReading(
                91, 5940, 6600, "Android BatteryManager", "lokale Lademessungen");
        assertEquals(91, reading.percent);
        assertEquals(6006, reading.capacityMah);
        assertEquals("Android BatteryManager", reading.source);
    }

    @Test public void healthSnapshotKeepsMeasuredCapacityWhenDesignValueIsUnavailable() {
        BatteryHealth.HealthReading reading = BatteryHealth.resolveReading(
                0, 6600, 0, "", "lokale Lademessungen");
        assertEquals(0, reading.percent);
        assertEquals(6600, reading.capacityMah);
    }

    @Test public void invalidSystemHealthFallsBackWithTheFallbackSource() {
        BatteryHealth.ReportedReading reading = BatteryHealth.resolveReportedReading(
                110, 91, "Android BatteryManager", "OPlus/ColorOS-Batterietreiber (SoH)");
        assertEquals(91, reading.percent);
        assertEquals("OPlus/ColorOS-Batterietreiber (SoH)", reading.source);
    }

    @Test public void invalidSystemAndFallbackHealthAreUnavailable() {
        BatteryHealth.ReportedReading reading = BatteryHealth.resolveReportedReading(
                110, 101, "Android BatteryManager", "Batterie-Treiber (SoH)");
        assertEquals(0, reading.percent);
        assertEquals("", reading.source);
    }

    @Test public void samsungAsocIsAHealthAttributeButQualitativeHealthIsNot() {
        assertTrue(BatteryCapacity.isStateOfHealthAttribute("state_of_health"));
        assertTrue(BatteryCapacity.isStateOfHealthAttribute("fg_asoc"));
        assertTrue(BatteryCapacity.isStateOfHealthAttribute("battery_soh"));
        assertFalse(BatteryCapacity.isStateOfHealthAttribute("health"));
    }

    @Test public void invalidHealthReadingCannotKeepAProvenanceLabel() {
        BatteryHealth.HealthReading reading = new BatteryHealth.HealthReading(
                110, 0, "Android BatteryManager");
        assertEquals(0, reading.percent);
        assertEquals("", reading.source);
    }

    @Test public void healthSourceLabelNamesSamsungAsocExplicitly() {
        assertEquals("Samsung-ASOC",
                BatteryHealth.displaySourceLabel("Samsung-Batterietreiber (ASOC)"));
        assertEquals("Batterie-Treiber-SoH",
                BatteryHealth.displaySourceLabel("Batterie-Treiber (SoH)"));
        assertEquals("keine Messung", BatteryHealth.displaySourceLabel(null));
    }

    @Test public void widgetUsesShortLayoutForVeryLowHeight() {
        assertEquals(BatteryWidgetLayoutRules.SHORT,
                BatteryWidgetLayoutRules.select(320, 56));
        assertEquals(BatteryWidgetLayoutRules.SHORT,
                BatteryWidgetLayoutRules.select(180, 71));
    }

    @Test public void widgetUsesShortLayoutWhenWidthCannotFitThreeColumns() {
        assertEquals(BatteryWidgetLayoutRules.SHORT,
                BatteryWidgetLayoutRules.select(109, 276));
        assertEquals(BatteryWidgetLayoutRules.SHORT,
                BatteryWidgetLayoutRules.select(159, 130));
    }

    @Test public void widgetUsesCompactLayoutOnlyWhenHeightCanShowItsContent() {
        assertEquals(BatteryWidgetLayoutRules.COMPACT,
                BatteryWidgetLayoutRules.select(180, 72));
        assertEquals(BatteryWidgetLayoutRules.COMPACT,
                BatteryWidgetLayoutRules.select(219, 130));
    }

    @Test public void widgetUsesStandardLayoutForWideContent() {
        assertEquals(BatteryWidgetLayoutRules.STANDARD,
                BatteryWidgetLayoutRules.select(220, 72));
        assertEquals(BatteryWidgetLayoutRules.STANDARD,
                BatteryWidgetLayoutRules.select(624, 276));
    }

    @Test public void widgetResponsiveCutoffsCoverTheEntireDeclaredResizeRange() {
        assertEquals(BatteryWidgetLayoutRules.SHORT,
                BatteryWidgetLayoutRules.select(109, 56));
        assertEquals(BatteryWidgetLayoutRules.SHORT,
                BatteryWidgetLayoutRules.select(219, 71));
        assertEquals(BatteryWidgetLayoutRules.SHORT,
                BatteryWidgetLayoutRules.select(109, 72));
        assertEquals(BatteryWidgetLayoutRules.COMPACT,
                BatteryWidgetLayoutRules.select(160, 72));
        assertEquals(BatteryWidgetLayoutRules.COMPACT,
                BatteryWidgetLayoutRules.select(219, 276));
        assertEquals(BatteryWidgetLayoutRules.STANDARD,
                BatteryWidgetLayoutRules.select(220, 72));
    }

    @Test public void persistedUpdateResumeTargetsOnlyCompletedDownloads() {
        assertTrue(UpdateChecker.shouldResumePersistedDownload(DownloadManager.STATUS_SUCCESSFUL));
        assertFalse(UpdateChecker.shouldResumePersistedDownload(DownloadManager.STATUS_PENDING));
        assertFalse(UpdateChecker.shouldResumePersistedDownload(DownloadManager.STATUS_RUNNING));
        assertFalse(UpdateChecker.shouldResumePersistedDownload(DownloadManager.STATUS_FAILED));
    }

    @Test public void apkUpdateChecksUnknownSourcePermissionBeforeDownloading() {
        assertFalse(UpdateChecker.requiresInstallPermissionPrompt(25, false));
        assertTrue(UpdateChecker.requiresInstallPermissionPrompt(26, false));
        assertTrue(UpdateChecker.requiresInstallPermissionPrompt(37, false));
        assertFalse(UpdateChecker.requiresInstallPermissionPrompt(37, true));
    }

    @Test public void compactDurationsNeverNeedEllipsisForMetricCards() {
        assertEquals("—", BatteryDuration.compact(0));
        assertEquals("45 m", BatteryDuration.compact(45));
        assertEquals("2 h", BatteryDuration.compact(120));
        assertEquals("20 h 4 m", BatteryDuration.compact(1204));
    }

    @Test public void liveHeaderRefreshHasASeparateWideHitbox() {
        assertEquals(BatteryHeaderLayout.LIVE_REFRESH,
                BatteryHeaderLayout.actionAt(379f, 36f, 411f));
        assertEquals(BatteryHeaderLayout.LIVE_REFRESH,
                BatteryHeaderLayout.actionAt(379f, 12f, 411f));
        assertEquals(BatteryHeaderLayout.LIVE_REFRESH,
                BatteryHeaderLayout.actionAt(379f, 59f, 411f));
        assertEquals(BatteryHeaderLayout.NONE,
                BatteryHeaderLayout.actionAt(350f, 36f, 360f));
        assertEquals(BatteryHeaderLayout.NONE,
                BatteryHeaderLayout.actionAt(379f, 60f, 411f));
        assertEquals(BatteryHeaderLayout.OVERFLOW,
                BatteryHeaderLayout.actionAt(284f, 12f, 320f));
    }

    @Test public void headerActionsDoNotCaptureTheVisibleGuttersBetweenImageButtons() {
        assertEquals(BatteryHeaderLayout.NONE,
                BatteryHeaderLayout.actionAt(253f, 36f, 320f));
        assertEquals(BatteryHeaderLayout.NONE,
                BatteryHeaderLayout.actionAt(258f, 36f, 320f));
        assertEquals(BatteryHeaderLayout.NONE,
                BatteryHeaderLayout.actionAt(264f, 36f, 411f));
        assertEquals(BatteryHeaderLayout.NONE,
                BatteryHeaderLayout.actionAt(325f, 36f, 411f));
        assertEquals(BatteryHeaderLayout.NONE,
                BatteryHeaderLayout.actionAt(230f, 16f, 320f));
    }

    @Test public void overlayHeaderUsesStableTextInsteadOfAPlatformGlyph() {
        assertEquals("Akku 88%   +900 mA",
                BatteryOverlayText.header("88%", "+900 mA"));
    }

    @Test public void liveOverlayHidesOnlyWhileAmpereIsOnScreen() {
        assertTrue(BatteryOverlayVisibility.shouldShow(true, false));
        assertFalse(BatteryOverlayVisibility.shouldShow(true, true));
        assertFalse(BatteryOverlayVisibility.shouldShow(false, false));
        assertFalse(BatteryOverlayVisibility.shouldShow(false, true));
    }

    @Test public void metricCardsStackBeforeTheirLabelLaneBecomesTooNarrow() {
        assertTrue(BatteryMetricLayout.shouldStack(136f));
        assertFalse(BatteryMetricLayout.shouldStack(181f));
    }

    @Test public void liveOverviewUsesRoomierTwoColumnMetricsOnPhones() {
        assertTrue(BatteryMetricLayout.shouldUseCompactLiveFlow(284f));
        assertTrue(BatteryMetricLayout.shouldUseCompactLiveFlow(430f));
        assertFalse(BatteryMetricLayout.shouldUseCompactLiveFlow(480f));
        assertFalse(BatteryMetricLayout.shouldUseCompactLiveFlow(640f));
    }

    @Test public void finalHealthGateNeverReturnsAnImpossiblePercentage() {
        for (int value : new int[]{-100, 0, 101, 110, 1000, Integer.MAX_VALUE}) {
            assertEquals(0, BatteryHealth.displayPercent(value));
        }
        for (int value = 1; value <= 100; value++) {
            assertEquals(value, BatteryHealth.displayPercent(value));
        }
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

    @Test public void dischargeAlarmUsesSafeThresholdAndOnlyAlertsOnce() {
        assertEquals(5, BatteryDischargeAlarm.normalizeThreshold(1));
        assertEquals(15, BatteryDischargeAlarm.normalizeThreshold(15));
        assertEquals(50, BatteryDischargeAlarm.normalizeThreshold(90));
        assertTrue(BatteryDischargeAlarm.shouldAlert(15, false, 15, false, 16));
        assertTrue(BatteryDischargeAlarm.shouldAlert(15, false, 15, false, -1));
        assertFalse(BatteryDischargeAlarm.shouldAlert(15, false, 15, false, 15));
        assertFalse(BatteryDischargeAlarm.shouldAlert(16, false, 15, false, 17));
        assertFalse(BatteryDischargeAlarm.shouldAlert(15, true, 15, false, 16));
        assertFalse(BatteryDischargeAlarm.shouldAlert(10, false, 15, true, 16));
    }

    @Test public void dischargeAlarmResetsOnChargingOrHysteresis() {
        assertTrue(BatteryDischargeAlarm.shouldReset(19, false, 15));
        assertFalse(BatteryDischargeAlarm.shouldReset(18, false, 15));
        assertTrue(BatteryDischargeAlarm.shouldReset(10, true, 15));
        assertTrue(BatteryDischargeAlarm.shouldReset(-1, false, 15));
    }

    @Test public void chargeAlarmOnlyFiresOnTargetCrossingAndUsesHysteresis() {
        assertTrue(BatteryChargeAlarm.shouldAlert(80, true, 80, false, -1));
        assertFalse(BatteryChargeAlarm.shouldAlert(80, true, 80, true, 79));
        assertTrue(BatteryChargeAlarm.shouldAlert(80, true, 80, false, 79));
        assertFalse(BatteryChargeAlarm.shouldAlert(80, true, 80, false, 80));
        assertFalse(BatteryChargeAlarm.shouldReset(79, true, 80));
        assertTrue(BatteryChargeAlarm.shouldReset(77, true, 80));
        assertTrue(BatteryChargeAlarm.shouldReset(80, false, 80));
    }

    @Test public void monitorWatchdogTreatsMissingAndRewoundHeartbeatsAsStale() {
        assertTrue(BatteryMonitorWatchdog.isHeartbeatStale(0L, 10_000L));
        assertTrue(BatteryMonitorWatchdog.isHeartbeatStale(50_000L, 49_999L));
        assertTrue(BatteryMonitorWatchdog.isHeartbeatStale(0L, 0L));
        assertFalse(BatteryMonitorWatchdog.isHeartbeatStale(100_000L,
                100_000L + BatteryMonitorWatchdog.STALE_AFTER_MS - 1L));
        assertTrue(BatteryMonitorWatchdog.isHeartbeatStale(100_000L,
                100_000L + BatteryMonitorWatchdog.STALE_AFTER_MS));
    }

    @Test public void voltageRejectsMissingAndImplausibleValues() {
        assertEquals(4200, BatteryVoltage.normalizeMilliVolts(4200));
        assertEquals(500, BatteryVoltage.normalizeMilliVolts(500));
        assertEquals(0, BatteryVoltage.normalizeMilliVolts(0));
        assertEquals(0, BatteryVoltage.normalizeMilliVolts(-1));
        assertEquals(0, BatteryVoltage.normalizeMilliVolts(20001));
        assertEquals(4200, BatteryVoltage.normalizeSysfsVoltage(4200));
        assertEquals(4200, BatteryVoltage.normalizeSysfsVoltage(4_200_000));
        assertEquals(0, BatteryVoltage.normalizeSysfsVoltage(400));
        assertEquals(0, BatteryVoltage.normalizeSysfsVoltage(20_000_001));
    }

    @Test public void sharedBatteryReadingKeepsValidatedBroadcastFieldsTogether() {
        BatteryReading reading = BatteryReading.fromValidatedValues(
                80, BatteryManager.BATTERY_STATUS_CHARGING,
                BatteryManager.BATTERY_PLUGGED_USB, 275, 4200, 0);
        assertEquals(80, reading.level);
        assertEquals(BatteryManager.BATTERY_STATUS_CHARGING, reading.status);
        assertEquals(BatteryManager.BATTERY_PLUGGED_USB, reading.plugged);
        assertTrue(reading.charging);
        assertEquals(275, reading.temperatureTenths);
        assertEquals(4200, reading.voltageMv);
        assertEquals(0, reading.currentMa);
        BatteryReading unavailable = BatteryReading.fromValidatedValues(
                -1, BatteryManager.BATTERY_STATUS_UNKNOWN, 0, 0, 0, 0);
        assertEquals(-1, unavailable.level);
        assertFalse(unavailable.charging);
    }

    @Test public void sharedBatteryReadingKeepsChargingWhenOemOmitsPlugField() {
        BatteryReading reading = BatteryReading.fromValidatedValues(
                80, BatteryManager.BATTERY_STATUS_CHARGING, 0, 0, 0, 0, false);
        assertTrue(reading.charging);

        BatteryReading explicitUnplugged = BatteryReading.fromValidatedValues(
                80, BatteryManager.BATTERY_STATUS_CHARGING, 0, 0, 0, 0, true);
        assertFalse(explicitUnplugged.charging);
    }

    @Test public void chargeTimeEstimateNeverInventsDurationWithoutCapacity() {
        assertEquals(30L, BatteryTimeEstimate.minutesToTarget(50, 100,
                3000, 3000f, 0));
        assertEquals(30L, BatteryTimeEstimate.minutesToTarget(50, 100,
                3000, 0f, 3000));
        assertEquals(0L, BatteryTimeEstimate.minutesToTarget(50, 100,
                0, 3000f, 3000));
        assertEquals(0L, BatteryTimeEstimate.minutesToTarget(50, 100,
                3000, Float.NaN, 49));
        assertEquals(0L, BatteryTimeEstimate.minutesToTarget(50, 100,
                3000, 49f, 0));
        assertEquals(0L, BatteryTimeEstimate.minutesToTarget(50, 100,
                3000, Float.POSITIVE_INFINITY, 100_001));
        assertEquals(0L, BatteryTimeEstimate.minutesToTarget(100, 100,
                3000, 3000f, 3000));
    }

    @Test public void fuelGaugeTimeRejectsSentinelsAndPrefersValidFullEstimate() {
        assertEquals(60, BatteryFuelGaugeTime.normalizeSeconds(3600));
        assertEquals(1, BatteryFuelGaugeTime.normalizeSeconds(61));
        assertEquals(0, BatteryFuelGaugeTime.normalizeSeconds(0));
        assertEquals(0, BatteryFuelGaugeTime.normalizeSeconds(-1));
        assertEquals(0, BatteryFuelGaugeTime.normalizeSeconds(48L * 60L * 60L + 1L));
        assertEquals(90, BatteryFuelGaugeTime.fullMinutes(5400, 7200));
        assertEquals(120, BatteryFuelGaugeTime.fullMinutes(0, 7200));
        assertEquals(0, BatteryFuelGaugeTime.fullMinutes(-1, 0));
    }

    @Test public void runtimeEstimateBlendsCurrentSessionOnlyAfterEnoughEvidence() {
        assertEquals(10f, BatteryRuntimeEstimate.blendRate(10f, 20f, 5L * 60L * 1000L), 0.001f);
        assertEquals(16f, BatteryRuntimeEstimate.blendRate(10f, 20f, 30L * 60L * 1000L), 0.001f);
        assertEquals(20f, BatteryRuntimeEstimate.blendRate(0f, 20f, 30L * 60L * 1000L), 0.001f);
        assertEquals(0f, BatteryRuntimeEstimate.blendRate(Float.NaN, Float.POSITIVE_INFINITY,
                30L * 60L * 1000L), 0.001f);
    }

    @Test public void runtimeEstimateUsesChargeCounterEnergyWhenPercentIsFlat() {
        assertEquals(10f, BatteryRuntimeEstimate.rateFromObserved(0f, 300, 3000,
                60L * 60L * 1000L), 0.001f);
        assertEquals(5f, BatteryRuntimeEstimate.rateFromObserved(5f, 0, 3000,
                60L * 60L * 1000L), 0.001f);
        assertEquals(0f, BatteryRuntimeEstimate.rateFromObserved(1f, 0, 3000,
                5L * 60L * 1000L), 0.001f);
    }

    @Test public void chargingRuntimeSourceRequiresUsableDischargeEvidence() {
        assertTrue(BatteryRuntimeEstimate.hasHistoricalEstimate(75, 0f, 0L, 12f));
        assertTrue(BatteryRuntimeEstimate.hasHistoricalEstimate(75, 4f, 5L * 60L * 1000L, 0f));
        assertFalse(BatteryRuntimeEstimate.hasHistoricalEstimate(75, 4f, 4L * 60L * 1000L, 0f));
        assertFalse(BatteryRuntimeEstimate.hasHistoricalEstimate(-1, 4f, 10L * 60L * 1000L, 12f));
        assertFalse(BatteryRuntimeEstimate.hasHistoricalEstimate(75, Float.NaN, 10L * 60L * 1000L, 0f));
    }

    @Test public void liveCurrentRuntimeRequiresPlausibleCapacityAndSignal() {
        assertEquals(900L, BatteryRuntimeEstimate.minutesFromCurrent(75, 4000, 200));
        assertEquals(0L, BatteryRuntimeEstimate.minutesFromCurrent(75, 0, 200));
        assertEquals(0L, BatteryRuntimeEstimate.minutesFromCurrent(75, 4000, 49));
        assertEquals(0L, BatteryRuntimeEstimate.minutesFromCurrent(101, 4000, 200));
        assertEquals(0L, BatteryRuntimeEstimate.minutesFromCurrent(75, 30001, 200));
    }

    @Test public void timelineCapsUnobservedIntegrationGaps() {
        assertEquals(30L * 60L * 1000L,
                BatteryTimelineRules.cappedElapsed(1_000L, 4_000_000L, 30L * 60L * 1000L));
        assertEquals(5_000L, BatteryTimelineRules.cappedElapsed(1_000L, 6_000L, 30L * 60L * 1000L));
        assertEquals(0L, BatteryTimelineRules.cappedElapsed(6_000L, 1_000L, 30L * 60L * 1000L));
    }

    @Test public void thermalStatusKeepsOnlyAndroidsKnownRange() {
        assertEquals(android.os.PowerManager.THERMAL_STATUS_NONE,
                BatteryThermalStatus.normalize(android.os.PowerManager.THERMAL_STATUS_NONE));
        assertEquals(android.os.PowerManager.THERMAL_STATUS_SEVERE,
                BatteryThermalStatus.normalize(android.os.PowerManager.THERMAL_STATUS_SEVERE));
        assertEquals(BatteryThermalStatus.UNKNOWN, BatteryThermalStatus.normalize(-2));
        assertEquals(BatteryThermalStatus.UNKNOWN, BatteryThermalStatus.normalize(99));
        assertEquals("Kritisch", BatteryThermalStatus.label(
                android.os.PowerManager.THERMAL_STATUS_CRITICAL));
        assertEquals("Nicht verfügbar", BatteryThermalStatus.label(BatteryThermalStatus.UNKNOWN));
    }

    @Test public void batteryTechnologyKeepsOnlyShortPrintableBroadcastValues() {
        assertEquals("Li-ion", BatteryTechnology.normalize("  Li-ion "));
        assertEquals("", BatteryTechnology.normalize("\u0000Li-ion"));
        assertEquals("", BatteryTechnology.normalize("123456789012345678901234567890123"));
        assertEquals("", BatteryTechnology.normalize(null));
    }

    @Test public void oemChargeThresholdRejectsDisabledAndImpossibleValues() {
        assertEquals(1, BatteryChargeControl.normalizeThreshold(1));
        assertEquals(100, BatteryChargeControl.normalizeThreshold(100));
        assertEquals(0, BatteryChargeControl.normalizeThreshold(0));
        assertEquals(0, BatteryChargeControl.normalizeThreshold(-1));
        assertEquals(0, BatteryChargeControl.normalizeThreshold(101));
        assertEquals("OEM-Ladefenster 40–80%",
                new BatteryChargeControl.Reading(80, 40, "test").label());
        assertEquals("OEM-Limit 80%",
                new BatteryChargeControl.Reading(80, 0, "test").label());
    }

    @Test public void manufactureDateRequiresARealCompleteCalendarDate() {
        assertTrue(BatteryManufactureDate.isValidDate(2024, 2, 29));
        assertFalse(BatteryManufactureDate.isValidDate(2023, 2, 29));
        assertFalse(BatteryManufactureDate.isValidDate(2024, 4, 31));
        assertFalse(BatteryManufactureDate.isValidDate(1969, 12, 31));
        assertFalse(BatteryManufactureDate.isValidDate(2101, 1, 1));
        assertEquals("2024-02-29", BatteryManufactureDate.fromParts(
                2024, 2, 29, "test").label());
        assertFalse(BatteryManufactureDate.fromParts(2024, 2, 30, "test").isAvailable());
        assertEquals("—", BatteryManufactureDate.Reading.unavailable().label());
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

    @Test public void telemetryDiagnosticsKeepVoltageDeviceAgnosticAndFlagOnlyMeasuredProblems() {
        String rows = "1700000000000,80,0,-1200,44.0,4.18,6600,1,,12,0\n"
                + "1700000060000,79,0,-1800,48.0,4.02,6500,1,,12,0\n"
                + "1700010860000,78,0,-900,46.0,3.95,6400,1,,12,0";
        BatteryTelemetryDiagnostics.Summary summary = BatteryTelemetryDiagnostics.analyze(
                rows, 15L * 60L * 1000L);
        assertEquals(3, summary.sampleCount);
        assertEquals(3, summary.dischargeSamples);
        assertEquals(440, summary.minTemperatureTenths);
        assertEquals(460, summary.averageTemperatureTenths);
        assertEquals(480, summary.maxTemperatureTenths);
        assertEquals(3950, summary.minDischargeVoltageMv);
        assertEquals(78, summary.minDischargeVoltageLevel);
        assertEquals(1800, summary.peakDischargeMa);
        assertTrue(summary.hasHighTemperature());
        assertTrue(summary.samplingGap);
        assertEquals(0, BatteryTelemetryDiagnostics.analyze(
                "1700000000000,80,1,900,25.0,8.40,6600,1,,12,1", 900000L).minDischargeVoltageMv);
    }

    @Test public void diagnosticReportExplainsMeasuredFlagsWithoutInventingEarlyCutoff() {
        String rows = "1700000000000,80,0,-1200,48.0,4.18,6600,1,,12,0\n"
                + "1700010860000,78,0,-900,46.0,3.95,6400,1,,12,0";
        String report = BatteryDiagnosticReport.build(rows, 15L * 60L * 1000L, 123L);
        assertTrue(report.contains("Max. Akkutemperatur: 48,0 °C"));
        assertTrue(report.contains("Min. Entladespannung: 3,950 V bei 78 %"));
        assertTrue(report.contains("WARNUNG: Akku erreichte mindestens 48 °C."));
        assertTrue(report.contains("Sampling-Lücke"));
        assertTrue(report.contains("nicht als Early Cutoff"));
        assertFalse(report.contains("6.200 V"));
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
        assertFalse(BatterySessionRules.isValid("Charge,+12%,18 Min.,11.09. 12:00,62,50,800,0.12"));
        assertFalse(BatterySessionRules.isValid("Discharge,-12%,18 Min.,11.09. 12:00,38,50,800,0.12"));
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

    @Test public void chartsLeaveGapsForMissedSamplingWindows() {
        long fifteenMinutes = 15L * 60L * 1000L;
        assertFalse(BatteryTimelineRules.isSamplingGap(1_000L, 1_000L + fifteenMinutes, fifteenMinutes));
        assertFalse(BatteryTimelineRules.isSamplingGap(2_000L, 1_000L, fifteenMinutes));
        assertFalse(BatteryTimelineRules.isSamplingGap(1_000L,
                1_000L + 2L * 60L * 60L * 1000L, fifteenMinutes));
        assertTrue(BatteryTimelineRules.isSamplingGap(1_000L,
                1_000L + 2L * 60L * 60L * 1000L + 1L, fifteenMinutes));
        assertFalse(BatteryTimelineRules.isSamplingGap(1_000L,
                1_000L + 31L * 60L * 1000L, fifteenMinutes));
        long twoHours = 2L * 60L * 60L * 1000L;
        assertTrue(BatteryTimelineRules.isSamplingGap(1_000L,
                1_000L + 2L * twoHours + 1L, twoHours));
    }

    @Test public void chartsInterpolateOnlyShortMissingRuns() {
        long fifteenMinutes = 15L * 60L * 1000L;
        assertFalse(BatteryTimelineRules.shouldInterpolate(1_000L, 1_000L + fifteenMinutes, fifteenMinutes));
        assertTrue(BatteryTimelineRules.shouldInterpolate(1_000L,
                1_000L + 4L * fifteenMinutes, fifteenMinutes));
        assertFalse(BatteryTimelineRules.shouldInterpolate(1_000L,
                1_000L + 7L * fifteenMinutes, fifteenMinutes));
        assertFalse(BatteryTimelineRules.shouldInterpolate(2_000L, 1_000L, fifteenMinutes));
    }

    @Test public void isolatedCurrentChartSampleNeedsAVisibleMarker() {
        assertFalse(BatteryTimelineRules.shouldRenderSingleSampleMarker(0));
        assertTrue(BatteryTimelineRules.shouldRenderSingleSampleMarker(1));
        assertFalse(BatteryTimelineRules.shouldRenderSingleSampleMarker(2));
    }

    @Test public void unobservedGapResetsOnlyAnOlderOpenSession() {
        long interval = 15L * 60L * 1000L;
        long previousSample = 2_000L;
        long afterGap = previousSample + 2L * 60L * 60L * 1000L + 1L;
        assertTrue(BatteryTimelineRules.shouldResetSession(1_000L, previousSample, afterGap, interval));
        assertFalse(BatteryTimelineRules.shouldResetSession(3_000L, previousSample, afterGap, interval));
        assertFalse(BatteryTimelineRules.shouldResetSession(1_000L, previousSample,
                previousSample + interval, interval));
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

    @Test public void sessionEnergyRejectsCounterResetsBeforeEfcCalculation() {
        assertEquals(6600, BatterySessionRules.normalizeEnergy(6600, 6600));
        assertEquals(19800, BatterySessionRules.normalizeEnergy(19800, 6600));
        assertEquals(0, BatterySessionRules.normalizeEnergy(19801, 6600));
        assertEquals(0, BatterySessionRules.normalizeEnergy(-1, 6600));
        assertEquals(30000, BatterySessionRules.normalizeEnergy(30000, 0));
        assertEquals(0, BatterySessionRules.normalizeEnergy(30001, 0));
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

    @Test public void chargerCapabilityKeepsValidatedRawLimitsSeparateFromPower() {
        BatteryChargerCapability.Reading reading = BatteryChargerCapability.Reading.fromRaw(
                1_500_000L, 5_000_000L);
        assertEquals(1500, reading.maxCurrentMa);
        assertEquals(5000, reading.maxVoltageMv);
        assertEquals(7500, reading.maxPowerMilliwatts);
        assertEquals("Ladegerät max. 1,50 A · 5,00 V · 7,5 W", reading.label());

        BatteryChargerCapability.Reading currentOnly = BatteryChargerCapability.Reading.fromRaw(
                900_000L, 0L);
        assertEquals(900, currentOnly.maxCurrentMa);
        assertEquals(0, currentOnly.maxVoltageMv);
        assertEquals(0, currentOnly.maxPowerMilliwatts);
        assertEquals("Ladegerät max. 0,90 A", currentOnly.label());

        BatteryChargerCapability.Reading hardware =
                BatteryChargerCapability.Reading.fromHardwareCurrentLimit(5_000_000L);
        assertEquals(5000, hardware.maxCurrentMa);
        assertEquals(0, hardware.maxVoltageMv);
        assertEquals(0, hardware.maxPowerMilliwatts);
        assertEquals("Ladehardware max. 5,00 A", hardware.label());
        assertEquals("", BatteryChargerCapability.Reading.fromHardwareCurrentLimit(0L).label());
    }

    @Test public void internalResistanceAcceptsOnlyPlausibleMicroOhms() {
        assertEquals(42, BatteryInternalResistance.normalizeMilliOhms(42_000L));
        assertEquals(1, BatteryInternalResistance.normalizeMilliOhms(500L));
        assertEquals(0, BatteryInternalResistance.normalizeMilliOhms(0L));
        assertEquals(0, BatteryInternalResistance.normalizeMilliOhms(-1L));
        assertEquals(0, BatteryInternalResistance.normalizeMilliOhms(10_000_001L));
        BatteryInternalResistance.Reading reading = BatteryInternalResistance.fromRaw(
                42_000L, "test");
        assertTrue(reading.isAvailable());
        assertEquals("42 mΩ", reading.label());
        assertEquals("0,5 mΩ", BatteryInternalResistance.fromRaw(500L, "test").label());
        assertEquals("—", BatteryInternalResistance.fromRaw(0L, "test").label());
    }

    @Test public void kernelChargeTypeParsesOnlyKnownReadOnlyAlgorithms() {
        assertEquals(1, BatteryChargeType.normalize("Trickle"));
        assertEquals(2, BatteryChargeType.normalize("FAST"));
        assertEquals(4, BatteryChargeType.normalize("Adaptive"));
        assertEquals(6, BatteryChargeType.normalize("Long_Life"));
        assertEquals(0, BatteryChargeType.normalize("Unknown"));
        assertEquals(0, BatteryChargeType.normalize("inhibit-charge"));
        assertEquals(3, BatteryChargeType.fromTypes(
                "Fast [Standard] Long_Life", "test").type);
        assertEquals(0, BatteryChargeType.fromTypes("Fast Standard", "test").type);
        assertEquals("Benutzerdefiniert", BatteryChargeType.label(5));
    }

    @Test public void kernelChargeBehaviourParsesOnlyKnownReadOnlyStates() {
        assertEquals("auto", BatteryChargeBehaviour.normalize("[auto]"));
        assertEquals("inhibit-charge", BatteryChargeBehaviour.normalize("INHIBIT-CHARGE"));
        assertEquals("inhibit-charge-awake", BatteryChargeBehaviour.normalize("inhibit-charge-awake"));
        assertEquals("force-discharge", BatteryChargeBehaviour.normalize("force-discharge"));
        assertEquals("", BatteryChargeBehaviour.normalize("bypass"));
        assertEquals("", BatteryChargeBehaviour.normalize("Unknown"));
        assertEquals("Laden gesperrt", BatteryChargeBehaviour.label("inhibit-charge"));
        assertEquals("Entladung erzwungen", BatteryChargeBehaviour.label("force-discharge"));
        assertTrue(BatteryChargeBehaviour.fromText("[auto]", "test").isAvailable());
        assertEquals("", BatteryChargeBehaviour.fromText("charge", "test").behaviour);
    }

    @Test public void capacityErrorMarginAllowsZeroButRejectsInvalidSentinels() {
        assertEquals(0, BatteryCapacityErrorMargin.normalize(0L));
        assertEquals(100, BatteryCapacityErrorMargin.normalize(100L));
        assertEquals(-1, BatteryCapacityErrorMargin.normalize(-1L));
        assertEquals(-1, BatteryCapacityErrorMargin.normalize(101L));
        BatteryCapacityErrorMargin.Reading reading =
                BatteryCapacityErrorMargin.fromRaw(7L, "test");
        assertTrue(reading.isAvailable());
        assertEquals("±7 %", reading.label());
        assertEquals("±0 %", BatteryCapacityErrorMargin.fromRaw(0L, "test").label());
        assertEquals("—", BatteryCapacityErrorMargin.fromRaw(-1L, "test").label());
    }

    @Test public void currentStatisticsExposeMinimumAverageAndMaximum() {
        BatteryCurrentStats.Summary summary = BatteryCurrentStats.summarize(
                Arrays.asList(120, 300, 180, 0, -5, null));
        assertEquals(120, summary.minimumMa);
        assertEquals(200, summary.averageMa);
        assertEquals(300, summary.maximumMa);
        assertEquals(3, summary.sampleCount);
        assertFalse(BatteryCurrentStats.summarize(Arrays.asList(0, -1)).isAvailable());
    }

    @Test public void batteryPowerUsesValidatedCurrentAndVoltage() {
        assertEquals(4500, BatteryPower.milliWatts(900, 5000));
        assertEquals(4500, BatteryPower.milliWatts(-900, 5000));
        assertEquals(0, BatteryPower.milliWatts(0, 5000));
        assertEquals(0, BatteryPower.milliWatts(900, 0));
        assertEquals(0, BatteryPower.milliWatts(Integer.MIN_VALUE, 5000));
        assertEquals("≈ 4,5 W", BatteryPower.label(4500));
    }

    @Test public void sharedTelemetryTextKeepsDirectionAcrossOutputSurfaces() {
        assertEquals("+900 mA", BatteryTelemetryText.current(900, true, true));
        assertEquals("−900mA", BatteryTelemetryText.current(900, false, false));
        assertEquals("+4,5 W", BatteryTelemetryText.power(900, 5000, true));
        assertEquals("−4,5 W", BatteryTelemetryText.power(900, 5000, false));
        assertEquals("—", BatteryTelemetryText.current(0, true, true));
        assertEquals("—", BatteryTelemetryText.power(900, 0, true));
    }

    @Test public void currentParserRejectsSentinelsAndUnrealisticSpikes() {
        assertEquals(900, BatteryCurrent.fromMicroamps(900_000));
        assertEquals(50, BatteryCurrent.fromMicroamps(50_000));
        assertEquals(900, BatteryCurrent.fromMicroamps(900_000L));
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
        Map<String, UsageEventAccumulator.State> active = new HashMap<>();
        UsageEventAccumulator.apply(totals, active, "app.one", "Main", 90L, 100L, 500L, true, false);
        UsageEventAccumulator.apply(totals, active, "app.one", "Main", 250L, 100L, 500L, false, true);
        UsageEventAccumulator.apply(totals, active, "app.one", "Main", 300L, 100L, 500L, true, false);
        UsageEventAccumulator.closeActive(totals, active, 500L);
        assertEquals(Long.valueOf(350L), totals.get("app.one"));
    }

    @Test public void usageEventsKeepActivitySwitchOpenAndIgnoreDuplicateCloses() {
        Map<String, Long> totals = new HashMap<>();
        Map<String, UsageEventAccumulator.State> active = new HashMap<>();
        UsageEventAccumulator.apply(totals, active, "app.one", "A", 100L, 100L, 500L, true, false);
        UsageEventAccumulator.apply(totals, active, "app.one", "B", 150L, 100L, 500L, true, false);
        UsageEventAccumulator.apply(totals, active, "app.one", "A", 200L, 100L, 500L, false, true);
        UsageEventAccumulator.apply(totals, active, "app.one", "A", 210L, 100L, 500L, false, true);
        UsageEventAccumulator.apply(totals, active, "app.one", "B", 300L, 100L, 500L, false, true);
        assertEquals(Long.valueOf(200L), totals.get("app.one"));
    }

    @Test public void usageEventsScreenOffClosesEveryOpenPackage() {
        Map<String, Long> totals = new HashMap<>();
        Map<String, UsageEventAccumulator.State> active = new HashMap<>();
        UsageEventAccumulator.apply(totals, active, "app.one", "A", 100L, 100L, 500L, true, false);
        UsageEventAccumulator.apply(totals, active, "app.two", "B", 120L, 100L, 500L, true, false);
        UsageEventAccumulator.closeAll(totals, active, 200L);
        assertEquals(Long.valueOf(100L), totals.get("app.one"));
        assertEquals(Long.valueOf(80L), totals.get("app.two"));
        assertTrue(active.isEmpty());
    }
}
