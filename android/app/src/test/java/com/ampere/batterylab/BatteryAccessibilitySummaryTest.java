package com.ampere.batterylab;

import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Guards the screen-reader summary strings built for each dashboard view. */
public class BatteryAccessibilitySummaryTest {

    @Test public void dischargeEstimateIncludesLabelAndValueAndSource() {
        String summary = BatteryAccessibilitySummary.dischargeEstimate(
                "Restlaufzeit bei normaler Nutzung", "8 Std.", "Lokale Telemetrie");
        assertTrue(summary, summary.contains("Restlaufzeit bei normaler Nutzung"));
        assertTrue(summary, summary.contains("8 Std."));
        assertTrue(summary, summary.contains("Lokale Telemetrie"));
    }

    @Test public void dischargeEstimateOmitsSourceForUnavailableValue() {
        String summary = BatteryAccessibilitySummary.dischargeEstimate(
                "Restlaufzeit", "\u2014", "");
        assertTrue(summary, summary.contains("nicht verf\u00fcgbar"));
        assertFalse(summary, summary.contains("Datenquelle"));
    }

    @Test public void dischargeEstimateShowsHintWhenOnlySourceHasContent() {
        String summary = BatteryAccessibilitySummary.dischargeEstimate(
                "Restlaufzeit", "\u2014", "Keine Sch\u00e4tzung m\u00f6glich");
        assertTrue(summary, summary.contains("Hinweis"));
        assertTrue(summary, summary.contains("Keine Sch\u00e4tzung m\u00f6glich"));
    }

    @Test public void dischargeEstimateBlankSourceShowsUnavailable() {
        String summary = BatteryAccessibilitySummary.dischargeEstimate(
                "Restlaufzeit", "8 Std.", "   ");
        assertTrue(summary, summary.contains("8 Std."));
        // Blank source is rendered as "Datenquelle: nicht verfügbar".
        assertTrue(summary, summary.contains("Datenquelle: nicht verfügbar"));
    }

    @Test public void dischargeEstimateReachesAndVollStillReportsSource() {
        // The discharge estimate reports the source for every available value,
        // including reached-target sentinels like "Erreicht" and "Voll".
        String summary = BatteryAccessibilitySummary.dischargeEstimate(
                "Restlaufzeit", "Erreicht", "Lokale Telemetrie");
        assertTrue(summary, summary.contains("Datenquelle: Lokale Telemetrie"));

        String summaryFull = BatteryAccessibilitySummary.dischargeEstimate(
                "Restlaufzeit", "Voll", "Lokale Telemetrie");
        assertTrue(summaryFull, summaryFull.contains("Datenquelle: Lokale Telemetrie"));
    }

    @Test public void overviewIncludesCurrentTemperatureVoltage() {
        String summary = BatteryAccessibilitySummary.overview(false,
                "8 Std.", "Lokale Telemetrie", "+850 mA", "25.0 \u00b0C", "3.8 V");
        assertTrue(summary, summary.contains("Akkustrom"));
        assertTrue(summary, summary.contains("+850 mA"));
        assertTrue(summary, summary.contains("Temperatur"));
        assertTrue(summary, summary.contains("25.0 \u00b0C"));
        assertTrue(summary, summary.contains("Spannung"));
        assertTrue(summary, summary.contains("3.8 V"));
    }

    @Test public void dischargeOverviewIncludesThreeForecasts() {
        String summary = BatteryAccessibilitySummary.discharge(
                "4 Std.", "Lokale Telemetrie",
                "12 Std.", "Lokale Telemetrie",
                "8 Std.", "Historische Sch\u00e4tzung");
        assertTrue(summary, summary.contains("4 Std."));
        assertTrue(summary, summary.contains("12 Std."));
        assertTrue(summary, summary.contains("8 Std."));
    }

    @Test public void historySummarisesChargedConsumedAndWear() {
        BatteryHistoryStats.Bucket bucket = new BatteryHistoryStats.Bucket(0, "");
        bucket.chargedMah = 1500;
        bucket.consumedMah = 900;
        bucket.wearCycles = 0.4f;
        bucket.chargeConsumptionRatioPercent = 167;
        bucket.measuredIntervals = 5;
        ArrayList<BatteryHistoryStats.Bucket> buckets = new ArrayList<>();
        buckets.add(bucket);

        String summary = BatteryAccessibilitySummary.history(
                "T\u00e4glich", "Heute", bucket, buckets, true);
        assertTrue(summary, summary.contains("1500 mAh"));
        assertTrue(summary, summary.contains("900 mAh"));
        assertTrue(summary, summary.contains("0,40 EFC"));
        assertTrue(summary, summary.contains("167 Prozent"));
    }

    @Test public void historyWearFieldReadsUnavailableWhenCapacityMissing() {
        BatteryHistoryStats.Bucket bucket = new BatteryHistoryStats.Bucket(0, "");
        bucket.chargedMah = 100;
        bucket.consumedMah = 80;
        bucket.wearCycles = 0.5f;
        bucket.chargeConsumptionRatioPercent = 125;
        bucket.measuredIntervals = 1;
        ArrayList<BatteryHistoryStats.Bucket> buckets = new ArrayList<>();
        buckets.add(bucket);

        String summary = BatteryAccessibilitySummary.history(
                "Woche", "Diese Woche", bucket, buckets, false);
        // The wear field must read as not computable.
        assertTrue(summary, summary.contains("Akkuverschleiß: nicht berechenbar"));
        // The bucket bar segment skips the wear value entirely.
        assertFalse(summary, summary.contains("Verschleiß 0,50"));
    }

    @Test public void historyWithNullBucketReportsNoData() {
        ArrayList<BatteryHistoryStats.Bucket> buckets = new ArrayList<>();
        String summary = BatteryAccessibilitySummary.history(
                "Monat", "Diesen Monat", null, buckets, true);
        assertTrue(summary, summary.contains("keine Messdaten"));
    }

    @Test public void healthSummaryIncludesAllFields() {
        String summary = BatteryAccessibilitySummary.health(
                "92 Prozent", "2950 mAh", "3200 mAh",
                "Lokale Telemetrie", "Aktuell", "350",
                "Gering", "24.5 \u00b0C", "3.8 V");
        assertTrue(summary, summary.contains("92 Prozent"));
        assertTrue(summary, summary.contains("2950 mAh"));
        assertTrue(summary, summary.contains("3200 mAh"));
        assertTrue(summary, summary.contains("Lokale Telemetrie"));
        assertTrue(summary, summary.contains("350"));
        assertTrue(summary, summary.contains("Sch\u00e4tzung"));
    }

    @Test public void chargingSummaryOmitsCurrentWhenInactive() {
        String summary = BatteryAccessibilitySummary.charging(false,
                "Voll", "+0 mA", "100 Prozent",
                "0 Min.", "Erreicht", "+0 mA", "+0 mA",
                "3000 mAh", "60 Min.", "25.0 \u00b0C", "4.1 V", "USB");
        assertFalse(summary, summary.contains("Akkustrom"));
    }

    @Test public void chargingSummaryIncludesCurrentWhenActive() {
        String summary = BatteryAccessibilitySummary.charging(true,
                "Laden", "+1200 mA", "80 Prozent",
                "45 Min.", "Lokale Telemetrie", "+1500 mA", "+800 mA",
                "1200 mAh", "30 Min.", "25.0 \u00b0C", "4.0 V", "USB-C");
        assertTrue(summary, summary.contains("+1200 mA"));
        assertTrue(summary, summary.contains("80 Prozent"));
        assertTrue(summary, summary.contains("USB-C"));
    }

    @Test public void unavailableValuesAreReplacedByPhrase() {
        String summary = BatteryAccessibilitySummary.overview(true,
                "8 Std.", "Lokale Telemetrie", "\u2014", "\u2014", "\u2014");
        assertFalse(summary, summary.contains("\u2014"));
        assertTrue(summary, summary.contains("nicht verf\u00fcgbar"));
    }
}
