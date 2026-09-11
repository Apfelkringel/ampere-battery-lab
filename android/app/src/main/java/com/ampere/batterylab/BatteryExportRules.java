package com.ampere.batterylab;

import java.util.ArrayList;

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
            if (!("0".equals(parts[2]) || "1".equals(parts[2]))) return false;
            if (!("0".equals(parts[7]) || "1".equals(parts[7]))) return false;
            // The monitor stores charging current as positive and discharge
            // current as negative. Keep contradictory legacy rows out of
            // exports instead of presenting a reversed measurement as fact.
            if (current != 0 && (("1".equals(parts[2]) && current < 0)
                    || ("0".equals(parts[2]) && current > 0))) return false;
            return true;
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

    /** Returns only complete, physically consistent telemetry rows for CSV/JSON export. */
    static ArrayList<String> validTelemetryRows(String serialized) {
        ArrayList<String> rows = new ArrayList<>();
        if (serialized == null || serialized.isEmpty()) return rows;
        for (String row : serialized.split("\\n", -1)) {
            if (!row.trim().isEmpty() && isValidTelemetry(row.split(",", -1))) rows.add(row);
        }
        return rows;
    }
}
