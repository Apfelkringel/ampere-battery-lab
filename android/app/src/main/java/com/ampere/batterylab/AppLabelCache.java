package com.ampere.batterylab;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Caches human-readable app labels and launcher icons so {@code onDraw}
 * does not hit {@link PackageManager} once per row per frame. Each cache is a
 * bounded {@link LinkedHashMap} so a long-running session with many distinct
 * packages cannot grow without limit. The "Launcher" override for the three
 * well-known home-screen package ids is handled here so the rest of the
 * dashboard can ask for a label without reimplementing the special case.
 */
final class AppLabelCache {
    private static final int LABEL_ENTRIES = 256;
    private static final int ICON_ENTRIES = 128;

    private static final LinkedHashMap<String, String> LABELS = new LinkedHashMap<String, String>(LABEL_ENTRIES, 0.75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return size() > LABEL_ENTRIES;
        }
    };
    private static final LinkedHashMap<String, Drawable> ICONS = new LinkedHashMap<String, Drawable>(ICON_ENTRIES, 0.75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, Drawable> eldest) {
            return size() > ICON_ENTRIES;
        }
    };

    private AppLabelCache() {}

    /**
     * Returns a stable, user-readable name for {@code packageName}. Uses the
     * canonical launcher label when available, falls back to the last path
     * segment of the package id, and surfaces the same well-known
     * "Startbildschirm" mapping for the three launcher package ids.
     */
    static synchronized String labelFor(Context context, String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return context == null ? "Unbekannte App" : AppText.t(context, "Unbekannte App");
        }
        String cached = LABELS.get(packageName);
        if (cached != null) return cached;
        String resolved = resolveLabel(context, packageName);
        LABELS.put(packageName, resolved);
        return resolved;
    }

    /**
     * Returns the launcher icon for {@code packageName} or {@code null} when
     * the package is missing. Cached by package id so repeat {@code onDraw}
     * calls do not re-enter PackageManager.
     */
    static synchronized Drawable iconFor(Context context, String packageName) {
        if (context == null || packageName == null || packageName.isEmpty()) return null;
        Drawable cached = ICONS.get(packageName);
        if (cached != null) return cached;
        try {
            Drawable icon = context.getPackageManager().getApplicationIcon(packageName);
            ICONS.put(packageName, icon);
            return icon;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String resolveLabel(Context context, String packageName) {
        if (context != null
                && ("com.google.android.apps.nexuslauncher".equals(packageName)
                || "com.google.android.apps.pixel.launcher".equals(packageName)
                || "com.android.launcher3".equals(packageName))) {
            return AppText.t(context, "Startbildschirm");
        }
        if (context != null) {
            try {
                PackageManager pm = context.getPackageManager();
                ApplicationInfo info = pm.getApplicationInfo(packageName, 0);
                CharSequence label = pm.getApplicationLabel(info);
                if (label != null && label.length() > 0 && !packageName.equals(label.toString())) {
                    return label.toString();
                }
            } catch (Exception ignored) { }
        }
        int separator = packageName.lastIndexOf('.');
        String fallback = separator >= 0 && separator + 1 < packageName.length()
                ? packageName.substring(separator + 1) : packageName;
        return fallback == null || fallback.isEmpty() ? "Unbekannte App" : fallback;
    }

    /** Used by {@link AppLabelCacheTest} to assert the cache semantics. */
    static synchronized void resetForTest() {
        LABELS.clear();
        ICONS.clear();
    }

    /** Snapshot of the current cache size, used only by tests. */
    static synchronized int labelCountForTest() {
        return LABELS.size();
    }

    /** Snapshot of the current icon cache size, used only by tests. */
    static synchronized int iconCountForTest() {
        return ICONS.size();
    }
}
