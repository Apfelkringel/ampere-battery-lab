package com.ampere.batterylab;

/** Scheduling policy for permission reminders; permission state itself comes from Android. */
final class BatteryPermissionAudit {
    static final long REMINDER_INTERVAL_MS = 7L * 24L * 60L * 60L * 1000L;

    private BatteryPermissionAudit() { }

    static boolean shouldRemind(boolean onboardingDone, boolean hasMissingAccess,
                                boolean accessWasRevoked, long lastReminderAt, long now) {
        if (!onboardingDone || !hasMissingAccess) return false;
        if (accessWasRevoked) return true;
        if (lastReminderAt <= 0L || now < lastReminderAt) return true;
        return now - lastReminderAt >= REMINDER_INTERVAL_MS;
    }
}
