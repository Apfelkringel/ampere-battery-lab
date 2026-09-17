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
                {"Minute", "minute"}, {"Minuten", "minutes"}, {"Tag", "day"}, {"Tage", "days"}
        };
        for (String[] phrase : phrases) result = result.replace(phrase[0], phrase[1]);
        return result;
    }
}
