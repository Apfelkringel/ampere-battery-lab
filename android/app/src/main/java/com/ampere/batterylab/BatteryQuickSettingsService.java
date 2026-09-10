package com.ampere.batterylab;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import java.util.Locale;

/** Informational Quick Settings tile; it never changes a system setting. */
@TargetApi(Build.VERSION_CODES.N)
public class BatteryQuickSettingsService extends TileService {
    private static final int REQUEST_CODE = 7024;

    @Override public void onStartListening() {
        super.onStartListening();
        refreshTile();
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    @Override public void onClick() {
        super.onClick();
        Intent launch = new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pending = PendingIntent.getActivity(this, REQUEST_CODE, launch,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (Build.VERSION.SDK_INT >= 34) {
            // Android 14+ rejects the Intent overload for targetSdk 34+.
            startActivityAndCollapse(pending);
        } else {
            startActivityAndCollapse(launch);
        }
    }

    /** Requests a refresh while the monitor process is alive. */
    static void requestRefresh(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return;
        try {
            requestListeningState(context.getApplicationContext(),
                    new ComponentName(context, BatteryQuickSettingsService.class));
        } catch (RuntimeException ignored) {
            // The tile may not have been added yet or the launcher may be restarting.
        }
    }

    private void refreshTile() {
        Tile tile = getQsTile();
        if (tile == null) return;
        Intent battery = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int rawLevel = battery == null ? -1 : battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = battery == null ? 100 : battery.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        int level = rawLevel >= 0 && scale > 0
                ? Math.max(0, Math.min(100, Math.round(rawLevel * 100f / scale))) : -1;
        int status = battery == null ? BatteryManager.BATTERY_STATUS_UNKNOWN
                : battery.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN);
        int plugged = battery == null ? 0 : battery.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
        boolean charging = BatteryState.isCharging(status, plugged);
        int temp = battery == null ? 0 : battery.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
        int voltage = battery == null ? 0 : battery.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);
        int current = readCurrentMa();

        tile.setLabel(level >= 0 ? level + "% Akku" : "Ampere");
        tile.setState(level >= 0 ? Tile.STATE_ACTIVE : Tile.STATE_UNAVAILABLE);
        if (Build.VERSION.SDK_INT >= 29) tile.setSubtitle(subtitle(status, charging, current, temp, voltage));
        if (Build.VERSION.SDK_INT >= 30) {
            tile.setContentDescription(level >= 0
                    ? "Akkustand " + level + " Prozent, " + subtitle(status, charging, current, temp, voltage)
                    : "Akkustand nicht verfügbar");
        }
        tile.updateTile();
    }

    private int readCurrentMa() {
        BatteryManager manager = (BatteryManager) getSystemService(BATTERY_SERVICE);
        if (manager == null) return 0;
        int microamps;
        try {
            microamps = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
            if (microamps == Integer.MIN_VALUE || microamps == 0) {
                microamps = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE);
            }
        } catch (RuntimeException ignored) {
            return 0;
        }
        return microamps == Integer.MIN_VALUE ? 0 : Math.abs(microamps) / 1000;
    }

    private static String subtitle(int status, boolean charging, int currentMa, int temperatureTenths, int voltageMv) {
        StringBuilder result = new StringBuilder();
        if (charging) result.append("Laden");
        else if (status == BatteryManager.BATTERY_STATUS_UNKNOWN) result.append("Status unbekannt");
        else result.append("Akkubetrieb");
        if (currentMa > 0) result.append(" · ").append(charging ? "+" : "−").append(formatCurrent(currentMa));
        if (temperatureTenths > 0) result.append(" · ").append(String.format(Locale.US, "%.1f°C", temperatureTenths / 10f));
        if (voltageMv > 0) result.append(" · ").append(String.format(Locale.US, "%.2fV", voltageMv / 1000f));
        return result.toString();
    }

    private static String formatCurrent(int currentMa) {
        return currentMa >= 1000
                ? String.format(Locale.US, "%.1fA", currentMa / 1000f)
                : currentMa + "mA";
    }
}
