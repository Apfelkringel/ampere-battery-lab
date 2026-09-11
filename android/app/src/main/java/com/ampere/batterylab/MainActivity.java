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
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.view.Gravity;
import android.widget.ScrollView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.text.InputType;
import android.content.SharedPreferences;
import android.os.Build;
import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.app.usage.UsageEvents;
import android.provider.Settings;
import android.net.Uri;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowInsets;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends Activity {
    private static final int CREATE_BACKUP_REQUEST = 1201;
    private static final int RESTORE_BACKUP_REQUEST = 1202;
    private static final int RESEARCH_EXPORT_REQUEST = 1203;
    private static final int CSV_EXPORT_REQUEST = 1204;
    private static final int DIAGNOSTIC_EXPORT_REQUEST = 1205;
    private static final int MAX_BACKUP_BYTES = 4 * 1024 * 1024;
    private static final String DATA_PREFS = "ampere-data";
    private static final String TELEMETRY_PREFS = "ampere-telemetry";
    private static final Set<String> RESTORABLE_DATA_KEYS = new HashSet<>(Arrays.asList(
            "history", "historyLong", "lastSample", "healthSamples", "sessions",
            "sessionStartedAt", "sessionStartLevel", "sessionStartChargeCounterMah", "lastCharging",
            "chargeAlarm", "chargeAlarmSent", "chargeAlarmLastLevel", "chargeLimit", "benchmarkActive", "benchmarkCapacityMah",
            "temperatureAlarm", "temperatureAlarmSent", "temperatureAlarmThresholdTenths",
            "dischargeAlarm", "dischargeAlarmSent", "dischargeAlarmThreshold", "dischargeAlarmLastLevel",
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
            "chargeAnchorAt", "chargeAnchorLevel", "chargeAnchorType", "chargeAnchorFullReachedThisPlug",
            "systemCycleCount", "systemCycleCountSource", "estimatedCycleLastCounterUah", "estimatedCycleFraction", "estimatedCycleCount", "cycleHistory",
            "monitorLastCharging", "monitorSampleAt", "monitorSessionStartCounterMah",
            "monitorSessionStartLevel", "monitorSessionStartedAt", "lastChargingCurrentMa", "monitoringMs", "screenOffDurationMin",
            "screenOnMs", "screenSampleAt"
    ));
    private static final Set<String> RESTORABLE_TELEMETRY_KEYS = new HashSet<>(Arrays.asList(
            "telemetrySamples", "telemetryLastSampleAt"
    ));
    private BatteryDashboard dashboard;
    private boolean batteryReceiverRegistered;
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
        window.setStatusBarColor(Color.rgb(11, 16, 17));
        window.setNavigationBarColor(Color.rgb(11, 16, 17));
        window.getDecorView().setSystemUiVisibility(0);
        dashboard = new BatteryDashboard(this);
        dashboard.applySystemBarTheme();
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        if (Build.VERSION.SDK_INT >= 35) {
            window.setDecorFitsSystemWindows(false);
            scroll.setOnApplyWindowInsetsListener((view, insets) -> {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
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
        batteryReceiverRegistered = true;
        if (battery != null) dashboard.readBattery(battery);
    }

    @Override protected void onResume() {
        super.onResume();
        if (dashboard == null) return;
        UpdateChecker.onActivityResumed(this);
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
        if (batteryReceiverRegistered) {
            try { unregisterReceiver(batteryReceiver); } catch (IllegalArgumentException ignored) { }
            batteryReceiverRegistered = false;
        }
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
        else if (requestCode == DIAGNOSTIC_EXPORT_REQUEST) writeDiagnosticExport(uri);
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
            SharedPreferences restoredTelemetryPrefs = getSharedPreferences(TELEMETRY_PREFS, MODE_PRIVATE);
            String restoredTelemetry = restoredTelemetryPrefs.getString("telemetrySamples", "");
            String normalizedTelemetry = BatteryExportRules.normalizeTelemetry(restoredTelemetry);
            if (!restoredTelemetry.equals(normalizedTelemetry)) {
                restoredTelemetryPrefs.edit().putString("telemetrySamples", normalizedTelemetry).apply();
            }
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
        if (!newPrefs.contains("telemetrySamples") && oldPrefs.contains("telemetrySamples")) {
            SharedPreferences.Editor migration = newPrefs.edit()
                    .putString("telemetrySamples", oldPrefs.getString("telemetrySamples", ""));
            if (oldPrefs.contains("telemetryLastSampleAt")) {
                migration.putLong("telemetryLastSampleAt", oldPrefs.getLong("telemetryLastSampleAt", 0L));
            }
            if (migration.commit()) {
                oldPrefs.edit().remove("telemetrySamples").remove("telemetryLastSampleAt").commit();
            }
        }
        String saved = newPrefs.getString("telemetrySamples", "");
        String normalized = BatteryExportRules.normalizeTelemetry(saved);
        if (!saved.equals(normalized)) newPrefs.edit().putString("telemetrySamples", normalized).commit();
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

    void createDiagnosticExport() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TITLE, "ampere-diagnosebericht.txt");
        startActivityForResult(intent, DIAGNOSTIC_EXPORT_REQUEST);
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

    private void writeDiagnosticExport(Uri uri) {
        try (OutputStream stream = getContentResolver().openOutputStream(uri)) {
            if (stream == null) throw new IllegalStateException("No output stream");
            String telemetry = getSharedPreferences(TELEMETRY_PREFS, MODE_PRIVATE)
                    .getString("telemetrySamples", "");
            long interval = dashboard == null ? 15L * 60L * 1000L : dashboard.samplingIntervalMs();
            byte[] output = BatteryDiagnosticReport.build(telemetry, interval,
                    System.currentTimeMillis()).getBytes(StandardCharsets.UTF_8);
            if (output.length > MAX_BACKUP_BYTES) throw new IllegalArgumentException("Report too large");
            stream.write(output);
            Toast.makeText(this, "Diagnosebericht gespeichert.", Toast.LENGTH_LONG).show();
        } catch (Exception ignored) {
            Toast.makeText(this, "Diagnosebericht konnte nicht gespeichert werden.", Toast.LENGTH_LONG).show();
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
            root.put("batteryTechnology", dashboard == null ? "" : dashboard.technologyForExport());
            root.put("chargerCapability", dashboard == null
                    ? new JSONObject() : dashboard.chargerCapabilityForExport());
            root.put("internalResistance", dashboard == null
                    ? new JSONObject() : dashboard.internalResistanceForExport());
            root.put("capacityErrorMargin", dashboard == null
                    ? new JSONObject() : dashboard.capacityErrorMarginForExport());
            root.put("kernelChargeType", dashboard == null
                    ? new JSONObject() : dashboard.chargeTypeForExport());
            root.put("kernelChargeBehaviour", dashboard == null
                    ? new JSONObject() : dashboard.chargeBehaviourForExport());
            root.put("oemChargeControl", dashboard == null
                    ? new JSONObject() : dashboard.chargeControlForExport());
            root.put("batteryManufactureDate", dashboard == null
                    ? new JSONObject() : dashboard.manufactureDateForExport());
            root.put("remainingBatteryEnergy", dashboard == null
                    ? new JSONObject() : dashboard.remainingEnergyForExport());
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
                if (parts.length > 16) {
                    Integer wakeups = BatteryExportRules.nonNegativeInt(parts[16]);
                    if (wakeups != null) row.put("screenWakeups", wakeups);
                }
                if (parts.length > 8) row.put("screenValueUnit", "Charge".equals(parts[0]) ? "mAh" : "percent");
                sessionRows.put(row);
            }
            root.put("sessions", sessionRows);

            JSONArray cycleRows = new JSONArray();
            if (dashboard != null) for (BatteryCycleHistory.Point point : dashboard.cycleHistoryForExport()) {
                JSONObject row = new JSONObject();
                row.put("date", point.date);
                row.put("totalCycles", point.cycles);
                row.put("source", point.source);
                cycleRows.put(row);
            }
            root.put("cycleHistory", cycleRows);

            SharedPreferences data = getSharedPreferences(DATA_PREFS, MODE_PRIVATE);
            JSONObject sinceCharge = new JSONObject();
            boolean sinceChargeActive = data.getBoolean("sinceFullActive", false);
            sinceCharge.put("active", sinceChargeActive);
            if (sinceChargeActive) {
                String anchorType = data.getString("chargeAnchorType", BatteryChargeAnchor.FULL);
                if (!BatteryChargeAnchor.FULL.equals(anchorType)
                        && !BatteryChargeAnchor.UNPLUGGED.equals(anchorType)) {
                    anchorType = BatteryChargeAnchor.FULL;
                }
                sinceCharge.put("anchorType", anchorType);
                sinceCharge.put("anchorTimestampMs", data.getLong("sinceFullStartAt", 0L));
                sinceCharge.put("anchorLevelPercent", BatteryLevel.normalizePercent(
                        data.getInt("sinceFullStartLevel", -1)));
                sinceCharge.put("currentLevelPercent", BatteryLevel.normalizePercent(
                        data.getInt("sinceFullLastLevel", -1)));
                sinceCharge.put("usedPercent", BatteryPercentage.normalizeCumulative(
                        data.getFloat("sinceFullPercent", 0f)));
                sinceCharge.put("usedMah", Math.max(0, data.getInt("sinceFullMah", 0)));
            }
            root.put("sinceCharge", sinceCharge);

            JSONArray telemetryRows = new JSONArray();
            String telemetry = getSharedPreferences(TELEMETRY_PREFS, MODE_PRIVATE).getString("telemetrySamples", "");
            for (String sample : BatteryExportRules.validTelemetryRows(telemetry)) {
                String[] parts = sample.split(",", -1);
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
                row.put("batteryPowerMw", BatteryPowerStats.milliWatts(parts));
                telemetryRows.put(row);
            }
            root.put("telemetry", telemetryRows);
            BatteryTelemetryDiagnostics.Summary diagnostics = BatteryTelemetryDiagnostics.analyze(
                    telemetry, dashboard == null ? 15L * 60L * 1000L : dashboard.samplingIntervalMs());
            JSONObject diagnosticJson = new JSONObject();
            diagnosticJson.put("sampleCount", diagnostics.sampleCount);
            diagnosticJson.put("dischargeSamples", diagnostics.dischargeSamples);
            diagnosticJson.put("minTemperatureC", diagnostics.minTemperatureTenths / 10.0);
            diagnosticJson.put("averageTemperatureC", diagnostics.averageTemperatureTenths / 10.0);
            diagnosticJson.put("maxTemperatureC", diagnostics.maxTemperatureTenths / 10.0);
            diagnosticJson.put("minDischargeVoltageV", diagnostics.minDischargeVoltageMv / 1000.0);
            diagnosticJson.put("minDischargeVoltageLevelPercent", diagnostics.minDischargeVoltageLevel);
            diagnosticJson.put("peakDischargeMa", diagnostics.peakDischargeMa);
            diagnosticJson.put("chargingPowerMinMw", diagnostics.powerStats.charging.minimumMw);
            diagnosticJson.put("chargingPowerAverageMw", diagnostics.powerStats.charging.averageMw);
            diagnosticJson.put("chargingPowerMaxMw", diagnostics.powerStats.charging.maximumMw);
            diagnosticJson.put("chargingPowerSamples", diagnostics.powerStats.charging.sampleCount);
            diagnosticJson.put("dischargingPowerMinMw", diagnostics.powerStats.discharging.minimumMw);
            diagnosticJson.put("dischargingPowerAverageMw", diagnostics.powerStats.discharging.averageMw);
            diagnosticJson.put("dischargingPowerMaxMw", diagnostics.powerStats.discharging.maximumMw);
            diagnosticJson.put("dischargingPowerSamples", diagnostics.powerStats.discharging.sampleCount);
            diagnosticJson.put("largestGapMs", diagnostics.largestGapMs);
            diagnosticJson.put("samplingGapDetected", diagnostics.samplingGap);
            diagnosticJson.put("highTemperatureDetected", diagnostics.hasHighTemperature());
            diagnosticJson.put("voltageRule", "reported only; no fixed pack-voltage sag threshold");
            root.put("telemetryDiagnostics", diagnosticJson);
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
    private int level = -1;
    private float temperature = 0f;
    private float voltage = 0f;
    private int platformHealth = BatteryManager.BATTERY_HEALTH_UNKNOWN;
    private int capacityLevel = -1;
    private int chargingStatus = 0;
    private int thermalStatus = BatteryThermalStatus.UNKNOWN;
    private int oemChargeLimitPercent = 0;
    private int oemChargeStartPercent = 0;
    private BatteryChargeControl.Reading oemChargeControl = BatteryChargeControl.Reading.unavailable();
    private BatteryManufactureDate.Reading manufactureDate = BatteryManufactureDate.Reading.unavailable();
    private long remainingEnergyNanoWattHours = 0L;
    private String technology = "";
    private BatteryChargerCapability.Reading chargerCapability = BatteryChargerCapability.Reading.empty();
    private BatteryInternalResistance.Reading internalResistance = BatteryInternalResistance.Reading.unavailable();
    private BatteryCapacityErrorMargin.Reading capacityErrorMargin = BatteryCapacityErrorMargin.Reading.unavailable();
    private BatteryChargeType.Reading kernelChargeType = BatteryChargeType.Reading.unavailable();
    private BatteryChargeBehaviour.Reading kernelChargeBehaviour = BatteryChargeBehaviour.Reading.unavailable();
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
    private final ArrayList<BatteryCycleHistory.Point> cycleHistory = new ArrayList<>();
    private final ArrayList<String> sessions = new ArrayList<>();
    private BatteryHealth.HealthReading healthReading = new BatteryHealth.HealthReading(0, 0, "");
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
    private int pressedRegion = 0;
    private final AccessibilityNodeProvider accessibilityNodeProvider = new DashboardAccessibilityNodeProvider();
    private int accessibilityFocusedVirtualView = AccessibilityNodeProvider.HOST_VIEW_ID;
    private int hoveredVirtualView = AccessibilityNodeProvider.HOST_VIEW_ID;
    // Drawing happens in two coordinate spaces: the full window for the
    // header and the centered body column for every page. Keeping the active
    // width here prevents generic text helpers from measuring against the
    // physical display and overflowing cards on tablets/foldables.
    private float layoutWidthDp;
    private float viewportWidthDp;
    private float viewportHeightDp;
    private final int lime = Color.rgb(199, 243, 107);
    private final int blue = Color.rgb(118, 184, 255);
    private final int amber = Color.rgb(242, 179, 106);
    private final int violet = Color.rgb(180, 154, 255);

    BatteryDashboard(Context context) {
        super(context);
        density = getResources().getDisplayMetrics().density;
        p.setTypeface(Typeface.create("sans", Typeface.NORMAL));
        setFocusable(true);
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        prefs = context.getSharedPreferences("ampere-data", Context.MODE_PRIVATE);
        telemetryPrefs = context.getSharedPreferences("ampere-telemetry", Context.MODE_PRIVATE);
        loadStoredData();
        refreshHealthReading();
        updateAccessibilitySummary();
    }

    /** Exposes the Canvas controls as real logical controls to TalkBack. */
    @Override public AccessibilityNodeProvider getAccessibilityNodeProvider() {
        return accessibilityNodeProvider;
    }

    @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(BatteryDashboard.class.getName());
        info.setScrollable(true);
        info.setFocusable(true);
    }

    void applySystemBarTheme() {
        Window window = ((Activity) getContext()).getWindow();
        int surface = light ? Color.rgb(246, 248, 243) : (amoled ? Color.BLACK : Color.rgb(11, 16, 17));
        window.setStatusBarColor(surface);
        window.setNavigationBarColor(surface);
        int flags = 0;
        if (light) {
            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= 26) flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }
        window.getDecorView().setSystemUiVisibility(flags);
    }

    void reloadStoredData() {
        history.clear();
        longHistory.clear();
        healthSamples.clear();
        cycleHistory.clear();
        sessions.clear();
        loadStoredData();
        refreshHealthReading();
        updateLayoutHeight();
        invalidate();
    }

    ArrayList<String> sessionsForExport() {
        return new ArrayList<>(sessions);
    }

    private void updateLayoutHeight() {
        int rowCount = Math.min(150, sessions.size());
        int contentDp = page == 4 ? Math.max(1320, 600 + rowCount * 44) : (page == 1 ? 1550 : (page == 3 ? 1500 : 1320));
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
        return panelBottom - 58;
    }

    private float overviewChartTop() {
        float width = getWidth() / density;
        float bodyWidth = contentWidth(width);
        if (viewportWidthDp >= 600f && viewportHeightDp > 0f && viewportHeightDp < 390f) return 174 + 218;
        if (viewportWidthDp >= 600f && viewportHeightDp > 0f && viewportHeightDp < 600f) return 182 + 218;
        float heroWidth = Math.min(bodyWidth - 36, 520);
        float heroHeight = heroWidth < 410f ? 400f : 320f;
        return 182 + heroHeight + 14 + 234;
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
        int normalizedLevel = BatteryLevel.percent(rawLevel, scale);
        if (normalizedLevel < 0) {
            updateAccessibilitySummary();
            invalidate();
            return;
        }
        level = normalizedLevel;
        boolean detectedCharging = BatteryState.isCharging(status, pluggedSource,
                intent.hasExtra(BatteryManager.EXTRA_PLUGGED));
        long now = System.currentTimeMillis();
        long monitorSampleAt = prefs.getLong("monitorSampleAt", 0L);
        boolean hasRecentMonitorSample = monitorSampleAt > 0L
                && now >= monitorSampleAt && now - monitorSampleAt <= 2L * 60L * 60L * 1000L;
        boolean newCharging = BatteryState.resolveUiCharging(detectedCharging, hasRecentMonitorSample,
                prefs.getBoolean("monitorLastCharging", detectedCharging));
        if (sessionStartedAt == 0L) {
            lastCharging = newCharging;
            sessionStartedAt = now;
            sessionStartLevel = level;
        } else if (newCharging != lastCharging) {
            lastCharging = newCharging;
            sessionStartedAt = now;
            sessionStartLevel = level;
            sessionStartChargeCounterMah = 0;
        }
        charging = newCharging;
        plugged = pluggedSource;
        int temp = BatteryTemperature.normalizeTenths(
                intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1));
        temperature = temp > 0 ? temp / 10f : 0f;
        platformHealth = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN);
        technology = BatteryTechnology.read(intent);
        capacityLevel = BatteryCapacityLevel.fromIntent(intent);
        chargingStatus = BatteryChargingState.fromIntent(intent);
        thermalStatus = BatteryThermalStatus.read(getContext());
        oemChargeControl = BatteryChargeControl.read();
        oemChargeLimitPercent = oemChargeControl.endThresholdPercent;
        oemChargeStartPercent = oemChargeControl.startThresholdPercent;
        chargerCapability = BatteryChargerCapability.read(intent);
        internalResistance = BatteryInternalResistance.read();
        capacityErrorMargin = BatteryCapacityErrorMargin.read();
        kernelChargeType = BatteryChargeType.read();
        kernelChargeBehaviour = BatteryChargeBehaviour.read();
        manufactureDate = BatteryManufactureDate.read();
        int mv = BatteryVoltage.readMilliVolts(intent);
        voltage = mv > 0 ? mv / 1000f : 0f;
        BatteryManager manager = (BatteryManager) getContext().getSystemService(Context.BATTERY_SERVICE);
        remainingEnergyNanoWattHours = BatteryEnergy.readNanoWattHours(manager);
        currentMa = BatteryCurrent.milliAmps(manager, newCharging, level);
        signedCurrentMa = currentMa == 0 ? 0 : (newCharging ? currentMa : -currentMa);
        chargeCounterMah = BatteryChargeCounter.toMilliampereHours(
                BatteryChargeCounter.readMicroampereHours(manager));
        if (chargeCounterMah > 0) {
            if (sessionStartChargeCounterMah <= 0) {
                sessionStartChargeCounterMah = chargeCounterMah;
                prefs.edit().putInt("sessionStartChargeCounterMah", sessionStartChargeCounterMah).apply();
            }
        }
        prefs.edit().putBoolean("lastCharging", lastCharging)
                .putLong("sessionStartedAt", sessionStartedAt)
                .putInt("sessionStartLevel", sessionStartLevel)
                .putInt("sessionStartChargeCounterMah", sessionStartChargeCounterMah)
                .apply();
        benchmarkActive = prefs.getBoolean("benchmarkActive", benchmarkActive);
        saveSample();
        reloadLiveCollections();
        updateAccessibilitySummary();
        invalidate();
        BatteryWidgetProvider.updateAll(getContext());
        BatteryQuickSettingsService.requestRefresh(getContext());
    }

    private void reloadLiveCollections() {
        healthSamples.clear();
        loadAndCleanHealthSamples();
        cycleHistory.clear();
        cycleHistory.addAll(BatteryCycleHistory.parse(prefs.getString("cycleHistory", "")));
        sessions.clear();
        loadSessions(prefs.getString("sessions", ""));
        refreshHealthReading();
        chargeAlarm = prefs.getBoolean("chargeAlarm", chargeAlarm);
        chargeLimit = loadChargeLimit();
        benchmarkActive = prefs.getBoolean("benchmarkActive", benchmarkActive);
        updateLayoutHeight();
    }

    private void reloadHealthSamples() {
        healthSamples.clear();
        loadAndCleanHealthSamples();
        refreshHealthReading();
    }

    private void refreshHealthReading() {
        healthReading = BatteryHealth.read(getContext(), prefs, designCapacityMah());
    }

    private void loadAndCleanHealthSamples() {
        String saved = prefs.getString("healthSamples", "");
        ArrayList<Integer> cleaned = BatteryHealth.parseSamples(saved);
        healthSamples.addAll(cleaned);
        String normalized = BatteryHealth.serializeSamples(cleaned);
        if (!saved.equals(normalized)) prefs.edit().putString("healthSamples", normalized).apply();
    }

    private void loadStoredData() {
        String savedHistory = prefs.getString("history", "");
        String normalizedHistory = BatteryLevel.normalizeSerialized(savedHistory);
        if (!normalizedHistory.isEmpty()) {
            for (String value : normalizedHistory.split(",")) {
                try {
                    history.add(Integer.parseInt(value.trim()));
                } catch (NumberFormatException ignored) { }
            }
        }
        while (history.size() > 48) history.remove(0);
        String savedLongHistory = prefs.getString("historyLong", "");
        String normalizedLongHistory = BatteryLevel.normalizeSerialized(savedLongHistory);
        if (!normalizedLongHistory.isEmpty()) {
            for (String value : normalizedLongHistory.split(",")) {
                try {
                    longHistory.add(Integer.parseInt(value.trim()));
                } catch (NumberFormatException ignored) { }
            }
        }
        if (longHistory.isEmpty()) longHistory.addAll(history);
        while (longHistory.size() > longHistoryRetentionSamples()) longHistory.remove(0);
        String canonicalHistory = BatteryLevel.serialize(history);
        String canonicalLongHistory = BatteryLevel.serialize(longHistory);
        if (!savedHistory.equals(canonicalHistory) || !savedLongHistory.equals(canonicalLongHistory)) {
            prefs.edit().putString("history", canonicalHistory).putString("historyLong", canonicalLongHistory).apply();
        }
        loadAndCleanHealthSamples();
        cycleHistory.addAll(BatteryCycleHistory.parse(prefs.getString("cycleHistory", "")));
        String savedSessions = prefs.getString("sessions", "");
        loadSessions(savedSessions);
        sessionStartedAt = prefs.getLong("sessionStartedAt", 0L);
        sessionStartLevel = prefs.getInt("sessionStartLevel", level);
        sessionStartChargeCounterMah = prefs.getInt("sessionStartChargeCounterMah", 0);
        lastCharging = prefs.getBoolean("lastCharging", false);
        chargeAlarm = prefs.getBoolean("chargeAlarm", true);
        chargeLimit = loadChargeLimit();
        benchmarkActive = prefs.getBoolean("benchmarkActive", false);
        overlayEnabled = prefs.getBoolean("overlayEnabled", false) && (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(getContext()));
        historyDays = prefs.getInt("historyDays", 7) == 30 ? 30 : 7;
        light = prefs.getBoolean("lightTheme", false);
        amoled = prefs.getBoolean("amoledTheme", false);
    }

    private int loadChargeLimit() {
        int stored = prefs.getInt("chargeLimit", BatteryChargeLimit.DEFAULT);
        int normalized = BatteryChargeLimit.normalize(stored);
        if (stored != normalized) prefs.edit().putInt("chargeLimit", normalized).apply();
        return normalized;
    }

    /** Loads history while dropping legacy cable/status blips without a signal. */
    private void loadSessions(String savedSessions) {
        if (savedSessions == null || savedSessions.isEmpty()) return;
        String cleaned = BatterySessionRules.normalizeSerialized(savedSessions);
        for (String value : cleaned.split("\\|")) {
            if (value.isEmpty()) continue;
            sessions.add(value);
        }
        if (!savedSessions.equals(cleaned)) prefs.edit().putString("sessions", cleaned).apply();
    }

    private boolean isInvalidSession(String value) {
        return !BatterySessionRules.isValid(value);
    }

    private void saveSample() {
        if (level < 0) return;
        long now = System.currentTimeMillis();
        long lastSample = prefs.getLong("lastSample", 0L);
        if (BatteryTimelineRules.isRollback(lastSample, now)) lastSample = 0L;
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

    long samplingIntervalMs() {
        int minutes = prefs.getInt("samplingIntervalMin", 15);
        if (minutes != 5 && minutes != 15 && minutes != 30 && minutes != 60) minutes = 15;
        return minutes * 60L * 1000L;
    }

    private int longHistoryRetentionSamples() {
        return Math.max(1, (int) Math.ceil(30L * 24L * 60L * 60L * 1000L / (double) samplingIntervalMs()));
    }

    private String formatDuration(long minutes) {
        return minutes >= 60 ? (minutes / 60) + " Std. " + (minutes % 60) + " Min." : minutes + " Min.";
    }

    private int healthPercent() {
        // Keep a final guard at the last consumer as well as in the reader.
        // This protects every Canvas and accessibility surface if a future
        // source reader or restored object ever bypasses the normal path.
        return BatteryHealth.displayPercent(healthReading.percent);
    }

    private int healthMeasurementMah() {
        return healthReading.capacityMah;
    }

    private String healthDisplay() { return healthReading.percent > 0 ? String.valueOf(healthReading.percent) : "—"; }

    private String temperatureDisplay() { return temperature > 0f ? String.format(Locale.US, "%.1f", temperature) : "—"; }

    private String voltageDisplay() { return voltage > 0f ? String.format(Locale.US, "%.2f", voltage) : "—"; }

    private String mahDisplay(int value) {
        return String.format(Locale.GERMANY, "%,d mAh", value);
    }

    private String designCapacityDisplay() {
        int design = designCapacityMah();
        return design > 0 ? mahDisplay(design) : "Nicht verfügbar";
    }

    private String liveCurrentDisplay() {
        if (currentMa <= 0) return "—";
        return (charging ? "+" : "−") + currentMa + " mA";
    }

    private String livePowerDisplay() {
        int power = BatteryPower.milliWatts(currentMa, Math.round(voltage * 1000f));
        return BatteryPower.label(power);
    }

    private String liveCurrentSubLabel(boolean compact) {
        String power = livePowerDisplay();
        return "—".equals(power) ? (charging ? "Ladestrom live" : "Entladestrom live")
                : (compact ? power + " Akku" : (charging ? "Ladestrom live · " : "Entladestrom live · ") + power + " Akku");
    }

    private String chargerTypeDisplay() {
        String source = charging ? BatteryPlugType.label(plugged) : "Nicht verbunden";
        if (charging && BatteryChargingState.isSpecial(chargingStatus)) {
            source += " · " + BatteryChargingState.label(chargingStatus);
        }
        if (charging && kernelChargeType.isAvailable()) {
            source += " · Algorithmus " + kernelChargeType.label();
        }
        return kernelChargeBehaviour.isAvailable()
                ? source + " · Verhalten " + kernelChargeBehaviour.label() : source;
    }

    private int designCapacityMah() { return BatteryCapacity.designCapacityMah(getContext()); }

    private String designCapacitySource() {
        if (BatteryCapacity.hasManualOverride(getContext())) return "Manuell festgelegt";
        return BatteryCapacity.hasAutomaticValue(getContext())
                ? BatteryCapacity.automaticSource(getContext())
                : "Nicht verfügbar · Nennkapazität manuell festlegen";
    }

    String technologyForExport() {
        return technology;
    }

    JSONObject chargerCapabilityForExport() {
        JSONObject value = new JSONObject();
        try {
            value.put("maxCurrentMa", chargerCapability.maxCurrentMa);
            value.put("maxVoltageMv", chargerCapability.maxVoltageMv);
            value.put("maxPowerMilliwatts", chargerCapability.maxPowerMilliwatts);
            value.put("source", chargerCapability.source);
        } catch (Exception ignored) { }
        return value;
    }

    JSONObject internalResistanceForExport() {
        JSONObject value = new JSONObject();
        try {
            value.put("microOhms", internalResistance.microOhms);
            value.put("milliOhms", internalResistance.milliOhms);
            value.put("source", internalResistance.source);
            value.put("interpretation", "Dynamic ESR; varies with state of charge and temperature");
        } catch (Exception ignored) { }
        return value;
    }

    JSONObject chargeTypeForExport() {
        JSONObject value = new JSONObject();
        try {
            value.put("type", kernelChargeType.type);
            value.put("label", kernelChargeType.isAvailable() ? kernelChargeType.label() : "");
            value.put("source", kernelChargeType.source);
        } catch (Exception ignored) { }
        return value;
    }

    JSONObject chargeBehaviourForExport() {
        JSONObject value = new JSONObject();
        try {
            value.put("behaviour", kernelChargeBehaviour.behaviour);
            value.put("label", kernelChargeBehaviour.isAvailable() ? kernelChargeBehaviour.label() : "");
            value.put("source", kernelChargeBehaviour.source);
            value.put("readOnly", true);
        } catch (Exception ignored) { }
        return value;
    }

    JSONObject chargeControlForExport() {
        JSONObject value = new JSONObject();
        try {
            value.put("startThresholdPercent", oemChargeStartPercent);
            value.put("endThresholdPercent", oemChargeLimitPercent);
            value.put("label", oemChargeControl.label());
            value.put("source", oemChargeControl.source);
            value.put("readOnly", true);
        } catch (Exception ignored) { }
        return value;
    }

    JSONObject manufactureDateForExport() {
        JSONObject value = new JSONObject();
        try {
            value.put("date", manufactureDate.isAvailable() ? manufactureDate.label() : "");
            value.put("source", manufactureDate.source);
            value.put("readOnly", true);
        } catch (Exception ignored) { }
        return value;
    }

    JSONObject remainingEnergyForExport() {
        JSONObject value = new JSONObject();
        try {
            value.put("nanoWattHours", remainingEnergyNanoWattHours);
            value.put("wattHours", BatteryEnergy.wattHours(remainingEnergyNanoWattHours));
            value.put("label", BatteryEnergy.label(remainingEnergyNanoWattHours));
            value.put("source", remainingEnergyNanoWattHours > 0L
                    ? "Android BatteryManager ENERGY_COUNTER" : "");
            value.put("interpretation", "Remaining battery energy, not charge counter");
        } catch (Exception ignored) { }
        return value;
    }

    JSONObject capacityErrorMarginForExport() {
        JSONObject value = new JSONObject();
        try {
            value.put("percent", capacityErrorMargin.percent);
            value.put("source", capacityErrorMargin.source);
            value.put("interpretation", "Fuel-gauge capacity uncertainty, not battery wear");
        } catch (Exception ignored) { }
        return value;
    }

    private String internalResistanceDisplay() {
        return internalResistance.isAvailable() ? internalResistance.label() : "—";
    }

    private int estimatedCapacityMah() {
        return healthReading.capacityMah > 0
                ? (designCapacityMah() > 0
                        ? Math.min(healthReading.capacityMah, designCapacityMah())
                        : healthReading.capacityMah) : 0;
    }

    private String healthMeasurementSource() {
        return healthReading.source.isEmpty() ? "keiner Messung" : healthReading.source;
    }

    private String healthMeasurementSourceLabel() {
        return BatteryHealth.displaySourceLabel(healthReading.source);
    }

    /**
     * Capacity used for time/rate calculations. A measured health estimate is
     * preferred, while the detected factory capacity keeps live projections
     * useful before the first health sample exists.
     */
    private int calculationCapacityMah() {
        int measured = estimatedCapacityMah();
        return measured > 0 ? measured : Math.max(0, designCapacityMah());
    }

    /** Uses the same guarded benchmark action for touch and accessibility. */
    private void toggleBenchmark() {
        if (benchmarkActive) {
            benchmarkActive = false;
            prefs.edit().putBoolean("benchmarkActive", false)
                    .remove("benchmarkStartLevel").remove("benchmarkStartCounterMah")
                    .remove("benchmarkChargeLastCounterMah").remove("benchmarkChargeAddedMah")
                    .remove("benchmarkChargeStatsBaselineMah").apply();
        } else if (charging || level > 25) {
            Toast.makeText(getContext(), "Starte den Benchmark getrennt vom Ladegerät unter 25 %.", Toast.LENGTH_LONG).show();
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
        updateAccessibilitySummary();
        invalidate();
    }

    private void editDesignCapacity() {
        EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(designCapacityMah()));
        input.setSelectAllOnFocus(true);
        new AlertDialog.Builder(getContext())
                .setTitle("Nennkapazität")
                .setMessage("Gib die werkseitige Kapazität in mAh ein. Mit 0 wird automatisch der von Android gemeldete Wert verwendet.")
                .setView(input)
                .setNegativeButton("Abbrechen", null)
                .setPositiveButton("Speichern", (dialog, which) -> {
                    try {
                        int capacity = Integer.parseInt(input.getText().toString().trim());
                        if (capacity == 0) {
                            prefs.edit().remove("designCapacityMah").apply();
                            refreshHealthReading();
                            invalidate();
                        } else if (capacity >= 500 && capacity <= 30000) {
                            prefs.edit().putInt("designCapacityMah", capacity).apply();
                            refreshHealthReading();
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
        if (!charging || level < 0) return "—";
        if (level >= 99) return "Voll";
        long systemMinutes = systemChargeTimeRemainingMinutes();
        if (systemMinutes > 0L) return formatDuration(systemMinutes);
        long localMinutes = BatteryTimeEstimate.minutesToTarget(level, 100,
                calculationCapacityMah(), averageChargeRateMahPerHour(), currentMa);
        return localMinutes > 0L ? formatDuration(localMinutes) : "—";
    }

    /**
     * Android's vendor-backed charge-time estimate is often more accurate
     * than a single instantaneous current reading. It is optional and may be
     * unavailable on devices that do not expose a charging estimate.
     */
    private long systemChargeTimeRemainingMinutes() {
        if (!charging || level < 0 || level >= 99) return 0L;
        long androidMinutes = androidChargeTimeRemainingMinutes();
        if (androidMinutes > 0L) return androidMinutes;
        return BatteryFuelGaugeTime.readMinutes(true);
    }

    private long androidChargeTimeRemainingMinutes() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return 0L;
        BatteryManager manager = (BatteryManager) getContext().getSystemService(Context.BATTERY_SERVICE);
        if (manager != null) {
            long remainingMs = manager.computeChargeTimeRemaining();
            if (remainingMs > 0L && remainingMs <= 7L * 24L * 60L * 60L * 1000L) {
                return Math.max(1L, Math.round(remainingMs / 60000f));
            }
        }
        return 0L;
    }

    private String chargeTimeEstimateLabel() {
        if (androidChargeTimeRemainingMinutes() > 0L) return "Android-Systemschätzung";
        if (BatteryFuelGaugeTime.readMinutes(true) > 0L) return "Fuel-Gauge-Schätzung";
        return averageChargeRateMahPerHour() > 0f ? "lokale 7-Tage-Schätzung" : "Momentanschätzung";
    }

    private String timeToLimit() {
        if (!charging || level < 0) return "—";
        if (level >= chargeLimit) return "Erreicht";
        long localMinutes = BatteryTimeEstimate.minutesToTarget(level, chargeLimit,
                calculationCapacityMah(), averageChargeRateMahPerHour(), currentMa);
        return localMinutes > 0L ? formatDuration(localMinutes) : "—";
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
        for (String row : BatteryExportRules.validTelemetryRows(saved)) {
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
        if (level < 0) return "—";
        if (chargeLimit <= level) return "Erreicht";
        float score = 0f;
        for (int percent = Math.max(0, level); percent < chargeLimit; percent++) {
            float stress = percent < 70 ? .70f
                    : percent < 85 ? 1.0f + (percent - 70) * .04f
                    : 1.60f + (percent - 85) * .10f;
            score += stress;
        }
        float average = score / Math.max(1, chargeLimit - Math.max(0, level));
        String label = average < .95f ? "Niedrig" : average < 1.35f ? "Mittel" : "Hoch";
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
            for (String row : BatteryExportRules.validTelemetryRows(saved)) {
                String[] parts = row.split(",", 11);
                if (parts.length < 8) continue;
                try {
                    long timestamp = Long.parseLong(parts[0]);
                    if (timestamp < windowStart) continue;
                    int sampleLevel = BatteryLevel.normalizePercent(Integer.parseInt(parts[1]));
                    if (sampleLevel < 0) continue;
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
        float consumed = BatteryPercentage.normalizePhase(prefs.getFloat(percentKey, 0f));
        long duration = prefs.getLong(durationKey, 0L);
        float rate = consumed > 0f && duration >= 5L * 60L * 1000L ? consumed * 3600000f / duration : 0f;
        return Float.isFinite(rate) && rate > 0f ? rate : 0f;
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

    private long currentDischargeDurationMs() {
        if (charging) return 0L;
        return Math.max(0L, prefs.getLong("dischargeScreenOnMs", 0L)
                + prefs.getLong("dischargeScreenOffMs", 0L));
    }

    private float currentDischargeRate() {
        if (charging) return 0f;
        float observedPercent = BatteryPercentage.normalizePhase(
                prefs.getFloat("dischargeScreenOnPercent", 0f))
                + BatteryPercentage.normalizePhase(
                prefs.getFloat("dischargeScreenOffPercent", 0f));
        return BatteryRuntimeEstimate.rateFromObserved(observedPercent,
                prefs.getInt("dischargeMah", 0), calculationCapacityMah(),
                currentDischargeDurationMs());
    }

    private String averageDischargeRateDisplay() {
        float rate = mixedDischargeRate();
        return rate > 0f ? String.format(Locale.US, "%.1f%%/h", rate) : "—";
    }

    private String runtimeEstimate() {
        if (level < 0) return "—";
        if (charging) {
            float used = BatteryPercentage.normalizePhase(prefs.getFloat("lastDischargeScreenOnPercent", 0f))
                    + BatteryPercentage.normalizePhase(prefs.getFloat("lastDischargeScreenOffPercent", 0f));
            long minutes = (prefs.getLong("lastDischargeScreenOnMs", 0L) + prefs.getLong("lastDischargeScreenOffMs", 0L)) / 60000L;
            int historicalLevel = lastDischargeEndLevel();
            if (historicalLevel < 0) return "—";
            float rate = mixedDischargeRate();
            return rate > 0f ? formatDuration(Math.max(1, Math.round(historicalLevel * 60f / rate)))
                    : (used > 0f && minutes >= 5 ? formatDuration(Math.max(1, Math.round(historicalLevel * minutes / used))) : "—");
        }
        float historicalRate = mixedDischargeRate();
        float rate = BatteryRuntimeEstimate.blendRate(historicalRate,
                currentDischargeRate(), currentDischargeDurationMs());
        if (rate > 0f) return formatDuration(Math.max(1, Math.round(level * 60f / rate)));
        if (calculationCapacityMah() <= 0) return "—";
        String systemPrediction = systemDischargePrediction();
        if (!"—".equals(systemPrediction)) return systemPrediction;
        if (currentMa < 50) return "—";
        int availableMah = Math.round(calculationCapacityMah() * level / 100f);
        return formatDuration(Math.max(1, Math.round(availableMah * 60f / currentMa)));
    }

    /**
     * Uses Android's own discharge model only when local history cannot provide
     * a personalized estimate. The system may return null or an unbounded
     * value, both of which are treated as unavailable instead of displayed as
     * false precision.
     */
    private String systemDischargePrediction() {
        if (charging) return "—";
        long fuelGaugeMinutes = BatteryFuelGaugeTime.readMinutes(false);
        if (fuelGaugeMinutes > 0L) return formatDuration(fuelGaugeMinutes);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return "—";
        PowerManager power = (PowerManager) getContext().getSystemService(Context.POWER_SERVICE);
        if (power == null) return "—";
        try {
            java.time.Duration prediction = power.getBatteryDischargePrediction();
            if (prediction == null) return "—";
            long minutes = prediction.toMinutes();
            if (minutes < 1L || minutes > 7L * 24L * 60L) return "—";
            return formatDuration(minutes);
        } catch (RuntimeException ignored) {
            return "—";
        }
    }

    private String runtimeEstimateSource() {
        if (charging) return "Basierend auf letzter Entladephase";
        float historicalRate = mixedDischargeRate();
        float currentRate = currentDischargeRate();
        if (currentRate > 0f && historicalRate > 0f) {
            return "Aktuelle Sitzung + lokale 7-Tage-Nutzung";
        }
        if (currentRate > 0f) return "Aktuelle Entladephase";
        if (historicalRate > 0f) return "Basierend auf lokaler 7-Tage-Nutzung";
        if (BatteryFuelGaugeTime.readMinutes(false) > 0L) return "Fuel-Gauge-Schätzung";
        if (!"—".equals(systemDischargePrediction())) return "Android-Systemschätzung";
        if (currentMa >= 50 && calculationCapacityMah() > 0) return "Momentanschätzung";
        return "Keine ausreichenden Daten";
    }

    private String drainRate() {
        int capacity = calculationCapacityMah();
        if (charging || currentMa < 50 || capacity <= 0) return "—";
        return String.format(Locale.US, "%.1f%% / Std.", currentMa * 100f / capacity);
    }

    private String dischargeSpeed(boolean screenOn) {
        String percentKey = screenOn ? "dischargeScreenOnPercent" : "dischargeScreenOffPercent";
        String durationKey = screenOn ? "dischargeScreenOnMs" : "dischargeScreenOffMs";
        if (charging) {
            percentKey = "last" + Character.toUpperCase(percentKey.charAt(0)) + percentKey.substring(1);
            durationKey = "last" + Character.toUpperCase(durationKey.charAt(0)) + durationKey.substring(1);
        }
        float percent = BatteryPercentage.normalizePhase(prefs.getFloat(percentKey, 0f));
        long minutes = prefs.getLong(durationKey, 0L) / 60000L;
        float storedRate = percent > 0f && minutes >= 5 ? percent * 60f / minutes : 0f;
        if (Float.isFinite(storedRate) && storedRate > 0f) return String.format(Locale.US, "%.1f%%/h", storedRate);
        int capacity = calculationCapacityMah();
        if (charging || currentMa < 50 || capacity <= 0) return "—";
        float liveRate = currentMa * 100f / capacity;
        return Float.isFinite(liveRate) && liveRate > 0f
                ? String.format(Locale.US, "%.1f%%/h", liveRate) : "—";
    }

    private String screenOnTime() {
        long minutes = prefs.getLong("screenOnMs", 0L) / 60000L;
        if (minutes <= 0) return "—";
        return (minutes / 60) + " Std. " + (minutes % 60) + " Min.";
    }

    private String screenOnTimeCard() {
        return BatteryDuration.compact(prefs.getLong("screenOnMs", 0L) / 60000L);
    }

    private String deepSleepTime() {
        long minutes = prefs.getLong(charging ? "lastDischargeDeepSleepMs" : "dischargeDeepSleepMs", 0L) / 60000L;
        if (minutes <= 0) return "—";
        return (minutes / 60) + " Std. " + (minutes % 60) + " Min.";
    }

    private String deepSleepPercent() {
        String deepKey = charging ? "lastDischargeDeepSleepMs" : "dischargeDeepSleepMs";
        String offKey = charging ? "lastDischargeScreenOffMs" : "dischargeScreenOffMs";
        long deepMs = prefs.getLong(deepKey, 0L);
        long offMs = prefs.getLong(offKey, 0L);
        if (deepMs <= 0L || offMs <= 0L) return "—";
        float ratio = deepMs * 100f / offMs;
        return Float.isFinite(ratio) && ratio >= 0f
                ? String.format(Locale.US, "%.0f%%", Math.min(100f, ratio)) : "—";
    }

    private int wakeupCount() {
        return prefs.getInt(charging ? "lastDischargeWakeups" : "dischargeWakeups", 0);
    }

    private String sinceFullRange() {
        if (!prefs.getBoolean("sinceFullActive", false)) return "Noch keine Ladebasis";
        int start = storedLevelForDisplay("sinceFullStartLevel", 100);
        int end = storedLevelForDisplay("sinceFullLastLevel", level);
        return (start >= 0 ? start + "%" : "—") + " → " + (end >= 0 ? end + "%" : "—");
    }

    private String sinceFullDuration() {
        if (!prefs.getBoolean("sinceFullActive", false)) return "—";
        long start = prefs.getLong("sinceFullStartAt", 0L);
        if (start <= 0L) return "—";
        return formatDuration(Math.max(1L, (System.currentTimeMillis() - start) / 60000L));
    }

    private String sinceFullUsageSummary() {
        if (!prefs.getBoolean("sinceFullActive", false)) return "Noch keine Ladebasis";
        float consumedPercent = BatteryPercentage.normalizeCumulative(prefs.getFloat("sinceFullPercent", 0f));
        String range = consumedPercent > 0f
                ? String.format(Locale.US, "%.0f%% verbraucht", consumedPercent) : "Noch kein Verbrauch";
        int mah = prefs.getInt("sinceFullMah", 0);
        return range + " · " + sinceFullDuration() + " · " + (mah > 0 ? mah + " mAh" : "—");
    }

    private String sinceFullAnchorLabel() {
        if (!prefs.getBoolean("sinceFullActive", false)) return "Seit Ladebasis";
        return BatteryChargeAnchor.UNPLUGGED.equals(prefs.getString("chargeAnchorType", ""))
                ? "Seit Abstecken" : "Seit voller Ladung";
    }

    private int dischargeMah() { return prefs.getInt(charging ? "lastDischargeMah" : "dischargeMah", 0); }

    private int lastChargeEnergyMah() { return prefs.getInt("lastChargeEnergyMah", 0); }

    private int totalChargedMah() { return Math.max(0, prefs.getInt("totalChargedMah", 0)); }

    private String healthEstimateStatus() {
        if (charging) {
            int change = chargeEndLevelForDisplay() - chargeStartLevelForDisplay();
            int energy = chargeEnergyForDisplay();
            if (change < 5) return "Mindestens 5 % Akkustand nötig";
            if (energy <= 0) return "Warte auf Energiedaten";
            return "Wird nach dem Ladevorgang einbezogen";
        }
        String reason = prefs.getString("lastChargeHealthReason", "");
        return reason.isEmpty() ? "Längere Ladevorgänge verbessern die Genauigkeit" : reason;
    }

    private int chargeStartLevelForDisplay() {
        return charging ? storedLevelForDisplay("monitorSessionStartLevel", sessionStartLevel)
                : storedLevelForDisplay("lastChargeStartLevel", 0);
    }

    private int chargeEndLevelForDisplay() {
        return charging ? BatteryLevel.normalizePercent(level)
                : storedLevelForDisplay("lastChargeEndLevel", 0);
    }

    private String chargeChangeForDisplay() {
        int start = chargeStartLevelForDisplay();
        int end = chargeEndLevelForDisplay();
        int change = start >= 0 && end >= 0 ? end - start : 0;
        return change > 0 && change <= 100 ? "+" + change + "%" : "—";
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
        return start > 0L ? new SimpleDateFormat("dd.MM. HH:mm", Locale.GERMANY).format(new Date(start)) : "—";
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
        int start = storedLevelForDisplay("lastChargeStartLevel", 0);
        int end = storedLevelForDisplay("lastChargeEndLevel", 0);
        return start >= 0 && end >= 0 ? start + "% → " + end + "%" : "—";
    }

    private int storedLevelForDisplay(String key, int fallback) {
        int value = BatteryLevel.normalizePercent(prefs.getInt(key, fallback));
        return value >= 0 ? value : BatteryLevel.normalizePercent(fallback);
    }

    private int lastDischargeEndLevel() {
        return BatteryLevel.normalizePercent(prefs.getInt("lastDischargeEndLevel", -1));
    }

    private String lastChargeDuration() {
        long minutes = prefs.getLong("lastChargeDurationMin", 0L);
        return minutes > 0L ? formatDuration(minutes) : "—";
    }

    private String dischargePercent(boolean screenOn) {
        String key = screenOn ? "dischargeScreenOnPercent" : "dischargeScreenOffPercent";
        if (charging) key = "last" + Character.toUpperCase(key.charAt(0)) + key.substring(1);
        float value = BatteryPercentage.normalizePhase(prefs.getFloat(key, 0f));
        return value > 0f ? String.format(Locale.US, "%.0f%%", value) : "—";
    }

    private String dischargeDuration(boolean screenOn) {
        String key = screenOn ? "dischargeScreenOnMs" : "dischargeScreenOffMs";
        if (charging) key = "last" + Character.toUpperCase(key.charAt(0)) + key.substring(1);
        long minutes = prefs.getLong(key, 0L) / 60000L;
        return minutes <= 0 ? "—" : formatDuration(minutes);
    }

    private String dischargeDurationCompact(boolean screenOn) {
        String key = screenOn ? "dischargeScreenOnMs" : "dischargeScreenOffMs";
        if (charging) key = "last" + Character.toUpperCase(key.charAt(0)) + key.substring(1);
        return BatteryDuration.compact(prefs.getLong(key, 0L) / 60000L);
    }

    private String dischargeRuntime(boolean screenOn) {
        if (level < 0) return "—";
        String percentKey = screenOn ? "dischargeScreenOnPercent" : "dischargeScreenOffPercent";
        String durationKey = screenOn ? "dischargeScreenOnMs" : "dischargeScreenOffMs";
        if (charging) {
            percentKey = "last" + Character.toUpperCase(percentKey.charAt(0)) + percentKey.substring(1);
            durationKey = "last" + Character.toUpperCase(durationKey.charAt(0)) + durationKey.substring(1);
        }
        float percent = BatteryPercentage.normalizePhase(prefs.getFloat(percentKey, 0f));
        long minutes = prefs.getLong(durationKey, 0L) / 60000L;
        int referenceLevel = charging ? lastDischargeEndLevel() : level;
        if (referenceLevel < 0) return "—";
        if (percent > 0f && minutes >= 5) return formatDuration(Math.max(1, Math.round(referenceLevel * minutes / percent)));
        if (charging) return "—";
        float rate = averageDischargeRate(screenOn);
        if (rate > 0f) return formatDuration(Math.max(1, Math.round(referenceLevel * 60f / rate)));
        if (currentMa < 50) return "—";
        int modeCurrent = screenOn ? currentMa : Math.max(50, Math.round(currentMa * .35f));
        return formatDuration(Math.max(1, Math.round(calculationCapacityMah() * referenceLevel / 100f * 60f / modeCurrent)));
    }

    private int chargeSpeedMahPerHour(boolean screenOn) {
        String mahKey = screenOn ? "chargeScreenOnMah" : "chargeScreenOffMah";
        String durationKey = screenOn ? "chargeScreenOnMs" : "chargeScreenOffMs";
        if (!charging) {
            mahKey = "last" + Character.toUpperCase(mahKey.charAt(0)) + mahKey.substring(1);
            durationKey = "last" + Character.toUpperCase(durationKey.charAt(0)) + durationKey.substring(1);
        }
        int mah = prefs.getInt(mahKey, 0);
        long minutes = prefs.getLong(durationKey, 0L) / 60000L;
        if (mah <= 0 || minutes < 5) return 0;
        return Math.round(mah * 60f / minutes);
    }

    private float chargeSpeedPercentPerHour(boolean screenOn) {
        String percentKey = screenOn ? "chargeScreenOnPercent" : "chargeScreenOffPercent";
        String durationKey = screenOn ? "chargeScreenOnMs" : "chargeScreenOffMs";
        if (!charging) {
            percentKey = "last" + Character.toUpperCase(percentKey.charAt(0)) + percentKey.substring(1);
            durationKey = "last" + Character.toUpperCase(durationKey.charAt(0)) + durationKey.substring(1);
        }
        float percent = BatteryPercentage.normalizePhase(prefs.getFloat(percentKey, 0f));
        long minutes = prefs.getLong(durationKey, 0L) / 60000L;
        float rate = percent > 0f && minutes >= 5 ? percent * 60f / minutes : 0f;
        return Float.isFinite(rate) && rate > 0f ? rate : 0f;
    }

    private String chargeSpeed(boolean screenOn) {
        int mahPerHour = chargeSpeedMahPerHour(screenOn);
        if (mahPerHour <= 0) return "—";
        float percentPerHour = chargeSpeedPercentPerHour(screenOn);
        if (percentPerHour > 0f) return String.format(Locale.US, "%d mA · %.1f%%/h", mahPerHour, percentPerHour);
        return String.format(Locale.US, "%d mA", mahPerHour);
    }

    private String compactChargeSpeed(boolean screenOn) {
        int mahPerHour = chargeSpeedMahPerHour(screenOn);
        if (mahPerHour <= 0) return "—";
        return String.format(Locale.US, "%d mA", mahPerHour);
    }

    private String compactChargeSpeedRate(boolean screenOn) {
        float percentPerHour = chargeSpeedPercentPerHour(screenOn);
        return percentPerHour > 0f ? String.format(Locale.US, "%.1f%%/h", percentPerHour) : "Rate nicht verfügbar";
    }

    private int chargeCycles() {
        int reported = prefs.getInt("systemCycleCount", -1);
        return reported >= 0 ? reported : prefs.getInt("chargeCycles", 0);
    }

    private String chargeCyclesDisplay() {
        int reported = prefs.getInt("systemCycleCount", -1);
        if (reported >= 0) return String.valueOf(reported);
        int locallyObserved = prefs.getInt("chargeCycles", 0);
        int estimated = prefs.getInt("estimatedCycleCount", 0);
        if (estimated > 0) return "~" + estimated;
        return locallyObserved > 0 ? String.valueOf(locallyObserved) : "—";
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

    ArrayList<BatteryCycleHistory.Point> cycleHistoryForExport() {
        return new ArrayList<>(cycleHistory);
    }

    private String cycleHistorySourceDisplay() {
        if (cycleHistory.isEmpty()) return "Quelle nicht verfügbar";
        BatteryCycleHistory.Point latest = cycleHistory.get(cycleHistory.size() - 1);
        return latest.isReported() ? "Quelle: Android/BMS-Zähler" : "Quelle: lokale EFC-Schätzung";
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
        String[] options = {"Dunkles Design", "AMOLED-Schwarz", "Helles Design", "Benachrichtigungen", "Temperaturwarnung", "Tiefstandwarnung", "Overlay-Berechtigung", "Daten & Datenschutz", "Sicherung & Wiederherstellung", "Hintergrundüberwachung", "Datenerfassung", "Nach Updates suchen", "Kurzanleitung", "Gesundheitsbasis zurücksetzen", "Lokale Daten löschen"};
        LinearLayout titleBar = new LinearLayout(getContext());
        titleBar.setOrientation(LinearLayout.HORIZONTAL);
        titleBar.setGravity(Gravity.CENTER_VERTICAL);
        int titlePadding = Math.round(20 * density);
        titleBar.setPadding(titlePadding, Math.round(8 * density), Math.round(8 * density), Math.round(4 * density));

        TextView title = new TextView(getContext());
        title.setText("Einstellungen");
        title.setTextSize(20);
        title.setTextColor(light ? Color.rgb(25, 32, 28) : Color.WHITE);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        titleBar.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView close = new TextView(getContext());
        close.setText("×");
        close.setTextSize(30);
        close.setGravity(Gravity.CENTER);
        close.setTextColor(light ? Color.rgb(25, 32, 28) : Color.WHITE);
        close.setContentDescription("Einstellungen schließen");
        int closeSize = Math.round(48 * density);
        titleBar.addView(close, new LinearLayout.LayoutParams(closeSize, closeSize));

        AlertDialog dialog = new AlertDialog.Builder(getContext()).setCustomTitle(titleBar).setItems(options, (itemDialog, which) -> {
            if (which == 0) { light = false; amoled = false; }
            else if (which == 1) { light = false; amoled = true; }
            else if (which == 2) { light = true; amoled = false; }
            else if (which == 3) {
                try {
                    Intent notificationSettings = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, getContext().getPackageName());
                    getContext().startActivity(notificationSettings);
                } catch (Exception ignored) { }
            } else if (which == 4) {
                showTemperatureAlarmSettings();
            } else if (which == 5) {
                showDischargeAlarmSettings();
            } else if (which == 6) {
                try { getContext().startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getContext().getPackageName()))); } catch (Exception ignored) { }
            } else if (which == 7) {
                showDataPrivacy();
            } else if (which == 8) {
                showBackupRestore();
            } else if (which == 9) {
                requestBackgroundMonitoring();
            } else if (which == 10) {
                showDataCollection();
            } else if (which == 11) {
                UpdateChecker.checkNow((Activity) getContext());
            } else if (which == 12) {
                showTutorial(true);
            } else if (which == 13) {
                confirmResetHealthBaseline();
            } else {
                confirmDeleteData();
            }
            prefs.edit().putBoolean("lightTheme", light).putBoolean("amoledTheme", amoled).apply();
            applySystemBarTheme();
            invalidate();
        }).create();
        close.setOnClickListener(view -> dialog.dismiss());
        dialog.setCanceledOnTouchOutside(true);
        dialog.setCancelable(true);
        dialog.setOnShowListener(shown -> {
            Window window = dialog.getWindow();
            if (window == null) return;
            window.addFlags(WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH);
            window.getDecorView().setOnTouchListener((view, event) -> {
                if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                    dialog.dismiss();
                    return true;
                }
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    float edge = 16 * density;
                    if (event.getX() < edge || event.getX() > view.getWidth() - edge
                            || event.getY() < edge || event.getY() > view.getHeight() - edge) {
                        dialog.dismiss();
                        return true;
                    }
                }
                return false;
            });
        });
        dialog.show();
    }

    private void showTemperatureAlarmSettings() {
        final int[] thresholds = {0, 400, 450, 500, 550};
        final String[] labels = {"Aus", "Ab 40 °C", "Ab 45 °C (empfohlen)", "Ab 50 °C", "Ab 55 °C"};
        int current = prefs.getBoolean("temperatureAlarm", true)
                ? BatteryTemperatureAlarm.normalizeThreshold(prefs.getInt("temperatureAlarmThresholdTenths", BatteryTemperatureAlarm.DEFAULT_THRESHOLD_TENTHS))
                : 0;
        int selected = 0;
        for (int i = 0; i < thresholds.length; i++) if (thresholds[i] == current) selected = i;
        final int[] choice = {selected};
        new AlertDialog.Builder(getContext())
                .setTitle("Temperaturwarnung · Rücksetzung 3 °C darunter")
                .setSingleChoiceItems(labels, selected, (dialog, which) -> choice[0] = which)
                .setNegativeButton("Abbrechen", null)
                .setPositiveButton("Speichern", (dialog, which) -> {
                    boolean enabled = thresholds[choice[0]] > 0;
                    int threshold = enabled ? thresholds[choice[0]] : BatteryTemperatureAlarm.DEFAULT_THRESHOLD_TENTHS;
                    prefs.edit().putBoolean("temperatureAlarm", enabled)
                            .putInt("temperatureAlarmThresholdTenths", threshold)
                            .remove("temperatureAlarmSent").apply();
                    Toast.makeText(getContext(), enabled ? labels[choice[0]] : "Temperaturwarnung ausgeschaltet", Toast.LENGTH_LONG).show();
                }).show();
    }

    private void showDischargeAlarmSettings() {
        final int[] thresholds = {0, 10, 15, 20, 25, 30};
        final String[] labels = {"Aus", "Bei 10 % oder weniger", "Bei 15 % oder weniger (empfohlen)", "Bei 20 % oder weniger", "Bei 25 % oder weniger", "Bei 30 % oder weniger"};
        boolean enabled = prefs.getBoolean("dischargeAlarm", true);
        int current = enabled
                ? BatteryDischargeAlarm.normalizeThreshold(prefs.getInt("dischargeAlarmThreshold", BatteryDischargeAlarm.DEFAULT_THRESHOLD))
                : 0;
        int selected = 0;
        for (int i = 0; i < thresholds.length; i++) if (thresholds[i] == current) selected = i;
        final int[] choice = {selected};
        new AlertDialog.Builder(getContext())
                .setTitle("Tiefstandwarnung · Rücksetzung mit 3 % Abstand")
                .setSingleChoiceItems(labels, selected, (dialog, which) -> choice[0] = which)
                .setNegativeButton("Abbrechen", null)
                .setPositiveButton("Speichern", (dialog, which) -> {
                    boolean alarmEnabled = thresholds[choice[0]] > 0;
                    int threshold = alarmEnabled
                            ? BatteryDischargeAlarm.normalizeThreshold(thresholds[choice[0]])
                            : BatteryDischargeAlarm.DEFAULT_THRESHOLD;
                    prefs.edit().putBoolean("dischargeAlarm", alarmEnabled)
                            .putInt("dischargeAlarmThreshold", threshold)
                            .remove("dischargeAlarmSent").remove("dischargeAlarmLastLevel").apply();
                    Toast.makeText(getContext(), alarmEnabled ? labels[choice[0]] : "Tiefstandwarnung ausgeschaltet", Toast.LENGTH_LONG).show();
                }).show();
    }

    private void showDataCollection() {
        final int[] intervals = {5, 15, 30, 60};
        final String[] labels = {"Alle 5 Minuten", "Alle 15 Minuten (empfohlen)", "Alle 30 Minuten", "Alle 60 Minuten"};
        int current = prefs.getInt("samplingIntervalMin", 15);
        int selected = 1;
        for (int i = 0; i < intervals.length; i++) if (intervals[i] == current) selected = i;
        final int[] choice = {selected};
        new AlertDialog.Builder(getContext())
                .setTitle("Datenerfassung · nur lokal")
                .setSingleChoiceItems(labels, selected, (dialog, which) -> choice[0] = which)
                .setNegativeButton("Abbrechen", null)
                .setPositiveButton("Speichern", (dialog, which) -> {
                    prefs.edit().putInt("samplingIntervalMin", intervals[choice[0]]).apply();
                    ((MainActivity) getContext()).restartMonitorService();
                    invalidate();
                }).show();
    }

    private void requestBackgroundMonitoring() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Toast.makeText(getContext(), "Hintergrundüberwachung ist ab Android 6 verfügbar.", Toast.LENGTH_LONG).show();
            return;
        }
        PowerManager power = (PowerManager) getContext().getSystemService(Context.POWER_SERVICE);
        if (power != null && power.isIgnoringBatteryOptimizations(getContext().getPackageName())) {
            Toast.makeText(getContext(), "Hintergrundüberwachung ist bereits erlaubt.", Toast.LENGTH_LONG).show();
            return;
        }
        new AlertDialog.Builder(getContext())
                .setTitle("Hintergrundüberwachung")
                .setMessage("Android kann Hintergrund-Apps zum Energiesparen pausieren. Darf Ampere auch bei geschlossener App den Akkuverlauf und Ladealarme aufzeichnen?")
                .setNegativeButton("Später", null)
                .setPositiveButton("Systemeinstellung öffnen", (dialog, which) -> {
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
        String[] exportChoices = {"CSV exportieren", "Forschungs-JSON", "Diagnosebericht"};
        new AlertDialog.Builder(getContext())
                .setTitle("Daten & Datenschutz")
                .setMessage("Ampere erfasst Messwerte lokal für Verlauf und Analyse: Zeit, Akkustand, Ladezustand, Strom, Temperatur, Spannung und Bildschirmstatus. Wenn du den Nutzungszugriff erlaubst, wird zusätzlich die aktive Vordergrund-App lokal gespeichert, um ihren Anteil am Verbrauch zu schätzen.\n\nEs werden keine Messwerte, Kontokennungen, Standortdaten oder Listen installierter Apps an einen Ampere-Server gesendet. Android-Sicherungen können Verlauf, Einstellungen und lokale Telemetrie über einen geeigneten verschlüsselten Sicherungsdienst enthalten; Gerät und Android bestimmen, ob und wann gesichert wird. Die Update-Prüfung ruft nur die konfigurierte Versionsdatei ab.\n\nCSV ist eine flache Tabelle. Der Forschungs-Export ist strukturiertes JSON und enthält Gerätemodell und Android-Version, aber keine Seriennummer oder Werbe-ID. Exporte starten erst, wenn du sie auswählst.")
                .setItems(exportChoices, (dialog, which) -> {
                    MainActivity activity = (MainActivity) getContext();
                    if (which == 0) activity.createCsvExport();
                    else if (which == 1) activity.createResearchExport();
                    else activity.createDiagnosticExport();
                })
                .setNegativeButton("Schließen", null)
                .show();
    }

    private void confirmDeleteData() {
        new AlertDialog.Builder(getContext())
                .setTitle("Lokale Daten löschen?")
                .setMessage("Damit werden lokaler Verlauf, Sitzungen, Gesundheitsmessungen, Telemetrie und Einstellungen gelöscht. Eine ältere Android-Sicherung kann bestehen bleiben, bis sie ersetzt wird.")
                .setNegativeButton("Abbrechen", null)
                .setPositiveButton("Löschen", (dialog, which) -> {
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
                .setTitle("Gesundheitsbasis zurücksetzen?")
                .setMessage("Damit beginnen Akku-Gesundheit, Benchmark-Berechnung und tägliche Zyklushistorie neu, zum Beispiel nach einem Akkutausch. Bestehende Sitzungen, Telemetrie, Einstellungen und Exporte bleiben erhalten.")
                .setNegativeButton("Abbrechen", null)
                .setPositiveButton("Basis zurücksetzen", (dialog, which) -> {
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
                            .remove("estimatedCycleLastCounterUah").remove("estimatedCycleFraction")
                            .remove("estimatedCycleCount").remove("cycleHistory")
                            .apply();
                    BackupManager.dataChanged(context.getPackageName());
                    reloadStoredData();
                    Intent battery = ((Activity) context).registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                    if (battery != null) readBattery(battery);
                    Toast.makeText(context, "Gesundheitsbasis zurückgesetzt; Verlauf bleibt erhalten.", Toast.LENGTH_LONG).show();
                })
                .show();
    }

    private void showBackupRestore() {
        new AlertDialog.Builder(getContext())
                .setTitle("Sicherung & Wiederherstellung")
                .setMessage("Updates behalten deine Daten automatisch. Die Überwachung fordert im Hintergrund den Android-Sicherungsdienst an. Bei einem geeigneten verschlüsselten Sicherungsdienst umfasst die automatische Android-Sicherung Verlauf, Einstellungen und lokale Telemetrie. " + backupStatus() + "\n\nErstelle vor der Deinstallation eine Sicherung und stelle sie nach der Neuinstallation wieder her. Eine aktivierte Cloud-/Gerätesicherung kann die enthaltenen Daten automatisch zurückspielen; die sichtbare Sicherung ist die zuverlässige Ausweichlösung.")
                .setPositiveButton("Sicherung erstellen", (dialog, which) -> ((MainActivity) getContext()).createBackup())
                .setNeutralButton("Sicherung wiederherstellen", (dialog, which) -> ((MainActivity) getContext()).restoreBackup())
                .setNegativeButton("Schließen", null)
                .show();
    }

    private String backupStatus() {
        long lastRequest = prefs.getLong("lastBackupRequestAt", 0L);
        if (lastRequest <= 0L) return "Noch keine automatische Sicherung angefordert.";
        String date = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMANY).format(new Date(lastRequest));
        return "Letzte automatische Sicherungsanforderung: " + date + ". Android steuert Dienst und Zeitpunkt der Sicherung.";
    }

    void startSavedOverlay() {
        if (!overlayEnabled) return;
        Intent service = new Intent(getContext(), BatteryOverlayService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) getContext().startForegroundService(service); else getContext().startService(service);
    }

    void showTutorial(boolean force) {
        if (!force && prefs.getBoolean("tutorialShown", false)) return;
        new AlertDialog.Builder(getContext())
                .setTitle("Willkommen bei Ampere")
                .setMessage("Ampere misst Akkustrom, Ladegeschwindigkeit, Verbrauch und geschätzte Kapazität lokal.\n\n1. Lass die Überwachungsbenachrichtigung für den Hintergrundverlauf aktiviert.\n2. Stelle den Ladealarm auf den Akkustand, bei dem du erinnert werden möchtest.\n3. Für eine möglichst genaue Gesundheitsbewertung starte den manuellen Benchmark unter 25 % und beende ihn über 95 %.\n\nOptional: Erlaube den Nutzungszugriff für App-Verbrauchsschätzungen und die Overlay-Berechtigung für Live-Werte über anderen Apps.")
                .setNegativeButton("Überspringen", (dialog, which) -> prefs.edit().putBoolean("tutorialShown", true).apply())
                .setPositiveButton("Kapazität festlegen", (dialog, which) -> {
                    prefs.edit().putBoolean("tutorialShown", true).apply();
                    editDesignCapacity();
                }).show();
    }

    private float u(float value) { return value * density; }
    private void fill(Canvas c, int color) { p.setShader(null); p.setStyle(Paint.Style.FILL); p.setColor(color); }
    private void stroke(Canvas c, int color, float width) { p.setShader(null); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(u(width)); p.setStrokeCap(Paint.Cap.ROUND); p.setStrokeJoin(Paint.Join.ROUND); p.setColor(color); }
    private void type(float size, int color, boolean bold) { p.setShader(null); p.setTextSize(u(size)); p.setColor(color); p.setTypeface(Typeface.create("sans", bold ? Typeface.BOLD : Typeface.NORMAL)); p.setStyle(Paint.Style.FILL); }
    private void text(Canvas c, String value, float x, float y, float size, int color, boolean bold) {
        float viewWidth = layoutWidthDp > 0f ? layoutWidthDp : getWidth() / density;
        float safeX = Math.max(8f, Math.min(x, Math.max(8f, viewWidth - 8f)));
        String fitted = fitText(value, Math.max(1f, viewWidth - safeX - 8f), size, bold);
        type(size, color, bold);
        c.drawText(fitted, u(safeX), u(y), p);
    }
    private void boundedText(Canvas c, String value, float leftX, float rightX, float y, float size, int color, boolean bold) {
        float left = Math.max(8f, leftX);
        float right = Math.max(left + 1f, rightX);
        String fitted = fitText(value, right - left, size, bold);
        type(size, color, bold);
        c.drawText(fitted, u(left), u(y), p);
    }
    private void centeredText(Canvas c, String value, float centerX, float y, float size, int color, boolean bold) {
        float viewWidth = layoutWidthDp > 0f ? layoutWidthDp : getWidth() / density;
        float halfWidth = Math.max(1f, Math.min(centerX - 8f, viewWidth - centerX - 8f));
        String fitted = fitText(value, halfWidth * 2f, size, bold);
        type(size, color, bold);
        c.drawText(fitted, u(centerX) - p.measureText(fitted) / 2f, u(y), p);
    }
    private void rightText(Canvas c, String value, float rightX, float y, float size, int color, boolean bold) {
        float viewWidth = layoutWidthDp > 0f ? layoutWidthDp : getWidth() / density;
        float safeRight = Math.max(8f, Math.min(rightX, viewWidth - 8f));
        String fitted = fitText(value, Math.max(1f, safeRight - 8f), size, bold);
        type(size, color, bold);
        c.drawText(fitted, u(safeRight) - p.measureText(fitted), u(y), p);
    }
    private void boundedRightText(Canvas c, String value, float leftX, float rightX, float y, float size, int color, boolean bold) {
        float right = Math.max(8f, rightX);
        float left = Math.max(0f, Math.min(leftX, right - 1f));
        String fitted = fitText(value, Math.max(1f, right - left), size, bold);
        type(size, color, bold);
        c.drawText(fitted, u(right) - p.measureText(fitted), u(y), p);
    }
    private void rounded(Canvas c, float l, float t, float r, float b, float radius, int color) {
        // One consistent surface language: large cards are softer than controls.
        float surfaceRadius = radius >= 12f ? 16f : radius;
        fill(c, color); rect.set(u(l), u(t), u(r), u(b)); c.drawRoundRect(rect, u(surfaceRadius), u(surfaceRadius), p);
    }
    private void line(Canvas c, float x1, float y1, float x2, float y2, int color, float width) { stroke(c, color, width); c.drawLine(u(x1), u(y1), u(x2), u(y2), p); }

    /**
     * A control surface keeps fill and outline on the very same bounds.
     * The old header drew a 44dp fill but inset its outline by 2dp, which made
     * the corners look mismatched on different densities.
     */
    private void controlSurface(Canvas c, float l, float t, float r, float b,
                                float radius, int fillColor, int borderColor) {
        fill(c, fillColor);
        rect.set(u(l), u(t), u(r), u(b));
        c.drawRoundRect(rect, u(radius), u(radius), p);
        stroke(c, borderColor, 1);
        rect.set(u(l), u(t), u(r), u(b));
        c.drawRoundRect(rect, u(radius), u(radius), p);
    }

    private int mixColor(int from, int to, float amount) {
        float t = Math.max(0f, Math.min(1f, amount));
        return Color.rgb(
                Math.round(Color.red(from) + (Color.red(to) - Color.red(from)) * t),
                Math.round(Color.green(from) + (Color.green(to) - Color.green(from)) * t),
                Math.round(Color.blue(from) + (Color.blue(to) - Color.blue(from)) * t));
    }

    private void gradientRounded(Canvas c, float l, float t, float r, float b, float radius,
                                 int topColor, int bottomColor) {
        p.setStyle(Paint.Style.FILL);
        p.setShader(new LinearGradient(0, u(t), 0, u(b), topColor, bottomColor, Shader.TileMode.CLAMP));
        rect.set(u(l), u(t), u(r), u(b));
        c.drawRoundRect(rect, u(radius), u(radius), p);
        p.setShader(null);
    }

    /**
     * Shared Energy Rail control: a restrained vertical material gradient,
     * an inset top edge and a real one-dp press-in. It gives every interactive
     * surface the same physical language without relying on glow or a pill
     * around every label.
     */
    private void smoothButton(Canvas c, float l, float t, float r, float b,
                              float radius, int baseColor, int borderColor, int accentColor,
                              boolean selected, boolean pressed) {
        float inset = pressed ? 1f : 0f;
        float left = l + inset;
        float top = t + inset;
        float right = r - inset;
        float bottom = b - inset;
        int surface = selected ? accentColor : baseColor;
        int topColor = mixColor(surface, Color.WHITE, selected ? .12f : (light ? .22f : .08f));
        int bottomColor = mixColor(surface, Color.rgb(14, 20, 17), selected ? .08f : (light ? .045f : .18f));
        if (pressed) {
            topColor = mixColor(topColor, Color.rgb(14, 20, 17), selected ? .08f : .04f);
            bottomColor = mixColor(bottomColor, Color.rgb(14, 20, 17), selected ? .10f : .06f);
        }
        gradientRounded(c, left, top, right, bottom, Math.max(7f, radius - (pressed ? 1f : 0f)), topColor, bottomColor);
        stroke(c, selected ? mixColor(accentColor, Color.rgb(37, 55, 25), .3f) : borderColor, pressed ? 1.35f : 1f);
        rect.set(u(left), u(top), u(right), u(bottom));
        c.drawRoundRect(rect, u(Math.max(7f, radius - (pressed ? 1f : 0f))), u(Math.max(7f, radius - (pressed ? 1f : 0f))), p);
        // A single hairline catches light at the top edge and replaces the
        // heavy shadow treatment common to generic dashboard buttons.
        line(c, left + radius, top + 1.2f, right - radius, top + 1.2f,
                Color.argb(light ? 120 : 80, 255, 255, 255), .7f);
        if (selected) {
            rounded(c, left + 10f, bottom - 4f, right - 10f, bottom - 2f, 1f,
                    mixColor(accentColor, Color.rgb(48, 70, 30), light ? .08f : .25f));
        }
    }

    private void drawToggleButton(Canvas c, float l, float t, float r, float b,
                                  String label, String status, boolean enabled,
                                  boolean pressed, int primary, int muted, int raised, int border) {
        smoothButton(c, l, t, r, b, 12, raised, border, lime, false, pressed);
        rounded(c, l + 9f, t + 10f, l + 13f, b - 10f, 2f, enabled ? lime : border);
        text(c, label, l + 21, t + (b - t) / 2f + 4f, 10, primary, true);
        rightText(c, status, r - 56, t + (b - t) / 2f + 4f, 9, enabled ? lime : muted, false);
        float switchLeft = r - 48f;
        float switchTop = t + (b - t - 24f) / 2f;
        int trackTop = enabled ? Color.rgb(111, 137, 60) : mixColor(border, Color.WHITE, light ? .08f : .04f);
        int trackBottom = enabled ? Color.rgb(73, 91, 40) : mixColor(border, Color.rgb(14, 20, 17), light ? .03f : .14f);
        gradientRounded(c, switchLeft, switchTop, r - 12f, switchTop + 24f, 12, trackTop, trackBottom);
        stroke(c, enabled ? Color.rgb(77, 96, 43) : border, .8f);
        rect.set(u(switchLeft), u(switchTop), u(r - 12f), u(switchTop + 24f));
        c.drawRoundRect(rect, u(12), u(12), p);
        fill(c, enabled ? lime : muted);
        c.drawCircle(u(enabled ? r - 24f : switchLeft + 12f),
                u(switchTop + 12f), u(8f), p);
        fill(c, Color.argb(light ? 135 : 85, 255, 255, 255));
        c.drawCircle(u(enabled ? r - 26f : switchLeft + 10f), u(switchTop + 9f), u(2.2f), p);
    }

    private void frame(Canvas c, float l, float t, float r, float b, int panel, int border, int accent) {
        rounded(c, l, t, r, b, 16, panel);
        stroke(c, border, 1);
        rect.set(u(l), u(t), u(r), u(b));
        c.drawRoundRect(rect, u(16), u(16), p);
        // A quiet color rail makes the information hierarchy scannable without
        // turning every card into a bright button.
        // Clip it to the card shape: a separate narrow rounded rectangle has
        // square-looking ends wherever the card itself has a large corner.
        rect.set(u(l), u(t), u(r), u(b));
        Path cardClip = new Path();
        cardClip.addRoundRect(rect, u(16), u(16), Path.Direction.CW);
        c.save();
        c.clipPath(cardClip);
        fill(c, Color.argb(150, Color.red(accent), Color.green(accent), Color.blue(accent)));
        c.drawRect(u(l), u(t), u(Math.min(r, l + 6)), u(b), p);
        c.restore();
    }

    private int pressedFill(int base, boolean pressed) {
        if (!pressed) return base;
        return Color.argb(light ? 34 : 46, Color.red(lime), Color.green(lime), Color.blue(lime));
    }

    private boolean isPressed(int region) { return pressedRegion == region; }

    private int pressedRegionAt(float x, float y, float w) {
        // Keep hit testing exactly aligned with the header's responsive action
        // geometry; this avoids a drawn control with a dead or shifted target.
        int headerAction = BatteryHeaderLayout.actionAt(x, y, w);
        if (headerAction != BatteryHeaderLayout.NONE) return headerAction;
        if (y >= 118 && y < 176 && x >= 18 && x <= w - 18) {
            float cell = (w - 36) / 5f;
            return 10 + Math.max(0, Math.min(4, (int) ((x - 18) / cell)));
        }
        if (page == 0 && y >= overviewChartTop() + 8f && y < overviewChartTop() + 60f
                && x >= contentInset(w) + contentWidth(w) - 112f) {
            float bodyX = x - contentInset(w);
            return bodyX < contentWidth(w) - 56f ? BatteryAccessibilityLayout.OVERVIEW_7D
                    : BatteryAccessibilityLayout.OVERVIEW_30D;
        }
        float bodyX = x - contentInset(w);
        float bodyW = contentWidth(w);
        if (page == 1 && y > 350 && y < 420 && bodyX > bodyW - 130) return 20; // charge target
        if (page == 1 && y >= 462 && y < 504 && bodyX >= bodyW - 145) return 21; // alarm
        if (page == 1 && y >= 508 && y < 552 && bodyX >= bodyW - 145) return 22; // overlay
        if (page == 3 && y > 690 && y < 825 && bodyX > bodyW - 140) return 30; // benchmark
        if (page == 4 && y > historyExportTop() && y < historyExportTop() + 55) return 40;
        return 0;
    }

    private String fitText(String value, float maxWidthDp, float size, boolean bold) {
        if (value == null) return "";
        type(size, Color.WHITE, bold);
        if (p.measureText(value) <= u(maxWidthDp)) return value;
        String suffix = "…";
        String result = value;
        while (result.length() > 1) {
            result = result.substring(0, result.length() - 1);
            if (p.measureText(result + suffix) <= u(maxWidthDp)) return result + suffix;
        }
        return suffix;
    }

    private float contentWidth(float viewWidth) {
        // The dashboard has a dedicated landscape composition for wide,
        // short windows. Give that composition enough horizontal room while
        // retaining a readable max width everywhere else.
        float drawableWidth = getWidth() / density;
        float drawableHeight = visibleViewportHeight();
        if (page == 0 && drawableWidth >= 600f && drawableWidth > drawableHeight) {
            return Math.min(Math.max(0f, viewWidth - 48f), 960f);
        }
        return Math.min(viewWidth, 560f);
    }
    private float contentInset(float viewWidth) { return Math.max(0f, (viewWidth - contentWidth(viewWidth)) / 2f); }

    private float visibleViewportHeight() {
        android.view.ViewParent parent = getParent();
        if (parent instanceof ScrollView && ((View) parent).getHeight() > 0) return ((View) parent).getHeight() / density;
        if (viewportHeightDp > 0f) return viewportHeightDp;
        return getHeight() / density;
    }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        float w = getWidth() / density;
        float h = getHeight() / density;
        Rect visibleWindow = new Rect();
        getWindowVisibleDisplayFrame(visibleWindow);
        viewportWidthDp = visibleWindow.width() > 0 ? visibleWindow.width() / density : w;
        viewportHeightDp = visibleWindow.height() > 0 ? visibleWindow.height() / density : h;
        layoutWidthDp = w;
        int bg = light ? Color.rgb(246, 248, 243) : (amoled ? Color.BLACK : Color.rgb(11, 16, 17));
        int panel = light ? Color.WHITE : (amoled ? Color.rgb(5, 5, 5) : Color.rgb(20, 28, 25));
        int raised = light ? Color.rgb(237, 242, 233) : (amoled ? Color.rgb(12, 12, 12) : Color.rgb(28, 39, 33));
        int border = light ? Color.rgb(207, 218, 207) : (amoled ? Color.rgb(36, 36, 36) : Color.rgb(49, 66, 55));
        int primary = light ? Color.rgb(23, 26, 29) : Color.rgb(242, 244, 239);
        int muted = light ? Color.rgb(82, 95, 87) : Color.rgb(138, 145, 157);
        int faint = light ? Color.rgb(103, 116, 106) : Color.rgb(102, 109, 121);
        int bgTop = light ? Color.rgb(251, 252, 249) : (amoled ? Color.BLACK : Color.rgb(20, 30, 25));
        p.setShader(new LinearGradient(0, 0, 0, u(Math.min(h, 520)), bgTop, bg, Shader.TileMode.CLAMP));
        fill(c, bg); c.drawRect(0, 0, getWidth(), getHeight(), p);
        p.setShader(null);

        drawHeader(c, w, primary, muted, border, panel);
        // Keep large-screen cards readable and visually anchored instead of
        // stretching telemetry across a tablet or foldable display.
        float bodyInset = contentInset(w);
        float bodyWidth = contentWidth(w);
        c.save();
        c.translate(u(bodyInset), 0);
        c.clipRect(0, 0, u(bodyWidth), getHeight());
        layoutWidthDp = bodyWidth;
        if (page == 0) drawOverview(c, bodyWidth, h, panel, raised, border, primary, muted, faint);
        else if (page == 1) drawChargingPage(c, bodyWidth, h, panel, raised, border, primary, muted, faint);
        else if (page == 2) drawDischargingPage(c, bodyWidth, h, panel, raised, border, primary, muted, faint);
        else if (page == 3) drawHealthPage(c, bodyWidth, h, panel, raised, border, primary, muted, faint);
        else drawHistoryPage(c, bodyWidth, h, panel, raised, border, primary, muted, faint);
        c.restore();
        layoutWidthDp = w;
    }

    private void drawHeader(Canvas c, float w, int primary, int muted, int border, int panel) {
        rounded(c, 18, 18, 54, 54, 14, lime);
        drawBolt(c, 36, 36, Color.rgb(26, 32, 17), 1.1f);
        text(c, "Ampere", 62, 39, 17, primary, true);
        String buildLabel = w < 300f ? "v" + BuildConfig.VERSION_NAME : "LIVE-TELEMETRIE · v" + BuildConfig.VERSION_NAME;
        rounded(c, 62, 44, w < 300f ? 103 : 188, 62, 9, panel);
        text(c, fitText(buildLabel, w < 300f ? 34f : 116f, 7.5f, true), 70, 56, 7.5f, lime, true);
        text(c, page == 0 ? "Überwachung  /  Übersicht" : "Überwachung  /  " + pageName(), 18, 80, 10, muted, false);
        text(c, page == 0 ? "Live-Übersicht" : pageName(), 18, 111, 28, primary, true);
        final float controlTop = 12f;
        final float controlBottom = 60f;
        if (w < 390f) {
            // Keep the actions as two independent 48-dp controls. A shared
            // capsule visually merged unrelated actions and made their
            // outlines look misaligned on narrow phones.
            smoothButton(c, w - 116, controlTop, w - 68, controlBottom, 16, panel, border, lime, false, isPressed(1));
            smoothButton(c, w - 60, controlTop, w - 12, controlBottom, 16, panel, border, lime, false, isPressed(2));
            drawHeaderOverflow(c, w - 90, muted);
            drawSun(c, w - 38, 36, muted);
        } else {
            // Three equal controls use the same measured cell and gap as the
            // narrow layout. Each icon and the LIVE label are centered inside
            // its own surface, never across a neighboring button.
            smoothButton(c, w - 176, controlTop, w - 128, controlBottom, 16, panel, border, lime, false, isPressed(1));
            smoothButton(c, w - 120, controlTop, w - 72, controlBottom, 16, panel, border, lime, false, isPressed(2));
            smoothButton(c, w - 64, controlTop, w - 16, controlBottom, 16, panel, border, lime, false, isPressed(3));
            drawHeaderOverflow(c, w - 152, muted);
            drawSun(c, w - 96, 36, muted);
            type(9, primary, true);
            float liveLabelWidth = p.measureText("LIVE") / density;
            float liveGroupWidth = 8f + 7f + liveLabelWidth;
            float liveLeft = w - 40f - liveGroupWidth / 2f;
            fill(c, lime); c.drawCircle(u(liveLeft + 2f), u(36), u(4), p);
            text(c, "LIVE", liveLeft + 12f, 40, 9, primary, true);
        }
        drawNav(c, w, primary, muted, border, panel);
    }

    private void drawHeaderOverflow(Canvas c, float cx, int color) {
        fill(c, color);
        c.drawCircle(u(cx), u(27), u(1.5f), p);
        c.drawCircle(u(cx), u(36), u(1.5f), p);
        c.drawCircle(u(cx), u(45), u(1.5f), p);
    }

    private void drawNav(Canvas c, float w, int primary, int muted, int border, int panel) {
        boolean compactNav = w < 480f;
        String[] labels = compactNav
                ? new String[]{"Start", "Laden", "Entl.", "Gesund.", "Verlauf"}
                : new String[]{"Übersicht", "Laden", "Entladen", "Gesundheit", "Verlauf"};
        float cell = (w - 36) / 5f;
        final float navTop = 120f;
        final float navBottom = 168f;
        controlSurface(c, 18, navTop, w - 18, navBottom, 16, panel, border);
        for (int i = 0; i < labels.length; i++) {
            float x = 18 + i * cell;
            float centerX = x + cell / 2f;
            boolean active = page == i;
            boolean pressed = isPressed(10 + i);
            if (active || pressed) {
                // The active state is an instrument rail, not another pill:
                // the tonal wash gives context while the lime footmark gives
                // a precise, color-plus-position cue for the selected page.
                int activeSurface = light ? Color.rgb(239, 247, 222) : Color.rgb(35, 48, 29);
                if (pressed) activeSurface = pressedFill(activeSurface, true);
                rounded(c, x + 4, 124, x + cell - 4, 164, 10, activeSurface);
                if (active) {
                    rounded(c, x + 13, 160, x + cell - 13, 163, 1.5f, lime);
                }
            }
            int activeTextColor = light ? Color.rgb(23, 28, 16) : lime;
            int iconColor = active ? activeTextColor : muted;
            if (compactNav) {
                // A fixed 24dp icon slot plus one shared label baseline is
                // the same top-centered model used by Material NavigationBar.
                drawNavGlyph(c, i, centerX, 136, iconColor);
                centeredText(c, labels[i], centerX, 158, 8.5f, active ? activeTextColor : muted, active);
            } else {
                // Material's horizontal navigation layout centers one shared
                // content block: a 24-dp icon box, fixed icon/label spacing,
                // and the measured label. Centering the label independently
                // makes items with different words look uneven.
                final float iconBox = 24f;
                final float iconLabelGap = 8f;
                String fittedLabel = fitText(labels[i], Math.max(24f, cell - iconBox - iconLabelGap - 12f), 9, active);
                type(9, active ? activeTextColor : muted, active);
                float labelWidth = p.measureText(fittedLabel) / density;
                float groupWidth = iconBox + iconLabelGap + labelWidth;
                float groupLeft = centerX - groupWidth / 2f;
                drawNavGlyph(c, i, groupLeft + iconBox / 2f, 144, iconColor);
                boundedText(c, fittedLabel, groupLeft + iconBox + iconLabelGap, x + cell - 8f, 148, 9,
                        active ? activeTextColor : muted, active);
            }
        }
    }

    private void drawNavGlyph(Canvas c, int index, float cx, float cy, int color) {
        if (index == 1) { drawBolt(c, cx, cy, color, .65f); return; }
        if (index == 2) { drawArrow(c, cx, cy, color); return; }
        if (index == 3) { drawHeart(c, cx, cy, color, .75f); return; }
        stroke(c, color, 1.5f);
        if (index == 0) {
            c.drawCircle(u(cx), u(cy), u(8), p);
            line(c, cx, cy, cx, cy - 5, color, 1.5f);
            line(c, cx, cy, cx + 4, cy + 3, color, 1.5f);
        } else {
            Path chart = new Path();
            chart.moveTo(u(cx - 8), u(cy + 5));
            chart.lineTo(u(cx - 3), u(cy - 1));
            chart.lineTo(u(cx + 1), u(cy + 2));
            chart.lineTo(u(cx + 8), u(cy - 6));
            c.drawPath(chart, p);
        }
    }

    private void drawOverview(Canvas c, float w, float h, int panel, int raised, int border, int primary, int muted, int faint) {
        // Use the actual drawable height. The root window also includes the
        // navigation area on some Android 16/17 configurations, so relying
        // on its height can select the tall landscape composition and paint
        // the bottom of the live card behind the gesture bar.
        // getWindowVisibleDisplayFrame() may retain pre-rotation dimensions
        // during an Android 16/17 rotation. The Canvas dimensions are the
        // authoritative geometry for this draw pass.
        float drawableWidth = getWidth() / density;
        float drawableHeight = visibleViewportHeight();
        boolean landscapeWindow = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE
                || (drawableWidth >= 600f && drawableWidth > drawableHeight);
        if (landscapeWindow) {
            float visibleHeight = drawableHeight > 0f ? drawableHeight : h;
            drawOverviewLandscape(c, w, visibleHeight, panel, raised, border, primary, muted, faint);
            return;
        }
        float top = 182;
        float heroW = Math.min(w - 36, 520);
        // Use the adaptive window class, not the content column width. A
        // small difference in device density must not switch otherwise
        // identical phones between two visibly different compositions.
        boolean compact = viewportWidthDp > 0f ? viewportWidthDp < 600f : w < 600f;
        float heroH = compact ? 400f : 320f;
        frame(c, 18, top, 18 + heroW, top + heroH, panel, border, lime);
        text(c, "AKKUSTAND · AUTOMATIK", 36, top + 31, 10, muted, true);
        text(c, "Aktueller Akkustand", 36, top + 56, 17, primary, true);
        if (!compact) {
            rounded(c, 18 + heroW - 142, top + 22, 18 + heroW - 36, top + 48, 13, raised);
            fill(c, lime); c.drawCircle(u(18 + heroW - 128), u(top + 35), u(3), p);
            text(c, "LIVE · GERÄT", 18 + heroW - 118, top + 39, 8, lime, true);
            line(c, 36, top + 101, 18 + heroW - 36, top + 101, border, 1);
        }
        boolean batteryAvailable = level >= 0;
        int stateColor = !batteryAvailable ? muted : (charging ? lime : blue);
        rounded(c, 36, top + 67, 122, top + 89, 11,
                Color.argb(batteryAvailable ? (charging ? 42 : 35) : 24,
                        Color.red(stateColor), Color.green(stateColor), Color.blue(stateColor)));
        fill(c, stateColor); c.drawCircle(u(47), u(top + 78), u(3), p);
        boundedText(c, batteryChipLabel(), 57, 122, top + 82, 8, stateColor, true);
        int health = healthPercent();
        String powerText = currentMa > 0 ? String.format(Locale.US, "ca. %.1f W aktueller Verbrauch", currentMa * voltage / 1000f) : "Warte auf Strommessung";
        String detectionText = batteryAvailable
                ? (charging ? chargerTypeDisplay() + " · automatisch von Android erkannt" : powerText + " · automatisch von Android erkannt")
                : "Warte auf Android-Akkuwert";
        if (compact) {
            float centerX = 18 + heroW / 2f;
            float gaugeOffset = heroW < 230f ? 22f : 0f;
            float gaugeRadius = Math.min(80f, Math.max(52f, heroW / 2f - 10f));
            float gaugeTextSize = gaugeRadius < 64f ? 32f : 44f;
            drawGauge(c, centerX, top + 153 + gaugeOffset, gaugeRadius, level, primary, faint);
            centeredText(c, levelDisplay(), centerX, top + 168 + gaugeOffset, gaugeTextSize, primary, true);
            centeredText(c, batteryModeLabel(), centerX, top + 207 + gaugeOffset, 9, muted, false);
            float compactDetailsOffset = heroW < 230f ? 20f : 0f;
            text(c, "Akkugesundheit", 36, top + 258 + compactDetailsOffset, 9, muted, false);
            text(c, health == 0 ? "Nicht gemessen" : health + "%", 36, top + 280 + compactDetailsOffset, 14, primary, true);
            float capacityX = 18 + heroW * .57f;
            float compactDetailsRight = 18 + heroW - 42;
            // Prefer a complete short label on phone-width cards. Fitting the
            // longer label character-by-character produced "Geschätzte
            // Kapaz…" inside an otherwise clean metric column.
            boundedText(c, heroW < 350f ? "Kapazität" : "Geschätzte Kapazität", capacityX, compactDetailsRight,
                    top + 258 + compactDetailsOffset, 9, muted, false);
            boundedText(c, health > 0 ? mahDisplay(estimatedCapacityMah()) : "—", capacityX, compactDetailsRight,
                    top + 280 + compactDetailsOffset, 12, primary, true);
            boundedText(c, "Nennwert " + designCapacityDisplay(), capacityX, compactDetailsRight,
                    top + 297 + compactDetailsOffset, 8, faint, false);
            float compactStatusOffset = heroW < 230f ? 20f : 0f;
            rounded(c, 36, top + 315 + compactStatusOffset, 18 + heroW - 36, top + 369 + compactStatusOffset, 8, raised);
            drawBolt(c, 52, top + 333 + compactStatusOffset, lime, .8f);
            text(c, batteryRowLabel(), 68, top + 332 + compactStatusOffset, 10, primary, true);
            String compactDetection = charging ? (heroW < 230f ? chargerTypeDisplay() : chargerTypeDisplay() + " · automatisch")
                    : (heroW < 230f ? "Automatisch erkannt" : "Akku automatisch erkannt");
            float statusRight = 18 + heroW - 50;
            float currentLeft = Math.max(68f, statusRight - 76f);
            boundedText(c, compactDetection, 68, currentLeft - 8f, top + 350 + compactStatusOffset, 8, muted, false);
            boundedRightText(c, liveCurrentDisplay(), currentLeft, statusRight,
                    top + 339 + compactStatusOffset, 9, charging ? lime : blue, false);
        } else {
            // Keep a clear vertical rhythm: the gauge ends before the
            // details/status rows begin. The previous 101-dp circle touched
            // the status chip on some densities.
            float gaugeRadius = Math.min(84f, Math.max(78f, heroW * .16f));
            float gaugeCx = 135f;
            float gaugeCy = top + 178f;
            drawGauge(c, gaugeCx, gaugeCy, gaugeRadius, level, primary, faint);
            centeredText(c, levelDisplay(), gaugeCx, top + 187, 46, primary, true);
            centeredText(c, batteryModeLabel(), gaugeCx, top + 211, 10, muted, false);
            float detailRight = 18 + heroW - 42;
            boundedText(c, health == 0 ? "Nicht gemessen" : (health > 80 ? "Guter Zustand" : "Prüfung nötig"),
                    255, detailRight, top + 117, 17, primary, true);
            boundedText(c, health == 0 ? "Benchmark starten" : (health > 80 ? "Im gesunden Bereich" : "Unter Erwartung"),
                    255, detailRight, top + 141, 10, muted, false);
            boundedText(c, "Volle Kapazität", 255, detailRight, top + 181, 10, muted, false);
            boundedText(c, health > 0 ? mahDisplay(estimatedCapacityMah()) : "—", 255, detailRight, top + 201, 12, primary, true);
            rounded(c, 255, top + 215, detailRight, top + 219, 3, border);
            if (health > 0) rounded(c, 255, top + 215, 255 + (detailRight - 255) * Math.min(1f, health / 100f), top + 219, 3, lime);
            boundedText(c, "Nennkapazität " + designCapacityDisplay(), 255, detailRight, top + 236, 9, faint, false);
            float statusTop = top + 271f;
            rounded(c, 36, statusTop, 18 + heroW - 36, statusTop + 38, 8, raised);
            drawBolt(c, 52, statusTop + 19, lime, .8f);
            text(c, batteryRowLabel(), 68, statusTop + 15, 10, primary, true);
            float statusRight = 18 + heroW - 50;
            float currentLeft = Math.max(68f, statusRight - 76f);
            boundedText(c, detectionText, 68, currentLeft - 8f, statusTop + 30, 9, muted, false);
            boundedRightText(c, liveCurrentDisplay(), currentLeft, statusRight,
                    statusTop + 22, 9, charging ? lime : blue, false);
        }

        float cardsTop = top + heroH + 14;
        float cardGap = 12;
        float cardW = (w - 36 - cardGap) / 2f;
        drawStat(c, 18, cardsTop, cardW, 105, "Akkugesundheit", healthDisplay(), health > 0 ? "%" : "", lime, primary, muted, border, panel, "heart");
        drawStat(c, 18 + cardW + cardGap, cardsTop, cardW, 105, "Akkutemperatur", temperatureDisplay(), temperature > 0f ? "°C" : "", amber, primary, muted, border, panel, "temp");
        drawStat(c, 18, cardsTop + 117, cardW, 105, "Spannung", voltageDisplay(), voltage > 0f ? "V" : "", blue, primary, muted, border, panel, "bolt");
        drawStat(c, 18 + cardW + cardGap, cardsTop + 117, cardW, 105, "Bildschirmzeit", screenOnTimeCard(), "", Color.rgb(180, 154, 255), primary, muted, border, panel, "clock");

        float lowerTop = cardsTop + 234;
        drawChart(c, 18, lowerTop, w - 36, 360, panel, border, primary, muted, faint);
    }

    /**
     * Landscape composition for wide but short windows. A single tall hero
     * card wastes the horizontal space and hides the key metrics below the
     fold, so the live card and supporting metrics share the first viewport.
     */
    private void drawOverviewLandscape(Canvas c, float w, float h, int panel, int raised, int border,
                                       int primary, int muted, int faint) {
        if (h < 390f) {
            drawOverviewLandscapeShort(c, w, h, panel, raised, border, primary, muted, faint);
            return;
        }
        float top = 182f;
        float gap = 16f;
        float available = Math.max(300f, w - 36f - gap);
        float heroW = Math.max(300f, Math.min(500f, available * .58f));
        float metricsX = 18f + heroW + gap;
        float metricsW = Math.max(150f, w - metricsX - 18f);
        float heroRight = 18f + heroW;
        // Keep the bottom status row clear of the rounded outline. On wide
        // landscape cards the bolt glyph needs a little more breathing room
        // than the text-only metrics above it.
        float heroBottom = top + 192f;
        frame(c, 18, top, heroRight, heroBottom, panel, border, lime);
        text(c, "AKKUSTAND · AUTOMATIK", 36, top + 31, 10, muted, true);
        text(c, "Aktueller Akkustand", 36, top + 56, 17, primary, true);
        // The live state is shown once in the dedicated status row below.
        // A second chip here competed for the same vertical lane as the
        // gauge on wide landscape cards and could be painted underneath it.
        // Keep the ring below the heading and above the dedicated bottom
        // status row. The previous 54-dp ring began on the heading baseline
        // in wide landscape cards, which made the two groups visually merge.
        float gaugeRadius = Math.min(42f, Math.max(38f, heroW * .14f));
        float gaugeCx = 36f + gaugeRadius + 8f;
        float gaugeCy = top + 122f;
        drawGauge(c, gaugeCx, gaugeCy, gaugeRadius, level, primary, faint);
        centeredText(c, levelDisplay(), gaugeCx, gaugeCy + 8, gaugeRadius < 52f ? 25f : 28f, primary, true);
        centeredText(c, batteryModeLabel(), gaugeCx, gaugeCy + 29, 7, muted, false);

        float detailX = Math.max(160f, heroW * .52f);
        int health = healthPercent();
        float detailRight = heroRight - 18f;
        boundedText(c, health == 0 ? "Nicht gemessen" : (health > 80 ? "Guter Zustand" : "Prüfung nötig"),
                detailX, detailRight, top + 91, 13, primary, true);
        boundedText(c, "Akkugesundheit", detailX, detailRight, top + 108, 7, muted, false);
        boundedText(c, "Volle Kapazität", detailX, detailRight, top + 130, 7, muted, false);
        boundedText(c, health > 0 ? mahDisplay(estimatedCapacityMah()) : "—", detailX, detailRight, top + 146, 11, primary, true);
        boundedText(c, "Nennwert " + designCapacityDisplay(), detailX, detailRight, top + 160, 7, faint, false);

        rounded(c, 36, top + 164, heroRight - 18, top + 176, 5, raised);
        drawBolt(c, 46, top + 170, lime, .5f);
        text(c, batteryRowLabel(), 57, top + 172, 7, primary, true);
        boundedRightText(c, liveCurrentDisplay(), heroRight - 86, heroRight - 26, top + 172, 7, charging ? lime : blue, false);

        float metricGap = 10f;
        float metricW = (metricsW - metricGap) / 2f;
        drawStat(c, metricsX, top, metricW, 98, "Gesundheit", healthDisplay(), health > 0 ? "%" : "", lime,
                primary, muted, border, panel, "heart");
        drawStat(c, metricsX + metricW + metricGap, top, metricW, 98, "Temperatur", temperatureDisplay(), temperature > 0f ? "°C" : "", amber,
                primary, muted, border, panel, "temp");
        drawStat(c, metricsX, top + 106, metricW, 98, "Spannung", voltageDisplay(), voltage > 0f ? "V" : "", blue,
                primary, muted, border, panel, "bolt");
        drawStat(c, metricsX + metricW + metricGap, top + 106, metricW, 98, "Screenzeit", screenOnTimeCard(), "", violet,
                primary, muted, border, panel, "clock");

        drawChart(c, 18, top + 218, w - 36, 360, panel, border, primary, muted, faint);
    }

    /**
     * Compact landscape composition for short split-screen and small tablet
     * windows. The live card remains complete; the metric cards continue below
     * in the ScrollView instead of being painted underneath system navigation.
     */
    private void drawOverviewLandscapeShort(Canvas c, float w, float h, int panel, int raised, int border,
                                            int primary, int muted, int faint) {
        float top = 174f;
        float gap = 14f;
        float available = Math.max(300f, w - 36f - gap);
        float heroW = Math.max(300f, Math.min(500f, available * .58f));
        float metricsX = 18f + heroW + gap;
        float metricsW = Math.max(150f, w - metricsX - 18f);
        float heroRight = 18f + heroW;
        // On a short landscape viewport the system navigation area can cover
        // the last 20–30 dp of the canvas. Keep the compact hero inside a
        // measured safe band instead of letting its lower edge disappear
        // behind that area. The card still scales down a little further for
        // split-screen windows whose visible height is smaller than 320 dp.
        float heroHeight = Math.max(84f, Math.min(88f, h - top - 12f));
        float heroBottom = top + heroHeight;
        frame(c, 18, top, heroRight, heroBottom, panel, border, lime);
        text(c, "AKKUSTAND · AUTOMATIK", 34, top + 23, 8, muted, true);
        text(c, fitText("Aktueller Akkustand", heroW - 32, 15, true), 34, top + 46, 15, primary, true);
        // The live state is represented by the lower status row. Keeping it
        // out of this short card's gauge lane prevents a hidden overlap when
        // the window is only a few hundred dp high.
        // Put the gauge in its own right-hand lane. Stacking it below the
        // status chip made the two visual groups collide in 320dp-tall
        // landscape windows even when the card itself fit.
        float gaugeRadius = Math.min(22f, Math.max(19f, heroW * .07f));
        float gaugeCx = heroRight - gaugeRadius - 24f;
        float gaugeCy = top + 47f;
        drawGauge(c, gaugeCx, gaugeCy, gaugeRadius, level, primary, faint);
        centeredText(c, levelDisplay(), gaugeCx, gaugeCy + 5, 13, primary, true);
        centeredText(c, batteryModeLabel(), gaugeCx, gaugeCy + 15, 5.5f, muted, false);

        int health = healthPercent();
        float detailRight = gaugeCx - gaugeRadius - 10f;
        boundedText(c, health == 0 ? "Gesundheit —" : "Gesundheit " + health + "%",
                34, detailRight, top + 84, 7, muted, false);

        float metricGap = 10f;
        float metricW = (metricsW - metricGap) / 2f;
        drawStat(c, metricsX, top, metricW, 98, "Gesundheit", healthDisplay(), health > 0 ? "%" : "", lime,
                primary, muted, border, panel, "heart");
        drawStat(c, metricsX + metricW + metricGap, top, metricW, 98, "Temperatur", temperatureDisplay(), temperature > 0f ? "°C" : "", amber,
                primary, muted, border, panel, "temp");
        drawStat(c, metricsX, top + 106, metricW, 98, "Spannung", voltageDisplay(), voltage > 0f ? "V" : "", blue,
                primary, muted, border, panel, "bolt");
        drawStat(c, metricsX + metricW + metricGap, top + 106, metricW, 98, "Screenzeit", screenOnTimeCard(), "", violet,
                primary, muted, border, panel, "clock");

        drawChart(c, 18, top + 218, w - 36, 360, panel, border, primary, muted, faint);
    }

    private void drawChargingPage(Canvas c, float w, float h, int panel, int raised, int border, int primary, int muted, int faint) {
        float y = 182;
        frame(c, 18, y, w - 18, y + 366, panel, border, lime);
        text(c, "LADEVORGANG", 36, y + 31, 10, muted, true);
        text(c, charging ? "Ladevorgang aktiv" : "Letzter Ladevorgang", 36, y + 58, 18, primary, true);
        drawBolt(c, 53, y + 105, lime, 1);
        // On a 320 dp window the old 31 dp current label crossed the column
        // divider. Give the left and right metrics explicit bounds and move the
        // environmental line below the divider in the compact composition.
        boolean narrowHeader = w < 390f;
        float dividerX = narrowHeader ? w * .58f : w * .54f;
        float rightColumn = narrowHeader ? w * .62f : w * .6f;
        boundedText(c, charging && currentMa > 0 ? currentMa + " mA" : "—", 77, dividerX - 8,
                y + 112, narrowHeader ? 27 : 31, primary, true);
        boundedText(c, charging ? liveCurrentSubLabel(narrowHeader) : (currentMa > 0 ? liveCurrentSubLabel(narrowHeader) : "getrennt · Verlaufsdaten"), 78, dividerX - 8,
                y + 132, 9, muted, false);
        line(c, dividerX, y + 86, dividerX, y + (narrowHeader ? 145 : 156), border, 1);
        text(c, charging ? (chargeLimit >= 100 ? "Zeit bis voll" : "Zeit bis Ziel") : "Letzte Ladung", rightColumn, y + 96, 10, muted, false);
        text(c, charging ? (chargeLimit >= 100 ? timeToFull() : timeToLimit()) : lastChargeRange(), rightColumn, y + 126, 20, primary, true);
        text(c, charging ? (chargeLimit >= 100 ? chargeTimeEstimateLabel() : "lokale 7-Tage-Schätzung") : lastChargeDuration(), rightColumn, y + 145, 9, faint, false);
        String thermalText = BatteryThermalStatus.isAvailable(thermalStatus)
                ? " · Thermik " + BatteryThermalStatus.label(thermalStatus) : "";
        String energyText = remainingEnergyNanoWattHours > 0L
                ? " · Restenergie " + BatteryEnergy.label(remainingEnergyNanoWattHours) : "";
        boundedText(c, "Temp. " + temperatureDisplay() + " °C · Spannung " + voltageDisplay() + " V" + thermalText + energyText,
                narrowHeader ? 36 : 78, w - 36, y + 151, 8, faint, false);
        text(c, "Ladeziel", 36, y + 190, 10, muted, false);
        String oemLimit = oemChargeControl.isAvailable() ? " · " + oemChargeControl.label() : "";
        boundedRightText(c, chargeLimit + "%" + oemLimit, w * .54f, w - 36, y + 190, 10, lime, true);
        float sourceLeft = narrowHeader ? 36f : 78f;
        float sourceRight = narrowHeader ? w * .58f : w * .54f;
        boundedText(c, "Quelle: " + chargerTypeDisplay(), sourceLeft, sourceRight, y + 169, 9, faint, false);
        boundedRightText(c, chargerCapability.label(),
                narrowHeader ? w * .62f : w * .60f, w - 36, y + 169, 9, faint, false);
        rounded(c, 36, y + 205, w - 36, y + 209, 3, border);
        rounded(c, 36, y + 205, 36 + (w - 72) * chargeLimit / 100f, y + 209, 3, lime);
        text(c, "Belastung bis zum Ziel", 36, y + 258, 9, muted, false);
        text(c, wearImpactToTarget(), w - 126, y + 258, 9, amber, true);
        drawToggleButton(c, 36, y + 280, w - 36, y + 320, "Ladealarm",
                chargeAlarm ? "Aktiv" : "Aus", chargeAlarm, isPressed(21),
                primary, muted, raised, border);
        drawToggleButton(c, 36, y + 326, w - 36, y + 366, "Live-Anzeige",
                overlayEnabled ? "Aktiv" : "Aus", overlayEnabled, isPressed(22),
                primary, muted, raised, border);
        int energyAdded = chargeEnergyForDisplay();
        drawStat(c, 18, y + 380, (w - 48) / 2f, 105, "Geladene Energie", energyAdded > 0 ? "+" + energyAdded : "—", "mAh", lime, primary, muted, border, panel, "bolt");
        drawStat(c, 30 + (w - 48) / 2f, y + 380, (w - 48) / 2f, 105, "Akkugesundheit", healthDisplay(), healthPercent() > 0 ? "%" : "", lime, primary, muted, border, panel, "heart");
        boolean compact = w < 430f;
        float amountBottom = compact ? y + 700 : y + 645;
        frame(c, 18, y + 500, w - 18, amountBottom, panel, border, lime);
        text(c, "LADEMENGE", 36, y + 530, 10, muted, true);
        text(c, "Änderung", 36, y + 558, 8, faint, false);
        text(c, chargeChangeForDisplay(), 36, y + 580, 13, primary, true);
        if (compact) {
            float secondColumn = 36 + (w - 72) / 2f;
            text(c, "Dauer", secondColumn, y + 558, 8, faint, false);
            text(c, chargeDurationForDisplay(), secondColumn, y + 580, 12, primary, true);
            text(c, "Gestartet", 36, y + 612, 8, faint, false);
            text(c, chargeStartForDisplay(), 36, y + 634, 11, primary, true);
            text(c, "Bildschirm an", 36, y + 666, 8, faint, false);
            text(c, chargeModeDetails(true), 36, y + 686, 9, blue, true);
            text(c, "Bildschirm aus", secondColumn, y + 666, 8, faint, false);
            text(c, chargeModeDetails(false), secondColumn, y + 686, 9, blue, true);
        } else {
            text(c, "Dauer", 150, y + 558, 8, faint, false);
            text(c, chargeDurationForDisplay(), 150, y + 580, 13, primary, true);
            text(c, "Gestartet", 285, y + 558, 8, faint, false);
            text(c, chargeStartForDisplay(), 285, y + 580, 13, primary, true);
            text(c, "Bildschirm an", 36, y + 612, 8, faint, false);
            text(c, chargeModeDetails(true), 36, y + 630, 10, blue, true);
            text(c, "Bildschirm aus", 285, y + 612, 8, faint, false);
            text(c, chargeModeDetails(false), 285, y + 630, 10, blue, true);
        }
        float remainingTop = compact ? y + 720 : y + 665;
        frame(c, 18, remainingTop, w - 18, remainingTop + 105, panel, border, violet);
        text(c, "VERBLEIBENDE NUTZUNGSZEIT", 36, remainingTop + 30, 10, muted, true);
        float useColumn = compact ? 36 : 36;
        float useColumnOn = compact ? 36 + (w - 72) / 3f : 160;
        float useColumnOff = compact ? 36 + 2f * (w - 72) / 3f : 284;
        text(c, "Gemischt", useColumn, remainingTop + 58, 8, faint, false);
        text(c, runtimeEstimate(), useColumn, remainingTop + 80, compact ? 10 : 12, primary, true);
        text(c, "An", useColumnOn, remainingTop + 58, 8, faint, false);
        text(c, dischargeRuntime(true), useColumnOn, remainingTop + 80, compact ? 10 : 12, blue, true);
        text(c, "Aus", useColumnOff, remainingTop + 58, 8, faint, false);
        text(c, dischargeRuntime(false), useColumnOff, remainingTop + 80, compact ? 10 : 12, blue, true);
        text(c, runtimeEstimateSource(), 36, remainingTop + 98, 8, faint, false);
        float capacityTop = remainingTop + 125;
        rounded(c, 18, capacityTop, w - 18, capacityTop + 85, 12, panel); stroke(c, border, 1); rect.set(u(18), u(capacityTop), u(w - 18), u(capacityTop + 85)); c.drawRoundRect(rect, u(12), u(12), p);
        text(c, "KAPAZITÄTSSCHÄTZUNG", 36, capacityTop + 30, 10, muted, true);
        text(c, healthPercent() > 0 ? mahDisplay(estimatedCapacityMah()) : "—", 36, capacityTop + 61, 24, lime, true);
        rightText(c, healthPercent() > 0 ? "Quelle: " + healthMeasurementSourceLabel() : "Länger laden für eine Schätzung", w - 30, capacityTop + 59, 8, faint, false);
        text(c, healthEstimateStatus(), 36, capacityTop + 79, 8, faint, false);
        float speedTop = capacityTop + 110;
        rounded(c, 18, speedTop, w - 18, speedTop + 105, 12, panel); stroke(c, border, 1); rect.set(u(18), u(speedTop), u(w - 18), u(speedTop + 105)); c.drawRoundRect(rect, u(12), u(12), p);
        text(c, "LADEGESCHWINDIGKEIT", 36, speedTop + 30, 10, muted, true);
        if (compact) {
            float secondColumn = 36 + (w - 72) / 2f;
            text(c, "Bildschirm an", 36, speedTop + 58, 8, faint, false);
            text(c, compactChargeSpeed(true), 36, speedTop + 78, 11, primary, true);
            text(c, compactChargeSpeedRate(true), 36, speedTop + 95, 8, blue, false);
            text(c, "Bildschirm aus", secondColumn, speedTop + 58, 8, faint, false);
            text(c, compactChargeSpeed(false), secondColumn, speedTop + 78, 11, primary, true);
            text(c, compactChargeSpeedRate(false), secondColumn, speedTop + 95, 8, blue, false);
        } else {
            text(c, "Bildschirm an", 36, speedTop + 62, 9, faint, false);
            text(c, chargeSpeed(true), 36, speedTop + 84, 13, primary, true);
            text(c, "Bildschirm aus", w * .54f, speedTop + 62, 9, faint, false);
            text(c, chargeSpeed(false), w * .54f, speedTop + 84, 13, primary, true);
            text(c, "Aus lokalen Sitzungsdaten", 36, speedTop + 99, 8, faint, false);
        }
        drawTelemetryChart(c, 18, speedTop + 125, w - 36, 220, panel, border, primary, muted, faint, true);
    }

    private void drawDischargingPage(Canvas c, float w, float h, int panel, int raised, int border, int primary, int muted, int faint) {
        float y = 182;
        rounded(c, 18, y, w - 18, y + 300, 12, panel); stroke(c, border, 1); rect.set(u(18), u(y), u(w - 18), u(y + 300)); c.drawRoundRect(rect, u(12), u(12), p);
        text(c, "ENTLADEVORGANG", 36, y + 31, 10, muted, true);
        int savedDischargeEnd = lastDischargeEndLevel();
        boolean hasDischargeHistory = savedDischargeEnd >= 0;
        text(c, charging ? (hasDischargeHistory ? "Letzter Entladevorgang" : "Noch keine Entladung") : "Akkuverbrauch",
                36, y + 58, 18, primary, true);
        int displayLevel = charging ? savedDischargeEnd : level;
        drawGauge(c, 94, y + 145, 55, displayLevel, primary, faint);
        text(c, percentDisplay(displayLevel), 70, y + 153, 22, primary, true);
        text(c, displayLevel >= 0 ? "verbleibend" : "noch keine Entladung", 69, y + 173, 9, muted, false);
        line(c, w * .54f, y + 94, w * .54f, y + 196, border, 1);
        text(c, "Gemischte Laufzeit", w * .6f, y + 106, 10, muted, false);
        text(c, runtimeEstimate(), w * .6f, y + 138, 20, primary, true);
        text(c, "basierend auf letzter Nutzung", w * .6f, y + 157, 9, faint, false);
        text(c, "Bildschirm an / aus", w * .6f, y + 187, 10, muted, false);
        text(c, dischargeRuntime(true) + " / " + dischargeRuntime(false), w * .6f, y + 207, 11, blue, true);
        text(c, "7-Tage-Durchschnitt", 36, y + 225, 10, muted, false);
        text(c, averageDischargeRateDisplay(), w - 92, y + 225, 10, blue, true);
        text(c, "Entladerate", 36, y + 245, 10, muted, false);
        text(c, dischargeSpeed(true) + " · " + dischargeSpeed(false), w - 145, y + 245, 10, blue, true);
        text(c, "Bildschirm an / aus", w - 112, y + 262, 8, faint, false);
        text(c, "Ladestrom live", 36, y + 280, 9, muted, false);
        text(c, !charging && currentMa > 0 ? "−" + currentMa + " mA" : "—", w - 95, y + 280, 10, blue, true);
        drawStat(c, 18, y + 316, (w - 48) / 2f, 105, "Bildschirmzeit", dischargeDurationCompact(true), "", Color.rgb(180, 154, 255), primary, muted, border, panel, "clock");
        drawStat(c, 30 + (w - 48) / 2f, y + 316, (w - 48) / 2f, 105, "Verbrauch", dischargeMah() > 0 ? String.valueOf(dischargeMah()) : "—", "mAh", blue, primary, muted, border, panel, "arrow");
        rounded(c, 18, y + 438, w - 18, y + 536, 12, panel); stroke(c, border, 1); rect.set(u(18), u(y + 438), u(w - 18), u(y + 536)); c.drawRoundRect(rect, u(12), u(12), p);
        text(c, "Nutzungsübersicht", 36, y + 468, 10, muted, true);
        int recordedDischargeMah = dischargeMah();
        boundedText(c, "An " + dischargePercent(true) + " · aus " + dischargePercent(false) + " · "
                + (recordedDischargeMah > 0 ? recordedDischargeMah + " mAh" : "—"), 36, w - 36, y + 486, 8, primary, false);
        boundedText(c, "Tiefschlaf: " + deepSleepPercent() + " · " + deepSleepTime() + " · Bildschirm-Aufweckungen " + wakeupCount(),
                36, w - 36, y + 502, 8, primary, false);
        boundedText(c, sinceFullAnchorLabel() + ": " + sinceFullUsageSummary(), 36, w - 36, y + 518, 8, primary, false);
        rounded(c, 18, y + 540, w - 18, y + 715, 12, panel); stroke(c, border, 1); rect.set(u(18), u(y + 540), u(w - 18), u(y + 715)); c.drawRoundRect(rect, u(12), u(12), p);
        text(c, "Vordergrund-Apps", 36, y + 571, 13, primary, true);
        if (hasUsageAccess()) {
            drawUsageRows(c, w, y + 600, primary, muted, faint);
        } else {
            text(c, "Optionale Android-Berechtigung", 36, y + 603, 10, amber, true);
            text(c, "Erlaube den Nutzungszugriff, um Apps mit", 36, y + 627, 10, muted, false);
            text(c, "dem höchsten Vordergrundverbrauch zu sehen.", 36, y + 645, 10, muted, false);
            text(c, "Tippen, um Android-Einstellungen zu öffnen", 36, y + 683, 9, lime, true);
        }
        drawTelemetryChart(c, 18, y + 735, w - 36, 220, panel, border, primary, muted, faint, false);
    }

    private boolean hasUsageAccess() {
        AppOpsManager ops = (AppOpsManager) getContext().getSystemService(Context.APP_OPS_SERVICE);
        if (ops == null) return false;
        int mode = ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), getContext().getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    private static final class AppUsageRow {
        final String packageName;
        final long foregroundMs;

        AppUsageRow(String packageName, long foregroundMs) {
            this.packageName = packageName;
            this.foregroundMs = foregroundMs;
        }
    }

    /**
     * UsageStats is aggregated to whole interval buckets, which can include
     * time outside an active discharge. Prefer exact foreground/background
     * events and keep the bucketed API only as a device-compatible fallback.
     */
    private List<AppUsageRow> appUsageRows(long start, long end) {
        UsageStatsManager manager = (UsageStatsManager) getContext().getSystemService(Context.USAGE_STATS_SERVICE);
        if (manager == null || end <= start) return new ArrayList<>();
        Map<String, Long> exact = exactForegroundTimes(manager, start, end);
        ArrayList<AppUsageRow> rows = new ArrayList<>();
        for (Map.Entry<String, Long> entry : exact.entrySet()) {
            if (!entry.getKey().equals(getContext().getPackageName()) && entry.getValue() >= 60L * 1000L) {
                rows.add(new AppUsageRow(entry.getKey(), entry.getValue()));
            }
        }
        // An exact event stream can legitimately contain only short sessions
        // below the one-minute display threshold. Do not replace that honest
        // result with an unbounded daily UsageStats bucket.
        if (rows.isEmpty() && exact.isEmpty()) {
            List<UsageStats> stats = manager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end);
            if (stats != null) {
                for (UsageStats stat : stats) {
                    if (!stat.getPackageName().equals(getContext().getPackageName())
                            && stat.getTotalTimeInForeground() >= 60L * 1000L) {
                        rows.add(new AppUsageRow(stat.getPackageName(), stat.getTotalTimeInForeground()));
                    }
                }
            }
        }
        Collections.sort(rows, new Comparator<AppUsageRow>() {
            @Override public int compare(AppUsageRow left, AppUsageRow right) {
                return Long.compare(right.foregroundMs, left.foregroundMs);
            }
        });
        return rows;
    }

    private Map<String, Long> exactForegroundTimes(UsageStatsManager manager, long start, long end) {
        Map<String, Long> totals = new HashMap<>();
        Map<String, UsageEventAccumulator.State> active = new HashMap<>();
        UsageEvents events = manager.queryEvents(Math.max(0L, start - 24L * 60L * 60L * 1000L), end);
        if (events == null) return totals;
        UsageEvents.Event event = new UsageEvents.Event();
        while (events.hasNextEvent()) {
            events.getNextEvent(event);
            String packageName = event.getPackageName();
            long timestamp = event.getTimeStamp();
            if (isScreenOffEvent(event.getEventType())) {
                UsageEventAccumulator.closeAll(totals, active, Math.min(end, timestamp));
                continue;
            }
            if (packageName == null || packageName.isEmpty() || timestamp > end) continue;
            // MOVE_TO_BACKGROUND and ACTIVITY_PAUSED share an event value on
            // Android. Keep the class name whenever the platform provides it;
            // UsageEventAccumulator treats a genuinely empty name as the
            // legacy package-wide close.
            String className = event.getClassName();
            UsageEventAccumulator.apply(totals, active, packageName, className,
                    timestamp, start, end, isForegroundEvent(event.getEventType()),
                    isBackgroundEvent(event.getEventType()));
        }
        UsageEventAccumulator.closeActive(totals, active, end);
        return totals;
    }

    private boolean isForegroundEvent(int type) {
        return type == UsageEvents.Event.MOVE_TO_FOREGROUND
                || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && type == UsageEvents.Event.ACTIVITY_RESUMED);
    }

    private boolean isBackgroundEvent(int type) {
        return type == UsageEvents.Event.MOVE_TO_BACKGROUND
                || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                && (type == UsageEvents.Event.ACTIVITY_PAUSED || type == UsageEvents.Event.ACTIVITY_STOPPED));
    }

    private boolean isScreenOffEvent(int type) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                && type == UsageEvents.Event.SCREEN_NON_INTERACTIVE;
    }

    private void drawUsageRows(Canvas c, float w, float y, int primary, int muted, int faint) {
        long end = System.currentTimeMillis();
        long start = prefs.getBoolean("sinceFullActive", false)
                ? prefs.getLong("sinceFullStartAt", end - 24 * 60 * 60 * 1000L)
                : prefs.getLong(charging ? "lastDischargeStartAt" : "dischargeStartAt", end - 24 * 60 * 60 * 1000L);
        if (start >= end) start = end - 60 * 60 * 1000L;
        List<AppUsageRow> rows = appUsageRows(start, end);
        long totalForegroundMs = 0L;
        for (AppUsageRow row : rows) totalForegroundMs += row.foregroundMs;
        int totalEnergy = dischargeMah();
        Map<String, Integer> directMah = telemetryAppMahByPackage(start, end);
        int directTotalMah = 0;
        for (Integer value : directMah.values()) directTotalMah += Math.max(0, value);
        int row = 0;
        for (AppUsageRow usage : rows) {
            String app = usage.packageName;
            try { app = getContext().getPackageManager().getApplicationLabel(getContext().getPackageManager().getApplicationInfo(usage.packageName, 0)).toString(); } catch (Exception ignored) { }
            long minutes = usage.foregroundMs / 60000L;
            float infoWidth = Math.max(70f, Math.min(130f, (w - 72f) / 2f));
            float appWidth = Math.max(72f, w - 72f - infoWidth - 8f);
            text(c, fitText(app, appWidth, 10, true), 36, y + row * 27, 10, primary, true);
            int appMah = BatteryAppAttribution.estimateMah(
                    directMah.containsKey(usage.packageName) ? directMah.get(usage.packageName) : 0,
                    directTotalMah, totalEnergy, usage.foregroundMs, totalForegroundMs);
            rightText(c, fitText(minutes + " Min. · " + (appMah > 0 ? "~" + appMah : "—") + " mAh gesch.", infoWidth, 8, false), w - 36, y + row * 27, 8, muted, false);
            line(c, 36, y + row * 27 + 9, w - 36, y + row * 27 + 9, Color.rgb(43, 47, 56), 1);
            if (++row == 3) break;
        }
        if (row == 0) text(c, "Seit dem Trennen keine App-Nutzung erfasst.", 36, y, 9, faint, false);
        text(c, "Tippen für alle App-Details", 36, y + 87, 9, lime, true);
    }

    private void showAppUsageDetails() {
        if (!hasUsageAccess()) {
            try { getContext().startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, Uri.parse("package:" + getContext().getPackageName()))); }
            catch (Exception ignored) { getContext().startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)); }
            return;
        }
        long end = System.currentTimeMillis();
        long start = prefs.getBoolean("sinceFullActive", false)
                ? prefs.getLong("sinceFullStartAt", end - 24 * 60 * 60 * 1000L)
                : prefs.getLong(charging ? "lastDischargeStartAt" : "dischargeStartAt", end - 24 * 60 * 60 * 1000L);
        if (start >= end) start = end - 60 * 60 * 1000L;
        List<AppUsageRow> rows = appUsageRows(start, end);
        long totalForegroundMs = 0L;
        for (AppUsageRow row : rows) totalForegroundMs += row.foregroundMs;
        int totalEnergy = Math.max(0, dischargeMah());
        Map<String, Integer> directMah = telemetryAppMahByPackage(start, end);
        int directTotalMah = 0;
        for (Integer value : directMah.values()) directTotalMah += Math.max(0, value);
        StringBuilder details = new StringBuilder("Vordergrundzeit seit Beginn des aktuellen Entladevorgangs.\n"
                + "mAh sind zeit-/telemetriebasierte Schätzungen, keine echten Android-Pro-App-Messungen.\n\n");
        int row = 0;
        for (AppUsageRow usage : rows) {
            String app = usage.packageName;
            try { app = getContext().getPackageManager().getApplicationLabel(getContext().getPackageManager().getApplicationInfo(usage.packageName, 0)).toString(); } catch (Exception ignored) { }
            long minutes = usage.foregroundMs / 60000L;
            int appMah = BatteryAppAttribution.estimateMah(
                    directMah.containsKey(usage.packageName) ? directMah.get(usage.packageName) : 0,
                    directTotalMah, totalEnergy, usage.foregroundMs, totalForegroundMs);
            details.append(app).append("\n").append(minutes).append(" Min. · ")
                    .append(appMah > 0 ? "~" + appMah + " mAh geschätzt" : "mAh nicht verfügbar")
                    .append("\n\n");
            if (++row == 50) break;
        }
        if (row == 0) details.append("Seit dem Trennen keine App-Nutzung erfasst.");
        new AlertDialog.Builder(getContext()).setTitle("App-Details").setMessage(details.toString()).setPositiveButton("Schließen", null).show();
    }

    /** Estimate direct app-attributed drain from local telemetry intervals. */
    private Map<String, Integer> telemetryAppMahByPackage(long start, long end) {
        String saved = telemetryPrefs.getString("telemetrySamples", "");
        Map<String, Integer> totals = new HashMap<>();
        if (saved.isEmpty()) return totals;
        ArrayList<String> rows = BatteryExportRules.validTelemetryRows(saved);
        for (int index = 0; index < rows.size(); index++) {
            String row = rows.get(index);
            String[] parts = row.split(",", 11);
            if (parts.length < 9 || parts[8].trim().isEmpty()) continue;
            try {
                long timestamp = Long.parseLong(parts[0]);
                if (timestamp < start || timestamp > end || "1".equals(parts[2])) continue;
                int current = Math.abs(Integer.parseInt(parts[3]));
                if (current <= 0) continue;
                long intervalEnd = end;
                if (index + 1 < rows.size()) {
                    String[] nextParts = rows.get(index + 1).split(",", 2);
                    try { intervalEnd = Long.parseLong(nextParts[0]); } catch (NumberFormatException ignored) { }
                }
                if (intervalEnd <= timestamp) intervalEnd = timestamp + samplingIntervalMs();
                intervalEnd = Math.min(end, Math.min(intervalEnd, timestamp + 2L * 60L * 60L * 1000L));
                if (intervalEnd > timestamp) {
                    int added = BatteryAppAttribution.sampleMah(
                            current, intervalEnd - timestamp, appAttributionIntervalCapMs());
                    totals.put(parts[8], totals.containsKey(parts[8])
                            ? totals.get(parts[8]) + added : added);
                }
            } catch (NumberFormatException ignored) { }
        }
        return totals;
    }

    /** Matches the monitor's stale-sample policy without claiming a long gap for one app. */
    private long appAttributionIntervalCapMs() {
        return Math.max(30L * 60L * 1000L, samplingIntervalMs());
    }

    private void drawHealthPage(Canvas c, float w, float h, int panel, int raised, int border, int primary, int muted, int faint) {
        float y = 182;
        rounded(c, 18, y, w - 18, y + 300, 12, panel); stroke(c, border, 1); rect.set(u(18), u(y), u(w - 18), u(y + 300)); c.drawRoundRect(rect, u(12), u(12), p);
        text(c, "AKKUGESUNDHEIT", 36, y + 31, 10, muted, true);
        int health = healthPercent();
        int design = designCapacityMah();
        text(c, health == 0 ? "Nicht gemessen" : (health > 80 ? "Guter Zustand" : "Prüfung nötig"), 36, y + 62, 23, primary, true);
        text(c, "Geschätzte Kapazität", 36, y + 102, 10, muted, false);
        text(c, health > 0 ? mahDisplay(estimatedCapacityMah()) : "—", 36, y + 132, 28, lime, true);
        text(c, design > 0 ? "von " + mahDisplay(design) + " Nennkapazität" : "Nennkapazität nicht verfügbar", 36, y + 153, 10, muted, false);
        rounded(c, 36, y + 181, w - 36, y + 187, 3, border);
        if (health > 0) rounded(c, 36, y + 181, 36 + (w - 72) * health / 100f, y + 187, 3, lime);
        String healthStatus = health > 0 ? health + "% Kapazität" : (w < 340f ? "Benchmark starten" : "Benchmark für Kapazität starten");
        text(c, fitText(healthStatus, w < 340f ? 104f : w - 190f, 10, true), 36, y + 211, 10, primary, true);
        text(c, w < 340f ? "Alterung" : "Akkualterung", w < 340f ? w - 90 : w - 145, y + 211, 10, muted, false);
        text(c, health > 0 ? (100 - Math.min(100, health)) + "%" : "—", w - 58, y + 211, 10, amber, true);
        line(c, 36, y + 232, w - 36, y + 232, border, 1);
        text(c, "Android-Zustand", 36, y + 257, 10, muted, false);
        String platformLabel = BatteryPlatformHealth.label(platformHealth);
        if (BatteryCapacityLevel.isAvailable(capacityLevel)) {
            platformLabel += " · " + BatteryCapacityLevel.label(capacityLevel);
        }
        if (!technology.isEmpty()) platformLabel += " · " + technology;
        boundedRightText(c, platformLabel, w * .50f, w - 36, y + 257, 10,
                BatteryPlatformHealth.isAvailable(platformHealth) ? lime : faint, true);
        if (w < 390f) {
            text(c, "T min/Ø/max", 36, y + 275, 8, muted, false);
            rightText(c, telemetryTemperatureDisplay(), w - 36, y + 275, 8, amber, true);
            text(c, "Vollzyklen (EFC)", 36, y + 292, 8, muted, false);
            rightText(c, totalEquivalentCycles(), w - 36, y + 292, 8, blue, true);
        } else {
            text(c, "Telemetrie T min/Ø/max", 36, y + 282, 9, muted, false);
            rightText(c, telemetryTemperatureDisplay(), w * .48f, y + 282, 9, amber, true);
            text(c, "Vollzyklen (EFC)", w * .55f, y + 282, 9, muted, false);
            rightText(c, totalEquivalentCycles(), w - 36, y + 282, 9, blue, true);
        }
        drawStat(c, 18, y + 316, (w - 48) / 2f, 105, "Spannung", voltageDisplay(), voltage > 0f ? "V" : "", blue, primary, muted, border, panel, "bolt");
        drawStat(c, 30 + (w - 48) / 2f, y + 316, (w - 48) / 2f, 105, "Ladezyklen", chargeCyclesDisplay(), "", Color.rgb(180, 154, 255), primary, muted, border, panel, "grid");
        rounded(c, 18, y + 438, w - 18, y + 520, 12, panel); stroke(c, border, 1); rect.set(u(18), u(y + 438), u(w - 18), u(y + 520)); c.drawRoundRect(rect, u(12), u(12), p);
        text(c, "So entsteht die Schätzung", 36, y + 468, 10, muted, true);
        boundedText(c, "Kapazität aus " + healthMeasurementSource(), 36, w * .53f, y + 493, 9, primary, false);
        text(c, "Messungen · letzter Ladevorgang " + lastChargeEquivalentCycles(), 36, y + 510, 9, primary, false);
        rightText(c, "Gesamt geladen: " + (totalChargedMah() > 0 ? totalChargedMah() + " mAh" : "—"), w - 30, y + 493, 8, blue, true);
        rightText(c, "Äquivalente Zyklen: " + totalEquivalentCycles(), w - 30, y + 512, 8, blue, true);
        if (manufactureDate.isAvailable()) {
            rightText(c, "Herstellung: " + manufactureDate.label(), w - 30, y + 530, 8, faint, false);
        }
        rounded(c, 18, y + 548, w - 18, y + 615, 12, panel); stroke(c, border, 1); rect.set(u(18), u(y + 548), u(w - 18), u(y + 615)); c.drawRoundRect(rect, u(12), u(12), p);
        text(c, benchmarkActive ? "Benchmark läuft" : "Manueller Benchmark", 36, y + 575, 11, primary, true);
        text(c, benchmarkActive ? "Zum Abschluss über 95 % laden" : "Für beste Ergebnisse unter 25 % starten", 36, y + 595, 9, muted, false);
        smoothButton(c, w - 132, y + 558, w - 36, y + 602, 14,
                benchmarkActive ? raised : lime, border, lime,
                !benchmarkActive, isPressed(30));
        centeredText(c, benchmarkActive ? "Aktiv" : "Start", w - 84, y + 586, 10,
                benchmarkActive ? lime : Color.rgb(23, 28, 16), true);
        rounded(c, 18, y + 630, w - 18, y + 697, 12, panel); stroke(c, border, 1); rect.set(u(18), u(y + 630), u(w - 18), u(y + 697)); c.drawRoundRect(rect, u(12), u(12), p);
        text(c, "Nennkapazität", 36, y + 659, 11, primary, true);
            text(c, designCapacitySource(), 36, y + 680, 9, muted, false);
        rightText(c, designCapacityDisplay(), w - 30, y + 667, 10, design > 0 ? lime : muted, true);
        rounded(c, 18, y + 710, w - 18, y + 850, 12, panel); stroke(c, border, 1); rect.set(u(18), u(y + 710), u(w - 18), u(y + 850)); c.drawRoundRect(rect, u(12), u(12), p);
        text(c, "Kapazitätsmessungen", 36, y + 740, 12, primary, true);
        String gaugeDiagnostics = internalResistance.isAvailable()
                ? "ESR " + internalResistanceDisplay() : "";
        if (capacityErrorMargin.isAvailable()) {
            gaugeDiagnostics += (gaugeDiagnostics.isEmpty() ? "" : " · ")
                    + "Unsicherheit " + capacityErrorMargin.label();
        }
        boundedRightText(c, gaugeDiagnostics,
                w * .48f, w - 36, y + 740, 8, blue, true);
        if (design <= 0) {
            text(c, "Nennkapazität festlegen, um den Trend zu normieren.", 36, y + 781, 9, muted, false);
        } else if (healthSamples.size() < 2) {
            text(c, "Schließe weitere Ladevorgänge für den Trend ab.", 36, y + 781, 9, muted, false);
        } else {
            float chartX = 36, chartY = y + 758, chartW = w - 72, chartH = 58;
            line(c, chartX, chartY + chartH, chartX + chartW, chartY + chartH, border, 1);
            Path trend = new Path();
            for (int i = 0; i < healthSamples.size(); i++) {
                float normalized = Math.max(0f, Math.min(1f, healthSamples.get(i) / (float) design));
                float px = chartX + chartW * i / Math.max(1, healthSamples.size() - 1);
                // The health chart uses the same physical 0–100% scale as
                // the value and progress bar. Do not retain the old 110%
                // headroom: it makes a full-capacity sample look artificially
                // low and suggests that health can exceed the real ceiling.
                float py = chartY + chartH - normalized * chartH;
                if (i == 0) trend.moveTo(u(px), u(py)); else trend.lineTo(u(px), u(py));
            }
            stroke(c, lime, 2); c.drawPath(trend, p);
            text(c, healthSamples.get(0) + " mAh", chartX, y + 835, 8, faint, false);
            text(c, healthSamples.get(healthSamples.size() - 1) + " mAh", w - 92, y + 835, 8, faint, false);
        }
        rounded(c, 18, y + 870, w - 18, y + 1045, 12, panel); stroke(c, border, 1); rect.set(u(18), u(y + 870), u(w - 18), u(y + 1045)); c.drawRoundRect(rect, u(12), u(12), p);
        text(c, "TÄGLICHE VOLLZYKLEN", 36, y + 900, 10, muted, true);
        text(c, "Gesamtzähler im Tagesverlauf", 36, y + 922, 9, primary, false);
        if (cycleHistory.isEmpty()) {
            text(c, "Noch keine täglichen Zykluswerte verfügbar.", 36, y + 975, 9, muted, false);
            text(c, "Die Überwachung zeichnet sie ab dem nächsten Messpunkt auf.", 36, y + 995, 8, faint, false);
        } else {
            int first = Math.max(0, cycleHistory.size() - 30);
            int count = cycleHistory.size() - first;
            float minCycles = Float.MAX_VALUE;
            float maxCycles = -Float.MAX_VALUE;
            for (int i = first; i < cycleHistory.size(); i++) {
                minCycles = Math.min(minCycles, cycleHistory.get(i).cycles);
                maxCycles = Math.max(maxCycles, cycleHistory.get(i).cycles);
            }
            float chartX = 36, chartY = y + 938, chartW = w - 72, chartH = 54;
            line(c, chartX, chartY + chartH, chartX + chartW, chartY + chartH, border, 1);
            Path trend = new Path();
            float range = Math.max(1f, maxCycles - minCycles);
            for (int i = 0; i < count; i++) {
                float normalized = (cycleHistory.get(first + i).cycles - minCycles) / range;
                float px = chartX + chartW * i / Math.max(1, count - 1);
                float py = chartY + chartH - normalized * chartH;
                if (i == 0) trend.moveTo(u(px), u(py)); else trend.lineTo(u(px), u(py));
            }
            stroke(c, blue, 2); c.drawPath(trend, p);
            text(c, cycleHistory.get(first).date, chartX, y + 1010, 7, faint, false);
            text(c, cycleHistory.get(cycleHistory.size() - 1).date, w - 92, y + 1010, 7, faint, false);
            rightText(c, cycleHistorySourceDisplay(), w - 30, y + 1030, 8, faint, false);
        }
        rounded(c, 18, y + 1065, w - 18, y + 1240, 12, panel); stroke(c, border, 1); rect.set(u(18), u(y + 1065), u(w - 18), u(y + 1240)); c.drawRoundRect(rect, u(12), u(12), p);
        text(c, "LADEVERSCHLEISS", 36, y + 1095, 10, muted, true);
        text(c, "Äquivalente Vollzyklen je Ladevorgang", 36, y + 1117, 9, primary, false);
        ArrayList<String[]> wearRows = chargeWearRows();
        if (wearRows.isEmpty()) {
            text(c, "Schließe einen Ladevorgang für den lokalen Trend ab.", 36, y + 1175, 9, muted, false);
        } else {
            float chartX = 36, chartY = y + 1133, chartW = w - 72, chartH = 74;
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
                if (i == 0 || i == wearRows.size() - 1) text(c, wearRows.get(i)[3], x, y + 1225, 7, faint, false);
            }
        }
    }

    private void drawHistoryPage(Canvas c, float w, float h, int panel, int raised, int border, int primary, int muted, int faint) {
        float y = 182;
        int rowCount = Math.min(150, sessions.size());
        float listBottom = y + 160 + rowCount * 44f;
        float panelBottom = Math.max(y + 610, listBottom + 250);
        rounded(c, 18, y, w - 18, panelBottom, 12, panel); stroke(c, border, 1); rect.set(u(18), u(y), u(w - 18), u(panelBottom)); c.drawRoundRect(rect, u(12), u(12), p);
        text(c, "VERLAUF", 36, y + 31, 10, muted, true);
        text(c, w < 340f ? "Sitzungsverlauf" : "Lade-/Entladesitzungen", 36, y + 61, w < 340f ? 19 : 20, primary, true);
        text(c, "Lokal gespeichert · bis zu 150", 36, y + 83, 10, faint, false);
        line(c, 36, y + 105, w - 36, y + 105, border, 1);
        if (sessions.isEmpty()) {
            text(c, "Noch keine Sitzungen abgeschlossen.", 36, y + 145, 11, primary, true);
            text(c, "Lass die Überwachung laufen, um", 36, y + 171, 10, muted, false);
            text(c, "lokale Verlaufseinträge zu erstellen.", 36, y + 189, 10, muted, false);
        } else {
            text(c, "Datum", 36, y + 130, 9, faint, true);
            text(c, "Typ", w * .53f, y + 130, 9, faint, true);
            text(c, "Änderung", w * .71f, y + 130, 9, faint, true);
            text(c, "Dauer", w - 75, y + 130, 9, faint, true);
            int row = 0;
            for (String session : sessions) {
                String[] parts = session.split(",", -1);
                if (parts.length < 4) continue;
                float rowY = y + 160 + row * 44;
                line(c, 36, rowY - 18, w - 36, rowY - 18, border, 1);
                text(c, parts[3], 36, rowY, 9, muted, false);
                text(c, sessionTypeDisplay(parts[0]), w * .53f, rowY, 9, parts[0].equals("Charge") ? lime : blue, true);
                text(c, parts[1], w * .71f, rowY, 9, primary, true);
                text(c, parts[2], w - 75, rowY, 9, faint, false);
                if (++row == rowCount) break;
            }
        }
        float summaryY = listBottom + 35;
        line(c, 36, summaryY - 20, w - 36, summaryY - 20, border, 1);
        text(c, "Messwerte aufgezeichnet", 36, summaryY + 13, 10, muted, false);
        text(c, String.valueOf(longHistory.size()), w - 75, summaryY + 13, 11, lime, true);
        text(c, "Zeitraum: bis zu 30 lokale Tage", 36, summaryY + 39, 9, faint, false);
        text(c, "Tiefschlaf", 36, summaryY + 69, 10, muted, false);
        text(c, deepSleepTime(), w - 75, summaryY + 69, 11, Color.rgb(180, 154, 255), true);
        boundedText(c, "Sitzungen: " + sessionCount("Charge") + " Laden · " + sessionCount("Discharge") + " Entladen",
                36, w - 36, summaryY + 99, 9, primary, true);
        boundedText(c, "Energie: " + sessionEnergyDisplay("Charge", "+") + " / " + sessionEnergyDisplay("Discharge", "-"),
                36, w - 36, summaryY + 121, 9, blue, true);
        BatteryTelemetryDiagnostics.Summary diagnostics = telemetryDiagnostics();
        boundedText(c, "Diagnose: " + telemetryDiagnosticDisplay(diagnostics), 36, w - 36, summaryY + 187, 8,
                diagnosticsColor(diagnostics), false);
        boundedText(c, "Akkumesswerte bleiben auf diesem Gerät.", 36, w - 36, summaryY + 143, 9, primary, true);
        boundedText(c, "Export nur auf deine Auswahl; kein Konto/Abonnement.", 36, w - 36, summaryY + 165, 8, muted, false);
        float exportTop = historyExportTop();
        smoothButton(c, w - 156, exportTop, w - 36, exportTop + 44, 14,
                lime, lime, lime, true, isPressed(40));
        drawArrow(c, w - 140, exportTop + 22, Color.rgb(23, 28, 16));
        text(c, "CSV exportieren", w - 127, exportTop + 27, 9, Color.rgb(23, 28, 16), true);
    }

    private BatteryTelemetryDiagnostics.Summary telemetryDiagnostics() {
        return BatteryTelemetryDiagnostics.analyze(
                telemetryPrefs.getString("telemetrySamples", ""), samplingIntervalMs());
    }

    private String telemetryDiagnosticDisplay(BatteryTelemetryDiagnostics.Summary summary) {
        if (!summary.hasSamples()) return "noch keine Telemetrie";
        StringBuilder result = new StringBuilder(summary.sampleCount + " Messwerte");
        if (summary.maxTemperatureTenths > 0) {
            result.append(" · T ");
            if (summary.hasTemperatureData()) {
                result.append(String.format(Locale.GERMANY, "%.1f/%.1f/%.1f°C",
                        summary.minTemperatureTenths / 10f,
                        summary.averageTemperatureTenths / 10f,
                        summary.maxTemperatureTenths / 10f));
            } else {
                result.append("max ").append(String.format(Locale.GERMANY, "%.1f°C",
                        summary.maxTemperatureTenths / 10f));
            }
        }
        if (summary.hasVoltageData()) {
            result.append(" · min ").append(String.format(Locale.GERMANY, "%.2fV",
                    summary.minDischargeVoltageMv / 1000f));
        }
        if (summary.powerStats.hasData()) {
            result.append(" · P ");
            if (summary.powerStats.charging.isAvailable()) {
                result.append("L ").append(String.format(Locale.GERMANY, "%.1fW",
                        summary.powerStats.charging.averageMw / 1000f));
            }
            if (summary.powerStats.discharging.isAvailable()) {
                if (summary.powerStats.charging.isAvailable()) result.append(" / ");
                result.append("E ").append(String.format(Locale.GERMANY, "%.1fW",
                        summary.powerStats.discharging.averageMw / 1000f));
            }
        }
        if (summary.hasHighTemperature()) result.append(" · Wärme prüfen");
        if (summary.samplingGap) result.append(" · Datenlücke");
        return result.toString();
    }

    private int diagnosticsColor(BatteryTelemetryDiagnostics.Summary summary) {
        return summary.hasHighTemperature() || summary.samplingGap
                ? Color.rgb(242, 179, 106) : Color.rgb(157, 224, 106);
    }

    private String telemetryTemperatureDisplay() {
        BatteryTelemetryDiagnostics.Summary summary = telemetryDiagnostics();
        return summary.hasTemperatureData()
                ? String.format(Locale.GERMANY, "%.1f/%.1f/%.1f°C",
                summary.minTemperatureTenths / 10f,
                summary.averageTemperatureTenths / 10f,
                summary.maxTemperatureTenths / 10f) : "—";
    }

    private void exportHistory() {
        ((MainActivity) getContext()).createCsvExport();
    }

    String historyCsv() {
        StringBuilder csv = new StringBuilder("type,change,duration,date,start_level,end_level,energy_mah,equivalent_full_cycles,screen_on_value,screen_off_value,screen_on_duration_min,screen_off_duration_min,deep_sleep_min,charger_source,start_timestamp_ms,end_timestamp_ms,screen_wakeups\n");
        for (String session : sessions) appendCsvRow(csv, session.split(",", -1));
        csv.append("\nlevel_percent\n");
        for (Integer point : longHistory) appendCsvRow(csv, new String[]{String.valueOf(point)});
        csv.append("\ncycle_date,total_cycles,source\n");
        for (BatteryCycleHistory.Point point : cycleHistory) {
            appendCsvRow(csv, new String[]{point.date, String.format(Locale.US, "%.3f", point.cycles), point.source});
        }
        csv.append("\ntelemetry_timestamp_ms,level_percent,charging,current_ma,temperature_c,voltage_v,charge_counter_mah,screen_on,foreground_package,system_cycle_count,plugged,battery_power_mw\n");
        String telemetry = telemetryPrefs.getString("telemetrySamples", "");
        for (String row : BatteryExportRules.validTelemetryRows(telemetry)) {
            String[] parts = row.split(",", -1);
            String[] exportParts = Arrays.copyOf(parts, parts.length + 1);
            exportParts[parts.length] = String.valueOf(BatteryPowerStats.milliWatts(parts));
            appendCsvRow(csv, exportParts);
        }
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
        details.append(sessionTypeDisplay(parts[0])).append("-Sitzung\n");
        details.append("Datum: ").append(parts[3]).append('\n');
        details.append("Änderung: ").append(parts[1]).append('\n');
        details.append("Dauer: ").append(parts[2]);
        if (parts.length >= 7) {
            details.append("\nStart: ").append(parts[4]).append('%');
            details.append("\nEnde: ").append(parts[5]).append('%');
            details.append("\nEnergie: ").append(parts[6]).append(" mAh");
            if (parts.length >= 8) details.append("\nÄquivalente Vollzyklen: ").append(parts[7]);
        }
        if (parts.length >= 14) {
            String unit = charge ? "mAh" : "%";
            String on = parts[8];
            String off = parts[9];
            if (!charge) {
                try { on = String.format(Locale.US, "%.1f", Integer.parseInt(parts[8]) / 10f); } catch (NumberFormatException ignored) { }
                try { off = String.format(Locale.US, "%.1f", Integer.parseInt(parts[9]) / 10f); } catch (NumberFormatException ignored) { }
            }
            details.append("\nBildschirm an: ").append(on).append(' ').append(unit);
            details.append("\nBildschirm aus: ").append(off).append(' ').append(unit);
            details.append("\nZeit Bildschirm an: ").append(parts[10]).append(" Min.");
            details.append("\nZeit Bildschirm aus: ").append(parts[11]).append(" Min.");
            if (!charge) details.append("\nTiefschlaf: ").append(parts[12]).append(" Min.");
            if (charge) details.append("\nLadequelle: ").append(parts[13]);
            if (parts.length >= 16) {
                details.append("\nGestartet: ").append(formatTimestamp(parts[14]));
                details.append("\nBeendet: ").append(formatTimestamp(parts[15]));
            }
            if (parts.length >= 17) details.append("\nBildschirm-Aufweckungen: ").append(parts[16]);
        }
        new AlertDialog.Builder(getContext())
                .setTitle("Sitzungsdetails")
                .setMessage(details.toString())
                .setNegativeButton("Schließen", null)
                .setPositiveButton(charge ? "Laden öffnen" : "Entladen öffnen", (dialog, which) -> {
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
            return new SimpleDateFormat("dd.MM., HH:mm", Locale.GERMANY).format(new Date(timestamp));
        } catch (NumberFormatException ignored) {
            return value;
        }
    }

    private String sessionTypeDisplay(String type) {
        return "Charge".equals(type) ? "Laden" : "Discharge".equals(type) ? "Entladen" : type;
    }

    private void drawStat(Canvas c, float x, float y, float width, float height, String label, String value, String unit, int accent, int primary, int muted, int border, int panel, String icon) {
        frame(c, x, y, x + width, y + height, panel, border, accent);
        if (BatteryMetricLayout.shouldStack(width)) {
            // Two-column cards become too narrow on small phones. Stack the
            // compact card content instead of letting icon, label and value
            // compete for the same horizontal row.
            rounded(c, x + 10, y + 12, x + 38, y + 40, 8, Color.argb(28, Color.red(accent), Color.green(accent), Color.blue(accent)));
            if (icon.equals("bolt")) drawBolt(c, x + 24, y + 26, accent, .65f); else if (icon.equals("temp")) drawThermometer(c, x + 24, y + 26, accent); else if (icon.equals("heart")) drawHeart(c, x + 24, y + 26, accent); else if (icon.equals("arrow")) drawArrow(c, x + 24, y + 26, accent); else if (icon.equals("grid")) drawGrid(c, x + 24, y + 26, accent); else drawClock(c, x + 24, y + 26, accent);
            String compactLabel = label.replace("Akkugesundheit", "Gesundheit")
                    .replace("Akkutemperatur", "Temperatur")
                    .replace("Bildschirmzeit", "Screenzeit")
                    .replace("Geladene Energie", "Energie");
            boundedText(c, compactLabel, x + 10, x + width - 10, y + 58, 9, muted, false);
            type(9, muted, false);
            float compactUnitWidth = unit.isEmpty() ? 0f : p.measureText(unit) / density + 4f;
            String compactValue = fitText(value, Math.max(20f, width - 20f - compactUnitWidth), 17, true);
            boundedText(c, compactValue, x + 10, x + width - 10 - compactUnitWidth, y + 86, 17, primary, true);
            if (!unit.isEmpty()) {
                type(17, primary, true);
                boundedText(c, unit, x + 10 + p.measureText(compactValue) / density + 3,
                        x + width - 8, y + 86, 9, muted, false);
            }
            return;
        }
        rounded(c, x + 15, y + 16, x + 45, y + 46, 8, Color.argb(28, Color.red(accent), Color.green(accent), Color.blue(accent)));
        if (icon.equals("bolt")) drawBolt(c, x + 30, y + 31, accent, .7f); else if (icon.equals("temp")) drawThermometer(c, x + 30, y + 31, accent); else if (icon.equals("heart")) drawHeart(c, x + 30, y + 31, accent); else if (icon.equals("arrow")) drawArrow(c, x + 30, y + 31, accent); else if (icon.equals("grid")) drawGrid(c, x + 30, y + 31, accent); else drawClock(c, x + 30, y + 31, accent);
        boundedText(c, label, x + 58, x + width - 12, y + 30, 10, muted, false);
        type(10, muted, false);
        float unitWidth = unit.isEmpty() ? 0f : p.measureText(unit) / density + 4f;
        String fittedValue = fitText(value, Math.max(24f, width - 72f - unitWidth), 21, true);
        boundedText(c, fittedValue, x + 58, x + width - 12 - unitWidth, y + 62, 21, primary, true);
        if (!unit.isEmpty()) {
            type(21, primary, true);
            float valueWidth = p.measureText(fittedValue) / density;
            boundedText(c, unit, x + 58 + valueWidth + 4, x + width - 8, y + 62, 10, muted, false);
        }
    }

    private void drawChart(Canvas c, float x, float y, float width, float height, int panel, int border, int primary, int muted, int faint) {
        frame(c, x, y, x + width, y + height, panel, border, lime);
        text(c, historyDays == 30 ? "Akkustand · 30 Tage" : "Akkustand · 7 Tage", x + 18, y + 28, 15, primary, true);
        smoothButton(c, x + width - 104, y + 10, x + width - 56, y + 42, 11,
                panel, border, lime, historyDays == 7,
                isPressed(BatteryAccessibilityLayout.OVERVIEW_7D));
        smoothButton(c, x + width - 52, y + 10, x + width - 12, y + 42, 11,
                panel, border, lime, historyDays == 30,
                isPressed(BatteryAccessibilityLayout.OVERVIEW_30D));
        centeredText(c, "7D", x + width - 80, y + 30, 8, historyDays == 7 ? Color.rgb(23, 28, 16) : muted, true);
        centeredText(c, "30D", x + width - 32, y + 30, 8, historyDays == 30 ? Color.rgb(23, 28, 16) : muted, true);
        float chartX = x + 18, chartY = y + 51, chartW = width - 36, chartH = 94;
        for (int i = 0; i < 3; i++) line(c, chartX, chartY + i * 45, chartX + chartW, chartY + i * 45, border, 1);
        rightText(c, "100%", x + width - 18, chartY + 9, 7, faint, false);
        rightText(c, "50%", x + width - 18, chartY + 54, 7, faint, false);
        rightText(c, "0%", x + width - 18, chartY + 99, 7, faint, false);
        ArrayList<LevelPoint> points = chartPoints();
        if (points.isEmpty()) {
            text(c, "Warte auf lokale Messwerte.", chartX, chartY + 52, 10, faint, false);
        } else {
            long end = System.currentTimeMillis();
            long start = end - chartWindowMs();
            int first = Math.max(0, points.size() - 48);
            Path path = new Path();
            boolean pathStarted = false;
            for (int i = first; i < points.size(); i++) {
                LevelPoint point = points.get(i);
                float timeFraction = Math.max(0f, Math.min(1f, (point.timestamp - start) / (float) chartWindowMs()));
                float px = u(chartX + timeFraction * chartW);
                float py = u(chartY + chartH - point.level / 100f * chartH);
                if (!pathStarted) {
                    path.moveTo(px, py);
                    pathStarted = true;
                } else if (BatteryTimelineRules.isSamplingGap(
                        points.get(i - 1).timestamp, point.timestamp, samplingIntervalMs())) {
                    // Missing monitoring data is not a measured ramp.
                    path.moveTo(px, py);
                } else {
                    path.lineTo(px, py);
                }
            }
            stroke(c, lime, 2); c.drawPath(path, p);
            LevelPoint last = points.get(points.size() - 1);
            float lastFraction = Math.max(0f, Math.min(1f, (last.timestamp - start) / (float) chartWindowMs()));
            fill(c, lime);
            c.drawCircle(u(chartX + lastFraction * chartW), u(chartY + chartH - last.level / 100f * chartH), u(4), p);
        }
        text(c, chartAxisLabel(0f), chartX, y + 166, 9, faint, false); text(c, chartAxisLabel(.33f), chartX + chartW * .32f, y + 166, 9, faint, false); text(c, chartAxisLabel(.66f), chartX + chartW * .64f, y + 166, 9, faint, false); text(c, chartAxisLabel(1f), chartX + chartW - 23, y + 166, 9, faint, false);
        text(c, "Ø " + chartAverage() + " · Spanne " + chartRange(), x + 18, y + 189, 8, muted, false);
        text(c, "Letzte Sitzungen", x + 18, y + 204, 13, primary, true);
        if (sessions.isEmpty()) {
            text(c, "Nach dem ersten Zyklus sichtbar.", x + 18, y + 230, 9, faint, false);
        } else {
            int row = 0;
            for (String session : sessions) {
                if (row == 4) break;
                String[] parts = session.split(",", -1);
                if (parts.length < 4) continue;
                float rowY = y + 230 + row * 25;
                line(c, x + 18, rowY - 14, x + width - 18, rowY - 14, border, 1);
                text(c, parts[3], x + 18, rowY, 9, muted, false);
                text(c, sessionTypeDisplay(parts[0]), x + width * .53f, rowY, 9, parts[0].equals("Charge") ? lime : blue, false);
                text(c, parts[1], x + width * .72f, rowY, 9, primary, true);
                text(c, parts[2], x + width - 62, rowY, 9, faint, false);
                row++;
            }
            if (sessions.size() > row) text(c, "Weitere Sitzungen im Verlauf", x + 18, y + 337, 8, faint, false);
        }
    }

    private void drawTelemetryChart(Canvas c, float x, float y, float width, float height,
                                    int panel, int border, int primary, int muted, int faint,
                                    boolean chargingFilter) {
        frame(c, x, y, x + width, y + height, panel, border, chargingFilter ? lime : blue);
        text(c, chargingFilter ? "Ladestrom" : "Entladestrom", x + 18, y + 28, 15, primary, true);
        ArrayList<CurrentPoint> points = currentChartPoints(chargingFilter);
        int first = Math.max(0, points.size() - 48);
        int count = points.size() - first;
        float chartX = x + 18, chartY = y + 51, chartW = width - 36, chartH = 105;
        for (int i = 0; i < 3; i++) line(c, chartX, chartY + i * chartH / 2f, chartX + chartW, chartY + i * chartH / 2f, border, 1);
        rightText(c, "mA", x + width - 18, chartY + 9, 7, faint, false);
        rightText(c, "0", x + width - 18, chartY + chartH - 2, 7, faint, false);
        if (count == 0) {
            text(c, "Warte auf lokale Telemetrie.", chartX, chartY + 57, 10, faint, false);
            return;
        }
        ArrayList<Integer> magnitudes = new ArrayList<>();
        for (int i = first; i < points.size(); i++) {
            int current = points.get(i).magnitudeMa;
            magnitudes.add(current);
        }
        BatteryCurrentStats.Summary stats = BatteryCurrentStats.summarize(magnitudes);
        int scaleMax = Math.max(100, stats.maximumMa);
        Path path = new Path();
        long startAt = points.get(first).timestamp;
        long endAt = points.get(points.size() - 1).timestamp;
        long span = endAt - startAt;
        for (int i = 0; i < count; i++) {
            CurrentPoint point = points.get(first + i);
            float timeFraction = span > 0L
                    ? Math.max(0f, Math.min(1f, (point.timestamp - startAt) / (float) span))
                    : (count == 1 ? 0f : i / (float) (count - 1));
            float px = chartX + timeFraction * chartW;
            // A long collection gap is real missing data, not a slow ramp.
            // Keep the gap visible instead of drawing a misleading diagonal.
            boolean gap = i > 0 && BatteryTimelineRules.isSamplingGap(
                    points.get(first + i - 1).timestamp, point.timestamp, samplingIntervalMs());
            int current = point.magnitudeMa;
            float py = chartY + chartH - current * chartH / (float) scaleMax;
            if (i == 0 || gap) path.moveTo(u(px), u(py)); else path.lineTo(u(px), u(py));
        }
        stroke(c, chargingFilter ? lime : blue, 2);
        c.drawPath(path, p);
        boundedText(c, "Min " + stats.minimumMa + " · Ø " + stats.averageMa
                        + " · Max " + stats.maximumMa + " mA",
                chartX, x + width - 18, y + 181, 8, muted, false);
        text(c, "letzte " + count + " lokalen Messwerte", chartX, y + 199, 8, faint, false);
    }

    private static final class CurrentPoint {
        final long timestamp;
        final int magnitudeMa;

        CurrentPoint(long timestamp, int magnitudeMa) {
            this.timestamp = timestamp;
            this.magnitudeMa = magnitudeMa;
        }
    }

    /** Reads directionally consistent current points without losing timestamps. */
    private ArrayList<CurrentPoint> currentChartPoints(boolean chargingFilter) {
        ArrayList<CurrentPoint> points = new ArrayList<>();
        String saved = telemetryPrefs.getString("telemetrySamples", "");
        if (saved.isEmpty()) return points;
        for (String row : BatteryExportRules.validTelemetryRows(saved)) {
            String[] parts = row.split(",", 11);
            if (parts.length < 4 || !String.valueOf(chargingFilter ? 1 : 0).equals(parts[2])) continue;
            try {
                long timestamp = Long.parseLong(parts[0].trim());
                int signedCurrent = Integer.parseInt(parts[3].trim());
                // Telemetry keeps direction explicit: charging is positive,
                // discharging is negative. The chart shows magnitude while
                // retaining the selected direction in its title/filter.
                boolean matchesDirection = chargingFilter ? signedCurrent > 0 : signedCurrent < 0;
                int magnitude = Math.abs(signedCurrent);
                if (matchesDirection && magnitude > 0) points.add(new CurrentPoint(timestamp, magnitude));
            } catch (NumberFormatException ignored) { }
        }
        return points;
    }

    private static final class LevelPoint {
        final long timestamp;
        final int level;

        LevelPoint(long timestamp, int level) {
            this.timestamp = timestamp;
            this.level = level;
        }
    }

    private long chartWindowMs() {
        return (historyDays == 30 ? 30L : 7L) * 24L * 60L * 60L * 1000L;
    }

    /** Uses timestamped telemetry; old APKs fall back to their level history. */
    private ArrayList<LevelPoint> chartPoints() {
        ArrayList<LevelPoint> points = new ArrayList<>();
        long end = System.currentTimeMillis();
        long start = end - chartWindowMs();
        String saved = telemetryPrefs.getString("telemetrySamples", "");
        if (!saved.isEmpty()) {
            for (String row : BatteryExportRules.validTelemetryRows(saved)) {
                String[] parts = row.split(",", 3);
                if (parts.length < 2) continue;
                try {
                    long timestamp = Long.parseLong(parts[0]);
                    if (timestamp < start || timestamp > end) continue;
                    int level = BatteryLevel.normalizePercent(Integer.parseInt(parts[1].trim()));
                    if (level >= 0) points.add(new LevelPoint(timestamp, level));
                } catch (NumberFormatException ignored) { }
            }
        }
        if (!points.isEmpty()) return points;
        ArrayList<Integer> fallback = historyDays == 30 ? longHistory : history;
        long fallbackEnd = System.currentTimeMillis();
        long fallbackStart = fallbackEnd - Math.max(0, fallback.size() - 1) * samplingIntervalMs();
        for (int i = 0; i < fallback.size(); i++) {
            points.add(new LevelPoint(fallbackStart + i * samplingIntervalMs(),
                    Math.max(0, Math.min(100, fallback.get(i)))));
        }
        return points;
    }

    private ArrayList<Integer> chartLevels() {
        ArrayList<Integer> values = new ArrayList<>();
        for (LevelPoint point : chartPoints()) values.add(point.level);
        return values;
    }

    private String chartAxisLabel(float fraction) {
        long end = System.currentTimeMillis();
        long start = end - (historyDays == 30 ? 30L : 7L) * 24L * 60L * 60L * 1000L;
        String pattern = historyDays == 30 ? "d. MMM" : "EEE";
        return new SimpleDateFormat(pattern, Locale.GERMANY).format(new Date(start + (long) ((end - start) * fraction)));
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

    private String pageName() { return page == 1 ? "Laden" : page == 2 ? "Entladen" : page == 3 ? "Akkugesundheit" : page == 4 ? "Verlauf" : "Übersicht"; }

    private String levelDisplay() { return percentDisplay(level); }
    private String percentDisplay(int value) { return value >= 0 && value <= 100 ? value + "%" : "—"; }

    private String batteryModeLabel() {
        return level < 0 ? "Nicht verfügbar" : (charging ? "Laden" : "Akkubetrieb");
    }

    private String batteryChipLabel() {
        return level < 0 ? "NICHT BEREIT" : (charging ? "LÄDT JETZT" : "AKKUBETRIEB");
    }

    private String batteryRowLabel() {
        return level < 0 ? "Akku nicht verfügbar" : (charging ? "Laden erkannt" : "Akkubetrieb");
    }

    private void updateAccessibilitySummary() {
        String state = level < 0 ? "Akku nicht verfügbar" : (charging ? "Laden erkannt" : "Akkubetrieb");
        int health = healthPercent();
        String capacity = BatteryCapacityLevel.isAvailable(capacityLevel)
                ? " Kapazitätsniveau " + BatteryCapacityLevel.label(capacityLevel) + "." : "";
        String chargingProfile = charging && BatteryChargingState.isSpecial(chargingStatus)
                ? " Ladeprofil " + BatteryChargingState.label(chargingStatus) + "." : "";
        setContentDescription(pageName() + ". " + state + ". Akkustand "
                + (level >= 0 ? level + " Prozent" : "nicht verfügbar") + ". "
                + "Akkugesundheit " + (health > 0 ? health + " Prozent" : "nicht gemessen") + ". "
                + "Android-Zustand " + BatteryPlatformHealth.label(platformHealth) + "." + capacity
                + chargingProfile
                + " Tabs: Übersicht, Laden, Entladen, Gesundheit, Verlauf. Aktiver Tab: " + pageName() + ".");
        sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED);
    }

    private boolean isVisibleVirtualView(int virtualViewId) {
        if (virtualViewId >= 10 && virtualViewId <= 14) return true;
        if (virtualViewId == BatteryHeaderLayout.OVERFLOW
                || virtualViewId == BatteryHeaderLayout.THEME) return true;
        if (virtualViewId == BatteryHeaderLayout.LIVE_REFRESH) return getWidth() / density >= 390f;
        return BatteryAccessibilityLayout.isVisible(virtualViewId, page);
    }

    private String virtualViewLabel(int virtualViewId) {
        if (virtualViewId == BatteryHeaderLayout.OVERFLOW) return "Einstellungen";
        if (virtualViewId == BatteryHeaderLayout.THEME) return light ? "Dunkles Design" : "Helles Design";
        if (virtualViewId == BatteryHeaderLayout.LIVE_REFRESH) return "Live-Daten aktualisieren";
        if (BatteryAccessibilityLayout.isVisible(virtualViewId, page)) {
            return BatteryAccessibilityLayout.label(virtualViewId, historyDays == 30,
                    chargeAlarm, overlayEnabled, benchmarkActive, chargeLimit);
        }
        String[] labels = getWidth() / density < 480f
                ? new String[]{"Start", "Laden", "Entladen", "Gesundheit", "Verlauf"}
                : new String[]{"Übersicht", "Laden", "Entladen", "Gesundheit", "Verlauf"};
        return virtualViewId >= 10 && virtualViewId <= 14 ? labels[virtualViewId - 10] : "";
    }

    private Rect virtualViewBounds(int virtualViewId) {
        float w = getWidth() / density;
        if (virtualViewId == BatteryHeaderLayout.OVERFLOW) {
            return new Rect(Math.round((w < 390f ? w - 116f : w - 176f) * density),
                    Math.round(12f * density),
                    Math.round((w < 390f ? w - 68f : w - 128f) * density),
                    Math.round(60f * density));
        }
        if (virtualViewId == BatteryHeaderLayout.THEME) {
            return new Rect(Math.round((w < 390f ? w - 60f : w - 120f) * density),
                    Math.round(12f * density),
                    Math.round((w < 390f ? w - 12f : w - 72f) * density),
                    Math.round(60f * density));
        }
        if (virtualViewId == BatteryHeaderLayout.LIVE_REFRESH) {
            return new Rect(Math.round((w - 64f) * density), Math.round(12f * density),
                    Math.round((w - 16f) * density), Math.round(60f * density));
        }
        if (BatteryAccessibilityLayout.isVisible(virtualViewId, page)) {
            float bodyWidth = contentWidth(w);
            float bodyInset = contentInset(w);
            int[] bounds = BatteryAccessibilityLayout.bounds(virtualViewId, bodyInset, bodyWidth,
                    overviewChartTop(), historyExportTop());
            return new Rect(Math.round(bounds[0] * density), Math.round(bounds[1] * density),
                    Math.round(bounds[2] * density), Math.round(bounds[3] * density));
        }
        float cell = (w - 36f) / 5f;
        float left = 18f + (virtualViewId - 10) * cell + 4f;
        return new Rect(Math.round(left * density), Math.round(124f * density),
                Math.round((left + cell - 8f) * density), Math.round(164f * density));
    }

    private int virtualViewAt(float x, float y) {
        int header = BatteryHeaderLayout.actionAt(x, y, getWidth() / density);
        if (header != BatteryHeaderLayout.NONE && isVisibleVirtualView(header)) return header;
        float w = getWidth() / density;
        if (y >= 118f && y < 176f && x >= 18f && x <= w - 18f) {
            float cell = (w - 36f) / 5f;
            return 10 + Math.max(0, Math.min(4, (int) ((x - 18f) / cell)));
        }
        float bodyWidth = contentWidth(w);
        float bodyInset = contentInset(w);
        for (int id : BatteryAccessibilityLayout.pageControlsFor(page)) {
            int[] bounds = BatteryAccessibilityLayout.bounds(id, bodyInset, bodyWidth,
                    overviewChartTop(), historyExportTop());
            if (x >= bounds[0] && x <= bounds[2] && y >= bounds[1] && y <= bounds[3]) return id;
        }
        return AccessibilityNodeProvider.HOST_VIEW_ID;
    }

    private void performVirtualClick(int virtualViewId) {
        if (virtualViewId == BatteryHeaderLayout.OVERFLOW) {
            showSettings();
        } else if (virtualViewId == BatteryHeaderLayout.THEME) {
            light = !light;
            amoled = false;
            prefs.edit().putBoolean("lightTheme", light).putBoolean("amoledTheme", amoled).apply();
            applySystemBarTheme();
            invalidate();
            updateAccessibilitySummary();
        } else if (virtualViewId == BatteryHeaderLayout.LIVE_REFRESH) {
            Intent battery = ((Activity) getContext()).registerReceiver(
                    null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (battery != null) readBattery(battery);
            Toast.makeText(getContext(), "Live-Daten aktualisiert.", Toast.LENGTH_SHORT).show();
        } else if (virtualViewId >= 10 && virtualViewId <= 14) {
            page = virtualViewId - 10;
            updateAccessibilitySummary();
            updateLayoutHeight();
            invalidate();
        } else if (virtualViewId == BatteryAccessibilityLayout.OVERVIEW_7D
                || virtualViewId == BatteryAccessibilityLayout.OVERVIEW_30D) {
            historyDays = virtualViewId == BatteryAccessibilityLayout.OVERVIEW_30D ? 30 : 7;
            prefs.edit().putInt("historyDays", historyDays).apply();
            updateAccessibilitySummary();
            invalidate();
        } else if (virtualViewId == BatteryAccessibilityLayout.CHARGE_ALARM) {
            chargeAlarm = !chargeAlarm;
                    prefs.edit().putBoolean("chargeAlarm", chargeAlarm)
                            .remove("chargeAlarmSent").remove("chargeAlarmLastLevel").apply();
            if (!chargeAlarm) {
                android.app.NotificationManager manager = (android.app.NotificationManager)
                        getContext().getSystemService(Context.NOTIFICATION_SERVICE);
                if (manager != null) manager.cancel(8);
                prefs.edit().putBoolean("chargeAlarmSent", false).apply();
            }
            updateAccessibilitySummary();
            invalidate();
        } else if (virtualViewId == BatteryAccessibilityLayout.CHARGE_OVERLAY) {
            setOverlayEnabled(!overlayEnabled);
            updateAccessibilitySummary();
        } else if (virtualViewId == BatteryAccessibilityLayout.HEALTH_BENCHMARK) {
            toggleBenchmark();
        } else if (virtualViewId == BatteryAccessibilityLayout.HEALTH_CAPACITY) {
            editDesignCapacity();
        } else if (virtualViewId == BatteryAccessibilityLayout.DISCHARGE_USAGE) {
            showAppUsageDetails();
        } else if (virtualViewId == BatteryAccessibilityLayout.HISTORY_EXPORT) {
            exportHistory();
        }
    }

    private void setChargeLimitFromAccessibility(int requested) {
        chargeLimit = BatteryAccessibilityLayout.normalizeChargeLimit(requested);
        prefs.edit().putInt("chargeLimit", chargeLimit)
                .remove("chargeAlarmSent").remove("chargeAlarmLastLevel").apply();
        if (level < chargeLimit) {
            android.app.NotificationManager manager = (android.app.NotificationManager)
                    getContext().getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) manager.cancel(8);
        }
        updateAccessibilitySummary();
        invalidate();
    }

    private void bringVirtualViewIntoView(int virtualViewId) {
        if (!BatteryAccessibilityLayout.isVisible(virtualViewId, page)) return;
        requestRectangleOnScreen(virtualViewBounds(virtualViewId), true);
    }

    private final class DashboardAccessibilityNodeProvider extends AccessibilityNodeProvider {
        @Override public AccessibilityNodeInfo createAccessibilityNodeInfo(int virtualViewId) {
            if (virtualViewId == HOST_VIEW_ID) {
                AccessibilityNodeInfo host = AccessibilityNodeInfo.obtain(BatteryDashboard.this);
                BatteryDashboard.this.onInitializeAccessibilityNodeInfo(host);
                for (int id = 10; id <= 14; id++) host.addChild(BatteryDashboard.this, id);
                host.addChild(BatteryDashboard.this, BatteryHeaderLayout.OVERFLOW);
                host.addChild(BatteryDashboard.this, BatteryHeaderLayout.THEME);
                if (isVisibleVirtualView(BatteryHeaderLayout.LIVE_REFRESH)) {
                    host.addChild(BatteryDashboard.this, BatteryHeaderLayout.LIVE_REFRESH);
                }
                for (int id : BatteryAccessibilityLayout.pageControlsFor(page)) {
                    host.addChild(BatteryDashboard.this, id);
                }
                return host;
            }
            if (!isVisibleVirtualView(virtualViewId)) return null;
            AccessibilityNodeInfo node = AccessibilityNodeInfo.obtain(
                    BatteryDashboard.this, virtualViewId);
            String label = virtualViewLabel(virtualViewId);
            node.setPackageName(getContext().getPackageName());
            boolean toggle = virtualViewId == BatteryAccessibilityLayout.CHARGE_ALARM
                    || virtualViewId == BatteryAccessibilityLayout.CHARGE_OVERLAY;
            boolean chargeSlider = virtualViewId == BatteryAccessibilityLayout.CHARGE_LIMIT;
            node.setClassName(toggle ? "android.widget.Switch"
                    : (chargeSlider ? "android.widget.SeekBar" : "android.widget.Button"));
            node.setText(label);
            node.setContentDescription(label);
            node.setParent(BatteryDashboard.this);
            Rect boundsInParent = virtualViewBounds(virtualViewId);
            node.setBoundsInParent(boundsInParent);
            // Accessibility services use screen bounds for touch exploration.
            // The dashboard lives inside a scrolling parent, so derive them
            // from the current on-screen location instead of assuming the
            // Canvas starts at (0, 0).
            int[] location = new int[2];
            getLocationOnScreen(location);
            boundsInParent.offset(location[0], location[1]);
            node.setBoundsInScreen(boundsInParent);
            node.setVisibleToUser(isShown());
            node.setEnabled(isEnabled());
            node.setFocusable(true);
            node.setClickable(true);
            node.setSelected((virtualViewId >= 10 && virtualViewId - 10 == page)
                    || (virtualViewId == BatteryAccessibilityLayout.OVERVIEW_7D && historyDays == 7)
                    || (virtualViewId == BatteryAccessibilityLayout.OVERVIEW_30D && historyDays == 30));
            if (toggle) {
                node.setCheckable(true);
                node.setChecked(virtualViewId == BatteryAccessibilityLayout.CHARGE_ALARM
                        ? chargeAlarm : overlayEnabled);
            }
            if (chargeSlider) {
                node.setFocusable(true);
                node.setRangeInfo(AccessibilityNodeInfo.RangeInfo.obtain(
                        AccessibilityNodeInfo.RangeInfo.RANGE_TYPE_INT, 50f, 100f, chargeLimit));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    node.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS);
                }
                node.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SHOW_ON_SCREEN);
            } else {
                node.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK);
            }
            node.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_ACCESSIBILITY_FOCUS);
            node.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLEAR_ACCESSIBILITY_FOCUS);
            node.setAccessibilityFocused(accessibilityFocusedVirtualView == virtualViewId);
            return node;
        }

        @Override public boolean performAction(int virtualViewId, int action, Bundle arguments) {
            if (!isVisibleVirtualView(virtualViewId)) return false;
            if (action == AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS) {
                bringVirtualViewIntoView(virtualViewId);
                accessibilityFocusedVirtualView = virtualViewId;
                sendVirtualAccessibilityEvent(virtualViewId, AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED);
                return true;
            }
            if (action == AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS) {
                if (accessibilityFocusedVirtualView == virtualViewId) {
                    accessibilityFocusedVirtualView = HOST_VIEW_ID;
                    sendVirtualAccessibilityEvent(virtualViewId, AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED);
                }
                return true;
            }
            if (action == AccessibilityNodeInfo.AccessibilityAction.ACTION_SHOW_ON_SCREEN.getId()) {
                bringVirtualViewIntoView(virtualViewId);
                return true;
            }
            if (virtualViewId == BatteryAccessibilityLayout.CHARGE_LIMIT
                    && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                    && action == AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.getId()) {
                if (arguments == null || !arguments.containsKey(
                        AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE)) return false;
                bringVirtualViewIntoView(virtualViewId);
                float requested = arguments.getFloat(
                        AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE, chargeLimit);
                if (!Float.isFinite(requested)) return false;
                setChargeLimitFromAccessibility(Math.round(requested));
                sendVirtualAccessibilityEvent(virtualViewId, AccessibilityEvent.TYPE_VIEW_SELECTED);
                return true;
            }
            if (action != AccessibilityNodeInfo.ACTION_CLICK) return false;
            performVirtualClick(virtualViewId);
            sendVirtualAccessibilityEvent(virtualViewId, AccessibilityEvent.TYPE_VIEW_CLICKED);
            return true;
        }

        @Override public List<AccessibilityNodeInfo> findAccessibilityNodeInfosByText(
                String searched, int virtualViewId) {
            ArrayList<AccessibilityNodeInfo> result = new ArrayList<>();
            if (searched == null) return result;
            String query = searched.toLowerCase(Locale.GERMANY);
            for (int id = 1; id <= BatteryAccessibilityLayout.HISTORY_EXPORT; id++) {
                if (!isVisibleVirtualView(id)) continue;
                String label = virtualViewLabel(id);
                if (label.toLowerCase(Locale.GERMANY).contains(query)) {
                    AccessibilityNodeInfo node = createAccessibilityNodeInfo(id);
                    if (node != null) result.add(node);
                }
            }
            return result;
        }
    }

    private void sendVirtualAccessibilityEvent(int virtualViewId, int eventType) {
        AccessibilityEvent event = AccessibilityEvent.obtain(eventType);
        event.setPackageName(getContext().getPackageName());
        event.setClassName("android.widget.Button");
        event.getText().add(virtualViewLabel(virtualViewId));
        event.setSource(this, virtualViewId);
        ViewParent parent = getParent();
        if (parent != null) parent.requestSendAccessibilityEvent(this, event);
    }

    /** Lets touch-exploration users discover the same controls by hovering. */
    @Override public boolean dispatchHoverEvent(MotionEvent event) {
        AccessibilityManager manager = (AccessibilityManager) getContext()
                .getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (manager == null || !manager.isTouchExplorationEnabled()) {
            return super.dispatchHoverEvent(event);
        }
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_HOVER_EXIT) {
            if (hoveredVirtualView != AccessibilityNodeProvider.HOST_VIEW_ID) {
                sendVirtualAccessibilityEvent(hoveredVirtualView, AccessibilityEvent.TYPE_VIEW_HOVER_EXIT);
                hoveredVirtualView = AccessibilityNodeProvider.HOST_VIEW_ID;
            }
            return true;
        }
        if (action != MotionEvent.ACTION_HOVER_ENTER && action != MotionEvent.ACTION_HOVER_MOVE) {
            return true;
        }
        int virtualViewId = virtualViewAt(event.getX() / density, event.getY() / density);
        if (virtualViewId == AccessibilityNodeProvider.HOST_VIEW_ID) {
            if (hoveredVirtualView != AccessibilityNodeProvider.HOST_VIEW_ID) {
                sendVirtualAccessibilityEvent(hoveredVirtualView, AccessibilityEvent.TYPE_VIEW_HOVER_EXIT);
                hoveredVirtualView = AccessibilityNodeProvider.HOST_VIEW_ID;
            }
            return true;
        }
        if (hoveredVirtualView != virtualViewId) {
            if (hoveredVirtualView != AccessibilityNodeProvider.HOST_VIEW_ID) {
                sendVirtualAccessibilityEvent(hoveredVirtualView, AccessibilityEvent.TYPE_VIEW_HOVER_EXIT);
            }
            hoveredVirtualView = virtualViewId;
            sendVirtualAccessibilityEvent(virtualViewId, AccessibilityEvent.TYPE_VIEW_HOVER_ENTER);
        }
        return true;
    }

    private void drawGauge(Canvas c, float cx, float cy, float radius, int value, int primary, int faint) {
        int track = Color.argb(light ? 120 : 90, Color.red(faint), Color.green(faint), Color.blue(faint));
        stroke(c, track, 10); rect.set(u(cx - radius), u(cy - radius), u(cx + radius), u(cy + radius)); c.drawArc(rect, -90, 360, false, p);
        if (value < 0) return;
        stroke(c, lime, 10); c.drawArc(rect, -90, 360 * Math.max(0, Math.min(100, value)) / 100f, false, p);
        float angle = (float) Math.toRadians(-90 + 360 * Math.max(0, Math.min(100, value)) / 100f);
        fill(c, lime); c.drawCircle(u(cx + (float) Math.cos(angle) * radius), u(cy + (float) Math.sin(angle) * radius), u(5), p);
    }
    private void drawBolt(Canvas c, float cx, float cy, int color, float width) { Path b = new Path(); b.moveTo(u(cx + 3), u(cy - 12)); b.lineTo(u(cx - 6), u(cy + 1)); b.lineTo(u(cx), u(cy + 1)); b.lineTo(u(cx - 3), u(cy + 12)); b.lineTo(u(cx + 7), u(cy - 2)); b.lineTo(u(cx + 1), u(cy - 2)); b.close(); fill(c, color); c.drawPath(b, p); }
    private void drawSun(Canvas c, float cx, float cy, int color) { stroke(c, color, 1.5f); c.drawCircle(u(cx), u(cy), u(4), p); for (int i=0; i<8; i++) { double a=i*Math.PI/4; line(c, cx+(float)Math.cos(a)*7, cy+(float)Math.sin(a)*7, cx+(float)Math.cos(a)*10, cy+(float)Math.sin(a)*10, color, 1.3f); } }
    private void drawHeart(Canvas c, float cx, float cy, int color) { drawHeart(c, cx, cy, color, 1f); }
    private void drawHeart(Canvas c, float cx, float cy, int color, float scale) {
        Path path = new Path();
        path.moveTo(u(cx), u(cy + 8f * scale));
        path.cubicTo(u(cx - 16f * scale), u(cy - 2f * scale), u(cx - 9f * scale), u(cy - 11f * scale), u(cx), u(cy - 5f * scale));
        path.cubicTo(u(cx + 9f * scale), u(cy - 11f * scale), u(cx + 16f * scale), u(cy - 2f * scale), u(cx), u(cy + 8f * scale));
        stroke(c, color, 1.7f);
        c.drawPath(path, p);
    }
    private void drawThermometer(Canvas c, float cx, float cy, int color) { stroke(c,color,1.7f); c.drawRoundRect(new RectF(u(cx-3),u(cy-11),u(cx+3),u(cy+5)),u(3),u(3),p); c.drawCircle(u(cx),u(cy+7),u(5),p); line(c,cx,cy-7,cx,cy+6,color,1.7f); }
    private void drawClock(Canvas c, float cx, float cy, int color) { stroke(c,color,1.7f); c.drawCircle(u(cx),u(cy),u(9),p); line(c,cx,cy,cx,cy-5,color,1.7f); line(c,cx,cy,cx+4,cy+3,color,1.7f); }
    private void drawArrow(Canvas c, float cx, float cy, int color) { line(c,cx,cy-9,cx,cy+6,color,1.7f); line(c,cx-5,cy+1,cx,cy+6,color,1.7f); line(c,cx+5,cy+1,cx,cy+6,color,1.7f); }
    private void drawGrid(Canvas c, float cx, float cy, int color) { stroke(c,color,1.4f); c.drawRoundRect(new RectF(u(cx-8),u(cy-8),u(cx-1),u(cy-1)),u(1),u(1),p); c.drawRoundRect(new RectF(u(cx+1),u(cy-8),u(cx+8),u(cy-1)),u(1),u(1),p); c.drawRoundRect(new RectF(u(cx-8),u(cy+1),u(cx-1),u(cy+8)),u(1),u(1),p); c.drawRoundRect(new RectF(u(cx+1),u(cy+1),u(cx+8),u(cy+8)),u(1),u(1),p); }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            touchDownY = event.getY();
            lastTouchY = touchDownY;
            touchDragged = false;
            float w = getWidth() / density;
            pressedRegion = pressedRegionAt(event.getX() / density, event.getY() / density, w);
            invalidate();
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            float currentY = event.getY();
            float delta = lastTouchY - currentY;
            if (Math.abs(currentY - touchDownY) > 8f * density) {
                touchDragged = true;
                pressedRegion = 0;
                invalidate();
            }
            if (touchDragged && getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
            if (touchDragged && getParent() instanceof ScrollView) {
                ((ScrollView) getParent()).scrollBy(0, Math.round(delta));
            }
            lastTouchY = currentY;
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_CANCEL) {
            touchDragged = false;
            pressedRegion = 0;
            invalidate();
            if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
            return true;
        }
        if (event.getAction() != MotionEvent.ACTION_UP) return true;
        if (touchDragged) {
            touchDragged = false;
            pressedRegion = 0;
            if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
            return true;
        }
        performClick();
        float screenX = event.getX() / density, y = event.getY() / density;
        int releasedRegion = pressedRegion;
        pressedRegion = 0;
        invalidate();
        if (System.currentTimeMillis() - lastTouch < 80) return true;
        lastTouch = System.currentTimeMillis();
        float w = getWidth() / density;
        if (releasedRegion == BatteryHeaderLayout.OVERFLOW && y < 70) { showSettings(); return true; }
        if (releasedRegion == BatteryHeaderLayout.THEME && y < 70) { light = !light; amoled = false; prefs.edit().putBoolean("lightTheme", light).putBoolean("amoledTheme", amoled).apply(); applySystemBarTheme(); invalidate(); return true; }
        if (releasedRegion == BatteryHeaderLayout.LIVE_REFRESH && y < 70) {
            // The LIVE control is an explicit refresh action: Android's
            // sticky battery broadcast is read immediately, so the user can
            // verify the current state without waiting for the next sample.
            Intent battery = ((Activity) getContext()).registerReceiver(
                    null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (battery != null) readBattery(battery);
            Toast.makeText(getContext(), "Live-Daten aktualisiert.", Toast.LENGTH_SHORT).show();
            return true;
        }
        if (y >= 124 && y < 174) {
            float cell = (w - 36) / 5f;
            page = Math.max(0, Math.min(4, (int) ((screenX - 18) / cell)));
            updateAccessibilitySummary();
            updateLayoutHeight();
            invalidate();
            return true;
        }
        // Page content is centered on wide displays; convert screen coordinates
        // back into the same local coordinate system used by onDraw().
        float x = screenX - contentInset(w);
        float bodyW = contentWidth(w);
        if (page == 0 && y > overviewChartTop() + 8 && y < overviewChartTop() + 60 && x > bodyW - 140) {
            historyDays = historyDays == 7 ? 30 : 7;
            prefs.edit().putInt("historyDays", historyDays).apply();
            invalidate();
            return true;
        }
        if (page == 1 && y >= 462 && y < 504 && x >= bodyW - 145) {
            chargeAlarm = !chargeAlarm;
            prefs.edit().putBoolean("chargeAlarm", chargeAlarm)
                    .remove("chargeAlarmSent").remove("chargeAlarmLastLevel").apply();
            if (!chargeAlarm) {
                android.app.NotificationManager manager = (android.app.NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
                if (manager != null) manager.cancel(8);
                prefs.edit().putBoolean("chargeAlarmSent", false).apply();
            }
            invalidate();
            return true;
        }
        if (page == 1 && y > 365 && y < 410 && x >= 36 && x <= bodyW - 36) {
            setChargeLimitFromAccessibility(
                    Math.round((x - 36) / (bodyW - 72) * 100));
            return true;
        }
        if (page == 1 && y >= 508 && y < 552 && x >= bodyW - 145) {
            setOverlayEnabled(!overlayEnabled);
            return true;
        }
        int historyRows = Math.min(150, sessions.size());
        if (page == 4 && y >= 342 && y < 342 + historyRows * 44f && x < bodyW - 145 && !sessions.isEmpty()) {
            int index = (int) ((y - 342) / 44);
            showSessionDetails(index);
            return true;
        }
        if (page == 4 && y > historyExportTop() && y < historyExportTop() + 45 && x > bodyW - 155) {
            exportHistory();
            return true;
        }
        if (page == 3 && y > 700 && y < 815 && x > bodyW - 140) {
            toggleBenchmark();
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
