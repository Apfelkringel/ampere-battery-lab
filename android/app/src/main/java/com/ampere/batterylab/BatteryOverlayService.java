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
import android.widget.TextView;
import android.graphics.drawable.GradientDrawable;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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
        overlay.setTextColor(Color.rgb(242, 244, 239));
        overlay.setTextSize(12f);
        overlay.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        overlay.setPadding(18, 12, 18, 12);
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.rgb(25, 28, 35));
        background.setCornerRadius(18f);
        background.setStroke(1, Color.rgb(199, 243, 107));
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
        try { windowManager.addView(overlay, params); } catch (WindowManager.BadTokenException ignored) { stopSelf(); return; }
        handler.post(refresh);
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) manager.createNotificationChannel(new NotificationChannel(CHANNEL_ID, "Live-Akkuanzeige", NotificationManager.IMPORTANCE_LOW));
    }

    private Notification notification() {
        Intent launch = new Intent(this, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(this, 2, launch, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? new Notification.Builder(this, CHANNEL_ID) : new Notification.Builder(this);
        return builder.setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("Ampere-Live-Anzeige aktiv")
                .setContentText("Live-Akkumesswerte werden auf dem Bildschirm angezeigt")
                .setContentIntent(pending)
                .setOngoing(true)
                .setShowWhen(false)
                .build();
    }

    private void updateText() {
        if (overlay == null) return;
        Intent battery = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (battery == null) return;
        int raw = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        int level = raw >= 0 && scale > 0 ? Math.round(raw * 100f / scale) : 0;
        int temperature = BatteryTemperature.normalizeTenths(
                battery.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0));
        int voltage = battery.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);
        int status = battery.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN);
        int plugged = battery.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
        boolean charging = BatteryState.isCharging(status, plugged);
        BatteryManager manager = (BatteryManager) getSystemService(BATTERY_SERVICE);
        int current = BatteryCurrent.milliAmps(manager);
        String currentText = current > 0 ? (charging ? "+" : "−") + current + " mA" : "—";
        int coreCpu = readCpuPercent();
        String topPackage = topAppPackage();
        String topLabel = topAppLabel(topPackage);
        int processCpu = readProcessCpuPercent(topPackage);
        String processText = processCpu >= 0 ? processCpu + "%" : "—";
        overlay.setText("⚡ " + level + "%   " + currentText + "\n" + (voltage / 1000f) + " V   " + (temperature / 10f) + "°C   CPU-Kerne " + coreCpu + "%\nTop-App: " + topLabel + " · Prozess " + processText);
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

    @Override public int onStartCommand(Intent intent, int flags, int startId) { return START_STICKY; }

    @Override public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (windowManager != null && overlay != null) try { windowManager.removeView(overlay); } catch (Exception ignored) { }
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
