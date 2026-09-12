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
