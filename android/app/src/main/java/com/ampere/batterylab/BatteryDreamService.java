package com.ampere.batterylab;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.service.dreams.DreamService;
import android.view.View;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.format.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Optional charging screensaver. The system decides when to start a Dream;
 * this service only renders validated local battery values while it is visible.
 */
public class BatteryDreamService extends DreamService {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private BatteryDreamView dreamView;
    private boolean receiverRegistered;
    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (dreamView == null) return;
            if (intent == null) return;
            if (Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) {
                dreamView.updateBattery(intent);
            }
        }
    };
    private final Runnable clockTick = new Runnable() {
        @Override public void run() {
            if (dreamView != null) {
                dreamView.invalidate();
                handler.postDelayed(this, 30_000L);
            }
        }
    };

    @Override public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);
        setFullscreen(true);
        setScreenBright(false);
        dreamView = new BatteryDreamView(this);
        setContentView(dreamView);
    }

    @Override public void onDreamingStarted() {
        super.onDreamingStarted();
        if (dreamView == null) return;
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(batteryReceiver, filter, RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(batteryReceiver, filter);
        }
        receiverRegistered = true;
        Intent battery = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (battery != null) dreamView.updateBattery(battery);
        handler.post(clockTick);
    }

    @Override public void onDreamingStopped() {
        handler.removeCallbacksAndMessages(null);
        if (receiverRegistered) {
            try { unregisterReceiver(batteryReceiver); } catch (IllegalArgumentException ignored) { }
            receiverRegistered = false;
        }
        super.onDreamingStopped();
    }

    @Override public void onDetachedFromWindow() {
        handler.removeCallbacksAndMessages(null);
        if (receiverRegistered) {
            try { unregisterReceiver(batteryReceiver); } catch (IllegalArgumentException ignored) { }
            receiverRegistered = false;
        }
        dreamView = null;
        super.onDetachedFromWindow();
    }
}

final class BatteryDreamView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF ring = new RectF();
    private final float density;
    private final int lime = Color.rgb(199, 243, 107);
    private final int blue = Color.rgb(118, 184, 255);
    private final int muted = Color.rgb(157, 166, 174);
    private final int faint = Color.rgb(89, 98, 106);
    private int level = -1;
    private int currentMa;
    private int voltageMv;
    private int temperatureTenths;
    private boolean charging;
    private int chargeLimit = BatteryChargeLimit.DEFAULT;

    BatteryDreamView(Context context) {
        super(context);
        density = getResources().getDisplayMetrics().density;
        setBackgroundColor(Color.BLACK);
        chargeLimit = BatteryChargeLimit.normalize(context.getSharedPreferences("ampere-data", Context.MODE_PRIVATE)
                .getInt("chargeLimit", BatteryChargeLimit.DEFAULT));
    }

    void updateBattery(Intent battery) {
        if (battery == null) return;
        BatteryReading reading = BatteryReading.read(getContext(), battery);
        level = reading.level;
        charging = reading.charging;
        temperatureTenths = reading.temperatureTenths;
        voltageMv = reading.voltageMv;
        currentMa = reading.currentMa;
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth() / density;
        float height = getHeight() / density;
        canvas.save();
        canvas.scale(density, density);
        paint.setTypeface(android.graphics.Typeface.create("sans", android.graphics.Typeface.NORMAL));
        drawHeader(canvas, width);
        drawGauge(canvas, width, height);
        drawStats(canvas, width, height);
        canvas.restore();
    }

    private void drawHeader(Canvas canvas, float width) {
        text(canvas, "AMPERE", 32, 48, 13, muted, true);
        String timePattern = DateFormat.is24HourFormat(getContext()) ? "HH:mm" : "h:mm";
        String time = new SimpleDateFormat(timePattern, Locale.getDefault()).format(new Date());
        rightText(canvas, time, width - 32, 50, 20, Color.WHITE, true);
        String date = new SimpleDateFormat("EEEE, d. MMMM", Locale.getDefault()).format(new Date());
        text(canvas, date, 32, 73, 11, faint, false);
        text(canvas, charging ? "LADEN" : "AKKUBETRIEB", 32, 95, 10, charging ? lime : blue, true);
    }

    private void drawGauge(Canvas canvas, float width, float height) {
        float radius = Math.min(108f, Math.max(72f, Math.min(width, height) * .18f));
        float cx = width / 2f;
        float cy = Math.min(height * .46f, 300f);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(9f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(Color.rgb(35, 43, 39));
        ring.set(cx - radius, cy - radius, cx + radius, cy + radius);
        canvas.drawArc(ring, -90f, 360f, false, paint);
        if (level >= 0) {
            paint.setColor(level <= 15 ? Color.rgb(255, 135, 105) : lime);
            canvas.drawArc(ring, -90f, 3.6f * level, false, paint);
        }
        String value = level >= 0 ? level + "%" : "—";
        centeredText(canvas, value, cx, cy + 13, 52, Color.WHITE, true);
        centeredText(canvas, charging ? "Ladezustand" : "Nicht am Ladegerät", cx, cy + 39, 11, muted, false);
        if (charging && chargeLimit < 100) {
            centeredText(canvas, "Ziel " + chargeLimit + "%", cx, cy + radius + 34, 11, lime, true);
        }
    }

    private void drawStats(Canvas canvas, float width, float height) {
        float y = Math.min(height - 120f, 510f);
        float gap = 12f;
        float cell = (width - 64f - gap * 2f) / 3f;
        Locale locale = AppText.uiLocale(getContext());
        stat(canvas, 32, y, cell, AppText.t(getContext(), "STROM"), BatteryTelemetryText.current(currentMa, charging, true, locale), charging ? lime : blue);
        stat(canvas, 32 + cell + gap, y, cell, AppText.t(getContext(), "LEISTUNG"), powerText(locale), charging ? lime : blue);
        stat(canvas, 32 + (cell + gap) * 2f, y, cell, "TEMP.", temperatureTenths > 0 ? String.format(locale, "%.1f °C", temperatureTenths / 10f) : "—", Color.rgb(242, 179, 106));
        text(canvas, voltageMv > 0 ? AppText.t(getContext(), String.format(locale, "Spannung %.2f V", voltageMv / 1000f)) : AppText.t(getContext(), "Spannung nicht verfügbar"), 32, y + 72, 11, faint, false);
        rightText(canvas, AppText.t(getContext(), "Berühren zum Beenden"), width - 32, y + 72, 11, faint, false);
    }

    private String powerText(Locale locale) {
        return BatteryTelemetryText.power(currentMa, voltageMv, charging, locale);
    }

    private void stat(Canvas canvas, float x, float y, float width, String label, String value, int color) {
        text(canvas, label, x, y, 9, faint, true);
        boundedText(canvas, value, x, x + width, y + 31, 16, color, true);
    }

    private void text(Canvas canvas, String value, float x, float y, float size, int color, boolean bold) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        paint.setTextSize(size);
        paint.setTypeface(android.graphics.Typeface.create("sans", bold ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL));
        canvas.drawText(value, x, y, paint);
    }

    private void centeredText(Canvas canvas, String value, float x, float y, float size, int color, boolean bold) {
        paint.setTextSize(size);
        paint.setTypeface(android.graphics.Typeface.create("sans", bold ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL));
        text(canvas, value, x - paint.measureText(value) / 2f, y, size, color, bold);
    }

    private void rightText(Canvas canvas, String value, float x, float y, float size, int color, boolean bold) {
        paint.setTextSize(size);
        paint.setTypeface(android.graphics.Typeface.create("sans", bold ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL));
        text(canvas, value, x - paint.measureText(value), y, size, color, bold);
    }

    private void boundedText(Canvas canvas, String value, float left, float right, float y, float size, int color, boolean bold) {
        paint.setTextSize(size);
        paint.setTypeface(android.graphics.Typeface.create("sans", bold ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL));
        float measured = paint.measureText(value);
        float scale = measured <= right - left || measured <= 0f
                ? 1f : (right - left) / measured;
        canvas.save();
        canvas.scale(scale, 1f, left, 0f);
        text(canvas, value, left, y, size, color, bold);
        canvas.restore();
    }
}
