package com.ampere.batterylab;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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
    private static final String UPDATE_CHANNEL_ID = "ampere-updates";
    private static final int UPDATE_NOTIFICATION_ID = 10;
    private static final String ACTION_SHOW_UPDATE = "com.ampere.batterylab.SHOW_UPDATE";
    private static final String DOWNLOAD_ID = "downloadId";
    private static final String DOWNLOAD_SHA256 = "downloadSha256";
    private static final long CHECK_INTERVAL_MS = 12L * 60L * 60L * 1000L;
    private static final int MAX_MANIFEST_BYTES = 128 * 1024;
    private static final int MAX_RELEASE_NOTES_CHARS = 8 * 1024;
    private static final long MAX_APK_BYTES = 128L * 1024L * 1024L;
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    private UpdateChecker() { }

    static void check(Activity activity) {
        check(activity, false);
    }

    static void checkNow(Activity activity) {
        check(activity, true);
    }

    static boolean isUpdateIntent(Intent intent) {
        return intent != null && ACTION_SHOW_UPDATE.equals(intent.getAction());
    }

    /** Performs a throttled manifest-only check from the persistent monitor service. */
    static void checkInBackground(Context context) {
        String manifestUrl = BuildConfig.UPDATE_MANIFEST_URL;
        if (manifestUrl == null || manifestUrl.trim().isEmpty()) return;
        Context app = context.getApplicationContext();
        SharedPreferences prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long now = System.currentTimeMillis();
        if (now - prefs.getLong("lastBackgroundCheck", 0L) < CHECK_INTERVAL_MS) return;
        prefs.edit().putLong("lastBackgroundCheck", now).apply();
        EXECUTOR.execute(() -> {
            UpdateInfo update = fetch(manifestUrl);
            if (update == null) return;
            int notifiedVersion = prefs.getInt("notifiedVersionCode", 0);
            if (update.versionCode <= notifiedVersion) return;
            notifyUpdateAvailable(app, update);
            prefs.edit().putInt("notifiedVersionCode", update.versionCode).apply();
        });
    }

    private static void check(Activity activity, boolean force) {
        String manifestUrl = BuildConfig.UPDATE_MANIFEST_URL;
        if (manifestUrl == null || manifestUrl.trim().isEmpty()) return;

        SharedPreferences prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long now = System.currentTimeMillis();
        if (!force && now - prefs.getLong("lastCheck", 0L) < CHECK_INTERVAL_MS) return;
        prefs.edit().putLong("lastCheck", now).apply();

        WeakReference<Activity> activityRef = new WeakReference<>(activity);
        EXECUTOR.execute(() -> {
            UpdateInfo update = fetch(manifestUrl);
            Activity target = activityRef.get();
            if (target == null || target.isFinishing()) return;
            target.runOnUiThread(() -> {
                if (update != null) showUpdateDialog(target, update);
                else if (force) Toast.makeText(target, "Keine neue Aktualisierung gefunden (oder der Update-Server ist nicht erreichbar).", Toast.LENGTH_LONG).show();
            });
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
            connection.setInstanceFollowRedirects(false);
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) return null;
            long contentLength = connection.getContentLength();
            if (contentLength > MAX_MANIFEST_BYTES) return null;

            ByteArrayOutputStream body = new ByteArrayOutputStream();
            try (InputStream stream = connection.getInputStream()) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = stream.read(buffer)) != -1) {
                    if (body.size() + count > MAX_MANIFEST_BYTES) return null;
                    body.write(buffer, 0, count);
                }
            }
            JSONObject json = new JSONObject(body.toString(StandardCharsets.UTF_8.name()));
            int versionCode = json.optInt("versionCode", 0);
            String versionName = json.optString("versionName", "");
            String apkUrl = json.optString("apkUrl", "");
            String sha256 = json.optString("sha256", "").trim().toLowerCase(Locale.US);
            String notes = json.optString("releaseNotes", "Neue Version verfügbar.");
            if (versionCode <= BuildConfig.VERSION_CODE || versionName.isEmpty() || versionName.length() > 64
                    || apkUrl.isEmpty() || apkUrl.length() > 512 || notes.length() > MAX_RELEASE_NOTES_CHARS
                    || !sha256.matches("[0-9a-f]{64}")) return null;
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
        NotificationManager manager = (NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) manager.cancel(UPDATE_NOTIFICATION_ID);
        new AlertDialog.Builder(activity)
                .setTitle("Update verfügbar · " + update.versionName)
                .setMessage(update.notes + "\n\nDie APK wird kostenlos heruntergeladen. Android fragt anschließend noch einmal nach deiner Bestätigung.")
                .setNegativeButton("Später", null)
                .setPositiveButton("Herunterladen", (dialog, which) -> download(activity, update))
                .show();
    }

    private static void notifyUpdateAvailable(Context context, UpdateInfo update) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                manager.createNotificationChannel(new NotificationChannel(UPDATE_CHANNEL_ID, "App-Aktualisierungen", NotificationManager.IMPORTANCE_DEFAULT));
        }
        Intent open = new Intent(context, MainActivity.class).setAction(ACTION_SHOW_UPDATE)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pending = PendingIntent.getActivity(context, UPDATE_NOTIFICATION_ID, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(context, UPDATE_CHANNEL_ID) : new Notification.Builder(context);
        builder.setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("Ampere-Update verfügbar · " + update.versionName)
                .setContentText("Tippen, um die kostenlose Aktualisierung zu prüfen")
                .setStyle(new Notification.BigTextStyle().bigText(update.notes))
                .setContentIntent(pending)
                .setAutoCancel(true)
                .setShowWhen(false);
        manager.notify(UPDATE_NOTIFICATION_ID, builder.build());
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

    /**
     * Handles DownloadManager completion from a manifest receiver. This keeps
     * the update flow alive when Android has reclaimed the app process.
     */
    static void handleDownloadCompleted(Context receiverContext, Intent intent) {
        if (!DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) return;
        Context context = receiverContext.getApplicationContext();
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long expected = prefs.getLong(DOWNLOAD_ID, -1L);
        long received = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L);
        if (expected < 0L || expected != received) return;

        DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        if (manager == null) return;
        DownloadManager.Query query = new DownloadManager.Query().setFilterById(received);
        android.database.Cursor cursor = manager.query(query);
        boolean successful = false;
        long totalSize = -1L;
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    successful = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL;
                    int totalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);
                    if (totalIndex >= 0) totalSize = cursor.getLong(totalIndex);
                }
            } finally {
                cursor.close();
            }
        }
        if (!successful) {
            prefs.edit().remove(DOWNLOAD_ID).remove(DOWNLOAD_SHA256).apply();
            Toast.makeText(context, "Update-Download fehlgeschlagen.", Toast.LENGTH_LONG).show();
            return;
        }
        if (totalSize > MAX_APK_BYTES) {
            manager.remove(received);
            prefs.edit().remove(DOWNLOAD_ID).remove(DOWNLOAD_SHA256).apply();
            Toast.makeText(context, "Update verworfen: Datei ist zu groß.", Toast.LENGTH_LONG).show();
            return;
        }
        Uri apkUri = manager.getUriForDownloadedFile(received);
        if (apkUri == null) {
            Toast.makeText(context, "Update-Datei konnte nicht geöffnet werden.", Toast.LENGTH_LONG).show();
            return;
        }
        String expectedSha256 = prefs.getString(DOWNLOAD_SHA256, "");
        EXECUTOR.execute(() -> {
            boolean verified = verifySha256(context, apkUri, expectedSha256);
            new Handler(Looper.getMainLooper()).post(() -> {
                if (!verified) {
                    manager.remove(received);
                    prefs.edit().remove(DOWNLOAD_ID).remove(DOWNLOAD_SHA256).apply();
                    Toast.makeText(context, "Update verworfen: Integritätsprüfung fehlgeschlagen.", Toast.LENGTH_LONG).show();
                    return;
                }
                prefs.edit().remove(DOWNLOAD_ID).remove(DOWNLOAD_SHA256).apply();
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

    private static boolean verifySha256(Context context, Uri uri, String expected) {
        if (expected == null || !expected.matches("[0-9a-fA-F]{64}")) return false;
        try (InputStream stream = context.getContentResolver().openInputStream(uri)) {
            if (stream == null) return false;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int count;
            long total = 0L;
            while ((count = stream.read(buffer)) != -1) {
                total += count;
                if (total > MAX_APK_BYTES) return false;
                digest.update(buffer, 0, count);
            }
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
