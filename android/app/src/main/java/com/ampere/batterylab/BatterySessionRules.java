package com.ampere.batterylab;

/** Keeps cable/status blips and malformed legacy rows out of session history. */
final class BatterySessionRules {
    private BatterySessionRules() { }

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
