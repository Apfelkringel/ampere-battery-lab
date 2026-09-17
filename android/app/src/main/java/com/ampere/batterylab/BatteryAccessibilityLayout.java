package com.ampere.batterylab;

/** Geometry and labels for interactive Canvas controls exposed to accessibility services. */
final class BatteryAccessibilityLayout {
    static final int OVERVIEW_7D = 50;
    static final int OVERVIEW_30D = 51;
    static final int CHARGE_ALARM = 52;
    static final int CHARGE_OVERLAY = 53;
    static final int CHARGE_LIMIT = 54;
    static final int HEALTH_BENCHMARK = 55;
    static final int HEALTH_CAPACITY = 56;
    static final int DISCHARGE_USAGE = 57;
    static final int HISTORY_EXPORT = 58;
    static final int HISTORY_DAY = 59;
    static final int HISTORY_WEEK = 60;
    static final int HISTORY_MONTH = 61;
    static final int HISTORY_VALUES = 62;
    static final int DISCHARGE_SCREEN_ON = 63;
    static final int DISCHARGE_SCREEN_OFF = 64;
    static final int DISCHARGE_NORMAL = 65;

    static boolean isDischargeEstimate(int virtualViewId) {
        return virtualViewId >= DISCHARGE_SCREEN_ON && virtualViewId <= DISCHARGE_NORMAL;
    }

    private BatteryAccessibilityLayout() { }

    static boolean isVisible(int virtualViewId, int page) {
        if (page == 0) return virtualViewId == OVERVIEW_7D || virtualViewId == OVERVIEW_30D;
        if (page == 1) return virtualViewId == CHARGE_ALARM || virtualViewId == CHARGE_OVERLAY
                || virtualViewId == CHARGE_LIMIT;
        if (page == 2) return virtualViewId == DISCHARGE_USAGE || isDischargeEstimate(virtualViewId);
        if (page == 3) return virtualViewId == HEALTH_BENCHMARK || virtualViewId == HEALTH_CAPACITY;
        if (page == 4) return virtualViewId == HISTORY_EXPORT
                || virtualViewId == HISTORY_DAY || virtualViewId == HISTORY_WEEK
                || virtualViewId == HISTORY_MONTH || virtualViewId == HISTORY_VALUES;
        return false;
    }

    static int[] pageControlsFor(int page) {
        switch (page) {
            case 0: return new int[]{OVERVIEW_7D, OVERVIEW_30D};
            case 1: return new int[]{CHARGE_ALARM, CHARGE_OVERLAY, CHARGE_LIMIT};
            case 2: return new int[]{DISCHARGE_SCREEN_ON, DISCHARGE_SCREEN_OFF,
                    DISCHARGE_NORMAL, DISCHARGE_USAGE};
            case 3: return new int[]{HEALTH_BENCHMARK, HEALTH_CAPACITY};
            case 4: return new int[]{HISTORY_DAY, HISTORY_WEEK, HISTORY_MONTH, HISTORY_VALUES, HISTORY_EXPORT};
            default: return new int[0];
        }
    }

    static String label(int virtualViewId, boolean historyDays30, boolean chargeAlarm,
                        boolean overlayEnabled, boolean benchmarkActive) {
        return label(virtualViewId, historyDays30, chargeAlarm, overlayEnabled,
                benchmarkActive, BatteryChargeLimit.DEFAULT);
    }

    static String label(int virtualViewId, boolean historyDays30, boolean chargeAlarm,
                        boolean overlayEnabled, boolean benchmarkActive, int chargeLimit) {
        switch (virtualViewId) {
            case OVERVIEW_7D: return "7 Tage" + (historyDays30 ? " (ausgewählt)" : "");
            case OVERVIEW_30D: return "30 Tage" + (historyDays30 ? "" : " (ausgewählt)");
            case CHARGE_ALARM: return "Ladealarm: " + (chargeAlarm ? "Aktiv" : "Aus");
            case CHARGE_OVERLAY: return "Live-Anzeige: " + (overlayEnabled ? "Aktiv" : "Aus");
            case CHARGE_LIMIT: return "Ladeziel: " + normalizeChargeLimit(chargeLimit) + " Prozent";
            case HEALTH_BENCHMARK: return benchmarkActive ? "Kapazitätsmessung stoppen" : "Kapazität messen";
            case HEALTH_CAPACITY: return "Nennkapazität bearbeiten";
            case DISCHARGE_USAGE: return "Akkuverbrauch deiner Apps anzeigen";
            case DISCHARGE_SCREEN_ON: return "Restlaufzeit bei dauerhaft eingeschaltetem Bildschirm";
            case DISCHARGE_SCREEN_OFF: return "Restlaufzeit bei ausgeschaltetem Bildschirm";
            case DISCHARGE_NORMAL: return "Restlaufzeit bei normaler Nutzung";
            case HISTORY_EXPORT: return "CSV exportieren";
            case HISTORY_DAY: return "Verlauf täglich";
            case HISTORY_WEEK: return "Verlauf wöchentlich";
            case HISTORY_MONTH: return "Verlauf monatlich";
            case HISTORY_VALUES: return "Exakte Werte im Bilanzdiagramm anzeigen";
            default: return "";
        }
    }

    static int normalizeChargeLimit(int value) {
        return Math.max(50, Math.min(100, value));
    }

    /** Full-cell navigation targets meet Android's 48 dp minimum on narrow phones. */
    static int[] navigationBounds(int tabIndex, float bodyWidth) {
        if (tabIndex < 0 || tabIndex >= 5 || bodyWidth <= 0f) {
            return new int[]{0, 0, 0, 0};
        }
        float inset = navigationInset(bodyWidth);
        float cell = (bodyWidth - 2f * inset) / 5f;
        float left = inset + tabIndex * cell;
        return new int[]{Math.round(left), 120, Math.round(left + cell), 168};
    }

    static int navigationTabAt(float x, float bodyWidth) {
        if (bodyWidth <= 0f) return -1;
        float inset = navigationInset(bodyWidth);
        float right = bodyWidth - inset;
        if (x < inset || x > right) return -1;
        float cell = (bodyWidth - 2f * inset) / 5f;
        return Math.max(0, Math.min(4, (int) ((x - inset) / cell)));
    }

    static float navigationInset(float bodyWidth) {
        return Math.min(18f, Math.max(0f, (bodyWidth - 240f) / 2f));
    }

    static int historyDaysForControl(int virtualViewId, int currentDays) {
        if (virtualViewId == OVERVIEW_7D) return 7;
        if (virtualViewId == OVERVIEW_30D) return 30;
        return currentDays == 30 ? 30 : 7;
    }

    static boolean isHistoryPeriodSelected(int virtualViewId, int selectedPeriodDays) {
        return (virtualViewId == HISTORY_DAY && selectedPeriodDays == 1)
                || (virtualViewId == HISTORY_WEEK && selectedPeriodDays == 7)
                || (virtualViewId == HISTORY_MONTH && selectedPeriodDays == 30);
    }

    /** Returns left, top, right, bottom in dashboard dp coordinates. */
    static int[] bounds(int virtualViewId, float bodyInset, float bodyWidth,
                        float overviewChartTop, float historyExportTop) {
        return bounds(virtualViewId, bodyInset, bodyWidth, overviewChartTop,
                historyExportTop, bodyWidth < 600f);
    }

    /** Returns bounds using the same portrait/wide mode as the dashboard renderer. */
    static int[] bounds(int virtualViewId, float bodyInset, float bodyWidth,
                        float overviewChartTop, float historyExportTop,
                        boolean editorialPortrait) {
        float left;
        float top;
        float right;
        float bottom;
        switch (virtualViewId) {
            case OVERVIEW_7D:
                left = bodyInset + bodyWidth - 130f;
                top = overviewChartTop + 2f;
                right = bodyInset + bodyWidth - 76f;
                bottom = overviewChartTop + 50f;
                break;
            case OVERVIEW_30D:
                left = bodyInset + bodyWidth - 72f;
                top = overviewChartTop + 2f;
                right = bodyInset + bodyWidth - 18f;
                bottom = overviewChartTop + 50f;
                break;
            case CHARGE_ALARM:
                left = bodyInset + (editorialPortrait ? 22f : 36f);
                top = 459f;
                right = bodyInset + bodyWidth - (editorialPortrait ? 22f : 36f);
                bottom = 507f;
                break;
            case CHARGE_OVERLAY:
                left = bodyInset + (editorialPortrait ? 22f : 36f);
                top = 507f;
                right = bodyInset + bodyWidth - (editorialPortrait ? 22f : 36f);
                bottom = 555f;
                break;
            case CHARGE_LIMIT:
                left = bodyInset + 36f;
                top = 365f;
                right = bodyInset + bodyWidth - 36f;
                bottom = 413f;
                break;
            case HEALTH_BENCHMARK:
                left = bodyInset + bodyWidth - 216f;
                top = editorialPortrait ? 704f : 734f;
                right = bodyInset + bodyWidth - 36f;
                bottom = editorialPortrait ? 752f : 782f;
                break;
            case HEALTH_CAPACITY:
                left = bodyInset + 18f;
                top = editorialPortrait ? 830f : 812f;
                right = bodyInset + bodyWidth - 18f;
                bottom = editorialPortrait ? 890f : 879f;
                break;
            case DISCHARGE_USAGE:
                left = bodyInset + 18f;
                top = editorialPortrait ? 806f : 722f;
                right = bodyInset + bodyWidth - 18f;
                bottom = editorialPortrait ? 981f : 897f;
                break;
            case DISCHARGE_SCREEN_ON:
            case DISCHARGE_SCREEN_OFF:
            case DISCHARGE_NORMAL:
                left = bodyInset + 42f;
                top = 182f + (virtualViewId - DISCHARGE_SCREEN_ON) * 40f + 214f;
                right = bodyInset + bodyWidth - 42f;
                bottom = top + 40f;
                break;
            case HISTORY_EXPORT:
                left = bodyInset + 36f;
                top = historyExportTop - 2f;
                right = bodyInset + bodyWidth - 36f;
                bottom = historyExportTop + 46f;
                break;
            case HISTORY_DAY:
                left = bodyInset + 36f;
                right = bodyInset + 42f + (bodyWidth - 84f) / 3f;
                top = 296f;
                bottom = 344f;
                break;
            case HISTORY_WEEK:
                left = bodyInset + 42f + (bodyWidth - 84f) / 3f;
                top = 296f;
                right = bodyInset + 54f + 2f * (bodyWidth - 84f) / 3f;
                bottom = 344f;
                break;
            case HISTORY_MONTH:
                left = bodyInset + 54f + 2f * (bodyWidth - 84f) / 3f;
                top = 296f;
                right = bodyInset + bodyWidth - 36f;
                bottom = 344f;
                break;
            case HISTORY_VALUES:
                left = bodyInset + 36f;
                top = 182f + 430f + 232f;
                right = bodyInset + bodyWidth - 36f;
                bottom = 182f + 430f + 280f;
                break;
            default:
                return new int[]{0, 0, 0, 0};
        }
        return new int[]{Math.round(left), Math.round(top), Math.round(right), Math.round(bottom)};
    }

    /** Maps the shared six-dp gutters to the nearest visible period button. */
    static int historyPeriodControlAt(float screenX, float bodyInset, float bodyWidth) {
        float bodyX = screenX - bodyInset;
        if (bodyX < 36f || bodyX >= bodyWidth - 36f) return 0;
        float controlWidth = (bodyWidth - 84f) / 3f;
        float dayWeekBoundary = 42f + controlWidth;
        float weekMonthBoundary = 54f + 2f * controlWidth;
        if (bodyX < dayWeekBoundary - .01f) return HISTORY_DAY;
        if (bodyX < weekMonthBoundary - .01f) return HISTORY_WEEK;
        return HISTORY_MONTH;
    }
}
