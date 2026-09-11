package com.ampere.batterylab;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.BatteryManager;
import android.widget.RemoteViews;
import java.util.Locale;

/** Small, local-only home-screen summary backed by Android's battery signals. */
public class BatteryWidgetProvider extends AppWidgetProvider {
    private static final int REQUEST_CODE = 7022;

    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        update(context, manager, appWidgetIds);
    }

    @Override public void onAppWidgetOptionsChanged(Context context, AppWidgetManager manager,
                                                    int appWidgetId, Bundle newOptions) {
        update(context, manager, new int[]{appWidgetId});
    }

    /** Refreshes every placed widget when the foreground monitor receives a sample. */
    static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName provider = new ComponentName(context, BatteryWidgetProvider.class);
        update(context, manager, manager.getAppWidgetIds(provider));
    }

    private static void update(Context context, AppWidgetManager manager, int[] ids) {
        if (ids == null || ids.length == 0) return;
        for (int id : ids) {
            Bundle options = manager.getAppWidgetOptions(id);
            int widthDp = options == null ? 180
                    : options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 180);
            int heightDp = options == null ? 72
                    : options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 72);
            manager.updateAppWidget(id, buildViews(context, widthDp, heightDp));
        }
    }

    private static RemoteViews buildViews(Context context, int widthDp, int heightDp) {
        Intent battery = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int rawLevel = battery == null ? -1 : battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = battery == null ? 100 : battery.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        int level = BatteryLevel.percent(rawLevel, scale);
        int status = battery == null ? BatteryManager.BATTERY_STATUS_UNKNOWN
                : battery.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN);
        int plugged = battery == null ? 0 : battery.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
        boolean charging = BatteryState.isCharging(status, plugged);
        int temperatureTenths = battery == null ? 0 : BatteryTemperature.normalizeTenths(
                battery.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0));
        int voltageMv = battery == null ? 0 : BatteryVoltage.normalizeMilliVolts(
                battery.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0));
        int currentMa = readCurrentMa(context);

        int layoutType = BatteryWidgetLayoutRules.select(widthDp, heightDp);
        int layout = layoutType == BatteryWidgetLayoutRules.SHORT
                ? R.layout.battery_widget_short
                : layoutType == BatteryWidgetLayoutRules.COMPACT
                ? R.layout.battery_widget_compact
                : R.layout.battery_widget;
        RemoteViews views = new RemoteViews(context.getPackageName(), layout);
        views.setTextViewText(R.id.widget_level, level >= 0 ? level + "%" : "—");
        views.setTextViewText(R.id.widget_status, statusText(status, charging));
        views.setTextViewText(R.id.widget_details, detailsText(charging, currentMa, temperatureTenths, voltageMv));
        views.setTextViewText(R.id.widget_caption, "Akku");
        views.setTextColor(R.id.widget_status, context.getColor(charging ? R.color.widget_accent : R.color.widget_muted));

        Intent launch = new Intent(context, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(context, REQUEST_CODE, launch,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_root, pending);
        String description = level >= 0 ? "Akkustand " + level + " Prozent, " : "Akkustand nicht verfügbar, ";
        views.setContentDescription(R.id.widget_root, description + statusText(status, charging)
                + ", " + detailsText(charging, currentMa, temperatureTenths, voltageMv));
        return views;
    }

    private static int readCurrentMa(Context context) {
        BatteryManager manager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
        return BatteryCurrent.milliAmps(manager);
    }

    private static String statusText(int status, boolean charging) {
        if (charging) return "Laden";
        if (status == BatteryManager.BATTERY_STATUS_UNKNOWN) return "Status unbekannt";
        return "Akkubetrieb";
    }

    private static String detailsText(boolean charging, int currentMa, int temperatureTenths, int voltageMv) {
        StringBuilder details = new StringBuilder();
        if (currentMa > 0) details.append(charging ? "+" : "−").append(formatCurrent(currentMa));
        else details.append("Strom —");
        if (temperatureTenths > 0) details.append(" · ").append(String.format(Locale.US, "%.1f °C", temperatureTenths / 10f));
        if (voltageMv > 0) details.append(" · ").append(String.format(Locale.US, "%.2f V", voltageMv / 1000f));
        return details.toString();
    }

    private static String formatCurrent(int currentMa) {
        return currentMa >= 1000
                ? String.format(Locale.US, "%.1f A", currentMa / 1000f)
                : currentMa + " mA";
    }
}
