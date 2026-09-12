package com.ampere.batterylab;

/** Single source of truth for the drawn header action hitboxes. */
final class BatteryHeaderLayout {
    static final int NONE = 0;
    static final int OVERFLOW = 1;
    static final int THEME = 2;
    static final int LIVE_REFRESH = 3;

    private BatteryHeaderLayout() { }

    static int actionAt(float x, float y, float width) {
        if (y < 8f || y >= 70f) return NONE;
        if (width < 390f) {
            if (x > width - 116f && x < width - 64f) return OVERFLOW;
            if (x >= width - 64f && x < width - 12f) return THEME;
            return NONE;
        }
        if (x > width - 200f && x < width - 144f) return OVERFLOW;
        if (x >= width - 144f && x < width - 88f) return THEME;
        if (x >= width - 84f && x < width - 12f) return LIVE_REFRESH;
        return NONE;
    }
}
