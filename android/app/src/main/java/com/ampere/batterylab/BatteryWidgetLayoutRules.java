package com.ampere.batterylab;

/** Chooses a widget layout that can contain its content at the current size. */
final class BatteryWidgetLayoutRules {
    static final int SHORT = 0;
    static final int COMPACT = 1;
    static final int STANDARD = 2;

    private static final int COMPACT_WIDTH_DP = 220;
    private static final int SHORT_WIDTH_DP = 160;
    private static final int SHORT_HEIGHT_DP = 72;

    private BatteryWidgetLayoutRules() { }

    static int select(int widthDp, int heightDp) {
        // At very narrow widths there is no honest room for icon, status and
        // percentage in the compact three-column layout. The single-row
        // short layout keeps the value readable instead of squeezing text
        // into a few pixels.
        if (heightDp < SHORT_HEIGHT_DP || widthDp < SHORT_WIDTH_DP) return SHORT;
        return widthDp < COMPACT_WIDTH_DP ? COMPACT : STANDARD;
    }
}
