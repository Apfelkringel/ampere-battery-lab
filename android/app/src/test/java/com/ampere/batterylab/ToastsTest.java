package com.ampere.batterylab;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** Coalesce-window guard for the dedup helper used for status toasts. */
public class ToastsTest {
    @Before public void clearState() {
        Toasts.resetForTest();
    }

    @Test public void firstCallAlwaysPasses() {
        assertEquals("Backup gespeichert.",
                Toasts.resolveForTest("Backup gespeichert.", 0L));
    }

    @Test public void identicalTextInsideWindowIsSuppressed() {
        Toasts.resolveForTest("Backup gespeichert.", 0L);
        assertEquals(Toasts.SUPPRESSED,
                Toasts.resolveForTest("Backup gespeichert.", 500L));
    }

    @Test public void differentTextAlwaysPasses() {
        Toasts.resolveForTest("Backup gespeichert.", 0L);
        assertEquals("CSV-Export gespeichert.",
                Toasts.resolveForTest("CSV-Export gespeichert.", 500L));
    }

    @Test public void identicalTextAfterWindowPassesAgain() {
        Toasts.resolveForTest("Backup gespeichert.", 0L);
        // Three seconds is past the two-second coalesce window.
        assertEquals("Backup gespeichert.",
                Toasts.resolveForTest("Backup gespeichert.", 3_000L));
    }

    @Test public void emptyAndNullDoNothing() {
        assertEquals(null, Toasts.resolveForTest(null, 0L));
        assertEquals("", Toasts.resolveForTest("", 0L));
    }
}
