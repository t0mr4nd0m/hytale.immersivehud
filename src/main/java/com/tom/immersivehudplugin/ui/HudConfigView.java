package com.tom.immersivehudplugin.ui;

public enum HudConfigView {
    PROFILES ("Profiles", """
            1) Choose a profile to instantly apply a preset HUD experience.
            Profiles provide predefined configurations tailored for different playstyles."""),
    VISIBILITY ("Visibility", """
            2) Toggle components to show or hide them on your HUD.
            Note: The visibility of the Input Bindings HUD component may not work as expected due to limitations in
            the current Hytale implementation. If you want to hide it permanently, please use the in-game settings."""),
    DYNAMIC_RULES ("Dynamic Rules", """
            3) Select the triggers that control when a component becomes visible.
            If no triggers are selected, the component behaves as static.
            Combine multiple triggers to customize its behavior.""");

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