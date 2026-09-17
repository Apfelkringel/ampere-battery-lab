package com.ampere.batterylab;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.BatteryManager;
import android.util.SizeF;
import android.widget.RemoteViews;
import java.util.LinkedHashMap;
import java.util.Map;
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
        WidgetState state = readState(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ can select among these layouts without waking the
            // app again for every launcher size. The lower-left points are
            // the exact cutoffs declared by the widget's resize range:
            // short <72dp high or <160dp wide, compact <220dp wide,
            // standard otherwise.
            Map<SizeF, RemoteViews> responsive = new LinkedHashMap<>();
            responsive.put(new SizeF(109f, 56f), populateViews(context,
                    BatteryWidgetLayoutRules.SHORT, state));
            responsive.put(new SizeF(109f, 72f), populateViews(context,
                    BatteryWidgetLayoutRules.SHORT, state));
            responsive.put(new SizeF(160f, 72f), populateViews(context,
                    BatteryWidgetLayoutRules.COMPACT, state));
            responsive.put(new SizeF(220f, 72f), populateViews(context,
                    BatteryWidgetLayoutRules.STANDARD, state));
            return new RemoteViews(responsive);
        }
        int layoutType = BatteryWidgetLayoutRules.select(widthDp, heightDp);
        return populateViews(context, layoutType, state);
    }

    private static WidgetState readState(Context context) {
        Intent battery = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        BatteryReading reading = BatteryReading.read(context, battery);
        return new WidgetState(reading.level, reading.status, reading.charging,
                reading.temperatureTenths, reading.voltageMv, reading.currentMa);
    }

    private static RemoteViews populateViews(Context context, int layoutType, WidgetState state) {
        int layout = layoutType == BatteryWidgetLayoutRules.SHORT
                ? R.layout.battery_widget_short
                : layoutType == BatteryWidgetLayoutRules.COMPACT
                ? R.layout.battery_widget_compact
                : R.layout.battery_widget;
        RemoteViews views = new RemoteViews(context.getPackageName(), layout);
        String statusText = statusText(state.status, state.charging);
        String detailsText = detailsText(context, state.charging, state.currentMa,
                state.temperatureTenths, state.voltageMv, layoutType);
        views.setTextViewText(R.id.widget_level, state.level >= 0 ? state.level + "%" : "—");
        views.setTextViewText(R.id.widget_status, AppText.t(context, statusText));
        views.setTextViewText(R.id.widget_details, AppText.t(context, detailsText));
        views.setTextViewText(R.id.widget_caption, AppText.t(context, "Akku"));
        views.setTextColor(R.id.widget_status, context.getColor(state.charging ? R.color.widget_accent : R.color.widget_muted));

        Intent launch = new Intent(context, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(context, REQUEST_CODE, launch,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_root, pending);
        String description = state.level >= 0 ? "Akkustand " + state.level + " Prozent, " : "Akkustand nicht verfügbar, ";
        views.setContentDescription(R.id.widget_root, AppText.t(context, description + statusText + ", " + detailsText));
        return views;
    }

    private static final class WidgetState {
        final int level;
        final int status;
        final boolean charging;
        final int temperatureTenths;
        final int voltageMv;
        final int currentMa;

        WidgetState(int level, int status, boolean charging, int temperatureTenths,
                    int voltageMv, int currentMa) {
            this.level = level;
            this.status = status;
            this.charging = charging;
            this.temperatureTenths = temperatureTenths;
            this.voltageMv = voltageMv;
            this.currentMa = currentMa;
        }
    }

    private static String statusText(int status, boolean charging) {
        if (charging) return "Laden";
        if (status == BatteryManager.BATTERY_STATUS_UNKNOWN) return "Unbekannt";
        return "Akku";
    }

    private static String detailsText(Context context, boolean charging, int currentMa, int temperatureTenths,
                                      int voltageMv, int layoutType) {
        Locale locale = AppText.uiLocale(context);
        String current = BatteryTelemetryText.current(currentMa, charging, true, locale);
        // Narrow layouts get one high-signal line so no important value is
        // pushed under the percentage or clipped by the launcher.
        if (layoutType != BatteryWidgetLayoutRules.STANDARD) {
            return current.equals("—") ? "Strom —" : current;
        }
        if (!current.equals("—")) {
            if (temperatureTenths > 0) {
                return current + " · " + String.format(locale, "%.1f °C", temperatureTenths / 10f);
            }
            return current;
        }
        if (temperatureTenths > 0) {
            return String.format(locale, "%.1f °C", temperatureTenths / 10f);
        }
        if (voltageMv > 0) {
            return String.format(locale, "%.2f V", voltageMv / 1000f);
        }
        return "Strom —";
    }
}
