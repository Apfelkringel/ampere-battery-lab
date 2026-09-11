package com.ampere.batterylab;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

/** Compact local daily history for reported cycle counts or the EFC fallback. */
final class BatteryCycleHistory {
    static final int MAX_POINTS = 90;
    static final String REPORTED = "reported";
    static final String ESTIMATED = "estimated";

    private BatteryCycleHistory() { }

    static final class Point {
        final String date;
        final float cycles;
        final String source;

        Point(String date, float cycles, String source) {
            this.date = date;
            this.cycles = cycles;
            this.source = source;
        }

        boolean isReported() { return REPORTED.equals(source); }
    }

    static ArrayList<Point> parse(String serialized) {
        ArrayList<Point> points = new ArrayList<>();
        if (serialized == null || serialized.isEmpty()) return points;
        for (String entry : serialized.split(";", -1)) {
            String[] parts = entry.split("\\|", -1);
            if (parts.length != 3 || !isDate(parts[0])) continue;
            try {
                float cycles = Float.parseFloat(parts[1]);
                String source = normalizeSource(parts[2]);
                if (!isValidCycles(cycles) || source == null) continue;
                merge(points, new Point(parts[0], cycles, source));
            } catch (NumberFormatException ignored) { }
        }
        sortAndTrim(points);
        return points;
    }

    static String record(String serialized, String date, float cycles, String source) {
        ArrayList<Point> points = parse(serialized);
        String normalizedSource = normalizeSource(source);
        if (!isDate(date) || !isValidCycles(cycles) || normalizedSource == null) {
            return serialize(points);
        }
        merge(points, new Point(date, cycles, normalizedSource));
        sortAndTrim(points);
        return serialize(points);
    }

    static String serialize(ArrayList<Point> points) {
        if (points == null || points.isEmpty()) return "";
        ArrayList<Point> clean = new ArrayList<>();
        for (Point point : points) {
            if (point == null || !isDate(point.date) || !isValidCycles(point.cycles)
                    || normalizeSource(point.source) == null) continue;
            merge(clean, new Point(point.date, point.cycles, normalizeSource(point.source)));
        }
        sortAndTrim(clean);
        StringBuilder output = new StringBuilder();
        for (Point point : clean) {
            if (output.length() > 0) output.append(';');
            output.append(point.date).append('|')
                    .append(String.format(Locale.US, "%.3f", point.cycles)).append('|')
                    .append(point.source);
        }
        return output.toString();
    }

    private static void merge(ArrayList<Point> points, Point incoming) {
        for (int i = 0; i < points.size(); i++) {
            Point existing = points.get(i);
            if (!existing.date.equals(incoming.date)) continue;
            // A real BMS/system reading is stronger than an estimate recorded
            // earlier on the same day. Otherwise never move a daily counter
            // backwards because of a reboot, restore or OEM recalibration.
            if (incoming.isReported() && !existing.isReported()) {
                points.set(i, incoming);
            } else if (incoming.source.equals(existing.source) && incoming.cycles > existing.cycles) {
                points.set(i, incoming);
            }
            return;
        }
        points.add(incoming);
    }

    private static void sortAndTrim(ArrayList<Point> points) {
        Collections.sort(points, new Comparator<Point>() {
            @Override public int compare(Point left, Point right) {
                return left.date.compareTo(right.date);
            }
        });
        while (points.size() > MAX_POINTS) points.remove(0);
    }

    private static boolean isDate(String value) {
        if (value == null || !value.matches("\\d{4}-\\d{2}-\\d{2}")) return false;
        try {
            int month = Integer.parseInt(value.substring(5, 7));
            int day = Integer.parseInt(value.substring(8, 10));
            return month >= 1 && month <= 12 && day >= 1 && day <= 31;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private static boolean isValidCycles(float cycles) {
        return Float.isFinite(cycles) && cycles >= 0f && cycles <= 100000f;
    }

    private static String normalizeSource(String source) {
        return REPORTED.equals(source) || ESTIMATED.equals(source) ? source : null;
    }
}
