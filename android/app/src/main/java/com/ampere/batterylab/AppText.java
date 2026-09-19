package com.ampere.batterylab;

import android.content.Context;
import android.content.res.Configuration;
import android.app.LocaleManager;
import android.os.Build;
import android.os.LocaleList;
import java.util.Locale;

/** Small, dependency-free UI language layer used by the custom canvas and services. */
final class AppText {
    private static final String LANGUAGE_KEY = "appLanguage";

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
        // These chart titles contain words that are also translated as standalone
        // labels. Keep the complete title atomic so later vocabulary rules cannot
        // change the capitalization of the already translated result.
        if ("Akkustand · 7 Tage".equals(value)) return "Battery level · 7 days";
        if ("Akkustand · 30 Tage".equals(value)) return "Battery level · 30 days";
        if ("Akkustand beim Start".equals(value)) return "Battery level at start";
        if ("Starte die Kapazitätsmessung getrennt vom Ladegerät unter 25 %.".equals(value)) {
            return "Start the capacity measurement unplugged and below 25%.";
        }
        if ("Entladung starten".equals(value)) return "Start a discharge session";
        if ("Übersicht anzeigen".equals(value)) return "Show overview";
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
        String result = value;
        String[][] phrases = {
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
                {"VERBLEIBENDE NUTZUNGSZEIT", "REMAINING RUNTIME"},
                {"KAPAZITÄTSSCHÄTZUNG", "CAPACITY ESTIMATE"},
                {"LADEGESCHWINDIGKEIT", "CHARGING SPEED"},
                {"ENTLADEVORGANG", "DISCHARGE SESSION"},
                {"An ", "On "}, {" · aus ", " · off "},
                {"Bildschirm-Aufweckungen", "screen wake-ups"},
                {"TÄGLICHE VOLLZYKLEN", "DAILY FULL CYCLES"},
                {"LADEVERSCHLEISS", "CHARGING WEAR"},
                {"AUSWERTUNG", "ANALYSIS"}, {"LETZTE SITZUNGEN", "RECENT SESSIONS"},
                {"VERLAUF", "HISTORY"}, {"DIAGNOSE", "DIAGNOSTICS"},
                {"Sitzungen:", "Sessions:"}, {"Ladungsmenge:", "Charge amount:"},
                {"VERLAUF & STATISTIK", "HISTORY & STATISTICS"},
                {"Lokal aus Messpunkten berechnet · keine Cloud", "Calculated locally from measurements · no cloud"},
                {"DIESER MONAT · MONATSANFANG BIS HEUTE", "THIS MONTH · START OF MONTH TO TODAY"},
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
                {"Täglich", "Daily"}, {"Wöchentlich", "Weekly"}, {"Monatlich", "Monthly"},
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
                {"Eine volle Ladung", "One full charge"}, {"schafft die Messbasis", "provides the baseline"},
                {"geeignete Sitzungen", "valid sessions"},
                {"Kapazitätsmessung weiter unten starten", "Start capacity measurement below"},
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
                {"Warte auf Android-Akkuwert", "Waiting for Android battery value"},
                {"Nicht verfügbar", "Not available"}, {"nicht verfügbar", "not available"},
                {"Keine Daten", "No data"}, {"Noch keine Daten", "No data yet"},
                {"Noch keine Verbrauchsdaten für die Quote", "No usage data for the ratio yet"},
                {"Wird gemessen", "Measuring"}, {"Noch offen", "Pending"}, {"gerade eben", "just now"},
                {"BERECHNET", "CALCULATED"}, {"GESCHÄTZT", "ESTIMATED"}, {"SYSTEM", "SYSTEM"},
                {"LIVE", "LIVE"}, {"AKKUVERBRAUCH", "BATTERY USAGE"}, {"Lade-/Verbrauchsquote", "charge/usage ratio"},
                {"lokale Verlaufseinträge zu erstellen.", "to create local history entries."},
                {"Akkumesswerte bleiben auf diesem Gerät.", "Battery measurements stay on this device."},
                {"Weitere Sitzungen im Verlauf", "More sessions in history"},
                {"Verbrauch den verwendeten Apps zu.", "Usage attributed to the apps used."},
                {"Wärme prüfen", "Check temperature"}, {"Unbekannt", "Unknown"},
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
                {"Berechtigungen & Zugriffe", "Permissions & access"}, {"Fertig", "Done"},
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
                {"Kapazität messen", "Measure capacity"},
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
                {"Tippen, um die kostenlose Aktualisierung zu prüfen", "Tap to verify the free update"},
                {"Download ist auf diesem Gerät nicht verfügbar.", "Downloads are not available on this device."},
                {"Update konnte nicht sicher vorbereitet werden.", "The update could not be prepared safely."},
                {"Update wird heruntergeladen …", "Downloading update …"},
                {"Suche nach Aktualisierungen …", "Checking for updates …"},
                {"Ein anderer Update-Vorgang läuft bereits.", "Another update operation is already running."},
                {"Update-Prüfung", "Update check"},
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
                {"Laden erkannt", "Charging detected"}, {"Bildschirm- und Hintergrundverbrauch lokal erfasst", "Screen and background usage recorded locally"},
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
                {"Geladen/Usage", "Charged/usage"},
                {"Laderate ", "Charge rate "}, {"Strom nicht verfügbar", "Current unavailable"},
                {"Temperatur nicht verfügbar", "Temperature unavailable"},
                {"Akkumesswerte werden auf diesem Gerät gespeichert", "Battery measurements are stored on this device"},
                {"Restenergie", "Remaining energy"}, {"Android-Zustand", "Android status"},
                {"Kapazitätsniveau", "Capacity level"}, {"Ladeprofil", "Charging profile"},
                {"Ladealgorithmus", "Charging algorithm"}, {"Ladeverhalten", "Charging behavior"},
                {"Thermik", "Thermal status"}, {"Gesundheit", "Health"}, {"Schätzung", "Estimate"},
                {"Lokale Akkuüberwachung", "Local battery monitoring"}, {"Laden erkannt", "Charging detected"},
                {"Bildschirm- und Hintergrundverbrauch lokal erfasst", "Screen and background usage recorded locally"},
                {"Akkuüberwachung", "Battery monitoring"}, {"Ladealarm", "Charge alert"},
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
                {"LIVE-ENERGIEFLUSS", "LIVE ENERGY FLOW"},
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
        // Apply complete phrases before short vocabulary fragments. Otherwise
        // replacing "Akku", "Tage" or "Schätzung" first can corrupt a full
        // sentence and leave mixed-language output behind.
        for (int i = 0; i < phrases.length - 1; i++) {
            for (int j = i + 1; j < phrases.length; j++) {
                if (phrases[j][0].length() > phrases[i][0].length()) {
                    String[] swap = phrases[i];
                    phrases[i] = phrases[j];
                    phrases[j] = swap;
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
}
