package com.ampere.batterylab;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Spoken, per-bucket values for the tappable battery-balance chart. */
final class BatteryHistoryBucketAccessibility {
    private BatteryHistoryBucketAccessibility() { }

    static String description(BatteryHistoryStats.Bucket bucket, int periodDays,
                              boolean capacityAvailable) {
        if (bucket == null) return "Zeitraum nicht verfügbar";
        String pattern = periodDays == 1 ? "EEEE, d. MMMM"
                : periodDays == 7 ? "d. MMMM yyyy" : "MMMM yyyy";
        String date = new SimpleDateFormat(pattern, Locale.GERMANY)
                .format(new Date(bucket.start));
        if (bucket.measuredIntervals <= 0) {
            return date + ". Keine auswertbaren Strommessungen. Strich bedeutet fehlende Werte, nicht null.";
        }
        String wear = capacityAvailable
                ? String.format(Locale.GERMANY, "%.2f EFC", bucket.wearCycles)
                : "nicht verfügbar, Nennkapazität fehlt";
        String ratio = bucket.consumedMah > 0
                ? bucket.chargeConsumptionRatioPercent + " Prozent"
                : "nicht verfügbar, kein Verbrauchswert";
        return date + ". Aufgeladen " + bucket.chargedMah + " mAh. Verbrauch "
                + bucket.consumedMah + " mAh. Verschleiß " + wear
                + ". Geladen zu Verbrauch " + ratio + ".";
    }
}
