package com.ampere.batterylab;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Checks for optional APK updates. No battery or usage data is sent. */
final class UpdateChecker {
    private static final String PREFS = "ampere-update";
    private static final String DOWNLOAD_ID = "downloadId";
    private static final String DOWNLOAD_SHA256 = "downloadSha256";
    private static final long CHECK_INTERVAL_MS = 12L * 60L * 60L * 1000L;
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static BroadcastReceiver downloadReceiver;

    private UpdateChecker() { }

    static void check(Activity activity) {
        String manifestUrl = BuildConfig.UPDATE_MANIFEST_URL;
        if (manifestUrl == null || manifestUrl.trim().isEmpty()) return;

        SharedPreferences prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long now = System.currentTimeMillis();
        if (now - prefs.getLong("lastCheck", 0L) < CHECK_INTERVAL_MS) return;
        prefs.edit().putLong("lastCheck", now).apply();

        WeakReference<Activity> activityRef = new WeakReference<>(activity);
        EXECUTOR.execute(() -> {
            UpdateInfo update = fetch(manifestUrl);
            Activity target = activityRef.get();
            if (update == null || target == null || target.isFinishing()) return;
            target.runOnUiThread(() -> showUpdateDialog(target, update));
        });
    }

    private static UpdateInfo fetch(String manifestUrl) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(manifestUrl);
            if (!isAllowedUpdateUrl(url)) return null;
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(7000);
            connection.setReadTimeout(7000);
            connection.setRequestMethod("GET");
            connection.setUseCaches(false);
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) return null;

            StringBuilder body = new StringBuilder();
            try (InputStream stream = connection.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
                String line;
                while ((line = reader.readLine()) != null) body.append(line);
            }
            JSONObject json = new JSONObject(body.toString());
            int versionCode = json.optInt("versionCode", 0);
            String versionName = json.optString("versionName", "");
            String apkUrl = json.optString("apkUrl", "");
            String sha256 = json.optString("sha256", "").trim().toLowerCase(Locale.US);
            String notes = json.optString("releaseNotes", "Neue Version verfügbar.");
            if (versionCode <= BuildConfig.VERSION_CODE || versionName.isEmpty() || apkUrl.isEmpty() || !sha256.matches("[0-9a-f]{64}")) return null;
            URL apk = new URL(apkUrl);
            if (!isAllowedUpdateUrl(apk)) return null;
            return new UpdateInfo(versionCode, versionName, apkUrl, sha256, notes);
        } catch (Exception ignored) {
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static boolean isAllowedUpdateUrl(URL url) {
        return "https".equalsIgnoreCase(url.getProtocol())
                && url.getPort() == -1
                && url.getUserInfo() == null
                && "raw.githubusercontent.com".equalsIgnoreCase(url.getHost())
                && url.getPath().startsWith("/Apfelkringel/ampere-battery-lab-updates/");
    }

    private static void showUpdateDialog(Activity activity, UpdateInfo update) {
        if (activity.isFinishing() || activity.isDestroyed()) return;
        new AlertDialog.Builder(activity)
                .setTitle("Update verfügbar · " + update.versionName)
                .setMessage(update.notes + "\n\nDie APK wird kostenlos heruntergeladen. Android fragt anschließend noch einmal nach deiner Bestätigung.")
                .setNegativeButton("Später", null)
                .setPositiveButton("Herunterladen", (dialog, which) -> download(activity, update))
                .show();
    }

    private static void download(Activity activity, UpdateInfo update) {
        DownloadManager manager = (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
        if (manager == null) {
            Toast.makeText(activity, "Download ist auf diesem Gerät nicht verfügbar.", Toast.LENGTH_LONG).show();
            return;
        }
        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(update.apkUrl));
            request.setTitle("Ampere-Update " + update.versionName);
            request.setDescription("Kostenloses Update wird heruntergeladen");
            request.setMimeType("application/vnd.android.package-archive");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(false);
            request.setDestinationInExternalFilesDir(activity, Environment.DIRECTORY_DOWNLOADS, "ampere-update-" + update.versionCode + ".apk");

            registerDownloadReceiver(activity, manager);
            long id = manager.enqueue(request);
            activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                    .putLong(DOWNLOAD_ID, id)
                    .putString(DOWNLOAD_SHA256, update.sha256)
                    .apply();
            Toast.makeText(activity, "Update wird heruntergeladen …", Toast.LENGTH_LONG).show();
        } catch (Exception error) {
            Toast.makeText(activity, "Update konnte nicht gestartet werden.", Toast.LENGTH_LONG).show();
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private static void registerDownloadReceiver(Activity activity, DownloadManager manager) {
        if (downloadReceiver != null) return;
        downloadReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                if (!DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) return;
                long expected = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(DOWNLOAD_ID, -1L);
                long received = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L);
                if (expected != received) return;

                DownloadManager.Query query = new DownloadManager.Query().setFilterById(received);
                android.database.Cursor cursor = manager.query(query);
                boolean successful = false;
                if (cursor != null) {
                    try {
                        successful = cursor.moveToFirst() && cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL;
                    } finally {
                        cursor.close();
                    }
                }
                if (!successful) {
                    Toast.makeText(context, "Update-Download fehlgeschlagen.", Toast.LENGTH_LONG).show();
                    return;
                }
                Uri apkUri = manager.getUriForDownloadedFile(received);
                if (apkUri == null) {
                    Toast.makeText(context, "Update-Datei konnte nicht geöffnet werden.", Toast.LENGTH_LONG).show();
                    return;
                }
                String expectedSha256 = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(DOWNLOAD_SHA256, "");
                EXECUTOR.execute(() -> {
                    boolean verified = verifySha256(context, apkUri, expectedSha256);
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (!verified) {
                            manager.remove(received);
                            Toast.makeText(context, "Update verworfen: Integritätsprüfung fehlgeschlagen.", Toast.LENGTH_LONG).show();
                            return;
                        }
                        Intent install = new Intent(Intent.ACTION_VIEW).setDataAndType(apkUri, "application/vnd.android.package-archive");
                        install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        try {
                            context.startActivity(install);
                        } catch (Exception ignored) {
                            Toast.makeText(context, "Bitte die heruntergeladene APK aus den Dateien öffnen.", Toast.LENGTH_LONG).show();
                        }
                    });
                });
            }
        };
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        // DownloadManager sends this system broadcast from a separate UID.
        if (Build.VERSION.SDK_INT >= 33) activity.getApplicationContext().registerReceiver(downloadReceiver, filter, Context.RECEIVER_EXPORTED);
        else activity.getApplicationContext().registerReceiver(downloadReceiver, filter);
    }

    private static boolean verifySha256(Context context, Uri uri, String expected) {
        if (expected == null || !expected.matches("[0-9a-fA-F]{64}")) return false;
        try (InputStream stream = context.getContentResolver().openInputStream(uri)) {
            if (stream == null) return false;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int count;
            while ((count = stream.read(buffer)) != -1) digest.update(buffer, 0, count);
            StringBuilder result = new StringBuilder(64);
            for (byte value : digest.digest()) result.append(String.format(Locale.US, "%02x", value));
            return result.toString().equalsIgnoreCase(expected);
        } catch (Exception ignored) {
            return false;
        }
    }

    private static final class UpdateInfo {
        final int versionCode;
        final String versionName;
        final String apkUrl;
        final String sha256;
        final String notes;

        UpdateInfo(int versionCode, String versionName, String apkUrl, String sha256, String notes) {
            this.versionCode = versionCode;
            this.versionName = versionName;
            this.apkUrl = apkUrl;
            this.sha256 = sha256;
            this.notes = notes;
        }
    }
}
