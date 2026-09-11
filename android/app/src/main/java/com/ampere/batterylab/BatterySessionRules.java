package com.ampere.batterylab;

/** Keeps cable/status blips and malformed legacy rows out of session history. */
final class BatterySessionRules {
    private BatterySessionRules() { }

    /**
     * Resolves a session's direction without trusting a noisy level snapshot
     * over a measured charge-counter delta. A zero result means there is not
     * enough evidence for a history row.
     */
    static int effectiveChange(int observedChange, int energyMah, int designMah, boolean charging) {
        if (charging && observedChange > 0) return observedChange;
        if (!charging && observedChange < 0) return observedChange;
        if (energyMah <= 0 || designMah <= 0) return 0;
        long inferred = Math.round(energyMah * 100d / designMah);
        if (inferred < 1L || inferred > 100L) return 0;
        return charging ? (int) inferred : -(int) inferred;
    }

    static boolean isValid(String value) {
        if (value == null || value.isEmpty()) return false;
        String[] parts = value.split(",", -1);
        // Four fields are the oldest supported format: type, change, duration, date.
        if (parts.length < 4 || parts[2].trim().isEmpty() || parts[3].trim().isEmpty()) return false;
        String type = parts[0].trim();
        if (!"Charge".equals(type) && !"Discharge".equals(type)) return false;
        try {
            int change = Integer.parseInt(parts[1].replace("%", "").replace("+", "").trim());
            return "Charge".equals(type) ? change > 0 : change < 0;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }
}
