package com.ampere.batterylab;

/** Single source of truth for the drawn header action hitboxes. */
final class BatteryHeaderLayout {
    static final int NONE = 0;
    static final int OVERFLOW = 1;
    static final int LIVE_REFRESH = 2;

    private BatteryHeaderLayout() { }

    static int actionAt(float x, float y, float width) {
        if (width < 390f) {
            if (y < 18f || y >= 54f) return NONE;
            if (x >= width - 60f && x < width - 12f) return OVERFLOW;
            return NONE;
        }
        if (y >= 18f && y < 54f) {
            if (x >= width - 140f && x < width - 92f) return OVERFLOW;
        }
        if (y >= 20f && y < 52f && x >= width - 80f && x < width - 16f) return LIVE_REFRESH;
        return NONE;
    }
}
