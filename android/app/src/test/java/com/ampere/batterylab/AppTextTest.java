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
            assertEquals("Runtime with normal use: 8 Std.",
                    AppText.t("Restlaufzeit bei normaler Nutzung: 8 Std."));
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
