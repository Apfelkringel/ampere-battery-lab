package com.ampere.batterylab;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class BatteryFreshnessTest {
    @Test public void labelsRecentReadingsPrecisely() {
        assertEquals("gerade eben", BatteryFreshness.label(100_000L, 102_000L));
        assertEquals("vor 12 Sek.", BatteryFreshness.label(100_000L, 112_000L));
        assertEquals("vor 2 Min.", BatteryFreshness.label(100_000L, 220_000L));
    }

    @Test public void protectsAgainstMissingOrFutureTimestamps() {
        assertEquals("Messzeit unbekannt", BatteryFreshness.label(0L, 100_000L));
        assertEquals("Messzeit unbekannt", BatteryFreshness.label(110_000L, 100_000L));
    }
}
