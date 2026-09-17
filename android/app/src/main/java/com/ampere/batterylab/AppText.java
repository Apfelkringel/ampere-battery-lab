package com.ampere.batterylab;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import java.util.Locale;

/** Small, dependency-free UI language layer used by the custom canvas and services. */
final class AppText {
    private AppText() {}

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

    static String t(Context context, String value) {
        return isEnglish(context) ? t(value) : value;
    }

    /**
     * Translates the UI vocabulary while preserving numbers, units, app names
     * and server-provided values. The method intentionally leaves unknown
     * strings untouched so diagnostics and user data are never corrupted.
     */
    static String t(String value) {
        if (value == null || value.isEmpty()
                || !"en".equalsIgnoreCase(Locale.getDefault().getLanguage())) return value;
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
                {"Normale Nutzung", "Normal use"}, {"Mehr Daten", "More data"},
                {"seit dem Abstecken", "since unplugging"},
                {"Seit dem Abstecken", "Since unplugging"},
                {"Seit dem Abstecken · geschätzte Werte", "Since unplugging · estimated values"},
                {"Ø Entladerate", "Average discharge rate"}, {"Entladerate", "Discharge rate"},
                {"Tiefschlaf", "Deep sleep"},
                {"Verbrauch seit dem Abstecken", "Usage since unplugging"},
                {"VERLAUF & STATISTIK", "HISTORY & STATISTICS"},
                {"Lokal aus Messpunkten berechnet · keine Cloud", "Calculated locally from measurements · no cloud"},
                {"DIESER MONAT · MONATSANFANG BIS HEUTE", "THIS MONTH · START OF MONTH TO TODAY"},
                {"Aufgeladen", "Charged"}, {"Batterieverbrauch", "Battery usage"},
                {"Geladen/Usage", "Charged/usage"},
                {"Woche", "Week"}, {"Monat", "Month"}, {"day", "Day"},
                {"Werte des aktiven Bereichs", "Values for the active section"},
                {"Gut lesbare Akkuwerte · live aktualisiert", "Readable battery values · updated live"},
                {"Berechtigungen & Zugriffe", "Permissions & access"},
                {"App-Akkuverbrauch", "App battery usage"},
                {"Akku-Bilanz", "Battery balance"},
                {"Sitzungsdetails", "Session details"},
                {"Letzte 24 Stunden", "Last 24 hours"},
                {"Letzte 7 Tage", "Last 7 days"},
                {"Letzte 30 Tage", "Last 30 days"},
                {"Täglich", "Daily"}, {"Wöchentlich", "Weekly"}, {"Monatlich", "Monthly"},
                {"Übersicht", "Overview"}, {"Start", "Home"}, {"Laden", "Charging"},
                {"Entladen", "Discharging"}, {"Akku", "Battery"}, {"Verlauf", "History"},
                {"Gesundheit", "Health"}, {"Einstellungen", "Settings"},
                {"Aktionen", "Actions"}, {"Ladeziel", "Charge target"},
                {"Akkustand", "Battery level"}, {"Akkustrom", "Battery current"},
                {"Akkuspannung", "Battery voltage"}, {"Akkuleistung", "Battery power"},
                {"Akkutemperatur", "Battery temperature"}, {"Temperatur", "Temperature"},
                {"Spannung", "Voltage"}, {"Verbrauch", "Usage"}, {"Akkuverbrauch", "Battery usage"},
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
                {"Lokale Daten löschen?", "Delete local data?"},
                {"Gesundheitsbasis zurücksetzen?", "Reset health baseline?"},
                {"Backup gespeichert.", "Backup saved."}, {"Backup wiederhergestellt.", "Backup restored."},
                {"CSV-Export gespeichert.", "CSV export saved."}, {"Diagnosebericht gespeichert.", "Diagnostic report saved."},
                {"Forschungs-Export gespeichert.", "Research export saved."},
                {"Live-Daten aktualisiert.", "Live data updated."},
                {"Akkuüberwachung", "Battery monitoring"}, {"Ladealarm", "Charge alert"},
                {"Ampere überwacht den Akku", "Ampere is monitoring your battery"},
                {"Akkubetrieb", "On battery"}, {"Akku entlädt", "Battery discharging"},
                {"Akkumesswerte werden auf diesem Gerät gespeichert", "Battery measurements are stored on this device"},
                {"Laden erkannt", "Charging detected"}, {"Bildschirm- und Hintergrundverbrauch lokal erfasst", "Screen and background usage recorded locally"},
                {"Lokale Akkuüberwachung · jede Sekunde", "Local battery monitoring · every second"},
                {"Prozent", "percent"}, {"Stunde", "hour"}, {"Stunden", "hours"},
                {"Minute", "minute"}, {"Minuten", "minutes"}, {"Tag", "day"}, {"Tage", "days"},
                {"Home measurement", "Start measurement"}
        };
        for (String[] phrase : phrases) result = result.replace(phrase[0], phrase[1]);
        return result;
    }
}
