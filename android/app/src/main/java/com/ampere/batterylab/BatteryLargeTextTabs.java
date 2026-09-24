package com.ampere.batterylab;

/** Shared labels and accessibility state for the reflowable large-text navigation. */
final class BatteryLargeTextTabs {
    private static final String[] NAMES = {"Übersicht", "Laden", "Entladen", "Akku", "Verlauf"};
    private static final String[] ACTIONS = {"Übersicht anzeigen", "Laden anzeigen",
            "Entladen anzeigen", "Akkugesundheit anzeigen", "Verlauf anzeigen"};

    private BatteryLargeTextTabs() { }

    static int count() { return NAMES.length; }

    static String name(int index) {
        return index >= 0 && index < NAMES.length ? NAMES[index] : "";
    }

    static String action(int index) {
        return index >= 0 && index < ACTIONS.length ? ACTIONS[index] : "";
    }

    static String contentDescription(int index, int selectedIndex) {
        String label = name(index);
        return index == selectedIndex && !label.isEmpty() ? "Ausgewählt: " + label : label;
    }

    static boolean shouldRevealSelection(int lastRevealedIndex, int selectedIndex) {
        return selectedIndex >= 0 && selectedIndex < NAMES.length
                && selectedIndex != lastRevealedIndex;
    }
}
