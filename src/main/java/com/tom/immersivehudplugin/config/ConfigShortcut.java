package com.tom.immersivehudplugin.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** Global shortcut expressed as game actions, independent of physical key bindings. */
public final class ConfigShortcut {
    private boolean enabled = true;
    private String modifier = "CROUCH";
    private String action = "OPEN_MAP";
    private boolean needsRepair;

    public boolean isEnabled() { return enabled; }
    public String getModifier() { return modifier; }
    public String getAction() { return action; }

    public boolean matches(boolean crouching) {
        return enabled && "CROUCH".equals(modifier) && crouching && isSupportedAction();
    }

    private boolean isSupportedAction() {
        return "OPEN_MAP".equals(action) || "OPEN_POCKET_CRAFTING".equals(action);
    }

    public boolean sanitize() {
        boolean changed = needsRepair;
        needsRepair = false;
        if (!"CROUCH".equals(modifier)) {
            modifier = "CROUCH";
            changed = true;
        }
        if (!isSupportedAction()) {
            action = "OPEN_MAP";
            changed = true;
        }
        return changed;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("enabled", enabled);
        json.addProperty("modifier", modifier);
        json.addProperty("action", action);
        return json;
    }

    public static ConfigShortcut fromJson(JsonElement element) {
        ConfigShortcut shortcut = new ConfigShortcut();
        if (element == null || !element.isJsonObject()) {
            shortcut.needsRepair = true;
            return shortcut;
        }
        JsonObject json = element.getAsJsonObject();
        JsonElement enabled = json.get("enabled");
        if (enabled != null && enabled.isJsonPrimitive() && enabled.getAsJsonPrimitive().isBoolean()) {
            shortcut.enabled = enabled.getAsBoolean();
        } else shortcut.needsRepair = true;
        shortcut.modifier = readString(json.get("modifier"));
        shortcut.action = readString(json.get("action"));
        return shortcut;
    }

    private static String readString(JsonElement element) {
        return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()
                ? element.getAsString() : null;
    }
}
