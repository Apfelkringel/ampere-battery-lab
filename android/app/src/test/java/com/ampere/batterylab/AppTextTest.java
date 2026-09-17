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
        } finally {
            Locale.setDefault(previous);
        }
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
