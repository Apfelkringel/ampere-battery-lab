package com.ampere.batterylab;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** Describes the exact time span represented by the samples in a current chart. */
final class BatteryCurrentChartWindow {
    private BatteryCurrentChartWindow() { }

    static String label(long startMs, long endMs, int sampleCount) {
        if (sampleCount <= 0) return "Keine Messwerte";
        String pattern = sameLocalDay(startMs, endMs) ? "HH:mm" : "dd.MM. HH:mm";
        SimpleDateFormat time = new SimpleDateFormat(pattern, Locale.GERMANY);
        String start = time.format(new Date(startMs));
        String end = time.format(new Date(endMs));
        String count = sampleCount + (sampleCount == 1 ? " Messwert" : " Messwerte");
        if (sampleCount == 1 || endMs <= startMs) {
            return "Momentaufnahme · " + start + " · " + count;
        }
        long minutes = Math.max(1L, Math.round((endMs - startMs) / 60000.0));
        String duration = minutes < 60L
                ? minutes + " Min."
                : (minutes / 60L) + " h" + (minutes % 60L == 0L
                ? "" : " " + (minutes % 60L) + " Min.");
        return start + "–" + end + " · " + duration + " · " + count;
    }

    private static boolean sameLocalDay(long startMs, long endMs) {
        Calendar start = Calendar.getInstance();
        Calendar end = Calendar.getInstance();
        start.setTimeInMillis(startMs);
        end.setTimeInMillis(endMs);
        return start.get(Calendar.ERA) == end.get(Calendar.ERA)
                && start.get(Calendar.YEAR) == end.get(Calendar.YEAR)
                && start.get(Calendar.DAY_OF_YEAR) == end.get(Calendar.DAY_OF_YEAR);
    }
}
