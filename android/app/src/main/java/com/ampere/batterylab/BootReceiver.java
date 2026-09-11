package com.ampere.batterylab;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/** Restores local battery monitoring after the device has restarted. */
public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        String action = intent == null ? null : intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) return;
        Intent service = new Intent(context, BatteryMonitorService.class);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(service); else context.startService(service);
        } catch (IllegalStateException ignored) {
            // Some OEMs defer background service starts; the next app launch repairs this state.
        }
        // Schedule recovery even when the OEM rejected the initial background
        // start; without a heartbeat the watchdog will retry later.
        BatteryMonitorWatchdog.schedule(context);
        // SystemUI can keep an existing tile entry across an APK update while
        // dropping the old TileService binding. Ask it to bind the new service
        // again without requiring the user to remove and re-add the tile.
        BatteryQuickSettingsService.requestRefresh(context);
    }
}
