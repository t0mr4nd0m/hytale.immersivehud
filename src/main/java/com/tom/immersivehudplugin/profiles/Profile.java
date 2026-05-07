package com.tom.immersivehudplugin.profiles;

public enum Profile {
    BALANCED (
            "immersivehud.gui.profile.balanced",
            "immersivehud.gui.profile.balanced.description"),
    IMMERSIVE (
            "immersivehud.gui.profile.immersive",
            "immersivehud.gui.profile.immersive.description"),
    VANILLA (
            "immersivehud.gui.profile.vanilla",
            "immersivehud.gui.profile.vanilla.description"),
    CUSTOM (
            "immersivehud.gui.profile.custom",
            "immersivehud.gui.profile.custom.description");

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
        if (s == null) {
            return null;
        }

        try {
            return Profile.valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}