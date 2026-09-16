package com.ampere.batterylab;

/** Maps a chart touch to its displayed time bucket without inventing data. */
final class BatteryHistoryChartSelection {
    private BatteryHistoryChartSelection() { }

    static int bucketIndexAt(float x, float left, float right, int count) {
        if (count <= 0 || !Float.isFinite(x) || !Float.isFinite(left)
                || !Float.isFinite(right) || right <= left || x < left || x >= right) return -1;
        return Math.min(count - 1, (int) ((x - left) / (right - left) * count));
    }
}
