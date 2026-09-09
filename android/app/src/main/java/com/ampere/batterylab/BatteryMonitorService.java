package com.ampere.batterylab;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.backup.BackupManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Lightweight, local-only recorder. It samples the public Android battery
 * broadcast every 15 minutes and keeps short and 30-day local histories.
 */
public class BatteryMonitorService extends Service {
    private static final String CHANNEL_ID = "ampere-monitor";
    private static final String ALARM_CHANNEL_ID = "ampere-charge-alarm";
    private static final long SAMPLE_INTERVAL = 15 * 60 * 1000L;
    private static final int MAX_TELEMETRY_SAMPLES = 2880;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            recordSample(intent);
        }
    };
    private final Runnable sampleTask = new Runnable() {
        @Override public void run() {
            recordSample();
            handler.postDelayed(this, SAMPLE_INTERVAL);
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        createChannel();
        startForeground(7, notification());
        IntentFilter batteryFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(batteryReceiver, batteryFilter, Context.RECEIVER_NOT_EXPORTED); else registerReceiver(batteryReceiver, batteryFilter);
        recordSample();
        handler.postDelayed(sampleTask, SAMPLE_INTERVAL);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) { return START_STICKY; }

    private void createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.createNotificationChannel(new NotificationChannel(CHANNEL_ID, "Battery monitoring", NotificationManager.IMPORTANCE_LOW));
            NotificationChannel alarm = new NotificationChannel(ALARM_CHANNEL_ID, "Charge alarm", NotificationManager.IMPORTANCE_HIGH);
            alarm.enableVibration(true);
            manager.createNotificationChannel(alarm);
        }
    }

    private Notification notification() {
        return statusNotification(-1, false, 0, 0, 0);
    }

    private Notification statusNotification(int value, boolean isCharging, int currentMa, int temperatureTenths, int voltageMv) {
        Intent launch = new Intent(this, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(this, 0, launch, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? new Notification.Builder(this, CHANNEL_ID) : new Notification.Builder(this);
        String title = value >= 0 ? value + "% · " + (isCharging ? "charging" : "on battery") : "Ampere is monitoring";
        String details = value >= 0 ? (currentMa > 0 ? currentMa + " mA" : "current unavailable") + " · " + (temperatureTenths / 10f) + "°C" + (voltageMv > 0 ? " · " + String.format(Locale.US, "%.2f V", voltageMv / 1000f) : "") : "Battery readings are stored on this device";
        return builder.setSmallIcon(com.ampere.batterylab.R.drawable.ic_launcher)
                .setContentTitle(title)
                .setContentText(details)
                .setSubText("Local battery monitor")
                .setContentIntent(pending)
                .setOngoing(true)
                .setShowWhen(false)
                .build();
    }

    private void recordSample() {
        Intent battery = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        recordSample(battery);
    }

    private void recordSample(Intent battery) {
        if (battery == null) return;
        int raw = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        if (raw < 0 || scale <= 0) return;
        int value = Math.max(0, Math.min(100, Math.round(raw * 100f / scale)));
        android.content.SharedPreferences prefs = getSharedPreferences("ampere-data", Context.MODE_PRIVATE);
        long now = System.currentTimeMillis();
        int status = battery.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN);
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;
        BatteryManager batteryManager = (BatteryManager) getSystemService(BATTERY_SERVICE);
        int microamps = batteryManager == null ? 0 : batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
        if (microamps == Integer.MIN_VALUE || microamps == 0) {
            microamps = batteryManager == null ? 0 : batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE);
        }
        int currentMa = microamps == Integer.MIN_VALUE ? 0 : Math.abs(microamps) / 1000;
        int temperature = battery.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
        int rawChargeCounter = batteryManager == null ? 0 : batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER);
        int chargeCounterMah = rawChargeCounter > 0 ? rawChargeCounter / 1000 : 0;
        PowerManager power = (PowerManager) getSystemService(POWER_SERVICE);
        boolean interactive = power == null || power.isInteractive();
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager != null) notificationManager.notify(7, statusNotification(value, isCharging, currentMa, temperature, battery.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)));
        updateDischargeStats(prefs, value, isCharging, chargeCounterMah, now, interactive);
        updateChargeStats(prefs, isCharging, chargeCounterMah, now, interactive);
        recordSession(prefs, value, isCharging, chargeCounterMah, now);
        recordTelemetrySample(prefs, now, value, isCharging, currentMa, temperature, battery.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0), chargeCounterMah, interactive);
        requestAutomaticBackup(prefs, now);
        int benchmarkCapacity = updateBenchmark(prefs, value, isCharging, chargeCounterMah);
        if (benchmarkCapacity > 0) recordHealthSample(prefs, benchmarkCapacity);
        int limit = prefs.getInt("chargeLimit", 80);
        boolean alarmEnabled = prefs.getBoolean("chargeAlarm", true);
        boolean alarmSent = prefs.getBoolean("chargeAlarmSent", false);
        if (alarmEnabled && isCharging && value >= limit && !alarmSent) {
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager != null) manager.notify(8, alarmNotification(value, limit));
            prefs.edit().putBoolean("chargeAlarmSent", true).apply();
        } else if (!alarmEnabled || !isCharging || value < limit) {
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager != null) manager.cancel(8);
            if (alarmSent) prefs.edit().putBoolean("chargeAlarmSent", false).apply();
        }
        if (now - prefs.getLong("lastSample", 0L) < 5 * 60 * 1000L) return;
        String saved = prefs.getString("history", "");
        ArrayList<Integer> points = new ArrayList<>();
        if (!saved.isEmpty()) for (String point : saved.split(",")) try { points.add(Integer.parseInt(point)); } catch (NumberFormatException ignored) { }
        points.add(value);
        while (points.size() > 48) points.remove(0);
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < points.size(); i++) { if (i > 0) output.append(','); output.append(points.get(i)); }
        String savedLong = prefs.getString("historyLong", "");
        ArrayList<Integer> longPoints = new ArrayList<>();
        if (!savedLong.isEmpty()) for (String point : savedLong.split(",")) try { longPoints.add(Integer.parseInt(point)); } catch (NumberFormatException ignored) { }
        longPoints.add(value);
        while (longPoints.size() > 2880) longPoints.remove(0);
        StringBuilder longOutput = new StringBuilder();
        for (int i = 0; i < longPoints.size(); i++) { if (i > 0) longOutput.append(','); longOutput.append(longPoints.get(i)); }
        prefs.edit().putString("history", output.toString()).putString("historyLong", longOutput.toString()).putLong("lastSample", now).apply();
        updateUsageCounters(prefs, value, now);
    }

    /**
     * Keeps an analysis-ready, local-only time series. It intentionally omits
     * account identifiers, installed-app lists and location data.
     */
    private void recordTelemetrySample(android.content.SharedPreferences prefs, long now, int level,
                                      boolean isCharging, int currentMa, int temperatureTenths,
                                      int voltageMv, int chargeCounterMah, boolean screenOn) {
        long last = prefs.getLong("telemetryLastSampleAt", 0L);
        if (now - last < SAMPLE_INTERVAL) return;
        String saved = prefs.getString("telemetrySamples", "");
        StringBuilder all = new StringBuilder(saved.length() + 96);
        if (!saved.isEmpty()) all.append(saved).append('\n');
        all.append(now).append(',')
                .append(level).append(',')
                .append(isCharging ? 1 : 0).append(',')
                .append(currentMa).append(',')
                .append(String.format(Locale.US, "%.1f", temperatureTenths / 10f)).append(',')
                .append(String.format(Locale.US, "%.3f", voltageMv / 1000f)).append(',')
                .append(chargeCounterMah).append(',')
                .append(screenOn ? 1 : 0);
        String[] rows = all.toString().split("\\n");
        int first = Math.max(0, rows.length - MAX_TELEMETRY_SAMPLES);
        StringBuilder trimmed = new StringBuilder();
        for (int i = first; i < rows.length; i++) {
            if (trimmed.length() > 0) trimmed.append('\n');
            trimmed.append(rows[i]);
        }
        prefs.edit().putString("telemetrySamples", trimmed.toString())
                .putLong("telemetryLastSampleAt", now).apply();
    }

    /** Ask Android's configured backup provider to schedule a background backup.
     *  Android still applies its own throttling and encryption/device settings. */
    private void requestAutomaticBackup(android.content.SharedPreferences prefs, long now) {
        long lastRequest = prefs.getLong("lastBackupRequestAt", 0L);
        if (now - lastRequest < 6L * 60L * 60L * 1000L) return;
        try {
            BackupManager.dataChanged(getPackageName());
            prefs.edit().putLong("lastBackupRequestAt", now).apply();
        } catch (Exception ignored) { }
    }

    private void updateUsageCounters(android.content.SharedPreferences prefs, int level, long now) {
        int previousLevel = prefs.getInt("cycleLastLevel", -1);
        float dischargePercent = prefs.getFloat("dischargePercent", 0f);
        int cycles = prefs.getInt("chargeCycles", 0);
        if (previousLevel >= 0 && level < previousLevel) dischargePercent += previousLevel - level;
        while (dischargePercent >= 100f) {
            cycles++;
            dischargePercent -= 100f;
        }
        PowerManager power = (PowerManager) getSystemService(POWER_SERVICE);
        boolean interactive = power == null || power.isInteractive();
        long lastMonitorSample = prefs.getLong("monitorSampleAt", now);
        long elapsed = lastMonitorSample > 0L ? Math.max(0L, now - lastMonitorSample) : 0L;
        long monitoringMs = prefs.getLong("monitoringMs", 0L) + elapsed;
        long deepSleepMs = prefs.getLong("deepSleepMs", 0L) + (!interactive ? elapsed : 0L);
        long lastScreenSample = prefs.getLong("screenSampleAt", now);
        long screenOnMs = prefs.getLong("screenOnMs", 0L);
        if (interactive && lastScreenSample > 0L) screenOnMs += Math.max(0L, now - lastScreenSample);
        prefs.edit().putInt("cycleLastLevel", level).putFloat("dischargePercent", dischargePercent)
                .putInt("chargeCycles", cycles).putLong("screenOnMs", screenOnMs)
                .putLong("screenSampleAt", now).putLong("monitoringMs", monitoringMs)
                .putLong("deepSleepMs", deepSleepMs).putLong("monitorSampleAt", now).apply();
    }

    private void recordSession(android.content.SharedPreferences prefs, int level, boolean charging, int counterMah, long now) {
        long startedAt = prefs.getLong("monitorSessionStartedAt", 0L);
        boolean previousCharging = prefs.getBoolean("monitorLastCharging", charging);
        int startLevel = prefs.getInt("monitorSessionStartLevel", level);
        int startCounter = prefs.getInt("monitorSessionStartCounterMah", 0);
        if (startedAt == 0L) {
            prefs.edit().putLong("monitorSessionStartedAt", now).putBoolean("monitorLastCharging", charging)
                    .putInt("monitorSessionStartLevel", level).putInt("monitorSessionStartCounterMah", counterMah).apply();
            return;
        }
        if (previousCharging == charging) return;
        long minutes = Math.max(1L, (now - startedAt) / 60000L);
        int change = level - startLevel;
        if (change != 0) {
            int energy = counterMah > 0 && startCounter > 0 ? (previousCharging ? Math.max(0, counterMah - startCounter) : Math.max(0, startCounter - counterMah)) : 0;
            String type = change > 0 ? "Charge" : "Discharge";
            String date = new SimpleDateFormat("MMM d HH:mm", Locale.US).format(new Date(now));
            String entry = type + "," + (change > 0 ? "+" : "") + change + "%," + duration(minutes) + "," + date + "," + startLevel + "," + level + "," + energy;
            String saved = prefs.getString("sessions", "");
            ArrayList<String> sessions = new ArrayList<>();
            if (!saved.isEmpty()) for (String session : saved.split("\\|")) if (!session.isEmpty()) sessions.add(session);
            sessions.add(0, entry);
            while (sessions.size() > 150) sessions.remove(sessions.size() - 1);
            StringBuilder output = new StringBuilder();
            for (String session : sessions) { if (output.length() > 0) output.append('|'); output.append(session); }
            android.content.SharedPreferences.Editor editor = prefs.edit().putString("sessions", output.toString());
            if (change > 0 && energy > 0) editor.putInt("totalChargedMah", prefs.getInt("totalChargedMah", 0) + energy);
            if (change > 0) {
                editor.putInt("lastChargeStartLevel", startLevel)
                        .putInt("lastChargeEndLevel", level)
                        .putInt("lastChargeEnergyMah", energy)
                        .putLong("lastChargeDurationMin", minutes)
                        .putLong("lastChargeStartAt", startedAt)
                        .putLong("lastChargeEndAt", now);
            }
            editor.apply();
            if (change >= 5 && energy > 0) {
                int estimatedCapacity = Math.round(energy * 100f / change);
                if (estimatedCapacity >= 500 && estimatedCapacity <= 20000) {
                    recordHealthSample(prefs, estimatedCapacity);
                }
            }
        }
        prefs.edit().putLong("monitorSessionStartedAt", now).putBoolean("monitorLastCharging", charging)
                .putInt("monitorSessionStartLevel", level).putInt("monitorSessionStartCounterMah", counterMah).apply();
    }

    private void recordHealthSample(android.content.SharedPreferences prefs, int capacityMah) {
        long sessionStartedAt = prefs.getLong("monitorSessionStartedAt", 0L);
        if (prefs.getLong("healthSampleSessionAt", -1L) == sessionStartedAt) return;
        String saved = prefs.getString("healthSamples", "");
        ArrayList<Integer> samples = new ArrayList<>();
        if (!saved.isEmpty()) for (String value : saved.split(",")) try { samples.add(Integer.parseInt(value)); } catch (NumberFormatException ignored) { }
        if (!samples.isEmpty() && samples.get(samples.size() - 1) == capacityMah) return;
        samples.add(capacityMah);
        while (samples.size() > 150) samples.remove(0);
        StringBuilder output = new StringBuilder();
        for (Integer sample : samples) { if (output.length() > 0) output.append(','); output.append(sample); }
        prefs.edit().putString("healthSamples", output.toString()).putLong("healthSampleSessionAt", sessionStartedAt).apply();
    }

    /**
     * Estimates full capacity from charge-counter increase during a deliberate
     * low-to-full run. BATTERY_PROPERTY_CHARGE_COUNTER is remaining charge,
     * not the factory capacity, so recording it at 95% would be incorrect.
     */
    private int updateBenchmark(android.content.SharedPreferences prefs, int level, boolean charging, int counterMah) {
        if (!prefs.getBoolean("benchmarkActive", false)) return 0;
        int startLevel = prefs.getInt("benchmarkStartLevel", 100);
        if (startLevel > 25 || !charging || counterMah <= 0) return 0;
        int lastCounter = prefs.getInt("benchmarkChargeLastCounterMah", 0);
        int startCounter = prefs.getInt("benchmarkStartCounterMah", 0);
        int added = prefs.getInt("benchmarkChargeAddedMah", 0);
        int baseline = lastCounter > 0 ? lastCounter : startCounter;
        if (baseline > 0 && counterMah > baseline) added += counterMah - baseline;
        android.content.SharedPreferences.Editor editor = prefs.edit()
                .putInt("benchmarkChargeLastCounterMah", counterMah)
                .putInt("benchmarkChargeAddedMah", added);
        if (startCounter <= 0) editor.putInt("benchmarkStartCounterMah", counterMah);
        if (level >= 95 && added > 0) {
            int percentGained = Math.max(1, level - startLevel);
            int capacity = Math.round(added * 100f / percentGained);
            if (capacity >= 500 && capacity <= 20000) {
                editor.putInt("benchmarkCapacityMah", capacity).putBoolean("benchmarkActive", false);
                editor.apply();
                return capacity;
            }
        }
        editor.apply();
        return 0;
    }

    private void updateDischargeStats(android.content.SharedPreferences prefs, int level, boolean charging, int counterMah, long now, boolean interactive) {
        boolean previousCharging = prefs.getBoolean("monitorLastCharging", charging);
        if (charging && !previousCharging) {
            prefs.edit().putLong("lastDischargeScreenOnMs", prefs.getLong("dischargeScreenOnMs", 0L))
                    .putLong("lastDischargeScreenOffMs", prefs.getLong("dischargeScreenOffMs", 0L))
                    .putFloat("lastDischargeScreenOnPercent", prefs.getFloat("dischargeScreenOnPercent", 0f))
                    .putFloat("lastDischargeScreenOffPercent", prefs.getFloat("dischargeScreenOffPercent", 0f))
                    .putInt("lastDischargeMah", prefs.getInt("dischargeMah", 0))
                    .putLong("lastDischargeDeepSleepMs", prefs.getLong("dischargeDeepSleepMs", 0L))
                    .putLong("lastDischargeStartAt", prefs.getLong("dischargeStartAt", 0L))
                    .putLong("lastDischargeEndAt", now)
                    .putInt("lastDischargeEndLevel", level)
                    .remove("dischargeLastLevel").remove("dischargeLastCounterMah")
                    .putLong("dischargeScreenOnMs", 0L).putLong("dischargeScreenOffMs", 0L)
                    .putFloat("dischargeScreenOnPercent", 0f).putFloat("dischargeScreenOffPercent", 0f)
                    .putInt("dischargeMah", 0).putLong("dischargeLastAt", now)
                    .putLong("dischargeDeepSleepMs", 0L).apply();
            return;
        }
        if (!charging && previousCharging) {
            prefs.edit().putInt("dischargeLastLevel", level).putInt("dischargeLastCounterMah", counterMah)
                    .putLong("dischargeLastAt", now).putLong("dischargeScreenOnMs", 0L)
                    .putLong("dischargeScreenOffMs", 0L).putFloat("dischargeScreenOnPercent", 0f)
                    .putFloat("dischargeScreenOffPercent", 0f).putInt("dischargeMah", 0)
                    .putLong("dischargeStartAt", now).apply();
            prefs.edit().putLong("dischargeDeepSleepMs", 0L).apply();
            return;
        }
        if (charging) return;
        int previousLevel = prefs.getInt("dischargeLastLevel", level);
        int previousCounter = prefs.getInt("dischargeLastCounterMah", counterMah);
        long lastAt = prefs.getLong("dischargeLastAt", now);
        long elapsed = Math.max(0L, now - lastAt);
        float onPercent = prefs.getFloat("dischargeScreenOnPercent", 0f);
        float offPercent = prefs.getFloat("dischargeScreenOffPercent", 0f);
        if (level < previousLevel) {
            if (interactive) onPercent += previousLevel - level; else offPercent += previousLevel - level;
        }
        int energy = prefs.getInt("dischargeMah", 0);
        if (counterMah > 0 && previousCounter > counterMah) energy += previousCounter - counterMah;
        long onMs = prefs.getLong("dischargeScreenOnMs", 0L) + (interactive ? elapsed : 0L);
        long offMs = prefs.getLong("dischargeScreenOffMs", 0L) + (interactive ? 0L : elapsed);
        long deepSleepMs = prefs.getLong("dischargeDeepSleepMs", 0L) + (interactive ? 0L : elapsed);
        prefs.edit().putInt("dischargeLastLevel", level).putInt("dischargeLastCounterMah", counterMah)
                .putLong("dischargeLastAt", now).putFloat("dischargeScreenOnPercent", onPercent)
                .putFloat("dischargeScreenOffPercent", offPercent).putInt("dischargeMah", energy)
                .putLong("dischargeScreenOnMs", onMs).putLong("dischargeScreenOffMs", offMs)
                .putLong("dischargeDeepSleepMs", deepSleepMs).apply();
    }

    private void updateChargeStats(android.content.SharedPreferences prefs, boolean charging, int counterMah, long now, boolean interactive) {
        boolean previousCharging = prefs.getBoolean("monitorLastCharging", charging);
        if (!charging && previousCharging) return;
        if (charging && !previousCharging) {
            prefs.edit().putInt("chargeLastCounterMah", counterMah).putLong("chargeLastAt", now)
                    .putLong("chargeScreenOnMs", 0L).putLong("chargeScreenOffMs", 0L)
                    .putInt("chargeScreenOnMah", 0).putInt("chargeScreenOffMah", 0)
                    .putBoolean("chargeAlarmSent", false).apply();
            return;
        }
        if (!charging) return;
        int previousCounter = prefs.getInt("chargeLastCounterMah", counterMah);
        long lastAt = prefs.getLong("chargeLastAt", now);
        long elapsed = Math.max(0L, now - lastAt);
        int added = counterMah > previousCounter && previousCounter > 0 ? counterMah - previousCounter : 0;
        long onMs = prefs.getLong("chargeScreenOnMs", 0L) + (interactive ? elapsed : 0L);
        long offMs = prefs.getLong("chargeScreenOffMs", 0L) + (interactive ? 0L : elapsed);
        int onMah = prefs.getInt("chargeScreenOnMah", 0) + (interactive ? added : 0);
        int offMah = prefs.getInt("chargeScreenOffMah", 0) + (interactive ? 0 : added);
        prefs.edit().putInt("chargeLastCounterMah", counterMah).putLong("chargeLastAt", now)
                .putLong("chargeScreenOnMs", onMs).putLong("chargeScreenOffMs", offMs)
                .putInt("chargeScreenOnMah", onMah).putInt("chargeScreenOffMah", offMah).apply();
    }

    private String duration(long minutes) {
        return minutes >= 60 ? (minutes / 60) + "h " + (minutes % 60) + "m" : minutes + " min";
    }

    private Notification alarmNotification(int value, int limit) {
        Intent launch = new Intent(this, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(this, 1, launch, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? new Notification.Builder(this, ALARM_CHANNEL_ID) : new Notification.Builder(this);
        return builder.setSmallIcon(com.ampere.batterylab.R.drawable.ic_launcher)
                .setContentTitle("Charge limit reached")
                .setContentText("Battery is at " + value + "% · configured limit " + limit + "%")
                .setContentIntent(pending)
                .setAutoCancel(true)
                .build();
    }

    @Override public void onDestroy() {
        try { unregisterReceiver(batteryReceiver); } catch (IllegalArgumentException ignored) { }
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
