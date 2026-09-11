package com.ampere.batterylab;

/** Keeps malformed legacy telemetry from aborting a user-requested export. */
final class BatteryExportRules {
    private BatteryExportRules() { }

    static boolean isValidTelemetry(String[] parts) {
        if (parts == null || parts.length < 11) return false;
        try {
            long timestamp = Long.parseLong(parts[0].trim());
            int level = Integer.parseInt(parts[1].trim());
            int current = Integer.parseInt(parts[3].trim());
            double temperature = Double.parseDouble(parts[4].trim());
            double voltage = Double.parseDouble(parts[5].trim());
            int chargeCounter = Integer.parseInt(parts[6].trim());
            int systemCycles = Integer.parseInt(parts[9].trim());
            int plugged = Integer.parseInt(parts[10].trim());
            if (timestamp <= 0L || level < 0 || level > 100
                    || Math.abs((long) current) > 100_000L
                    || !Double.isFinite(temperature) || temperature < 0d || temperature > 100d
                    || !Double.isFinite(voltage) || voltage < 0d || voltage > 10d
                    || chargeCounter < 0 || chargeCounter > 30_000
                    || systemCycles < -1 || systemCycles > 100_000
                    || plugged < 0 || plugged > 32) return false;
            return ("0".equals(parts[2]) || "1".equals(parts[2]))
                    && ("0".equals(parts[7]) || "1".equals(parts[7]));
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    static Integer nonNegativeInt(String value) {
        try {
            int parsed = Integer.parseInt(value == null ? "" : value.trim());
            return parsed >= 0 ? parsed : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
