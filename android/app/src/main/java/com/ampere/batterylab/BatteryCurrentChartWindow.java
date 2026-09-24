package com.ampere.batterylab;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** Describes the exact time span represented by the samples in a current chart. */
final class BatteryCurrentChartWindow {
    private BatteryCurrentChartWindow() { }

    static String label(long startMs, long endMs, int sampleCount) {
        return label(startMs, endMs, sampleCount, Locale.GERMANY);
    }

    static String label(long startMs, long endMs, int sampleCount, Locale locale) {
        Locale safeLocale = locale == null ? Locale.GERMANY : locale;
        boolean english = Locale.ENGLISH.getLanguage().equals(safeLocale.getLanguage());
        if (sampleCount <= 0) return english ? "No measurements" : "Keine Messwerte";
        String pattern = sameLocalDay(startMs, endMs)
                ? (english ? "h:mm a" : "HH:mm")
                : (english ? "M/d h:mm a" : "dd.MM. HH:mm");
        SimpleDateFormat time = new SimpleDateFormat(pattern, safeLocale);
        String start = time.format(new Date(startMs));
        String end = time.format(new Date(endMs));
        String count = sampleCount + (english
                ? (sampleCount == 1 ? " reading" : " readings")
                : (sampleCount == 1 ? " Messwert" : " Messwerte"));
        if (sampleCount == 1 || endMs <= startMs) {
            return (english ? "Snapshot · " : "Momentaufnahme · ") + start + " · " + count;
        }
        long minutes = Math.max(1L, Math.round((endMs - startMs) / 60000.0));
        String duration;
        if (english) {
            duration = minutes < 60L ? minutes + " min"
                    : (minutes / 60L) + " hr" + (minutes % 60L == 0L
                    ? "" : " " + (minutes % 60L) + " min");
        } else {
            duration = minutes < 60L ? minutes + " Min."
                    : (minutes / 60L) + " h" + (minutes % 60L == 0L
                    ? "" : " " + (minutes % 60L) + " Min.");
        }
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
