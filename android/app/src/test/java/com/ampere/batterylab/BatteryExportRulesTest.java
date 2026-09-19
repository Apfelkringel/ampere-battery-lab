package com.ampere.batterylab;

import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Validation guards for the legacy telemetry serializer used by CSV and JSON
 * exports, backups and history aggregation.
 */
public class BatteryExportRulesTest {

    private static String row(long timestamp, int level, String charging, int currentMa,
                             double temperature, double voltage, int chargeCounter, String usb,
                             int health, int systemCycles, int plugged) {
        return timestamp + "," + level + "," + charging + "," + currentMa + ","
                + temperature + "," + voltage + "," + chargeCounter + "," + usb + ","
                + health + "," + systemCycles + "," + plugged;
    }

    @Test public void rejectsShortRow() {
        String[] parts = row(1700000000000L, 50, "0", 100, 25.0, 3.8, 1500, "1", 90, 200, 2)
                .split(",", -1);
        // Drop the last field so the row is shorter than the minimum schema.
        String[] truncated = new String[parts.length - 1];
        System.arraycopy(parts, 0, truncated, 0, truncated.length);
        assertEquals(false, BatteryExportRules.isValidTelemetry(truncated));
    }

    @Test public void rejectsNegativeTimestamp() {
        String row = row(-1L, 50, "0", 100, 25.0, 3.8, 1500, "1", 90, 200, 2);
        assertEquals(false, BatteryExportRules.isValidTelemetry(row.split(",", -1)));
    }

    @Test public void rejectsOutOfRangeLevel() {
        String row = row(1700000000000L, 150, "0", 100, 25.0, 3.8, 1500, "1", 90, 200, 2);
        assertEquals(false, BatteryExportRules.isValidTelemetry(row.split(",", -1)));
    }

    @Test public void rejectsImplausibleCurrent() {
        String row = row(1700000000000L, 50, "0", 1000000, 25.0, 3.8, 1500, "1", 90, 200, 2);
        assertEquals(false, BatteryExportRules.isValidTelemetry(row.split(",", -1)));
    }

    @Test public void rejectsContradictoryCurrentSign() {
        // Charging row with negative current is treated as a measurement error
        // and must never make it into the exported file.
        String row = row(1700000000000L, 50, "1", -100, 25.0, 3.8, 1500, "1", 90, 200, 2);
        assertEquals(false, BatteryExportRules.isValidTelemetry(row.split(",", -1)));
    }

    @Test public void acceptsWellFormedDischargingRow() {
        String row = row(1700000000000L, 50, "0", -250, 25.0, 3.8, 1500, "0", 90, 200, 0);
        assertEquals(true, BatteryExportRules.isValidTelemetry(row.split(",", -1)));
    }

    @Test public void acceptsWellFormedChargingRow() {
        String row = row(1700000000000L, 80, "1", 1200, 26.5, 4.1, 1800, "1", 88, 215, 2);
        assertEquals(true, BatteryExportRules.isValidTelemetry(row.split(",", -1)));
    }

    @Test public void rejectsMalformedNumericField() {
        // Replace the level with a non-numeric value: parser must give up,
        // not crash and not silently accept the row.
        String broken = "1700000000000,not-a-number,0,100,25.0,3.8,1500,1,90,200,2";
        assertEquals(false, BatteryExportRules.isValidTelemetry(broken.split(",", -1)));
    }

    @Test public void validRowsAreReturnedInChronologicalOrder() {
        String first = row(1700000000000L, 50, "0", -250, 25.0, 3.8, 1500, "1", 90, 200, 2);
        String middle = row(1700000003000L, 49, "0", -200, 25.1, 3.79, 1490, "1", 90, 200, 2);
        String last = row(1700000006000L, 48, "0", -300, 25.2, 3.78, 1480, "1", 90, 200, 2);
        // Feed rows out of order and verify the returned list sorts them.
        ArrayList<String> rows = BatteryExportRules.validTelemetryRows(last + "\n" + first + "\n" + middle);
        assertEquals(3, rows.size());
        assertEquals(first, rows.get(0));
        assertEquals(middle, rows.get(1));
        assertEquals(last, rows.get(2));
    }

    @Test public void invalidRowsAreSkipped() {
        String valid = row(1700000000000L, 50, "0", -250, 25.0, 3.8, 1500, "1", 90, 200, 2);
        String broken = "not-a-row";
        ArrayList<String> rows = BatteryExportRules.validTelemetryRows(broken + "\n" + valid);
        assertEquals(1, rows.size());
        assertEquals(valid, rows.get(0));
    }

    @Test public void emptyOrNullSerializationsReturnEmpty() {
        assertEquals(0, BatteryExportRules.validTelemetryRows(null).size());
        assertEquals(0, BatteryExportRules.validTelemetryRows("").size());
    }

    @Test public void rowsBetweenHonoursInclusiveWindow() {
        String a = row(1700000000000L, 50, "0", -250, 25.0, 3.8, 1500, "1", 90, 200, 2);
        String b = row(1700000003000L, 49, "0", -200, 25.1, 3.79, 1490, "1", 90, 200, 2);
        String c = row(1700000006000L, 48, "0", -300, 25.2, 3.78, 1480, "1", 90, 200, 2);
        String serialized = a + "\n" + b + "\n" + c;
        // Inverted window returns nothing instead of throwing.
        assertEquals(0, BatteryExportRules.telemetryRowsBetween(
                serialized, 1700000006000L, 1700000000000L).size());
        ArrayList<String> rows = BatteryExportRules.telemetryRowsBetween(
                serialized, 1700000000000L, 1700000003000L);
        assertEquals(2, rows.size());
        assertEquals(a, rows.get(0));
        assertEquals(b, rows.get(1));
    }

    @Test public void normalizeTelemetryConcatenatesOnlyValidRows() {
        String valid = row(1700000000000L, 50, "0", -250, 25.0, 3.8, 1500, "1", 90, 200, 2);
        String normalized = BatteryExportRules.normalizeTelemetry("\n" + valid + "\n\nbroken row");
        assertEquals(valid, normalized);
    }

    @Test public void nonNegativeIntRejectsMalformedAndNegative() {
        assertEquals(Integer.valueOf(0), BatteryExportRules.nonNegativeInt("0"));
        assertEquals(Integer.valueOf(7), BatteryExportRules.nonNegativeInt("  7 "));
        assertNull(BatteryExportRules.nonNegativeInt("-3"));
        assertNull(BatteryExportRules.nonNegativeInt("not-a-number"));
        assertNull(BatteryExportRules.nonNegativeInt(null));
    }

    @Test public void normalizeTelemetryKeepsRowOrderAcrossNewlines() {
        String a = row(1700000000000L, 50, "0", -250, 25.0, 3.8, 1500, "1", 90, 200, 2);
        String b = row(1700000003000L, 49, "0", -200, 25.1, 3.79, 1490, "1", 90, 200, 2);
        // Out-of-order lines must still sort before being persisted.
        String normalized = BatteryExportRules.normalizeTelemetry(b + "\n" + a);
        assertEquals(a + "\n" + b, normalized);
        assertTrue("expected chronological output", normalized.indexOf(a) < normalized.indexOf(b));
    }
}
