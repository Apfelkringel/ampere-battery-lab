package com.ampere.batterylab;

/** Single source of truth for the drawn header action hitboxes. */
final class BatteryHeaderLayout {
    static final int NONE = 0;
    static final int OVERFLOW = 1;
    static final int LIVE_REFRESH = 2;
    private static final float TARGET_TOP = 12f;
    private static final float TARGET_BOTTOM = 60f;

    private BatteryHeaderLayout() { }

    static float pageTitleSize(float width) {
        return width < 280f ? 18f : 21.5f;
    }

    static int actionAt(float x, float y, float width) {
        if (width < 390f) {
            if (y < TARGET_TOP || y >= TARGET_BOTTOM) return NONE;
            if (x >= width - 60f && x < width - 12f) return OVERFLOW;
            return NONE;
        }
        if (y >= TARGET_TOP && y < TARGET_BOTTOM) {
            if (x >= width - 140f && x < width - 92f) return OVERFLOW;
        }
        if (y >= TARGET_TOP && y < TARGET_BOTTOM
                && x >= width - 80f && x < width - 16f) return LIVE_REFRESH;
        return NONE;
    }
}
