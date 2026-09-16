package com.ampere.batterylab;

/** Selects the native reflowable dashboard when the user requests larger text. */
final class BatteryLargeTextMode {
    private static final float MINIMUM_FONT_SCALE = 1.25f;

    private BatteryLargeTextMode() { }

    static boolean shouldUseNativeLayout(float fontScale) {
        return Float.isFinite(fontScale) && fontScale >= MINIMUM_FONT_SCALE;
    }
}
