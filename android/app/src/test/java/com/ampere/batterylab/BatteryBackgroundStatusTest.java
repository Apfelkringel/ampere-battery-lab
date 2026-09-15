package com.ampere.batterylab;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BatteryBackgroundStatusTest {
    @Test public void notificationStatusReportsEachSettingThatCanHideTheLiveCard() {
        assertEquals("Berechtigung fehlt", BatteryBackgroundStatus.notificationStatus(
                false, true, true));
        assertEquals("App-Benachrichtigungen sind deaktiviert",
                BatteryBackgroundStatus.notificationStatus(true, false, true));
        assertEquals("Kanal „Akkuüberwachung“ ist deaktiviert",
                BatteryBackgroundStatus.notificationStatus(true, true, false));
        assertEquals("aktiviert", BatteryBackgroundStatus.notificationStatus(
                true, true, true));
        assertEquals("uneingeschränkt",
                BatteryBackgroundStatus.batteryOptimizationStatus(true));
        assertEquals("vom System optimiert",
                BatteryBackgroundStatus.batteryOptimizationStatus(false));
    }

    @Test public void backgroundExplanationSeparatesHiddenNotificationFromStoppedService() {
        String message = BatteryBackgroundStatus.explanation("Berechtigung fehlt",
                BatteryBackgroundStatus.batteryOptimizationStatus(false));
        assertTrue(message.contains("Live-Benachrichtigung: Berechtigung fehlt"));
        assertTrue(message.contains("Akkuoptimierung:"));
        assertTrue(message.contains("Dienst trotzdem laufen"));
        assertTrue(message.contains("Stopp erzwingen"));
        assertTrue(message.contains("nicht dauerhaft wach"));
    }
}
