package com.ampere.batterylab;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BatterySamplingPolicyTest {
    @Test public void unsupportedIntervalsUseTheSafeDefault() {
        assertEquals(15, BatterySamplingPolicy.normalizeMinutes(7));
        assertEquals(15L * 60L * 1000L, BatterySamplingPolicy.intervalMs(7));
    }

    @Test public void retentionCoversThirtyDaysForEverySupportedInterval() {
        assertEquals(8640, BatterySamplingPolicy.retentionSamples(5));
        assertEquals(720, BatterySamplingPolicy.retentionSamples(60));
    }

    @Test public void rollbackStartsANewSamplingBasis() {
        long interval = BatterySamplingPolicy.intervalMs(15);
        assertFalse(BatterySamplingPolicy.shouldSample(100_000L, 100_000L + interval - 1, interval));
        assertTrue(BatterySamplingPolicy.shouldSample(100_000L, 99_000L, interval));
    }
}
