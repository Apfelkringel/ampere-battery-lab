package com.ampere.batterylab;

import android.os.BatteryManager;

/** Reads Android current properties once and rejects sentinels, bad units and spikes. */
final class BatteryCurrent {
    private static final long MIN_MICROAMPS = 1_000L;
    private static final long MAX_MICROAMPS = 100_000_000L;

    private BatteryCurrent() { }

    static int milliAmps(BatteryManager manager) {
        if (manager == null) return 0;
        try {
            int now = fromMicroamps(manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW));
            if (now > 0) return now;
            return fromMicroamps(manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE));
        } catch (RuntimeException ignored) {
            return 0;
        }
    }

    static int fromMicroamps(int raw) {
        long magnitude = Math.abs((long) raw);
        if (raw == Integer.MIN_VALUE || magnitude < MIN_MICROAMPS || magnitude > MAX_MICROAMPS) return 0;
        return (int) (magnitude / 1_000L);
    }
}
