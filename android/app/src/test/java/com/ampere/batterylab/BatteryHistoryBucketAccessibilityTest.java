package com.ampere.batterylab;

import java.util.Locale;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BatteryHistoryBucketAccessibilityTest {
    @Test public void describesDateAndActualMetricsForMeasuredBucket() {
        BatteryHistoryStats.Bucket bucket = new BatteryHistoryStats.Bucket(0L, "Jan.");
        bucket.chargedMah = 1234;
        bucket.consumedMah = 567;
        bucket.measuredIntervals = 3;
        bucket.wearCycles = .28f;
        bucket.chargeConsumptionRatioPercent = 218;

        String description = BatteryHistoryBucketAccessibility.description(bucket, 30, true);

        assertTrue(description.contains("Januar 1970"));
        assertTrue(description.contains("Aufgeladen 1234 mAh"));
        assertTrue(description.contains("Verbrauch 567 mAh"));
        assertTrue(description.contains("0,28 EFC"));
        assertTrue(description.contains("218 Prozent"));
    }

    @Test public void describesMeasuredBucketInEnglishForEnglishAppLocale() {
        BatteryHistoryStats.Bucket bucket = new BatteryHistoryStats.Bucket(0L, "Jan.");
        bucket.chargedMah = 1234;
        bucket.consumedMah = 567;
        bucket.measuredIntervals = 3;
        bucket.wearCycles = .28f;
        bucket.chargeConsumptionRatioPercent = 218;

        String description = BatteryHistoryBucketAccessibility.description(
                bucket, 30, true, Locale.US);

        assertTrue(description.contains("January 1970"));
        assertTrue(description.contains("Charged 1234 mAh"));
        assertTrue(description.contains("Used 567 mAh"));
        assertTrue(description.contains("0.28 EFC"));
        assertTrue(description.contains("218%"));
    }

    @Test public void missingIntervalsAreNotReadAsZeroAndCapacityIsExplained() {
        BatteryHistoryStats.Bucket missing = new BatteryHistoryStats.Bucket(0L, "Jan.");
        String missingDescription = BatteryHistoryBucketAccessibility.description(
                missing, 30, true);
        assertTrue(missingDescription.contains("Keine auswertbaren Strommessungen"));
        assertTrue(missingDescription.contains("nicht null"));
        assertFalse(missingDescription.contains("Aufgeladen 0 mAh"));

        BatteryHistoryStats.Bucket withoutCapacity = new BatteryHistoryStats.Bucket(0L, "Jan.");
        withoutCapacity.measuredIntervals = 1;
        String description = BatteryHistoryBucketAccessibility.description(
                withoutCapacity, 30, false);
        assertTrue(description.contains("Nennkapazität fehlt"));
        assertTrue(description.contains("kein Verbrauchswert"));
    }
}
