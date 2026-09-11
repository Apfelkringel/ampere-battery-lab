package com.ampere.batterylab;

/** Pure presentation rules for a charge target; keeps labels and geometry honest. */
final class BatteryChargePresentation {
    private BatteryChargePresentation() { }

    static boolean targetReached(int levelPercent, int targetPercent) {
        return levelPercent >= 0 && targetPercent > 0 && levelPercent >= targetPercent;
    }

    static float progressToTarget(int levelPercent, int targetPercent) {
        if (levelPercent < 0 || targetPercent <= 0) return 0f;
        return Math.max(0f, Math.min(1f, levelPercent / (float) targetPercent));
    }

    static String status(int levelPercent, int targetPercent, boolean charging, String timeToTarget) {
        if (levelPercent < 0) return "Akkustand wird ermittelt";
        if (targetReached(levelPercent, targetPercent)) {
            return charging ? "Ladeziel erreicht · Gerät lädt weiter" : "Ladeziel erreicht";
        }
        if (!charging) return "Nicht mit Strom verbunden";
        if (timeToTarget == null || timeToTarget.trim().isEmpty() || "—".equals(timeToTarget)) {
            return "Lädt bis " + targetPercent + "%";
        }
        return timeToTarget + " bis " + targetPercent + "%";
    }
}
