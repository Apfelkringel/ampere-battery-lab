package com.ampere.batterylab;

import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;

/**
 * One validated, read-only battery snapshot shared by the lightweight output surfaces.
 * The monitor and dashboard may still override charging with their persisted edge state,
 * but they use the same individual validators when doing so.
 */
final class BatteryReading {
    final int level;
    final int status;
    final int plugged;
    final boolean charging;
    final int temperatureTenths;
    final int voltageMv;
    final int currentMa;

    private BatteryReading(int level, int status, int plugged, boolean charging,
                           int temperatureTenths, int voltageMv, int currentMa) {
        this.level = level;
        this.status = status;
        this.plugged = plugged;
        this.charging = charging;
        this.temperatureTenths = temperatureTenths;
        this.voltageMv = voltageMv;
        this.currentMa = currentMa;
    }

    static BatteryReading read(Context context, Intent battery) {
        if (battery == null) return unavailable();
        int rawLevel = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        int level = BatteryLevel.percent(rawLevel, scale);
        int status = battery.getIntExtra(BatteryManager.EXTRA_STATUS,
                BatteryManager.BATTERY_STATUS_UNKNOWN);
        int plugged = battery.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
        boolean charging = BatteryState.isCharging(status, plugged,
                battery.hasExtra(BatteryManager.EXTRA_PLUGGED));
        int temperature = BatteryTemperature.normalizeTenths(
                battery.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0));
        int voltage = BatteryVoltage.readMilliVolts(battery);
        BatteryManager manager = context == null ? null
                : (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
        int current = BatteryCurrent.milliAmps(manager, charging, Math.max(0, level));
        return fromValidatedValues(level, status, plugged, temperature, voltage, current,
                battery.hasExtra(BatteryManager.EXTRA_PLUGGED));
    }

    /** Pure constructor used by tests after the individual raw values are validated. */
    static BatteryReading fromValidatedValues(int level, int status, int plugged,
                                              int temperatureTenths, int voltageMv,
                                              int currentMa) {
        return fromValidatedValues(level, status, plugged, temperatureTenths, voltageMv,
                currentMa, true);
    }

    /** Keeps the distinction between an explicit unplugged value and a missing OEM field. */
    static BatteryReading fromValidatedValues(int level, int status, int plugged,
                                              int temperatureTenths, int voltageMv,
                                              int currentMa, boolean plugValuePresent) {
        return new BatteryReading(level, status, plugged,
                BatteryState.isCharging(status, plugged, plugValuePresent),
                temperatureTenths, voltageMv, currentMa);
    }

    private static BatteryReading unavailable() {
        return new BatteryReading(-1, BatteryManager.BATTERY_STATUS_UNKNOWN, 0,
                false, 0, 0, 0);
    }
}
