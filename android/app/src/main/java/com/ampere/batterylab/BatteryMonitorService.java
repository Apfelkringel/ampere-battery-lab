package com.ampere.batterylab;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.AppOpsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.app.backup.BackupManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Lightweight, local-only recorder. It samples the public Android battery
 * broadcast at the configured interval and keeps short and 30-day local histories.
 */
public class BatteryMonitorService extends Service {
    private static final String CHANNEL_ID = "ampere-monitor";
    private static final String ALARM_CHANNEL_ID = "ampere-charge-alarm";
    private static final long CHARGING_STATE_CONFIRMATION_MS = 2500L;
    private Handler handler;
    private HandlerThread monitorThread;
    private boolean transitionCheckScheduled;
    private Boolean powerConnectedHint;
    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (handler == null) return;
            handler.post(() -> {
                String action = intent == null ? null : intent.getAction();
                if (Intent.ACTION_POWER_CONNECTED.equals(action)
                        || Intent.ACTION_POWER_DISCONNECTED.equals(action)) {
                    // Power broadcasts are authoritative for the physical cable
                    // edge. Keep that hint for the following battery snapshots so
                    // a stale/intermediate EXTRA_STATUS cannot flip the session.
                    powerConnectedHint = Intent.ACTION_POWER_CONNECTED.equals(action);
                    recordSample();
                } else {
                    recordSample(intent);
                }
            });
        }
    };
    private boolean screenInteractive;
    private final BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (handler == null) return;
            handler.post(() -> {
                if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                    screenInteractive = false;
                } else if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                    if (!screenInteractive) recordScreenWakeup();
                    screenInteractive = true;
                }
            });
        }
    };
    private final Runnable sampleTask = new Runnable() {
        @Override public void run() {
            recordSample();
            handler.postDelayed(this, sampleInterval());
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        MainActivity.migrateTelemetryPrefs(this);
        monitorThread = new HandlerThread("ampere-battery-monitor", android.os.Process.THREAD_PRIORITY_BACKGROUND);
        monitorThread.start();
        handler = new Handler(monitorThread.getLooper());
        createChannel();
        startForeground(7, notification());
        IntentFilter batteryFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        batteryFilter.addAction(Intent.ACTION_POWER_CONNECTED);
        batteryFilter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(batteryReceiver, batteryFilter, Context.RECEIVER_NOT_EXPORTED); else registerReceiver(batteryReceiver, batteryFilter);
        PowerManager power = (PowerManager) getSystemService(POWER_SERVICE);
        screenInteractive = power == null || power.isInteractive();
        IntentFilter screenFilter = new IntentFilter();
        screenFilter.addAction(Intent.ACTION_SCREEN_ON);
        screenFilter.addAction(Intent.ACTION_SCREEN_OFF);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(screenReceiver, screenFilter, Context.RECEIVER_NOT_EXPORTED); else registerReceiver(screenReceiver, screenFilter);
        handler.post(this::recordSample);
        handler.postDelayed(sampleTask, sampleInterval());
        UpdateChecker.checkInBackground(this);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) { return START_STICKY; }

    private void createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.createNotificationChannel(new NotificationChannel(CHANNEL_ID, "Akkuüberwachung", NotificationManager.IMPORTANCE_LOW));
            NotificationChannel alarm = new NotificationChannel(ALARM_CHANNEL_ID, "Ladealarm", NotificationManager.IMPORTANCE_HIGH);
            alarm.enableVibration(true);
            manager.createNotificationChannel(alarm);
        }
    }

    private Notification notification() {
        return statusNotification(-1, false, 0, 0, 0, BatteryManager.BATTERY_HEALTH_UNKNOWN, -1, 0, 0);
    }

    private Notification statusNotification(int value, boolean isCharging, int currentMa, int temperatureTenths,
                                            int voltageMv, int platformHealth, int capacityLevel, int chargingStatus,
                                            int maxChargingPowerMilliwatts) {
        Intent launch = new Intent(this, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(this, 0, launch, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? new Notification.Builder(this, CHANNEL_ID) : new Notification.Builder(this);
        String title = value >= 0 ? value + "% · " + (isCharging ? "Laden" : "Akkubetrieb") : "Ampere überwacht den Akku";
        int currentMagnitudeMa = Math.abs(currentMa);
        String temperatureText = temperatureTenths > 0
                ? String.format(Locale.US, "%.1f°C", temperatureTenths / 10f)
                : "Temperatur nicht verfügbar";
        String details = value >= 0 ? (currentMagnitudeMa > 0 ? currentMagnitudeMa + " mA" : "Strom nicht verfügbar") + " · " + temperatureText + (voltageMv > 0 ? " · " + String.format(Locale.US, "%.2f V", voltageMv / 1000f) : "") : "Akkumesswerte werden auf diesem Gerät gespeichert";
        if (value >= 0) {
            android.content.SharedPreferences prefs = getSharedPreferences("ampere-data", MODE_PRIVATE);
            int design = BatteryCapacity.designCapacityMah(this);
            int health = BatteryHealth.percent(this, prefs, design);
            int capacity = BatteryHealth.estimatedCapacityMah(this, prefs, design);
            details += "\n" + (isCharging ? "Laden erkannt" : "Bildschirm- und Hintergrundverbrauch lokal erfasst")
                    + " · Android-Zustand " + BatteryPlatformHealth.label(platformHealth)
                    + (BatteryCapacityLevel.isAvailable(capacityLevel)
                    ? " · Kapazitätsniveau " + BatteryCapacityLevel.label(capacityLevel) : "")
                    + (isCharging && BatteryChargingState.isSpecial(chargingStatus)
                    ? " · Ladeprofil " + BatteryChargingState.label(chargingStatus) : "")
                    + (isCharging && maxChargingPowerMilliwatts > 0
                    ? " · " + BatteryChargerCapability.label(maxChargingPowerMilliwatts) : "")
                    + (health > 0 ? " · Gesundheit " + health + "%" : "")
                    + (capacity > 0 ? " · Schätzung " + capacity + " mAh" : "");
        }
        return builder.setSmallIcon(com.ampere.batterylab.R.drawable.ic_launcher)
                .setContentTitle(title)
                .setContentText(details)
                .setStyle(new Notification.BigTextStyle().bigText(details))
                .setSubText("Lokale Akkuüberwachung")
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
        int value = BatteryLevel.percent(raw, scale);
        if (value < 0) return;
        android.content.SharedPreferences prefs = getSharedPreferences("ampere-data", Context.MODE_PRIVATE);
        android.content.SharedPreferences telemetryPrefs = getSharedPreferences("ampere-telemetry", Context.MODE_PRIVATE);
        long now = System.currentTimeMillis();
        int status = battery.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN);
        int plugged = battery.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
        boolean detectedCharging = powerConnectedHint != null
                ? powerConnectedHint : BatteryState.isCharging(status, plugged);
        Boolean stableCharging;
        if (powerConnectedHint != null) {
            prefs.edit().remove("pendingChargingState").remove("pendingChargingSince").apply();
            stableCharging = detectedCharging;
        } else {
            stableCharging = stabilizeChargingState(prefs, detectedCharging, now);
        }
        if (stableCharging == null) return;
        boolean isCharging = stableCharging;
        BatteryManager batteryManager = (BatteryManager) getSystemService(BATTERY_SERVICE);
        int currentMa = BatteryCurrent.milliAmps(batteryManager);
        int signedCurrentMa = currentMa == 0 ? 0 : (isCharging ? currentMa : -currentMa);
        int temperature = BatteryTemperature.normalizeTenths(
                battery.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0));
        int voltageMv = BatteryVoltage.normalizeMilliVolts(
                battery.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0));
        long rawChargeCounterUah = readChargeCounterUah(batteryManager);
        int chargeCounterMah = rawChargeCounterUah > 0
                ? (int) Math.min(Integer.MAX_VALUE, Math.round(rawChargeCounterUah / 1000d)) : 0;
        BatteryCycleCount.Reading cycleReading = BatteryCycleCount.read(battery);
        int systemCycleCount = cycleReading == null ? -1 : cycleReading.cycles;
        if (cycleReading != null) {
            prefs.edit().putInt("systemCycleCount", cycleReading.cycles)
                    .putString("systemCycleCountSource", cycleReading.source).apply();
        }
        PowerManager power = (PowerManager) getSystemService(POWER_SERVICE);
        boolean interactive = power == null || power.isInteractive();
        long deepSleepDeltaMs = recordDeepSleepClock(prefs);
        String foregroundPackage = foregroundPackage(now);
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager != null) notificationManager.notify(7, statusNotification(value, isCharging, signedCurrentMa, temperature,
                voltageMv,
                battery.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN),
                BatteryCapacityLevel.fromIntent(battery),
                BatteryChargingState.fromIntent(battery),
                BatteryChargerCapability.maxPowerMilliwatts(battery)));
        BatteryWidgetProvider.updateAll(this);
        BatteryQuickSettingsService.requestRefresh(this);
        updateSinceFullStats(prefs, value, isCharging, chargeCounterMah, currentMa, now, interactive, deepSleepDeltaMs);
        updateDischargeStats(prefs, value, isCharging, chargeCounterMah, currentMa, now, interactive, deepSleepDeltaMs);
        updateChargeStats(prefs, value, isCharging, chargeCounterMah, currentMa, now, interactive, plugged);
        recordSession(prefs, value, isCharging, chargeCounterMah, now);
        recordTelemetrySample(telemetryPrefs, now, value, isCharging, signedCurrentMa, temperature, voltageMv, chargeCounterMah, interactive, foregroundPackage, systemCycleCount, plugged);
        requestAutomaticBackup(prefs, now);
        UpdateChecker.checkInBackground(this);
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
        boolean temperatureAlarm = prefs.getBoolean("temperatureAlarm", true);
        int temperatureThreshold = BatteryTemperatureAlarm.normalizeThreshold(
                prefs.getInt("temperatureAlarmThresholdTenths", BatteryTemperatureAlarm.DEFAULT_THRESHOLD_TENTHS));
        boolean temperatureSent = prefs.getBoolean("temperatureAlarmSent", false);
        if (BatteryTemperatureAlarm.shouldAlert(temperature, temperatureThreshold, temperatureAlarm, temperatureSent)) {
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager != null) manager.notify(9, temperatureAlarmNotification(temperature, temperatureThreshold));
            prefs.edit().putBoolean("temperatureAlarmSent", true).apply();
        } else if (!temperatureAlarm || BatteryTemperatureAlarm.shouldReset(temperature, temperatureThreshold)) {
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager != null) manager.cancel(9);
            if (temperatureSent) prefs.edit().putBoolean("temperatureAlarmSent", false).apply();
        }
        if (now - prefs.getLong("lastSample", 0L) < sampleInterval()) return;
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
        while (longPoints.size() > longHistoryRetentionSamples()) longPoints.remove(0);
        StringBuilder longOutput = new StringBuilder();
        for (int i = 0; i < longPoints.size(); i++) { if (i > 0) longOutput.append(','); longOutput.append(longPoints.get(i)); }
        prefs.edit().putString("history", output.toString()).putString("historyLong", longOutput.toString()).putLong("lastSample", now).apply();
        updateUsageCounters(prefs, value, isCharging, now);
        updateEstimatedCycles(prefs, rawChargeCounterUah, isCharging);
    }

    private long readChargeCounterUah(BatteryManager batteryManager) {
        return BatteryChargeCounter.readMicroampereHours(batteryManager);
    }

    private void updateEstimatedCycles(android.content.SharedPreferences prefs, long currentCounterUah,
                                       boolean charging) {
        if (currentCounterUah <= 0L) return;
        int designMah = BatteryCapacity.designCapacityMah(this);
        long previousCounterUah = prefs.getLong("estimatedCycleLastCounterUah", -1L);
        float fraction = prefs.getFloat("estimatedCycleFraction", 0f);
        float updated = BatteryCycleEstimator.addChargedFraction(fraction, previousCounterUah,
                currentCounterUah, charging, designMah);
        int completed = BatteryCycleEstimator.completedCycles(updated);
        int cycles = Math.max(0, prefs.getInt("estimatedCycleCount", 0)) + completed;
        prefs.edit().putLong("estimatedCycleLastCounterUah", currentCounterUah)
                .putFloat("estimatedCycleFraction", Math.max(0f, updated - completed))
                .putInt("estimatedCycleCount", cycles).apply();
    }

    /**
     * Battery and power broadcasts can arrive as a short sequence of
     * contradictory snapshots while a cable is inserted or removed. Keep the
     * previous state until the new state survives a small confirmation window;
     * the delayed read makes this work even when Android sends only one event.
     */
    private Boolean stabilizeChargingState(android.content.SharedPreferences prefs, boolean detectedCharging, long now) {
        boolean stableCharging = prefs.getBoolean("monitorLastCharging", detectedCharging);
        boolean hasPending = prefs.contains("pendingChargingState");
        if (detectedCharging == stableCharging) {
            if (hasPending) prefs.edit().remove("pendingChargingState").remove("pendingChargingSince").apply();
            return stableCharging;
        }
        boolean pendingCharging = hasPending && prefs.getBoolean("pendingChargingState", detectedCharging);
        long pendingSince = prefs.getLong("pendingChargingSince", 0L);
        if (!hasPending || pendingCharging != detectedCharging) {
            prefs.edit().putBoolean("pendingChargingState", detectedCharging).putLong("pendingChargingSince", now).apply();
            scheduleTransitionCheck();
            return null;
        }
        if (now - pendingSince < CHARGING_STATE_CONFIRMATION_MS) {
            scheduleTransitionCheck();
            return null;
        }
        prefs.edit().remove("pendingChargingState").remove("pendingChargingSince").apply();
        return detectedCharging;
    }

    private void scheduleTransitionCheck() {
        if (transitionCheckScheduled) return;
        transitionCheckScheduled = true;
        handler.postDelayed(() -> {
            transitionCheckScheduled = false;
            recordSample();
        }, CHARGING_STATE_CONFIRMATION_MS);
    }

    /** Counts screen wake events as a transparent, device-independent wakeup proxy. */
    private void recordScreenWakeup() {
        android.content.SharedPreferences prefs = getSharedPreferences("ampere-data", MODE_PRIVATE);
        if (!prefs.getBoolean("monitorLastCharging", false)) {
            prefs.edit().putInt("dischargeWakeups", prefs.getInt("dischargeWakeups", 0) + 1).apply();
        }
        if (prefs.getBoolean("sinceFullActive", false)) {
            prefs.edit().putInt("sinceFullWakeups", prefs.getInt("sinceFullWakeups", 0) + 1).apply();
        }
    }

    /**
     * Measures actual system suspend time. elapsedRealtime includes deep sleep,
     * while uptimeMillis stops during it. The persisted clock pair also lets a
     * service restart continue the current session; a reboot resets the pair.
     */
    private long recordDeepSleepClock(android.content.SharedPreferences prefs) {
        long elapsed = SystemClock.elapsedRealtime();
        long uptime = SystemClock.uptimeMillis();
        long previousElapsed = prefs.getLong("deepSleepClockElapsed", -1L);
        long previousUptime = prefs.getLong("deepSleepClockUptime", -1L);
        long delta = 0L;
        if (previousElapsed >= 0L && previousUptime >= 0L
                && elapsed >= previousElapsed && uptime >= previousUptime) {
            delta = Math.max(0L, (elapsed - previousElapsed) - (uptime - previousUptime));
        }
        prefs.edit().putLong("deepSleepClockElapsed", elapsed)
                .putLong("deepSleepClockUptime", uptime)
                .putLong("deepSleepMs", prefs.getLong("deepSleepMs", 0L) + delta)
                .apply();
        return delta;
    }

    /**
     * Keeps an analysis-ready, local-only time series. It intentionally omits
     * account identifiers, installed-app lists and location data.
     */
    private void recordTelemetrySample(android.content.SharedPreferences prefs, long now, int level,
                                      boolean isCharging, int currentMa, int temperatureTenths,
                                      int voltageMv, int chargeCounterMah, boolean screenOn,
                                      String foregroundPackage, int systemCycleCount, int plugged) {
        long last = prefs.getLong("telemetryLastSampleAt", 0L);
        if (now - last < sampleInterval()) return;
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
                .append(screenOn ? 1 : 0).append(',')
                .append(foregroundPackage == null ? "" : foregroundPackage).append(',')
                .append(systemCycleCount).append(',')
                .append(plugged);
        String[] rows = all.toString().split("\\n");
        int first = Math.max(0, rows.length - telemetryRetentionSamples());
        StringBuilder trimmed = new StringBuilder();
        for (int i = first; i < rows.length; i++) {
            if (trimmed.length() > 0) trimmed.append('\n');
            trimmed.append(rows[i]);
        }
        prefs.edit().putString("telemetrySamples", trimmed.toString())
                .putLong("telemetryLastSampleAt", now).apply();
    }

    private long sampleInterval() {
        android.content.SharedPreferences prefs = getSharedPreferences("ampere-data", Context.MODE_PRIVATE);
        int minutes = prefs.getInt("samplingIntervalMin", 15);
        if (minutes != 5 && minutes != 15 && minutes != 30 && minutes != 60) minutes = 15;
        return minutes * 60L * 1000L;
    }

    /** Keeps approximately thirty days of telemetry at every supported rate. */
    private int telemetryRetentionSamples() {
        long thirtyDaysMs = 30L * 24L * 60L * 60L * 1000L;
        return Math.max(1, (int) Math.ceil(thirtyDaysMs / (double) sampleInterval()));
    }

    private int longHistoryRetentionSamples() {
        return Math.max(1, (int) Math.ceil(30L * 24L * 60L * 60L * 1000L / (double) sampleInterval()));
    }

    /** Cap stale integration while honoring the selected sampling interval. */
    private long accountingIntervalCapMs() {
        return Math.max(30L * 60L * 1000L, sampleInterval());
    }

    /**
     * Returns the most recently resumed package when Usage Access is enabled.
     * Android may omit events while the device is idle or when the vendor
     * restricts usage history, so this value is intentionally best-effort.
     */
    private String foregroundPackage(long end) {
        try {
            AppOpsManager ops = (AppOpsManager) getSystemService(APP_OPS_SERVICE);
            if (ops == null || ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(), getPackageName()) != AppOpsManager.MODE_ALLOWED) return "";
            UsageStatsManager manager = (UsageStatsManager) getSystemService(USAGE_STATS_SERVICE);
            if (manager == null) return "";
            UsageEvents events = manager.queryEvents(end - 2L * 60L * 60L * 1000L, end);
            if (events == null) return "";
            UsageEvents.Event event = new UsageEvents.Event();
            String current = "";
            while (events.hasNextEvent()) {
                events.getNextEvent(event);
                int type = event.getEventType();
                if (type == UsageEvents.Event.MOVE_TO_FOREGROUND
                        || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                        && type == UsageEvents.Event.ACTIVITY_RESUMED)) {
                    current = event.getPackageName();
                }
            }
            return getPackageName().equals(current) ? "" : current;
        } catch (RuntimeException ignored) {
            return "";
        }
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

    private void updateUsageCounters(android.content.SharedPreferences prefs, int level, boolean charging, long now) {
        int previousLevel = prefs.getInt("cycleLastLevel", -1);
        float dischargePercent = prefs.getFloat("dischargePercent", 0f);
        int cycles = prefs.getInt("chargeCycles", 0);
        // Do not interpret an OEM recalibration or a brief counter reversal
        // while plugged in as battery wear. The last level is still persisted
        // below so the next real discharge starts from the current value.
        dischargePercent = BatteryCycleAccumulator.addDischargePercent(dischargePercent, previousLevel, level, charging);
        int completedCycles = BatteryCycleAccumulator.completedCycles(dischargePercent);
        cycles += completedCycles;
        dischargePercent -= completedCycles * 100f;
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

    /** Tracks battery use after the most recent observed full charge. */
    private void updateSinceFullStats(android.content.SharedPreferences prefs, int level, boolean charging,
                                      int counterMah, int currentMa, long now, boolean interactive,
                                      long deepSleepDeltaMs) {
        boolean active = prefs.getBoolean("sinceFullActive", false);
        if (charging && level >= 99 && !active) {
            prefs.edit().putBoolean("sinceFullActive", true).putLong("sinceFullStartAt", now)
                    .putInt("sinceFullStartLevel", level).putInt("sinceFullLastLevel", level)
                    .putInt("sinceFullLastCounterMah", counterMah).putLong("sinceFullLastAt", now)
                    .putFloat("sinceFullPercent", 0f).putInt("sinceFullMah", 0)
                    .putLong("sinceFullScreenOnMs", 0L).putLong("sinceFullScreenOffMs", 0L)
                    .putLong("sinceFullDeepSleepMs", 0L).putInt("sinceFullWakeups", 0).apply();
            return;
        }
        if (!active) return;
        int previousLevel = prefs.getInt("sinceFullLastLevel", level);
        int previousCounter = prefs.getInt("sinceFullLastCounterMah", counterMah);
        long lastAt = prefs.getLong("sinceFullLastAt", now);
        long elapsed = Math.min(accountingIntervalCapMs(), Math.max(0L, now - lastAt));
        float usedPercent = prefs.getFloat("sinceFullPercent", 0f);
        if (!charging && level < previousLevel) usedPercent += previousLevel - level;
        int usedMah = prefs.getInt("sinceFullMah", 0);
        if (!charging && counterMah > 0 && previousCounter > 0 && previousCounter > counterMah) {
            usedMah += previousCounter - counterMah;
        } else if (!charging && counterMah <= 0 && currentMa > 0) {
            usedMah += Math.round(currentMa * elapsed / 3600000f);
        }
        long screenOnMs = prefs.getLong("sinceFullScreenOnMs", 0L) + ((!charging && interactive) ? elapsed : 0L);
        long screenOffMs = prefs.getLong("sinceFullScreenOffMs", 0L) + ((!charging && !interactive) ? elapsed : 0L);
        long deepSleepMs = prefs.getLong("sinceFullDeepSleepMs", 0L) + (!charging ? deepSleepDeltaMs : 0L);
        prefs.edit().putInt("sinceFullLastLevel", level).putInt("sinceFullLastCounterMah", counterMah)
                .putLong("sinceFullLastAt", now).putFloat("sinceFullPercent", usedPercent)
                .putInt("sinceFullMah", usedMah).putLong("sinceFullScreenOnMs", screenOnMs)
                .putLong("sinceFullScreenOffMs", screenOffMs).putLong("sinceFullDeepSleepMs", deepSleepMs).apply();
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
        int energy = counterMah > 0 && startCounter > 0 ? (previousCharging ? Math.max(0, counterMah - startCounter) : Math.max(0, startCounter - counterMah)) : 0;
        // The state transition is authoritative, but a level snapshot can be
        // noisy while the device is under load or held at an OEM charge limit.
        // Resolve that snapshot against measured energy before deciding whether
        // a history row has enough evidence.
        String type = previousCharging ? "Charge" : "Discharge";
        int screenOnValue;
        int screenOffValue;
        long screenOnMs;
        long screenOffMs;
        long deepSleepMs;
        String chargerSource;
        if (previousCharging) {
            screenOnValue = prefs.getInt("lastChargeScreenOnMah", 0);
            screenOffValue = prefs.getInt("lastChargeScreenOffMah", 0);
            screenOnMs = prefs.getLong("lastChargeScreenOnMs", 0L);
            screenOffMs = prefs.getLong("lastChargeScreenOffMs", 0L);
            deepSleepMs = 0L;
            chargerSource = chargerLabel(prefs.getInt("lastChargePlugged", prefs.getInt("chargePlugged", 0)));
            if (energy <= 0) energy = screenOnValue + screenOffValue;
        } else {
            float onPercent = prefs.getFloat("lastDischargeScreenOnPercent", 0f);
            float offPercent = prefs.getFloat("lastDischargeScreenOffPercent", 0f);
            screenOnValue = Math.round(onPercent * 10f);
            screenOffValue = Math.round(offPercent * 10f);
            screenOnMs = prefs.getLong("lastDischargeScreenOnMs", 0L);
            screenOffMs = prefs.getLong("lastDischargeScreenOffMs", 0L);
            deepSleepMs = prefs.getLong("lastDischargeDeepSleepMs", 0L);
            chargerSource = "Battery";
            if (energy <= 0) energy = prefs.getInt("lastDischargeMah", 0);
        }
        int designCapacity = BatteryCapacity.designCapacityMah(this);
        int effectiveChange = BatterySessionRules.effectiveChange(
                change, energy, designCapacity, previousCharging);
        // A session is a percentage-based history item. Never create a 0% row
        // or silently label a contradictory transition as a real session.
        if (effectiveChange == 0) {
            if (previousCharging && energy > 0) {
                prefs.edit().putInt("totalChargedMah", prefs.getInt("totalChargedMah", 0) + energy).apply();
            }
            prefs.edit().putLong("monitorSessionStartedAt", now).putBoolean("monitorLastCharging", charging)
                    .putInt("monitorSessionStartLevel", level).putInt("monitorSessionStartCounterMah", counterMah).apply();
            return;
        }
        String date = new SimpleDateFormat("dd.MM. HH:mm", Locale.GERMANY).format(new Date(now));
        float cycleEquivalent = energy > 0 && designCapacity > 0 ? energy / (float) designCapacity : Math.abs(change) / 100f;
        int screenWakeups = previousCharging ? 0 : prefs.getInt("lastDischargeWakeups", prefs.getInt("dischargeWakeups", 0));
        String entry = type + "," + (effectiveChange > 0 ? "+" : "") + effectiveChange + "%," + duration(minutes) + "," + date + "," + startLevel + "," + level + "," + energy + "," + String.format(Locale.US, "%.2f", cycleEquivalent)
                + "," + screenOnValue + "," + screenOffValue + "," + (screenOnMs / 60000L) + "," + (screenOffMs / 60000L)
                + "," + (deepSleepMs / 60000L) + "," + chargerSource + "," + startedAt + "," + now + "," + screenWakeups;
        String saved = prefs.getString("sessions", "");
        ArrayList<String> sessions = new ArrayList<>();
        if (!saved.isEmpty()) for (String session : saved.split("\\|")) if (!session.isEmpty()) sessions.add(session);
        sessions.add(0, entry);
        while (sessions.size() > 150) sessions.remove(sessions.size() - 1);
        StringBuilder output = new StringBuilder();
        for (String session : sessions) { if (output.length() > 0) output.append('|'); output.append(session); }
        android.content.SharedPreferences.Editor editor = prefs.edit().putString("sessions", output.toString());
        if (previousCharging && energy > 0) editor.putInt("totalChargedMah", prefs.getInt("totalChargedMah", 0) + energy);
        if (previousCharging) {
            String healthReason;
            if (effectiveChange < 5) healthReason = "Zu geringe Akkustandänderung (mindestens 5 % nötig)";
            else if (energy <= 0) healthReason = "Energiezähler/Strom nicht verfügbar";
            else healthReason = "Wird in den nächsten Gesundheitsdurchschnitt einbezogen";
            editor.putInt("lastChargeStartLevel", startLevel)
                    .putInt("lastChargeEndLevel", level)
                    .putInt("lastChargeEnergyMah", energy)
                    .putLong("lastChargeDurationMin", minutes)
                    .putLong("lastChargeStartAt", startedAt)
                    .putLong("lastChargeEndAt", now)
                    .putString("lastChargeHealthReason", healthReason);
        }
        editor.apply();
        if (previousCharging && effectiveChange >= 5 && energy > 0) {
            int estimatedCapacity = Math.round(energy * 100f / effectiveChange);
            if (estimatedCapacity >= 500 && estimatedCapacity <= 20000) {
                recordHealthSample(prefs, estimatedCapacity);
            }
        }
        prefs.edit().putLong("monitorSessionStartedAt", now).putBoolean("monitorLastCharging", charging)
                .putInt("monitorSessionStartLevel", level).putInt("monitorSessionStartCounterMah", counterMah).apply();
    }

    private void recordHealthSample(android.content.SharedPreferences prefs, int capacityMah) {
        long sessionStartedAt = prefs.getLong("monitorSessionStartedAt", 0L);
        if (prefs.getLong("healthSampleSessionAt", -1L) == sessionStartedAt) return;
        String saved = prefs.getString("healthSamples", "");
        ArrayList<Integer> samples = BatteryHealth.parseSamples(saved);
        if (!samples.isEmpty() && samples.get(samples.size() - 1) == capacityMah) return;
        samples.add(capacityMah);
        while (samples.size() > 150) samples.remove(0);
        prefs.edit().putString("healthSamples", BatteryHealth.serializeSamples(samples))
                .putLong("healthSampleSessionAt", sessionStartedAt).apply();
    }

    /**
     * Estimates full capacity from charge-counter increase during a deliberate
     * low-to-full run. BATTERY_PROPERTY_CHARGE_COUNTER is remaining charge,
     * not the factory capacity, so recording it at 95% would be incorrect.
     */
    private int updateBenchmark(android.content.SharedPreferences prefs, int level, boolean charging, int counterMah) {
        if (!prefs.getBoolean("benchmarkActive", false)) return 0;
        int startLevel = prefs.getInt("benchmarkStartLevel", 100);
        if (startLevel > 25 || !charging) return 0;
        int lastCounter = prefs.getInt("benchmarkChargeLastCounterMah", 0);
        int startCounter = prefs.getInt("benchmarkStartCounterMah", 0);
        int added = prefs.getInt("benchmarkChargeAddedMah", 0);
        if (counterMah > 0) {
            int baseline = lastCounter > 0 ? lastCounter : startCounter;
            if (baseline > 0 && counterMah > baseline) added += counterMah - baseline;
        } else {
            int observedChargeMah = prefs.getInt("chargeScreenOnMah", 0) + prefs.getInt("chargeScreenOffMah", 0);
            int observedBaseline = prefs.getInt("benchmarkChargeStatsBaselineMah", 0);
            if (observedChargeMah > observedBaseline) added += observedChargeMah - observedBaseline;
            prefs.edit().putInt("benchmarkChargeStatsBaselineMah", observedChargeMah).apply();
        }
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

    private void updateDischargeStats(android.content.SharedPreferences prefs, int level, boolean charging,
                                      int counterMah, int currentMa, long now, boolean interactive,
                                      long deepSleepDeltaMs) {
        boolean previousCharging = prefs.getBoolean("monitorLastCharging", charging);
        if (charging && !previousCharging) {
            refreshDischargeDeepSleep(prefs);
            prefs.edit().putLong("lastDischargeScreenOnMs", prefs.getLong("dischargeScreenOnMs", 0L))
                    .putLong("lastDischargeScreenOffMs", prefs.getLong("dischargeScreenOffMs", 0L))
                    .putFloat("lastDischargeScreenOnPercent", prefs.getFloat("dischargeScreenOnPercent", 0f))
                    .putFloat("lastDischargeScreenOffPercent", prefs.getFloat("dischargeScreenOffPercent", 0f))
                    .putInt("lastDischargeMah", prefs.getInt("dischargeMah", 0))
                    .putInt("lastDischargeWakeups", prefs.getInt("dischargeWakeups", 0))
                    .putLong("lastDischargeDeepSleepMs", prefs.getLong("dischargeDeepSleepMs", 0L))
                    .putLong("lastDischargeStartAt", prefs.getLong("dischargeStartAt", 0L))
                    .putLong("lastDischargeEndAt", now)
                    .putInt("lastDischargeEndLevel", level)
                    .remove("dischargeLastLevel").remove("dischargeLastCounterMah")
                    .putLong("dischargeScreenOnMs", 0L).putLong("dischargeScreenOffMs", 0L)
                    .putFloat("dischargeScreenOnPercent", 0f).putFloat("dischargeScreenOffPercent", 0f)
                    .putInt("dischargeMah", 0).putInt("dischargeWakeups", 0).putLong("dischargeLastAt", now)
                    .putLong("dischargeDeepSleepMs", 0L).apply();
            return;
        }
        if (!charging && previousCharging) {
            prefs.edit().putInt("dischargeLastLevel", level).putInt("dischargeLastCounterMah", counterMah)
                    .putLong("dischargeLastAt", now).putLong("dischargeScreenOnMs", 0L)
                    .putLong("dischargeScreenOffMs", 0L).putFloat("dischargeScreenOnPercent", 0f)
                    .putFloat("dischargeScreenOffPercent", 0f).putInt("dischargeMah", 0).putInt("dischargeWakeups", 0)
                    .putLong("dischargeStartAt", now).apply();
            prefs.edit().putLong("dischargeDeepSleepMs", 0L)
                    .putLong("dischargeDeepSleepStartElapsed", SystemClock.elapsedRealtime())
                    .putLong("dischargeDeepSleepStartUptime", SystemClock.uptimeMillis()).apply();
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
        if (counterMah > 0 && previousCounter > 0 && previousCounter > counterMah) {
            energy += previousCounter - counterMah;
        } else if (counterMah <= 0 && currentMa > 0) {
            energy += Math.round(currentMa * Math.min(elapsed, accountingIntervalCapMs()) / 3600000f);
        }
        long onMs = prefs.getLong("dischargeScreenOnMs", 0L) + (interactive ? elapsed : 0L);
        long offMs = prefs.getLong("dischargeScreenOffMs", 0L) + (interactive ? 0L : elapsed);
        long deepSleepMs = prefs.getLong("dischargeDeepSleepMs", 0L) + deepSleepDeltaMs;
        prefs.edit().putInt("dischargeLastLevel", level).putInt("dischargeLastCounterMah", counterMah)
                .putLong("dischargeLastAt", now).putFloat("dischargeScreenOnPercent", onPercent)
                .putFloat("dischargeScreenOffPercent", offPercent).putInt("dischargeMah", energy)
                .putLong("dischargeScreenOnMs", onMs).putLong("dischargeScreenOffMs", offMs)
                .putLong("dischargeDeepSleepMs", deepSleepMs).apply();
    }

    private void refreshDischargeDeepSleep(android.content.SharedPreferences prefs) {
        long startElapsed = prefs.getLong("dischargeDeepSleepStartElapsed", -1L);
        long startUptime = prefs.getLong("dischargeDeepSleepStartUptime", -1L);
        long elapsed = SystemClock.elapsedRealtime();
        long uptime = SystemClock.uptimeMillis();
        if (startElapsed < 0L || startUptime < 0L) return;
        if (elapsed < startElapsed || uptime < startUptime) {
            prefs.edit().putLong("dischargeDeepSleepStartElapsed", elapsed)
                    .putLong("dischargeDeepSleepStartUptime", uptime)
                    .putLong("dischargeDeepSleepMs", 0L).apply();
            return;
        }
        long deepSleepMs = Math.max(0L, (elapsed - startElapsed) - (uptime - startUptime));
        prefs.edit().putLong("dischargeDeepSleepMs", deepSleepMs).apply();
    }

    private void updateChargeStats(android.content.SharedPreferences prefs, int level, boolean charging, int counterMah,
                                   int currentMa, long now, boolean interactive, int plugged) {
        boolean previousCharging = prefs.getBoolean("monitorLastCharging", charging);
        if (!charging && previousCharging) {
            // Preserve the completed session's screen-on/off breakdown before
            // the next charging session resets the live counters.
            prefs.edit()
                    .putLong("lastChargeScreenOnMs", prefs.getLong("chargeScreenOnMs", 0L))
                    .putLong("lastChargeScreenOffMs", prefs.getLong("chargeScreenOffMs", 0L))
                    .putInt("lastChargeScreenOnMah", prefs.getInt("chargeScreenOnMah", 0))
                    .putInt("lastChargeScreenOffMah", prefs.getInt("chargeScreenOffMah", 0))
                    .putFloat("lastChargeScreenOnPercent", prefs.getFloat("chargeScreenOnPercent", 0f))
                    .putFloat("lastChargeScreenOffPercent", prefs.getFloat("chargeScreenOffPercent", 0f))
                    .putInt("lastChargePlugged", prefs.getInt("chargePlugged", plugged))
                    .apply();
            return;
        }
        if (charging && !previousCharging) {
            prefs.edit().putInt("chargeLastCounterMah", counterMah).putInt("chargeLastLevel", level).putLong("chargeLastAt", now)
                    .putInt("chargePlugged", plugged)
                    .putLong("chargeScreenOnMs", 0L).putLong("chargeScreenOffMs", 0L)
                    .putInt("chargeScreenOnMah", 0).putInt("chargeScreenOffMah", 0)
                    .putFloat("chargeScreenOnPercent", 0f).putFloat("chargeScreenOffPercent", 0f)
                    .putBoolean("chargeAlarmSent", false).apply();
            return;
        }
        if (!charging) return;
        if (plugged != 0 && plugged != prefs.getInt("chargePlugged", 0)) {
            prefs.edit().putInt("chargePlugged", plugged).apply();
        }
        int previousCounter = prefs.getInt("chargeLastCounterMah", counterMah);
        long lastAt = prefs.getLong("chargeLastAt", now);
        long elapsed = Math.max(0L, now - lastAt);
        int added;
        if (counterMah > 0 && previousCounter > 0) {
            added = counterMah > previousCounter ? counterMah - previousCounter : 0;
        } else {
            added = currentMa > 0
                    ? Math.round(currentMa * Math.min(elapsed, accountingIntervalCapMs()) / 3600000f)
                    : 0;
        }
        long onMs = prefs.getLong("chargeScreenOnMs", 0L) + (interactive ? elapsed : 0L);
        long offMs = prefs.getLong("chargeScreenOffMs", 0L) + (interactive ? 0L : elapsed);
        int onMah = prefs.getInt("chargeScreenOnMah", 0) + (interactive ? added : 0);
        int offMah = prefs.getInt("chargeScreenOffMah", 0) + (interactive ? 0 : added);
        float onPercent = prefs.getFloat("chargeScreenOnPercent", 0f);
        float offPercent = prefs.getFloat("chargeScreenOffPercent", 0f);
        int previousLevel = prefs.getInt("chargeLastLevel", level);
        if (level > previousLevel) {
            float delta = level - previousLevel;
            if (interactive) onPercent += delta; else offPercent += delta;
        }
        prefs.edit().putInt("chargeLastCounterMah", counterMah).putLong("chargeLastAt", now)
                .putInt("chargePlugged", plugged != 0 ? plugged : prefs.getInt("chargePlugged", 0))
                .putLong("chargeScreenOnMs", onMs).putLong("chargeScreenOffMs", offMs)
                .putInt("chargeScreenOnMah", onMah).putInt("chargeScreenOffMah", offMah)
                .putFloat("chargeScreenOnPercent", onPercent).putFloat("chargeScreenOffPercent", offPercent)
                .putInt("chargeLastLevel", level).apply();
    }

    private String chargerLabel(int plugged) {
        if (plugged == BatteryManager.BATTERY_PLUGGED_AC) return "Netzteil";
        if (plugged == BatteryManager.BATTERY_PLUGGED_USB) return "USB";
        if (plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS) return "Kabellos";
        return "Externe Stromquelle";
    }

    private String duration(long minutes) {
        return minutes >= 60 ? (minutes / 60) + " Std. " + (minutes % 60) + " Min." : minutes + " Min.";
    }

    private Notification alarmNotification(int value, int limit) {
        Intent launch = new Intent(this, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(this, 1, launch, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? new Notification.Builder(this, ALARM_CHANNEL_ID) : new Notification.Builder(this);
        return builder.setSmallIcon(com.ampere.batterylab.R.drawable.ic_launcher)
                .setContentTitle("Ladeziel erreicht")
                .setContentText("Akku bei " + value + "% · eingestelltes Ziel " + limit + "%")
                .setContentIntent(pending)
                .setAutoCancel(true)
                .build();
    }

    private Notification temperatureAlarmNotification(int temperatureTenths, int thresholdTenths) {
        Intent launch = new Intent(this, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(this, 3, launch, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? new Notification.Builder(this, ALARM_CHANNEL_ID) : new Notification.Builder(this);
        return builder.setSmallIcon(com.ampere.batterylab.R.drawable.ic_launcher)
                .setContentTitle("Hohe Akkutemperatur")
                .setContentText(String.format(Locale.GERMANY, "Akku bei %.1f °C · Grenzwert %.1f °C", temperatureTenths / 10f, thresholdTenths / 10f))
                .setContentIntent(pending)
                .setAutoCancel(false)
                .build();
    }

    @Override public void onDestroy() {
        try { unregisterReceiver(screenReceiver); } catch (IllegalArgumentException ignored) { }
        try { unregisterReceiver(batteryReceiver); } catch (IllegalArgumentException ignored) { }
        if (handler != null) handler.removeCallbacksAndMessages(null);
        if (monitorThread != null) {
            monitorThread.quitSafely();
            monitorThread = null;
        }
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
