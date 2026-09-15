package com.ampere.batterylab;

/** User-facing explanation for Android settings that affect the live monitor. */
final class BatteryBackgroundStatus {
    private BatteryBackgroundStatus() { }

    static String notificationStatus(boolean permissionGranted, boolean appNotificationsEnabled,
                                     boolean monitoringChannelEnabled) {
        if (!permissionGranted) return "Berechtigung fehlt";
        if (!appNotificationsEnabled) return "App-Benachrichtigungen sind deaktiviert";
        if (!monitoringChannelEnabled) return "Kanal „Akkuüberwachung“ ist deaktiviert";
        return "aktiviert";
    }

    static String batteryOptimizationStatus(boolean exemptFromOptimization) {
        return exemptFromOptimization ? "uneingeschränkt" : "vom System optimiert";
    }

    static String explanation(String notificationStatus, String batteryOptimizationStatus) {
        return "Live-Benachrichtigung: " + notificationStatus + "\n"
                + "Akkuoptimierung: " + batteryOptimizationStatus + "\n\n"
                + "Ampere überwacht den Akku über einen sichtbaren Android-Dienst. Sind Benachrichtigungen gesperrt, kann der Dienst trotzdem laufen, aber seine Live-Anzeige fehlt. Energiesparfunktionen des Herstellers oder „Stopp erzwingen“ können die Überwachung anhalten. Im Tiefschlaf darf Android Aktualisierungen verzögern; Ampere hält das Gerät bewusst nicht dauerhaft wach, um keinen zusätzlichen Akkuverbrauch zu verursachen.";
    }
}
