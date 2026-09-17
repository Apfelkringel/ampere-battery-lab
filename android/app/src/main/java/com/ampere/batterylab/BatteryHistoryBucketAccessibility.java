package com.ampere.batterylab;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Spoken, per-bucket values for the tappable battery-balance chart. */
final class BatteryHistoryBucketAccessibility {
    private BatteryHistoryBucketAccessibility() { }

    static String description(BatteryHistoryStats.Bucket bucket, int periodDays,
                              boolean capacityAvailable) {
        return description(bucket, periodDays, capacityAvailable, Locale.GERMANY);
    }

    static String description(BatteryHistoryStats.Bucket bucket, int periodDays,
                              boolean capacityAvailable, Locale locale) {
        if (bucket == null) return "Zeitraum nicht verfügbar";
        boolean english = Locale.ENGLISH.getLanguage().equals(locale.getLanguage());
        String pattern = periodDays == 1 ? "EEEE, d. MMMM"
                : periodDays == 7 ? "d. MMMM yyyy" : "MMMM yyyy";
        String date = new SimpleDateFormat(pattern, locale)
                .format(new Date(bucket.start));
        if (bucket.measuredIntervals <= 0) {
            return english
                    ? date + ". No usable current measurements. A dash means missing values, not zero."
                    : date + ". Keine auswertbaren Strommessungen. Strich bedeutet fehlende Werte, nicht null.";
        }
        String wear = capacityAvailable
                ? String.format(locale, "%.2f EFC", bucket.wearCycles)
                : (english ? "not available, design capacity missing" : "nicht verfügbar, Nennkapazität fehlt");
        String ratio = bucket.consumedMah > 0
                ? bucket.chargeConsumptionRatioPercent + (english ? "%" : " Prozent")
                : (english ? "not available, no usage value" : "nicht verfügbar, kein Verbrauchswert");
        return english
                ? date + ". Charged " + bucket.chargedMah + " mAh. Used "
                + bucket.consumedMah + " mAh. Wear " + wear
                + ". Charged-to-used ratio " + ratio + "."
                : date + ". Aufgeladen " + bucket.chargedMah + " mAh. Verbrauch "
                + bucket.consumedMah + " mAh. Verschleiß " + wear
                + ". Geladen zu Verbrauch " + ratio + ".";
    }
}
