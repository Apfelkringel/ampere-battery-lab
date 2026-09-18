package com.ampere.batterylab;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Pure-JVM guards for the package-label fallback path. PackageManager is
 * avoided here so the test never has to fake a Context; the cache is
 * exercised through the parts of its public surface that do not need it.
 */
public class AppLabelCacheTest {
    @Before public void clearState() {
        AppLabelCache.resetForTest();
    }

    @Test public void emptyPackageReturnsUnknownApp() {
        // The method must not crash on null/empty input. The label is the
        // human-readable fallback so callers can use it as a row label.
        String label = AppLabelCache.labelFor(null, "");
        assertNotNull(label);
        assertTrue(label.length() > 0);
    }

    @Test public void unknownPackageFallsBackToLastSegment() {
        // Without a real PackageManager the helper must still produce the
        // package's last path segment, which is the documented fallback.
        String label = AppLabelCache.labelFor(null, "com.example.ampere.pro");
        assertEquals("pro", label);
    }

    @Test public void singleSegmentPackageIsReturnedAsIs() {
        String label = AppLabelCache.labelFor(null, "ampere");
        assertEquals("ampere", label);
    }

    @Test public void iconLookupWithoutContextReturnsNullSafely() {
        // When no Context is supplied, the icon helper cannot resolve a
        // launcher icon; it must return null rather than throwing.
        assertNull(AppLabelCache.iconFor(null, "com.example.ampere"));
    }

    @Test public void iconLookupWithEmptyPackageReturnsNullSafely() {
        assertNull(AppLabelCache.iconFor(null, ""));
        assertNull(AppLabelCache.iconFor(null, null));
    }

    @Test public void resetClearsBothCaches() {
        // Trigger a miss path so the cache holds at least one label entry.
        AppLabelCache.labelFor(null, "com.example.ampere.first");
        assertTrue(AppLabelCache.labelCountForTest() >= 1);
        AppLabelCache.resetForTest();
        assertEquals(0, AppLabelCache.labelCountForTest());
        assertEquals(0, AppLabelCache.iconCountForTest());
    }
}
