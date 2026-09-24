package com.ampere.batterylab;

import java.util.ArrayList;
import java.util.Locale;

/** Builds visible scale labels for the independently normalized history series. */
final class BatteryHistoryChartLegend {
    private BatteryHistoryChartLegend() { }

    static String[] labels(ArrayList<BatteryHistoryStats.Bucket> buckets) {
        return labels(buckets, Locale.GERMANY);
    }

    static String[] labels(ArrayList<BatteryHistoryStats.Bucket> buckets, Locale locale) {
        int maxChargedMah = 0;
        int maxConsumedMah = 0;
        float maxWear = 0f;
        int maxRatio = 0;
        boolean hasConsumption = false;
        if (buckets != null) {
            for (BatteryHistoryStats.Bucket bucket : buckets) {
                maxChargedMah = Math.max(maxChargedMah, bucket.chargedMah);
                maxConsumedMah = Math.max(maxConsumedMah, bucket.consumedMah);
                maxWear = Math.max(maxWear, bucket.wearCycles);
                maxRatio = Math.max(maxRatio, bucket.chargeConsumptionRatioPercent);
                hasConsumption |= bucket.consumedMah > 0;
            }
        }
        String chargedScale = maxChargedMah > 0 ? "max " + maxChargedMah + " mAh" : "max —";
        String consumedScale = maxConsumedMah > 0 ? "max " + maxConsumedMah + " mAh" : "max —";
        String wearScale = maxWear > 0f
                ? String.format(locale == null ? Locale.GERMANY : locale,
                        "max %.2f EFC", maxWear) : "max —";
        String ratioScale = hasConsumption ? "max " + maxRatio + "%" : "max —";
        return new String[]{"Geladen · " + chargedScale, "Verbrauch · " + consumedScale,
                "Verschleiß · " + wearScale, "Ladequote · " + ratioScale};
    }
}
