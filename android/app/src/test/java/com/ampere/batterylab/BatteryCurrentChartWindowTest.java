package com.ampere.batterylab;

import org.junit.Test;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** String-formatting guards for the live current-chart window label. */
public class BatteryCurrentChartWindowTest {

    private static long at(int year, int month, int day, int hour, int minute) {
        Calendar calendar = Calendar.getInstance(TimeZone.getDefault(), Locale.GERMANY);
        calendar.set(year, month - 1, day, hour, minute, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    @Test public void emptySampleCountAlwaysReadsNoReadings() {
        assertEquals("Keine Messwerte",
                BatteryCurrentChartWindow.label(at(2026, 9, 18, 10, 0), at(2026, 9, 18, 11, 0), 0));
    }

    @Test public void singleSampleShowsSnapshotLabel() {
        String label = BatteryCurrentChartWindow.label(
                at(2026, 9, 18, 10, 0), at(2026, 9, 18, 10, 0), 1);
        // "Momentaufnahme" followed by time and "1 Messwert" (singular).
        assertTrue("expected 'Momentaufnahme' in: " + label, label.contains("Momentaufnahme"));
        assertTrue("expected singular count in: " + label, label.contains("1 Messwert"));
        assertTrue("did not expect plural form in: " + label, !label.contains("Messwerte"));
    }

    @Test public void multipleSamplesOnSameDayUseTimeOnlyPattern() {
        String label = BatteryCurrentChartWindow.label(
                at(2026, 9, 18, 8, 15), at(2026, 9, 18, 8, 45), 3);
        // Same-day window: HH:mm pattern, range "HH:mm–HH:mm", duration in minutes.
        assertTrue("expected same-day time range in: " + label, label.contains("08:15"));
        assertTrue("expected end time in: " + label, label.contains("08:45"));
        assertTrue("expected duration in: " + label, label.contains("Min."));
        assertTrue("expected plural sample count in: " + label, label.contains("Messwerte"));
    }

    @Test public void crossDayWindowIncludesDatePrefix() {
        String label = BatteryCurrentChartWindow.label(
                at(2026, 9, 18, 23, 30), at(2026, 9, 19, 1, 0), 4);
        // Cross-day uses dd.MM. HH:mm pattern, so both start and end include
        // a day and month prefix.
        assertTrue("expected cross-day date prefix in: " + label, label.contains("18.09."));
        assertTrue("expected second date prefix in: " + label, label.contains("19.09."));
    }

    @Test public void roundedHourDurationHasNoTrailingZero() {
        // 90 minutes renders as "1 h 30 Min."
        String label = BatteryCurrentChartWindow.label(
                at(2026, 9, 18, 8, 0), at(2026, 9, 18, 9, 30), 5);
        assertTrue("expected hours-and-minutes duration in: " + label, label.contains("1 h 30 Min."));
    }

    @Test public void exactlyOneHourDurationHasNoMinutesTail() {
        String label = BatteryCurrentChartWindow.label(
                at(2026, 9, 18, 8, 0), at(2026, 9, 18, 9, 0), 3);
        assertTrue("expected bare hour duration in: " + label, label.contains("1 h"));
        assertTrue("did not expect trailing minutes in: " + label, !label.contains("Min."));
    }

    @Test public void subMinuteDurationClampsToAtLeastOneMinute() {
        // A two-second window must still read at least "1 Min." so the label
        // never claims a zero-length interval.
        long start = at(2026, 9, 18, 8, 0);
        long end = start + 2000L;
        String label = BatteryCurrentChartWindow.label(start, end, 2);
        assertTrue("expected at least 1 Min. in: " + label, label.contains("1 Min."));
    }

    @Test public void invertedRangeStillReportsSampleCount() {
        // If end <= start (clock skew), fall back to the momentaufnahme shape
        // but keep the sample count visible.
        String label = BatteryCurrentChartWindow.label(
                at(2026, 9, 18, 9, 0), at(2026, 9, 18, 8, 30), 2);
        assertTrue("expected momentaufnahme fallback in: " + label, label.contains("Momentaufnahme"));
        assertTrue("expected sample count in: " + label, label.contains("Messwerte"));
    }

    @Test public void englishWindowUsesEnglishDatesDurationsAndReadingCounts() {
        String label = BatteryCurrentChartWindow.label(
                at(2026, 9, 18, 10, 0), at(2026, 9, 18, 11, 0), 2, Locale.US);
        assertTrue(label, label.contains("10:00 AM–11:00 AM"));
        assertTrue(label, label.contains("1 hr"));
        assertTrue(label, label.contains("2 readings"));
        assertTrue(BatteryCurrentChartWindow.label(0L, 0L, 0, Locale.US)
                .contains("No measurements"));
    }
}
