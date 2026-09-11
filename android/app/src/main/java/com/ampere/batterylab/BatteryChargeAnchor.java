package com.ampere.batterylab;

/** Pure rules for the local "since charge" baseline. */
final class BatteryChargeAnchor {
    static final String FULL = "full";
    static final String UNPLUGGED = "unplugged";

    private BatteryChargeAnchor() { }

    static final class State {
        final long anchorAt;
        final int anchorLevel;
        final String anchorType;
        final boolean fullReachedThisPlug;

        State(long anchorAt, int anchorLevel, String anchorType, boolean fullReachedThisPlug) {
            this.anchorAt = anchorAt;
            this.anchorLevel = anchorLevel;
            this.anchorType = normalizeType(anchorType);
            this.fullReachedThisPlug = fullReachedThisPlug;
        }

        boolean hasAnchor() { return anchorAt > 0L && anchorLevel >= 0 && !anchorType.isEmpty(); }
    }

    static State onPowerConnected(State state) {
        return new State(state.anchorAt, state.anchorLevel, state.anchorType, false);
    }

    static State onBatteryChanged(State state, int level, boolean plugged, boolean full, long now) {
        if (!plugged || !full || state.fullReachedThisPlug || level < 0 || now <= 0L) return state;
        return new State(now, level, FULL, true);
    }

    static State onPowerDisconnected(State state, int level, long now) {
        if (state.fullReachedThisPlug || level < 0 || now <= 0L) {
            return new State(state.anchorAt, state.anchorLevel, state.anchorType, false);
        }
        return new State(now, level, UNPLUGGED, false);
    }

    static boolean sameAnchor(State left, State right) {
        return left.anchorAt == right.anchorAt
                && left.anchorLevel == right.anchorLevel
                && left.anchorType.equals(right.anchorType);
    }

    private static String normalizeType(String type) {
        return FULL.equals(type) || UNPLUGGED.equals(type) ? type : "";
    }
}
