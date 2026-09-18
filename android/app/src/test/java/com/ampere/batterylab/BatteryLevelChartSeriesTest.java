package com.ampere.batterylab;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Regression guards for the long-range battery chart's per-day grouping and
 * missing-day gap detection.
 */
public class BatteryLevelChartSeriesTest {

    private static BatteryLevelChartSeries.Point at(int year, int month, int day, int hour, int minute, int level) {
        Calendar calendar = Calendar.getInstance(TimeZone.getDefault(), Locale.GERMANY);
        calendar.set(year, month - 1, day, hour, minute, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return new BatteryLevelChartSeries.Point(calendar.getTimeInMillis(), level);
    }

    @Test public void emptyInputProducesEmptyResult() {
        assertEquals(0, BatteryLevelChartSeries.dailyLastSamples(new ArrayList<>(), TimeZone.getDefault()).size());
    }

    @Test public void nullInputProducesEmptyResult() {
        assertEquals(0, BatteryLevelChartSeries.dailyLastSamples(null, TimeZone.getDefault()).size());
    }

    @Test public void singleSampleSurvives() {
        ArrayList<BatteryLevelChartSeries.Point> samples = new ArrayList<>();
        samples.add(at(2026, 9, 18, 10, 0, 80));
        ArrayList<BatteryLevelChartSeries.Point> daily = BatteryLevelChartSeries.dailyLastSamples(samples, TimeZone.getDefault());
        assertEquals(1, daily.size());
        assertEquals(80, daily.get(0).level);
    }

    @Test public void multipleSamplesOnSameDayKeepLatest() {
        ArrayList<BatteryLevelChartSeries.Point> samples = new ArrayList<>();
        samples.add(at(2026, 9, 18, 8, 0, 90));
        samples.add(at(2026, 9, 18, 12, 0, 70));
        samples.add(at(2026, 9, 18, 18, 0, 50));
        ArrayList<BatteryLevelChartSeries.Point> daily = BatteryLevelChartSeries.dailyLastSamples(samples, TimeZone.getDefault());
        assertEquals(1, daily.size());
        assertEquals(50, daily.get(0).level);
    }

    @Test public void samplesAcrossTwoDaysKeepOnePerDay() {
        ArrayList<BatteryLevelChartSeries.Point> samples = new ArrayList<>();
        samples.add(at(2026, 9, 18, 8, 0, 90));
        samples.add(at(2026, 9, 18, 22, 0, 60));
        samples.add(at(2026, 9, 19, 8, 0, 80));
        samples.add(at(2026, 9, 19, 22, 0, 55));
        ArrayList<BatteryLevelChartSeries.Point> daily = BatteryLevelChartSeries.dailyLastSamples(samples, TimeZone.getDefault());
        assertEquals(2, daily.size());
        assertEquals(60, daily.get(0).level);
        assertEquals(55, daily.get(1).level);
    }

    @Test public void unsortedInputIsOrderedBeforeGrouping() {
        ArrayList<BatteryLevelChartSeries.Point> samples = new ArrayList<>();
        samples.add(at(2026, 9, 19, 22, 0, 55));
        samples.add(at(2026, 9, 18, 8, 0, 90));
        samples.add(at(2026, 9, 18, 22, 0, 60));
        ArrayList<BatteryLevelChartSeries.Point> daily = BatteryLevelChartSeries.dailyLastSamples(samples, TimeZone.getDefault());
        assertEquals(2, daily.size());
        assertEquals(60, daily.get(0).level);
        assertEquals(55, daily.get(1).level);
    }

    @Test public void nextCalendarDayReportsGap() {
        long earlier = at(2026, 9, 18, 22, 0, 60).timestamp;
        long later = at(2026, 9, 20, 8, 0, 40).timestamp;
        // Missing the 19th means we crossed two day boundaries past the
        // previous day, so the chart should leave a visible gap.
        assertTrue(BatteryLevelChartSeries.skipsCalendarDay(earlier, later, TimeZone.getDefault()));
    }

    @Test public void consecutiveDaysDoNotReportGap() {
        long earlier = at(2026, 9, 18, 22, 0, 60).timestamp;
        long later = at(2026, 9, 19, 6, 0, 55).timestamp;
        assertEquals(false, BatteryLevelChartSeries.skipsCalendarDay(earlier, later, TimeZone.getDefault()));
    }

    @Test public void sameDayDoesNotReportGap() {
        long earlier = at(2026, 9, 18, 8, 0, 90).timestamp;
        long later = at(2026, 9, 18, 20, 0, 60).timestamp;
        assertEquals(false, BatteryLevelChartSeries.skipsCalendarDay(earlier, later, TimeZone.getDefault()));
    }

    @Test public void invertedOrEqualTimestampsNeverSkip() {
        long t = at(2026, 9, 18, 10, 0, 80).timestamp;
        assertEquals(false, BatteryLevelChartSeries.skipsCalendarDay(t, t, TimeZone.getDefault()));
        assertEquals(false, BatteryLevelChartSeries.skipsCalendarDay(t + 1000L, t, TimeZone.getDefault()));
    }
}
