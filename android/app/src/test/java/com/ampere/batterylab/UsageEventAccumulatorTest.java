package com.ampere.batterylab;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Guards the foreground/background event aggregator that drives app attribution. */
public class UsageEventAccumulatorTest {

    @Test public void foregroundThenBackgroundClosesSingleSession() {
        Map<String, Long> totals = new HashMap<>();
        Map<String, UsageEventAccumulator.State> active = new HashMap<>();
        long start = 1_000L;
        long end = 10_000L;
        // Foreground at t=1000, background at t=4000 -> session length = 3000 ms.
        UsageEventAccumulator.apply(totals, active, "com.example.app",
                "MainActivity", 1_000L, start, end, true, false);
        UsageEventAccumulator.apply(totals, active, "com.example.app",
                "MainActivity", 4_000L, start, end, false, true);
        assertEquals(3_000L, totals.get("com.example.app").longValue());
        assertTrue("active state must be cleared after background",
                active.isEmpty());
    }

    @Test public void foregroundEarlierThanWindowStartClampsToWindowStart() {
        Map<String, Long> totals = new HashMap<>();
        Map<String, UsageEventAccumulator.State> active = new HashMap<>();
        long start = 1_000L;
        long end = 10_000L;
        // Foreground at t=500 (before window) clamps sessionStart to 1000.
        UsageEventAccumulator.apply(totals, active, "com.example.app",
                "MainActivity", 500L, start, end, true, false);
        UsageEventAccumulator.apply(totals, active, "com.example.app",
                "MainActivity", 4_000L, start, end, false, true);
        // Session length is 4000 - 1000 = 3000.
        assertEquals(3_000L, totals.get("com.example.app").longValue());
    }

    @Test public void backgroundInsideWindowContributesFullInterval() {
        Map<String, Long> totals = new HashMap<>();
        Map<String, UsageEventAccumulator.State> active = new HashMap<>();
        long start = 1_000L;
        long end = 10_000L;
        // Foreground at t=1000, background at t=8000 -> 7000 ms in foreground.
        UsageEventAccumulator.apply(totals, active, "com.example.app",
                "MainActivity", 1_000L, start, end, true, false);
        UsageEventAccumulator.apply(totals, active, "com.example.app",
                "MainActivity", 8_000L, start, end, false, true);
        assertEquals("package must record 7000 ms of foreground time",
                7_000L, totals.get("com.example.app").longValue());
    }

    @Test public void emptyClassNameClosesEntirePackage() {
        Map<String, Long> totals = new HashMap<>();
        Map<String, UsageEventAccumulator.State> active = new HashMap<>();
        long start = 1_000L;
        long end = 10_000L;
        // Open two activities for the same package.
        UsageEventAccumulator.apply(totals, active, "com.example.app",
                "ActivityA", 1_000L, start, end, true, false);
        UsageEventAccumulator.apply(totals, active, "com.example.app",
                "ActivityB", 2_000L, start, end, true, false);
        // MOVE_TO_BACKGROUND (no class) must still close the package.
        UsageEventAccumulator.apply(totals, active, "com.example.app",
                null, 5_000L, start, end, false, true);
        assertEquals("package must record 4000 ms of foreground time",
                4_000L, totals.get("com.example.app").longValue());
        assertTrue(active.isEmpty());
    }

    @Test public void closingOneActivityLeavesOtherOpen() {
        Map<String, Long> totals = new HashMap<>();
        Map<String, UsageEventAccumulator.State> active = new HashMap<>();
        long start = 1_000L;
        long end = 10_000L;
        UsageEventAccumulator.apply(totals, active, "com.example.app",
                "ActivityA", 1_000L, start, end, true, false);
        UsageEventAccumulator.apply(totals, active, "com.example.app",
                "ActivityB", 2_000L, start, end, true, false);
        // Pause only ActivityB at t=5: package still has ActivityA so it stays
        // open and contributes nothing yet.
        UsageEventAccumulator.apply(totals, active, "com.example.app",
                "ActivityB", 5_000L, start, end, false, true);
        assertEquals(null, totals.get("com.example.app"));
        assertTrue(active.containsKey("com.example.app"));
        // Pause ActivityA at t=7: package now closes.
        UsageEventAccumulator.apply(totals, active, "com.example.app",
                "ActivityA", 7_000L, start, end, false, true);
        assertEquals("package must record 6000 ms of foreground time",
                6_000L, totals.get("com.example.app").longValue());
        assertTrue(active.isEmpty());
    }

    @Test public void duplicateBackgroundEventsAreHarmless() {
        Map<String, Long> totals = new HashMap<>();
        Map<String, UsageEventAccumulator.State> active = new HashMap<>();
        long start = 1_000L;
        long end = 10_000L;
        UsageEventAccumulator.apply(totals, active, "com.example.app",
                "MainActivity", 1_000L, start, end, true, false);
        UsageEventAccumulator.apply(totals, active, "com.example.app",
                "MainActivity", 4_000L, start, end, false, true);
        // Second pause for the same class arrives after the close.
        UsageEventAccumulator.apply(totals, active, "com.example.app",
                "MainActivity", 6_000L, start, end, false, true);
        assertEquals(3_000L, totals.get("com.example.app").longValue());
        assertTrue(active.isEmpty());
    }

    @Test public void closeAllFlushesActiveSessionsToEnd() {
        Map<String, Long> totals = new HashMap<>();
        Map<String, UsageEventAccumulator.State> active = new HashMap<>();
        long start = 1_000L;
        long end = 10_000L;
        UsageEventAccumulator.apply(totals, active, "com.example.alpha",
                "MainActivity", 1_000L, start, end, true, false);
        UsageEventAccumulator.apply(totals, active, "com.example.beta",
                "MainActivity", 2_000L, start, end, true, false);
        UsageEventAccumulator.closeAll(totals, active, end);
        assertEquals("alpha must record 9000 ms of foreground time",
                9_000L, totals.get("com.example.alpha").longValue());
        assertEquals("beta must record 8000 ms of foreground time",
                8_000L, totals.get("com.example.beta").longValue());
        assertTrue(active.isEmpty());
    }

    @Test public void foregroundAfterWindowIsIgnored() {
        Map<String, Long> totals = new HashMap<>();
        Map<String, UsageEventAccumulator.State> active = new HashMap<>();
        long start = 1_000L;
        long end = 10_000L;
        // Foreground past the window end must not register a session.
        UsageEventAccumulator.apply(totals, active, "com.example.app",
                "MainActivity", 50_000L, start, end, true, false);
        assertTrue(active.isEmpty());
        assertEquals(null, totals.get("com.example.app"));
    }

    @Test public void backgroundWithoutOpenSessionIsIgnored() {
        Map<String, Long> totals = new HashMap<>();
        Map<String, UsageEventAccumulator.State> active = new HashMap<>();
        long start = 1_000L;
        long end = 10_000L;
        UsageEventAccumulator.apply(totals, active, "com.example.app",
                "MainActivity", 5_000L, start, end, false, true);
        assertTrue(active.isEmpty());
        assertEquals(null, totals.get("com.example.app"));
    }

    @Test public void secondPackageCoexistsIndependently() {
        Map<String, Long> totals = new HashMap<>();
        Map<String, UsageEventAccumulator.State> active = new HashMap<>();
        long start = 1_000L;
        long end = 10_000L;
        UsageEventAccumulator.apply(totals, active, "com.example.alpha",
                "MainActivity", 1_000L, start, end, true, false);
        UsageEventAccumulator.apply(totals, active, "com.example.beta",
                "MainActivity", 3_000L, start, end, true, false);
        // Close alpha at t=6, beta stays open until t=8.
        UsageEventAccumulator.apply(totals, active, "com.example.alpha",
                "MainActivity", 6_000L, start, end, false, true);
        UsageEventAccumulator.apply(totals, active, "com.example.beta",
                "MainActivity", 8_000L, start, end, false, true);
        assertEquals("alpha must record 5000 ms of foreground time",
                5_000L, totals.get("com.example.alpha").longValue());
        assertEquals("beta must record 5000 ms of foreground time",
                5_000L, totals.get("com.example.beta").longValue());
    }
}
