package com.ampere.batterylab;

/** Builds concise, truthful labels for the permission checklist rows. */
final class BatteryPermissionChecklist {
    private BatteryPermissionChecklist() { }

    static String[] entries(boolean notifications, boolean usage, boolean usageRequested,
                            boolean overlay, boolean overlayRequested) {
        return new String[]{
                "Benachrichtigungen\n" + (notifications ? "Aktiv" : "Nicht erteilt")
                        + " · Live-Status und Alarme",
                "App-Nutzungszugriff\n" + optionalStatus(usage, usageRequested)
                        + " · Verbrauch je App",
                "Overlay\n" + optionalStatus(overlay, overlayRequested)
                        + " · Live-Anzeige"
        };
    }

    private static String optionalStatus(boolean granted, boolean requested) {
        if (granted) return "Aktiv";
        return requested ? "Nicht erteilt · optional" : "Optional · nicht aktiviert";
    }
}
