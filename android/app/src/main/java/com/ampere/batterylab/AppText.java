package com.ampere.batterylab;

import android.content.Context;
import android.content.res.Configuration;
import android.app.LocaleManager;
import android.os.Build;
import android.os.LocaleList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Small, dependency-free UI language layer used by the custom canvas and services. */
final class AppText {
    private static final String LANGUAGE_KEY = "appLanguage";
    private static volatile String[][] sortedTranslationPhrases;
    private static final Pattern GERMAN_PERCENT_COMPARISON = Pattern.compile(
            "(\\d+(?:[.,]\\d+)?\\s*%)\\s+von\\s+(\\d+(?:[.,]\\d+)?\\s*%)");
    private static final Pattern GERMAN_SERVICE_HEARTBEAT_AGE = Pattern.compile(
            "Dienst zuletzt vor (\\d+) Min\\. bestätigt");
    private static final Pattern GERMAN_UI_MARKER = Pattern.compile(
            "(?iu)(?:[äöüß]|(?:akku|batterie|lade|entlade|verlauf|gesundheit|gesund|kapazität|zykl|" +
                    "spannung|strom|aktuell|aktualisiert|aktualisierung|adaptiv|anzeige|bildschirm|berechnet|" +
                    "datum|diagnose|effizienz|erlauben|erlaubt|fehlend|fehler|gemeldet|gemischt|" +
                    "grenzwert|hintergrunddienst|kalender|lernen|manuell|normalbetrieb|notfall|nennwert|grafisch|" +
                    "optimiert|restenergie|schattiert|tiefschlaf|unbekannt|verbunden|zusammenfassung|" +
                    "telemetrie|detailanalyse|sensoren|jetzt|gespeichert|zugeordnet|antwortete|temperaturwarnung|temperatur(?!e)|thermik|dauer|" +
                    "mess(?:ung|ungen|wert|werte|reihe|reihen|bereich|status|punkt|punkte|basis|zeit|daten|gerät|en)|gemess|gerät|nutzung|verbrauch|zeitraum|" +
                    "sitzung|sicherung|berechtigung|einstellung|geschätzt|schätzung|wird|werden|" +
                    "keine|keiner|keinen|noch|seit|letzte|weiter|verfügbar|abbrechen|fertig|" +
                    "zurück|löschen|durchschnitt|niedrig|voll|erreicht|abstecken|geladen|" +
                    "entladen|entladung|energie|zugriff|kennzahl|oem-limit|anzeigen|daten|zähler|aktiv|inaktiv|gestartet|starten|warten|" +
                    "erlaubt|verwendet|eingestellt|lokal|quelle|ladungsmenge|akkustand|" +
                    "erfasst|aufgezeichnet|prozentpunkte|ruhephasen|abgeschaltet|ausgeschaltet|" +
                    "ausgewählt|übersicht|dein|wächs|läuft|entspannt|privat|gesamt|vordergrund|" +
                    "prozesslast|prüf|ziel|über|analysezentrale|analyse|netzteil|bereit|zustand|" +
                    "reihe|zeit|monatlich|wunsch|akzeptabel|abschaltung|kalendertag|kabellos|" +
                    "benachrichtig|gesperrt|tiefstandwarnung)" +
                    "\\p{L}*|\\b(?:hoch|mittel|aus|ein|heute|monat|tage|stunden|minuten|werte|messpunkte|temperatur|für|ab|alle|nicht)\\b)");

    private AppText() {}

    /** Applies the persisted app language on Android versions without LocaleManager. */
    static Context applyStoredLocale(Context base) {
        if (base == null || Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base;
        String language = BatteryDataRepository.data(base).getString(LANGUAGE_KEY, "");
        if (language == null || language.isEmpty()) return base;
        Configuration configuration = new Configuration(base.getResources().getConfiguration());
        configuration.setLocale(Locale.forLanguageTag(language));
        return base.createConfigurationContext(configuration);
    }

    /** Sets the app-specific language and recreates the activity with the new resources. */
    static void setLanguage(android.app.Activity activity, String languageTag) {
        if (activity == null || languageTag == null || languageTag.isEmpty()) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            LocaleManager localeManager = activity.getSystemService(LocaleManager.class);
            if (localeManager != null) {
                localeManager.setApplicationLocales(LocaleList.forLanguageTags(languageTag));
            }
        } else {
            BatteryDataRepository.data(activity).edit().putString(LANGUAGE_KEY, languageTag).apply();
            activity.recreate();
        }
    }

    static boolean isEnglish(Context context) {
        if (context == null) return "en".equalsIgnoreCase(Locale.getDefault().getLanguage());
        Configuration configuration = context.getResources().getConfiguration();
        Locale locale;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            locale = configuration.getLocales().isEmpty()
                    ? Locale.getDefault() : configuration.getLocales().get(0);
        } else {
            //noinspection deprecation
            locale = configuration.locale == null ? Locale.getDefault() : configuration.locale;
        }
        return "en".equalsIgnoreCase(locale.getLanguage());
    }

    /** Locale used by every user-visible numeric value, including widgets and services. */
    static Locale uiLocale(Context context) {
        return isEnglish(context) ? Locale.US : Locale.GERMANY;
    }

    static String t(Context context, String value) {
        // Use the context locale here instead of Locale.getDefault(). Android
        // supports an app-specific language, so the app can be English while
        // the rest of the device remains in another language.
        return isEnglish(context) ? translate(value) : value;
    }

    /**
     * Translates the UI vocabulary while preserving numbers, units, app names
     * and server-provided values. The method intentionally leaves unknown
     * strings untouched so diagnostics and user data are never corrupted.
     */
    static String t(String value) {
        if (value == null || value.isEmpty()
                || !"en".equalsIgnoreCase(Locale.getDefault().getLanguage())) return value;
        return translate(value);
    }

    private static String translate(String value) {
        if (value == null || value.isEmpty()) return value;
        String exact = translateExactPreference(value);
        if (exact != null) return exact;
        exact = translateExactUiPhrase(value);
        if (exact != null) return exact;
        value = translateServiceHeartbeatAge(value);
        Matcher percentComparison = GERMAN_PERCENT_COMPARISON.matcher(value);
        if (percentComparison.find()) value = percentComparison.replaceAll("$1 of $2");
        if ("Start".equals(value)) return "Home";
        if ("Quelle: USB-Ladegerät".equals(value)) return "Source: USB charger";
        if ("Ampere · Großschrift".equals(value)) return "Ampere · Large text";
        if ("Android-Systemschätzung".equals(value)) return "Android system estimate";
        if ("Prüfen".equals(value)) return "Check";
        if ("Kapazität aus ".equals(value)) return "Capacity from ";
        if ("KAPAZITÄT".equals(value)) return "CAPACITY";
        if ("Nicht am Ladegerät".equals(value)) return "Not charging";
        if ("Ladezustand".equals(value)) return "Charge status";
        if ("Ziel ".equals(value)) return "Target ";
        // These chart titles contain words that are also translated as standalone
        // labels. Keep the complete title atomic so later vocabulary rules cannot
        // change the capitalization of the already translated result.
        if ("Akkustand · 7 Tage".equals(value)) return "Battery level · 7 days";
        if ("Akkustand · 30 Tage".equals(value)) return "Battery level · 30 days";
        if ("Akkustand beim Start".equals(value)) return "Battery level at start";
        if ("Akkustand aktuell".equals(value) || "Aktueller Akkustand".equals(value)) {
            return "Current battery level";
        }
        if ("aktueller Akkustand".equals(value)) return "current battery level";
        if ("Aktuelle Sitzung + lokale 7-Tage-Nutzung".equals(value)) {
            return "Current session + local 7-day usage";
        }
        if ("Basierend auf lokaler 7-Tage-Nutzung".equals(value)) return "Based on local 7-day usage";
        if ("Mindestens 5 % Akkustand nötig".equals(value)) return "At least 5% battery level required";
        if ("Lokaler Verlauf".equals(value)) return "Local history";
        if ("Momentanschätzung".equals(value)) return "Instantaneous estimate";
        if ("getrennt · Verlaufsdaten".equals(value)) return "unplugged · historical data";
        if ("Gesundheit —".equals(value)) return "Health —";
        if ("Noch offen".equals(value)) return "Pending";
        if ("Lokaler Messbereich".equals(value)) return "Local measurement range";
        if ("Gemessener Akkustand".equals(value)) return "Measured battery level";
        if ("Gesamtzähler im Tagesverlauf".equals(value)) return "Daily total counter";
        if ("Änderung · Dauer".equals(value)) return "Change · duration";
        if ("Änderung".equals(value)) return "Change";
        if ("Zeitraum: bis zu 30 lokale Tage".equals(value)) return "Period: up to 30 local days";
        if ("Äquivalente Vollzyklen je Ladevorgang".equals(value)) {
            return "Equivalent full cycles per charge session";
        }
        if ("schafft die Messbasis".equals(value)) return "establishes the measurement baseline";
        if ("Äquivalente Zyklen: ".equals(value)) return "Equivalent cycles: ";
        if ("Lokal".equals(value)) return "Local";
        if ("Starte die Kapazitätsmessung getrennt vom Ladegerät unter 25 %.".equals(value)) {
            return "Start the capacity measurement unplugged and below 25%.";
        }
        if ("Entladung starten".equals(value)) return "Start a discharge session";
        if ("Übersicht anzeigen".equals(value)) return "Show overview";
        if ("Laden anzeigen".equals(value)) return "Show charging";
        if ("Entladen anzeigen".equals(value)) return "Show discharging";
        if ("Akkugesundheit anzeigen".equals(value)) return "Show battery health";
        if ("Verlauf anzeigen".equals(value)) return "Show history";
        if ("Ausgewählt: Übersicht".equals(value)) return "Selected: overview";
        if ("Ausgewählt: Laden".equals(value)) return "Selected: charging";
        if ("Ausgewählt: Entladen".equals(value)) return "Selected: discharging";
        if ("Ausgewählt: Akku".equals(value)) return "Selected: battery health";
        if ("Ausgewählt: Verlauf".equals(value)) return "Selected: history";
        if ("Übersicht".equals(value)) return "Overview";
        if ("Laden".equals(value)) return "Charging";
        if ("Entladen".equals(value)) return "Discharging";
        if ("Akku".equals(value)) return "Battery health";
        if ("Verlauf".equals(value)) return "History";
        if ("Akku-Bilanz · September 2026".equals(value)) {
            return "Battery balance · September 2026";
        }
        if ("September".equals(value)) return "September";
        if ("aktueller Strom + lokale Messwerte".equals(value)) {
            return "current + local measurements";
        }
        if ("lokale 7 Tage".equals(value)) return "local 7 days";
        if ("Letzte Sitzungen".equals(value)) return "Recent sessions";
        if ("Weitere Sitzungen im Verlauf".equals(value)) return "More sessions in history";
        if ("Nicht verbunden".equals(value)) return "Not connected";
        if ("Keine Daten verfügbar.".equals(value)) return "No data available.";
        if ("Laden wird überwacht.".equals(value)) return "Charging is monitored.";
        if ("Ladeziel erreicht · Gerät lädt weiter".equals(value)) {
            return "Charge target reached · Device is still charging";
        }
        if ("Übersicht zeigt den Live-Akkustand, Temperatur, Spannung und Verlauf.\n\nLaden enthält Ladeziel und Sitzungen. Entladen zeigt Verbrauch und Laufzeit. Akku erklärt Gesundheit und Kapazität. Verlauf vergleicht Tag, Woche und Monat.\n\nDie fünf Bereiche wechselst du über die Leiste unten. Deine Akku-Messwerte bleiben lokal auf diesem Gerät.".equals(value)) {
            return "Overview shows live battery level, temperature, voltage, and history.\n\nCharging includes the charge target and sessions. Discharging shows usage and runtime. Battery health explains condition and capacity. History compares days, weeks, and months.\n\nSwitch between the five sections using the bar at the bottom. Your battery measurements stay on this device.";
        }
        if ("Nicht mit Strom verbunden".equals(value)) return "Not connected to power";
        if ("Noch keine Sitzungen abgeschlossen.".equals(value)) {
            return "No sessions completed yet.";
        }
        if ("Ladestatus: Nicht verfügbar. Ladeziel: Erreicht. Geladene Energie: 7612 mAh. Sitzungsdauer: 8 Std. 29 Min.".equals(value)) {
            return "Charge status: Not available. Charge target: Reached. Charged energy: 7612 mAh. Session duration: 8 hr 29 min.";
        }
        if ("Verlauf monatlich. Ausgewählter Zeitraum: 6 KALENDERMONATE. Lade-/Verbrauchsquote (geladen geteilt durch verbraucht): 1424 Prozent".equals(value)) {
            return "Monthly history. Selected period: 6 CALENDAR MONTHS. Charge/usage ratio (charged divided by used): 1424 percent";
        }
        if ("Messstatus: Mindestens 5 % Battery level nötig. Die Charge/usage ratio vergleicht geladene mit useder Energie und ist keine gemessene Battery-Efficiency.".equals(value)) {
            return "Measurement status: At least 5% battery level required. The charge/usage ratio compares charged with used energy and is not measured battery efficiency.";
        }
        if ("AKKU-BILANZ · 6 KALENDERMONATE".equals(value)) return "BATTERY BALANCE · 6 CALENDAR MONTHS";
        // Many callers already choose a complete English message from the app
        // locale. Do not run those sentences through German substring fixes:
        // they can turn "Overview shows" into "Show overviews" or alter
        // ordinary English words that happen to begin with a German fragment.
        if (!GERMAN_UI_MARKER.matcher(value).find()) return value;
        String result = value;
        String[][] phrases = sortedTranslationPhrases;
        if (phrases == null) {
            synchronized (AppText.class) {
                phrases = sortedTranslationPhrases;
                if (phrases == null) {
                    phrases = new String[][] {
                {"Kapazität aus ", "Capacity from "},
                {"Ladevorgang aktiv", "Charging in progress"},
                {"Letzter Ladevorgang", "Last charge session"},
                {"Zeit bis voll", "Time to full"},
                {"Zeit bis Ziel", "Time to target"},
                {"Letzte Ladung", "Last charge"},
                {"lokale 7-Tage-Schätzung", "local 7-day estimate"},
                {"Nicht gemessen", "Not measured"},
                {"Nennkapazität nicht verfügbar", "Design capacity unavailable"},
                {"Messungen · letzter Ladevorgang", "Measurements · last charge session"},
                {"Nach dem ersten Zyklus sichtbar.", "Visible after the first cycle."},
                {"Deine Akku-Messwerte bleiben auf diesem Gerät.", "Your battery measurements stay on this device."},
                {"Akkustand wird ermittelt", "Detecting battery level"},
                {"Nicht mit Strom verbunden", "Not connected to power"},
                {"Akkustand · Automatik", "Battery level · automatic"},
                {"Zum Abschluss über 95 % laden.", "Charge above 95% to complete."},
                {"Unter 25 % starten, dann in Ruhe vollladen.", "Start below 25%, then charge fully without interruption."},
                {"lokale 7 Tage", "local 7 days"},
                {"Belastung bis zum Ziel", "Usage until target"},
                {"Keine Messreihe", "No measurement series yet"},
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
                {"ANALYSEZENTRALE", "ANALYSIS CENTER"},
                {"Akkustand · Strom · Leistung · Spannung", "Battery level · current · power · voltage"},
                {"Temperatur · mAh · Wh · Bildschirmstatus", "Temperature · mAh · Wh · screen status"},
                {"Ladeziel", "Charge target"},
                {"Ladeverhalten", "Charging behavior"},
                {"Ladegeschwindigkeit", "Charging speed"},
                {"VERBLEIBENDE NUTZUNGSZEIT", "REMAINING RUNTIME"},
                {"Gemischt", "Mixed"},
                {"Messungen · letzter Ladevorgang ", "Measurements · last charge session "},
                {"mAh und Anteile sind Schätzungen aus Vordergrundzeit und lokaler Akku-Telemetrie. Eine individuelle mAh/h-Rate zeigen wir nur bei direkter App-Telemetrie; Android stellt keine exakten Akkuwerte je App bereit.", "mAh values and shares are estimates based on foreground time and local battery telemetry. An app-specific mAh/h rate is shown only when direct app telemetry is available; Android does not provide exact battery usage for each app."},
                {"Letzte 7 Tage · Schätzung; Rest ggf. nicht zuordenbar", "Last 7 days · estimate; remaining usage may be unattributed"},
                {"Letzte 24 Stunden · Schätzung; Rest ggf. nicht zuordenbar", "Last 24 hours · estimate; remaining usage may be unattributed"},
                {"Quelle: zugeordnete Akku-Telemetrie (Schätzung)", "Source: attributed battery telemetry (estimate)"},
                {"Quelle: anteilig nach Vordergrundzeit (Schätzung)", "Source: allocated by foreground time (estimate)"},
                {"Keine App-Werte zugeordnet", "No app usage attributed"},
                {"Nutzungszugriff aus", "Usage access off"},
                {"Noch keine Akku-Messwerte", "No battery measurements yet"},
                {"App-Verbrauch", "App usage"},
                {"Fehler", "Failure"},
                {" mAh zugeordnet", " mAh attributed"},
                {" Min. · ", " min · "},
                {"Export nur auf deine Auswahl; kein Konto/Abonnement.", "Export only when you choose; no account or subscription."},
                {"Wir lernen", "We're learning"},
                {"deinen Rhythmus.", "your pattern."},
                {"Deine Werte bleiben bei dir.", "Your data stays yours."},
                {"Kapazitätsmessung läuft.", "Capacity measurement in progress."},
                {"deine Geschichte.", "here."},
                {"Beobachte deinen Akku", "Monitor your battery"},
                {"Akkuverbrauch deiner Apps", "Battery usage by app"},
                {"Dein Akku im Zeitverlauf", "Your battery over time"},
                {"Dein Tagesrhythmus", "Your daily pattern"},
                {"Akkugesundheit", "Battery health"},
                {"Akkukapazität", "Battery capacity"},
                {"Akkuzustand", "Battery status"},
                {"Akkuinformationen werden geladen.", "Loading battery information."},
                {"Alles läuft", "Everything is running"},
                {"ganz entspannt.", "smoothly."},
                {"Noch nicht gemessen", "Not measured yet"},
                {"Finde Kapazität und Verschleiß heraus.", "Measure capacity and battery wear."},
                {"Messung aktualisieren", "Refresh measurement"},
                {"Messung starten", "Start measurement"},
                {"Designkapazität", "Design capacity"},
                {"Gemessene Kapazität", "Measured capacity"},
                {"AKKUSTAND · AUTOMATISCH", "BATTERY LEVEL · AUTOMATIC"},
                {"AKKUSTAND · AUTOMATIK", "BATTERY LEVEL · AUTOMATIC"},
                {"AKKUGESUNDHEIT", "BATTERY HEALTH"},
                {"AKKUSTAND", "BATTERY LEVEL"},
                {"LÄDT JETZT", "CHARGING NOW"},
                {"AKKUBETRIEB", "ON BATTERY"},
                {"UNTERWEGS MIT AKKU", "ON THE GO"},
                {"DEIN AKKU-MUSTER", "YOUR BATTERY PATTERN"},
                {"DEIN AKKU-TAGEBUCH", "YOUR BATTERY JOURNAL"},
                {"LIVE & SITZUNG", "LIVE & SESSION"},
                {"APP-VERBRAUCH · GESCHÄTZT", "APP USAGE · ESTIMATED"},
                {"ALLE APP-DETAILS ANSEHEN  →", "VIEW ALL APP DETAILS  →"},
                {"Ø VERBRAUCH", "AVERAGE USAGE"},
                {"am Ende der Entladung", "at the end of discharge"},
                {"aktueller Akkustand", "current battery level"},
                {"Gerät wird geladen", "Device is charging"},
                {"Sitzung startet beim Abstecken", "Session starts when unplugged"},
                {"Bildschirm an", "Screen on"}, {"Bildschirm aus", "Screen off"},
                {"Bildschirm an / aus", "Screen on / off"},
                {"Nach dem Abstecken", "After unplugging"},
                {"Entladung starten", "Start a discharge session"},
                {"Ladeziel erreicht", "Charge target reached"},
                {"Lädt bis ", "Charging to "},
                {"Hohe Akkutemperatur", "High battery temperature"},
                {"Akku fast leer", "Battery nearly empty"},
                {"eingestelltes Ziel", "set target"}, {"Grenzwert", "threshold"},
                {"CPU gesamt", "Total CPU"}, {"Vordergrund-App", "Foreground app"},
                {"Prozesslast", "Process load"},
                {"Normale Nutzung", "Normal use"}, {"Mehr Daten", "More data"},
                {"seit dem Abstecken", "since unplugging"},
                {"Seit dem Abstecken", "Since unplugging"},
                {"Seit dem Abstecken · geschätzte Werte", "Since unplugging · estimated values"},
                {"Ø Entladerate", "Average discharge rate"}, {"Entladerate", "Discharge rate"},
                {"Tiefschlaf", "Deep sleep"},
                {"Verbrauch seit dem Abstecken", "Usage since unplugging"},
                {"LIVE · GERÄT", "LIVE · DEVICE"},
                {"LADEBEGLEITUNG", "CHARGE COMPANION"},
                {"AKTUELL", "CURRENT"},
                {"RATE JETZT", "CURRENT RATE"},
                {"TEMPERATUR · SPANNUNG", "TEMPERATURE · VOLTAGE"},
                {"SENSOREN", "SENSORS"},
                {"Volle Kapazität", "Full capacity"},
                {"Nennkapazität", "Design capacity"},
                {"Nennwert", "Design value"},
                {"Alarm bei", "Alert at"},
                {"kein Ladestopp", "no charge stop"},
                {"Laden im Blick.", "Charging at a glance."},
                {"Bereit zum Laden.", "Ready to charge."},
                {"Fortschritt zum Ladeziel", "Progress toward charge target"},
                {"Benachrichtigt dich bei", "Notifies you at"},
                {"Zurzeit ausgeschaltet", "Currently disabled"},
                {"Live-Anzeige", "Live overlay"},
                {"Schwebt über anderen Apps", "Floats over other apps"},
                {"Über anderen Apps ausgeblendet", "Hidden over other apps"},
                {"Quelle: ", "Source: "},
                {"Status · Quelle", "Status · source"},
                {"Zeit bis Ladeziel", "Time to charge target"},
                {"Länger laden für eine Schätzung", "Charge longer for an estimate"},
                {"AKKUVERBRAUCH", "BATTERY USAGE"},
                {"ÄNDERUNG", "CHANGE"}, {"DAUER", "DURATION"}, {"GESTARTET", "STARTED"},
                {"GESCHÄTZTE RESTLAUFZEIT BIS 0 %", "ESTIMATED TIME REMAINING TO 0%"},
                {"Noch keine Messung", "No measurement yet"},
                {"Eine volle Ladung", "A full charge"},
                {"SYSTEMDATEN AKTIV", "SYSTEM DATA ACTIVE"},
                {"SO ENTSTEHT DIE SCHÄTZUNG", "HOW THE ESTIMATE IS CALCULATED"},
                {"NENNKAPAZITÄT", "DESIGN CAPACITY"},
                {"ANTIPPEN ZUM ÄNDERN", "TAP TO EDIT"},
                {"Nur lokal gespeichert.", "Stored locally only."},
                {"PRIVAT VON ANFANG AN", "PRIVATE BY DESIGN"},
                {"ANALYSEZENTRALE", "ANALYSIS CENTER"},
                {"LADEVORGANG", "CHARGING SESSION"},
                {"LADEMENGE", "CHARGE AMOUNT"},
                {"LADEN", "CHARGING"},
                {"VERBLEIBENDE NUTZUNGSZEIT", "REMAINING RUNTIME"},
                {"KAPAZITÄTSSCHÄTZUNG", "CAPACITY ESTIMATE"},
                {"LADEGESCHWINDIGKEIT", "CHARGING SPEED"},
                {"ENTLADEVORGANG", "DISCHARGE SESSION"},
                {"An ", "On "}, {" · aus ", " · off "}, {"Mittel · ", "Medium · "},
                {"Bildschirm-Aufweckungen", "screen wake-ups"},
                {"TÄGLICHE VOLLZYKLEN", "DAILY FULL CYCLES"},
                {"LADEVERSCHLEISS", "CHARGING WEAR"},
                {"LIVE-ENERGIEFLUSS", "LIVE ENERGY FLOW"},
                {"Alarm bei 80 % · kein Ladestopp", "Alert at 80% · charging will continue"},
                {" · kein Ladestopp", " · charging will continue"},
                {"LADE-SITZUNG", "CHARGING SESSION"},
                {"geschätzte Restkapazität", "estimated remaining capacity"},
                {"auf diesem Gerät.", "on this device."},
                {"Erste Sitzung wird automatisch aufgezeichnet", "Your first session is recorded automatically"},
                {"Tiefschlaf: ", "Deep sleep: "},
                {"AUSWERTUNG", "ANALYSIS"}, {"LETZTE SITZUNGEN", "RECENT SESSIONS"},
                {"VERLAUF", "HISTORY"}, {"DIAGNOSE", "DIAGNOSTICS"},
                {"Sitzungen:", "Sessions:"}, {"Ladungsmenge:", "Charge amount:"},
                {"Temp. ", "Temp. "}, {" · Spannung ", " · Voltage "},
                {"ca. ", "about "}, {" W aktueller Verbrauch", " W current usage"},
                {"Ladehistorie", "charging history"}, {"Messstrom + lokale ", "measured current + local "},
                {"Messstrom + ", "measured current + "}, {"lokale Messwerte", "local measurements"},
                {"Kapazität messen", "Measure capacity"}, {"Im gesunden Bereich", "Within healthy range"},
                {"Kapazitätsmessungen", "Capacity measurements"},
                {"Kapazitätsmessung", "Capacity measurement"},
                {"Kapazitätsverlust", "Capacity loss"}, {"Datenqualität", "Data quality"},
                {"Messung und Datenqualität", "Measurement and data quality"},
                {"ZYKLEN & THERMIK", "CYCLES & THERMALS"},
                {"Hier wächst bald", "Your story starts"},
                {"Bereit für deine erste Kurve.", "Ready for your first chart."},
                {"Nutzungsübersicht", "Usage overview"},
                {"Für diesen Zeitraum wurde keine App-Nutzung erfasst.", "No app usage was recorded for this period."},
                {"Geschätzte Kapazität", "Estimated capacity"},
                {"% Kapazität", "% capacity"},
                {"Überhitzt", "Overheated"}, {"Überspannung", "Overvoltage"},
                {"Zu kalt", "Too cold"}, {"Zu heiß", "Too hot"},
                {"Akzeptabel", "Fair"}, {"Akkuschonend", "Battery protection"},
                {"Adaptiv", "Adaptive"}, {"Leicht erhöht", "Slightly elevated"},
                {"Mäßig", "Moderate"}, {"Notfall", "Emergency"}, {"Abschaltung", "Shutdown"},
                {"Ladeende noch nicht stabil (über 25 mA)", "Charge end is not stable yet (above 25 mA)"},
                {" · Datenlücke", " · data gap"},
                {"Damit ordnet Ampere den geschätzten", "This lets Ampere attribute estimated"},
                {"Nutzungszugriff ist optional", "Usage access is optional"},
                {"Zugriff einrichten  →", "Set up access  →"},
                {"Beobachten", "Monitor"}, {"Prüfung empfohlen", "Check recommended"},
                {"Nennkapazität ", "Design capacity "}, {"Nennwert ", "Design value "},
                {"Niedrig · ", "Low · "}, {"Mittel · ", "Medium · "}, {"Gut · ", "Good · "},
                {"Messungen", "measurements"}, {"Messreihe", "measurement series"},
                {"Sobald Strom fließt, bin ich da.", "Live readings appear when current starts flowing."},
                {"angenehm kühl", "comfortably cool"}, {"Live-Sensor", "Live sensor"},
                {"Was gerade im Akku passiert", "What's happening in the battery"},
                {"Seit dem Anschließen", "Since plugging in"},
                {"Lass die Überwachung laufen, um", "Keep monitoring running to create"},
                {"lokale Verlaufseinträge zu erstellen.", "local history entries."},
                {"Kapazitätsmessung weiter unten starten", "Start capacity measurement below"},
                {"geeignete Sitzungen", "valid sessions"},
                {"Kapazitätsmessung läuft", "Capacity measurement in progress"},
                {"Zum Abschluss über 95 % laden", "Charge above 95% to complete"},
                {"Für beste Ergebnisse unter 25 % starten", "For best results, start below 25%"},
                {"Nennkapazität festlegen, um den Trend zu normieren.", "Set design capacity to normalize the trend."},
                {"Schließe weitere Ladevorgänge für den Trend ab.", "Complete more charge sessions to build the trend."},
                {"Noch keine täglichen Zykluswerte verfügbar.", "No daily cycle values available yet."},
                {"Die Überwachung zeichnet sie ab dem nächsten Messpunkt auf.", "Monitoring will record them from the next reading."},
                {"Schließe einen Ladevorgang für den lokalen Trend ab.", "Complete a charge session for a local trend."},
                {"Noch keine Sitzungen abgeschlossen.", "No sessions completed yet."},
                {"Noch keine Entladung", "No discharge yet"},
                {"noch keine Entladung", "no discharge yet"},
                {"Sitzungsverlauf", "Session history"}, {"Lade-/Entladesitzungen", "Charge/discharge sessions"},
                {"Messwerte aufgezeichnet", "Measurements recorded"},
                {"Export nur auf deine Auswahl; kein Konto/Abonnement.", "Export only when you choose; no account or subscription."},
                {"Noch keine lokalen Messwerte", "No local measurements yet"},
                {"Gestrichelt = geschätzt", "Dashed = estimated"},
                {"Schattiert = fehlender Tag", "Shaded = missing day"},
                {"Schattiert = Messlücke", "Shaded = data gap"},
                {"Punkt = letzter Messwert je Tag", "Dot = last reading of each day"},
                {"Letzte Sitzungen", "Recent sessions"}, {"Weitere Sitzungen im Verlauf", "More sessions in history"},
                {"Entladestrom", "Discharge current"}, {"Ladestrom", "Charging current"},
                {"VERLAUF & STATISTIK", "HISTORY & STATISTICS"},
                {"Lokal aus Messpunkten berechnet · keine Cloud", "Calculated locally from measurements · no cloud"},
                {"Ältere Monate können leer sein · lokale Daten: etwa ", "Older months may be blank · local data: about "},
                {"DIESER MONAT · MONATSANFANG BIS HEUTE", "THIS MONTH · START OF MONTH TO TODAY"},
                {"7 KALENDERTAGE", "7 CALENDAR DAYS"},
                {"5 KALENDERWOCHEN", "5 CALENDAR WEEKS"},
                {"6 KALENDERMONATE", "6 CALENDAR MONTHS"},
                {"Täglich", "Daily"}, {"Wöchentlich", "Weekly"}, {"Monatlich", "Monthly"},
                {"— = keine auswertbaren Messwerte", "— = no usable measurements"},
                {"Balkenhöhe je Kennzahl relativ zum Maximum", "Bar height is relative to each metric's maximum"},
                {"Zahlen = Messwerte · Farben wie Legende", "Numbers = measurements · colors match the legend"},
                {"Aufgeladen", "Charged"}, {"Batterieverbrauch", "Battery usage"},
                {"Geladen", "Charged"}, {"Ladequote", "Charge ratio"},
                {"Mär", "Mar"}, {"Mai", "May"}, {"Juni", "June"},
                {"Juli", "July"}, {"Okt", "Oct"}, {"Dez", "Dec"},
                {"Sept", "Sep"}, {"Mo.", "Mon"}, {"Di.", "Tue"},
                {"Mi.", "Wed"}, {"Do.", "Thu"}, {"Fr.", "Fri"},
                {"Sa.", "Sat"}, {"So.", "Sun"},
                {"Geladen/Usage", "Charged/usage"},
                {"Woche", "Week"}, {"Monat", "Month"}, {"day", "Day"},
                {"Werte des aktiven Bereichs", "Values for the active section"},
                {"Gut lesbare Akkuwerte · live aktualisiert", "Readable battery values · updated live"},
                {"Berechtigungen & Zugriffe", "Permissions & access"},
                {"App-Akkuverbrauch", "App battery usage"},
                {"Akku-Bilanz", "Battery balance"},
                {"Sitzungsdetails", "Session details"},
                {"Für diesen Zeitraum liegen keine auswertbaren Strommessungen vor.", "No usable current measurements are available for this period."},
                {"bedeutet fehlende Messwerte, nicht 0.", "means missing measurements, not 0."},
                {"Die Balken jeder Kennzahl werden separat skaliert.", "Each metric's bars are scaled separately."},
                {"Kapazität fehlt", "capacity missing"},
                {"kein Verbrauchswert", "no usage value"},
                {"Aus ", "From "}, {" auswertbaren Messintervallen.", " usable measurement intervals."},
                {"Jede Kennzahl hat im Diagramm eine eigene Skala.", "Each metric has its own chart scale."},
                {"Laden öffnen", "Open charging"}, {"Entladen öffnen", "Open discharging"},
                {"Datum:", "Date:"}, {"Änderung:", "Change:"}, {"Dauer:", "Duration:"},
                {"Energie:", "Energy:"}, {"Äquivalente Vollzyklen:", "Equivalent full cycles:"},
                {"Zeit Bildschirm an:", "Screen-on time:"}, {"Zeit Bildschirm aus:", "Screen-off time:"},
                {"Ladequelle:", "Charging source:"}, {"Gestartet:", "Started:"}, {"Beendet:", "Ended:"},
                {"Sitzung", "Session"}, {"Bildschirm-Aufweckungen:", "Screen wake-ups:"},
                {"mAh", "mAh"},
                {"Einstellung öffnen", "Open settings"},
                {"Letzte 24 Stunden", "Last 24 hours"},
                {"Letzte 7 Tage", "Last 7 days"},
                {"Letzte 30 Tage", "Last 30 days"},
                {"Übersicht", "Overview"}, {"Start", "Home"}, {"Laden", "Charging"},
                {"Entladen", "Discharging"}, {"Akku", "Battery"}, {"Verlauf", "History"},
                {"Gesundheit", "Health"}, {"Einstellungen", "Settings"},
                {"Aktionen", "Actions"}, {"Ladeziel", "Charge target"},
                {"Akkustand", "Battery level"}, {"Akkustrom", "Battery current"},
                {"Startbildschirm", "Home screen"}, {"Unbekannte App", "Unknown app"},
                {"Akkuspannung", "Battery voltage"}, {"Akkuleistung", "Battery power"},
                {"Akkutemperatur", "Battery temperature"}, {"Temperatur", "Temperature"},
                {"Spannung", "Voltage"}, {"Verbrauch", "Usage"}, {"Akkuverbrauch", "Battery usage"},
                {"Bildschirmzeit", "Screen time"}, {"Ladezyklen", "Charge cycles"},
                {"Lernen braucht", "Learning takes"}, {"ein wenig Zeit.", "a little time."},
                {"Systemzyklen", "System cycles"}, {"von Android gemeldet", "reported by Android"},
                {"Android-Testwert", "Android test value"}, {"Android-Akkusensor", "Android battery sensor"},
                {"Ampere-Vollzyklen", "Ampere full cycles"}, {"MESSBASIS", "MEASUREMENT BASELINE"},
                {"VOLLE KAPAZITÄT", "FULL CAPACITY"}, {"AKKUSPANNUNG", "BATTERY VOLTAGE"},
                {"SYSTEMZYKLEN", "SYSTEM CYCLES"},
                {"BILDSCHIRM", "SCREEN"}, {"VERBRAUCH", "USAGE"},
                {"aktive Nutzung", "active use"}, {"geschätzte Werte", "estimated values"},
                {"Verbrauchte Ladung", "Charge used"}, {"Verbrauchte Energie", "Energy used"},
                {"Geladen/Verbrauch", "Charged/used"}, {"Akkuverschleiß", "Battery wear"},
                {"Verschleiß", "Wear"}, {"Effizienz", "Efficiency"}, {"Sitzungen", "Sessions"},
                {"Ladeeingang", "Charging input"}, {"Akkuseite", "Battery side"},
                {"Aktueller Akkustand", "Current battery level"}, {"aktueller Akkustand", "current battery level"},
                {"Aktualisiert live", "Updated live"}, {"Warte auf Strommessung", "Waiting for current measurement"},
                {"Ladestrom", "Charging current"},
                {"Live-Daten aktualisieren", "Refresh live data"},
                {"USB-Ladegerät", "USB charger"},
                {"Ladegerät max. ", "Charger max. "},
                {"Ladehardware max. ", "Charging hardware max. "},
                {"OEM-Ladefenster ", "OEM charge window "},
                {"OEM-Limit ", "OEM limit "},
                {"Laden im Wachzustand gesperrt", "Charging paused while awake"},
                {"Laden gesperrt", "Charging paused"},
                {"Entladung erzwungen", "Forced discharge"},
                {"Normalbetrieb", "Normal operation"},
                {"Benutzerdefiniert", "Custom"},
                {"Lädt schnell", "Fast charging"},
                {"Trickle", "Trickle charging"},
                {"Adaptiv", "Adaptive"},
                {"Warte auf Energiedaten", "Waiting for energy data"},
                {"Wird nach dem Ladevorgang einbezogen", "Included after charging"},
                {"Längere Ladevorgänge verbessern die Genauigkeit", "Longer charging sessions improve accuracy"},
                {"Kabellos", "Wireless"}, {"Externe Stromquelle", "External power source"},
                {"Warte auf Android-Akkuwert", "Waiting for Android battery value"},
                {"Nicht verfügbar", "Not available"}, {"nicht verfügbar", "not available"},
                {"Keine Daten", "No data"}, {"Noch keine Daten", "No data yet"},
                {"Noch keine Verbrauchsdaten für die Quote", "No usage data for the ratio yet"},
                {"Quote = geladen ÷ Verbrauch · EFC = Vollzyklen, kein Zellwirkungsgrad.", "Ratio = charged ÷ used · EFC = full cycles, not cell efficiency."},
                {"Noch keine abgeschlossenen Lade- oder Entladevorgänge.", "No completed charge or discharge sessions yet."},
                {"Sitzung antippen für Details", "Tap a session for details"},
                {"Wird gemessen", "Measuring"}, {"Noch offen", "Pending"}, {"gerade eben", "just now"},
                {"BERECHNET", "CALCULATED"}, {"GESCHÄTZT", "ESTIMATED"}, {"SYSTEM", "SYSTEM"},
                {"LIVE", "LIVE"}, {"AKKUVERBRAUCH", "BATTERY USAGE"}, {"Lade-/Verbrauchsquote", "charge/usage ratio"},
                {"Akkumesswerte bleiben auf diesem Gerät.", "Battery measurements stay on this device."},
                {"Weitere Sitzungen im Verlauf", "More sessions in history"},
                {"Verbrauch den verwendeten Apps zu.", "Usage attributed to the apps used."},
                {"Wärme prüfen", "Check temperature"}, {"Unbekannt", "Unknown"},
                {"Status unbekannt", "Status unknown"},
                {"Temperaturwarnung", "Temperature alert"}, {"Tiefstandwarnung", "Low battery alert"},
                {"Datenerfassung", "Data collection"}, {"Hintergrundüberwachung", "Background monitoring"},
                {"Daten & Datenschutz", "Data & privacy"}, {"Nutzungsanalyse", "Usage analytics"},
                {"Sicherung & Wiederherstellung", "Backup & restore"},
                {"Benachrichtigungen", "Notifications"}, {"Berechtigungen prüfen", "Check permissions"},
                {"Ladeziel & Ladealarm", "Charge target & alert"}, {"Overlay-Berechtigung", "Overlay permission"},
                {"Nach Updates suchen", "Check for updates"}, {"Kurzanleitung", "Quick guide"},
                {"Aktuellen Status kopieren", "Copy current status"}, {"Aktuellen Status teilen", "Share current status"},
                {"Gesundheitsbasis zurücksetzen", "Reset health baseline"},
                {"Temperaturwarnung · ab ", "Temperature alert · at "}, {"Temperaturwarnung · aus", "Temperature alert · off"},
                {"Tiefstandwarnung · bei ", "Low battery alert · at "}, {"Tiefstandwarnung · aus", "Low battery alert · off"},
                {"Datenerfassung · alle ", "Data collection · every "}, {"Minuten", "minutes"},
                {"Temperaturwarnung · Rücksetzung 3 °C darunter", "Temperature alert · resets 3 °C below"},
                {"Tiefstandwarnung · Rücksetzung mit 3 % Abstand", "Low battery alert · resets with a 3% margin"},
                {"Datenerfassung · nur lokal", "Data collection · local only"},
                {"Aus", "Off"}, {"Ab ", "At "}, {"Bei ", "At "}, {"oder weniger", "or less"},
                {"(empfohlen)", "(recommended)"}, {"Alle ", "Every "},
                {"Abbrechen", "Cancel"}, {"Speichern", "Save"}, {"Schließen", "Close"},
                {"Einstellungen schließen", "Close settings"}, {"Akku-Einstellungen", "Battery settings"},
                {"Aktiv", "Active"}, {"Nicht erteilt", "Not granted"}, {"optional", "optional"},
                {"Optional · nicht aktiviert", "Optional · not enabled"},
                {"Live-Status und Alarme", "Live status and alerts"},
                {"App-Nutzungszugriff", "App usage access"}, {"Verbrauch je App", "Usage by app"},
                {"Nicht erteilt · optional", "Not granted · optional"},
                {"Overlay", "Overlay"}, {"Live-Anzeige", "Live overlay"},
                {"Einige Zugriffe fehlen oder wurden von Android zurückgesetzt.", "Some accesses are missing or were reset by Android."},
                {"Ampere prüft sie beim Öffnen erneut", "Ampere checks them again when opened"},
                {"abgelehnte optionale Zugriffe melden wir höchstens monatlich.", "we remind you about declined optional accesses at most once a month."},
                {"Widerrufe erkennen wir beim nächsten Öffnen.", "We detect revoked access the next time you open the app."},
                {"Benachrichtigungen ermöglichen Live-Status und Ladealarme.", "Notifications enable live status and charge alerts."},
                {"App-Nutzungszugriff zeigt den Verbrauch je App", "App usage access shows usage by app"},
                {"Overlay zeigt die Live-Anzeige über anderen Apps.", "Overlay shows the live display over other apps."},
                {"Diese beiden Zugriffe sind optional.", "These two accesses are optional."},
                {"Ampere prüft den Status beim Öffnen erneut.", "Ampere checks their status again when opened."},
                {"Tippe auf einen Eintrag, um ihn zu ändern.", "Tap an entry to change it."},
                {"Temperaturwarnung ausgeschaltet", "Temperature alert turned off"},
                {"Tiefstandwarnung ausgeschaltet", "Low battery alert turned off"},
                {"Hintergrundüberwachung ist ab Android 6 verfügbar.", "Background monitoring is available from Android 6."},
                {"Einstellungen", "Settings"},
                {"Lokale Daten löschen?", "Delete local data?"},
                {"Gesundheitsbasis zurücksetzen?", "Reset health baseline?"},
                {"Backup gespeichert.", "Backup saved."}, {"Backup wiederhergestellt.", "Backup restored."},
                {"Backup konnte nicht gespeichert werden.", "Backup could not be saved."},
                {"Backup ist ungültig oder konnte nicht gelesen werden.", "Backup is invalid or could not be read."},
                {"CSV-Export gespeichert.", "CSV export saved."}, {"Diagnosebericht gespeichert.", "Diagnostic report saved."},
                {"CSV-Export konnte nicht gespeichert werden.", "CSV export could not be saved."},
                {"Diagnosebericht konnte nicht gespeichert werden.", "Diagnostic report could not be saved."},
                {"Forschungs-Export gespeichert.", "Research export saved."},
                {"Forschungs-Export konnte nicht gespeichert werden.", "Research export could not be saved."},
                {"Live-Daten aktualisiert.", "Live data refreshed."},
                {"Akkuüberwachung", "Battery monitoring"}, {"Ladealarm", "Charge alert"},
                {"Warte auf Akkudaten", "Waiting for battery data"},
                {"Letzter Entladevorgang", "Last discharge"},
                {"Zusammenfassung.", "summary."},
                {"Bereit für Entladung.", "Ready for discharge."},
                {"Startet beim Abstecken.", "Starts when unplugged."},
                {"Telemetrie-Schätzung", "Telemetry estimate"},
                {"Vordergrundzeit-Schätzung", "Foreground-time estimate"},
                {"Akkuverbrauch nicht verfügbar", "Battery usage unavailable"},
                {"Zurück", "Back"}, {"SCHLIESSEN", "CLOSE"},
                {"ausgewählt", "selected"},
                {"Verlauf täglich", "Daily history"}, {"Verlauf wöchentlich", "Weekly history"},
                {"Verlauf monatlich", "Monthly history"},
                {"CSV exportieren", "Export CSV"},
                {"Exakte Werte im Bilanzdiagramm anzeigen", "Show exact values in the balance chart"},
                {"Kapazitätsmessung stoppen", "Stop capacity measurement"},
                {"Nennkapazität bearbeiten", "Edit design capacity"},
                {"Restlaufzeit bei dauerhaft eingeschaltetem Bildschirm", "Runtime with screen always on"},
                {"Restlaufzeit bei ausgeschaltetem Bildschirm", "Runtime with screen off"},
                {"Restlaufzeit bei normaler Nutzung", "Runtime with normal use"},
                {"Bildschirm dauerhaft an", "Screen always on"},
                {"Ladealarm: ", "Charge alert: "}, {"Live-Anzeige: ", "Live overlay: "},
                {"Ladeziel: ", "Charge target: "},
                {"Akku ist voll", "Battery is full"},
                {"Voll geladen", "Fully charged"},
                {"Akku wird geladen", "Charging now"},
                {"Akku entlädt sich", "Battery is discharging"},
                {"Akkubetrieb aktiv", "On battery"},
                {"Keine Daten verfügbar.", "No data available."},
                {"Bereit für die Nutzung.", "Ready to use."},
                {"Laden wird überwacht.", "Charging is monitored."},
                {"Verbrauch wird live erfasst.", "Usage is tracked live."},
                {"Überwachung läuft weiter.", "Monitoring continues."},
                {"Live-Akkuanzeige", "Live battery overlay"},
                {"App-Aktualisierungen", "App updates"},
                {"Update verfügbar · ", "Update available · "},
                {"Ampere-Update verfügbar · ", "Ampere update available · "},
                {"Update", "Update"}, {"verfügbar", "available"},
                {"Update verfügbar", "Update available"},
                {"Tippen zum Herunterladen", "Tap to download"},
                {"Aktualisierung suchen", "Check for updates"},
                {"Die kostenlose APK wird vor der Installation auf Hash, Paketname, Version und Release-Signatur geprüft. Android fragt anschließend noch einmal nach deiner Bestätigung.", "Before installation, the free APK is checked for its hash, package name, version and release signature. Android will ask for your confirmation once more."},
                {"Später", "Later"}, {"Herunterladen", "Download"},
                {"Im Browser öffnen", "Open in browser"},
                {"Tippen, um die kostenlose Aktualisierung zu prüfen", "Tap to verify the free update"},
                {"Download ist auf diesem Gerät nicht verfügbar.", "Downloads are not available on this device."},
                {"Update konnte nicht sicher vorbereitet werden.", "The update could not be prepared safely."},
                {"Update wird heruntergeladen …", "Downloading update …"},
                {"Suche nach Aktualisierungen …", "Checking for updates …"},
                {"Ein anderer Update-Vorgang läuft bereits.", "Another update operation is already running."},
               {"Update-Prüfung", "Update check"},
                {"Erneut prüfen", "Check again"},
                {"Der Server meldet keine neuere Version als ", "The server reports no newer version than "},
                {"Die konfigurierte Update-Adresse wurde aus Sicherheitsgründen abgelehnt.", "The configured update address was rejected for security reasons."},
                {"Der Update-Server antwortete mit HTTP ", "The update server responded with HTTP "},
                {". Prüfe die Internetverbindung und versuche es erneut.", ". Check your internet connection and try again."},
                {"Die Update-Datei ist ungewöhnlich groß und wurde aus Sicherheitsgründen abgelehnt.", "The update file is unusually large and was rejected for security reasons."},
                {"Die Update-Datei überschreitet die erlaubte Größe.", "The update file exceeds the allowed size."},
                {"Die APK-Adresse wurde aus Sicherheitsgründen abgelehnt.", "The APK address was rejected for security reasons."},
                {"Die Update-Prüfung konnte nicht abgeschlossen werden (", "The update check could not be completed ("},
                {"). Prüfe Internetzugriff und Datum/Uhrzeit des Geräts.", "). Check internet access and the device date/time."},
                {"Kostenloses Update wird heruntergeladen", "Free update is downloading"},
                {"Update konnte nicht gestartet werden.", "The update could not be started."},
                {"Installation einmal erlauben", "Allow installation once"},
                {"Android braucht deine Freigabe, damit Ampere eine APK zur Installation übergeben darf. Es wird noch nichts heruntergeladen. Nach der Freigabe erscheint das Update hier erneut; Android fragt vor der Installation zusätzlich nach deiner Bestätigung.", "Android needs your permission before Ampere can hand over an APK for installation. Nothing is downloaded yet. After you allow it, the update will appear here again; Android will also ask for confirmation before installing."},
                {"Installationsfreigabe bitte in den App-Einstellungen aktivieren.", "Please enable installation permission in the app settings."},
                {"Update-Download fehlgeschlagen.", "Update download failed."},
                {"Update verworfen: Datei ist zu groß.", "Update discarded: file is too large."},
                {"Update-Datei konnte nicht geöffnet werden.", "The update file could not be opened."},
                {"Update verworfen: Hash, Version oder Release-Signatur ungültig.", "Update discarded: hash, version or release signature is invalid."},
                {"Update konnte nicht sicher gestartet werden.", "The update could not be started safely."},
                {"Bitte die heruntergeladene APK aus den Dateien öffnen.", "Please open the downloaded APK from Files."},
                {"Berechtigung fehlt", "Permission not granted"},
                {"App-Benachrichtigungen sind deaktiviert", "App notifications are disabled"},
                {"Kanal „Akkuüberwachung“ ist deaktiviert", "The “Battery monitoring” channel is disabled"},
                {"aktiviert", "Enabled"},
                {"uneingeschränkt", "Unrestricted"},
                {"vom System optimiert", "Optimized by the system"},
                {"Kein aktuelles Dienstsignal (letztes Signal älter als 30 Min. oder nicht verfügbar)", "No recent service signal (last signal is older than 30 minutes or unavailable)"},
                {"Dienst zuletzt gerade eben bestätigt", "Service confirmed just now"},
                {"Dienst zuletzt vor ", "Service last confirmed "},
                {" Min. bestätigt", " minutes ago"},
                {"Live-Benachrichtigung:", "Live notification:"},
                {"Hintergrunddienst:", "Background service:"},
                {"Akkuoptimierung:", "Battery optimization:"},
                {"Wenn kein aktuelles Dienstsignal vorliegt, öffne Ampere einmal; dabei wird die Überwachung neu gestartet.", "If there is no recent service signal, open Ampere once; monitoring will be restarted."},
                {"Android kann sie nach „Stopp erzwingen“ oder durch Hersteller-Energiesparregeln anhalten.", "Android may stop it after “Force stop” or because of manufacturer power-saving rules."},
                {"Ampere überwacht den Akku über einen sichtbaren Android-Dienst.", "Ampere monitors the battery through a visible Android service."},
                {"Sind Benachrichtigungen gesperrt, kann der Dienst trotzdem laufen, aber seine Live-Anzeige fehlt.", "The service may still run when notifications are blocked, but its live display is unavailable."},
                {"Energiesparfunktionen des Herstellers oder „Stopp erzwingen“ können die Überwachung anhalten.", "Manufacturer power-saving features or “Force stop” can stop monitoring."},
                {"Im Tiefschlaf darf Android Aktualisierungen verzögern; Ampere hält das Gerät bewusst nicht dauerhaft wach, um keinen zusätzlichen Akkuverbrauch zu verursachen.", "During deep sleep, Android may delay updates; Ampere deliberately does not keep the device awake to avoid extra battery use."},
                {"Ampere überwacht den Akku", "Ampere is monitoring your battery"},
                {"Akkubetrieb", "On battery"}, {"Akku entlädt", "Battery discharging"},
                {"Akkumesswerte werden auf diesem Gerät gespeichert", "Battery measurements are stored on this device"},
                {"Laden erkannt", "Charging detected"}, {"nicht gemessen", "not measured"},
                {"Gut", "Good"}, {"Tabs:", "Tabs:"}, {"Aktiver Tab:", "Active tab:"},
                {"Grafische ", "Graphical "}, {"-Ansicht", " view"},
                {"Die Werte stehen oben in der Großschrift-Ansicht.", "The values are shown above in large-text view."},
                {"Ladeziel zwischen 50 und 100 Prozent", "Charge target between 50 and 100 percent"},
                {"Starte die Kapazitätsmessung getrennt vom Ladegerät unter 25 %.", "Start the capacity measurement unplugged and below 25%."},
                {"Gib die werkseitige Kapazität in mAh ein. Mit 0 wird automatisch der von Android gemeldete Wert verwendet.", "Enter the factory capacity in mAh. Enter 0 to use the value reported by Android automatically."},
                {"Akkustatus kopiert.", "Battery status copied."},
                {"Strom", "Current"}, {"Quelle", "Source"}, {"lokal auf Android", "local on Android"},
                {"Version", "Version"},
                {"Lokale Akkuüberwachung · jede Sekunde", "Local battery monitoring · every second"},
                {"Prozent", "percent"}, {"Stunde", "hour"}, {"Stunden", "hours"},
                {"Minute", "minute"}, {"Minuten", "minutes"}, {"Tag", "day"}, {"Tage", "days"},
                {"Home measurement", "Start measurement"},
                // Resolve fragments after the broader vocabulary replacements above.
                {"Temperature alert · ab ", "Temperature alert · at "},
                {"Temperaturewarnung · ab ", "Temperature alert · at "},
                {"Low battery alert · bei ", "Low battery alert · at "},
                {"Data collection · alle ", "Data collection · every "},
                {"HISTORY & STATISTIK", "HISTORY & STATISTICS"},
                {"Batterieverbrauch", "Battery usage"}, {"Batterieverschleiß", "Battery wear"},
                {"Batteryverbrauch", "Battery usage"}, {"Batteryverschleiß", "Battery wear"},
                {"Laderate ", "Charge rate "}, {"Strom nicht verfügbar", "Current unavailable"},
                {"Temperatur nicht verfügbar", "Temperature unavailable"},
                {"Restenergie", "Remaining energy"}, {"Android-Zustand", "Android status"},
                {"Kapazitätsniveau", "Capacity level"}, {"Ladeprofil", "Charging profile"},
                {"Ladealgorithmus", "Charging algorithm"}, {"Ladeverhalten", "Charging behavior"},
                {"Thermik", "Thermal status"}, {"Gesundheit", "Health"}, {"Schätzung", "Estimate"},
                {"Lokale Akkuüberwachung", "Local battery monitoring"}, {"Laden erkannt", "Charging detected"},
                {"Bildschirm- und Hintergrundverbrauch lokal erfasst", "Screen and background usage recorded locally"},
                {"Batterystand", "Battery level"}, {"daye", "days"},
                {"History täglich", "Daily history"}, {"History wöchentlich", "Weekly history"},
                {"History monatlich", "Monthly history"}, {" von ", " of "},
                {"Charged/Usage", "Charge/usage ratio"},
                {"Rate nicht verfügbar", "Rate unavailable"},
                {"EFC nicht verfügbar", "EFC unavailable"},
                {"Battery level beim Home", "Battery level at start"},
                {"Battery level beim Start", "Battery level at start"},
                {"Battery level aktuell", "Current battery level"},
                // The broad navigation word "Start" must not alter "Starte ..." in dialogs.
                {"Homee die Kapazitätsmessung getrennt vom Ladegerät unter 25 %.", "Start the capacity measurement unplugged and below 25%."},
                {"Home a discharge session", "Start a discharge session"},
                {"Batterystatus kopiert.", "Battery status copied."},
                {"Since unplugging · geschätzte Werte", "Since unplugging · estimated values"},
                {"STROM", "CURRENT"}, {"LEISTUNG", "POWER"},
                {"Berühren zum Beenden", "Touch to exit"},
                {"anzeigen", "show"},
                // Correct compounds affected by the broad "Akku" replacement.
                {"Batteriebetrieb", "On battery"},
                {"Batterybetrieb", "On battery"},
                {"On battery aktiv", "On battery"},
                {"Overview show", "Show overview"}, {"Charging show", "Show charging"},
                {"Discharging show", "Show discharging"}, {"Battery show", "Show battery"},
                {"History show", "Show history"},
                {"Healthsbasis zurücksetzen", "Reset health baseline"},
                {"Lokale Daten löschen", "Delete local data"},
                {"App-Sprache", "App language"},
                {"Deutsch", "German"},
                {"Englisch", "English"},
                {"AKKU-BILANZ", "BATTERY BALANCE"},
                {"KALENDERTAGE", "CALENDAR DAYS"},
                {"KALENDERWOCHEN", "CALENDAR WEEKS"},
                {"KALENDERMONATE", "CALENDAR MONTHS"},
                // Keep the full month name intact after the short "Sept" label mapping.
                {"Sepember", "September"},
                // Repair phrases affected by the broad navigation-word replacements above.
                {"Charging erkannt", "Charging detected"}, {"Activeer Tab:", "Active tab:"},
                {"Batterystrom", "Battery current"}, {"Temperaturee", "Temperature"},
                {"Grad Celsius", "degrees Celsius"},
                {"Battery ist voll", "Battery is full"}, {"Battery wird geladen", "Charging now"},
                {"Battery entlädt sich", "Battery is discharging"}, {"Battery fast leer", "Battery nearly empty"},
                {"Ladestatus", "Charge status"}, {"Erreicht", "Reached"},
                {"Charge rate bei ", "Charge rate with "}, {"Chargede Energy", "Charged energy"},
                {"Sessionsdauer", "Session duration"}, {"Netzteil", "Power adapter"},
                {"Std.", "hr"}, {"Min.", "min."},
                {"Geschätzte Vollkapazität", "Estimated full capacity"},
                {"Messquelle", "Measurement source"}, {"Keine Messung", "No measurement"},
                {"Mindestens 5 % Battery level nötig", "At least 5% battery level required"},
                {"Belastung bis zum Charge target", "Wear at charge target"},
                {"Kapazität und Belastung sind Estimateen, keine direkte chemische Messung.", "Capacity and wear are estimates, not a direct chemical measurement."},
                {"History Monthlich", "Monthly history"}, {"Offgewählter Zeitraum", "Selected period"},
                {"Lade-/Usagesquote", "Charge/usage ratio"},
                {"geladen geteilt durch verbraucht", "charged divided by used"},
                {"aufgeladen", "charged"}, {"verbraucht", "used"},
                {"Die Lade-/Usagesquote vergleicht geladene mit verbrauchter Energie und ist keine gemessene Battery-Efficiency. Balkenwerte (jede Kennzahl ist separat skaliert):", "The charge/usage ratio compares charged with used energy and is not measured battery efficiency. Bar values (each metric is scaled separately):"},
                {"EFC sind äquivalente Vollzyklen, kein direkt gemessener chemischer Healthsverlust.", "EFC are equivalent full cycles, not a directly measured chemical health loss."}
                ,{"Messstatus", "Measurement status"}, {"useder", "used"},
                {"Battery-Efficiency", "battery efficiency"},
                {"Die Charge/usage ratio vergleicht geladene mit useder Energie und ist keine gemessene Battery-Efficiency. Balkenwerte (jede Kennzahl ist separat skaliert):", "The charge/usage ratio compares charged with used energy and is not measured battery efficiency. Bar values (each metric is scaled separately):"},
                {"Die Charge/usage ratio vergleicht geladene mit used Energie und ist keine gemessene battery efficiency.", "The charge/usage ratio compares charged with used energy and is not measured battery efficiency."},
                {"Usage wird live erfasst.", "Usage is tracked live."},
                {"Sehr gut", "Very good"},
                {"Batterytemperatur", "Battery temperature"},
                {"aktueller Current + lokale Messwerte", "current + local measurements"},
                {"aktueller Current", "current"},
                {"lokale 7 days", "local 7 days"},
                {"lokale 7-Tage-Schätzung", "local 7-day estimate"},
                {"lokale 7-days-Schätzung", "local 7-day estimate"},
                {"lokale 7-days-Estimate", "local 7-day estimate"},
                {"Letzte Sessionen", "Recent sessions"},
                {"Letzte Sessions", "Recent sessions"},
                {"Weitere Sessionen im History", "More sessions in history"},
                {"Weitere Sessions im History", "More sessions in history"},
                {"Nicht verbunden", "Not connected"},
                {"Die Update check konnte nicht abgeschlossen werden (", "The update check could not be completed ("},
                {"Download ist auf diesem Gerät not available.", "Download is not available on this device."},
                {"Live-Batterymesswerte werden auf dem Screen ongezeigt", "Live battery measurements are shown on screen"},
                {"Charge target reached · Gerät lädt weiter", "Charge target reached · Device is still charging"},
                {"Nicht mit Current verbunden", "Not connected to power"},
                {"Noch keine Usagesdaten für die Quote", "No usage data for the ratio yet"},
                {"lokale Historyseinträge zu erstellen.", "to create local history entries."},
                {"Batterymesswerte bleiben auf diesem Gerät.", "Battery measurements stay on this device."},
                {"Temperaturewarnung · Rücksetzung 3 °C darunter", "Temperature alert · reset 3 °C below"},
                {"Low battery alert · Rücksetzung mit 3 % Abstand", "Low battery alert · reset with a 3% margin"},
                {"Settings schließen", "Close settings"},
                {"Notifications ermöglichen Live-Status und Charge alerte.", "Notifications enable live status and charge alerts."},
                {"App usage access zeigt den Usage je App", "App usage access shows usage by app"},
                {"Overlay zeigt die Live overlay über anderen Apps.", "Overlay shows the live display over other apps."},
                {"Background monitoring ist ab Android 6 verfügbar.", "Background monitoring is available from Android 6 onward."},
                {"Batteryüberwachung", "Battery monitoring"},
                {"No data verfügbar.", "No data available."},
                {"Charging wird überwacht.", "Charging is monitored."},
                {"Sobald Current fließt, bin ich da.", "I will appear as soon as current flows."},
                {"Was gerade im Battery passiert", "What is happening in the battery now"},
                {"Noch keine Sessionen abgeschlossen.", "No sessions completed yet."},
                {"Zeitraum: bis zu 30 lokale days", "Period: up to 30 local days"},
                {"Punkt = letzter Messwert je day", "Dot = last measurement per day"},
                {"Gemessener Battery level", "Measured battery level"},
                {"Ladealarm ausschalten · derzeit aktiv", "Turn off charge alert · currently active"},
                {"Live overlay ausschalten · derzeit aktiv", "Turn off live overlay · currently active"},
                {"Gesamtzähler im dayssverlauf", "Daily total counter"},
                {"Session antippen für Details", "Tap a session for details"},
                {"Source: lokale EFC-Estimate", "Source: local EFC estimate"},
                {"Akkustand · 7 Tage", "Battery level · 7 days"},
                {"Akkustand · 30 Tage", "Battery level · 30 days"},
                {"..", "."}
                    };
                    // Sort once: AppText is called for many labels per frame.
                    // Stable sorting preserves the existing order for equal keys.
                    Arrays.sort(phrases, new Comparator<String[]>() {
                        @Override public int compare(String[] left, String[] right) {
                            return Integer.compare(right[0].length(), left[0].length());
                        }
                    });
                    sortedTranslationPhrases = phrases;
                }
            }
        }
        // Protect translated fragments while processing the remaining source
        // text. Without this, a German source word can accidentally match a
        // word inside the English result of an earlier, longer phrase.
        String[] protectedResults = new String[phrases.length];
        int protectedCount = 0;
        for (String[] phrase : phrases) {
            if (phrase[0].isEmpty() || !result.contains(phrase[0])) continue;
            String token = "\uE000" + protectedCount + "\uE001";
            result = result.replace(phrase[0], token);
            protectedResults[protectedCount] = phrase[1];
            protectedCount++;
        }
        for (int i = 0; i < protectedCount; i++) {
            result = result.replace("\uE000" + i + "\uE001", protectedResults[i]);
        }
        return result;
    }

    private static String translateServiceHeartbeatAge(String value) {
        Matcher matcher = GERMAN_SERVICE_HEARTBEAT_AGE.matcher(value);
        if (!matcher.find()) return value;
        StringBuffer translated = new StringBuffer();
        do {
            String number = matcher.group(1);
            String unit = "1".equals(number) ? " minute ago" : " minutes ago";
            matcher.appendReplacement(translated, Matcher.quoteReplacement(
                    "Service last confirmed " + number + unit));
        } while (matcher.find());
        matcher.appendTail(translated);
        return translated.toString();
    }

    /** Full translations for preference rows whose digits and units must remain intact. */
    private static String translateExactPreference(String value) {
        switch (value) {
            case "Aus": return "Off";
            case "Ab 40 °C": return "At 40 °C";
            case "Ab 45 °C (empfohlen)": return "At 45 °C (recommended)";
            case "Ab 50 °C": return "At 50 °C";
            case "Ab 55 °C": return "At 55 °C";
            case "Bei 10 % oder weniger": return "At 10% or less";
            case "Bei 15 % oder weniger (empfohlen)": return "At 15% or less (recommended)";
            case "Bei 20 % oder weniger": return "At 20% or less";
            case "Bei 25 % oder weniger": return "At 25% or less";
            case "Bei 30 % oder weniger": return "At 30% or less";
            case "Alle 5 Minuten": return "Every 5 minutes";
            case "Alle 15 Minuten (empfohlen)": return "Every 15 minutes (recommended)";
            case "Alle 30 Minuten": return "Every 30 minutes";
            case "Alle 60 Minuten": return "Every 60 minutes";
            default: return null;
        }
    }

    /** Exact UI phrases that would otherwise be damaged by word-level replacements. */
    private static String translateExactUiPhrase(String value) {
        switch (value) {
            case "Dauer": return "Duration";
            case "Gestartet": return "Started";
            case "Fertig": return "Done";
            case "Weiter": return "Continue";
            case "Löschen": return "Delete";
            case "Zustimmen und aktivieren": return "Agree and enable";
            case "aktiv": return "active";
            case "ein": return "on";
            case "App-Sprache": return "App language";
            case "Benutzerdefiniert": return "Custom";
            case "Deutsch": return "German";
            case "Englisch": return "English";
            case "Tag": return "Day";
            case "Woche": return "Week";
            case "Monat": return "Month";
            case "AUSWERTUNG": return "ANALYSIS";
            case "T min/Ø/max": return "Temp. min/avg/max";
            case "Telemetrie T min/Ø/max": return "Telemetry temp. min/avg/max";
            case "Charged/Usage": return "Charge/usage ratio";
            case "LIVE-TELEMETRIE": return "LIVE TELEMETRY";
            case "DETAILANALYSE": return "DETAILED ANALYSIS";
            case "MANUELL": return "MANUAL";
            case "TESTDATEN": return "TEST DATA";
            case "Sehr gut": return "Very good";
            case "NICHT BEREIT": return "NOT READY";
            case "LÄDT JETZT": return "CHARGING NOW";
            case "AKKUBETRIEB": return "ON BATTERY";
            case "Android-Testwert": return "Android test value";
            case "Lernen braucht": return "Learning takes";
            case "Live-Anzeige": return "Live overlay";
            case "Bildschirm an": return "Screen on";
            case "Bildschirm aus": return "Screen off";
            case "Bildschirm an / aus": return "Screen on / off";
            case "Speichern": return "Save";
            case "Ausschalten": return "Turn off";
            case "Benachrichtigungseinstellungen öffnen": return "Open notification settings";
            case "Akku-Einstellungen öffnen": return "Open battery settings";
            case "Nach Updates suchen": return "Check for updates";
            case "Kurzanleitung": return "Quick guide";
            case "Aktuellen Status kopieren": return "Copy current status";
            case "Aktuellen Status teilen": return "Share current status";
            case "Ausgewählter Zeitraum": return "Selected period";
            case "Ampere-Live-Anzeige aktiv": return "Ampere live display is active";
            case "Live-Akkumesswerte werden auf dem Bildschirm angezeigt":
                return "Live battery measurements are shown on screen";
            case "Akkuverbrauch deiner Apps anzeigen": return "Show battery usage by app";
            case "Akkustrom live": return "Live battery current";
            case "Temperatur": return "Temperature";
            case "Android-Zustand": return "Android status";
            case "Datum": return "Date";
            case "Typ": return "Type";
            case "Verlust": return "Level drop";
            case "Leistungsaufnahme": return "Power draw";
            case "Energie": return "Energy";
            case "Vergangene Zeit": return "Elapsed time";
            case "Restlaufzeit · Screen-on": return "Runtime · screen on";
            case "Leistung Min / Ø / Max": return "Power min / avg / max";
            case "Min / Ø / Max": return "Min / avg / max";
            case "Voll": return "Full";
            case "Niedrig": return "Low";
            case "Hoch": return "High";
            case "Letzte Entladephase": return "Last discharge phase";
            case "Aktuelle Entladephase": return "Current discharge phase";
            case "Noch keine Ladebasis": return "No charge baseline yet";
            case "Noch kein Verbrauch": return "No usage yet";
            case "Seit Ladebasis": return "Since baseline";
            case "Seit Abstecken": return "Since unplugging";
            case "Seit voller Ladung": return "Since full charge";
            case "Akku fehlt": return "Battery not detected";
            case "Letzte Sitzung": return "Last session";
            case "Momentanstrom": return "Instantaneous current";
            case "Quelle: Android/BMS-Zähler": return "Source: Android/BMS counter";
            case "Quelle: lokale EFC-Schätzung": return "Source: local EFC estimate";
            case "Max. Ladeleistung nicht verfügbar": return "Max. charging power unavailable";
            case "Android-Batterie-API": return "Android battery API";
            case "Batterie-Treiber": return "Battery driver";
            case "Android-Systemwert": return "Android system value";
            case "lokale Lademessungen": return "local charging measurements";
            case "Batterie-Treiber-SoH": return "Battery driver SoH";
            case "BMS-/Treiberwert": return "BMS/driver value";
            case "keine Messung": return "no measurement";
            case "Nutzungszugriff aus": return "Usage access off";
            case "Ab 5 Min.": return "After 5 min.";
            case "Standby-Modell": return "Standby model";
            case "Warte auf Energiedaten": return "Waiting for energy data";
            case "Wird nach dem Ladevorgang einbezogen":
                return "Included after charging";
            case "Längere Ladevorgänge verbessern die Genauigkeit":
                return "Longer charging sessions improve accuracy";
            case "Mittel": return "Medium";
            case "Akzeptabel": return "Fair";
            case "Abschaltung": return "Shutdown";
            case "Kabellos": return "Wireless";
            case "Kein Konto · kein Abo · Export nur auf Wunsch":
                return "No account · no subscription · export only when you choose";
            case "Akkustatus teilen": return "Share battery status";
            case "Derzeit aktiv.": return "Currently active.";
            case "Noch keine automatische Sicherung angefordert.":
                return "No automatic backup has been requested yet.";
            case "Letzte automatische Sicherungsanforderung: ":
                return "Last automatic backup request: ";
            case ". Android steuert Dienst und Zeitpunkt der Sicherung.":
                return ". Android controls the backup service and timing.";
            case "Sicherung erstellen": return "Create backup";
            case "Sicherung wiederherstellen": return "Restore backup";
            case "Lokale Daten gelöscht.": return "Local data deleted.";
            case "Basis zurücksetzen": return "Reset baseline";
            case "Gesundheitsbasis zurückgesetzt; Verlauf bleibt erhalten.":
                return "Health baseline reset; history was kept.";
            case "Noch keine Reihe": return "No readings yet";
            case "Noch keine Basis": return "No baseline yet";
            case "Messung ": return "Measurement ";
            case "Messung": return "Measurement";
            case "Prozentpunkte": return "percentage points";
            case "SITZUNGSANALYSE": return "SESSION ANALYSIS";
            case "ZYKLEN & THERMIK": return "CYCLES & THERMALS";
            case "Ladungsmenge": return "Charge amount";
            case "Sitzungsdauer": return "Session duration";
            case "Systemzyklen": return "System cycles";
            case "Zeit, Ladung und Ruhephasen": return "Time, charge, and idle periods";
            case "Messpunkte": return "Readings";
            case "Zyklen sauber getrennt": return "Cycles tracked separately";
            case "Gesamt geladen": return "Total charged";
            case "Gesamt geladen: ": return "Total charged: ";
            case "Temperatur Min / Ø / Max": return "Temperature min / avg / max";
            case "Niedrig · 1 Messung": return "Low · 1 reading";
            case "gesammelt · Sitzung läuft": return "collected · session in progress";
            case "lokal gespeichert": return "stored locally";
            case "noch keine Telemetrie": return "no telemetry yet";
            case " · Wärme prüfen": return " · check temperature";
            case " · Thermik ": return " · thermal status ";
            case "Nicht aktiv": return "Inactive";
            case "Live-Anzeige ausschalten · derzeit aktiv":
                return "Turn off live display · currently active";
            case "Live-Anzeige einschalten · derzeit aus":
                return "Turn on live display · currently off";
            case "Ladealarm ausschalten · derzeit aktiv":
                return "Turn off charge alert · currently active";
            case "Ladealarm einschalten · derzeit aus":
                return "Turn on charge alert · currently off";
            case "Akkualterung": return "Battery aging";
            case "Noch keine Telemetrie": return "No telemetry yet";
            case " Messwerte": return " readings";
            case "Zeitraum nicht verfügbar": return "Period unavailable";
            case "Nicht verfügbar · Nennkapazität manuell festlegen":
                return "Unavailable · set design capacity manually";
            case "Zu geringe Akkustandänderung (mindestens 5 % nötig)":
                return "Battery level change too small (at least 5% required)";
            case "Energiezähler/Strom nicht verfügbar":
                return "Energy counter/current unavailable";
            case "Automatische Schätzung erst ab 95 % Ladezustand":
                return "Automatic estimate requires a charge level of at least 95%";
            case "Ladestrom am Ladeende nicht verfügbar":
                return "Charging current unavailable at end of charge";
            case "Ladeende noch nicht stabil (über 25 mA)":
                return "Charge is not stable yet at the end (above 25 mA)";
            case "Wird in den nächsten Gesundheitsdurchschnitt einbezogen":
                return "Included in the next battery health average";
            default: return null;
        }
    }
}
