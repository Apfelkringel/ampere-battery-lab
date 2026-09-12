package com.ampere.batterylab;

/** Single source of truth for the drawn header action hitboxes. */
final class BatteryHeaderLayout {
    static final int NONE = 0;
    static final int OVERFLOW = 1;
    static final int THEME = 2;
    static final int LIVE_REFRESH = 3;

    private BatteryHeaderLayout() { }

    static int actionAt(float x, float y, float width) {
        if (width < 390f) {
            if (y < 18f || y >= 54f) return NONE;
            if (x >= width - 116f && x < width - 68f) return OVERFLOW;
            if (x >= width - 60f && x < width - 12f) return THEME;
            return NONE;
        }
        if (y >= 18f && y < 54f) {
            if (x >= width - 196f && x < width - 148f) return OVERFLOW;
            if (x >= width - 140f && x < width - 92f) return THEME;
        }
        if (y >= 20f && y < 52f && x >= width - 80f && x < width - 16f) return LIVE_REFRESH;
        return NONE;
    }
}
