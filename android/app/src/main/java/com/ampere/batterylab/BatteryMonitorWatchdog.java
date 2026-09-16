package com.ampere.batterylab;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;

/** Restarts local monitoring only after its persisted heartbeat becomes stale. */
public final class BatteryMonitorWatchdog extends BroadcastReceiver {
    static final long CHECK_INTERVAL_MS = 20L * 60L * 1000L;
    static final long STALE_AFTER_MS = 30L * 60L * 1000L;
    private static final int REQUEST_CODE = 2351;
    private static final String PREFS = "ampere-data";
    private static final String KEY_HEARTBEAT = "monitorHeartbeatElapsed";

    private static PendingIntent pendingIntent(Context context) {
        Intent intent = new Intent(context, BatteryMonitorWatchdog.class);
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static void recordHeartbeat(Context context) {
        if (context == null) return;
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putLong(KEY_HEARTBEAT, SystemClock.elapsedRealtime()).apply();
    }

    static boolean isHeartbeatStale(long lastHeartbeat, long now) {
        return lastHeartbeat <= 0L || now < lastHeartbeat
                || now - lastHeartbeat >= STALE_AFTER_MS;
    }

    static long lastHeartbeat(Context context) {
        if (context == null) return 0L;
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getLong(KEY_HEARTBEAT, 0L);
    }

    static void schedule(Context context) {
        if (context == null) return;
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarms == null) return;
        alarms.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + CHECK_INTERVAL_MS, pendingIntent(context));
    }

    @Override public void onReceive(Context context, Intent intent) {
        if (context == null) return;
        long now = SystemClock.elapsedRealtime();
        long last = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getLong(KEY_HEARTBEAT, 0L);
        if (isHeartbeatStale(last, now)) {
            Intent service = new Intent(context, BatteryMonitorService.class);
            try {
                context.stopService(service);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(service);
                } else {
                    context.startService(service);
                }
            } catch (IllegalStateException ignored) {
                // Android/OEM background-start restrictions are retried by the
                // next watchdog tick or the next visible app launch.
            }
        }
        schedule(context);
    }
}
