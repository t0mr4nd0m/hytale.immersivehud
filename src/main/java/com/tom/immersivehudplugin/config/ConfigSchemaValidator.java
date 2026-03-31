package com.tom.immersivehudplugin.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public final class ConfigSchemaValidator {

    private ConfigSchemaValidator() {
    }

    /**
     * Global config:
     * - root must be an object
     * - known top-level fields, if present, must have a reasonable type
     * - nested sections, if present, must be objects
     * - extra keys are allowed
     * - missing keys are allowed and will be repaired later by mapper + sanitize
     */
    public static boolean isValidGlobalConfig(JsonElement root) {

        if (!isObject(root)) { return false; }

        JsonObject obj = root.getAsJsonObject();

        if (hasAndNotString(obj, "ConfigVersion")) return false;
        if (hasAndNotNumber(obj, "IntervalMs")) return false;
        if (hasAndNotNumber(obj, "HideDelayMs")) return false;
        if (hasAndNotNumber(obj, "ReticleTargetRange")) return false;
        if (hasAndNotObject(obj, "DefaultHudComponents")) return false;
        if (hasAndNotObject(obj, "DefaultDynamicHud")) return false;

        return true;
    }

    /**
     * Player config:
     * - root must be an object
     * - HudComponents / DynamicHud, if present, must be objects
     * - extra keys are allowed
     * - missing keys are allowed and will be repaired later by mapper + sanitize
     */
    public static boolean isValidPlayerConfig(JsonElement root) {

        if (!isObject(root)) { return false; }

        JsonObject obj = root.getAsJsonObject();

        if (hasAndNotObject(obj, "HudComponents")) return false;
        if (hasAndNotObject(obj, "DynamicHud")) return false;

        return true;
    }

    public static boolean isReadableHudComponentsConfig(JsonElement root) {
        return root == null || root.isJsonNull() || root.isJsonObject();
    }

    public static boolean isReadableDynamicHudConfig(JsonElement root) {
        return root == null || root.isJsonNull() || root.isJsonObject();
    }

    /**
     * A dynamic rule section is considered readable if:
     * - it is an object
     * - "Rules", if present, is either:
     *   - null
     *   - a string (old CSV style)
     *   - an array
     *
     * We intentionally do not reject unknown/invalid individual rule names here.
     * The mapper will ignore bad entries and keep loading the rest.
     */
    public static boolean isReadableDynamicHudRuleConfig(JsonElement root) {

        if (!isObject(root)) { return false; }

        JsonObject obj = root.getAsJsonObject();
        JsonElement rulesEl = obj.get("Rules");

        if (rulesEl == null || rulesEl.isJsonNull()) { return true; }

        return rulesEl.isJsonPrimitive() || rulesEl.isJsonArray();
    }

    private static boolean hasAndNotObject(JsonObject obj, String key) {

        JsonElement el = obj.get(key);
        return el != null && !el.isJsonNull() && !el.isJsonObject();
    }

    private static boolean hasAndNotString(JsonObject obj, String key) {

        JsonElement el = obj.get(key);
        return el != null && !el.isJsonNull()
                && !(el.isJsonPrimitive() && el.getAsJsonPrimitive().isString());
    }

    private static boolean hasAndNotNumber(JsonObject obj, String key) {

        JsonElement el = obj.get(key);
        return el != null && !el.isJsonNull()
                && !(el.isJsonPrimitive() && el.getAsJsonPrimitive().isNumber());
    }

    private static boolean isObject(JsonElement el) {
        return el != null && !el.isJsonNull() && el.isJsonObject();
    }
}