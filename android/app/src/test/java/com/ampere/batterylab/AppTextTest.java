package com.ampere.batterylab;

import java.util.Locale;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Guards the English UI against generic replacement-order regressions. */
public class AppTextTest {
    @Test public void englishTranslationsKeepSpecificWordsIntact() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.ENGLISH);
            assertEquals("Screen time", AppText.t("Bildschirmzeit"));
            assertEquals("Battery level · 7 days",
                    AppText.t("Akkustand · 7 Tage"));
            assertEquals("100% of 80%", AppText.t("100% von 80%"));
            assertEquals("Charge/usage ratio", AppText.t("Charged/Usage"));
            assertEquals("Battery level at start", AppText.t("Akkustand beim Start"));
            assertEquals("Rate unavailable", AppText.t("Rate n/v"));
            assertEquals("Since unplugging · estimated values",
                    AppText.t("Seit dem Abstecken · geschätzte Werte"));
            assertEquals("Runtime with normal use: 8 Std.",
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
            assertEquals("Charge status: Not available. Charge target: Reached. Charged energy: 7612 mAh. Session duration: 8 hr 29 min.",
                    AppText.t("Ladestatus: Nicht verfügbar. Ladeziel: Erreicht. Geladene Energie: 7612 mAh. Sitzungsdauer: 8 Std. 29 Min."));
            assertEquals("Monthly history. Selected period: 6 CALENDAR MONTHS. Charge/usage ratio (charged divided by used): 1424 percent",
                    AppText.t("Verlauf monatlich. Ausgewählter Zeitraum: 6 KALENDERMONATE. Lade-/Verbrauchsquote (geladen geteilt durch verbraucht): 1424 Prozent"));
            assertEquals("BATTERY BALANCE · 6 CALENDAR MONTHS",
                    AppText.t("AKKU-BILANZ · 6 KALENDERMONATE"));
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
