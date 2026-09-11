package com.ampere.batterylab;

/** Formats durations for compact metric cards without truncating meaningful data. */
final class BatteryDuration {
    private BatteryDuration() { }

    static String compact(long minutes) {
        if (minutes <= 0L) return "—";
        long hours = minutes / 60L;
        long remainingMinutes = minutes % 60L;
        if (hours <= 0L) return minutes + " m";
        return remainingMinutes == 0L
                ? hours + " h"
                : hours + " h " + remainingMinutes + " m";
    }
}
