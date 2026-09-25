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

    @Test public void monitorStatusShowsFreshnessWithoutClaimingProcessState() {
        assertEquals("Dienst zuletzt gerade eben bestätigt",
                BatteryBackgroundStatus.monitorHeartbeatStatus(90_000L, 149_999L));
        assertEquals("Dienst zuletzt vor 2 Min. bestätigt",
                BatteryBackgroundStatus.monitorHeartbeatStatus(90_000L, 210_000L));
        assertTrue(BatteryBackgroundStatus.monitorHeartbeatStatus(0L, 210_000L)
                .startsWith("Kein aktuelles Dienstsignal"));
        assertTrue(BatteryBackgroundStatus.monitorHeartbeatStatus(100_000L, 90_000L)
                .startsWith("Kein aktuelles Dienstsignal"));
        assertTrue(BatteryBackgroundStatus.monitorHeartbeatStatus(100_000L,
                100_000L + BatteryMonitorWatchdog.STALE_AFTER_MS)
                .startsWith("Kein aktuelles Dienstsignal"));
    }

    @Test public void backgroundExplanationSeparatesHiddenNotificationFromStoppedService() {
        String message = BatteryBackgroundStatus.explanation("Berechtigung fehlt",
                BatteryBackgroundStatus.batteryOptimizationStatus(false),
                BatteryBackgroundStatus.monitorHeartbeatStatus(90_000L, 210_000L));
        assertTrue(message.contains("Live-Benachrichtigung: Berechtigung fehlt"));
        assertTrue(message.contains("Hintergrunddienst: Dienst zuletzt vor 2 Min. bestätigt"));
        assertTrue(message.contains("öffne AkkuTakt einmal"));
        assertTrue(message.contains("Akkuoptimierung:"));
        assertTrue(message.contains("Dienst trotzdem laufen"));
        assertTrue(message.contains("Stopp erzwingen"));
        assertTrue(message.contains("nicht dauerhaft wach"));
    }
}
