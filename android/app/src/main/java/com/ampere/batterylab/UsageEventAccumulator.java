package com.ampere.batterylab;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Activity-aware interval accounting for Android foreground usage events. */
final class UsageEventAccumulator {
    static final class State {
        final Set<String> activeClasses = new HashSet<>();
        long sessionStart;

        State(long sessionStart) {
            this.sessionStart = sessionStart;
        }
    }

    private UsageEventAccumulator() { }

    static void apply(Map<String, Long> totals, Map<String, State> active,
                      String packageName, String className, long timestamp,
                      long start, long end, boolean foreground, boolean background) {
        if (packageName == null || packageName.isEmpty() || timestamp > end) return;
        if (foreground) {
            State state = active.get(packageName);
            if (state == null) {
                state = new State(Math.max(start, timestamp));
                active.put(packageName, state);
            }
            if (state.activeClasses.isEmpty()) state.sessionStart = Math.max(start, timestamp);
            state.activeClasses.add(normalizeClassName(className));
        } else if (background) {
            State state = active.get(packageName);
            if (state == null) return;
            // MOVE_TO_BACKGROUND has no reliable activity class and closes the
            // package as a whole. Activity PAUSED/STOPPED only closes its own
            // class; duplicate close events are therefore harmless.
            if (className == null || className.isEmpty()) {
                close(totals, active, packageName, state, Math.min(end, timestamp));
            } else if (state.activeClasses.remove(className) && state.activeClasses.isEmpty()) {
                close(totals, active, packageName, state, Math.min(end, timestamp));
            }
        }
    }

    static void closeAll(Map<String, Long> totals, Map<String, State> active, long timestamp) {
        for (Map.Entry<String, State> entry : new java.util.ArrayList<>(active.entrySet())) {
            close(totals, active, entry.getKey(), entry.getValue(), timestamp);
        }
    }

    static void closeActive(Map<String, Long> totals, Map<String, State> active, long end) {
        for (Map.Entry<String, State> entry : new java.util.ArrayList<>(active.entrySet())) {
            close(totals, active, entry.getKey(), entry.getValue(), end);
        }
    }

    private static String normalizeClassName(String className) {
        return className == null || className.isEmpty() ? "\u0000package" : className;
    }

    private static void close(Map<String, Long> totals, Map<String, State> active,
                              String packageName, State state, long end) {
        add(totals, packageName, state.sessionStart, end);
        active.remove(packageName);
    }

    private static void add(Map<String, Long> totals, String packageName, long start, long end) {
        if (end <= start) return;
        totals.put(packageName, totals.containsKey(packageName)
                ? totals.get(packageName) + end - start : end - start);
    }
}
