package com.ampere.batterylab;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.backup.BackupManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ScrollView;
import android.widget.EditText;
import android.widget.Toast;
import android.text.InputType;
import android.content.SharedPreferences;
import android.os.Build;
import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.provider.Settings;
import android.net.Uri;
import android.view.Window;
import android.view.WindowInsets;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Comparator;

public class MainActivity extends Activity {
    private static final int CREATE_BACKUP_REQUEST = 1201;
    private static final int RESTORE_BACKUP_REQUEST = 1202;
    private static final int MAX_BACKUP_BYTES = 4 * 1024 * 1024;
    private BatteryDashboard dashboard;
    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (dashboard != null) dashboard.readBattery(intent);
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        Window window = getWindow();
        window.setStatusBarColor(Color.rgb(17, 19, 24));
        window.setNavigationBarColor(Color.rgb(17, 19, 24));
        window.getDecorView().setSystemUiVisibility(0);
        dashboard = new BatteryDashboard(this);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        if (Build.VERSION.SDK_INT >= 35) {
            window.setDecorFitsSystemWindows(false);
            scroll.setOnApplyWindowInsetsListener((view, insets) -> {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                view.setPadding(view.getPaddingLeft(), bars.top, view.getPaddingRight(), bars.bottom);
                return insets;
            });
        }
        int contentHeight = Math.round(1320 * getResources().getDisplayMetrics().density);
        scroll.addView(dashboard, new ScrollView.LayoutParams(-1, contentHeight));
        setContentView(scroll);
        UpdateChecker.check(this);
        Intent service = new Intent(this, BatteryMonitorService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(service); else startService(service);
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != getPackageManager().PERMISSION_GRANTED) {
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 44);
        }
        dashboard.startSavedOverlay();
        dashboard.postDelayed(() -> dashboard.showTutorial(false), 1200L);
        IntentFilter batteryFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent battery = Build.VERSION.SDK_INT >= 33 ? registerReceiver(batteryReceiver, batteryFilter, Context.RECEIVER_NOT_EXPORTED) : registerReceiver(batteryReceiver, batteryFilter);
        if (battery != null) dashboard.readBattery(battery);
    }

    @Override protected void onDestroy() {
        unregisterReceiver(batteryReceiver);
        super.onDestroy();
    }

    void createBackup() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("application/json")
                .putExtra(Intent.EXTRA_TITLE, "ampere-battery-backup.json");
        startActivityForResult(intent, CREATE_BACKUP_REQUEST);
    }

    void restoreBackup() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("application/json")
                .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
        startActivityForResult(intent, RESTORE_BACKUP_REQUEST);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        if (requestCode == CREATE_BACKUP_REQUEST) writeBackup(uri);
        else if (requestCode == RESTORE_BACKUP_REQUEST) readBackup(uri);
    }

    private void writeBackup(Uri uri) {
        try (OutputStream stream = getContentResolver().openOutputStream(uri)) {
            if (stream == null) throw new IllegalStateException("No output stream");
            JSONObject root = new JSONObject();
            root.put("schema", 1);
            root.put("package", getPackageName());
            root.put("createdAt", System.currentTimeMillis());
            JSONObject values = new JSONObject();
            for (java.util.Map.Entry<String, ?> entry : getSharedPreferences("ampere-data", MODE_PRIVATE).getAll().entrySet()) {
                Object value = entry.getValue();
                JSONObject encoded = new JSONObject();
                if (value instanceof Boolean) { encoded.put("type", "boolean"); encoded.put("value", value); }
                else if (value instanceof Integer) { encoded.put("type", "int"); encoded.put("value", value); }
                else if (value instanceof Long) { encoded.put("type", "long"); encoded.put("value", value); }
                else if (value instanceof Float) { encoded.put("type", "float"); encoded.put("value", value); }
                else if (value instanceof String) { encoded.put("type", "string"); encoded.put("value", value); }
                else continue;
                values.put(entry.getKey(), encoded);
            }
            root.put("preferences", values);
            stream.write(root.toString(2).getBytes(StandardCharsets.UTF_8));
            Toast.makeText(this, "Backup gespeichert.", Toast.LENGTH_LONG).show();
        } catch (Exception error) {
            Toast.makeText(this, "Backup konnte nicht gespeichert werden.", Toast.LENGTH_LONG).show();
        }
    }

    private void readBackup(Uri uri) {
        try (InputStream stream = getContentResolver().openInputStream(uri)) {
            if (stream == null) throw new IllegalStateException("No input stream");
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int count;
            int total = 0;
            while ((count = stream.read(buffer)) != -1) {
                total += count;
                if (total > MAX_BACKUP_BYTES) throw new IllegalArgumentException("Backup too large");
                bytes.write(buffer, 0, count);
            }
            JSONObject root = new JSONObject(bytes.toString("UTF-8"));
            if (!getPackageName().equals(root.optString("package")) || root.optInt("schema", 0) != 1) throw new IllegalArgumentException("Invalid backup");
            JSONObject values = root.getJSONObject("preferences");
            SharedPreferences.Editor editor = getSharedPreferences("ampere-data", MODE_PRIVATE).edit().clear();
            java.util.Iterator<String> keys = values.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                JSONObject encoded = values.getJSONObject(key);
                String type = encoded.optString("type");
                if ("boolean".equals(type)) editor.putBoolean(key, encoded.getBoolean("value"));
                else if ("int".equals(type)) editor.putInt(key, encoded.getInt("value"));
                else if ("long".equals(type)) editor.putLong(key, encoded.getLong("value"));
                else if ("float".equals(type)) editor.putFloat(key, (float) encoded.getDouble("value"));
                else if ("string".equals(type)) editor.putString(key, encoded.getString("value"));
            }
            editor.apply();
            BackupManager.dataChanged(getPackageName());
            dashboard.reloadStoredData();
            Intent battery = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (battery != null) dashboard.readBattery(battery);
            Toast.makeText(this, "Backup wiederhergestellt.", Toast.LENGTH_LONG).show();
        } catch (Exception error) {
            Toast.makeText(this, "Backup ist ungültig oder konnte nicht gelesen werden.", Toast.LENGTH_LONG).show();
        }
    }
}

class BatteryDashboard extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final float density;
    private int level = 0;
    private float temperature = 0f;
    private float voltage = 0f;
    private int currentMa = 0;
    private int chargeCounterMah = 0;
    private boolean lastCharging = false;
    private long sessionStartedAt = 0L;
    private int sessionStartLevel = 0;
    private int sessionStartChargeCounterMah = 0;
    private final SharedPreferences prefs;
    private final ArrayList<Integer> history = new ArrayList<>();
    private final ArrayList<Integer> longHistory = new ArrayList<>();
    private final ArrayList<Integer> healthSamples = new ArrayList<>();
    private final ArrayList<String> sessions = new ArrayList<>();
    private boolean charging = false;
    private boolean light = false;
    private boolean amoled = false;
    private boolean chargeAlarm = true;
    private int chargeLimit = 80;
    private boolean benchmarkActive = false;
    private boolean overlayEnabled = false;
    private int historyDays = 7;
    private int page = 0;
    private long lastTouch;
    private final int lime = Color.rgb(199, 243, 107);
    private final int blue = Color.rgb(118, 184, 255);
    private final int amber = Color.rgb(242, 179, 106);

    BatteryDashboard(Context context) {
        super(context);
        density = getResources().getDisplayMetrics().density;
        p.setTypeface(Typeface.create("sans", Typeface.NORMAL));
        setFocusable(true);
        prefs = context.getSharedPreferences("ampere-data", Context.MODE_PRIVATE);
        loadStoredData();
    }

    void reloadStoredData() {
        history.clear();
        longHistory.clear();
        healthSamples.clear();
        sessions.clear();
        loadStoredData();
        invalidate();
    }

    void readBattery(Intent intent) {
        int rawLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN);
        if (rawLevel >= 0 && scale > 0) level = Math.max(0, Math.min(100, Math.round(rawLevel * 100f / scale)));
        boolean newCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;
        if (sessionStartedAt == 0L) {
            lastCharging = newCharging;
            sessionStartedAt = System.currentTimeMillis();
            sessionStartLevel = level;
        } else if (newCharging != lastCharging) {
            lastCharging = newCharging;
            sessionStartedAt = System.currentTimeMillis();
            sessionStartLevel = level;
            sessionStartChargeCounterMah = 0;
        }
        charging = newCharging;
        int temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
        if (temp > 0) temperature = temp / 10f;
        int mv = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
        if (mv > 0) voltage = mv / 1000f;
        BatteryManager manager = (BatteryManager) getContext().getSystemService(Context.BATTERY_SERVICE);
        int microamps = manager == null ? 0 : manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
        if (microamps == Integer.MIN_VALUE || microamps == 0) {
            microamps = manager == null ? 0 : manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE);
        }
        currentMa = microamps == Integer.MIN_VALUE ? 0 : Math.abs(microamps) / 1000;
        int chargeCounter = manager == null ? 0 : manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER);
        if (chargeCounter > 0) {
            chargeCounterMah = chargeCounter / 1000;
            if (sessionStartChargeCounterMah <= 0) {
                sessionStartChargeCounterMah = chargeCounterMah;
                prefs.edit().putInt("sessionStartChargeCounterMah", sessionStartChargeCounterMah).apply();
            }
        }
        benchmarkActive = prefs.getBoolean("benchmarkActive", benchmarkActive);
        saveSample();
        reloadHealthSamples();
        invalidate();
    }

    private void reloadHealthSamples() {
        String saved = prefs.getString("healthSamples", "");
        healthSamples.clear();
        if (!saved.isEmpty()) for (String value : saved.split(",")) try { healthSamples.add(Integer.parseInt(value)); } catch (NumberFormatException ignored) { }
    }

    private void loadStoredData() {
        String savedHistory = prefs.getString("history", "");
        if (!savedHistory.isEmpty()) {
            for (String value : savedHistory.split(",")) {
                try { history.add(Integer.parseInt(value)); } catch (NumberFormatException ignored) { }
            }
        }
        String savedLongHistory = prefs.getString("historyLong", "");
        if (!savedLongHistory.isEmpty()) {
            for (String value : savedLongHistory.split(",")) {
                try { longHistory.add(Integer.parseInt(value)); } catch (NumberFormatException ignored) { }
            }
        }
        if (longHistory.isEmpty()) longHistory.addAll(history);
        String savedHealth = prefs.getString("healthSamples", "");
        if (!savedHealth.isEmpty()) for (String value : savedHealth.split(",")) try { healthSamples.add(Integer.parseInt(value)); } catch (NumberFormatException ignored) { }
        String savedSessions = prefs.getString("sessions", "");
        if (!savedSessions.isEmpty()) {
            for (String value : savedSessions.split("\\|")) if (!value.isEmpty()) sessions.add(value);
        }
        sessionStartedAt = prefs.getLong("sessionStartedAt", 0L);
        sessionStartLevel = prefs.getInt("sessionStartLevel", level);
        sessionStartChargeCounterMah = prefs.getInt("sessionStartChargeCounterMah", 0);
        lastCharging = prefs.getBoolean("lastCharging", false);
        chargeAlarm = prefs.getBoolean("chargeAlarm", true);
        chargeLimit = prefs.getInt("chargeLimit", 80);
        benchmarkActive = prefs.getBoolean("benchmarkActive", false);
        overlayEnabled = prefs.getBoolean("overlayEnabled", false) && (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(getContext()));
        historyDays = prefs.getInt("historyDays", 7) == 30 ? 30 : 7;
        light = prefs.getBoolean("lightTheme", false);
        amoled = prefs.getBoolean("amoledTheme", false);
    }

    private void saveSample() {
        long now = System.currentTimeMillis();
        long lastSample = prefs.getLong("lastSample", 0L);
        if (now - lastSample < 5 * 60 * 1000L && !history.isEmpty()) return;
        history.add(level);
        while (history.size() > 48) history.remove(0);
        longHistory.add(level);
        while (longHistory.size() > 2880) longHistory.remove(0);
        StringBuilder values = new StringBuilder();
        for (int i = 0; i < history.size(); i++) { if (i > 0) values.append(','); values.append(history.get(i)); }
        StringBuilder longValues = new StringBuilder();
        for (int i = 0; i < longHistory.size(); i++) { if (i > 0) longValues.append(','); longValues.append(longHistory.get(i)); }
        prefs.edit().putString("history", values.toString()).putString("historyLong", longValues.toString()).putLong("lastSample", now).apply();
    }

    private String formatDuration(long minutes) {
        return minutes >= 60 ? (minutes / 60) + "h " + (minutes % 60) + "m" : minutes + " min";
    }

    private int healthPercent() {
        int measured = 0;
        if (!healthSamples.isEmpty()) {
            int count = Math.min(5, healthSamples.size());
            int total = 0;
            for (int i = healthSamples.size() - count; i < healthSamples.size(); i++) total += healthSamples.get(i);
            measured = Math.round(total / (float) count);
        }
        if (measured <= 0) measured = prefs.getInt("benchmarkCapacityMah", 0);
        if (measured <= 0) return 0;
        return Math.max(1, Math.min(110, Math.round(measured * 100f / designCapacityMah())));
    }

    private String healthDisplay() { return healthPercent() > 0 ? String.valueOf(healthPercent()) : "—"; }

    private String temperatureDisplay() { return temperature > 0f ? String.format(Locale.US, "%.1f", temperature) : "—"; }

    private String voltageDisplay() { return voltage > 0f ? String.format(Locale.US, "%.2f", voltage) : "—"; }

    private int designCapacityMah() { return prefs.getInt("designCapacityMah", 4500); }

    private int estimatedCapacityMah() { return Math.round(designCapacityMah() * healthPercent() / 100f); }

    private void editDesignCapacity() {
        EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(designCapacityMah()));
        input.setSelectAllOnFocus(true);
        new AlertDialog.Builder(getContext())
                .setTitle("Design capacity")
                .setMessage("Enter the factory capacity in mAh.")
                .setView(input)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (dialog, which) -> {
                    try {
                        int capacity = Integer.parseInt(input.getText().toString().trim());
                        if (capacity >= 500 && capacity <= 20000) {
                            prefs.edit().putInt("designCapacityMah", capacity).apply();
                            invalidate();
                        }
                    } catch (NumberFormatException ignored) { }
                }).show();
    }

    private int sessionEnergyMah() {
        if (chargeCounterMah <= 0 || sessionStartChargeCounterMah <= 0) return 0;
        return charging ? Math.max(0, chargeCounterMah - sessionStartChargeCounterMah) : Math.max(0, sessionStartChargeCounterMah - chargeCounterMah);
    }

    private String timeToFull() {
        if (!charging || currentMa < 50) return "—";
        int missingMah = Math.round(estimatedCapacityMah() * (100 - level) / 100f);
        return formatDuration(Math.max(1, Math.round(missingMah * 60f / currentMa)));
    }

    private String runtimeEstimate() {
        if (charging) {
            float used = prefs.getFloat("lastDischargeScreenOnPercent", 0f) + prefs.getFloat("lastDischargeScreenOffPercent", 0f);
            long minutes = (prefs.getLong("lastDischargeScreenOnMs", 0L) + prefs.getLong("lastDischargeScreenOffMs", 0L)) / 60000L;
            int historicalLevel = prefs.getInt("lastDischargeEndLevel", level);
            return used > 0f && minutes >= 5 ? formatDuration(Math.max(1, Math.round(historicalLevel * minutes / used))) : "—";
        }
        if (currentMa < 50 || estimatedCapacityMah() <= 0) return "—";
        int availableMah = Math.round(estimatedCapacityMah() * level / 100f);
        return formatDuration(Math.max(1, Math.round(availableMah * 60f / currentMa)));
    }

    private String drainRate() {
        int capacity = estimatedCapacityMah();
        if (charging || currentMa < 50 || capacity <= 0) return "—";
        return String.format(Locale.US, "%.1f%% / hour", currentMa * 100f / capacity);
    }

    private String screenOnTime() {
        long minutes = prefs.getLong("screenOnMs", 0L) / 60000L;
        if (minutes <= 0) return "—";
        return (minutes / 60) + "h " + (minutes % 60) + "m";
    }

    private String deepSleepTime() {
        long minutes = prefs.getLong(charging ? "lastDischargeDeepSleepMs" : "dischargeDeepSleepMs", 0L) / 60000L;
        if (minutes <= 0) return "—";
        return (minutes / 60) + "h " + (minutes % 60) + "m";
    }

    private int dischargeMah() { return prefs.getInt(charging ? "lastDischargeMah" : "dischargeMah", 0); }

    private int lastChargeEnergyMah() { return prefs.getInt("lastChargeEnergyMah", 0); }

    private String lastChargeRange() {
        long ended = prefs.getLong("lastChargeEndAt", 0L);
        if (ended <= 0L) return "—";
        return prefs.getInt("lastChargeStartLevel", 0) + "% → " + prefs.getInt("lastChargeEndLevel", 0) + "%";
    }

    private String lastChargeDuration() {
        long minutes = prefs.getLong("lastChargeDurationMin", 0L);
        return minutes > 0L ? formatDuration(minutes) : "—";
    }

    private String dischargePercent(boolean screenOn) {
        String key = screenOn ? "dischargeScreenOnPercent" : "dischargeScreenOffPercent";
        if (charging) key = "last" + Character.toUpperCase(key.charAt(0)) + key.substring(1);
        float value = prefs.getFloat(key, 0f);
        return String.format(Locale.US, "%.0f%%", value);
    }

    private String dischargeDuration(boolean screenOn) {
        String key = screenOn ? "dischargeScreenOnMs" : "dischargeScreenOffMs";
        if (charging) key = "last" + Character.toUpperCase(key.charAt(0)) + key.substring(1);
        long minutes = prefs.getLong(key, 0L) / 60000L;
        return minutes <= 0 ? "—" : formatDuration(minutes);
    }

    private String dischargeRuntime(boolean screenOn) {
        float percent = prefs.getFloat(screenOn ? "dischargeScreenOnPercent" : "dischargeScreenOffPercent", 0f);
        long minutes = prefs.getLong(screenOn ? "dischargeScreenOnMs" : "dischargeScreenOffMs", 0L) / 60000L;
        if (percent > 0f && minutes >= 5) return formatDuration(Math.max(1, Math.round(level * minutes / percent)));
        if (charging || currentMa < 50) return "—";
        int modeCurrent = screenOn ? currentMa : Math.max(50, Math.round(currentMa * .35f));
        return formatDuration(Math.max(1, Math.round(estimatedCapacityMah() * level / 100f * 60f / modeCurrent)));
    }

    private String chargeSpeed(boolean screenOn) {
        int mah = prefs.getInt(screenOn ? "chargeScreenOnMah" : "chargeScreenOffMah", 0);
        long minutes = prefs.getLong(screenOn ? "chargeScreenOnMs" : "chargeScreenOffMs", 0L) / 60000L;
        if (mah <= 0 || minutes < 5) return "—";
        return String.format(Locale.US, "%.0f mA", mah * 60f / minutes);
    }

    private int chargeCycles() { return prefs.getInt("chargeCycles", 0); }

    private String chargingEfficiency() {
        int charged = prefs.getInt("totalChargedMah", 0);
        int cycles = chargeCycles();
        if (charged <= 0 || cycles <= 0) return "—";
        return String.format(Locale.US, "%.0f%%", charged * 100f / (designCapacityMah() * cycles));
    }

    private void setOverlayEnabled(boolean enabled) {
        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(getContext())) {
            try { getContext().startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getContext().getPackageName()))); } catch (Exception ignored) { getContext().startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)); }
            return;
        }
        overlayEnabled = enabled;
        prefs.edit().putBoolean("overlayEnabled", overlayEnabled).apply();
        Intent service = new Intent(getContext(), BatteryOverlayService.class);
        if (overlayEnabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) getContext().startForegroundService(service); else getContext().startService(service);
        } else {
            getContext().stopService(service);
        }
        invalidate();
    }

    private void showSettings() {
        String[] options = {"Dark theme", "AMOLED black", "Light theme", "Notification settings", "Overlay permission", "Data & privacy", "Backup & restore", "Quick tutorial"};
        new AlertDialog.Builder(getContext()).setTitle("Settings").setItems(options, (dialog, which) -> {
            if (which == 0) { light = false; amoled = false; }
            else if (which == 1) { light = false; amoled = true; }
            else if (which == 2) { light = true; amoled = false; }
            else if (which == 3) {
                try {
                    Intent notificationSettings = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, getContext().getPackageName());
                    getContext().startActivity(notificationSettings);
                } catch (Exception ignored) { }
            } else if (which == 4) {
                try { getContext().startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getContext().getPackageName()))); } catch (Exception ignored) { }
            } else if (which == 5) {
                showDataPrivacy();
            } else if (which == 6) {
                showBackupRestore();
            } else {
                showTutorial(true);
            }
            prefs.edit().putBoolean("lightTheme", light).putBoolean("amoledTheme", amoled).apply();
            invalidate();
        }).show();
    }

    private void showDataPrivacy() {
        new AlertDialog.Builder(getContext())
                .setTitle("Data & privacy")
                .setMessage("Ampere collects battery readings locally for your history and analysis: time, battery level, charging state, current, temperature, voltage and screen state.\n\nNo battery readings, account identifiers, location or installed-app lists are uploaded. The update checker only requests its configured version file.\n\nUse History → Export CSV whenever you want to analyze or share your data.")
                .setPositiveButton("Export CSV", (dialog, which) -> exportHistory())
                .setNeutralButton("Delete local data", (dialog, which) -> confirmDeleteData())
                .setNegativeButton("Close", null)
                .show();
    }

    private void confirmDeleteData() {
        new AlertDialog.Builder(getContext())
                .setTitle("Delete local data?")
                .setMessage("This removes local history, sessions, health samples, telemetry and settings. Android backup may still contain an older copy until it is replaced.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> {
                    Context context = getContext();
                    SharedPreferences data = context.getSharedPreferences("ampere-data", Context.MODE_PRIVATE);
                    data.edit().clear().apply();
                    BackupManager.dataChanged(context.getPackageName());
                    reloadStoredData();
                    Intent battery = ((Activity) context).registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                    if (battery != null) readBattery(battery);
                    Toast.makeText(context, "Lokale Daten gelöscht.", Toast.LENGTH_LONG).show();
                })
                .show();
    }

    private void showBackupRestore() {
        new AlertDialog.Builder(getContext())
                .setTitle("Backup & restore")
                .setMessage("Updates keep your data automatically. Before uninstalling, create a backup and restore it after reinstalling. Android cloud/device backup may also restore these settings when enabled on your device.")
                .setPositiveButton("Create backup", (dialog, which) -> ((MainActivity) getContext()).createBackup())
                .setNeutralButton("Restore backup", (dialog, which) -> ((MainActivity) getContext()).restoreBackup())
                .setNegativeButton("Close", null)
                .show();
    }

    void startSavedOverlay() {
        if (!overlayEnabled) return;
        Intent service = new Intent(getContext(), BatteryOverlayService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) getContext().startForegroundService(service); else getContext().startService(service);
    }

    void showTutorial(boolean force) {
        if (!force && prefs.getBoolean("tutorialShown", false)) return;
        new AlertDialog.Builder(getContext())
                .setTitle("Welcome to Ampere")
                .setMessage("Ampere measures battery current, charging speed, discharge use and estimated capacity locally.\n\n1. Keep the monitor notification enabled for background history.\n2. Set the charge alarm slider to the level where you want a reminder to unplug.\n3. For the most accurate health estimate, start the manual benchmark below 25% and finish above 95%.\n\nOptional: grant Usage access for foreground-app estimates and Overlay permission for live stats over other apps.")
                .setNegativeButton("Skip", (dialog, which) -> prefs.edit().putBoolean("tutorialShown", true).apply())
                .setPositiveButton("Set capacity", (dialog, which) -> {
                    prefs.edit().putBoolean("tutorialShown", true).apply();
                    editDesignCapacity();
                }).show();
    }

    private float u(float value) { return value * density; }
    private void fill(Canvas c, int color) { p.setStyle(Paint.Style.FILL); p.setColor(color); }
    private void stroke(Canvas c, int color, float width) { p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(u(width)); p.setStrokeCap(Paint.Cap.ROUND); p.setStrokeJoin(Paint.Join.ROUND); p.setColor(color); }
    private void type(float size, int color, boolean bold) { p.setTextSize(u(size)); p.setColor(color); p.setTypeface(Typeface.create("sans", bold ? Typeface.BOLD : Typeface.NORMAL)); p.setStyle(Paint.Style.FILL); }
    private void text(Canvas c, String value, float x, float y, float size, int color, boolean bold) { type(size, color, bold); c.drawText(value, u(x), u(y), p); }
    private void rounded(Canvas c, float l, float t, float r, float b, float radius, int color) { fill(c, color); rect.set(u(l), u(t), u(r), u(b)); c.drawRoundRect(rect, u(radius), u(radius), p); }
    private void line(Canvas c, float x1, float y1, float x2, float y2, int color, float width) { stroke(c, color, width); c.drawLine(u(x1), u(y1), u(x2), u(y2), p); }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        float w = getWidth() / density;
        float h = getHeight() / density;
        int bg = light ? Color.rgb(243, 245, 239) : (amoled ? Color.BLACK : Color.rgb(17, 19, 24));
        int panel = light ? Color.WHITE : (amoled ? Color.rgb(5, 5, 5) : Color.rgb(25, 28, 35));
        int raised = light ? Color.rgb(238, 241, 233) : (amoled ? Color.rgb(12, 12, 12) : Color.rgb(29, 32, 40));
        int border = light ? Color.rgb(223, 228, 217) : (amoled ? Color.rgb(36, 36, 36) : Color.rgb(43, 47, 56));
        int primary = light ? Color.rgb(23, 26, 29) : Color.rgb(242, 244, 239);
        int muted = light ? Color.rgb(105, 113, 105) : Color.rgb(138, 145, 157);
        int faint = light ? Color.rgb(154, 164, 155) : Color.rgb(102, 109, 121);
        fill(c, bg); c.drawRect(0, 0, getWidth(), getHeight(), p);

        drawHeader(c, w, primary, muted, border, panel);
        if (page == 0) drawOverview(c, w, h, panel, raised, border, primary, muted, faint);
        else if (page == 1) drawChargingPage(c, w, h, panel, raised, border, primary, muted, faint);
        else if (page == 2) drawDischargingPage(c, w, h, panel, raised, border, primary, muted, faint);
        else if (page == 3) drawHealthPage(c, w, h, panel, raised, border, primary, muted, faint);
        else drawHistoryPage(c, w, h, panel, raised, border, primary, muted, faint);
    }

    private void drawHeader(Canvas c, float w, int primary, int muted, int border, int panel) {
        rounded(c, 18, 18, 48, 48, 11, lime);
        drawBolt(c, 33, 33, Color.rgb(26, 32, 17), 1.1f);
        text(c, "Ampere", 57, 39, 17, primary, true);
        text(c, page == 0 ? "Monitor  /  Overview" : "Monitor  /  " + pageName(), 18, 79, 10, muted, false);
        text(c, page == 0 ? "Overview" : pageName(), 18, 111, 30, primary, true);
        rounded(c, w - 91, 22, w - 19, 50, 16, panel);
        stroke(c, border, 1); rect.set(u(w - 91), u(22), u(w - 19), u(50)); c.drawRoundRect(rect, u(16), u(16), p);
        fill(c, lime); c.drawCircle(u(w - 75), u(36), u(4), p); text(c, "Live", w - 65, 40, 10, primary, true);
        rounded(c, w - 138, 22, w - 101, 50, 8, panel); stroke(c, border, 1); rect.set(u(w - 138), u(22), u(w - 101), u(50)); c.drawRoundRect(rect, u(8), u(8), p);
        drawSun(c, w - 119, 36, muted);
        rounded(c, w - 178, 22, w - 143, 50, 8, panel); stroke(c, border, 1); rect.set(u(w - 178), u(22), u(w - 143), u(50)); c.drawRoundRect(rect, u(8), u(8), p);
        fill(c, muted); c.drawCircle(u(w - 160), u(29), u(1.5f), p); c.drawCircle(u(w - 160), u(36), u(1.5f), p); c.drawCircle(u(w - 160), u(43), u(1.5f), p);
        drawNav(c, w, primary, muted, border, panel);
        line(c, 18, 166, w - 18, 166, border, 1);
    }

    private void drawNav(Canvas c, float w, int primary, int muted, int border, int panel) {
        String[] labels = {"Overview", "Charge", "Drain", "Health", "History"};
        float cell = (w - 36) / 5f;
        for (int i = 0; i < labels.length; i++) {
            float x = 18 + i * cell;
            if (page == i) {
                rounded(c, x, 132, x + cell - 5, 157, 7, panel);
                fill(c, lime); c.drawCircle(u(x + 9), u(144), u(3), p);
            }
            text(c, labels[i], x + 16, 148, 9, page == i ? primary : muted, page == i);
        }
    }

    private void drawOverview(Canvas c, float w, float h, int panel, int raised, int border, int primary, int muted, int faint) {
        float top = 182;
        float heroW = Math.min(w - 36, 470);
        float heroH = 320;
        rounded(c, 18, top, 18 + heroW, top + heroH, 12, panel);
        stroke(c, border, 1); rect.set(u(18), u(top), u(18 + heroW), u(top + heroH)); c.drawRoundRect(rect, u(12), u(12), p);
        text(c, "BATTERY LEVEL", 36, top + 31, 10, muted, true);
        text(c, "Live status", 36, top + 56, 17, primary, true);
        drawGauge(c, 135, top + 170, 101, level, primary, faint);
        text(c, level + "%", 91, top + 178, 52, primary, true);
        text(c, charging ? "Charging" : "On battery", 108, top + 204, 10, muted, false);
        int health = healthPercent();
        text(c, health == 0 ? "Not measured" : (health > 80 ? "Good condition" : "Needs attention"), 255, top + 117, 17, primary, true);
        text(c, health == 0 ? "Complete a low-to-full charge benchmark." : (health > 80 ? "Battery is within its expected range." : "Capacity is below the expected range."), 255, top + 141, 10, muted, false);
        text(c, "Estimated full capacity", 255, top + 181, 10, muted, false);
        text(c, health > 0 ? String.format(Locale.US, "%,d mAh", estimatedCapacityMah()) : "—", 255, top + 201, 12, primary, true);
        rounded(c, 255, top + 215, 18 + heroW - 28, top + 219, 3, border);
        if (health > 0) rounded(c, 255, top + 215, 255 + (heroW - 46) * Math.min(1f, health / 100f), top + 219, 3, lime);
        text(c, "Design capacity " + String.format(Locale.US, "%,d mAh", designCapacityMah()), 255, top + 236, 9, faint, false);
        rounded(c, 36, top + 263, 18 + heroW - 36, top + 301, 8, raised);
        drawBolt(c, 52, top + 282, lime, .8f);
        text(c, charging ? "Charger connected" : "On battery", 68, top + 278, 10, primary, true);
        String powerText = currentMa > 0 ? String.format(Locale.US, "Approx. %.1f W live draw", currentMa * voltage / 1000f) : "Waiting for current reading";
        text(c, charging ? "Live Android reading" : powerText, 68, top + 293, 9, muted, false);
        text(c, currentMa > 0 ? currentMa + " mA" : "—", 285, top + 285, 9, lime, false);
        rounded(c, 18 + heroW - 72, top + 274, 18 + heroW - 41, top + 290, 9, charging ? Color.rgb(87, 108, 48) : border);
        rounded(c, charging ? 18 + heroW - 56 : 18 + heroW - 70, top + 276, charging ? 18 + heroW - 43 : 18 + heroW - 57, top + 288, 6, charging ? lime : muted);

        float cardsTop = top + heroH + 14;
        float cardGap = 12;
        float cardW = (w - 36 - cardGap) / 2f;
        drawStat(c, 18, cardsTop, cardW, 105, "Battery health", healthDisplay(), health > 0 ? "%" : "", lime, primary, muted, border, panel, "heart");
        drawStat(c, 18 + cardW + cardGap, cardsTop, cardW, 105, "Battery temperature", temperatureDisplay(), temperature > 0f ? "°C" : "", amber, primary, muted, border, panel, "temp");
        drawStat(c, 18, cardsTop + 117, cardW, 105, "Voltage", voltageDisplay(), voltage > 0f ? "V" : "", blue, primary, muted, border, panel, "bolt");
        drawStat(c, 18 + cardW + cardGap, cardsTop + 117, cardW, 105, "Screen-on time", screenOnTime(), "", Color.rgb(180, 154, 255), primary, muted, border, panel, "clock");

        float lowerTop = cardsTop + 234;
        drawChart(c, 18, lowerTop, w - 36, 360, panel, border, primary, muted, faint);
    }

    private void drawChargingPage(Canvas c, float w, float h, int panel, int raised, int border, int primary, int muted, int faint) {
        float y = 182;
        rounded(c, 18, y, w - 18, y + 300, 12, panel); stroke(c, border, 1); rect.set(u(18), u(y), u(w - 18), u(y + 300)); c.drawRoundRect(rect, u(12), u(12), p);
        text(c, "CHARGING SESSION", 36, y + 31, 10, muted, true);
        text(c, charging ? "Power connected" : "Most recent charge", 36, y + 58, 18, primary, true);
        drawBolt(c, 53, y + 105, lime, 1);
        text(c, charging && currentMa > 0 ? currentMa + " mA" : "—", 77, y + 112, 31, primary, true);
        text(c, charging ? "live charge current" : "unplugged · historical data", 78, y + 132, 9, muted, false);
        line(c, w * .54f, y + 86, w * .54f, y + 156, border, 1);
        text(c, charging ? "Time to full" : "Last charge", w * .6f, y + 96, 10, muted, false);
        text(c, charging ? timeToFull() : lastChargeRange(), w * .6f, y + 126, 20, primary, true);
        text(c, charging ? "estimated" : lastChargeDuration(), w * .6f, y + 145, 9, faint, false);
        text(c, "Charge limit", 36, y + 190, 10, muted, false);
        text(c, chargeLimit + "%", w - 67, y + 190, 10, lime, true);
        rounded(c, 36, y + 205, w - 36, y + 209, 3, border);
        rounded(c, 36, y + 205, 36 + (w - 72) * chargeLimit / 100f, y + 209, 3, lime);
        text(c, "Charge speed on / off: " + chargeSpeed(true) + " / " + chargeSpeed(false), 36, y + 238, 9, faint, false);
        rounded(c, 36, y + 257, w - 36, y + 287, 7, raised);
        text(c, "Charge alarm", 50, y + 276, 10, primary, true);
        text(c, chargeAlarm ? "Enabled" : "Disabled", w - 106, y + 276, 9, chargeAlarm ? lime : muted, false);
        rounded(c, w - 70, y + 265, w - 40, y + 281, 9, chargeAlarm ? Color.rgb(87, 108, 48) : border);
        rounded(c, chargeAlarm ? w - 55 : w - 68, y + 267, chargeAlarm ? w - 42 : w - 55, y + 279, 6, chargeAlarm ? lime : muted);
        rounded(c, 36, y + 295, w - 36, y + 325, 7, raised);
        text(c, "Live stats overlay", 50, y + 314, 10, primary, true);
        text(c, overlayEnabled ? "Enabled" : "Disabled", w - 106, y + 314, 9, overlayEnabled ? lime : muted, false);
        rounded(c, w - 70, y + 303, w - 40, y + 319, 9, overlayEnabled ? Color.rgb(87, 108, 48) : border);
        rounded(c, overlayEnabled ? w - 55 : w - 68, y + 305, overlayEnabled ? w - 42 : w - 55, y + 317, 6, overlayEnabled ? lime : muted);
        int energyAdded = charging ? sessionEnergyMah() : lastChargeEnergyMah();
        drawStat(c, 18, y + 350, (w - 48) / 2f, 105, "Energy added", energyAdded > 0 ? "+" + energyAdded : "—", "mAh", lime, primary, muted, border, panel, "bolt");
        drawStat(c, 30 + (w - 48) / 2f, y + 350, (w - 48) / 2f, 105, "Battery health", healthDisplay(), healthPercent() > 0 ? "%" : "", lime, primary, muted, border, panel, "heart");
    }

    private void drawDischargingPage(Canvas c, float w, float h, int panel, int raised, int border, int primary, int muted, int faint) {
        float y = 182;
        rounded(c, 18, y, w - 18, y + 300, 12, panel); stroke(c, border, 1); rect.set(u(18), u(y), u(w - 18), u(y + 300)); c.drawRoundRect(rect, u(12), u(12), p);
        text(c, "DISCHARGE SESSION", 36, y + 31, 10, muted, true);
        text(c, charging ? "Most recent discharge" : "Battery use overview", 36, y + 58, 18, primary, true);
        int displayLevel = charging ? prefs.getInt("lastDischargeEndLevel", level) : level;
        drawGauge(c, 94, y + 145, 55, displayLevel, primary, faint);
        text(c, displayLevel + "%", 70, y + 153, 22, primary, true);
        text(c, "remaining", 69, y + 173, 9, muted, false);
        line(c, w * .54f, y + 94, w * .54f, y + 196, border, 1);
        text(c, "Mixed runtime", w * .6f, y + 106, 10, muted, false);
        text(c, runtimeEstimate(), w * .6f, y + 138, 20, primary, true);
        text(c, "based on recent use", w * .6f, y + 157, 9, faint, false);
        text(c, "Screen on / off", w * .6f, y + 187, 10, muted, false);
        text(c, dischargeRuntime(true) + " / " + dischargeRuntime(false), w * .6f, y + 207, 11, blue, true);
        drawStat(c, 18, y + 316, (w - 48) / 2f, 105, "Screen-on time", dischargeDuration(true), "", Color.rgb(180, 154, 255), primary, muted, border, panel, "clock");
        drawStat(c, 30 + (w - 48) / 2f, y + 316, (w - 48) / 2f, 105, "Energy used", dischargeMah() > 0 ? String.valueOf(dischargeMah()) : "—", "mAh", blue, primary, muted, border, panel, "arrow");
        rounded(c, 18, y + 438, w - 18, y + 520, 12, panel); stroke(c, border, 1); rect.set(u(18), u(y + 438), u(w - 18), u(y + 520)); c.drawRoundRect(rect, u(12), u(12), p);
        text(c, "Usage note", 36, y + 468, 10, muted, true);
        text(c, "Screen on " + dischargePercent(true) + " · off " + dischargePercent(false) + " · " + dischargeMah() + " mAh", 36, y + 493, 9, primary, false);
        text(c, "Deep sleep: " + deepSleepTime() + " · on " + dischargeDuration(true) + " / off " + dischargeDuration(false), 36, y + 510, 9, primary, false);
        rounded(c, 18, y + 540, w - 18, y + 715, 12, panel); stroke(c, border, 1); rect.set(u(18), u(y + 540), u(w - 18), u(y + 715)); c.drawRoundRect(rect, u(12), u(12), p);
        text(c, "Foreground app usage", 36, y + 571, 13, primary, true);
        if (hasUsageAccess()) {
            drawUsageRows(c, w, y + 600, primary, muted, faint);
        } else {
            text(c, "Optional Android permission", 36, y + 603, 10, amber, true);
            text(c, "Allow usage access to see which apps use", 36, y + 627, 10, muted, false);
            text(c, "the most battery while in the foreground.", 36, y + 645, 10, muted, false);
            text(c, "Tap here to open Android settings", 36, y + 683, 9, lime, true);
        }
    }

    private boolean hasUsageAccess() {
        AppOpsManager ops = (AppOpsManager) getContext().getSystemService(Context.APP_OPS_SERVICE);
        if (ops == null) return false;
        int mode = ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), getContext().getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    private void drawUsageRows(Canvas c, float w, float y, int primary, int muted, int faint) {
        UsageStatsManager manager = (UsageStatsManager) getContext().getSystemService(Context.USAGE_STATS_SERVICE);
        if (manager == null) return;
        long end = System.currentTimeMillis();
        long start = prefs.getLong("dischargeStartAt", end - 24 * 60 * 60 * 1000L);
        if (start >= end) start = end - 60 * 60 * 1000L;
        List<UsageStats> stats = manager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end);
        if (stats == null) return;
        Collections.sort(stats, new Comparator<UsageStats>() {
            @Override public int compare(UsageStats left, UsageStats right) { return Long.compare(right.getTotalTimeInForeground(), left.getTotalTimeInForeground()); }
        });
        long totalForegroundMs = 0L;
        for (UsageStats stat : stats) if (!stat.getPackageName().equals(getContext().getPackageName())) totalForegroundMs += stat.getTotalTimeInForeground();
        int totalEnergy = prefs.getInt("dischargeMah", 0);
        int row = 0;
        for (UsageStats stat : stats) {
            if (stat.getTotalTimeInForeground() < 60 * 1000L || stat.getPackageName().equals(getContext().getPackageName())) continue;
            String app = stat.getPackageName();
            try { app = getContext().getPackageManager().getApplicationLabel(getContext().getPackageManager().getApplicationInfo(stat.getPackageName(), 0)).toString(); } catch (Exception ignored) { }
            long minutes = stat.getTotalTimeInForeground() / 60000L;
            text(c, app, 36, y + row * 27, 10, primary, true);
            int appMah = totalForegroundMs > 0L ? Math.round(totalEnergy * stat.getTotalTimeInForeground() / (float) totalForegroundMs) : 0;
            text(c, minutes + " min · " + appMah + " mAh", w - 145, y + row * 27, 8, muted, false);
            line(c, 36, y + row * 27 + 9, w - 36, y + row * 27 + 9, Color.rgb(43, 47, 56), 1);
            if (++row == 3) break;
        }
        if (row == 0) text(c, "No app usage recorded since unplugging.", 36, y, 9, faint, false);
    }

    private void drawHealthPage(Canvas c, float w, float h, int panel, int raised, int border, int primary, int muted, int faint) {
        float y = 182;
        rounded(c, 18, y, w - 18, y + 300, 12, panel); stroke(c, border, 1); rect.set(u(18), u(y), u(w - 18), u(y + 300)); c.drawRoundRect(rect, u(12), u(12), p);
        text(c, "BATTERY HEALTH", 36, y + 31, 10, muted, true);
        int health = healthPercent();
        text(c, health == 0 ? "Not measured" : (health > 80 ? "Good condition" : "Needs attention"), 36, y + 62, 23, primary, true);
        text(c, "Estimated capacity", 36, y + 102, 10, muted, false);
        text(c, health > 0 ? String.format(Locale.US, "%,d mAh", estimatedCapacityMah()) : "—", 36, y + 132, 28, lime, true);
        text(c, "of " + String.format(Locale.US, "%,d mAh", designCapacityMah()) + " design capacity", 36, y + 153, 10, muted, false);
        rounded(c, 36, y + 181, w - 36, y + 187, 3, border);
        if (health > 0) rounded(c, 36, y + 181, 36 + (w - 72) * health / 100f, y + 187, 3, lime);
        text(c, health > 0 ? health + "% of original capacity" : "Run a benchmark to estimate capacity", 36, y + 211, 10, primary, true);
        text(c, "Battery wear", w - 122, y + 211, 10, muted, false);
        text(c, health > 0 ? (100 - Math.min(100, health)) + "%" : "—", w - 58, y + 211, 10, amber, true);
        line(c, 36, y + 232, w - 36, y + 232, border, 1);
        text(c, "Temperature today", 36, y + 257, 10, muted, false);
        text(c, temperature > 0f ? String.format(Locale.US, "%.1f°C", temperature) : "—", w - 93, y + 257, 10, amber, true);
        text(c, "Charging efficiency", 36, y + 282, 10, muted, false);
        text(c, chargingEfficiency(), w - 75, y + 282, 10, blue, true);
        drawStat(c, 18, y + 316, (w - 48) / 2f, 105, "Voltage", voltageDisplay(), voltage > 0f ? "V" : "", blue, primary, muted, border, panel, "bolt");
        drawStat(c, 30 + (w - 48) / 2f, y + 316, (w - 48) / 2f, 105, "Charge cycles", String.valueOf(chargeCycles()), "", Color.rgb(180, 154, 255), primary, muted, border, panel, "grid");
        rounded(c, 18, y + 438, w - 18, y + 520, 12, panel); stroke(c, border, 1); rect.set(u(18), u(y + 438), u(w - 18), u(y + 520)); c.drawRoundRect(rect, u(12), u(12), p);
        text(c, "How this estimate works", 36, y + 468, 10, muted, true);
        text(c, "Capacity is estimated from charge and discharge", 36, y + 493, 10, primary, false);
        text(c, "samples collected on this device.", 36, y + 510, 10, primary, false);
        rounded(c, 18, y + 548, w - 18, y + 615, 12, panel); stroke(c, border, 1); rect.set(u(18), u(y + 548), u(w - 18), u(y + 615)); c.drawRoundRect(rect, u(12), u(12), p);
        text(c, benchmarkActive ? "Benchmark in progress" : "Manual benchmark", 36, y + 575, 11, primary, true);
        text(c, benchmarkActive ? "Charge above 95% to finish" : "Start below 25% for best results", 36, y + 595, 9, muted, false);
        rounded(c, w - 102, y + 566, w - 38, y + 596, 7, benchmarkActive ? Color.rgb(87, 108, 48) : lime);
        text(c, benchmarkActive ? "Active" : "Start", w - 88, y + 585, 9, benchmarkActive ? lime : Color.rgb(23, 28, 16), true);
        rounded(c, 18, y + 630, w - 18, y + 697, 12, panel); stroke(c, border, 1); rect.set(u(18), u(y + 630), u(w - 18), u(y + 697)); c.drawRoundRect(rect, u(12), u(12), p);
        text(c, "Design capacity", 36, y + 659, 11, primary, true);
        text(c, "Factory rating used for health estimates", 36, y + 680, 9, muted, false);
        text(c, String.format(Locale.US, "%,d mAh", designCapacityMah()), w - 112, y + 667, 10, lime, true);
        rounded(c, 18, y + 710, w - 18, y + 850, 12, panel); stroke(c, border, 1); rect.set(u(18), u(y + 710), u(w - 18), u(y + 850)); c.drawRoundRect(rect, u(12), u(12), p);
        text(c, "Capacity samples", 36, y + 740, 12, primary, true);
        if (healthSamples.size() < 2) {
            text(c, "Complete more full charges to build a health trend.", 36, y + 781, 9, muted, false);
        } else {
            float chartX = 36, chartY = y + 758, chartW = w - 72, chartH = 58;
            line(c, chartX, chartY + chartH, chartX + chartW, chartY + chartH, border, 1);
            Path trend = new Path();
            for (int i = 0; i < healthSamples.size(); i++) {
                float normalized = Math.max(0f, Math.min(1.1f, healthSamples.get(i) / (float) designCapacityMah()));
                float px = chartX + chartW * i / Math.max(1, healthSamples.size() - 1);
                float py = chartY + chartH - normalized * chartH / 1.1f;
                if (i == 0) trend.moveTo(u(px), u(py)); else trend.lineTo(u(px), u(py));
            }
            stroke(c, lime, 2); c.drawPath(trend, p);
            text(c, healthSamples.get(0) + " mAh", chartX, y + 835, 8, faint, false);
            text(c, healthSamples.get(healthSamples.size() - 1) + " mAh", w - 92, y + 835, 8, faint, false);
        }
    }

    private void drawHistoryPage(Canvas c, float w, float h, int panel, int raised, int border, int primary, int muted, int faint) {
        float y = 182;
        rounded(c, 18, y, w - 18, y + 610, 12, panel); stroke(c, border, 1); rect.set(u(18), u(y), u(w - 18), u(y + 610)); c.drawRoundRect(rect, u(12), u(12), p);
        text(c, "HISTORY", 36, y + 31, 10, muted, true);
        text(c, "Charge & discharge sessions", 36, y + 61, 20, primary, true);
        text(c, "Stored locally · up to 150 sessions", 36, y + 83, 10, faint, false);
        line(c, 36, y + 105, w - 36, y + 105, border, 1);
        if (sessions.isEmpty()) {
            text(c, "No completed sessions yet.", 36, y + 145, 11, primary, true);
            text(c, "Leave the monitor running while charging or using", 36, y + 171, 10, muted, false);
            text(c, "your phone to create local history entries.", 36, y + 189, 10, muted, false);
        } else {
            text(c, "Date", 36, y + 130, 9, faint, true);
            text(c, "Type", w * .53f, y + 130, 9, faint, true);
            text(c, "Change", w * .71f, y + 130, 9, faint, true);
            text(c, "Length", w - 75, y + 130, 9, faint, true);
            int row = 0;
            for (String session : sessions) {
                String[] parts = session.split(",", 4);
                if (parts.length < 4) continue;
                float rowY = y + 160 + row * 44;
                line(c, 36, rowY - 18, w - 36, rowY - 18, border, 1);
                text(c, parts[3], 36, rowY, 9, muted, false);
                text(c, parts[0], w * .53f, rowY, 9, parts[0].equals("Charge") ? lime : blue, true);
                text(c, parts[1], w * .71f, rowY, 9, primary, true);
                text(c, parts[2], w - 75, rowY, 9, faint, false);
                if (++row == 5) break;
            }
        }
        line(c, 36, y + 405, w - 36, y + 405, border, 1);
        text(c, "Samples recorded", 36, y + 438, 10, muted, false);
        text(c, String.valueOf(longHistory.size()), w - 75, y + 438, 11, lime, true);
        text(c, "Rolling window: up to 30 local days", 36, y + 464, 9, faint, false);
        text(c, "Deep sleep", 36, y + 494, 10, muted, false);
        text(c, deepSleepTime(), w - 75, y + 494, 11, Color.rgb(180, 154, 255), true);
        text(c, "Battery readings stay on this device.", 36, y + 524, 10, primary, true);
        text(c, "Export only when you choose; no account or subscription.", 36, y + 548, 9, muted, false);
        rounded(c, w - 136, y + 566, w - 36, y + 600, 8, lime);
        text(c, "Export CSV", w - 119, y + 588, 9, Color.rgb(23, 28, 16), true);
    }

    private void exportHistory() {
        StringBuilder csv = new StringBuilder("type,change,duration,date,start_level,end_level,energy_mah\n");
        for (String session : sessions) csv.append(session).append('\n');
        csv.append("\nlevel_percent\n");
        for (Integer point : longHistory) csv.append(point).append('\n');
        csv.append("\ntelemetry_timestamp_ms,level_percent,charging,current_ma,temperature_c,voltage_v,charge_counter_mah,screen_on\n");
        String telemetry = prefs.getString("telemetrySamples", "");
        if (!telemetry.isEmpty()) csv.append(telemetry).append('\n');
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/csv");
        share.putExtra(Intent.EXTRA_SUBJECT, "Ampere battery data export");
        share.putExtra(Intent.EXTRA_TEXT, csv.toString());
        getContext().startActivity(Intent.createChooser(share, "Export battery data"));
    }

    private void showSessionDetails(int index) {
        if (index < 0 || index >= sessions.size()) return;
        String[] parts = sessions.get(index).split(",", 7);
        if (parts.length < 4) return;
        StringBuilder details = new StringBuilder();
        details.append(parts[0]).append(" session\n");
        details.append("Date: ").append(parts[3]).append('\n');
        details.append("Change: ").append(parts[1]).append('\n');
        details.append("Duration: ").append(parts[2]);
        if (parts.length >= 7) {
            details.append("\nStart: ").append(parts[4]).append('%');
            details.append("\nEnd: ").append(parts[5]).append('%');
            details.append("\nEnergy: ").append(parts[6]).append(" mAh");
        }
        new AlertDialog.Builder(getContext()).setTitle("Session details").setMessage(details.toString()).setPositiveButton("Close", null).show();
    }

    private void drawStat(Canvas c, float x, float y, float width, float height, String label, String value, String unit, int accent, int primary, int muted, int border, int panel, String icon) {
        rounded(c, x, y, x + width, y + height, 12, panel); stroke(c, border, 1); rect.set(u(x), u(y), u(x + width), u(y + height)); c.drawRoundRect(rect, u(12), u(12), p);
        rounded(c, x + 15, y + 16, x + 45, y + 46, 8, Color.argb(28, Color.red(accent), Color.green(accent), Color.blue(accent)));
        if (icon.equals("bolt")) drawBolt(c, x + 30, y + 31, accent, .7f); else if (icon.equals("temp")) drawThermometer(c, x + 30, y + 31, accent); else if (icon.equals("heart")) drawHeart(c, x + 30, y + 31, accent); else if (icon.equals("arrow")) drawArrow(c, x + 30, y + 31, accent); else if (icon.equals("grid")) drawGrid(c, x + 30, y + 31, accent); else drawClock(c, x + 30, y + 31, accent);
        text(c, label, x + 58, y + 30, 10, muted, false);
        text(c, value, x + 58, y + 62, 21, primary, true); if (!unit.isEmpty()) text(c, unit, x + 58 + p.measureText(value) / density + 4, y + 62, 10, muted, false);
    }

    private void drawChart(Canvas c, float x, float y, float width, float height, int panel, int border, int primary, int muted, int faint) {
        rounded(c, x, y, x + width, y + height, 12, panel); stroke(c, border, 1); rect.set(u(x), u(y), u(x + width), u(y + height)); c.drawRoundRect(rect, u(12), u(12), p);
        text(c, historyDays == 30 ? "30-day battery level" : "7-day battery level", x + 18, y + 28, 15, primary, true);
        rounded(c, x + width - 100, y + 14, x + width - 62, y + 38, 6, historyDays == 7 ? lime : panel);
        rounded(c, x + width - 58, y + 14, x + width - 18, y + 38, 6, historyDays == 30 ? lime : panel);
        text(c, "7D", x + width - 90, y + 30, 8, historyDays == 7 ? Color.rgb(23, 28, 16) : muted, true);
        text(c, "30D", x + width - 51, y + 30, 8, historyDays == 30 ? Color.rgb(23, 28, 16) : muted, true);
        float chartX = x + 18, chartY = y + 51, chartW = width - 36, chartH = 94;
        for (int i = 0; i < 3; i++) line(c, chartX, chartY + i * 45, chartX + chartW, chartY + i * 45, border, 1);
        float[] values = chartValues();
        if (values.length == 0) {
            text(c, "Waiting for local battery samples.", chartX, chartY + 52, 10, faint, false);
        } else {
            Path path = new Path();
            for (int i = 0; i < values.length; i++) {
                float px = u(values.length == 1 ? chartX : chartX + i * chartW / (values.length - 1));
                float py = u(chartY + chartH - values[i] * chartH);
                if (i == 0) path.moveTo(px, py); else path.lineTo(px, py);
            }
            stroke(c, lime, 2); c.drawPath(path, p);
            fill(c, lime); float lastX = values.length == 1 ? chartX : chartX + chartW, lastY = chartY + chartH - values[values.length - 1] * chartH; c.drawCircle(u(lastX), u(lastY), u(4), p);
        }
        text(c, historyDays == 30 ? "1" : "Mon", chartX, y + 166, 9, faint, false); text(c, historyDays == 30 ? "10" : "Wed", chartX + chartW * .32f, y + 166, 9, faint, false); text(c, historyDays == 30 ? "20" : "Fri", chartX + chartW * .64f, y + 166, 9, faint, false); text(c, historyDays == 30 ? "30" : "Sun", chartX + chartW - 23, y + 166, 9, faint, false);
        text(c, "Average", x + width - 103, y + 58, 9, muted, false);
        text(c, "Recent sessions", x + 18, y + 204, 13, primary, true);
        if (sessions.isEmpty()) {
            text(c, "Sessions will appear after a charge or discharge cycle.", x + 18, y + 230, 9, faint, false);
        } else {
            int row = 0;
            for (String session : sessions) {
                String[] parts = session.split(",", 4);
                if (parts.length < 4) continue;
                float rowY = y + 230 + row * 25;
                line(c, x + 18, rowY - 14, x + width - 18, rowY - 14, border, 1);
                text(c, parts[3], x + 18, rowY, 9, muted, false);
                text(c, parts[0], x + width * .53f, rowY, 9, parts[0].equals("Charge") ? lime : blue, false);
                text(c, parts[1], x + width * .72f, rowY, 9, primary, true);
                text(c, parts[2], x + width - 62, rowY, 9, faint, false);
                row++;
            }
        }
    }

    private float[] chartValues() {
        ArrayList<Integer> source = historyDays == 30 ? longHistory : history;
        if (source.isEmpty()) return new float[0];
        int count = Math.min(12, source.size());
        float[] values = new float[count];
        for (int i = 0; i < count; i++) {
            int value = source.get(source.size() - count + i);
            values[i] = Math.max(0, Math.min(100, value)) / 100f;
        }
        return values;
    }

    private void drawPlaceholder(Canvas c, float w, float h, int page, int panel, int border, int primary, int muted) {
        float y = 182;
        rounded(c, 18, y, w - 18, Math.min(h - 35, y + 380), 12, panel); stroke(c, border, 1); rect.set(u(18), u(y), u(w - 18), u(Math.min(h - 35, y + 380))); c.drawRoundRect(rect, u(12), u(12), p);
        rounded(c, 38, y + 32, 86, y + 80, 12, Color.argb(30, Color.red(lime), Color.green(lime), Color.blue(lime)));
        drawBolt(c, 62, y + 56, lime, 1);
        text(c, "AMPERE MONITOR", 38, y + 114, 10, muted, true);
        text(c, pageName(), 38, y + 145, 23, primary, true);
        text(c, "This view is ready for live device data.", 38, y + 180, 11, muted, false);
        text(c, "The dashboard reads directly from Android and", 38, y + 200, 11, muted, false);
        text(c, "keeps your readings on this device.", 38, y + 219, 11, muted, false);
    }

    private String pageName() { return page == 1 ? "Charging" : page == 2 ? "Discharging" : page == 3 ? "Battery health" : page == 4 ? "History" : "Overview"; }

    private void drawGauge(Canvas c, float cx, float cy, float radius, int value, int primary, int faint) {
        stroke(c, Color.rgb(43, 47, 56), 9); rect.set(u(cx - radius), u(cy - radius), u(cx + radius), u(cy + radius)); c.drawArc(rect, -90, 360, false, p);
        stroke(c, lime, 9); c.drawArc(rect, -90, 360 * value / 100f, false, p);
    }
    private void drawBolt(Canvas c, float cx, float cy, int color, float width) { Path b = new Path(); b.moveTo(u(cx + 3), u(cy - 12)); b.lineTo(u(cx - 6), u(cy + 1)); b.lineTo(u(cx), u(cy + 1)); b.lineTo(u(cx - 3), u(cy + 12)); b.lineTo(u(cx + 7), u(cy - 2)); b.lineTo(u(cx + 1), u(cy - 2)); b.close(); fill(c, color); c.drawPath(b, p); }
    private void drawSun(Canvas c, float cx, float cy, int color) { stroke(c, color, 1.5f); c.drawCircle(u(cx), u(cy), u(4), p); for (int i=0; i<8; i++) { double a=i*Math.PI/4; line(c, cx+(float)Math.cos(a)*7, cy+(float)Math.sin(a)*7, cx+(float)Math.cos(a)*10, cy+(float)Math.sin(a)*10, color, 1.3f); } }
    private void drawHeart(Canvas c, float cx, float cy, int color) { Path path = new Path(); path.moveTo(u(cx),u(cy+8)); path.cubicTo(u(cx-16),u(cy-2),u(cx-9),u(cy-11),u(cx),u(cy-5)); path.cubicTo(u(cx+9),u(cy-11),u(cx+16),u(cy-2),u(cx),u(cy+8)); stroke(c,color,1.7f); c.drawPath(path,p); }
    private void drawThermometer(Canvas c, float cx, float cy, int color) { stroke(c,color,1.7f); c.drawRoundRect(new RectF(u(cx-3),u(cy-11),u(cx+3),u(cy+5)),u(3),u(3),p); c.drawCircle(u(cx),u(cy+7),u(5),p); line(c,cx,cy-7,cx,cy+6,color,1.7f); }
    private void drawClock(Canvas c, float cx, float cy, int color) { stroke(c,color,1.7f); c.drawCircle(u(cx),u(cy),u(9),p); line(c,cx,cy,cx,cy-5,color,1.7f); line(c,cx,cy,cx+4,cy+3,color,1.7f); }
    private void drawArrow(Canvas c, float cx, float cy, int color) { line(c,cx,cy-9,cx,cy+6,color,1.7f); line(c,cx-5,cy+1,cx,cy+6,color,1.7f); line(c,cx+5,cy+1,cx,cy+6,color,1.7f); }
    private void drawGrid(Canvas c, float cx, float cy, int color) { stroke(c,color,1.4f); c.drawRoundRect(new RectF(u(cx-8),u(cy-8),u(cx-1),u(cy-1)),u(1),u(1),p); c.drawRoundRect(new RectF(u(cx+1),u(cy-8),u(cx+8),u(cy-1)),u(1),u(1),p); c.drawRoundRect(new RectF(u(cx-8),u(cy+1),u(cx-1),u(cy+8)),u(1),u(1),p); c.drawRoundRect(new RectF(u(cx+1),u(cy+1),u(cx+8),u(cy+8)),u(1),u(1),p); }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_UP) return true;
        float x = event.getX() / density, y = event.getY() / density;
        if (System.currentTimeMillis() - lastTouch < 80) return true;
        lastTouch = System.currentTimeMillis();
        float w = getWidth() / density;
        if (y < 60 && x > w - 180 && x < w - 143) { showSettings(); return true; }
        if (y < 60 && x > w - 138) { light = !light; amoled = false; prefs.edit().putBoolean("lightTheme", light).putBoolean("amoledTheme", amoled).apply(); invalidate(); return true; }
        if (y >= 132 && y < 166) {
            float cell = (w - 36) / 5f;
            page = Math.max(0, Math.min(4, (int) ((x - 18) / cell)));
            invalidate();
            return true;
        }
        if (page == 0 && y > 445 && y < 520 && x > w - 130) { charging = !charging; invalidate(); return true; }
        if (page == 0 && y > 750 && y < 805 && x > w - 140) {
            historyDays = historyDays == 7 ? 30 : 7;
            prefs.edit().putInt("historyDays", historyDays).apply();
            invalidate();
            return true;
        }
        if (page == 1 && y > 425 && y < 475 && x > w - 130) {
            chargeAlarm = !chargeAlarm;
            prefs.edit().putBoolean("chargeAlarm", chargeAlarm).apply();
            if (!chargeAlarm) {
                android.app.NotificationManager manager = (android.app.NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
                if (manager != null) manager.cancel(8);
                prefs.edit().putBoolean("chargeAlarmSent", false).apply();
            }
            invalidate();
            return true;
        }
        if (page == 1 && y > 365 && y < 410 && x >= 36 && x <= w - 36) {
            chargeLimit = Math.max(50, Math.min(100, Math.round((x - 36) / (w - 72) * 100)));
            prefs.edit().putInt("chargeLimit", chargeLimit).apply();
            if (level < chargeLimit) {
                android.app.NotificationManager manager = (android.app.NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
                if (manager != null) manager.cancel(8);
            }
            invalidate();
            return true;
        }
        if (page == 1 && y > 475 && y < 530 && x > w - 140) {
            setOverlayEnabled(!overlayEnabled);
            return true;
        }
        if (page == 4 && y >= 330 && y < 570 && x < w - 145 && !sessions.isEmpty()) {
            int index = (int) ((y - 342) / 44);
            showSessionDetails(index);
            return true;
        }
        if (page == 4 && y > 740 && y < 795 && x > w - 155) {
            exportHistory();
            return true;
        }
        if (page == 3 && y > 700 && y < 815 && x > w - 140) {
            if (benchmarkActive) {
                benchmarkActive = false;
                prefs.edit().putBoolean("benchmarkActive", false)
                        .remove("benchmarkStartLevel").remove("benchmarkStartCounterMah")
                        .remove("benchmarkChargeLastCounterMah").remove("benchmarkChargeAddedMah").apply();
            } else if (charging || level > 25) {
                Toast.makeText(getContext(), "Start the benchmark below 25% while unplugged.", Toast.LENGTH_LONG).show();
            } else {
                benchmarkActive = true;
                SharedPreferences.Editor editor = prefs.edit().putBoolean("benchmarkActive", true)
                        .putInt("benchmarkStartLevel", level).putInt("benchmarkChargeAddedMah", 0)
                        .remove("benchmarkChargeLastCounterMah");
                if (chargeCounterMah > 0) editor.putInt("benchmarkStartCounterMah", chargeCounterMah);
                else editor.remove("benchmarkStartCounterMah");
                editor.apply();
            }
            invalidate();
            return true;
        }
        if (page == 3 && y > 815 && y < 890) {
            editDesignCapacity();
            return true;
        }
        if (page == 2 && y > 700 && y < 920) {
            try { getContext().startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, Uri.parse("package:" + getContext().getPackageName()))); } catch (Exception ignored) { getContext().startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)); }
            return true;
        }
        return true;
    }
}
