package com.ampere.batterylab;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Keeps cable/status blips and malformed legacy rows out of session history. */
final class BatterySessionRules {
    private static final long MIN_SINGLE_PERCENT_SESSION_MINUTES = 5L;
    private static final Pattern LEGACY_ENGLISH_DATE_TIME = Pattern.compile(
            "\\d{1,2}:\\d{2} (?:AM|PM)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SERIALIZED_DURATION = Pattern.compile(
            "(?:(\\d+)\\s*(?:Std\\.|hr)\\s+)?(\\d+)\\s*(?:Min\\.|min)",
            Pattern.CASE_INSENSITIVE);

    private BatterySessionRules() { }

    static String datePattern(boolean english) {
        return english ? "MMM d · h:mm a" : "dd.MM. HH:mm";
    }

    /**
     * Resolves a session's direction without trusting a noisy level snapshot
     * over a measured charge-counter delta. A zero result means there is not
     * enough evidence for a history row.
     */
    static int effectiveChange(int observedChange, int energyMah, int designMah, boolean charging) {
        if (charging && observedChange >= 1 && observedChange <= 100) return observedChange;
        if (!charging && observedChange >= -100 && observedChange <= -1) return observedChange;
        if (energyMah <= 0 || designMah <= 0) return 0;
        long inferred = Math.round(energyMah * 100d / designMah);
        if (inferred < 1L || inferred > 100L) return 0;
        return charging ? (int) inferred : -(int) inferred;
    }

    /**
     * Filters cable/status blips that have no measured energy and last only a
     * moment. Larger level changes, measured energy, and long slow 1-% changes
     * remain valid sessions.
     */
    static boolean shouldRecord(int effectiveChange, int energyMah, long durationMinutes) {
        if (effectiveChange == 0 || energyMah < 0 || durationMinutes <= 0L) return false;
        return Math.abs(effectiveChange) >= 2
                || energyMah > 0
                || durationMinutes >= MIN_SINGLE_PERCENT_SESSION_MINUTES;
    }

    /**
     * Rejects a counter reset or unit error before it becomes session energy.
     * The stored EFC format intentionally allows at most three full cycles per
     * continuous phase; zero means unavailable and lets the percentage delta
     * remain useful without inventing an mAh value.
     */
    static int normalizeEnergy(int energyMah, int designMah) {
        if (energyMah < 0) return 0;
        long maximum = designMah > 0
                ? Math.min(30_000L, designMah * 3L)
                : 30_000L;
        return energyMah <= maximum ? energyMah : 0;
    }

    static boolean isValid(String value) {
        if (value == null || value.isEmpty()) return false;
        String[] parts = value.split(",", -1);
        // Four fields are the oldest supported format: type, change, duration, date.
        if (parts.length < 4 || parts[3].trim().isEmpty() || durationMinutes(parts[2]) <= 0L) return false;
        String type = parts[0].trim();
        if (!"Charge".equals(type) && !"Discharge".equals(type)) return false;
        try {
            int change = Integer.parseInt(parts[1].replace("%", "").replace("+", "").trim());
            if (change < -100 || change > 100) return false;
            // Extended rows contain an EFC value used by the health chart.
            // Float.parseFloat accepts NaN and Infinity, so explicitly reject
            // both and keep impossible chart scales out of the UI.
            if (parts.length >= 6) {
                if (!isValidLevel(parts[4]) || !isValidLevel(parts[5])) return false;
                // Older builds could persist a row after a noisy status
                // transition with the start/end levels reversed. The row is
                // individually numeric, but its physical direction is
                // impossible: charging cannot finish lower and discharging
                // cannot finish higher. Drop only that contradictory row;
                // valid historical entries remain untouched.
                if (!matchesDirection(type, parts[4], parts[5])) return false;
            }
            if (parts.length >= 7 && !isNonNegativeInt(parts[6])) return false;
            if (parts.length >= 8 && !isValidEquivalentCycles(parts[7])) return false;
            if (parts.length >= 10
                    && (!isValidScreenValue(parts[8], "Discharge".equals(type))
                    || !isValidScreenValue(parts[9], "Discharge".equals(type)))) return false;
            if (parts.length >= 13
                    && (!isNonNegativeInt(parts[10]) || !isNonNegativeInt(parts[11])
                    || !isNonNegativeInt(parts[12]))) return false;
            if (parts.length >= 16) {
                long startedAt = positiveLong(parts[14]);
                long endedAt = positiveLong(parts[15]);
                if (startedAt <= 0L || endedAt < startedAt) return false;
            }
            if (parts.length >= 17 && !isNonNegativeInt(parts[16])) return false;
            return "Charge".equals(type) ? change > 0 : change < 0;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    /** Keeps only supported session rows in their original order. */
    static String normalizeSerialized(String serialized) {
        StringBuilder normalized = new StringBuilder();
        if (serialized == null || serialized.isEmpty()) return "";
        for (String savedValue : serialized.split("\\|", -1)) {
            String value = normalizeLegacyEnglishDate(savedValue);
            if (value.isEmpty() || !isValid(value)) continue;
            if (normalized.length() > 0) normalized.append('|');
            normalized.append(value);
        }
        return normalized.toString();
    }

    /** Repairs rows written with a comma inside the old English date format. */
    private static String normalizeLegacyEnglishDate(String value) {
        String[] parts = value.split(",", -1);
        if (!(parts.length == 5 || parts.length >= 18)
                || !LEGACY_ENGLISH_DATE_TIME.matcher(parts[4].trim()).matches()) return value;
        StringBuilder normalized = new StringBuilder(value.length());
        for (int i = 0; i < parts.length; i++) {
            if (i == 4) continue;
            if (normalized.length() > 0) normalized.append(',');
            normalized.append(i == 3 ? parts[3] + " · " + parts[4].trim() : parts[i]);
        }
        return normalized.toString();
    }

    static boolean isValidEquivalentCycles(String value) {
        try {
            float parsed = Float.parseFloat(value == null ? "" : value.trim());
            return Float.isFinite(parsed) && parsed >= 0f && parsed <= 3f;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private static boolean isValidLevel(String value) {
        try {
            return BatteryLevel.normalizePercent(Integer.parseInt(value.trim())) >= 0;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean matchesDirection(String type, String startValue, String endValue) {
        try {
            int start = Integer.parseInt(startValue.trim());
            int end = Integer.parseInt(endValue.trim());
            return "Charge".equals(type) ? end >= start : end <= start;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean isValidScreenValue(String value, boolean discharge) {
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed >= 0 && (!discharge || parsed <= 1000);
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean isNonNegativeInt(String value) {
        try {
            return Integer.parseInt(value.trim()) >= 0;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static long positiveLong(String value) {
        try {
            long parsed = Long.parseLong(value == null ? "" : value.trim());
            return parsed > 0L ? parsed : -1L;
        } catch (Exception ignored) {
            return -1L;
        }
    }

    /** Parses the exact human-readable duration written by the monitor. */
    private static long durationMinutes(String value) {
        if (value == null) return -1L;
        Matcher matcher = SERIALIZED_DURATION.matcher(value.trim());
        if (!matcher.matches()) return -1L;
        try {
            boolean hasHours = matcher.group(1) != null;
            long hours = hasHours ? Long.parseLong(matcher.group(1)) : 0L;
            long minutes = Long.parseLong(matcher.group(2));
            if ((hasHours && hours < 1L) || minutes < 0L || (hasHours && minutes > 59L)
                    || hours > (Long.MAX_VALUE - minutes) / 60L) return -1L;
            long totalMinutes = hours * 60L + minutes;
            return totalMinutes > 0L ? totalMinutes : -1L;
        } catch (Exception ignored) {
            return -1L;
        }
    }
}
