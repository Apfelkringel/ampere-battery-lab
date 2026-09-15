package com.ampere.batterylab;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.EnumMap;
import java.util.Map;

/** Opt-in-only, allow-listed product analytics. No battery or foreground-app values are accepted. */
final class AnalyticsTracker {
    // Kept outside the battery preferences so consent is never included in
    // user-exported or Android-restored battery backups.
    private static final String PRIVACY_PREFS = "ampere-privacy";
    static final String CONSENT_KEY = "analyticsConsent";
    private static final String GRANTED = "granted";
    private static final String DENIED = "denied";

    private AnalyticsTracker() { }

    static void restoreConsent(Context context) {
        SharedPreferences preferences = privacyPreferences(context);
        boolean granted = GRANTED.equals(preferences.getString(CONSENT_KEY, ""));
        FirebaseAnalytics analytics = analyticsOrNull(context);
        if (analytics == null) return;
        analytics.setConsent(consent(granted));
        analytics.setAnalyticsCollectionEnabled(granted);
    }

    static boolean hasDecision(Context context) {
        return privacyPreferences(context).contains(CONSENT_KEY);
    }

    static boolean isEnabled(Context context) {
        return GRANTED.equals(privacyPreferences(context).getString(CONSENT_KEY, ""));
    }

    static void setConsent(Context context, boolean granted) {
        privacyPreferences(context).edit()
                .putString(CONSENT_KEY, granted ? GRANTED : DENIED)
                .apply();
        FirebaseAnalytics analytics = analyticsOrNull(context);
        if (analytics == null) return;
        analytics.setConsent(consent(granted));
        analytics.setAnalyticsCollectionEnabled(granted);
        if (!granted) analytics.resetAnalyticsData();
    }

    private static SharedPreferences privacyPreferences(Context context) {
        return context.getSharedPreferences(PRIVACY_PREFS, Context.MODE_PRIVATE);
    }

    static void logSection(Context context, int page) {
        if (!isEnabled(context)) return;
        String section;
        switch (page) {
            case 1: section = "charge"; break;
            case 2: section = "discharge"; break;
            case 3: section = "battery_health"; break;
            case 4: section = "history"; break;
            default: section = "overview";
        }
        Bundle parameters = new Bundle();
        parameters.putString("section", section);
        log(context, "ampere_section_view", parameters);
    }

    static void logFeature(Context context, String feature) {
        if (!isEnabled(context) || !isAllowedFeature(feature)) return;
        Bundle parameters = new Bundle();
        parameters.putString("feature", feature);
        log(context, "ampere_feature_used", parameters);
    }

    private static boolean isAllowedFeature(String feature) {
        return "health_measurement".equals(feature)
                || "history_export".equals(feature)
                || "charge_alarm".equals(feature)
                || "discharge_alarm".equals(feature)
                || "temperature_alarm".equals(feature)
                || "overlay".equals(feature);
    }

    private static void log(Context context, String eventName, Bundle parameters) {
        FirebaseAnalytics analytics = analyticsOrNull(context);
        if (analytics != null) analytics.logEvent(eventName, parameters);
    }

    private static FirebaseAnalytics analyticsOrNull(Context context) {
        try {
            if (FirebaseApp.getApps(context.getApplicationContext()).isEmpty()) return null;
            return FirebaseAnalytics.getInstance(context.getApplicationContext());
        } catch (IllegalStateException | IllegalArgumentException ignored) {
            // The SDK remains inert if a Firebase project has not been linked.
            return null;
        }
    }

    private static Map<FirebaseAnalytics.ConsentType, FirebaseAnalytics.ConsentStatus> consent(boolean granted) {
        Map<FirebaseAnalytics.ConsentType, FirebaseAnalytics.ConsentStatus> values =
                new EnumMap<>(FirebaseAnalytics.ConsentType.class);
        values.put(FirebaseAnalytics.ConsentType.ANALYTICS_STORAGE,
                granted ? FirebaseAnalytics.ConsentStatus.GRANTED : FirebaseAnalytics.ConsentStatus.DENIED);
        values.put(FirebaseAnalytics.ConsentType.AD_STORAGE, FirebaseAnalytics.ConsentStatus.DENIED);
        values.put(FirebaseAnalytics.ConsentType.AD_USER_DATA, FirebaseAnalytics.ConsentStatus.DENIED);
        values.put(FirebaseAnalytics.ConsentType.AD_PERSONALIZATION, FirebaseAnalytics.ConsentStatus.DENIED);
        return values;
    }
}
