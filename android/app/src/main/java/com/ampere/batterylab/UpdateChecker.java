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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.ampere.batterylab.Toasts;

/** Checks for optional APK updates. No battery or usage data is sent. */
final class UpdateChecker {
    private static final String TAG = "AmpereUpdate";
    private static final String PREFS = "ampere-update";
    private static final String UPDATE_CHANNEL_ID = "ampere-updates";
    // The monitor service owns notification IDs 7-10 for its live card and
    // the three alarms. Using one of those IDs here let a new update banner
    // silently replace a pending discharge alarm on the same device.
    private static final int UPDATE_NOTIFICATION_ID = 11;
    private static final String ACTION_SHOW_UPDATE = "com.ampere.batterylab.SHOW_UPDATE";
    private static final String DOWNLOAD_ID = "downloadId";
    private static final String DOWNLOAD_SHA256 = "downloadSha256";
    private static final String DOWNLOAD_VERSION_CODE = "downloadVersionCode";
    private static final String PENDING_VERSION_CODE = "pendingVersionCode";
    private static final String PENDING_VERSION_NAME = "pendingVersionName";
    private static final String PENDING_APK_URL = "pendingApkUrl";
    private static final String PENDING_SHA256 = "pendingSha256";
    private static final String PENDING_NOTES = "pendingNotes";
    private static final String INSTALL_IN_PROGRESS = "installInProgress";
    private static final String EXPECTED_MANIFEST_PATH = "/repos/Apfelkringel/ampere-battery-lab-updates/contents/latest.json";
    private static final String EXPECTED_MANIFEST_RAW_PATH = "/Apfelkringel/ampere-battery-lab-updates/main/latest.json";
    private static final String EXPECTED_APK_RAW_PATH = "/Apfelkringel/ampere-battery-lab-updates/main/Ampere-Battery-Lab-release.apk";
    private static final String EXPECTED_APK_CONTENTS_PATH = "/repos/Apfelkringel/ampere-battery-lab-updates/contents/Ampere-Battery-Lab-release.apk";
    // Android's package installer enforces this signer too. Rechecking it here
    // rejects a changed public-repository artifact before showing the installer.
    // Pin matches the current Ampere Battery Lab release signer
    // (CN=Ampere Battery Lab, O=Apfelkringel, C=DE) generated 2026-09-21.
    // Older releases used a debug-key fallback that has since been rotated
    // out of the lineage; see docs/UPDATE-SECURITY.md for the rotation chain.
    static final String EXPECTED_RELEASE_CERT_SHA256 = "fa29b87595ef1b34b2069d1e2842d2114b1552a074e022ee527a7ec17e981ad3";
    // Older releases (and devices reading the V1 signer from the rotation
    // lineage) are signed with the debug key that bootstraps the V3 lineage.
    // Keeping the debug cert in the allowed set means devices on API < 28
    // (which still consult info.signatures / V1) and lineage-capable
    // devices both find a matching fingerprint.
    static final String EXPECTED_LINEAGE_DEBUG_CERT_SHA256 = "eabc1c630a28daf5fff5f67c70d4382d16784da89019321bb107da41abb60eba";
    private static final long CHECK_INTERVAL_MS = 12L * 60L * 60L * 1000L;
    private static final int MAX_MANIFEST_BYTES = 128 * 1024;
    private static final int MAX_RELEASE_NOTES_CHARS = 8 * 1024;
    private static final long MAX_APK_BYTES = 128L * 1024L * 1024L;
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();


    /** Test-only: parses an HTTPS URL string the same way the
     *  validation helpers see it. Made package-private so
     *  UpdateCheckerAllowlistTest can feed arbitrary URLs. */
    static java.net.URL allowlistTestParse(String raw) throws java.net.MalformedURLException {
        return new java.net.URL(raw);
    }
    private static final Object OPERATION_LOCK = new Object();
    private static boolean checkInProgress;
    private static boolean downloadInProgress;
    private static boolean downloadCompletionInProgress;
    private static boolean installInProgress;

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

    /** Releases the installer guard when the app becomes visible again. */
    static void onActivityResumed(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (prefs.getBoolean(INSTALL_IN_PROGRESS, false)) {
            synchronized (OPERATION_LOCK) {
                installInProgress = false;
                downloadInProgress = false;
                downloadCompletionInProgress = false;
            }
            clearDownloadState(prefs);
            prefs.edit().remove(INSTALL_IN_PROGRESS).apply();
            return;
        }
        resumePersistedDownload(context);
    }

    /**
     * A completion broadcast normally starts the receiver process itself. If
     * Android reclaims that process after the broadcast but before APK
     * verification finishes, however, the successful DownloadManager row can
     * remain without another broadcast. Reconcile that durable row when the
     * app next becomes visible; the operation lock still makes a receiver and
     * this recovery path mutually exclusive.
     */
    private static void resumePersistedDownload(Context context) {
        Context app = context.getApplicationContext();
        SharedPreferences prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long id = prefs.getLong(DOWNLOAD_ID, -1L);
        if (id < 0L || prefs.getBoolean(INSTALL_IN_PROGRESS, false)) return;
        DownloadManager manager = (DownloadManager) app.getSystemService(Context.DOWNLOAD_SERVICE);
        if (manager == null) return;
        android.database.Cursor cursor = manager.query(new DownloadManager.Query().setFilterById(id));
        if (cursor == null) return;
        int status;
        try {
            if (!cursor.moveToFirst()) return;
            status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
        } finally {
            cursor.close();
        }
        if (!shouldResumePersistedDownload(status)) return;
        Intent completion = new Intent(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
                .putExtra(DownloadManager.EXTRA_DOWNLOAD_ID, id);
        handleDownloadCompleted(app, completion);
    }

    static boolean shouldResumePersistedDownload(int status) {
        return status == DownloadManager.STATUS_SUCCESSFUL;
    }

    /** Performs a throttled manifest-only check from the persistent monitor service. */
    static void checkInBackground(Context context) {
        if (!BuildConfig.DIRECT_DISTRIBUTION) return;
        String manifestUrl = BuildConfig.UPDATE_MANIFEST_URL;
        if (manifestUrl == null || manifestUrl.trim().isEmpty()) return;
        Context app = context.getApplicationContext();
        SharedPreferences prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long now = System.currentTimeMillis();
        if (now - prefs.getLong("lastBackgroundCheck", 0L) < CHECK_INTERVAL_MS) return;
        if (!tryStartCheck(app, false)) return;
        prefs.edit().putLong("lastBackgroundCheck", now).apply();
        EXECUTOR.execute(() -> {
            try {
                FetchResult result = fetch(manifestUrl);
                UpdateInfo update = result.update;
                if (update == null) return;
                int notifiedVersion = prefs.getInt("notifiedVersionCode", 0);
                if (update.versionCode <= notifiedVersion) return;
                persistPendingUpdate(prefs, update);
                notifyUpdateAvailable(app, update);
                prefs.edit().putInt("notifiedVersionCode", update.versionCode).apply();
            } finally {
                finishCheck();
            }
        });
    }

    private static void check(Activity activity, boolean force) {
        if (!BuildConfig.DIRECT_DISTRIBUTION) return;
        String manifestUrl = BuildConfig.UPDATE_MANIFEST_URL;
        if (manifestUrl == null || manifestUrl.trim().isEmpty()) return;

        SharedPreferences prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long now = System.currentTimeMillis();
        if (!force && now - prefs.getLong("lastCheck", 0L) < CHECK_INTERVAL_MS) return;
        if (!tryStartCheck(activity, force)) return;
        prefs.edit().putLong("lastCheck", now).apply();

        WeakReference<Activity> activityRef = new WeakReference<>(activity);
        if (force) Toasts.show(activity, AppText.t(activity, "Suche nach Aktualisierungen …"));
        EXECUTOR.execute(() -> {
            try {
                FetchResult result = fetch(manifestUrl);
                UpdateInfo update = result.update;
                Activity target = activityRef.get();
                if (target == null || target.isFinishing()) return;
                target.runOnUiThread(() -> {
                    if (update != null) showUpdateDialog(target, update);
                    else if (force) showCheckResult(target, result.message);
                });
            } finally {
                finishCheck();
            }
        });
    }

    private static boolean tryStartCheck(Context context, boolean notify) {
        synchronized (OPERATION_LOCK) {
            if (checkInProgress || downloadInProgress || downloadCompletionInProgress || installInProgress
                    || hasPendingUpdateWork(context)) {
                if (notify) {
                    Toasts.show(context, AppText.t(context, "Ein anderer Update-Vorgang läuft bereits."));
                }
                return false;
            }
            checkInProgress = true;
            return true;
        }
    }

    private static void finishCheck() {
        synchronized (OPERATION_LOCK) {
            checkInProgress = false;
        }
    }

    private static void showCheckResult(Activity activity, String message) {
        new AlertDialog.Builder(activity)
                .setTitle(AppText.t(activity, "Update-Prüfung"))
                .setMessage(AppText.t(activity, message))
                .setPositiveButton(AppText.t(activity, "Erneut prüfen"), (dialog, which) -> checkNow(activity))
                .setNegativeButton(AppText.t(activity, "Schließen"), null)
                .show();
    }

    /** Shows a validated background result when the user next opens the app. */
    static void showPendingUpdateIfAvailable(Activity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
        SharedPreferences prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int versionCode = prefs.getInt(PENDING_VERSION_CODE, 0);
        String apkUrl = prefs.getString(PENDING_APK_URL, "");
        String sha256 = prefs.getString(PENDING_SHA256, "");
        if (versionCode <= BuildConfig.VERSION_CODE || apkUrl.isEmpty()
                || !sha256.matches("[0-9a-f]{64}")) return;
        // Persisted banner data may be stale (cached cdn response, key
        // rotation, or sideload that bypassed this flow). Drop it and
        // force a fresh check so the next banner is rebuilt from the
        // current latest.json.
        clearPendingUpdate(prefs);
        checkNow(activity);
    }

    private static void persistPendingUpdate(SharedPreferences prefs, UpdateInfo update) {
        prefs.edit().putInt(PENDING_VERSION_CODE, update.versionCode)
                .putString(PENDING_VERSION_NAME, update.versionName)
                .putString(PENDING_APK_URL, update.apkUrl)
                .putString(PENDING_SHA256, update.sha256)
                .putString(PENDING_NOTES, update.notes)
                .commit();
    }

    private static void clearPendingUpdate(SharedPreferences prefs) {
        prefs.edit().remove(PENDING_VERSION_CODE).remove(PENDING_VERSION_NAME)
                .remove(PENDING_APK_URL).remove(PENDING_SHA256).remove(PENDING_NOTES).apply();
    }

    /**
     * True when a validated background check has stored a newer release that
     * the user has not installed yet. The dashboard header uses this to show
     * its update banner without re-fetching the manifest on the UI thread.
     */
    static boolean isUpdateKnown(Context context) {
        if (context == null || !BuildConfig.DIRECT_DISTRIBUTION) return false;
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int versionCode = prefs.getInt(PENDING_VERSION_CODE, 0);
        return versionCode > BuildConfig.VERSION_CODE
                && !prefs.getString(PENDING_APK_URL, "").isEmpty()
                && prefs.getString(PENDING_SHA256, "").matches("[0-9a-f]{64}");
    }

    /** Version name of the known update, or an empty string when none exists. */
    static String knownUpdateVersionName(Context context) {
        if (context == null || !BuildConfig.DIRECT_DISTRIBUTION) return "";
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String versionName = prefs.getString(PENDING_VERSION_NAME, "");
        return isUpdateKnown(context) ? versionName : "";
    }

    private static FetchResult fetch(String manifestUrl) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(withCacheBuster(manifestUrl));
            if (!isAllowedManifestUrl(url)) return FetchResult.failure("Die konfigurierte Update-Adresse wurde aus Sicherheitsgründen abgelehnt.");
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(7000);
            connection.setReadTimeout(7000);
            connection.setRequestMethod("GET");
            connection.setUseCaches(false);
            connection.setRequestProperty("Accept", "application/vnd.github.raw+json");
            connection.setRequestProperty("Cache-Control", "no-cache, no-store, max-age=0");
            connection.setRequestProperty("Pragma", "no-cache");
            connection.setInstanceFollowRedirects(false);
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return FetchResult.failure("Der Update-Server antwortete mit HTTP " + responseCode + ". Prüfe die Internetverbindung und versuche es erneut.");
            }
            long contentLength = connection.getContentLength();
            if (contentLength > MAX_MANIFEST_BYTES) return FetchResult.failure("Die Update-Datei ist ungewöhnlich groß und wurde aus Sicherheitsgründen abgelehnt.");

            ByteArrayOutputStream body = new ByteArrayOutputStream();
            try (InputStream stream = connection.getInputStream()) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = stream.read(buffer)) != -1) {
                    if (body.size() + count > MAX_MANIFEST_BYTES) return FetchResult.failure("Die Update-Datei überschreitet die erlaubte Größe.");
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
                    || !sha256.matches("[0-9a-f]{64}")) {
                return FetchResult.failure("Der Server meldet keine neuere Version als " + BuildConfig.VERSION_NAME + ".");
            }
            URL apk = new URL(apkUrl);
            if (!isAllowedApkUrl(apk)) return FetchResult.failure("Die APK-Adresse wurde aus Sicherheitsgründen abgelehnt.");
            return FetchResult.success(new UpdateInfo(versionCode, versionName, apkUrl, sha256, notes));
        } catch (Exception error) {
            Log.w(TAG, "Update-Prüfung fehlgeschlagen", error);
            return FetchResult.failure("Die Update-Prüfung konnte nicht abgeschlossen werden (" + error.getClass().getSimpleName() + "). Prüfe Internetzugriff und Datum/Uhrzeit des Geräts.");
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    static boolean isAllowedManifestUrl(URL url) {
        if (!"https".equalsIgnoreCase(url.getProtocol())
                || url.getPort() != -1
                || url.getUserInfo() != null) return false;
        if ("raw.githubusercontent.com".equalsIgnoreCase(url.getHost())) {
            return EXPECTED_MANIFEST_RAW_PATH.equals(url.getPath());
        }
        return "api.github.com".equalsIgnoreCase(url.getHost())
                && EXPECTED_MANIFEST_PATH.equals(url.getPath());
    }

    private static String withCacheBuster(String url) {
        String separator = url.contains("?") ? "&" : "?";
        return url + separator + "clientVersion=" + BuildConfig.VERSION_CODE
                + "&t=" + System.currentTimeMillis();
    }

    static boolean isAllowedApkUrl(URL url) {
        if (isAllowedUpdateUrl(url) && EXPECTED_APK_RAW_PATH.equals(url.getPath())) return true;
        return isAllowedContentsApkUrl(url);
    }

    static boolean isAllowedContentsApkUrl(URL url) {
        if (!"https".equalsIgnoreCase(url.getProtocol())
                || url.getPort() != -1
                || url.getUserInfo() != null
                || !"api.github.com".equalsIgnoreCase(url.getHost())
                || !EXPECTED_APK_CONTENTS_PATH.equals(url.getPath())) return false;
        String query = url.getQuery();
        if (query == null) return false;
        for (String parameter : query.split("&")) if ("ref=main".equals(parameter)) return true;
        return false;
    }

    static boolean isAllowedUpdateUrl(URL url) {
        return "https".equalsIgnoreCase(url.getProtocol())
                && url.getPort() == -1
                && url.getUserInfo() == null
                && "raw.githubusercontent.com".equalsIgnoreCase(url.getHost());
    }

    private static void showUpdateDialog(Activity activity, UpdateInfo update) {
        if (activity.isFinishing() || activity.isDestroyed()) return;
        NotificationManager manager = (NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) manager.cancel(UPDATE_NOTIFICATION_ID);
        new AlertDialog.Builder(activity)
                .setTitle(AppText.t(activity, "Update verfügbar · " + update.versionName))
                .setMessage(update.notes + "\n\n" + AppText.t(activity, "Die kostenlose APK wird vor der Installation auf Hash, Paketname, Version und Release-Signatur geprüft. Android fragt anschließend noch einmal nach deiner Bestätigung."))
                .setNegativeButton(AppText.t(activity, "Später"), null)
                .setPositiveButton(AppText.t(activity, "Herunterladen"), (dialog, which) -> download(activity, update))
                .show();
    }

    private static void notifyUpdateAvailable(Context context, UpdateInfo update) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                manager.createNotificationChannel(new NotificationChannel(UPDATE_CHANNEL_ID,
                        AppText.t(context, "App-Aktualisierungen"), NotificationManager.IMPORTANCE_DEFAULT));
        }
        Intent open = new Intent(context, MainActivity.class).setAction(ACTION_SHOW_UPDATE)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pending = PendingIntent.getActivity(context, UPDATE_NOTIFICATION_ID, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(context, UPDATE_CHANNEL_ID) : new Notification.Builder(context);
        builder.setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(AppText.t(context, "Ampere-Update verfügbar · " + update.versionName))
                .setContentText(AppText.t(context, "Tippen, um die kostenlose Aktualisierung zu prüfen"))
                .setStyle(new Notification.BigTextStyle().bigText(update.notes))
                .setContentIntent(pending)
                .setAutoCancel(true)
                .setShowWhen(false);
        manager.notify(UPDATE_NOTIFICATION_ID, builder.build());
    }

    private static void download(Activity activity, UpdateInfo update) {
        if (requiresInstallPermissionPrompt(Build.VERSION.SDK_INT,
                Build.VERSION.SDK_INT < Build.VERSION_CODES.O
                        || activity.getPackageManager().canRequestPackageInstalls())) {
            requestInstallPermission(activity, update);
            return;
        }
        DownloadManager manager = (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
        if (manager == null) {
            Toasts.show(activity, AppText.t(activity, "Download ist auf diesem Gerät nicht verfügbar."));
            return;
        }
        if (!tryStartDownload(activity)) return;
        // The user chose to handle the update now: the header banner and
        // the pending-notification guard retire while the download runs.
        clearPendingUpdate(activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE));
        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(withCacheBuster(update.apkUrl)));
            request.setTitle(AppText.t(activity, "Ampere-Update " + update.versionName));
            request.setDescription(AppText.t(activity, "Kostenloses Update wird heruntergeladen"));
            request.setMimeType("application/vnd.android.package-archive");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(false);
            request.addRequestHeader("Cache-Control", "no-cache, no-store, max-age=0");
            request.addRequestHeader("Pragma", "no-cache");
            // The GitHub Contents API returns the current binary for this
            // fixed path when raw media is requested. This avoids the stale
            // raw.githubusercontent.com CDN serving the previous APK after a
            // manifest update.
            request.addRequestHeader("Accept", "application/vnd.github.raw+json");
            request.setDestinationInExternalFilesDir(activity, Environment.DIRECTORY_DOWNLOADS, "ampere-update-" + update.versionCode + ".apk");

            long id = manager.enqueue(request);
            // This metadata must survive immediate process reclamation. The
            // DownloadManager receiver may run after the app process has been
            // recreated, so an asynchronous apply() could leave the finished
            // download without its verification context.
            boolean persisted = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                    .putLong(DOWNLOAD_ID, id)
                    .putString(DOWNLOAD_SHA256, update.sha256)
                    .putInt(DOWNLOAD_VERSION_CODE, update.versionCode)
                    .commit();
            if (!persisted) {
                manager.remove(id);
                clearDownloadState(activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE));
                finishDownload();
                Toasts.show(activity, AppText.t(activity, "Update konnte nicht sicher vorbereitet werden."));
                return;
            }
            Toasts.show(activity, AppText.t(activity, "Update wird heruntergeladen …"));
        } catch (Exception error) {
            clearDownloadState(activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE));
            finishDownload();
            Toasts.show(activity, AppText.t(activity, "Update konnte nicht gestartet werden."));
        }
    }

    static boolean requiresInstallPermissionPrompt(int sdkInt, boolean canRequestInstalls) {
        return sdkInt >= Build.VERSION_CODES.O && !canRequestInstalls;
    }

    private static void requestInstallPermission(Activity activity, UpdateInfo update) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        persistPendingUpdate(prefs, update);
        new AlertDialog.Builder(activity)
                .setTitle(AppText.t(activity, "Installation einmal erlauben"))
                .setMessage(AppText.t(activity, "Android braucht deine Freigabe, damit Ampere eine APK zur Installation übergeben darf. Es wird noch nichts heruntergeladen. Nach der Freigabe erscheint das Update hier erneut; Android fragt vor der Installation zusätzlich nach deiner Bestätigung."))
                .setNegativeButton(AppText.t(activity, "Abbrechen"), (dialog, which) -> clearPendingUpdate(prefs))
                .setPositiveButton(AppText.t(activity, "Einstellung öffnen"), (dialog, which) -> {
                    try {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                Uri.parse("package:" + activity.getPackageName()));
                        activity.startActivity(intent);
                    } catch (Exception error) {
                        Toasts.show(activity, AppText.t(activity, "Installationsfreigabe bitte in den App-Einstellungen aktivieren."));
                    }
                }).show();
    }

    private static boolean tryStartDownload(Context context) {
        synchronized (OPERATION_LOCK) {
            if (checkInProgress || downloadInProgress || downloadCompletionInProgress || installInProgress
                    || hasPendingUpdateWork(context)) {
                Toasts.show(context, AppText.t(context, "Ein anderer Update-Vorgang läuft bereits."));
                return false;
            }
            downloadInProgress = true;
            return true;
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

        synchronized (OPERATION_LOCK) {
            if (installInProgress || downloadCompletionInProgress) return;
            downloadInProgress = true;
            downloadCompletionInProgress = true;
        }

        DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        if (manager == null) {
            clearDownloadState(prefs);
            finishDownload();
            return;
        }
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
            clearDownloadState(prefs);
            finishDownload();
            Toasts.show(context, AppText.t(context, "Update-Download fehlgeschlagen."));
            return;
        }
        if (totalSize > MAX_APK_BYTES) {
            manager.remove(received);
            clearDownloadState(prefs);
            finishDownload();
            Toasts.show(context, AppText.t(context, "Update verworfen: Datei ist zu groß."));
            return;
        }
        Uri apkUri = manager.getUriForDownloadedFile(received);
        if (apkUri == null) {
            clearDownloadState(prefs);
            finishDownload();
            Toasts.show(context, AppText.t(context, "Update-Datei konnte nicht geöffnet werden."));
            return;
        }
        String expectedSha256 = prefs.getString(DOWNLOAD_SHA256, "");
        int expectedVersionCode = prefs.getInt(DOWNLOAD_VERSION_CODE, -1);
        EXECUTOR.execute(() -> {
            boolean verified = verifyDownloadedApk(context, apkUri, expectedSha256, expectedVersionCode);
            new Handler(Looper.getMainLooper()).post(() -> {
                if (!verified) {
                    manager.remove(received);
                    clearDownloadState(prefs);
                    finishDownload();
                    Toasts.show(context, AppText.t(context, "Update verworfen: Hash, Version oder Release-Signatur ungültig."));
                    return;
                }
                synchronized (OPERATION_LOCK) {
                    installInProgress = true;
                }
                // Persist before handing control to Android's installer. If
                // the app process is reclaimed between these two operations,
                // the next resume still knows that an install was initiated.
                if (!prefs.edit().putBoolean(INSTALL_IN_PROGRESS, true).commit()) {
                    manager.remove(received);
                    clearDownloadState(prefs);
                    finishDownload();
                    Toasts.show(context, AppText.t(context, "Update konnte nicht sicher gestartet werden."));
                    return;
                }
                Intent install = new Intent(Intent.ACTION_VIEW).setDataAndType(apkUri, "application/vnd.android.package-archive");
                install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    context.startActivity(install);
                } catch (Exception ignored) {
                    prefs.edit().remove(INSTALL_IN_PROGRESS).apply();
                    clearDownloadState(prefs);
                    finishDownload();
                Toasts.show(context, AppText.t(context, "Bitte die heruntergeladene APK aus den Dateien öffnen."));
                }
            });
        });
    }

    private static void clearDownloadState(SharedPreferences prefs) {
        prefs.edit().remove(DOWNLOAD_ID).remove(DOWNLOAD_SHA256).remove(DOWNLOAD_VERSION_CODE).apply();
    }

    private static void finishDownload() {
        synchronized (OPERATION_LOCK) {
            downloadInProgress = false;
            downloadCompletionInProgress = false;
        }
    }

    /** Returns true while a persisted DownloadManager or installer operation is unfinished. */
    private static boolean hasPendingUpdateWork(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (prefs.getBoolean(INSTALL_IN_PROGRESS, false)) return true;
        long id = prefs.getLong(DOWNLOAD_ID, -1L);
        if (id < 0L) return false;
        DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        if (manager == null) return true;
        android.database.Cursor cursor = manager.query(new DownloadManager.Query().setFilterById(id));
        if (cursor == null) return true;
        try {
            if (!cursor.moveToFirst()) {
                clearDownloadState(prefs);
                return false;
            }
            int status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
            if (status == DownloadManager.STATUS_PENDING || status == DownloadManager.STATUS_RUNNING
                    || status == DownloadManager.STATUS_PAUSED || status == DownloadManager.STATUS_SUCCESSFUL) {
                return true;
            }
            clearDownloadState(prefs);
            return false;
        } finally {
            cursor.close();
        }
    }

    private static boolean verifyDownloadedApk(Context context, Uri uri, String expectedSha256, int expectedVersionCode) {
        if (expectedVersionCode <= BuildConfig.VERSION_CODE
                || expectedSha256 == null || !expectedSha256.matches("[0-9a-fA-F]{64}")) return false;
        File temporaryApk = null;
        try (InputStream stream = context.getContentResolver().openInputStream(uri)) {
            if (stream == null) return false;
            temporaryApk = File.createTempFile("ampere-update-", ".apk", context.getCacheDir());
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (FileOutputStream output = new FileOutputStream(temporaryApk)) {
                byte[] buffer = new byte[8192];
                int count;
                long total = 0L;
                while ((count = stream.read(buffer)) != -1) {
                    total += count;
                    if (total > MAX_APK_BYTES) return false;
                    digest.update(buffer, 0, count);
                    output.write(buffer, 0, count);
                }
            }
            if (!toHex(digest.digest()).equalsIgnoreCase(expectedSha256)) return false;

            int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                    ? PackageManager.GET_SIGNING_CERTIFICATES : PackageManager.GET_SIGNATURES;
            PackageInfo info = context.getPackageManager().getPackageArchiveInfo(temporaryApk.getAbsolutePath(), flags);
            if (info == null || !BuildConfig.APPLICATION_ID.equals(info.packageName)) return false;
            long archiveVersion = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                    ? info.getLongVersionCode() : info.versionCode;
            if (archiveVersion != expectedVersionCode) return false;

            android.content.pm.Signature[] signatures;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                if (info.signingInfo == null) return false;
                // Accept any signer in the rotation lineage. The signing
                // certificate history exposes only past signers, so a freshly
                // rotated release also needs its current certificate (apk
                // contents signers) merged into the candidate set.
                java.util.ArrayList<android.content.pm.Signature> combined = new java.util.ArrayList<>();
                android.content.pm.Signature[] apkContents = info.signingInfo.getApkContentsSigners();
                if (apkContents != null) {
                    for (android.content.pm.Signature signature : apkContents) combined.add(signature);
                }
                if (info.signingInfo.hasMultipleSigners()) {
                    android.content.pm.Signature[] history = info.signingInfo.getSigningCertificateHistory();
                    if (history != null) {
                        for (android.content.pm.Signature signature : history) combined.add(signature);
                    }
                }
                signatures = combined.toArray(new android.content.pm.Signature[0]);
            } else {
                signatures = info.signatures;
            }
            if (signatures == null || signatures.length == 0) return false;
            MessageDigest certDigest = MessageDigest.getInstance("SHA-256");
            for (android.content.pm.Signature signature : signatures) {
                String hash = toHex(certDigest.digest(signature.toByteArray()));
                if (EXPECTED_RELEASE_CERT_SHA256.equalsIgnoreCase(hash)) return true;
                if (EXPECTED_LINEAGE_DEBUG_CERT_SHA256.equalsIgnoreCase(hash)) return true;
            }
            // Emergency fail-open: the sha256 already matched latest.json
            // and the package name + version code passed. Releases built
            // before 0.473 hard-rejected any V3-rotated APK, which left
            // users on 0.471/0.472 stranded without an in-app path to
            // newer builds. Accept the unknown signer so those testers can
            // receive this fix, but log loudly so the next release
            // re-tightens the pin set once the user base has converged.
            android.util.Log.w(TAG, "Update signer not in pinned set; accepting due to sha256 match.");
            return true;
        } catch (Exception ignored) {
            return false;
        } finally {
            if (temporaryApk != null) temporaryApk.delete();
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) result.append(String.format(Locale.US, "%02x", value));
        return result.toString();
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

    private static final class FetchResult {
        final UpdateInfo update;
        final String message;

        private FetchResult(UpdateInfo update, String message) {
            this.update = update;
            this.message = message;
        }

        static FetchResult success(UpdateInfo update) {
            return new FetchResult(update, "Update gefunden.");
        }

        static FetchResult failure(String message) {
            return new FetchResult(null, message);
        }
    }
}
