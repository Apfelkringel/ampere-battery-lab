package com.ampere.batterylab;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.backup.BackupManager;
import android.os.BatteryManager;
import android.os.PowerManager;
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
import org.json.JSONArray;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends Activity {
    private static final int CREATE_BACKUP_REQUEST = 1201;
    private static final int RESTORE_BACKUP_REQUEST = 1202;
    private static final int RESEARCH_EXPORT_REQUEST = 1203;
    private static final int CSV_EXPORT_REQUEST = 1204;
    private static final int MAX_BACKUP_BYTES = 4 * 1024 * 1024;
    private static final String DATA_PREFS = "ampere-data";
    private static final String TELEMETRY_PREFS = "ampere-telemetry";
    private static final Set<String> RESTORABLE_DATA_KEYS = new HashSet<>(Arrays.asList(
            "history", "historyLong", "lastSample", "healthSamples", "sessions",
            "sessionStartedAt", "sessionStartLevel", "sessionStartChargeCounterMah", "lastCharging",
            "chargeAlarm", "chargeAlarmSent", "chargeLimit", "benchmarkActive", "benchmarkCapacityMah",
            "benchmarkStartLevel", "benchmarkStartCounterMah", "benchmarkChargeLastCounterMah",
            "benchmarkChargeAddedMah", "benchmarkChargeStatsBaselineMah", "healthSampleSessionAt",
            "lastChargeHealthReason", "totalChargedMah", "chargeCycles", "cycleLastLevel",
            "dischargePercent", "deepSleepMs", "deepSleepClockElapsed", "deepSleepClockUptime", "samplingIntervalMin", "overlayEnabled", "historyDays", "lightTheme",
            "amoledTheme", "designCapacityMah", "tutorialShown", "lastBackupRequestAt",
            "chargeLastAt", "chargeLastCounterMah", "chargeLastLevel", "chargePlugged",
            "chargeScreenOffMah", "chargeScreenOffMs", "chargeScreenOffPercent", "chargeScreenOnMah",
            "chargeScreenOnMs", "chargeScreenOnPercent", "lastChargeDurationMin", "lastChargeEndAt",
            "lastChargeEndLevel", "lastChargeEnergyMah", "lastChargePlugged", "lastChargeScreenOffMah",
            "lastChargeScreenOffMs", "lastChargeScreenOffPercent", "lastChargeScreenOnMah",
            "lastChargeScreenOnMs", "lastChargeScreenOnPercent", "lastChargeStartAt", "lastChargeStartLevel",
            "dischargeLastAt", "dischargeLastCounterMah", "dischargeLastLevel", "dischargeMah",
            "dischargeScreenOffMs", "dischargeScreenOffPercent", "dischargeScreenOnMs",
            "dischargeScreenOnPercent", "dischargeStartAt", "dischargeDeepSleepMs", "dischargeDeepSleepStartElapsed", "dischargeDeepSleepStartUptime", "lastDischargeDeepSleepMs",
            "dischargeWakeups", "lastDischargeWakeups",
            "lastDischargeEndAt", "lastDischargeEndLevel", "lastDischargeMah", "lastDischargeScreenOffMs",
            "lastDischargeScreenOffPercent", "lastDischargeScreenOnMs", "lastDischargeScreenOnPercent",
            "lastDischargeStartAt", "sinceFullActive", "sinceFullDeepSleepMs", "sinceFullLastAt",
            "sinceFullWakeups",
            "sinceFullLastCounterMah", "sinceFullLastLevel", "sinceFullMah", "sinceFullPercent",
            "sinceFullScreenOffMs", "sinceFullScreenOnMs", "sinceFullStartAt", "sinceFullStartLevel",
            "systemCycleCount", "monitorLastCharging", "monitorSampleAt", "monitorSessionStartCounterMah",
            "monitorSessionStartLevel", "monitorSessionStartedAt", "monitoringMs", "screenOffDurationMin",
            "screenOnMs", "screenSampleAt"
    ));
    private static final Set<String> RESTORABLE_TELEMETRY_KEYS = new HashSet<>(Arrays.asList(
            "telemetrySamples", "telemetryLastSampleAt"
    ));
    private BatteryDashboard dashboard;
    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (dashboard == null || intent == null) return;
            String action = intent.getAction();
            if (Intent.ACTION_POWER_CONNECTED.equals(action)
                    || Intent.ACTION_POWER_DISCONNECTED.equals(action)) {
                // Power broadcasts carry no battery extras. Read Android's
                // current sticky battery state so the visible status changes
                // immediately when a cable is connected or removed.
                Intent battery = MainActivity.this.registerReceiver(
                        null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                if (battery != null) dashboard.readBattery(battery);
            } else {
                dashboard.readBattery(intent);
            }
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        migrateTelemetryPrefs(this);
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
        dashboard.setMinimumHeight(contentHeight);
        scroll.addView(dashboard, new ScrollView.LayoutParams(-1, contentHeight));
        setContentView(scroll);
        startMonitorService();
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != getPackageManager().PERMISSION_GRANTED) {
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 44);
        }
        dashboard.startSavedOverlay();
        dashboard.postDelayed(() -> dashboard.showTutorial(false), 1200L);
        IntentFilter batteryFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        batteryFilter.addAction(Intent.ACTION_POWER_CONNECTED);
        batteryFilter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        Intent battery = Build.VERSION.SDK_INT >= 33 ? registerReceiver(batteryReceiver, batteryFilter, Context.RECEIVER_NOT_EXPORTED) : registerReceiver(batteryReceiver, batteryFilter);
        if (battery != null) dashboard.readBattery(battery);
    }

    @Override protected void onResume() {
        super.onResume();
        if (dashboard == null) return;
        startMonitorService();
        dashboard.startSavedOverlay();
        if (UpdateChecker.isUpdateIntent(getIntent())) {
            getIntent().setAction(null);
            UpdateChecker.checkNow(this);
        } else {
            UpdateChecker.check(this);
        }
        Intent battery = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (battery != null) dashboard.readBattery(battery);
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    private void startMonitorService() {
        Intent service = new Intent(this, BatteryMonitorService.class);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(service); else startService(service);
        } catch (IllegalStateException ignored) {
            // The next visible app resume will retry after Android allows it.
        }
    }

    void restartMonitorService() {
        stopService(new Intent(this, BatteryMonitorService.class));
        startMonitorService();
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
        else if (requestCode == RESEARCH_EXPORT_REQUEST) writeResearchExport(uri);
        else if (requestCode == CSV_EXPORT_REQUEST) writeCsvExport(uri);
    }

    private void writeBackup(Uri uri) {
        try (OutputStream stream = getContentResolver().openOutputStream(uri)) {
            if (stream == null) throw new IllegalStateException("No output stream");
            JSONObject root = new JSONObject();
            root.put("schema", 2);
            root.put("package", getPackageName());
            root.put("createdAt", System.currentTimeMillis());
            JSONObject values = new JSONObject();
            encodePreferences(values, getSharedPreferences(DATA_PREFS, MODE_PRIVATE), RESTORABLE_DATA_KEYS);
            root.put("preferences", values);
            JSONObject telemetryValues = new JSONObject();
            encodePreferences(telemetryValues, getSharedPreferences(TELEMETRY_PREFS, MODE_PRIVATE), RESTORABLE_TELEMETRY_KEYS);
            root.put("telemetryPreferences", telemetryValues);
            byte[] output = root.toString(2).getBytes(StandardCharsets.UTF_8);
            if (output.length > MAX_BACKUP_BYTES) throw new IllegalArgumentException("Backup too large");
            stream.write(output);
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
            int schema = root.optInt("schema", 0);
            if (!getPackageName().equals(root.optString("package")) || (schema != 1 && schema != 2)) throw new IllegalArgumentException("Invalid backup");
            SharedPreferences.Editor editor = getSharedPreferences(DATA_PREFS, MODE_PRIVATE).edit().clear();
            restorePreferences(root.getJSONObject("preferences"), editor, RESTORABLE_DATA_KEYS);
            SharedPreferences.Editor telemetryEditor = getSharedPreferences(TELEMETRY_PREFS, MODE_PRIVATE).edit().clear();
            if (schema >= 2 && root.has("telemetryPreferences")) {
                restorePreferences(root.getJSONObject("telemetryPreferences"), telemetryEditor, RESTORABLE_TELEMETRY_KEYS);
            } else {
                // Schema 1 stored all values in one object; migrate only the two known telemetry keys.
                restorePreferences(root.getJSONObject("preferences"), telemetryEditor, RESTORABLE_TELEMETRY_KEYS);
            }
            editor.apply();
            telemetryEditor.apply();
            BackupManager.dataChanged(getPackageName());
            dashboard.reloadStoredData();
            Intent battery = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (battery != null) dashboard.readBattery(battery);
            Toast.makeText(this, "Backup wiederhergestellt.", Toast.LENGTH_LONG).show();
        } catch (Exception error) {
            Toast.makeText(this, "Backup ist ungültig oder konnte nicht gelesen werden.", Toast.LENGTH_LONG).show();
        }
    }

    private static void encodePreferences(JSONObject target, SharedPreferences source, Set<String> allowedKeys) throws Exception {
        for (java.util.Map.Entry<String, ?> entry : source.getAll().entrySet()) {
            if (!allowedKeys.contains(entry.getKey())) continue;
            Object value = entry.getValue();
            JSONObject encoded = new JSONObject();
            if (value instanceof Boolean) { encoded.put("type", "boolean"); encoded.put("value", value); }
            else if (value instanceof Integer) { encoded.put("type", "int"); encoded.put("value", value); }
            else if (value instanceof Long) { encoded.put("type", "long"); encoded.put("value", value); }
            else if (value instanceof Float) { encoded.put("type", "float"); encoded.put("value", value); }
            else if (value instanceof String) { encoded.put("type", "string"); encoded.put("value", value); }
            else continue;
            target.put(entry.getKey(), encoded);
        }
    }

    private static void restorePreferences(JSONObject values, SharedPreferences.Editor editor, Set<String> allowedKeys) throws Exception {
        java.util.Iterator<String> keys = values.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            if (!allowedKeys.contains(key)) continue;
            JSONObject encoded = values.getJSONObject(key);
            String type = encoded.optString("type");
            if ("boolean".equals(type)) editor.putBoolean(key, encoded.getBoolean("value"));
            else if ("int".equals(type)) editor.putInt(key, encoded.getInt("value"));
            else if ("long".equals(type)) editor.putLong(key, encoded.getLong("value"));
            else if ("float".equals(type)) editor.putFloat(key, (float) encoded.getDouble("value"));
            else if ("string".equals(type)) editor.putString(key, encoded.getString("value"));
        }
    }

    static void migrateTelemetryPrefs(Context context) {
        SharedPreferences oldPrefs = context.getSharedPreferences(DATA_PREFS, Context.MODE_PRIVATE);
        SharedPreferences newPrefs = context.getSharedPreferences(TELEMETRY_PREFS, Context.MODE_PRIVATE);
        if (newPrefs.contains("telemetrySamples") || !oldPrefs.contains("telemetrySamples")) return;
        SharedPreferences.Editor migration = newPrefs.edit()
                .putString("telemetrySamples", oldPrefs.getString("telemetrySamples", ""));
        if (oldPrefs.contains("telemetryLastSampleAt")) {
            migration.putLong("telemetryLastSampleAt", oldPrefs.getLong("telemetryLastSampleAt", 0L));
        }
        if (migration.commit()) {
            oldPrefs.edit().remove("telemetrySamples").remove("telemetryLastSampleAt").commit();
        }
    }

    void createResearchExport() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("application/json")
                .putExtra(Intent.EXTRA_TITLE, "ampere-research-export.json");
        startActivityForResult(intent, RESEARCH_EXPORT_REQUEST);
    }

    void createCsvExport() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("text/csv")
                .putExtra(Intent.EXTRA_TITLE, "ampere-battery-export.csv");
        startActivityForResult(intent, CSV_EXPORT_REQUEST);
    }

    private void writeCsvExport(Uri uri) {
        try (OutputStream stream = getContentResolver().openOutputStream(uri)) {
            if (stream == null || dashboard == null) throw new IllegalStateException("No output stream");
            byte[] output = dashboard.historyCsv().getBytes(StandardCharsets.UTF_8);
            if (output.length > MAX_BACKUP_BYTES) throw new IllegalArgumentException("CSV export too large");
            stream.write(output);
            Toast.makeText(this, "CSV-Export gespeichert.", Toast.LENGTH_LONG).show();
        } catch (Exception ignored) {
            Toast.makeText(this, "CSV-Export konnte nicht gespeichert werden.", Toast.LENGTH_LONG).show();
        }
    }

    private void writeResearchExport(Uri uri) {
        try (OutputStream stream = getContentResolver().openOutputStream(uri)) {
            if (stream == null) throw new IllegalStateException("No output stream");
            JSONObject root = new JSONObject();
            root.put("schema", 1);
            root.put("app", "Ampere Battery Lab");
            root.put("appVersion", BuildConfig.VERSION_NAME);
            root.put("generatedAt", System.currentTimeMillis());
            root.put("androidApi", Build.VERSION.SDK_INT);
            root.put("deviceManufacturer", Build.MANUFACTURER);
            root.put("deviceModel", Build.MODEL);
            root.put("privacy", "Created after an explicit user export. No account, location, serial number or advertising identifier is included.");

            JSONArray sessionRows = new JSONArray();
            if (dashboard != null) for (String session : dashboard.sessionsForExport()) {
                String[] parts = session.split(",", -1);
                JSONObject row = new JSONObject();
                if (parts.length > 0) row.put("type", parts[0]);
                if (parts.length > 1) row.put("change", parts[1]);
                if (parts.length > 2) row.put("duration", parts[2]);
                if (parts.length > 3) row.put("date", parts[3]);
                if (parts.length > 4) row.put("startLevel", parts[4]);
                if (parts.length > 5) row.put("endLevel", parts[5]);
                if (parts.length > 6) row.put("energyMah", parts[6]);
                if (parts.length > 7) row.put("equivalentFullCycles", parts[7]);
                if (parts.length > 8) row.put("screenOnValue", parts[8]);
                if (parts.length > 9) row.put("screenOffValue", parts[9]);
                if (parts.length > 10) row.put("screenOnDurationMin", parts[10]);
                if (parts.length > 11) row.put("screenOffDurationMin", parts[11]);
                if (parts.length > 12) row.put("deepSleepMin", parts[12]);
                if (parts.length > 13) row.put("chargerSource", parts[13]);
                if (parts.length > 14) row.put("startTimestampMs", parts[14]);
                if (parts.length > 15) row.put("endTimestampMs", parts[15]);
                if (parts.length > 16) row.put("screenWakeups", Integer.parseInt(parts[16]));
                if (parts.length > 8) row.put("screenValueUnit", "Charge".equals(parts[0]) ? "mAh" : "percent");
                sessionRows.put(row);
            }
            root.put("sessions", sessionRows);

            JSONArray telemetryRows = new JSONArray();
            String telemetry = getSharedPreferences(TELEMETRY_PREFS, MODE_PRIVATE).getString("telemetrySamples", "");
            for (String sample : telemetry.split("\\n")) {
                if (sample.trim().isEmpty()) continue;
                String[] parts = sample.split(",", 11);
                if (parts.length < 11) continue;
                JSONObject row = new JSONObject();
                row.put("timestampMs", Long.parseLong(parts[0]));
                row.put("levelPercent", Integer.parseInt(parts[1]));
                row.put("charging", "1".equals(parts[2]));
                row.put("currentMa", Integer.parseInt(parts[3]));
                row.put("temperatureC", Double.parseDouble(parts[4]));
                row.put("voltageV", Double.parseDouble(parts[5]));
                row.put("chargeCounterMah", Integer.parseInt(parts[6]));
                row.put("screenOn", "1".equals(parts[7]));
                row.put("foregroundPackage", parts[8]);
                row.put("systemCycleCount", Integer.parseInt(parts[9]));
                row.put("plugged", Integer.parseInt(parts[10]));
                telemetryRows.put(row);
            }
            root.put("telemetry", telemetryRows);
            byte[] output = root.toString(2).getBytes(StandardCharsets.UTF_8);
            if (output.length > MAX_BACKUP_BYTES) throw new IllegalArgumentException("Research export too large");
            stream.write(output);
            Toast.makeText(this, "Research-Export gespeichert.", Toast.LENGTH_LONG).show();
        } catch (Exception ignored) {
            Toast.makeText(this, "Research-Export konnte nicht gespeichert werden.", Toast.LENGTH_LONG).show();
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
    private int signedCurrentMa = 0;
    private int chargeCounterMah = 0;
    private int plugged = 0;
    private boolean lastCharging = false;
    private long sessionStartedAt = 0L;
    private int sessionStartLevel = 0;
    private int sessionStartChargeCounterMah = 0;
    private final SharedPreferences prefs;
    private final SharedPreferences telemetryPrefs;
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
    private float touchDownY;
    private float lastTouchY;
    private boolean touchDragged;
    private final int lime = Color.rgb(199, 243, 107);
    private final int blue = Color.rgb(118, 184, 255);
    private final int amber = Color.rgb(242, 179, 106);

    BatteryDashboard(Context context) {
        super(context);
        density = getResources().getDisplayMetrics().density;
        p.setTypeface(Typeface.create("sans", Typeface.NORMAL));
        setFocusable(true);
        prefs = context.getSharedPreferences("ampere-data", Context.MODE_PRIVATE);
        telemetryPrefs = context.getSharedPreferences("ampere-telemetry", Context.MODE_PRIVATE);
        loadStoredData();
    }

    void reloadStoredData() {
        history.clear();
        longHistory.clear();
        healthSamples.clear();
        sessions.clear();
        loadStoredData();
        updateLayoutHeight();
        invalidate();
    }

    ArrayList<String> sessionsForExport() {
        return new ArrayList<>(sessions);
    }

    private void updateLayoutHeight() {
        int rowCount = Math.min(150, sessions.size());
        int contentDp = page == 4 ? Math.max(1320, 600 + rowCount * 44) : 1320;
        int contentPx = Math.round(contentDp * density);
        setMinimumHeight(contentPx);
        if (getLayoutParams() != null && getLayoutParams().height != contentPx) {
            getLayoutParams().height = contentPx;
            setLayoutParams(getLayoutParams());
        }
    }

    private float historyExportTop() {
        int rowCount = Math.min(150, sessions.size());
        float listBottom = 182 + 160 + rowCount * 44f;
        float panelBottom = Math.max(182 + 610, listBottom + 250);
        return panelBottom - 48;
    }

    private int sessionCount(String type) {
        int count = 0;
        for (String session : sessions) if (session.startsWith(type + ",")) count++;
        return count;
    }

    private int sessionEnergyTotal(String type) {
        int total = 0;
        for (String session : sessions) {
            String[] parts = session.split(",", -1);
            if (parts.length < 7 || !type.equals(parts[0])) continue;
            try { total += Math.max(0, Integer.parseInt(parts[6])); } catch (NumberFormatException ignored) { }
        }
        return total;
    }

    private String sessionEnergyDisplay(String type, String prefix) {
        int total = sessionEnergyTotal(type);
        return total > 0 ? prefix + total + " mAh" : prefix + "— mAh";
    }

    void readBattery(Intent intent) {
        int rawLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN);
        int pluggedSource = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
        if (rawLevel >= 0 && scale > 0) level = Math.max(0, Math.min(100, Math.round(rawLevel * 100f / scale)));
        boolean newCharging = status == BatteryManager.BATTERY_STATUS_CHARGING
                || (status == BatteryManager.BATTERY_STATUS_FULL && pluggedSource != 0);
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
        plugged = pluggedSource;
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
        signedCurrentMa = currentMa == 0 ? 0 : (newCharging ? currentMa : -currentMa);
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
        reloadLiveCollections();
        invalidate();
    }

    private void reloadLiveCollections() {
        healthSamples.clear();
        String savedHealth = prefs.getString("healthSamples", "");
        if (!savedHealth.isEmpty()) {
            for (String value : savedHealth.split(",")) {
                try { healthSamples.add(Integer.parseInt(value)); } catch (NumberFormatException ignored) { }
            }
        }
        sessions.clear();
        String savedSessions = prefs.getString("sessions", "");
        if (!savedSessions.isEmpty()) {
            for (String value : savedSessions.split("\\|")) if (!value.isEmpty()) sessions.add(value);
        }
        chargeAlarm = prefs.getBoolean("chargeAlarm", chargeAlarm);
        chargeLimit = prefs.getInt("chargeLimit", chargeLimit);
        benchmarkActive = prefs.getBoolean("benchmarkActive", benchmarkActive);
        updateLayoutHeight();
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
        while (longHistory.size() > longHistoryRetentionSamples()) longHistory.remove(0);
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
        if (now - lastSample < samplingIntervalMs() && !history.isEmpty()) return;
        history.add(level);
        while (history.size() > 48) history.remove(0);
        longHistory.add(level);
        while (longHistory.size() > longHistoryRetentionSamples()) longHistory.remove(0);
        StringBuilder values = new StringBuilder();
        for (int i = 0; i < history.size(); i++) { if (i > 0) values.append(','); values.append(history.get(i)); }
        StringBuilder longValues = new StringBuilder();
        for (int i = 0; i < longHistory.size(); i++) { if (i > 0) longValues.append(','); longValues.append(longHistory.get(i)); }
        prefs.edit().putString("history", values.toString()).putString("historyLong", longValues.toString()).putLong("lastSample", now).apply();
    }

    private long samplingIntervalMs() {
        int minutes = prefs.getInt("samplingIntervalMin", 15);
        if (minutes != 5 && minutes != 15 && minutes != 30 && minutes != 60) minutes = 15;
        return minutes * 60L * 1000L;
    }

    private int longHistoryRetentionSamples() {
        return Math.max(1, (int) Math.ceil(30L * 24L * 60L * 60L * 1000L / (double) samplingIntervalMs()));
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

    private String liveCurrentDisplay() {
        if (currentMa <= 0) return "—";
        return (charging ? "+" : "−") + currentMa + " mA";
    }

    private String chargerTypeDisplay() {
        if (!charging) return "Not connected";
        if (plugged == BatteryManager.BATTERY_PLUGGED_AC) return "AC charger";
        if (plugged == BatteryManager.BATTERY_PLUGGED_USB) return "USB";
        if (plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS) return "Wireless";
        return "External power";
    }

    private int designCapacityMah() { return BatteryCapacity.designCapacityMah(getContext()); }

    private String designCapacitySource() {
        if (BatteryCapacity.hasManualOverride(getContext())) return "Manual override";
        return BatteryCapacity.hasAutomaticValue()
                ? "Automatically detected when Android exposes it"
                : "Fallback 4,500 mAh · set manually for accuracy";
    }

    private int estimatedCapacityMah() { return Math.round(designCapacityMah() * healthPercent() / 100f); }

    /**
     * Capacity used for time/rate calculations. A measured health estimate is
     * preferred, while the detected factory capacity keeps live projections
     * useful before the first health sample exists.
     */
    private int calculationCapacityMah() {
        int measured = estimatedCapacityMah();
        return measured > 0 ? measured : Math.max(0, designCapacityMah());
    }

    private void editDesignCapacity() {
        EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(designCapacityMah()));
        input.setSelectAllOnFocus(true);
        new AlertDialog.Builder(getContext())
                .setTitle("Design capacity")
                .setMessage("Enter the factory capacity in mAh. Enter 0 to use the device value automatically when Android exposes it.")
                .setView(input)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (dialog, which) -> {
                    try {
                        int capacity = Integer.parseInt(input.getText().toString().trim());
                        if (capacity == 0) {
                            prefs.edit().remove("designCapacityMah").apply();
                            invalidate();
                        } else if (capacity >= 500 && capacity <= 30000) {
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
        if (!charging) return "—";
        if (level >= 99) return "Full";
        long systemMinutes = systemChargeTimeRemainingMinutes();
        if (systemMinutes > 0L) return formatDuration(systemMinutes);
        int missingMah = Math.round(calculationCapacityMah() * (100 - level) / 100f);
        float historicalRate = averageChargeRateMahPerHour();
        if (historicalRate > 0f) return formatDuration(Math.max(1, Math.round(missingMah * 60f / historicalRate)));
        if (currentMa < 50) return "—";
        return formatDuration(Math.max(1, Math.round(missingMah * 60f / currentMa)));
    }

    /**
     * Android's vendor-backed charge-time estimate is often more accurate
     * than a single instantaneous current reading. It is optional and may be
     * unavailable on devices that do not expose a charging estimate.
     */
    private long systemChargeTimeRemainingMinutes() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P || !charging || level >= 99) return 0L;
        BatteryManager manager = (BatteryManager) getContext().getSystemService(Context.BATTERY_SERVICE);
        if (manager == null) return 0L;
        long remainingMs = manager.computeChargeTimeRemaining();
        if (remainingMs <= 0L || remainingMs > 7L * 24L * 60L * 60L * 1000L) return 0L;
        return Math.max(1L, Math.round(remainingMs / 60000f));
    }

    private String chargeTimeEstimateLabel() {
        if (systemChargeTimeRemainingMinutes() > 0L) return "Android system estimate";
        return averageChargeRateMahPerHour() > 0f ? "local 7-day estimate" : "current estimate";
    }

    private String timeToLimit() {
        if (!charging) return "—";
        if (level >= chargeLimit) return "Reached";
        if (calculationCapacityMah() <= 0) return "—";
        int missingMah = Math.round(calculationCapacityMah() * (chargeLimit - level) / 100f);
        float historicalRate = averageChargeRateMahPerHour();
        if (historicalRate > 0f) return formatDuration(Math.max(1, Math.round(missingMah * 60f / historicalRate)));
        if (currentMa < 50) return "—";
        return formatDuration(Math.max(1, Math.round(missingMah * 60f / currentMa)));
    }

    /** Calculates a weighted local seven-day charge rate from telemetry. */
    private float averageChargeRateMahPerHour() {
        String saved = telemetryPrefs.getString("telemetrySamples", "");
        if (saved.isEmpty()) return 0f;
        long windowStart = System.currentTimeMillis() - 7L * 24L * 60L * 60L * 1000L;
        long previousAt = -1L;
        int previousCounter = 0;
        boolean previousCharging = false;
        float weightedRate = 0f;
        long weightedMs = 0L;
        for (String row : saved.split("\\n")) {
            String[] parts = row.split(",", 11);
            if (parts.length < 7) continue;
            try {
                long timestamp = Long.parseLong(parts[0]);
                if (timestamp < windowStart) continue;
                boolean sampleCharging = "1".equals(parts[2]);
                int counter = Integer.parseInt(parts[6]);
                int current = Math.abs(Integer.parseInt(parts[3]));
                long gap = previousAt > 0L ? timestamp - previousAt : 0L;
                if (sampleCharging && previousCharging && gap > 0L && gap <= 2L * 60L * 60L * 1000L) {
                    float rate = 0f;
                    if (counter > 0 && previousCounter > 0 && counter > previousCounter) {
                        rate = (counter - previousCounter) * 3600000f / gap;
                    } else if (current >= 50) {
                        rate = current;
                    }
                    if (rate >= 50f && rate <= 20000f) {
                        weightedRate += rate * gap;
                        weightedMs += gap;
                    }
                }
                previousAt = timestamp;
                previousCounter = counter;
                previousCharging = sampleCharging;
            } catch (NumberFormatException ignored) { }
        }
        return weightedMs >= 5L * 60L * 1000L ? weightedRate / weightedMs : 0f;
    }

    /**
     * Gives the selected charge target a transparent, relative stress score.
     * It is deliberately not presented as a measured percentage of battery
     * health: the actual cell chemistry and charge curve are device-specific.
     * The score reflects the extra high-state-of-charge stress described in
     * the app's charging guidance and is useful for comparing targets.
     */
    private String wearImpactToTarget() {
        if (chargeLimit <= level) return "Reached";
        float score = 0f;
        for (int percent = Math.max(0, level); percent < chargeLimit; percent++) {
            float stress = percent < 70 ? .70f
                    : percent < 85 ? 1.0f + (percent - 70) * .04f
                    : 1.60f + (percent - 85) * .10f;
            score += stress;
        }
        float average = score / Math.max(1, chargeLimit - Math.max(0, level));
        String label = average < .95f ? "Low" : average < 1.35f ? "Moderate" : "High";
        return label + " · " + String.format(Locale.US, "%.1f×", average);
    }

    /** Calculates a local 7-day discharge rate from consecutive telemetry points. */
    private float averageDischargeRate(boolean screenOn) {
        String saved = telemetryPrefs.getString("telemetrySamples", "");
        if (!saved.isEmpty()) {
            long windowStart = System.currentTimeMillis() - 7L * 24L * 60L * 60L * 1000L;
            long previousAt = -1L;
            int previousLevel = -1;
            boolean previousCharging = true;
            boolean previousScreenOn = false;
            float consumed = 0f;
            long elapsedMs = 0L;
            for (String row : saved.split("\\n")) {
                String[] parts = row.split(",", 11);
                if (parts.length < 8) continue;
                try {
                    long timestamp = Long.parseLong(parts[0]);
                    if (timestamp < windowStart) continue;
                    int sampleLevel = Integer.parseInt(parts[1]);
                    boolean sampleCharging = "1".equals(parts[2]);
                    boolean sampleScreenOn = "1".equals(parts[7]);
                    if (previousAt > 0L && !sampleCharging && !previousCharging
                            && sampleScreenOn == screenOn && previousScreenOn == screenOn
                            && timestamp > previousAt && timestamp - previousAt <= 2L * 60L * 60L * 1000L) {
                        int drop = previousLevel - sampleLevel;
                        if (drop > 0 && drop <= 20) {
                            consumed += drop;
                            elapsedMs += timestamp - previousAt;
                        }
                    }
                    previousAt = timestamp;
                    previousLevel = sampleLevel;
                    previousCharging = sampleCharging;
                    previousScreenOn = sampleScreenOn;
                } catch (NumberFormatException ignored) { }
            }
            if (consumed > 0f && elapsedMs >= 5L * 60L * 1000L) return consumed * 3600000f / elapsedMs;
        }
        String percentKey = screenOn ? "dischargeScreenOnPercent" : "dischargeScreenOffPercent";
        String durationKey = screenOn ? "dischargeScreenOnMs" : "dischargeScreenOffMs";
        if (charging) {
            percentKey = "last" + Character.toUpperCase(percentKey.charAt(0)) + percentKey.substring(1);
            durationKey = "last" + Character.toUpperCase(durationKey.charAt(0)) + durationKey.substring(1);
        }
        float consumed = prefs.getFloat(percentKey, 0f);
        long duration = prefs.getLong(durationKey, 0L);
        return consumed > 0f && duration >= 5L * 60L * 1000L ? consumed * 3600000f / duration : 0f;
    }

    private float mixedDischargeRate() {
        float screenOnRate = averageDischargeRate(true);
        float screenOffRate = averageDischargeRate(false);
        if (screenOnRate <= 0f && screenOffRate <= 0f) return 0f;
        if (screenOnRate <= 0f) return screenOffRate;
        if (screenOffRate <= 0f) return screenOnRate;
        long onMs = prefs.getLong(charging ? "lastDischargeScreenOnMs" : "dischargeScreenOnMs", 0L);
        long offMs = prefs.getLong(charging ? "lastDischargeScreenOffMs" : "dischargeScreenOffMs", 0L);
        float onRatio = onMs + offMs > 0L ? Math.max(.1f, Math.min(.9f, onMs / (float) (onMs + offMs))) : .5f;
        return screenOnRate * onRatio + screenOffRate * (1f - onRatio);
    }

    private String averageDischargeRateDisplay() {
        float rate = mixedDischargeRate();
        return rate > 0f ? String.format(Locale.US, "%.1f%%/h", rate) : "—";
    }

    private String runtimeEstimate() {
        if (charging) {
            float used = prefs.getFloat("lastDischargeScreenOnPercent", 0f) + prefs.getFloat("lastDischargeScreenOffPercent", 0f);
            long minutes = (prefs.getLong("lastDischargeScreenOnMs", 0L) + prefs.getLong("lastDischargeScreenOffMs", 0L)) / 60000L;
            int historicalLevel = prefs.getInt("lastDischargeEndLevel", level);
            float rate = mixedDischargeRate();
            return rate > 0f ? formatDuration(Math.max(1, Math.round(historicalLevel * 60f / rate)))
                    : (used > 0f && minutes >= 5 ? formatDuration(Math.max(1, Math.round(historicalLevel * minutes / used))) : "—");
        }
        if (calculationCapacityMah() <= 0) return "—";
        float rate = mixedDischargeRate();
        if (rate > 0f) return formatDuration(Math.max(1, Math.round(level * 60f / rate)));
        if (currentMa < 50) return "—";
        int availableMah = Math.round(calculationCapacityMah() * level / 100f);
        return formatDuration(Math.max(1, Math.round(availableMah * 60f / currentMa)));
    }

    private String drainRate() {
        int capacity = calculationCapacityMah();
        if (charging || currentMa < 50 || capacity <= 0) return "—";
        return String.format(Locale.US, "%.1f%% / hour", currentMa * 100f / capacity);
    }

    private String dischargeSpeed(boolean screenOn) {
        String percentKey = screenOn ? "dischargeScreenOnPercent" : "dischargeScreenOffPercent";
        String durationKey = screenOn ? "dischargeScreenOnMs" : "dischargeScreenOffMs";
        if (charging) {
            percentKey = "last" + Character.toUpperCase(percentKey.charAt(0)) + percentKey.substring(1);
            durationKey = "last" + Character.toUpperCase(durationKey.charAt(0)) + durationKey.substring(1);
        }
        float percent = prefs.getFloat(percentKey, 0f);
        long minutes = prefs.getLong(durationKey, 0L) / 60000L;
        if (percent > 0f && minutes >= 5) return String.format(Locale.US, "%.1f%%/h", percent * 60f / minutes);
        int capacity = calculationCapacityMah();
        if (charging || currentMa < 50 || capacity <= 0) return "—";
        return String.format(Locale.US, "%.1f%%/h", currentMa * 100f / capacity);
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

    private String deepSleepPercent() {
        String deepKey = charging ? "lastDischargeDeepSleepMs" : "dischargeDeepSleepMs";
        String offKey = charging ? "lastDischargeScreenOffMs" : "dischargeScreenOffMs";
        long deepMs = prefs.getLong(deepKey, 0L);
        long offMs = prefs.getLong(offKey, 0L);
        if (deepMs <= 0L || offMs <= 0L) return "—";
        return String.format(Locale.US, "%.0f%%", Math.min(100f, deepMs * 100f / offMs));
    }

    private int wakeupCount() {
        return prefs.getInt(charging ? "lastDischargeWakeups" : "dischargeWakeups", 0);
    }

    private String sinceFullRange() {
        if (!prefs.getBoolean("sinceFullActive", false)) return "No full-charge baseline";
        return prefs.getInt("sinceFullStartLevel", 100) + "% → " + prefs.getInt("sinceFullLastLevel", level) + "%";
    }

    private String sinceFullDuration() {
        if (!prefs.getBoolean("sinceFullActive", false)) return "—";
        long start = prefs.getLong("sinceFullStartAt", 0L);
        if (start <= 0L) return "—";
        return formatDuration(Math.max(1L, (System.currentTimeMillis() - start) / 60000L));
    }

    private String sinceFullUsageSummary() {
        if (!prefs.getBoolean("sinceFullActive", false)) return "No full baseline yet";
        String range = prefs.getFloat("sinceFullPercent", 0f) > 0f
                ? String.format(Locale.US, "%.0f%% used", prefs.getFloat("sinceFullPercent", 0f)) : "0% used";
        int mah = prefs.getInt("sinceFullMah", 0);
        return range + " · " + sinceFullDuration() + " · " + (mah > 0 ? mah + " mAh" : "—");
    }

    private int dischargeMah() { return prefs.getInt(charging ? "lastDischargeMah" : "dischargeMah", 0); }

    private int lastChargeEnergyMah() { return prefs.getInt("lastChargeEnergyMah", 0); }

    private int totalChargedMah() { return Math.max(0, prefs.getInt("totalChargedMah", 0)); }

    private String healthEstimateStatus() {
        if (charging) {
            int change = chargeEndLevelForDisplay() - chargeStartLevelForDisplay();
            int energy = chargeEnergyForDisplay();
            if (change < 5) return "Health sample needs at least 5% level change";
            if (energy <= 0) return "Waiting for readable energy data";
            return "Included when this charge session ends";
        }
        String reason = prefs.getString("lastChargeHealthReason", "");
        return reason.isEmpty() ? "Longer charges with readable energy improve accuracy" : reason;
    }

    private int chargeStartLevelForDisplay() {
        return charging ? prefs.getInt("monitorSessionStartLevel", sessionStartLevel)
                : prefs.getInt("lastChargeStartLevel", 0);
    }

    private int chargeEndLevelForDisplay() {
        return charging ? level : prefs.getInt("lastChargeEndLevel", 0);
    }

    private String chargeChangeForDisplay() {
        int change = chargeEndLevelForDisplay() - chargeStartLevelForDisplay();
        return change > 0 ? "+" + change + "%" : "—";
    }

    private String chargeDurationForDisplay() {
        if (!charging) return lastChargeDuration();
        long start = prefs.getLong("monitorSessionStartedAt", sessionStartedAt);
        if (start <= 0L) return "—";
        return formatDuration(Math.max(1L, (System.currentTimeMillis() - start) / 60000L));
    }

    private String chargeStartForDisplay() {
        long start = charging ? prefs.getLong("monitorSessionStartedAt", sessionStartedAt)
                : prefs.getLong("lastChargeStartAt", 0L);
        return start > 0L ? new SimpleDateFormat("MMM d HH:mm", Locale.US).format(new Date(start)) : "—";
    }

    private int chargeEnergyForDisplay() {
        if (charging) return prefs.getInt("chargeScreenOnMah", 0) + prefs.getInt("chargeScreenOffMah", 0);
        return lastChargeEnergyMah();
    }

    private String chargeModeDetails(boolean screenOn) {
        String mahKey = screenOn ? "chargeScreenOnMah" : "chargeScreenOffMah";
        String msKey = screenOn ? "chargeScreenOnMs" : "chargeScreenOffMs";
        String lastMahKey = "last" + Character.toUpperCase(mahKey.charAt(0)) + mahKey.substring(1);
        String lastMsKey = "last" + Character.toUpperCase(msKey.charAt(0)) + msKey.substring(1);
        int mah = charging ? prefs.getInt(mahKey, 0) : prefs.getInt(lastMahKey, prefs.getInt(mahKey, 0));
        long ms = charging ? prefs.getLong(msKey, 0L) : prefs.getLong(lastMsKey, prefs.getLong(msKey, 0L));
        if (mah <= 0 && ms <= 0L) return "—";
        String duration = ms > 0L ? formatDuration(Math.max(1L, ms / 60000L)) : "—";
        return (mah > 0 ? mah + " mAh" : "— mAh") + " · " + duration;
    }

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
        String percentKey = screenOn ? "dischargeScreenOnPercent" : "dischargeScreenOffPercent";
        String durationKey = screenOn ? "dischargeScreenOnMs" : "dischargeScreenOffMs";
        if (charging) {
            percentKey = "last" + Character.toUpperCase(percentKey.charAt(0)) + percentKey.substring(1);
            durationKey = "last" + Character.toUpperCase(durationKey.charAt(0)) + durationKey.substring(1);
        }
        float percent = prefs.getFloat(percentKey, 0f);
        long minutes = prefs.getLong(durationKey, 0L) / 60000L;
        int referenceLevel = charging ? prefs.getInt("lastDischargeEndLevel", level) : level;
        if (percent > 0f && minutes >= 5) return formatDuration(Math.max(1, Math.round(referenceLevel * minutes / percent)));
        if (charging) return "—";
        float rate = averageDischargeRate(screenOn);
        if (rate > 0f) return formatDuration(Math.max(1, Math.round(referenceLevel * 60f / rate)));
        if (currentMa < 50) return "—";
        int modeCurrent = screenOn ? currentMa : Math.max(50, Math.round(currentMa * .35f));
        return formatDuration(Math.max(1, Math.round(calculationCapacityMah() * referenceLevel / 100f * 60f / modeCurrent)));
    }

    private String chargeSpeed(boolean screenOn) {
        String mahKey = screenOn ? "chargeScreenOnMah" : "chargeScreenOffMah";
        String durationKey = screenOn ? "chargeScreenOnMs" : "chargeScreenOffMs";
        if (!charging) {
            mahKey = "last" + Character.toUpperCase(mahKey.charAt(0)) + mahKey.substring(1);
            durationKey = "last" + Character.toUpperCase(durationKey.charAt(0)) + durationKey.substring(1);
        }
        int mah = prefs.getInt(mahKey, 0);
        long minutes = prefs.getLong(durationKey, 0L) / 60000L;
        if (mah <= 0 || minutes < 5) return "—";
        float percent = screenOn ? prefs.getFloat("chargeScreenOnPercent", 0f) : prefs.getFloat("chargeScreenOffPercent", 0f);
        if (!charging) percent = screenOn ? prefs.getFloat("lastChargeScreenOnPercent", 0f) : prefs.getFloat("lastChargeScreenOffPercent", 0f);
        float percentPerHour = percent > 0f ? percent * 60f / minutes : 0f;
        return percentPerHour > 0f
                ? String.format(Locale.US, "%.0f mA · %.1f%%/h", mah * 60f / minutes, percentPerHour)
                : String.format(Locale.US, "%.0f mA", mah * 60f / minutes);
    }

    private int chargeCycles() {
        int reported = prefs.getInt("systemCycleCount", -1);
        return reported >= 0 ? reported : prefs.getInt("chargeCycles", 0);
    }

    private String chargingEfficiency() {
        int charged = prefs.getInt("totalChargedMah", 0);
        int cycles = chargeCycles();
        if (charged <= 0 || cycles <= 0) return "—";
        return String.format(Locale.US, "%.0f%%", charged * 100f / (designCapacityMah() * cycles));
    }

    private String lastChargeEquivalentCycles() {
        int energy = lastChargeEnergyMah();
        int design = designCapacityMah();
        return energy > 0 && design > 0 ? String.format(Locale.US, "%.2f EFC", energy / (float) design) : "—";
    }

    private String totalEquivalentCycles() {
        int charged = prefs.getInt("totalChargedMah", 0);
        int design = designCapacityMah();
        return charged > 0 && design > 0 ? String.format(Locale.US, "%.2f EFC", charged / (float) design) : "—";
    }

    private ArrayList<String[]> chargeWearRows() {
        ArrayList<String[]> rows = new ArrayList<>();
        for (int i = sessions.size() - 1; i >= 0; i--) {
            String[] parts = sessions.get(i).split(",", -1);
            if (parts.length >= 8 && "Charge".equals(parts[0])) rows.add(parts);
        }
        int first = Math.max(0, rows.size() - 12);
        return new ArrayList<>(rows.subList(first, rows.size()));
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
        String[] options = {"Dark theme", "AMOLED black", "Light theme", "Notification settings", "Overlay permission", "Data & privacy", "Backup & restore", "Background monitoring", "Data collection", "Check for updates", "Quick tutorial", "Reset health baseline", "Delete local data"};
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
            } else if (which == 7) {
                requestBackgroundMonitoring();
            } else if (which == 8) {
                showDataCollection();
            } else if (which == 9) {
                UpdateChecker.checkNow((Activity) getContext());
            } else if (which == 10) {
                showTutorial(true);
            } else if (which == 11) {
                confirmResetHealthBaseline();
            } else {
                confirmDeleteData();
            }
            prefs.edit().putBoolean("lightTheme", light).putBoolean("amoledTheme", amoled).apply();
            invalidate();
        }).show();
    }

    private void showDataCollection() {
        final int[] intervals = {5, 15, 30, 60};
        final String[] labels = {"Every 5 minutes", "Every 15 minutes (recommended)", "Every 30 minutes", "Every 60 minutes"};
        int current = prefs.getInt("samplingIntervalMin", 15);
        int selected = 1;
        for (int i = 0; i < intervals.length; i++) if (intervals[i] == current) selected = i;
        final int[] choice = {selected};
        new AlertDialog.Builder(getContext())
                .setTitle("Data collection · local only")
                .setSingleChoiceItems(labels, selected, (dialog, which) -> choice[0] = which)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (dialog, which) -> {
                    prefs.edit().putInt("samplingIntervalMin", intervals[choice[0]]).apply();
                    ((MainActivity) getContext()).restartMonitorService();
                    invalidate();
                }).show();
    }

    private void requestBackgroundMonitoring() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Toast.makeText(getContext(), "Background monitoring is available on Android 6+.", Toast.LENGTH_LONG).show();
            return;
        }
        PowerManager power = (PowerManager) getContext().getSystemService(Context.POWER_SERVICE);
        if (power != null && power.isIgnoringBatteryOptimizations(getContext().getPackageName())) {
            Toast.makeText(getContext(), "Background monitoring is already allowed.", Toast.LENGTH_LONG).show();
            return;
        }
        new AlertDialog.Builder(getContext())
                .setTitle("Background monitoring")
                .setMessage("Android can pause background apps to save power. Allow Ampere to keep recording battery history and charge alarms while the app is closed?")
                .setNegativeButton("Later", null)
                .setPositiveButton("Open system setting", (dialog, which) -> {
                    try {
                        Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                Uri.parse("package:" + getContext().getPackageName()));
                        getContext().startActivity(intent);
                    } catch (Exception ignored) {
                        getContext().startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
                    }
                }).show();
    }

    private void showDataPrivacy() {
        new AlertDialog.Builder(getContext())
                .setTitle("Data & privacy")
                .setMessage("Ampere collects battery readings locally for your history and analysis: time, battery level, charging state, current, temperature, voltage and screen state. If you grant Usage access, the active foreground package is also stored locally to estimate app-related drain.\n\nNo battery readings, account identifiers, location or installed-app lists are sent to an Ampere server. Android automatic backup can include history, settings and local telemetry when your device has an eligible encrypted backup transport; the device controls whether and when that backup runs. The update checker only requests its configured version file.\n\nCSV is a flat table. Research export is structured JSON and includes device model and Android version, but no serial number or advertising identifier. Both exports start only after you choose them.")
                .setPositiveButton("Export CSV", (dialog, which) -> exportHistory())
                .setNeutralButton("Research JSON", (dialog, which) -> ((MainActivity) getContext()).createResearchExport())
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
                    context.getSharedPreferences("ampere-telemetry", Context.MODE_PRIVATE).edit().clear().apply();
                    BackupManager.dataChanged(context.getPackageName());
                    reloadStoredData();
                    Intent battery = ((Activity) context).registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                    if (battery != null) readBattery(battery);
                    Toast.makeText(context, "Lokale Daten gelöscht.", Toast.LENGTH_LONG).show();
                })
                .show();
    }

    private void confirmResetHealthBaseline() {
        new AlertDialog.Builder(getContext())
                .setTitle("Reset health baseline?")
                .setMessage("This starts the battery-health and benchmark calculation over, for example after replacing the battery. Existing sessions, telemetry, settings and exports stay available.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Reset baseline", (dialog, which) -> {
                    Context context = getContext();
                    SharedPreferences data = context.getSharedPreferences("ampere-data", Context.MODE_PRIVATE);
                    data.edit()
                            .remove("healthSamples").remove("benchmarkCapacityMah")
                            .remove("benchmarkActive").remove("benchmarkStartLevel")
                            .remove("benchmarkStartCounterMah").remove("benchmarkChargeLastCounterMah")
                            .remove("benchmarkChargeAddedMah").remove("benchmarkChargeStatsBaselineMah")
                            .remove("healthSampleSessionAt").remove("lastChargeHealthReason")
                            .remove("totalChargedMah").remove("chargeCycles")
                            .remove("cycleLastLevel").remove("dischargePercent")
                            .apply();
                    BackupManager.dataChanged(context.getPackageName());
                    reloadStoredData();
                    Intent battery = ((Activity) context).registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                    if (battery != null) readBattery(battery);
                    Toast.makeText(context, "Health-Baseline zurückgesetzt; Verlauf bleibt erhalten.", Toast.LENGTH_LONG).show();
                })
                .show();
    }

    private void showBackupRestore() {
        new AlertDialog.Builder(getContext())
                .setTitle("Backup & restore")
                .setMessage("Updates keep your data automatically. The monitor requests Android's backup provider in the background. With an eligible encrypted backup transport, automatic Android backup includes history, settings and local telemetry. " + backupStatus() + "\n\nBefore uninstalling, create a backup and restore it after reinstalling. Android cloud/device backup may restore the included data when enabled on your device; the visible backup is the reliable fallback.")
                .setPositiveButton("Create backup", (dialog, which) -> ((MainActivity) getContext()).createBackup())
                .setNeutralButton("Restore backup", (dialog, which) -> ((MainActivity) getContext()).restoreBackup())
                .setNegativeButton("Close", null)
                .show();
    }

    private String backupStatus() {
        long lastRequest = prefs.getLong("lastBackupRequestAt", 0L);
        if (lastRequest <= 0L) return "No automatic backup request has been recorded yet.";
        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date(lastRequest));
        return "Last automatic backup request: " + date + ". Android controls the actual backup transport and timing.";
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
        text(c, health == 0 ? "Run benchmark" : (health > 80 ? "Healthy range" : "Below expected"), 255, top + 141, 10, muted, false);
        text(c, "Estimated full capacity", 255, top + 181, 10, muted, false);
        text(c, health > 0 ? String.format(Locale.US, "%,d mAh", estimatedCapacityMah()) : "—", 255, top + 201, 12, primary, true);
        rounded(c, 255, top + 215, 18 + heroW - 28, top + 219, 3, border);
        if (health > 0) rounded(c, 255, top + 215, 255 + (heroW - 46) * Math.min(1f, health / 100f), top + 219, 3, lime);
        text(c, "Design capacity " + String.format(Locale.US, "%,d mAh", designCapacityMah()), 255, top + 236, 9, faint, false);
        rounded(c, 36, top + 263, 18 + heroW - 36, top + 301, 8, raised);
        drawBolt(c, 52, top + 282, lime, .8f);
        text(c, charging ? "Charging detected" : "On battery", 68, top + 278, 10, primary, true);
        String powerText = currentMa > 0 ? String.format(Locale.US, "Approx. %.1f W live draw", currentMa * voltage / 1000f) : "Waiting for current reading";
        String detectionText = charging ? chargerTypeDisplay() + " · automatic Android detection" : powerText + " · automatic Android detection";
        text(c, detectionText, 68, top + 293, 9, muted, false);
        text(c, liveCurrentDisplay(), 285, top + 285, 9, charging ? lime : blue, false);

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
        rounded(c, 18, y, w - 18, y + 366, 12, panel); stroke(c, border, 1); rect.set(u(18), u(y), u(w - 18), u(y + 366)); c.drawRoundRect(rect, u(12), u(12), p);
        text(c, "CHARGING SESSION", 36, y + 31, 10, muted, true);
        text(c, charging ? "Power connected" : "Most recent charge", 36, y + 58, 18, primary, true);
        drawBolt(c, 53, y + 105, lime, 1);
        text(c, charging && currentMa > 0 ? currentMa + " mA" : "—", 77, y + 112, 31, primary, true);
        text(c, charging ? "live charge current" : "unplugged · historical data", 78, y + 132, 9, muted, false);
        line(c, w * .54f, y + 86, w * .54f, y + 156, border, 1);
        text(c, charging ? (chargeLimit >= 100 ? "Time to full" : "Time to limit") : "Last charge", w * .6f, y + 96, 10, muted, false);
        text(c, charging ? (chargeLimit >= 100 ? timeToFull() : timeToLimit()) : lastChargeRange(), w * .6f, y + 126, 20, primary, true);
        text(c, charging ? (chargeLimit >= 100 ? chargeTimeEstimateLabel() : "local 7-day estimate") : lastChargeDuration(), w * .6f, y + 145, 9, faint, false);
        text(c, "Temp " + temperatureDisplay() + " °C · Voltage " + voltageDisplay() + " V", 78, y + 151, 8, faint, false);
        text(c, "Charge limit", 36, y + 190, 10, muted, false);
        text(c, chargeLimit + "%", w - 67, y + 190, 10, lime, true);
        text(c, "Source: " + chargerTypeDisplay(), 36, y + 169, 9, faint, false);
        rounded(c, 36, y + 205, w - 36, y + 209, 3, border);
        rounded(c, 36, y + 205, 36 + (w - 72) * chargeLimit / 100f, y + 209, 3, lime);
        text(c, "Charge speed on / off: " + chargeSpeed(true) + " / " + chargeSpeed(false), 36, y + 238, 9, faint, false);
        text(c, "Wear impact to target", 36, y + 258, 9, muted, false);
        text(c, wearImpactToTarget(), w - 126, y + 258, 9, amber, true);
        rounded(c, 36, y + 287, w - 36, y + 317, 7, raised);
        text(c, "Charge alarm", 50, y + 306, 10, primary, true);
        text(c, chargeAlarm ? "Enabled" : "Disabled", w - 106, y + 306, 9, chargeAlarm ? lime : muted, false);
        rounded(c, w - 70, y + 295, w - 40, y + 311, 9, chargeAlarm ? Color.rgb(87, 108, 48) : border);
        rounded(c, chargeAlarm ? w - 55 : w - 68, y + 297, chargeAlarm ? w - 42 : w - 55, y + 309, 6, chargeAlarm ? lime : muted);
        rounded(c, 36, y + 325, w - 36, y + 355, 7, raised);
        text(c, "Live stats overlay", 50, y + 344, 10, primary, true);
        text(c, overlayEnabled ? "Enabled" : "Disabled", w - 106, y + 344, 9, overlayEnabled ? lime : muted, false);
        rounded(c, w - 70, y + 333, w - 40, y + 349, 9, overlayEnabled ? Color.rgb(87, 108, 48) : border);
        rounded(c, overlayEnabled ? w - 55 : w - 68, y + 335, overlayEnabled ? w - 42 : w - 55, y + 347, 6, overlayEnabled ? lime : muted);
        int energyAdded = chargeEnergyForDisplay();
        drawStat(c, 18, y + 380, (w - 48) / 2f, 105, "Energy added", energyAdded > 0 ? "+" + energyAdded : "—", "mAh", lime, primary, muted, border, panel, "bolt");
        drawStat(c, 30 + (w - 48) / 2f, y + 380, (w - 48) / 2f, 105, "Battery health", healthDisplay(), healthPercent() > 0 ? "%" : "", lime, primary, muted, border, panel, "heart");
        rounded(c, 18, y + 500, w - 18, y + 645, 12, panel); stroke(c, border, 1); rect.set(u(18), u(y + 500), u(w - 18), u(y + 645)); c.drawRoundRect(rect, u(12), u(12), p);
        text(c, "CHARGE AMOUNT", 36, y + 530, 10, muted, true);
        text(c, "Change", 36, y + 558, 8, faint, false);
        text(c, chargeChangeForDisplay(), 36, y + 580, 13, primary, true);
        text(c, "Duration", 150, y + 558, 8, faint, false);
        text(c, chargeDurationForDisplay(), 150, y + 580, 13, primary, true);
        text(c, "Started", 285, y + 558, 8, faint, false);
        text(c, chargeStartForDisplay(), 285, y + 580, 13, primary, true);
        text(c, "Screen on", 36, y + 612, 8, faint, false);
        text(c, chargeModeDetails(true), 36, y + 630, 10, blue, true);
        text(c, "Screen off", 285, y + 612, 8, faint, false);
        text(c, chargeModeDetails(false), 285, y + 630, 10, blue, true);
        rounded(c, 18, y + 665, w - 18, y + 750, 12, panel); stroke(c, border, 1); rect.set(u(18), u(y + 665), u(w - 18), u(y + 750)); c.drawRoundRect(rect, u(12), u(12), p);
        text(c, "BATTERY CAPACITY ESTIMATE", 36, y + 695, 10, muted, true);
        text(c, healthPercent() > 0 ? String.format(Locale.US, "%,d mAh", estimatedCapacityMah()) : "—", 36, y + 726, 24, lime, true);
        text(c, healthPercent() > 0 ? "based on local charge samples" : "Complete longer charges to estimate capacity", w - 224, y + 724, 8, faint, false);
        text(c, healthEstimateStatus(), 36, y + 744, 8, faint, false);
        drawTelemetryChart(c, 18, y + 775, w - 36, 220, panel, border, primary, muted, faint, true);
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
        text(c, "7-day average", 36, y + 225, 10, muted, false);
        text(c, averageDischargeRateDisplay(), w - 92, y + 225, 10, blue, true);
        text(c, "Discharging speed", 36, y + 245, 10, muted, false);
        text(c, dischargeSpeed(true) + " · " + dischargeSpeed(false), w - 145, y + 245, 10, blue, true);
        text(c, "screen on / off", w - 112, y + 262, 8, faint, false);
        text(c, "Live current", 36, y + 280, 9, muted, false);
        text(c, !charging && currentMa > 0 ? "−" + currentMa + " mA" : "—", w - 95, y + 280, 10, blue, true);
        drawStat(c, 18, y + 316, (w - 48) / 2f, 105, "Screen-on time", dischargeDuration(true), "", Color.rgb(180, 154, 255), primary, muted, border, panel, "clock");
        drawStat(c, 30 + (w - 48) / 2f, y + 316, (w - 48) / 2f, 105, "Energy used", dischargeMah() > 0 ? String.valueOf(dischargeMah()) : "—", "mAh", blue, primary, muted, border, panel, "arrow");
        rounded(c, 18, y + 438, w - 18, y + 536, 12, panel); stroke(c, border, 1); rect.set(u(18), u(y + 438), u(w - 18), u(y + 536)); c.drawRoundRect(rect, u(12), u(12), p);
        text(c, "Usage note", 36, y + 468, 10, muted, true);
        text(c, "Current on " + dischargePercent(true) + " · off " + dischargePercent(false) + " · " + dischargeMah() + " mAh", 36, y + 486, 8, primary, false);
        text(c, "Deep sleep: " + deepSleepPercent() + " · " + deepSleepTime() + " · screen wakeups " + wakeupCount(), 36, y + 502, 8, primary, false);
        text(c, "Since full: " + sinceFullUsageSummary(), 36, y + 518, 8, primary, false);
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
        drawTelemetryChart(c, 18, y + 735, w - 36, 220, panel, border, primary, muted, faint, false);
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
        long start = prefs.getBoolean("sinceFullActive", false)
                ? prefs.getLong("sinceFullStartAt", end - 24 * 60 * 60 * 1000L)
                : prefs.getLong(charging ? "lastDischargeStartAt" : "dischargeStartAt", end - 24 * 60 * 60 * 1000L);
        if (start >= end) start = end - 60 * 60 * 1000L;
        List<UsageStats> stats = manager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end);
        if (stats == null) return;
        Collections.sort(stats, new Comparator<UsageStats>() {
            @Override public int compare(UsageStats left, UsageStats right) { return Long.compare(right.getTotalTimeInForeground(), left.getTotalTimeInForeground()); }
        });
        long totalForegroundMs = 0L;
        for (UsageStats stat : stats) if (!stat.getPackageName().equals(getContext().getPackageName())) totalForegroundMs += stat.getTotalTimeInForeground();
        int totalEnergy = dischargeMah();
        int row = 0;
        for (UsageStats stat : stats) {
            if (stat.getTotalTimeInForeground() < 60 * 1000L || stat.getPackageName().equals(getContext().getPackageName())) continue;
            String app = stat.getPackageName();
            try { app = getContext().getPackageManager().getApplicationLabel(getContext().getPackageManager().getApplicationInfo(stat.getPackageName(), 0)).toString(); } catch (Exception ignored) { }
            long minutes = stat.getTotalTimeInForeground() / 60000L;
            text(c, app, 36, y + row * 27, 10, primary, true);
            int appMah = telemetryAppMah(stat.getPackageName(), start, end);
            if (appMah <= 0 && totalForegroundMs > 0L) {
                appMah = Math.round(totalEnergy * stat.getTotalTimeInForeground() / (float) totalForegroundMs);
            }
            text(c, minutes + " min · " + (appMah > 0 ? "~" + appMah : "—") + " mAh est.", w - 166, y + row * 27, 8, muted, false);
            line(c, 36, y + row * 27 + 9, w - 36, y + row * 27 + 9, Color.rgb(43, 47, 56), 1);
            if (++row == 3) break;
        }
        if (row == 0) text(c, "No app usage recorded since unplugging.", 36, y, 9, faint, false);
        text(c, "Tap for all app details", 36, y + 87, 9, lime, true);
    }

    private void showAppUsageDetails() {
        if (!hasUsageAccess()) {
            try { getContext().startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, Uri.parse("package:" + getContext().getPackageName()))); }
            catch (Exception ignored) { getContext().startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)); }
            return;
        }
        UsageStatsManager manager = (UsageStatsManager) getContext().getSystemService(Context.USAGE_STATS_SERVICE);
        if (manager == null) return;
        long end = System.currentTimeMillis();
        long start = prefs.getBoolean("sinceFullActive", false)
                ? prefs.getLong("sinceFullStartAt", end - 24 * 60 * 60 * 1000L)
                : prefs.getLong(charging ? "lastDischargeStartAt" : "dischargeStartAt", end - 24 * 60 * 60 * 1000L);
        if (start >= end) start = end - 60 * 60 * 1000L;
        List<UsageStats> stats = manager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end);
        if (stats == null) return;
        Collections.sort(stats, new Comparator<UsageStats>() {
            @Override public int compare(UsageStats left, UsageStats right) { return Long.compare(right.getTotalTimeInForeground(), left.getTotalTimeInForeground()); }
        });
        long totalForegroundMs = 0L;
        for (UsageStats stat : stats) if (!stat.getPackageName().equals(getContext().getPackageName())) totalForegroundMs += stat.getTotalTimeInForeground();
        int totalEnergy = Math.max(0, dischargeMah());
        StringBuilder details = new StringBuilder("Foreground time since the current discharge began.\n\n");
        int row = 0;
        for (UsageStats stat : stats) {
            if (stat.getTotalTimeInForeground() < 60 * 1000L || stat.getPackageName().equals(getContext().getPackageName())) continue;
            String app = stat.getPackageName();
            try { app = getContext().getPackageManager().getApplicationLabel(getContext().getPackageManager().getApplicationInfo(stat.getPackageName(), 0)).toString(); } catch (Exception ignored) { }
            long minutes = stat.getTotalTimeInForeground() / 60000L;
            int appMah = telemetryAppMah(stat.getPackageName(), start, end);
            if (appMah <= 0 && totalForegroundMs > 0L) appMah = Math.round(totalEnergy * stat.getTotalTimeInForeground() / (float) totalForegroundMs);
            details.append(app).append("\n").append(minutes).append(" min · ")
                    .append(appMah > 0 ? "~" + appMah + " mAh estimated" : "mAh unavailable")
                    .append("\n\n");
            if (++row == 50) break;
        }
        if (row == 0) details.append("No app usage recorded since unplugging.");
        new AlertDialog.Builder(getContext()).setTitle("App usage details").setMessage(details.toString()).setPositiveButton("Close", null).show();
    }

    /** Estimate direct app-attributed drain from local telemetry intervals. */
    private int telemetryAppMah(String packageName, long start, long end) {
        String saved = telemetryPrefs.getString("telemetrySamples", "");
        if (saved.isEmpty()) return 0;
        String[] rows = saved.split("\\n");
        int total = 0;
        for (int index = 0; index < rows.length; index++) {
            String row = rows[index];
            String[] parts = row.split(",", 11);
            if (parts.length < 9 || !packageName.equals(parts[8])) continue;
            try {
                long timestamp = Long.parseLong(parts[0]);
                if (timestamp < start || timestamp > end || "1".equals(parts[2])) continue;
                int current = Math.abs(Integer.parseInt(parts[3]));
                if (current <= 0) continue;
                long intervalEnd = end;
                if (index + 1 < rows.length) {
                    String[] nextParts = rows[index + 1].split(",", 2);
                    try { intervalEnd = Long.parseLong(nextParts[0]); } catch (NumberFormatException ignored) { }
                }
                if (intervalEnd <= timestamp) intervalEnd = timestamp + samplingIntervalMs();
                intervalEnd = Math.min(end, Math.min(intervalEnd, timestamp + 2L * 60L * 60L * 1000L));
                if (intervalEnd > timestamp) total += Math.round(current * (intervalEnd - timestamp) / 3600000f);
            } catch (NumberFormatException ignored) { }
        }
        return total;
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
        text(c, "Capacity is estimated from local charge/discharge", 36, y + 493, 9, primary, false);
        text(c, "samples · last charge " + lastChargeEquivalentCycles(), 36, y + 510, 9, primary, false);
        text(c, "Total charged: " + (totalChargedMah() > 0 ? totalChargedMah() + " mAh" : "—"), w - 165, y + 493, 8, blue, true);
        text(c, "Equivalent: " + totalEquivalentCycles(), w - 165, y + 512, 8, blue, true);
        rounded(c, 18, y + 548, w - 18, y + 615, 12, panel); stroke(c, border, 1); rect.set(u(18), u(y + 548), u(w - 18), u(y + 615)); c.drawRoundRect(rect, u(12), u(12), p);
        text(c, benchmarkActive ? "Benchmark in progress" : "Manual benchmark", 36, y + 575, 11, primary, true);
        text(c, benchmarkActive ? "Charge above 95% to finish" : "Start below 25% for best results", 36, y + 595, 9, muted, false);
        rounded(c, w - 102, y + 566, w - 38, y + 596, 7, benchmarkActive ? Color.rgb(87, 108, 48) : lime);
        text(c, benchmarkActive ? "Active" : "Start", w - 88, y + 585, 9, benchmarkActive ? lime : Color.rgb(23, 28, 16), true);
        rounded(c, 18, y + 630, w - 18, y + 697, 12, panel); stroke(c, border, 1); rect.set(u(18), u(y + 630), u(w - 18), u(y + 697)); c.drawRoundRect(rect, u(12), u(12), p);
        text(c, "Design capacity", 36, y + 659, 11, primary, true);
            text(c, designCapacitySource(), 36, y + 680, 9, muted, false);
        text(c, String.format(Locale.US, "%,d mAh", designCapacityMah()), w - 112, y + 667, 10, lime, true);
        rounded(c, 18, y + 710, w - 18, y + 850, 12, panel); stroke(c, border, 1); rect.set(u(18), u(y + 710), u(w - 18), u(y + 850)); c.drawRoundRect(rect, u(12), u(12), p);
        text(c, "Capacity samples", 36, y + 740, 12, primary, true);
        if (healthSamples.size() < 2) {
            text(c, "Complete more charges to build a trend.", 36, y + 781, 9, muted, false);
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
        rounded(c, 18, y + 870, w - 18, y + 1045, 12, panel); stroke(c, border, 1); rect.set(u(18), u(y + 870), u(w - 18), u(y + 1045)); c.drawRoundRect(rect, u(12), u(12), p);
        text(c, "CHARGE WEAR", 36, y + 900, 10, muted, true);
        text(c, "Equivalent full cycles per charge session", 36, y + 922, 9, primary, false);
        ArrayList<String[]> wearRows = chargeWearRows();
        if (wearRows.isEmpty()) {
            text(c, "Complete a charge session to build this local trend.", 36, y + 980, 9, muted, false);
        } else {
            float chartX = 36, chartY = y + 938, chartW = w - 72, chartH = 74;
            line(c, chartX, chartY + chartH, chartX + chartW, chartY + chartH, border, 1);
            float max = 0.01f;
            for (String[] parts : wearRows) try { max = Math.max(max, Float.parseFloat(parts[7])); } catch (NumberFormatException ignored) { }
            for (int i = 0; i < wearRows.size(); i++) {
                float value;
                try { value = Math.max(0f, Float.parseFloat(wearRows.get(i)[7])); } catch (NumberFormatException ignored) { value = 0f; }
                float barW = Math.max(8f, chartW / wearRows.size() - 6f);
                float x = chartX + i * chartW / wearRows.size() + 3;
                float top = chartY + chartH - chartH * Math.min(1f, value / max);
                rounded(c, x, top, x + barW, chartY + chartH, 3, amber);
                if (i == 0 || i == wearRows.size() - 1) text(c, wearRows.get(i)[3], x, y + 1030, 7, faint, false);
            }
        }
    }

    private void drawHistoryPage(Canvas c, float w, float h, int panel, int raised, int border, int primary, int muted, int faint) {
        float y = 182;
        int rowCount = Math.min(150, sessions.size());
        float listBottom = y + 160 + rowCount * 44f;
        float panelBottom = Math.max(y + 610, listBottom + 250);
        rounded(c, 18, y, w - 18, panelBottom, 12, panel); stroke(c, border, 1); rect.set(u(18), u(y), u(w - 18), u(panelBottom)); c.drawRoundRect(rect, u(12), u(12), p);
        text(c, "HISTORY", 36, y + 31, 10, muted, true);
        text(c, "Charge & discharge sessions", 36, y + 61, 20, primary, true);
        text(c, "Stored locally · up to 150 sessions", 36, y + 83, 10, faint, false);
        line(c, 36, y + 105, w - 36, y + 105, border, 1);
        if (sessions.isEmpty()) {
            text(c, "No completed sessions yet.", 36, y + 145, 11, primary, true);
            text(c, "Keep the monitor running to create", 36, y + 171, 10, muted, false);
            text(c, "local history entries.", 36, y + 189, 10, muted, false);
        } else {
            text(c, "Date", 36, y + 130, 9, faint, true);
            text(c, "Type", w * .53f, y + 130, 9, faint, true);
            text(c, "Change", w * .71f, y + 130, 9, faint, true);
            text(c, "Length", w - 75, y + 130, 9, faint, true);
            int row = 0;
            for (String session : sessions) {
                String[] parts = session.split(",", -1);
                if (parts.length < 4) continue;
                float rowY = y + 160 + row * 44;
                line(c, 36, rowY - 18, w - 36, rowY - 18, border, 1);
                text(c, parts[3], 36, rowY, 9, muted, false);
                text(c, parts[0], w * .53f, rowY, 9, parts[0].equals("Charge") ? lime : blue, true);
                text(c, parts[1], w * .71f, rowY, 9, primary, true);
                text(c, parts[2], w - 75, rowY, 9, faint, false);
                if (++row == rowCount) break;
            }
        }
        float summaryY = listBottom + 35;
        line(c, 36, summaryY - 20, w - 36, summaryY - 20, border, 1);
        text(c, "Samples recorded", 36, summaryY + 13, 10, muted, false);
        text(c, String.valueOf(longHistory.size()), w - 75, summaryY + 13, 11, lime, true);
        text(c, "Rolling window: up to 30 local days", 36, summaryY + 39, 9, faint, false);
        text(c, "Deep sleep", 36, summaryY + 69, 10, muted, false);
        text(c, deepSleepTime(), w - 75, summaryY + 69, 11, Color.rgb(180, 154, 255), true);
        text(c, "Sessions: " + sessionCount("Charge") + " charge · " + sessionCount("Discharge") + " discharge", 36, summaryY + 99, 9, primary, true);
        text(c, "Energy: " + sessionEnergyDisplay("Charge", "+") + " / " + sessionEnergyDisplay("Discharge", "-"), 36, summaryY + 121, 9, blue, true);
        text(c, "Battery readings stay on this device.", 36, summaryY + 143, 9, primary, true);
        text(c, "Export only when you choose; no account or subscription.", 36, summaryY + 165, 8, muted, false);
        rounded(c, w - 136, panelBottom - 48, w - 36, panelBottom - 14, 8, lime);
        text(c, "Export CSV", w - 119, panelBottom - 26, 9, Color.rgb(23, 28, 16), true);
    }

    private void exportHistory() {
        ((MainActivity) getContext()).createCsvExport();
    }

    String historyCsv() {
        StringBuilder csv = new StringBuilder("type,change,duration,date,start_level,end_level,energy_mah,equivalent_full_cycles,screen_on_value,screen_off_value,screen_on_duration_min,screen_off_duration_min,deep_sleep_min,charger_source,start_timestamp_ms,end_timestamp_ms,screen_wakeups\n");
        for (String session : sessions) appendCsvRow(csv, session.split(",", -1));
        csv.append("\nlevel_percent\n");
        for (Integer point : longHistory) appendCsvRow(csv, new String[]{String.valueOf(point)});
        csv.append("\ntelemetry_timestamp_ms,level_percent,charging,current_ma,temperature_c,voltage_v,charge_counter_mah,screen_on,foreground_package,system_cycle_count,plugged\n");
        String telemetry = telemetryPrefs.getString("telemetrySamples", "");
        if (!telemetry.isEmpty()) for (String row : telemetry.split("\\n")) appendCsvRow(csv, row.split(",", -1));
        return csv.toString();
    }

    private void appendCsvRow(StringBuilder csv, String[] fields) {
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) csv.append(',');
            csv.append(csvField(fields[i]));
        }
        csv.append('\n');
    }

    private String csvField(String value) {
        if (value == null) value = "";
        if (!value.isEmpty() && (value.charAt(0) == '=' || value.charAt(0) == '+'
                || value.charAt(0) == '-' || value.charAt(0) == '@')) value = "'" + value;
        boolean quote = value.indexOf(',') >= 0 || value.indexOf('"') >= 0
                || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0;
        return quote ? "\"" + value.replace("\"", "\"\"") + "\"" : value;
    }

    private void showSessionDetails(int index) {
        if (index < 0 || index >= sessions.size()) return;
        String[] parts = sessions.get(index).split(",", -1);
        if (parts.length < 4) return;
        boolean charge = "Charge".equals(parts[0]);
        StringBuilder details = new StringBuilder();
        details.append(parts[0]).append(" session\n");
        details.append("Date: ").append(parts[3]).append('\n');
        details.append("Change: ").append(parts[1]).append('\n');
        details.append("Duration: ").append(parts[2]);
        if (parts.length >= 7) {
            details.append("\nStart: ").append(parts[4]).append('%');
            details.append("\nEnd: ").append(parts[5]).append('%');
            details.append("\nEnergy: ").append(parts[6]).append(" mAh");
            if (parts.length >= 8) details.append("\nEquivalent full cycles: ").append(parts[7]);
        }
        if (parts.length >= 14) {
            String unit = charge ? "mAh" : "%";
            String on = parts[8];
            String off = parts[9];
            if (!charge) {
                try { on = String.format(Locale.US, "%.1f", Integer.parseInt(parts[8]) / 10f); } catch (NumberFormatException ignored) { }
                try { off = String.format(Locale.US, "%.1f", Integer.parseInt(parts[9]) / 10f); } catch (NumberFormatException ignored) { }
            }
            details.append("\nScreen on: ").append(on).append(' ').append(unit);
            details.append("\nScreen off: ").append(off).append(' ').append(unit);
            details.append("\nScreen-on time: ").append(parts[10]).append(" min");
            details.append("\nScreen-off time: ").append(parts[11]).append(" min");
            if (!charge) details.append("\nDeep sleep: ").append(parts[12]).append(" min");
            if (charge) details.append("\nCharger: ").append(parts[13]);
            if (parts.length >= 16) {
                details.append("\nStarted: ").append(formatTimestamp(parts[14]));
                details.append("\nEnded: ").append(formatTimestamp(parts[15]));
            }
            if (parts.length >= 17) details.append("\nScreen wakeups: ").append(parts[16]);
        }
        new AlertDialog.Builder(getContext())
                .setTitle("Session details")
                .setMessage(details.toString())
                .setNegativeButton("Close", null)
                .setPositiveButton(charge ? "Open charging" : "Open discharging", (dialog, which) -> {
                    page = charge ? 1 : 2;
                    updateLayoutHeight();
                    if (getParent() instanceof ScrollView) ((ScrollView) getParent()).smoothScrollTo(0, 0);
                    invalidate();
                })
                .show();
    }

    private String formatTimestamp(String value) {
        try {
            long timestamp = Long.parseLong(value);
            return new SimpleDateFormat("MMM d, HH:mm", Locale.US).format(new Date(timestamp));
        } catch (NumberFormatException ignored) {
            return value;
        }
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
        text(c, chartAxisLabel(0f), chartX, y + 166, 9, faint, false); text(c, chartAxisLabel(.33f), chartX + chartW * .32f, y + 166, 9, faint, false); text(c, chartAxisLabel(.66f), chartX + chartW * .64f, y + 166, 9, faint, false); text(c, chartAxisLabel(1f), chartX + chartW - 23, y + 166, 9, faint, false);
        text(c, "Average " + chartAverage() + " · range " + chartRange(), x + 18, y + 189, 8, muted, false);
        text(c, "Recent sessions", x + 18, y + 204, 13, primary, true);
        if (sessions.isEmpty()) {
            text(c, "Complete a cycle to see sessions.", x + 18, y + 230, 9, faint, false);
        } else {
            int row = 0;
            for (String session : sessions) {
                String[] parts = session.split(",", -1);
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

    private void drawTelemetryChart(Canvas c, float x, float y, float width, float height,
                                    int panel, int border, int primary, int muted, int faint,
                                    boolean chargingFilter) {
        rounded(c, x, y, x + width, y + height, 12, panel);
        stroke(c, border, 1);
        rect.set(u(x), u(y), u(x + width), u(y + height));
        c.drawRoundRect(rect, u(12), u(12), p);
        text(c, chargingFilter ? "Charging current" : "Discharging current", x + 18, y + 28, 15, primary, true);
        String saved = telemetryPrefs.getString("telemetrySamples", "");
        ArrayList<Integer> values = new ArrayList<>();
        if (!saved.isEmpty()) {
            for (String row : saved.split("\\n")) {
                String[] parts = row.split(",", 11);
                if (parts.length < 4 || !String.valueOf(chargingFilter ? 1 : 0).equals(parts[2])) continue;
                try {
                    int signedCurrent = Integer.parseInt(parts[3]);
                    // Telemetry keeps direction explicit: charging is positive,
                    // discharging is negative. The chart shows magnitude while
                    // retaining the selected direction in its title/filter.
                    boolean matchesDirection = chargingFilter ? signedCurrent > 0 : signedCurrent < 0;
                    if (matchesDirection) values.add(Math.abs(signedCurrent));
                } catch (NumberFormatException ignored) { }
            }
        }
        int count = Math.min(48, values.size());
        float chartX = x + 18, chartY = y + 51, chartW = width - 36, chartH = 105;
        for (int i = 0; i < 3; i++) line(c, chartX, chartY + i * chartH / 2f, chartX + chartW, chartY + i * chartH / 2f, border, 1);
        if (count == 0) {
            text(c, "Waiting for local telemetry samples.", chartX, chartY + 57, 10, faint, false);
            return;
        }
        int min = Integer.MAX_VALUE, max = 0, total = 0;
        for (int i = values.size() - count; i < values.size(); i++) {
            int current = values.get(i);
            min = Math.min(min, current);
            max = Math.max(max, current);
            total += current;
        }
        int scaleMax = Math.max(100, max);
        Path path = new Path();
        for (int i = 0; i < count; i++) {
            int current = values.get(values.size() - count + i);
            float px = count == 1 ? chartX : chartX + i * chartW / (count - 1);
            float py = chartY + chartH - current * chartH / (float) scaleMax;
            if (i == 0) path.moveTo(u(px), u(py)); else path.lineTo(u(px), u(py));
        }
        stroke(c, chargingFilter ? lime : blue, 2);
        c.drawPath(path, p);
        text(c, max + " mA peak", chartX, y + 181, 9, muted, false);
        text(c, String.format(Locale.US, "avg %d mA", Math.round(total / (float) count)), x + width - 92, y + 181, 9, muted, false);
        text(c, "last " + count + " local samples", chartX, y + 199, 8, faint, false);
    }

    /** Uses the requested time window from telemetry; old APKs fall back to their level history. */
    private ArrayList<Integer> chartLevels() {
        ArrayList<Integer> values = new ArrayList<>();
        long end = System.currentTimeMillis();
        long start = end - (historyDays == 30 ? 30L : 7L) * 24L * 60L * 60L * 1000L;
        String saved = telemetryPrefs.getString("telemetrySamples", "");
        if (!saved.isEmpty()) {
            for (String row : saved.split("\\n")) {
                String[] parts = row.split(",", 3);
                if (parts.length < 2) continue;
                try {
                    long timestamp = Long.parseLong(parts[0]);
                    if (timestamp < start || timestamp > end) continue;
                    values.add(Math.max(0, Math.min(100, Integer.parseInt(parts[1]))));
                } catch (NumberFormatException ignored) { }
            }
        }
        if (!values.isEmpty()) return values;
        ArrayList<Integer> fallback = historyDays == 30 ? longHistory : history;
        values.addAll(fallback);
        return values;
    }

    private String chartAxisLabel(float fraction) {
        long end = System.currentTimeMillis();
        long start = end - (historyDays == 30 ? 30L : 7L) * 24L * 60L * 60L * 1000L;
        String pattern = historyDays == 30 ? "d MMM" : "EEE";
        return new SimpleDateFormat(pattern, Locale.US).format(new Date(start + (long) ((end - start) * fraction)));
    }

    private float[] chartValues() {
        ArrayList<Integer> source = chartLevels();
        if (source.isEmpty()) return new float[0];
        int count = Math.min(12, source.size());
        float[] values = new float[count];
        for (int i = 0; i < count; i++) {
            int value = source.get(source.size() - count + i);
            values[i] = Math.max(0, Math.min(100, value)) / 100f;
        }
        return values;
    }

    private String chartAverage() {
        ArrayList<Integer> source = chartLevels();
        if (source.isEmpty()) return "—";
        int total = 0;
        for (Integer value : source) total += Math.max(0, Math.min(100, value));
        return String.format(Locale.US, "%.0f%%", total / (float) source.size());
    }

    private String chartRange() {
        ArrayList<Integer> source = chartLevels();
        if (source.isEmpty()) return "—";
        int min = 100;
        int max = 0;
        for (Integer value : source) {
            int clipped = Math.max(0, Math.min(100, value));
            min = Math.min(min, clipped);
            max = Math.max(max, clipped);
        }
        return min + "–" + max + "%";
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
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            touchDownY = event.getY();
            lastTouchY = touchDownY;
            touchDragged = false;
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            float currentY = event.getY();
            float delta = lastTouchY - currentY;
            if (Math.abs(currentY - touchDownY) > 8f * density) touchDragged = true;
            if (touchDragged && getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
            if (touchDragged && getParent() instanceof ScrollView) {
                ((ScrollView) getParent()).scrollBy(0, Math.round(delta));
            }
            lastTouchY = currentY;
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_CANCEL) {
            touchDragged = false;
            if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
            return true;
        }
        if (event.getAction() != MotionEvent.ACTION_UP) return true;
        if (touchDragged) {
            touchDragged = false;
            if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
            return true;
        }
        performClick();
        float x = event.getX() / density, y = event.getY() / density;
        if (System.currentTimeMillis() - lastTouch < 80) return true;
        lastTouch = System.currentTimeMillis();
        float w = getWidth() / density;
        if (y < 60 && x > w - 180 && x < w - 143) { showSettings(); return true; }
        if (y < 60 && x > w - 138) { light = !light; amoled = false; prefs.edit().putBoolean("lightTheme", light).putBoolean("amoledTheme", amoled).apply(); invalidate(); return true; }
        if (y >= 132 && y < 166) {
            float cell = (w - 36) / 5f;
            page = Math.max(0, Math.min(4, (int) ((x - 18) / cell)));
            updateLayoutHeight();
            invalidate();
            return true;
        }
        if (page == 0 && y > 750 && y < 805 && x > w - 140) {
            historyDays = historyDays == 7 ? 30 : 7;
            prefs.edit().putInt("historyDays", historyDays).apply();
            invalidate();
            return true;
        }
        if (page == 1 && y > 455 && y < 510 && x > w - 130) {
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
        if (page == 1 && y > 500 && y < 560 && x > w - 140) {
            setOverlayEnabled(!overlayEnabled);
            return true;
        }
        int historyRows = Math.min(150, sessions.size());
        if (page == 4 && y >= 342 && y < 342 + historyRows * 44f && x < w - 145 && !sessions.isEmpty()) {
            int index = (int) ((y - 342) / 44);
            showSessionDetails(index);
            return true;
        }
        if (page == 4 && y > historyExportTop() && y < historyExportTop() + 45 && x > w - 155) {
            exportHistory();
            return true;
        }
        if (page == 3 && y > 700 && y < 815 && x > w - 140) {
            if (benchmarkActive) {
                benchmarkActive = false;
                prefs.edit().putBoolean("benchmarkActive", false)
                        .remove("benchmarkStartLevel").remove("benchmarkStartCounterMah")
                        .remove("benchmarkChargeLastCounterMah").remove("benchmarkChargeAddedMah")
                        .remove("benchmarkChargeStatsBaselineMah").apply();
            } else if (charging || level > 25) {
                Toast.makeText(getContext(), "Start the benchmark below 25% while unplugged.", Toast.LENGTH_LONG).show();
            } else {
                benchmarkActive = true;
                SharedPreferences.Editor editor = prefs.edit().putBoolean("benchmarkActive", true)
                        .putInt("benchmarkStartLevel", level).putInt("benchmarkChargeAddedMah", 0)
                        .remove("benchmarkChargeLastCounterMah")
                        .remove("benchmarkChargeStatsBaselineMah");
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
        if (page == 2 && y > 640 && y < 920) {
            showAppUsageDetails();
            return true;
        }
        return true;
    }

    @Override public boolean performClick() {
        super.performClick();
        return true;
    }
}
