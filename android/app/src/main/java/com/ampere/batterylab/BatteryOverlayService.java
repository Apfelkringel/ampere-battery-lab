package com.ampere.batterylab;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ActivityManager;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.View;
import android.widget.TextView;
import android.graphics.drawable.GradientDrawable;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Local live current overlay, enabled explicitly by the user. */
public class BatteryOverlayService extends Service {
    private static final String CHANNEL_ID = "ampere-overlay";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private WindowManager windowManager;
    private TextView overlay;
    private long[] previousCoreTotals = new long[0];
    private long[] previousCoreIdles = new long[0];
    private int cpuPercent = 0;
    private String previousProcessPackage = "";
    private long previousProcessTicks = -1L;
    private long previousSystemTicks = -1L;
    private int processCpuPercent = 0;
    private final Runnable refresh = new Runnable() {
        @Override public void run() {
            updateText();
            handler.postDelayed(this, 2000L);
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            stopSelf();
            return;
        }
        createChannel();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForeground(9, notification());
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        overlay = new TextView(this);
        overlay.setTextColor(Color.rgb(255, 247, 232));
        overlay.setTextSize(12f);
        overlay.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
        overlay.setIncludeFontPadding(false);
        overlay.setMaxLines(4);
        overlay.setMaxWidth(Math.round(getResources().getDisplayMetrics().widthPixels * 0.78f));
        float density = getResources().getDisplayMetrics().density;
        int horizontalPadding = Math.round(14f * density);
        int verticalPadding = Math.round(10f * density);
        overlay.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding);
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.rgb(4, 52, 56));
        background.setCornerRadius(14f * density);
        background.setStroke(Math.max(1, Math.round(density)), Color.rgb(20, 114, 111));
        overlay.setBackground(background);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.END;
        params.x = 18;
        params.y = 120;
        overlay.setVisibility(shouldDisplayOverlay() ? View.VISIBLE : View.GONE);
        try { windowManager.addView(overlay, params); } catch (WindowManager.BadTokenException ignored) { stopSelf(); return; }
        handler.post(refresh);
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) manager.createNotificationChannel(new NotificationChannel(CHANNEL_ID,
                AppText.t(this, "Live-Akkuanzeige"), NotificationManager.IMPORTANCE_LOW));
    }

    private Notification notification() {
        Intent launch = new Intent(this, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(this, 2, launch, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? new Notification.Builder(this, CHANNEL_ID) : new Notification.Builder(this);
        return builder.setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(AppText.t(this, "Ampere-Live-Anzeige aktiv"))
                .setContentText(AppText.t(this, "Live-Akkumesswerte werden auf dem Bildschirm angezeigt"))
                .setContentIntent(pending)
                .setOngoing(true)
                .setShowWhen(false)
                .build();
    }

    private boolean shouldDisplayOverlay() {
        boolean enabled = BatteryDataRepository.data(this).getBoolean("overlayEnabled", false);
        boolean activityVisible = getSharedPreferences("ampere-ui-state", MODE_PRIVATE)
                .getBoolean("mainActivityVisible", false);
        return BatteryOverlayVisibility.shouldShow(enabled, activityVisible);
    }

    private void updateText() {
        if (overlay == null) return;
        boolean visible = shouldDisplayOverlay();
        overlay.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (!visible) return;
        Intent battery = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (battery == null) return;
        BatteryReading reading = BatteryReading.read(this, battery);
        int level = reading.level;
        int temperature = reading.temperatureTenths;
        int voltage = reading.voltageMv;
        boolean charging = reading.charging;
        int current = reading.currentMa;
        String currentText = BatteryTelemetryText.current(current, charging, true);
        int coreCpu = readCpuPercent();
        String topPackage = topAppPackage();
        String topLabel = topAppLabel(topPackage);
        int processCpu = readProcessCpuPercent(topPackage);
        String processText = processCpu >= 0 ? processCpu + "%" : "—";
        String levelText = level >= 0 ? level + "%" : "—";
        String voltageText = voltage > 0 ? String.format(Locale.GERMANY, "%.2f V", voltage / 1000f) : "— V";
        String temperatureText = temperature > 0 ? String.format(Locale.GERMANY, "%.1f°C", temperature / 10f) : "—°C";
        String overlayText = BatteryOverlayText.header(levelText, currentText) + "\n"
                + voltageText + "   " + temperatureText + "   CPU gesamt " + coreCpu
                + "%\nVordergrund-App: " + topLabel + " · Prozesslast " + processText;
        overlay.setText(AppText.t(this, overlayText));
    }

    private int readCpuPercent() {
        try (BufferedReader reader = new BufferedReader(new FileReader("/proc/stat"))) {
            String line;
            long[] totals = new long[Runtime.getRuntime().availableProcessors()];
            long[] idles = new long[totals.length];
            int cores = 0;
            while ((line = reader.readLine()) != null) {
                if (!line.matches("^cpu\\d+\\s+.*")) {
                    if (cores > 0) break;
                    continue;
                }
                String[] values = line.trim().split("\\s+");
                if (values.length < 5 || cores == totals.length) continue;
                long total = 0L;
                for (int i = 1; i < values.length; i++) total += Long.parseLong(values[i]);
                totals[cores] = total;
                idles[cores] = Long.parseLong(values[4]) + (values.length > 5 ? Long.parseLong(values[5]) : 0L);
                cores++;
            }
            if (cores == 0) return cpuPercent;
            if (previousCoreTotals.length != cores) {
                previousCoreTotals = new long[cores];
                previousCoreIdles = new long[cores];
                System.arraycopy(totals, 0, previousCoreTotals, 0, cores);
                System.arraycopy(idles, 0, previousCoreIdles, 0, cores);
                return cpuPercent;
            }
            int totalPercent = 0;
            int measured = 0;
            for (int i = 0; i < cores; i++) {
                long totalDelta = totals[i] - previousCoreTotals[i];
                long idleDelta = idles[i] - previousCoreIdles[i];
                if (totalDelta > 0L) { totalPercent += Math.max(0, Math.min(100, Math.round((totalDelta - idleDelta) * 100f / totalDelta))); measured++; }
            }
            if (measured > 0) cpuPercent = totalPercent / measured;
            System.arraycopy(totals, 0, previousCoreTotals, 0, cores);
            System.arraycopy(idles, 0, previousCoreIdles, 0, cores);
        } catch (Exception ignored) { }
        return cpuPercent;
    }

    private String topAppPackage() {
        AppOpsManager ops = (AppOpsManager) getSystemService(APP_OPS_SERVICE);
        if (ops == null || ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), getPackageName()) != AppOpsManager.MODE_ALLOWED) return "Nutzungszugriff aus";
        UsageStatsManager manager = (UsageStatsManager) getSystemService(USAGE_STATS_SERVICE);
        if (manager == null) return "—";
        long end = System.currentTimeMillis();
        List<UsageStats> stats = manager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, end - 60 * 60 * 1000L, end);
        if (stats == null) return "—";
        Collections.sort(stats, new Comparator<UsageStats>() {
            @Override public int compare(UsageStats left, UsageStats right) { return Long.compare(right.getTotalTimeInForeground(), left.getTotalTimeInForeground()); }
        });
        for (UsageStats stat : stats) {
            if (stat.getTotalTimeInForeground() < 60 * 1000L || stat.getPackageName().equals(getPackageName())) continue;
            return stat.getPackageName();
        }
        return "—";
    }

    private String topAppLabel(String packageName) {
        if (packageName == null || packageName.isEmpty()) return "—";
        if ("Nutzungszugriff aus".equals(packageName)) return packageName;
        if ("—".equals(packageName)) return packageName;
        try { return getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(packageName, 0)).toString(); }
        catch (Exception ignored) { return packageName; }
    }

    /** Best-effort process CPU usage from local kernel counters; no process list is uploaded. */
    private int readProcessCpuPercent(String packageName) {
        if (packageName == null || packageName.isEmpty() || "Nutzungszugriff aus".equals(packageName) || "—".equals(packageName)) {
            previousProcessPackage = "";
            previousProcessTicks = -1L;
            previousSystemTicks = -1L;
            processCpuPercent = 0;
            return -1;
        }
        try {
            ActivityManager activity = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
            if (activity == null) return -1;
            int pid = -1;
            for (ActivityManager.RunningAppProcessInfo process : activity.getRunningAppProcesses()) {
                if (process.processName != null && (process.processName.equals(packageName) || process.processName.startsWith(packageName + ":"))) {
                    pid = process.pid;
                    break;
                }
            }
            if (pid <= 0) return -1;
            long processTicks = readProcessTicks(pid);
            long systemTicks = readSystemTicks();
            if (processTicks < 0L || systemTicks < 0L) return -1;
            if (!packageName.equals(previousProcessPackage)) {
                previousProcessPackage = packageName;
                previousProcessTicks = processTicks;
                previousSystemTicks = systemTicks;
                processCpuPercent = 0;
                return 0;
            }
            long processDelta = processTicks - previousProcessTicks;
            long systemDelta = systemTicks - previousSystemTicks;
            previousProcessTicks = processTicks;
            previousSystemTicks = systemTicks;
            int cores = Math.max(1, Runtime.getRuntime().availableProcessors());
            if (processDelta >= 0L && systemDelta > 0L) {
                processCpuPercent = Math.max(0, Math.min(100 * cores, Math.round(processDelta * cores * 100f / systemDelta)));
            }
            return processCpuPercent;
        } catch (Exception ignored) {
            return -1;
        }
    }

    private long readProcessTicks(int pid) throws Exception {
        try (BufferedReader reader = new BufferedReader(new FileReader("/proc/" + pid + "/stat"))) {
            String line = reader.readLine();
            if (line == null) return -1L;
            int closing = line.lastIndexOf(')');
            if (closing < 0 || closing + 2 >= line.length()) return -1L;
            String[] fields = line.substring(closing + 2).trim().split("\\s+");
            if (fields.length < 13) return -1L;
            return Long.parseLong(fields[11]) + Long.parseLong(fields[12]);
        }
    }

    private long readSystemTicks() throws Exception {
        try (BufferedReader reader = new BufferedReader(new FileReader("/proc/stat"))) {
            String line = reader.readLine();
            if (line == null || !line.startsWith("cpu ")) return -1L;
            String[] fields = line.trim().split("\\s+");
            long total = 0L;
            for (int i = 1; i < fields.length; i++) total += Long.parseLong(fields[i]);
            return total;
        }
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (overlay != null) updateText();
        return START_STICKY;
    }

    @Override public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (windowManager != null && overlay != null) try { windowManager.removeView(overlay); } catch (Exception ignored) { }
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
