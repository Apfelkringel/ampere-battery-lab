package com.ampere.batterylab;

/** Geometry and labels for interactive Canvas controls exposed to accessibility services. */
final class BatteryAccessibilityLayout {
    static final int OVERVIEW_7D = 50;
    static final int OVERVIEW_30D = 51;
    static final int CHARGE_ALARM = 52;
    static final int CHARGE_OVERLAY = 53;
    static final int HEALTH_BENCHMARK = 54;
    static final int HEALTH_CAPACITY = 55;
    static final int DISCHARGE_USAGE = 56;
    static final int HISTORY_EXPORT = 57;

    private BatteryAccessibilityLayout() { }

    static boolean isVisible(int virtualViewId, int page) {
        if (page == 0) return virtualViewId == OVERVIEW_7D || virtualViewId == OVERVIEW_30D;
        if (page == 1) return virtualViewId == CHARGE_ALARM || virtualViewId == CHARGE_OVERLAY;
        if (page == 2) return virtualViewId == DISCHARGE_USAGE;
        if (page == 3) return virtualViewId == HEALTH_BENCHMARK || virtualViewId == HEALTH_CAPACITY;
        if (page == 4) return virtualViewId == HISTORY_EXPORT;
        return false;
    }

    static int[] pageControlsFor(int page) {
        switch (page) {
            case 0: return new int[]{OVERVIEW_7D, OVERVIEW_30D};
            case 1: return new int[]{CHARGE_ALARM, CHARGE_OVERLAY};
            case 2: return new int[]{DISCHARGE_USAGE};
            case 3: return new int[]{HEALTH_BENCHMARK, HEALTH_CAPACITY};
            case 4: return new int[]{HISTORY_EXPORT};
            default: return new int[0];
        }
    }

    static String label(int virtualViewId, boolean historyDays30, boolean chargeAlarm,
                        boolean overlayEnabled, boolean benchmarkActive) {
        switch (virtualViewId) {
            case OVERVIEW_7D: return "7 Tage" + (historyDays30 ? " (ausgewählt)" : "");
            case OVERVIEW_30D: return "30 Tage" + (historyDays30 ? "" : " (ausgewählt)");
            case CHARGE_ALARM: return "Ladealarm: " + (chargeAlarm ? "Aktiv" : "Aus");
            case CHARGE_OVERLAY: return "Live-Anzeige: " + (overlayEnabled ? "Aktiv" : "Aus");
            case HEALTH_BENCHMARK: return benchmarkActive ? "Benchmark stoppen" : "Benchmark starten";
            case HEALTH_CAPACITY: return "Nennkapazität bearbeiten";
            case DISCHARGE_USAGE: return "Vordergrundverbrauch öffnen";
            case HISTORY_EXPORT: return "CSV exportieren";
            default: return "";
        }
    }

    /** Returns left, top, right, bottom in dashboard dp coordinates. */
    static int[] bounds(int virtualViewId, float bodyInset, float bodyWidth,
                        float overviewChartTop, float historyExportTop) {
        float left;
        float top;
        float right;
        float bottom;
        switch (virtualViewId) {
            case OVERVIEW_7D:
                left = bodyInset + bodyWidth - 100f;
                top = overviewChartTop + 14f;
                right = bodyInset + bodyWidth - 62f;
                bottom = overviewChartTop + 42f;
                break;
            case OVERVIEW_30D:
                left = bodyInset + bodyWidth - 58f;
                top = overviewChartTop + 14f;
                right = bodyInset + bodyWidth - 18f;
                bottom = overviewChartTop + 42f;
                break;
            case CHARGE_ALARM:
                left = bodyInset + bodyWidth - 145f;
                top = 462f;
                right = bodyInset + bodyWidth - 30f;
                bottom = 504f;
                break;
            case CHARGE_OVERLAY:
                left = bodyInset + bodyWidth - 145f;
                top = 500f;
                right = bodyInset + bodyWidth - 30f;
                bottom = 545f;
                break;
            case HEALTH_BENCHMARK:
                left = bodyInset + bodyWidth - 145f;
                top = 730f;
                right = bodyInset + bodyWidth - 30f;
                bottom = 800f;
                break;
            case HEALTH_CAPACITY:
                left = bodyInset + 18f;
                top = 812f;
                right = bodyInset + bodyWidth - 18f;
                bottom = 882f;
                break;
            case DISCHARGE_USAGE:
                left = bodyInset + 18f;
                top = 722f;
                right = bodyInset + bodyWidth - 18f;
                bottom = 897f;
                break;
            case HISTORY_EXPORT:
                left = bodyInset + bodyWidth - 155f;
                top = historyExportTop;
                right = bodyInset + bodyWidth - 18f;
                bottom = historyExportTop + 48f;
                break;
            default:
                return new int[]{0, 0, 0, 0};
        }
        return new int[]{Math.round(left), Math.round(top), Math.round(right), Math.round(bottom)};
    }
}
