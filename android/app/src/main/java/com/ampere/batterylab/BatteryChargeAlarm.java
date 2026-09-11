package com.ampere.batterylab;

/** Edge-triggered charge-target alarm with a small reset band. */
final class BatteryChargeAlarm {
    static final int HYSTERESIS_PERCENT = 3;

    private BatteryChargeAlarm() { }

    static boolean shouldAlert(int level, boolean charging, int target,
                               boolean alreadySent, int previousLevel) {
        int normalizedTarget = BatteryChargeLimit.normalize(target);
        if (!charging || alreadySent || BatteryLevel.normalizePercent(level) < 0) return false;
        int previous = BatteryLevel.normalizePercent(previousLevel);
        // A first valid sample above the target is actionable; afterward only
        // an actual upward crossing can fire the notification.
        return previous < 0
                ? level >= normalizedTarget
                : previous < normalizedTarget && level >= normalizedTarget;
    }

    static boolean shouldReset(int level, boolean charging, int target) {
        int normalizedTarget = BatteryChargeLimit.normalize(target);
        if (!charging || BatteryLevel.normalizePercent(level) < 0) return true;
        return level <= normalizedTarget - HYSTERESIS_PERCENT;
    }
}
