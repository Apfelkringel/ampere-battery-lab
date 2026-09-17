package com.ampere.batterylab;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
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
    private boolean batteryReceiverRegistered;

    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            refreshTile(intent);
        }
    };

    @Override public void onStartListening() {
        super.onStartListening();
        Intent sticky = registerBatteryReceiver();
        refreshTile(sticky);
    }

    private Intent registerBatteryReceiver() {
        if (batteryReceiverRegistered) return null;
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent sticky;
        if (Build.VERSION.SDK_INT >= 33) {
            sticky = registerReceiver(batteryReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            sticky = registerReceiver(batteryReceiver, filter);
        }
        batteryReceiverRegistered = true;
        return sticky;
    }

    private void unregisterBatteryReceiver() {
        if (!batteryReceiverRegistered) return;
        try { unregisterReceiver(batteryReceiver); } catch (IllegalArgumentException ignored) { }
        batteryReceiverRegistered = false;
    }

    @Override public void onStopListening() {
        unregisterBatteryReceiver();
        super.onStopListening();
    }

    @Override public void onTileRemoved() {
        unregisterBatteryReceiver();
        super.onTileRemoved();
    }

    @Override public void onDestroy() {
        unregisterBatteryReceiver();
        super.onDestroy();
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
    @SuppressLint("NewApi")
    static void requestRefresh(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return;
        try {
            requestListeningState(context.getApplicationContext(),
                    new ComponentName(context, BatteryQuickSettingsService.class));
        } catch (RuntimeException ignored) {
            // The tile may not have been added yet or the launcher may be restarting.
        }
    }

    private void refreshTile(Intent battery) {
        Tile tile = getQsTile();
        if (tile == null) return;
        BatteryReading reading = BatteryReading.read(this, battery);
        int level = reading.level;
        int status = reading.status;
        boolean charging = reading.charging;
        int temp = reading.temperatureTenths;
        int voltage = reading.voltageMv;
        int current = reading.currentMa;

        // Keep the title stable so SystemUI does not cache a stale dynamic
        // label. Put the live value in the subtitle, like established battery
        // tiles do; this also keeps the tile readable in compact layouts.
        tile.setLabel("Ampere");
        tile.setState(level >= 0 ? Tile.STATE_INACTIVE : Tile.STATE_UNAVAILABLE);
        if (Build.VERSION.SDK_INT >= 29) tile.setSubtitle(level >= 0
                ? AppText.t(this, shortSubtitle(level, status, charging, current, temp))
                : AppText.t(this, "Akku nicht verfügbar"));
        if (Build.VERSION.SDK_INT >= 30) {
            tile.setContentDescription(level >= 0
                    ? AppText.t(this, "Akkustand " + level + " Prozent, "
                    + subtitle(status, charging, current, temp, voltage))
                    : AppText.t(this, "Akkustand nicht verfügbar"));
        }
        tile.updateTile();
    }

    private static String shortSubtitle(int level, int status, boolean charging, int currentMa, int temperatureTenths) {
        StringBuilder result = new StringBuilder().append(level).append("% · ")
                .append(charging ? "Laden" : status == BatteryManager.BATTERY_STATUS_UNKNOWN ? "Status unbekannt" : "Akkubetrieb");
        if (currentMa > 0) result.append(" · ").append(BatteryTelemetryText.current(currentMa, charging, false));
        if (temperatureTenths > 0) result.append(" · ").append(String.format(Locale.GERMANY, "%.1f°C", temperatureTenths / 10f));
        return result.toString();
    }

    private static String subtitle(int status, boolean charging, int currentMa, int temperatureTenths, int voltageMv) {
        StringBuilder result = new StringBuilder();
        if (charging) result.append("Laden");
        else if (status == BatteryManager.BATTERY_STATUS_UNKNOWN) result.append("Status unbekannt");
        else result.append("Akkubetrieb");
        if (currentMa > 0) result.append(" · ").append(BatteryTelemetryText.current(currentMa, charging, false));
        if (temperatureTenths > 0) result.append(" · ").append(String.format(Locale.GERMANY, "%.1f°C", temperatureTenths / 10f));
        if (voltageMv > 0) result.append(" · ").append(String.format(Locale.GERMANY, "%.2fV", voltageMv / 1000f));
        return result.toString();
    }

}
