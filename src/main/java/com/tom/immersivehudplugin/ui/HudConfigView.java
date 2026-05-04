package com.tom.immersivehudplugin.ui;

public enum HudConfigView {
    PROFILES ("Profiles", "Profiles help"),
    VISIBILITY("Visibility", "Visibility help");

    private final String label;
    private final String helpText;

    HudConfigView(String label, String helpText) {
        this.label = label;
        this.helpText = helpText;
    }

    public String label() {
        return label;
    }

    public String helpText() {
        return helpText;
    }
}