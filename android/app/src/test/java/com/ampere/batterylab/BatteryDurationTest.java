package com.ampere.batterylab;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** Guards the duration formatter shared by dashboard and metric cards. */
public class BatteryDurationTest {

    @Test public void compactReturnsDashForNonPositive() {
        assertEquals("\u2014", BatteryDuration.compact(0));
        assertEquals("\u2014", BatteryDuration.compact(-15));
    }

    @Test public void compactShowsMinutesWhenUnderAnHour() {
        assertEquals("1 m", BatteryDuration.compact(1));
        assertEquals("59 m", BatteryDuration.compact(59));
    }

    @Test public void compactShowsWholeHourWhenNoRemainder() {
        assertEquals("1 h", BatteryDuration.compact(60));
        assertEquals("5 h", BatteryDuration.compact(300));
    }

    @Test public void compactShowsHoursAndMinutes() {
        assertEquals("1 h 30 m", BatteryDuration.compact(90));
        assertEquals("23 h 59 m", BatteryDuration.compact(23 * 60 + 59));
    }

    @Test public void dashboardReturnsDashForNonPositive() {
        assertEquals("\u2014", BatteryDuration.dashboard(0));
        assertEquals("\u2014", BatteryDuration.dashboard(-5));
    }

    @Test public void dashboardShowsMinutesWhenUnderAnHour() {
        assertEquals("1m", BatteryDuration.dashboard(1));
        assertEquals("59m", BatteryDuration.dashboard(59));
    }

    @Test public void dashboardShowsWholeHourWhenNoRemainder() {
        assertEquals("1h", BatteryDuration.dashboard(60));
        assertEquals("5h", BatteryDuration.dashboard(300));
    }

    @Test public void dashboardShowsHoursAndMinutes() {
        assertEquals("1h 30m", BatteryDuration.dashboard(90));
        assertEquals("23h 59m", BatteryDuration.dashboard(23 * 60 + 59));
    }
}
