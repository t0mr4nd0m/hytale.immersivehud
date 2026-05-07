package com.tom.immersivehudplugin.ui;

public enum HudConfigView {
    PROFILES ("Profiles"),
    VISIBILITY("Visibility");

    private final String label;

    HudConfigView(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}