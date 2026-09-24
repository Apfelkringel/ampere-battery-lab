package com.ampere.batterylab;

import java.util.Locale;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Guards the English UI against generic replacement-order regressions. */
public class AppTextTest {
    @Test public void translatesRemainingHistoryStatisticsLabels() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.ENGLISH);
            assertEquals("7 CALENDAR DAYS", AppText.t("7 KALENDERTAGE"));
            assertEquals("5 CALENDAR WEEKS", AppText.t("5 KALENDERWOCHEN"));
            assertEquals("6 CALENDAR MONTHS", AppText.t("6 KALENDERMONATE"));
            assertEquals("Daily", AppText.t("Täglich"));
            assertEquals("Weekly", AppText.t("Wöchentlich"));
            assertEquals("Monthly", AppText.t("Monatlich"));
            assertEquals("— = no usable measurements", AppText.t("— = keine auswertbaren Messwerte"));
            assertEquals("HISTORY & STATISTICS", AppText.t("VERLAUF & STATISTIK"));
            assertEquals("Older months may be blank · local data: about 30 days",
                    AppText.t("Ältere Monate können leer sein · lokale Daten: etwa 30 Tage"));
            assertEquals("Ratio = charged ÷ used · EFC = full cycles, not cell efficiency.",
                    AppText.t("Quote = geladen ÷ Verbrauch · EFC = Vollzyklen, kein Zellwirkungsgrad."));
            assertEquals("No completed charge or discharge sessions yet.",
                    AppText.t("Noch keine abgeschlossenen Lade- oder Entladevorgänge."));
            assertEquals("Tap a session for details", AppText.t("Sitzung antippen für Details"));
            assertEquals("Alert at 80% · charging will continue", AppText.t("Alarm bei 80 % · kein Ladestopp"));
            assertEquals("about 3.2 W current usage", AppText.t("ca. 3.2 W aktueller Verbrauch"));
            assertEquals("Keep monitoring running to create local history entries.",
                    AppText.t("Lass die Überwachung laufen, um lokale Verlaufseinträge zu erstellen."));
            assertEquals("No daily cycle values available yet.", AppText.t("Noch keine täglichen Zykluswerte verfügbar."));
            assertEquals("CHARGE COMPANION", AppText.t("LADEBEGLEITUNG"));
            assertEquals("BATTERY HEALTH", AppText.t("AKKUGESUNDHEIT"));
            assertEquals("CHARGING SESSION", AppText.t("LADE-SITZUNG"));
            assertEquals("PRIVATE BY DESIGN", AppText.t("PRIVAT VON ANFANG AN"));
            assertEquals("Source: USB charger", AppText.t("Quelle: USB-Ladegerät"));
        } finally {
            Locale.setDefault(previous);
        }
    }
    @Test public void englishTranslationsKeepSpecificWordsIntact() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.ENGLISH);
            assertEquals("Overview", AppText.t("Übersicht"));
            assertEquals("Charging", AppText.t("Laden"));
            assertEquals("Discharging", AppText.t("Entladen"));
            assertEquals("Battery health", AppText.t("Akku"));
            assertEquals("History", AppText.t("Verlauf"));
            assertEquals("Status unknown", AppText.t("Status unbekannt"));
            assertEquals("Show charging", AppText.t("Laden anzeigen"));
            assertEquals("Show discharging", AppText.t("Entladen anzeigen"));
            assertEquals("Show battery health", AppText.t("Akkugesundheit anzeigen"));
            assertEquals("Show history", AppText.t("Verlauf anzeigen"));
            assertEquals("Save", AppText.t("Speichern"));
            assertEquals("Charging current", AppText.t("Ladestrom"));
            assertEquals("CHARGING", AppText.t("LADEN"));
            assertEquals("Refresh live data", AppText.t("Live-Daten aktualisieren"));
            assertEquals("No discharge yet", AppText.t("Noch keine Entladung"));
            assertEquals("no discharge yet", AppText.t("noch keine Entladung"));
            assertEquals("Charging to 80%", AppText.t("Lädt bis 80%"));
            assertEquals("Charger max. 1.50 A", AppText.t("Ladegerät max. 1.50 A"));
            assertEquals("Charging hardware max. 5.00 A", AppText.t("Ladehardware max. 5.00 A"));
            assertEquals("USB charger", AppText.t("USB-Ladegerät"));
            assertEquals("OEM charge window 40–80%", AppText.t("OEM-Ladefenster 40–80%"));
            assertEquals("OEM limit 80%", AppText.t("OEM-Limit 80%"));
            assertEquals("Charging paused while awake", AppText.t("Laden im Wachzustand gesperrt"));
            assertEquals("Charging paused", AppText.t("Laden gesperrt"));
            assertEquals("Forced discharge", AppText.t("Entladung erzwungen"));
            assertEquals("Custom", AppText.t("Benutzerdefiniert"));
            assertEquals("Waiting for energy data", AppText.t("Warte auf Energiedaten"));
            assertEquals("Included after charging", AppText.t("Wird nach dem Ladevorgang einbezogen"));
            assertEquals("Longer charging sessions improve accuracy",
                    AppText.t("Längere Ladevorgänge verbessern die Genauigkeit"));
            assertEquals("Wireless", AppText.t("Kabellos"));
            assertEquals("External power source", AppText.t("Externe Stromquelle"));
            assertEquals("At 40 °C", AppText.t("Ab 40 °C"));
            assertEquals("At 45 °C (recommended)", AppText.t("Ab 45 °C (empfohlen)"));
            assertEquals("At 50 °C", AppText.t("Ab 50 °C"));
            assertEquals("At 55 °C", AppText.t("Ab 55 °C"));
            assertEquals("At 10% or less", AppText.t("Bei 10 % oder weniger"));
            assertEquals("At 15% or less (recommended)", AppText.t("Bei 15 % oder weniger (empfohlen)"));
            assertEquals("At 20% or less", AppText.t("Bei 20 % oder weniger"));
            assertEquals("At 25% or less", AppText.t("Bei 25 % oder weniger"));
            assertEquals("At 30% or less", AppText.t("Bei 30 % oder weniger"));
            assertEquals("Every 5 minutes", AppText.t("Alle 5 Minuten"));
            assertEquals("Every 15 minutes (recommended)", AppText.t("Alle 15 Minuten (empfohlen)"));
            assertEquals("Every 30 minutes", AppText.t("Alle 30 Minuten"));
            assertEquals("Every 60 minutes", AppText.t("Alle 60 Minuten"));
            assertEquals("Temperature alert · resets 3 °C below",
                    AppText.t("Temperaturwarnung · Rücksetzung 3 °C darunter"));
            assertEquals("Low battery alert · resets with a 3% margin",
                    AppText.t("Tiefstandwarnung · Rücksetzung mit 3 % Abstand"));
            assertEquals("Data collection · local only",
                    AppText.t("Datenerfassung · nur lokal"));
            assertEquals("Selected: charging", AppText.t("Ausgewählt: Laden"));
            assertEquals("Equivalent full cycles per charge session",
                    AppText.t("Äquivalente Vollzyklen je Ladevorgang"));
            assertEquals("Current session + local 7-day usage",
                    AppText.t("Aktuelle Sitzung + lokale 7-Tage-Nutzung"));
            assertEquals("Based on local 7-day usage",
                    AppText.t("Basierend auf lokaler 7-Tage-Nutzung"));
            assertEquals("At least 5% battery level required",
                    AppText.t("Mindestens 5 % Akkustand nötig"));
            assertEquals("Local history", AppText.t("Lokaler Verlauf"));
            assertEquals("Instantaneous estimate", AppText.t("Momentanschätzung"));
            assertEquals("Current battery level", AppText.t("Aktueller Akkustand"));
            assertEquals("Health —", AppText.t("Gesundheit —"));
            assertEquals("Change · duration", AppText.t("Änderung · Dauer"));
            assertEquals("Daily total counter", AppText.t("Gesamtzähler im Tagesverlauf"));
            assertEquals("Overview shows live battery level, temperature, voltage, and history.\n\nCharging includes the charge target and sessions. Discharging shows usage and runtime. Battery health explains condition and capacity. History compares days, weeks, and months.\n\nSwitch between the five sections using the bar at the bottom. Your battery measurements stay on this device.",
                    AppText.t("Übersicht zeigt den Live-Akkustand, Temperatur, Spannung und Verlauf.\n\nLaden enthält Ladeziel und Sitzungen. Entladen zeigt Verbrauch und Laufzeit. Akku erklärt Gesundheit und Kapazität. Verlauf vergleicht Tag, Woche und Monat.\n\nDie fünf Bereiche wechselst du über die Leiste unten. Deine Akku-Messwerte bleiben lokal auf diesem Gerät."));
            assertEquals("Screen time", AppText.t("Bildschirmzeit"));
            assertEquals("Battery level · 7 days",
                    AppText.t("Akkustand · 7 Tage"));
            assertEquals("100% of 80%", AppText.t("100% von 80%"));
            assertEquals("Charge/usage ratio", AppText.t("Charged/Usage"));
            assertEquals("Battery level at start", AppText.t("Akkustand beim Start"));
            assertEquals("Rate unavailable", AppText.t("Rate nicht verfügbar"));
            assertEquals("EFC unavailable", AppText.t("EFC nicht verfügbar"));
            assertEquals("Since unplugging · estimated values",
                    AppText.t("Seit dem Abstecken · geschätzte Werte"));
            assertEquals("Runtime with normal use: 8 hr",
                    AppText.t("Restlaufzeit bei normaler Nutzung: 8 Std."));
            assertEquals("Learning takes a little time.",
                    AppText.t("Lernen braucht ein wenig Zeit."));
            assertEquals("Backup could not be saved.",
                    AppText.t("Backup konnte nicht gespeichert werden."));
            assertEquals("Start the capacity measurement unplugged and below 25%.",
                    AppText.t("Starte die Kapazitätsmessung getrennt vom Ladegerät unter 25 %."));
            assertEquals("Battery status copied.", AppText.t("Akkustatus kopiert."));
            assertEquals("Start a discharge session", AppText.t("Entladung starten"));
            assertEquals("Charge target reached", AppText.t("Ladeziel erreicht"));
            assertEquals("High battery temperature", AppText.t("Hohe Akkutemperatur"));
            assertEquals("Total CPU 42% · Foreground app: Photos · Process load 8%",
                    AppText.t("CPU gesamt 42% · Vordergrund-App: Photos · Prozesslast 8%"));
            assertEquals("On battery", AppText.t("Akkubetrieb"));
            assertEquals("On battery", AppText.t("Akkubetrieb aktiv"));
            assertEquals("Show overview", AppText.t("Übersicht anzeigen"));
            assertEquals("Reset health baseline", AppText.t("Gesundheitsbasis zurücksetzen"));
            assertEquals("Delete local data", AppText.t("Lokale Daten löschen"));
            assertEquals("Live data refreshed.", AppText.t("Live-Daten aktualisiert."));
            assertEquals("Battery balance · September 2026",
                    AppText.t("Akku-Bilanz · September 2026"));
            assertEquals("September", AppText.t("September"));
            assertEquals("No usable current measurements are available for this period.\n\n— means missing measurements, not 0.\n\nEach metric's bars are scaled separately.",
                    AppText.t("Für diesen Zeitraum liegen keine auswertbaren Strommessungen vor.\n\n— bedeutet fehlende Messwerte, nicht 0.\n\nDie Balken jeder Kennzahl werden separat skaliert."));
            assertEquals("Open settings", AppText.t("Einstellung öffnen"));
            assertEquals("Charging detected. Battery level 100 percent. Battery health not measured. Android status Good.",
                    AppText.t("Laden erkannt. Akkustand 100 Prozent. Akkugesundheit nicht gemessen. Android-Zustand Gut."));
            assertEquals("Tabs: Overview, Charging, Discharging, Health, History. Active tab: Overview.",
                    AppText.t("Tabs: Übersicht, Laden, Entladen, Gesundheit, Verlauf. Aktiver Tab: Übersicht."));
            assertEquals("Battery current: +900 mA. Temperature: 25.0 degrees Celsius.",
                    AppText.t("Akkustrom: +900 mA. Temperatur: 25.0 Grad Celsius."));
            assertEquals("Battery is full", AppText.t("Akku ist voll"));
            assertEquals("Charging now", AppText.t("Akku wird geladen"));
            assertEquals("Battery is discharging", AppText.t("Akku entlädt sich"));
            assertEquals("Usage is tracked live.", AppText.t("Verbrauch wird live erfasst."));
            assertEquals("LIVE ENERGY FLOW", AppText.t("LIVE-ENERGIEFLUSS"));
            assertEquals("Very good", AppText.t("Sehr gut"));
            assertEquals("Battery temperature", AppText.t("Akkutemperatur"));
            assertEquals("current + local measurements", AppText.t("aktueller Strom + lokale Messwerte"));
            assertEquals("local 7 days", AppText.t("lokale 7 Tage"));
            assertEquals("local 7-day estimate", AppText.t("lokale 7-Tage-Schätzung"));
            assertEquals("Recent sessions", AppText.t("Letzte Sitzungen"));
            assertEquals("More sessions in history", AppText.t("Weitere Sitzungen im Verlauf"));
            assertEquals("Not connected", AppText.t("Nicht verbunden"));
            assertEquals("No data available.", AppText.t("Keine Daten verfügbar."));
            assertEquals("Charging is monitored.", AppText.t("Laden wird überwacht."));
            assertEquals("Charge target reached · Device is still charging",
                    AppText.t("Ladeziel erreicht · Gerät lädt weiter"));
            assertEquals("Not connected to power", AppText.t("Nicht mit Strom verbunden"));
            assertEquals("No sessions completed yet.", AppText.t("Noch keine Sitzungen abgeschlossen."));
            assertEquals("Charge status: Not available. Charge target: Reached. Charged energy: 7612 mAh. Session duration: 8 hr 29 min.",
                    AppText.t("Ladestatus: Nicht verfügbar. Ladeziel: Erreicht. Geladene Energie: 7612 mAh. Sitzungsdauer: 8 Std. 29 Min."));
            assertEquals("Monthly history. Selected period: 6 CALENDAR MONTHS. Charge/usage ratio (charged divided by used): 1424 percent",
                    AppText.t("Verlauf monatlich. Ausgewählter Zeitraum: 6 KALENDERMONATE. Lade-/Verbrauchsquote (geladen geteilt durch verbraucht): 1424 Prozent"));
            assertEquals("Measurement status: At least 5% battery level required. The charge/usage ratio compares charged with used energy and is not measured battery efficiency.",
                    AppText.t("Messstatus: Mindestens 5 % Battery level nötig. Die Charge/usage ratio vergleicht geladene mit useder Energie und ist keine gemessene Battery-Efficiency."));
            assertEquals("BATTERY BALANCE · 6 CALENDAR MONTHS",
                    AppText.t("AKKU-BILANZ · 6 KALENDERMONATE"));
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test public void translatesCanvasDashboardLabelsAndLiveStatuses() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.ENGLISH);
            String[][] samples = {
                    {"Ladevorgang aktiv", "Charging in progress"},
                    {"Letzter Ladevorgang", "Last charge session"},
                    {"Zeit bis voll", "Time to full"},
                    {"Zeit bis Ziel", "Time to target"},
                    {"Letzte Ladung", "Last charge"},
                    {"lokale 7-Tage-Schätzung", "local 7-day estimate"},
                    {"Nicht gemessen", "Not measured"},
                    {"Nennkapazität nicht verfügbar", "Design capacity unavailable"},
                    {"Sitzung startet beim Abstecken", "Session starts when unplugged"},
                    {"Messungen · letzter Ladevorgang", "Measurements · last charge session"},
                    {"Nach dem ersten Zyklus sichtbar.", "Visible after the first cycle."},
                    {"Deine Akku-Messwerte bleiben auf diesem Gerät.", "Your battery measurements stay on this device."},
                    {"Akkuverbrauch deiner Apps", "Battery usage by app"},
                    {"Temperatur nicht verfügbar", "Temperature unavailable"},
                    {"Strom nicht verfügbar", "Current unavailable"},
                    {"Akkustand wird ermittelt", "Detecting battery level"},
                    {"Nicht mit Strom verbunden", "Not connected to power"},
                    {"Akkustand · Automatik", "Battery level · automatic"},
                    {"Dein Tagesrhythmus", "Your daily pattern"},
                    {"Zum Abschluss über 95 % laden.", "Charge above 95% to complete."},
                    {"Unter 25 % starten, dann in Ruhe vollladen.", "Start below 25%, then charge fully without interruption."},
                    {"Kapazität aus Android-Akkusensor", "Capacity from Android battery sensor"},
                    {"Ladeziel erreicht · Gerät lädt weiter", "Charge target reached · Device is still charging"},
                    {"Akku fast leer", "Battery nearly empty"},
                    {"Hohe Akkutemperatur", "High battery temperature"},
                    {"Akkustand · 30 Tage", "Battery level · 30 days"},
                    {"LIVE-ENERGIEFLUSS", "LIVE ENERGY FLOW"},
                    {"lokale 7 Tage", "local 7 days"},
                    {"Belastung bis zum Ziel", "Usage until target"},
                    {"Noch keine Messreihe", "No measurement series yet"},
                    {"ZEIT →", "TIME →"},
                    {"Kein Konto · kein Abo · Export nur auf Wunsch", "No account · no subscription · export only when you choose"},
                    {"Lokal gespeichert · bis zu 150", "Stored locally · up to 150"},
                    {"Seit dem Trennen keine App-Nutzung erfasst.", "No app usage recorded since unplugging."},
                    {"Warte auf lokale Telemetrie.", "Waiting for local telemetry."},
                    {"Aus lokalen Sitzungsdaten", "From local session data"},
                    {"basierend auf letzter Nutzung", "based on recent usage"},
                    {"7-Tage-Durchschnitt", "7-day average"},
                    {"Ladestrom live", "Live charging current"},
                    {"Vollzyklen (EFC)", "Full cycles (EFC)"},
                    {"T min/Ø/max", "Temp. min/avg/max"},
                    {"Telemetrie T min/Ø/max", "Telemetry temp. min/avg/max"},
                    {"Akkustand · Strom · Leistung · Spannung", "Battery level · current · power · voltage"},
                    {"Temperatur · mAh · Wh · Bildschirmstatus", "Temperature · mAh · Wh · screen status"},
                    {"VERBLEIBENDE NUTZUNGSZEIT", "REMAINING RUNTIME"},
                    {"Gemischt", "Mixed"},
                    {"Export nur auf deine Auswahl; kein Konto/Abonnement.", "Export only when you choose; no account or subscription."},
                    {"Messungen · letzter Ladevorgang ", "Measurements · last charge session "},
                    {"Wir lernen", "We're learning"},
                    {"deinen Rhythmus.", "your pattern."},
                    {"Deine Werte bleiben bei dir.", "Your data stays yours."},
                    {"Kapazitätsmessung läuft.", "Capacity measurement in progress."},
                    {"Hier wächst bald", "Your story starts"},
                    {"deine Geschichte.", "here."},
                    {"mAh und Anteile sind Schätzungen aus Vordergrundzeit und lokaler Akku-Telemetrie. Eine individuelle mAh/h-Rate zeigen wir nur bei direkter App-Telemetrie; Android stellt keine exakten Akkuwerte je App bereit.", "mAh values and shares are estimates based on foreground time and local battery telemetry. An app-specific mAh/h rate is shown only when direct app telemetry is available; Android does not provide exact battery usage for each app."},
                    {"Letzte 7 Tage · Schätzung; Rest ggf. nicht zuordenbar", "Last 7 days · estimate; remaining usage may be unattributed"},
                    {"Letzte 24 Stunden · Schätzung; Rest ggf. nicht zuordenbar", "Last 24 hours · estimate; remaining usage may be unattributed"},
                    {"Quelle: zugeordnete Akku-Telemetrie (Schätzung)", "Source: attributed battery telemetry (estimate)"},
                    {"Quelle: anteilig nach Vordergrundzeit (Schätzung)", "Source: allocated by foreground time (estimate)"},
                    {"Keine App-Werte zugeordnet", "No app usage attributed"},
                    {"Noch keine Akku-Messwerte", "No battery measurements yet"},
                    {"App-Verbrauch", "App usage"},
                    {"~120 mAh zugeordnet", "~120 mAh attributed"},
                    {"7 Min. · Vordergrundzeit-Schätzung · Rate nicht verfügbar", "7 min · Foreground-time estimate · Rate unavailable"}
            };
            for (String[] sample : samples) {
                assertEquals(sample[0], sample[1], AppText.t(sample[0]));
            }
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test public void translatesComposedQuickTileAndNotificationMessages() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.ENGLISH);
            assertEquals("Battery level 82 percent, Charging · +1.25 A · 32.5°C · 4.20V",
                    AppText.t("Akkustand 82 Prozent, Laden · +1.25 A · 32.5°C · 4.20V"));
            assertEquals("Charging detected · Android status Failure · Capacity level Normal · Charging profile Battery protection · Charging algorithm Custom · Charging behavior Charging paused · Charging hardware max. 1.50 A · Thermal status Too hot · OEM limit 80 · Health 95% · Estimate 2800 mAh",
                    AppText.t("Laden erkannt · Android-Zustand Fehler · Kapazitätsniveau Normal · Ladeprofil Akkuschonend · Ladealgorithmus Benutzerdefiniert · Ladeverhalten Laden gesperrt · Ladehardware max. 1.50 A · Thermik Zu heiß · OEM-Limit 80 · Gesundheit 95% · Schätzung 2800 mAh"));
            assertEquals("Live notification: App notifications are disabled\nBackground service: Service last confirmed 2 minutes ago\nBattery optimization: Optimized by the system",
                    AppText.t("Live-Benachrichtigung: App-Benachrichtigungen sind deaktiviert\nHintergrunddienst: Dienst zuletzt vor 2 Min. bestätigt\nAkkuoptimierung: vom System optimiert"));
            assertEquals("Fully charged", AppText.t("Voll geladen"));
            assertEquals("Unknown", AppText.t("Unbekannt"));
            assertEquals("Battery not available", AppText.t("Akku nicht verfügbar"));
            assertEquals("Battery level not available", AppText.t("Akkustand nicht verfügbar"));
            assertEquals("Battery level 82 percent, Charging, Current —",
                    AppText.t("Akkustand 82 Prozent, Laden, Strom —"));
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test public void translatesHeartbeatAgeWithCorrectEnglishSingularAndPlural() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.ENGLISH);
            assertEquals("Service last confirmed 1 minute ago",
                    AppText.t(BatteryBackgroundStatus.monitorHeartbeatStatus(90_000L, 150_000L)));
            assertEquals("Service last confirmed 2 minutes ago",
                    AppText.t(BatteryBackgroundStatus.monitorHeartbeatStatus(90_000L, 210_000L)));
            String explanation = BatteryBackgroundStatus.explanation("Berechtigung fehlt",
                    "vom System optimiert",
                    BatteryBackgroundStatus.monitorHeartbeatStatus(90_000L, 150_000L));
            assertTrue(AppText.t(explanation).contains("Service last confirmed 1 minute ago"));
            assertEquals("Open notification settings",
                    AppText.t("Benachrichtigungseinstellungen öffnen"));
            assertEquals("Open battery settings", AppText.t("Akku-Einstellungen öffnen"));
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test public void translatesShortSessionAndLiveOverlayLabelsAtomically() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.ENGLISH);
            assertEquals("Duration", AppText.t("Dauer"));
            assertEquals("Started", AppText.t("Gestartet"));
            assertEquals("Ampere live display is active", AppText.t("Ampere-Live-Anzeige aktiv"));
            assertEquals("Live battery measurements are shown on screen",
                    AppText.t("Live-Akkumesswerte werden auf dem Bildschirm angezeigt"));
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test public void telemetryUsesTheSelectedNumericLocale() {
        assertEquals("+1.2 A", BatteryTelemetryText.current(1200, true, true, Locale.US));
        assertEquals("+1,2 A", BatteryTelemetryText.current(1200, true, true, Locale.GERMANY));
        assertEquals("+4.5 W", BatteryTelemetryText.power(900, 5000, true, Locale.US));
    }

    @Test public void englishAccessibilityLabelsUseEnglishVocabulary() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.ENGLISH);
            String label = AppText.t("Verlauf monatlich (ausgewählt)");
            assertTrue(label.contains("Monthly history"));
            assertTrue(label.contains("selected"));
        } finally {
            Locale.setDefault(previous);
        }
    }
}
