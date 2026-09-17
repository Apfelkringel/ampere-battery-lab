package com.ampere.batterylab;

/** Responsive rules for hero copy sharing a card with its decorative battery character. */
final class BatteryEditorialLayout {
    private static final float ILLUSTRATION_MIN_WIDTH_DP = 320f;
    private static final float CARD_INSET_DP = 36f;
    private static final float ILLUSTRATION_TEXT_GUTTER_DP = 130f;

    private BatteryEditorialLayout() { }

    static boolean showSideIllustration(float widthDp) {
        return widthDp >= ILLUSTRATION_MIN_WIDTH_DP;
    }

    static float textRight(float widthDp) {
        return widthDp - (showSideIllustration(widthDp)
                ? ILLUSTRATION_TEXT_GUTTER_DP : CARD_INSET_DP);
    }
}
