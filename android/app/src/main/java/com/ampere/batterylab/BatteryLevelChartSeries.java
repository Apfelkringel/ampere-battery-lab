package com.ampere.batterylab;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
import java.util.TimeZone;

/** Makes the long-range battery chart readable without inventing readings. */
final class BatteryLevelChartSeries {
    private BatteryLevelChartSeries() { }

    /** Keeps the latest real battery reading from each local calendar day. */
    static ArrayList<Point> dailyLastSamples(ArrayList<Point> samples, TimeZone timeZone) {
        ArrayList<Point> result = new ArrayList<>();
        if (samples == null || samples.isEmpty()) return result;
        ArrayList<Point> ordered = new ArrayList<>(samples);
        Collections.sort(ordered, new Comparator<Point>() {
            @Override public int compare(Point left, Point right) {
                return Long.compare(left.timestamp, right.timestamp);
            }
        });
        Calendar localDay = Calendar.getInstance(
                timeZone == null ? TimeZone.getDefault() : timeZone, Locale.GERMANY);
        long previousDayStart = Long.MIN_VALUE;
        for (Point point : ordered) {
            localDay.setTimeInMillis(point.timestamp);
            localDay.set(Calendar.HOUR_OF_DAY, 0);
            localDay.set(Calendar.MINUTE, 0);
            localDay.set(Calendar.SECOND, 0);
            localDay.set(Calendar.MILLISECOND, 0);
            long dayStart = localDay.getTimeInMillis();
            if (dayStart == previousDayStart) result.set(result.size() - 1, point);
            else {
                result.add(point);
                previousDayStart = dayStart;
            }
        }
        return result;
    }

    /** A missing calendar day is shown as a gap, never joined as measured data. */
    static boolean skipsCalendarDay(long previousTimestamp, long currentTimestamp,
                                    TimeZone timeZone) {
        if (currentTimestamp <= previousTimestamp) return false;
        TimeZone zone = timeZone == null ? TimeZone.getDefault() : timeZone;
        Calendar expected = Calendar.getInstance(zone, Locale.GERMANY);
        expected.setTimeInMillis(previousTimestamp);
        expected.set(Calendar.HOUR_OF_DAY, 0);
        expected.set(Calendar.MINUTE, 0);
        expected.set(Calendar.SECOND, 0);
        expected.set(Calendar.MILLISECOND, 0);
        expected.add(Calendar.DAY_OF_MONTH, 1);
        Calendar actual = Calendar.getInstance(zone, Locale.GERMANY);
        actual.setTimeInMillis(currentTimestamp);
        actual.set(Calendar.HOUR_OF_DAY, 0);
        actual.set(Calendar.MINUTE, 0);
        actual.set(Calendar.SECOND, 0);
        actual.set(Calendar.MILLISECOND, 0);
        return actual.getTimeInMillis() > expected.getTimeInMillis();
    }

    static final class Point {
        final long timestamp;
        final int level;

        Point(long timestamp, int level) {
            this.timestamp = timestamp;
            this.level = level;
        }
    }
}
