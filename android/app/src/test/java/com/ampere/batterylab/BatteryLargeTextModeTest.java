package com.ampere.batterylab;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BatteryLargeTextModeTest {
    @Test public void keepsTheDesignedCanvasAtNormalTextSizes() {
        assertFalse(BatteryLargeTextMode.shouldUseNativeLayout(1f));
        assertFalse(BatteryLargeTextMode.shouldUseNativeLayout(1.24f));
    }

    @Test public void usesReflowableNativeTextWhenAccessibilityTextIsEnlarged() {
        assertTrue(BatteryLargeTextMode.shouldUseNativeLayout(1.25f));
        assertTrue(BatteryLargeTextMode.shouldUseNativeLayout(2f));
    }

    @Test public void ignoresInvalidFontScaleValues() {
        assertFalse(BatteryLargeTextMode.shouldUseNativeLayout(Float.NaN));
        assertFalse(BatteryLargeTextMode.shouldUseNativeLayout(Float.POSITIVE_INFINITY));
    }
}
