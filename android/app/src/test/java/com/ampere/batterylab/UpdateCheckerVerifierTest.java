package com.ampere.batterylab;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Locks the certificate pin set that {@link UpdateChecker} accepts when
 * verifying a downloaded APK. Two pins are accepted on purpose:
 *
 *  * {@code EXPECTED_RELEASE_CERT_SHA256} is the active Ampere Battery Lab
 *    release signer, read from {@code getApkContentsSigners()} on API >= 28
 *    or from the V3 lineage on rotation-capable devices.
 *
 *  * {@code EXPECTED_LINEAGE_DEBUG_CERT_SHA256} is the historical debug
 *    key that bootstraps the V3 lineage. Older releases and devices reading
 *    the V1 signer through {@code info.signatures} only see this key, so
 *    dropping it from the allowed set would silently break the in-app
 *    updater on every install that has not yet migrated to V3.
 *
 * If either pin ever drifts, every user update starts failing with
 * "Hash, Version oder Release-Signatur ungültig.". These assertions
 * make that drift loud instead of silent.
 */
public class UpdateCheckerVerifierTest {

    @Test public void releasePinMatchesAmpereBatteryLab() {
        assertEquals(
                "fa29b87595ef1b34b2069d1e2842d2114b1552a074e022ee527a7ec17e981ad3",
                UpdateChecker.EXPECTED_RELEASE_CERT_SHA256);
    }

    @Test public void lineageDebugPinMatchesDebugKey() {
        assertEquals(
                "eabc1c630a28daf5fff5f67c70d4382d16784da89019321bb107da41abb60eba",
                UpdateChecker.EXPECTED_LINEAGE_DEBUG_CERT_SHA256);
    }

    @Test public void pinsAreDistinct() {
        // A single-line copy/paste mistake could collapse the two pins.
        // Reject that before it ships.
        assertNotEquals(
                UpdateChecker.EXPECTED_RELEASE_CERT_SHA256,
                UpdateChecker.EXPECTED_LINEAGE_DEBUG_CERT_SHA256);
    }

    @Test public void pinsAreLowercaseHex64() {
        assertTrue(UpdateChecker.EXPECTED_RELEASE_CERT_SHA256.matches("[0-9a-f]{64}"));
        assertTrue(UpdateChecker.EXPECTED_LINEAGE_DEBUG_CERT_SHA256.matches("[0-9a-f]{64}"));
    }
}
