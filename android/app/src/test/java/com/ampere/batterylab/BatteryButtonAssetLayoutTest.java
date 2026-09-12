package com.ampere.batterylab;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;

public class BatteryButtonAssetLayoutTest {
    private static final String[] MOBILE_NAV_ASSETS = {
            "01_laden_mobile.svg",
            "02_entladen_mobile.svg",
            "03_akku_mobile.svg",
            "04_verlauf_mobile.svg",
            "05_start_mobile.svg"
    };
    private static final int[] ICON_ELEMENT_COUNTS = {1, 1, 1, 2, 2};

    @Test public void mobileNavigationKeepsIconAndLabelInSeparateVerticalSlots()
            throws IOException {
        Path assetDirectory = findRepositoryRoot()
                .resolve("design/batteryhub-buttons-mobile");

        for (int index = 0; index < MOBILE_NAV_ASSETS.length; index++) {
            String fileName = MOBILE_NAV_ASSETS[index];
            String svg = Files.readString(assetDirectory.resolve(fileName),
                    StandardCharsets.UTF_8);
            String message = fileName + " must move only its icon, never icon and label together";

            assertFalse(message, svg.contains("<g transform=\"translate(0,-16)\">"));
            assertFalse(message, svg.contains("<g class=\"icon-slot\""));
            assertFalse(fileName + " must remain compatible with the native SVG exporter",
                    svg.contains("data-slot="));
            String transform = "transform=\"translate(0,-16)\"";
            assertTrue(message, countOccurrences(svg, transform) == ICON_ELEMENT_COUNTS[index]);
            int labelStart = svg.indexOf("<text");
            assertTrue(message, svg.lastIndexOf(transform) < labelStart);
            assertTrue(fileName + " must keep the label on the balanced 175 baseline",
                    svg.substring(labelStart).contains("y=\"175\""));
            assertFalse(fileName + " must not transform its label",
                    svg.substring(labelStart).contains("transform=\"translate(0,-16)\""));
        }
    }

    @Test public void narrowHistoryUsesDedicatedVerticalSessionRows() throws IOException {
        String dashboard = Files.readString(findRepositoryRoot()
                .resolve("android/app/src/main/java/com/ampere/batterylab/MainActivity.java"),
                StandardCharsets.UTF_8);
        assertTrue("320-dp history must not pack change and duration into one row",
                dashboard.contains("drawCompactHistorySessionRow"));
        assertTrue("history export needs a dedicated panel-bottom calculation",
                dashboard.contains("historyPanelBottom"));
    }

    @Test public void narrowHistoryUsesCompactExportArtwork() throws IOException {
        String dashboard = Files.readString(findRepositoryRoot()
                .resolve("android/app/src/main/java/com/ampere/batterylab/MainActivity.java"),
                StandardCharsets.UTF_8);
        assertTrue("narrow history export must select the legible compact image button",
                dashboard.contains("historyExportArtwork(w)"));
        assertTrue("compact export artwork must be available for narrow phones",
                dashboard.contains("width < 390f ? actionCsvCompactArtwork : actionCsvWideArtwork"));
    }

    @Test public void historyDeepSleepSummaryIsRightBounded() throws IOException {
        String dashboard = Files.readString(findRepositoryRoot()
                .resolve("android/app/src/main/java/com/ampere/batterylab/MainActivity.java"),
                StandardCharsets.UTF_8);
        assertTrue("deep-sleep minutes must stay inside the history card",
                dashboard.contains("rightText(c, deepSleepTime(), w - 36"));
    }

    @Test public void historyDiagnosticsUseReadableWrappedBlock() throws IOException {
        String dashboard = Files.readString(findRepositoryRoot()
                .resolve("android/app/src/main/java/com/ampere/batterylab/MainActivity.java"),
                StandardCharsets.UTF_8);
        assertTrue("diagnostics must use a dedicated wrapped block on narrow history cards",
                dashboard.contains("drawHistoryDiagnostic"));
        assertTrue("the old squeezed one-line diagnostic must be gone",
                !dashboard.contains("boundedText(c, \"Diagnose: \" + telemetryDiagnosticDisplay"));
    }

    @Test public void wideHistoryDurationIsRightBounded() throws IOException {
        String dashboard = Files.readString(findRepositoryRoot()
                .resolve("android/app/src/main/java/com/ampere/batterylab/MainActivity.java"),
                StandardCharsets.UTF_8);
        assertTrue("history durations must end inside the card padding",
                dashboard.contains("rightText(c, parts[2], w - 36"));
        assertTrue("history must not place durations from an unbounded left edge",
                !dashboard.contains("text(c, parts[2], w - 75"));
    }

    @Test public void themeChoicesAndToggleAreRemoved() throws IOException {
        String dashboard = Files.readString(findRepositoryRoot()
                .resolve("android/app/src/main/java/com/ampere/batterylab/MainActivity.java"),
                StandardCharsets.UTF_8);
        assertTrue("settings must not offer theme choices",
                !dashboard.contains("Dunkles Design")
                        && !dashboard.contains("AMOLED-Schwarz"));
        assertTrue("dashboard must not keep a theme toggle state",
                !dashboard.contains("amoled") && !dashboard.contains("BatteryHeaderLayout.THEME"));
        assertTrue("dashboard must keep the fixed petrol background",
                dashboard.contains("int bg = Color.rgb(4, 52, 56)"));
    }

    @Test public void emptyHistoryCalloutDoesNotCoverTimeAxis() throws IOException {
        String dashboard = Files.readString(findRepositoryRoot()
                .resolve("android/app/src/main/java/com/ampere/batterylab/MainActivity.java"),
                StandardCharsets.UTF_8);
        assertTrue("empty history callout must sit below the chart time axis",
                dashboard.contains("rounded(c, 36, y + 253, w - 36, y + 280"));
        assertTrue("empty history callout label must follow its moved surface",
                dashboard.contains("y + 271, 8, cream, true"));
        assertTrue("empty history placeholder must sit between chart grid lines",
                dashboard.contains("chartLeft, y + 214, 8.5f, cream, true"));
        assertTrue("empty history placeholder must not sit on the 50% grid line",
                !dashboard.contains("chartLeft, y + 201, 8.5f, cream, true"));
    }

    @Test public void emptyHistoryAndHealthCopyAvoidIllustrationLane() throws IOException {
        String dashboard = Files.readString(findRepositoryRoot()
                .resolve("android/app/src/main/java/com/ampere/batterylab/MainActivity.java"),
                StandardCharsets.UTF_8);
        assertTrue("empty history privacy copy must reserve the illustration lane",
                dashboard.contains("Nur lokal gespeichert.")
                        && dashboard.contains("auf diesem Gerät."));
        assertTrue("health measurement basis must wrap before the battery illustration",
                dashboard.contains("Eine volle Ladung")
                        && dashboard.contains("schafft die Messbasis"));
    }

    @Test public void healthHeroReservesIllustrationLaneAndBalancesBattery() throws IOException {
        String dashboard = Files.readString(findRepositoryRoot()
                .resolve("android/app/src/main/java/com/ampere/batterylab/MainActivity.java"),
                StandardCharsets.UTF_8);
        assertTrue("health copy must stop before the battery illustration lane",
                dashboard.contains("float healthTextRight = w - 130f")
                        && dashboard.contains("boundedText(c, \"Noch keine Messung\", 36, healthTextRight"));
        assertTrue("health illustration must use the vertically balanced position",
                dashboard.contains("drawEditorialBattery(c, w - 79, y + 129"));
    }

    @Test public void compactHealthCardUsesSymmetricHeroInsets() throws IOException {
        String dashboard = Files.readString(findRepositoryRoot()
                .resolve("android/app/src/main/java/com/ampere/batterylab/MainActivity.java"),
                StandardCharsets.UTF_8);
        assertTrue("compact health card must use a centered right edge",
                dashboard.contains("float infoRight = 18 + heroW - 18")
                        && dashboard.contains("rounded(c, 36, infoTop, infoRight, top + 374"));
        assertTrue("compact health status values must follow the centered card",
                dashboard.contains("drawBolt(c, infoRight - 37")
                        && dashboard.contains("float statusRight = infoRight - 32f"));
        assertTrue("compact health headline and detail must share a centered text axis",
                dashboard.contains("float infoTextRight = infoRight - 52f")
                        && dashboard.contains("centeredDisplayBoundedText(c, health == 0 ? \"Noch nicht gemessen\""));
    }

    @Test public void dischargeEmptyStateUsesCompactReadableCopy() throws IOException {
        String dashboard = Files.readString(findRepositoryRoot()
                .resolve("android/app/src/main/java/com/ampere/batterylab/MainActivity.java"),
                StandardCharsets.UTF_8);
        assertTrue("discharge helper copy must fit the illustration lane",
                dashboard.contains("Sitzung startet beim Abstecken"));
        assertTrue("the old clipped discharge sentence must be gone",
                !dashboard.contains("Beim Abstecken startet die Sitzung automatisch"));
    }

    @Test public void updateChannelAvoidsGithubApiRateLimit() throws IOException {
        String build = Files.readString(findRepositoryRoot()
                .resolve("android/app/build.gradle"), StandardCharsets.UTF_8);
        String checker = Files.readString(findRepositoryRoot()
                .resolve("android/app/src/main/java/com/ampere/batterylab/UpdateChecker.java"),
                StandardCharsets.UTF_8);
        assertTrue("manifest must use the public raw CDN instead of the rate-limited API",
                build.contains("https://raw.githubusercontent.com/Apfelkringel/ampere-battery-lab-updates/main/latest.json"));
        assertTrue("checker must allow only the pinned raw manifest path",
                checker.contains("EXPECTED_MANIFEST_RAW_PATH")
                        && checker.contains("raw.githubusercontent.com"));
    }

    @Test public void cardsDoNotUseDecorativeLeftRails() throws IOException {
        String dashboard = Files.readString(findRepositoryRoot()
                .resolve("android/app/src/main/java/com/ampere/batterylab/MainActivity.java"),
                StandardCharsets.UTF_8);
        assertTrue("card frames must not draw the generic AI-style left rail",
                !dashboard.contains("c.drawRect(u(l), u(t), u(Math.min(r, l + 6))")
                        && !dashboard.contains("u(l + 4), u(b - 18)")
                        && !dashboard.contains("rounded(c, l + 9f, t + 10f"));
    }

    private static int countOccurrences(String value, String needle) {
        int count = 0;
        int offset = 0;
        while ((offset = value.indexOf(needle, offset)) >= 0) {
            count++;
            offset += needle.length();
        }
        return count;
    }

    private static Path findRepositoryRoot() {
        Path cursor = Paths.get("").toAbsolutePath();
        while (cursor != null) {
            if (Files.isDirectory(cursor.resolve("design/batteryhub-buttons-mobile"))) {
                return cursor;
            }
            cursor = cursor.getParent();
        }
        throw new IllegalStateException("Could not locate repository root");
    }
}
