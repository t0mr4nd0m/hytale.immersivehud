package com.tom.immersivehudplugin.profiles;

public enum Profile {
    DEFAULT("Default"),
    IMMERSIVE("Immersive"),
    DISABLED("Disabled");

    private final String label;

    Profile(String label) {
        this.label = label;
    }

    public String label() {
        return label;
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