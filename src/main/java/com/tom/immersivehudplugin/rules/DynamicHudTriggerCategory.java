package com.tom.immersivehudplugin.rules;

public enum DynamicHudTriggerCategory {
    COMBAT("COMBAT"),
    INTERACTION("INTERACTION"),
    MOVEMENT("MOVEMENT"),
    STATUS("STATUS"),
    SPECIAL("SPECIAL");

    private final String label;

    DynamicHudTriggerCategory(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}