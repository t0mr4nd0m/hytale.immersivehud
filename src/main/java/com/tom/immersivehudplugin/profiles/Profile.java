package com.tom.immersivehudplugin.profiles;

public enum Profile {
    DEFAULT("Default", """
            Balanced experience.
            Shows HUD elements only when they are relevant during gameplay."""),
    IMMERSIVE("Immersive", """
            Minimal HUD, maximum immersion.
            Most elements stay hidden and only appear in key gameplay moments."""),
    DISABLED("Disabled", """
            Full HUD visibility.
            All HUD elements are always visible (vanilla-like experience)."""),
    CUSTOM("Custom", """
            Your personalized HUD configuration.
            You’ve customized components or triggers outside the predefined profiles.""");

    private final String label;
    private final String description;

    Profile(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String label() {
        return label;
    }

    public String description() {
        return description;
    }

    public static Profile fromString(String s) {
        if (s == null) return null;

        try {
            return Profile.valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}