package com.tom.immersivehudplugin.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public final class ConfigSchemaValidator {

    private ConfigSchemaValidator() {}

    @SuppressWarnings("RedundantIfStatement")
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

    @SuppressWarnings("RedundantIfStatement")
    public static boolean isValidPlayerConfig(JsonElement root) {

        if (!isObject(root)) { return false; }

        JsonObject obj = root.getAsJsonObject();

        if (hasAndNotObject(obj, "HudComponents")) return false;
        if (hasAndNotObject(obj, "DynamicHud")) return false;

        return true;
    }

    private static boolean hasAndNotObject(JsonObject obj, String key) {

        JsonElement el = obj.get(key);
        return el != null && !el.isJsonNull() && !el.isJsonObject();
    }

    @SuppressWarnings("SameParameterValue")
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

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean isObject(JsonElement el) {
        return el != null && !el.isJsonNull() && el.isJsonObject();
    }
}