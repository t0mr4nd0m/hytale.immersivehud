package com.tom.immersivehudplugin.profiles;

public enum Profile {
    BALANCED("Balanced", """
            Balanced experience. [RECOMMENDED]
            HUD elements appear only when relevant during gameplay."""),
    IMMERSIVE("Immersive", """
            Minimal HUD, maximum immersion. [ADVANCED]
            Most elements stay hidden and only some of them appear only during key gameplay moments."""),
    VANILLA("Vanilla", """
            Original Hytale HUD experience.
            All HUD elements remain visible at all times."""),
    CUSTOM("Custom", """
            Your personalized HUD configuration.
            You’ve customized HUD visibility beyond predefined profiles.""");

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