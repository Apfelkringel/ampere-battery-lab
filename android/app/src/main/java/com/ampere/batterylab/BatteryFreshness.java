package com.ampere.batterylab;

import java.util.Locale;

/** Human-readable age for a live sensor reading. */
final class BatteryFreshness {
    private BatteryFreshness() { }

    static String label(long measuredAtMs, long nowMs) {
        return label(measuredAtMs, nowMs, Locale.GERMANY);
    }

    static String label(long measuredAtMs, long nowMs, Locale locale) {
        boolean english = locale != null
                && Locale.ENGLISH.getLanguage().equals(locale.getLanguage());
        if (measuredAtMs <= 0L || nowMs < measuredAtMs) {
            return english ? "Measurement time unknown" : "Messzeit unbekannt";
        }
        long seconds = (nowMs - measuredAtMs) / 1000L;
        if (seconds < 3L) return english ? "just now" : "gerade eben";
        if (seconds < 60L) return english ? seconds + " sec ago" : "vor " + seconds + " Sek.";
        long minutes = seconds / 60L;
        if (english) return minutes == 1L ? "1 min ago" : minutes + " min ago";
        return minutes == 1L ? "vor 1 Min." : "vor " + minutes + " Min.";
    }
}
