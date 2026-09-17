package com.ampere.batterylab;

/** Human-readable age for a live sensor reading. */
final class BatteryFreshness {
    private BatteryFreshness() { }

    static String label(long measuredAtMs, long nowMs) {
        if (measuredAtMs <= 0L || nowMs < measuredAtMs) return "Messzeit unbekannt";
        long seconds = (nowMs - measuredAtMs) / 1000L;
        if (seconds < 3L) return "gerade eben";
        if (seconds < 60L) return "vor " + seconds + " Sek.";
        long minutes = seconds / 60L;
        return minutes == 1L ? "vor 1 Min." : "vor " + minutes + " Min.";
    }
}
