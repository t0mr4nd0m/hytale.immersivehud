package com.tom.immersivehudplugin.ui;

public enum HudConfigView {
    PROFILES ("Profiles", """
            Choose a profile to instantly apply a preset HUD experience.
            Profiles provide predefined configurations tailored for different playstyles."""),
    VISIBILITY("Visibility", """
            Lorem Ipsum""");

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