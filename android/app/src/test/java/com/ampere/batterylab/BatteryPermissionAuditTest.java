package com.ampere.batterylab;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BatteryPermissionAuditTest {
    @Test public void remindersWaitForOnboardingAndOnlyRepeatMonthly() {
        long now = 20L * BatteryPermissionAudit.REMINDER_INTERVAL_MS;
        assertFalse(BatteryPermissionAudit.shouldRemind(false, true, false, 0L, now));
        assertFalse(BatteryPermissionAudit.shouldRemind(true, false, false, 0L, now));
        assertTrue(BatteryPermissionAudit.shouldRemind(true, true, false, 0L, now));
        assertFalse(BatteryPermissionAudit.shouldRemind(true, true, false,
                now - BatteryPermissionAudit.REMINDER_INTERVAL_MS + 1L, now));
        assertTrue(BatteryPermissionAudit.shouldRemind(true, true, false,
                now - BatteryPermissionAudit.REMINDER_INTERVAL_MS, now));
    }

    @Test public void revokedAccessIsReportedImmediatelyAndClockRollbackDoesNotSuppressIt() {
        long now = 20L * BatteryPermissionAudit.REMINDER_INTERVAL_MS;
        assertTrue(BatteryPermissionAudit.shouldRemind(true, true, true, now, now));
        assertTrue(BatteryPermissionAudit.shouldRemind(true, true, false,
                now + 1000L, now));
    }

    @Test public void previouslyGrantedOptionalAccessIsTrackedEvenWithoutInAppRequest() {
        assertTrue(BatteryPermissionAudit.wasRevoked(true, false));
        assertFalse(BatteryPermissionAudit.wasRevoked(false, false));
        assertTrue(BatteryPermissionAudit.trackOptionalAccess(false, true));
        assertFalse(BatteryPermissionAudit.trackOptionalAccess(false, false));
    }
}
