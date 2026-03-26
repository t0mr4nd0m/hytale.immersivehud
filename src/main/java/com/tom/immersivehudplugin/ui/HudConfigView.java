package com.tom.immersivehudplugin.ui;

public enum HudConfigView {
    PROFILES ("Profiles", """
            Choose a profile to instantly apply a preset HUD experience.
            Profiles provide predefined configurations tailored for different playstyles."""),
    VISIBILITY ("Visibility", """
            Toggle components to show or hide them on your HUD.
            Note: Input binding HUD component visibility is a bit buggy. Use game Settings to hide it."""),
    DYNAMIC_RULES ("Dynamic Rules", """
            Choose a component, then select the triggers that control when it becomes visible.
            If no rules are selected, the component behaves as static.
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