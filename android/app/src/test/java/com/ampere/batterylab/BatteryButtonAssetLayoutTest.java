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

    @Test public void appUsageBackButtonMeetsMinimumTouchWidth() throws IOException {
        String dashboard = Files.readString(findRepositoryRoot()
                .resolve("android/app/src/main/java/com/ampere/batterylab/MainActivity.java"),
                StandardCharsets.UTF_8);
        assertTrue("app-usage back control must have at least a 48-dp touch width",
                dashboard.contains("toolbar.addView(back, new LinearLayout.LayoutParams(dp(48), dp(54)))"));
    }

    @Test public void historyChartBucketsAreIndividualAccessibleControls() throws IOException {
        String dashboard = Files.readString(findRepositoryRoot()
                .resolve("android/app/src/main/java/com/ampere/batterylab/MainActivity.java"),
                StandardCharsets.UTF_8);
        assertTrue("chart buckets must appear as separate virtual accessibility children",
                dashboard.contains("host.addChild(BatteryDashboard.this, 100 + index)"));
        assertTrue("chart bucket nodes must announce their actual date and measurements",
                dashboard.contains("BatteryHistoryBucketAccessibility.description"));
        assertTrue("touch exploration must resolve chart bars to the matching bucket",
                dashboard.contains("x - contentInset(w), 36f, contentWidth(w) - 36f"));
    }

    @Test public void documentedOrientationMatchesThePortraitOnlyActivity() throws IOException {
        Path root = findRepositoryRoot();
        String manifest = Files.readString(root.resolve("android/app/src/main/AndroidManifest.xml"),
                StandardCharsets.UTF_8);
        String uxNotes = Files.readString(root.resolve("docs/UI-UX.md"), StandardCharsets.UTF_8);
        assertTrue("the app must remain locked to portrait", manifest.contains("android:screenOrientation=\"portrait\""));
        assertTrue("UX notes must accurately document portrait-only orientation",
                uxNotes.contains("activity is locked to portrait"));
        assertFalse("stale landscape-support guidance must be removed",
                uxNotes.contains("The activity is not forced into portrait"));
    }

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
        assertTrue("compact export artwork must be available through the shared mobile breakpoint",
                dashboard.contains("isCompactHistoryWidth(width) ? actionCsvCompactArtwork : actionCsvWideArtwork"));
        assertTrue("the mobile breakpoint must cover medium-width phones",
                dashboard.contains("return width < 480f;"));
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
                dashboard.contains("float healthTextRight = BatteryEditorialLayout.textRight(w)")
                        && dashboard.contains("boundedText(c, \"Noch keine Messung\", 36, healthTextRight"));
        assertTrue("health illustration must be omitted when its text lane would overlap",
                dashboard.contains("if (BatteryEditorialLayout.showSideIllustration(w))")
                        && dashboard.contains("drawEditorialBattery(c, w - 79, y + 129"));
    }

    @Test public void compactHealthCardUsesSymmetricHeroInsets() throws IOException {
        String dashboard = Files.readString(findRepositoryRoot()
                .resolve("android/app/src/main/java/com/ampere/batterylab/MainActivity.java"),
                StandardCharsets.UTF_8);
        assertTrue("compact health card must use a centered right edge",
                dashboard.contains("float infoRight = 18 + heroW - 18")
                        && dashboard.contains("rounded(c, 36, infoTop, infoRight, top + 408"));
        assertTrue("compact health card must use a semantic health icon",
                dashboard.contains("drawHeart(c, 66, infoTop + 24, lime, .62f)"));
        assertTrue("compact health content must use a clear single reading order",
                dashboard.contains("centeredText(c, health == 0 ? \"Noch nicht gemessen\"")
                        && dashboard.contains("Finde Kapazität und Verschleiß heraus.")
                        && dashboard.contains("Designkapazität")
                        && dashboard.contains("Gemessene Kapazität")
                        && dashboard.contains("Verschleiß")
                        && dashboard.contains("centeredText(c, health > 0 ? \"Messung aktualisieren\" : \"Messung starten\""));
        assertTrue("compact health card must keep its hero inside the mobile viewport",
                dashboard.contains("float heroH = compact ? 410f : 320f")
                        && dashboard.contains("float heroHeight = compact ? 410f : 320f"));
        assertTrue("compact health card must not mix in charging telemetry",
                !dashboard.contains("text(c, batteryRowLabel(), 52, infoTop")
                        && !dashboard.contains("compactDetection, 144, currentLeft"));
        assertTrue("compact measurement CTA must trigger the real benchmark action",
                dashboard.contains("return 23;")
                        && dashboard.contains("if (page == 0 && releasedRegion == 23)")
                        && dashboard.contains("toggleBenchmark();"));
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

    @Test public void widgetTextUsesTheSameVerticalBaselineAsItsSurface() throws IOException {
        Path root = findRepositoryRoot().resolve("android/app/src/main/res/layout");
        for (String name : new String[]{"battery_widget.xml", "battery_widget_compact.xml", "battery_widget_short.xml"}) {
            String layout = Files.readString(root.resolve(name), StandardCharsets.UTF_8);
            assertTrue(name + " must remove font-leading that pushes labels downward",
                    layout.contains("android:includeFontPadding=\"false\""));
        }
        String standard = Files.readString(root.resolve("battery_widget.xml"), StandardCharsets.UTF_8);
        String shortLayout = Files.readString(root.resolve("battery_widget_short.xml"), StandardCharsets.UTF_8);
        assertTrue("horizontal widget roots must not use baseline alignment",
                standard.contains("android:baselineAligned=\"false\"")
                        && shortLayout.contains("android:baselineAligned=\"false\""));
    }

    @Test public void metricIconsMatchTheirMeaning() throws IOException {
        String dashboard = Files.readString(findRepositoryRoot()
                .resolve("android/app/src/main/java/com/ampere/batterylab/MainActivity.java"),
                StandardCharsets.UTF_8);
        assertTrue("system cycle metrics must use a grid icon, not a health heart",
                dashboard.contains("\"Systemzyklen\", chargeCyclesDisplay(), \"von Android gemeldet\", \"grid\""));
        assertTrue("deep-sleep metrics must use a sleep icon",
                dashboard.contains("\"nach erster Sitzung\", \"moon\""));
        assertTrue("the sleep icon must be implemented in both metric layouts",
                dashboard.contains("else if (icon.equals(\"moon\")) drawMoon"));
    }

    @Test public void overviewHeaderUsesNaturalBatteryFocusedCopy() throws IOException {
        String dashboard = Files.readString(findRepositoryRoot()
                .resolve("android/app/src/main/java/com/ampere/batterylab/MainActivity.java"),
                StandardCharsets.UTF_8);
        assertTrue("overview title should invite users to monitor their battery",
                dashboard.contains("page == 0 ? \"Beobachte deinen Akku\" : pageName()"));
        assertTrue("the ungrammatical greeting should not return",
                !dashboard.contains("Hallo, dein Akku."));
    }

    @Test public void historyPeriodLabelsMatchCalendarBucketsAndEveryLegendMetricHasBars()
            throws IOException {
        String dashboard = Files.readString(findRepositoryRoot()
                .resolve("android/app/src/main/java/com/ampere/batterylab/MainActivity.java"),
                StandardCharsets.UTF_8);
        assertTrue(dashboard.contains("BatteryHistoryStats.rangeLabel(historyPeriodDays).toUpperCase(Locale.GERMANY)"));
        assertTrue("Heute · seit Mitternacht".equals(BatteryHistoryStats.rangeLabel(1)));
        assertTrue("Diese Woche · Montag bis heute".equals(BatteryHistoryStats.rangeLabel(7)));
        assertTrue("Dieser Monat · Monatsanfang bis heute".equals(BatteryHistoryStats.rangeLabel(30)));
        assertTrue(dashboard.contains("7 KALENDERTAGE"));
        assertTrue(dashboard.contains("5 KALENDERWOCHEN"));
        assertTrue(dashboard.contains("6 KALENDERMONATE"));
        assertTrue("the chart should plot efficiency rather than label a missing series",
                dashboard.contains("bucket.chargeConsumptionRatioPercent / (float) maxRatio"));
        assertTrue("dash values should be explained as unavailable, not zero",
                dashboard.contains("— = keine auswertbaren Messwerte"));
        assertTrue("the chart must show the numerical scale and invite inspection of exact values",
                dashboard.contains("Skalenmaximum in Legende · antippen für Werte"));
        assertTrue("chart details must disclose separate scales and distinguish missing data",
                dashboard.contains("Jede Kennzahl hat im Diagramm eine eigene Skala.")
                        && dashboard.contains("keine auswertbaren Strommessungen"));
    }

    @Test public void mobileCopyStaysConcreteAndLocalized() throws IOException {
        String dashboard = Files.readString(findRepositoryRoot()
                .resolve("android/app/src/main/java/com/ampere/batterylab/MainActivity.java"),
                StandardCharsets.UTF_8);
        assertTrue("charging status must explain the alarm without implying a forced stop",
                dashboard.contains("Alarm bei \" + chargeLimit + \" % · kein Ladestopp"));
        assertTrue("health measurement callout must name the concrete action",
                dashboard.contains("Kapazität messen"));
        assertTrue("technical analysis heading must remain in the app language",
                dashboard.contains("DETAILANALYSE") && !dashboard.contains("DEEP ANALYSIS"));
        assertTrue("cycle labels must be understandable without English jargon",
                dashboard.contains("Systemzyklen") && dashboard.contains("Vollzyklen (EFC)"));
    }

    @Test public void healthGradeThresholdsStayConsistentAcrossLayouts() throws IOException {
        String dashboard = Files.readString(findRepositoryRoot()
                .resolve("android/app/src/main/java/com/ampere/batterylab/MainActivity.java"),
                StandardCharsets.UTF_8);
        assertTrue("every rendered health status must use the shared grade thresholds",
                dashboard.contains("healthGradeLabel(health)"));
        assertFalse("health cards must not keep the old strict-greater-than-80 split",
                dashboard.contains("health > 80 ? \"Guter Zustand\""));
        assertFalse("the UI must use the full German label, not an English shorthand",
                dashboard.contains("\"Screenzeit\""));
    }

    @Test public void healthSourceCopyKeepsEmptyStateGrammatical() throws IOException {
        String dashboard = Files.readString(findRepositoryRoot()
                .resolve("android/app/src/main/java/com/ampere/batterylab/MainActivity.java"),
                StandardCharsets.UTF_8);
        assertTrue("empty health source must read as a complete statement",
                dashboard.contains("Keine Messung vorhanden"));
        assertFalse("empty health source must not use the ungrammatical dative fragment",
                dashboard.contains("keiner Messung"));
        assertTrue("measurement guidance must use typographic spacing before percent signs",
                dashboard.contains("Unter 25 % starten") && dashboard.contains("über 95 % laden"));
    }

    @Test public void liveOverlayBoundsLongAppNamesOnSmallDisplays() throws IOException {
        String overlay = Files.readString(findRepositoryRoot()
                .resolve("android/app/src/main/java/com/ampere/batterylab/BatteryOverlayService.java"),
                StandardCharsets.UTF_8);
        String dashboard = Files.readString(findRepositoryRoot()
                .resolve("android/app/src/main/java/com/ampere/batterylab/MainActivity.java"),
                StandardCharsets.UTF_8);
        assertTrue("overlay text must be bounded to the current display width",
                overlay.contains("setMaxWidth(Math.round(getResources().getDisplayMetrics().widthPixels * 0.78f))"));
        assertTrue("overlay must cap lines and ellipsize long app labels",
                overlay.contains("setMaxLines(4)") && overlay.contains("TextUtils.TruncateAt.END"));
        assertTrue("overlay labels must describe foreground usage in German",
                overlay.contains("Vordergrund-App") && overlay.contains("Prozesslast"));
        assertTrue("the live overlay must not cover Ampere while its screen is visible",
                overlay.contains("BatteryOverlayVisibility.shouldShow(enabled, activityVisible)")
                        && dashboard.contains("putBoolean(MAIN_ACTIVITY_VISIBLE, true)")
                        && dashboard.contains("putBoolean(MAIN_ACTIVITY_VISIBLE, false)"));
        assertTrue("the floating surface must use Ampere's petrol and turquoise palette",
                overlay.contains("background.setColor(Color.rgb(4, 52, 56))")
                        && overlay.contains("Color.rgb(20, 114, 111)"));
        assertFalse("the live overlay must not reintroduce the off-palette lime accent",
                overlay.contains("Color.rgb(199, 243, 107)"));
    }

    @Test public void systemSurfaceCopyUsesGermanProductTerms() throws IOException {
        String dashboard = Files.readString(findRepositoryRoot()
                .resolve("android/app/src/main/java/com/ampere/batterylab/MainActivity.java"),
                StandardCharsets.UTF_8);
        String overlay = Files.readString(findRepositoryRoot()
                .resolve("android/app/src/main/java/com/ampere/batterylab/BatteryOverlayService.java"),
                StandardCharsets.UTF_8);
        assertTrue("research export feedback must use the German product term",
                dashboard.contains("Forschungs-Export gespeichert."));
        assertFalse("the visible export feedback must not retain the English label",
                dashboard.contains("Research-Export gespeichert."));
        assertTrue("overlay CPU label must describe the aggregate percentage",
                overlay.contains("CPU gesamt"));
    }

    @Test public void healthWideEstimationCardContainsOptionalManufactureDate() throws IOException {
        String dashboard = Files.readString(findRepositoryRoot()
                .resolve("android/app/src/main/java/com/ampere/batterylab/MainActivity.java"),
                StandardCharsets.UTF_8);
        assertTrue("health estimation card must extend below the optional manufacture date",
                dashboard.contains("rounded(c, 18, y + 438, w - 18, y + 540")
                        && dashboard.contains("rect.set(u(18), u(y + 438), u(w - 18), u(y + 540))")
                        && dashboard.contains("rightText(c, \"Herstellung: \" + manufactureDate.label(), w - 30, y + 530"));
    }

    @Test public void mobileHealthSourceUsesShortBoundedLabel() throws IOException {
        String dashboard = Files.readString(findRepositoryRoot()
                .resolve("android/app/src/main/java/com/ampere/batterylab/MainActivity.java"),
                StandardCharsets.UTF_8);
        assertTrue("mobile health card must not render raw OEM source paths",
                dashboard.contains("healthReading.source.isEmpty() ? \"Keine Messung\" : healthMeasurementSourceLabel()")
                        && dashboard.contains("boundedText(c, healthReading.source.isEmpty() ? \"Keine Messung\" : healthMeasurementSourceLabel(),"));
    }

    @Test public void ultraNarrowHeaderAvoidsBadgeMenuCollision() throws IOException {
        String dashboard = Files.readString(findRepositoryRoot()
                .resolve("android/app/src/main/java/com/ampere/batterylab/MainActivity.java"),
                StandardCharsets.UTF_8);
        assertTrue("very narrow headers must hide the secondary badge before the menu cell",
                dashboard.contains("if (w >= 280f)")
                        && dashboard.contains("if (w < 390f)")
                        && dashboard.contains("w - 60, controlTop + 6"));
    }

    @Test public void ultraNarrowNavigationAvoidsStretchedBakedLabels() throws IOException {
        String dashboard = Files.readString(findRepositoryRoot()
                .resolve("android/app/src/main/java/com/ampere/batterylab/MainActivity.java"),
                StandardCharsets.UTF_8);
        assertTrue("very narrow navigation must shorten labels and skip stretched image buttons",
                dashboard.contains("boolean ultraCompactNav = w < 280f")
                        && dashboard.contains("new String[]{\"Start\", \"Laden\", \"Entl.\", \"Akku\", \"Verl.\"}")
                        && dashboard.contains("compactNav && !ultraCompactNav && (active || pressed)"));
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
