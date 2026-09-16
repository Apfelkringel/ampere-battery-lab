package com.ampere.batterylab;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BatteryPermissionChecklistTest {
    @Test public void checklistHasThreeActionableRowsWithAccurateStatuses() {
        String[] rows = BatteryPermissionChecklist.entries(false, false, true, false, false);

        assertEquals(3, rows.length);
        assertTrue(rows[0].contains("Benachrichtigungen\nNicht erteilt"));
        assertTrue(rows[1].contains("App-Nutzungszugriff\nNicht erteilt · optional"));
        assertTrue(rows[2].contains("Overlay\nOptional · nicht aktiviert"));
        assertTrue(BatteryPermissionChecklist.entries(true, true, true, true, true)[2]
                .contains("Overlay\nAktiv"));
    }
}
