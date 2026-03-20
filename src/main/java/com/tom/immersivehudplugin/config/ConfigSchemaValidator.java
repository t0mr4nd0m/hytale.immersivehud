package com.tom.immersivehudplugin.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tom.immersivehudplugin.registry.HudComponentRegistry;
import com.tom.immersivehudplugin.registry.HudComponentRegistry.HudEntry;
import com.tom.immersivehudplugin.rules.DynamicHudTriggers;

import java.util.HashSet;
import java.util.Set;

public final class ConfigSchemaValidator {

    private ConfigSchemaValidator() {
    }

    public static boolean isValidGlobalConfig(JsonElement root) {
        if (!isObject(root)) {
            return false;
        }

        JsonObject obj = root.getAsJsonObject();

        Set<String> expected = Set.of(
                "ConfigVersion",
                "IntervalMs",
                "HideDelayMs",
                "ReticleTargetRange",
                "DefaultHudComponents",
                "DefaultDynamicHud"
        );

        if (!hasExactlyKeys(obj, expected)) {
            return false;
        }

        return isString(obj.get("ConfigVersion"))
                && isNumber(obj.get("IntervalMs"))
                && isNumber(obj.get("HideDelayMs"))
                && isNumber(obj.get("ReticleTargetRange"))
                && isValidHudComponentsConfig(obj.get("DefaultHudComponents"))
                && isValidDynamicHudConfig(obj.get("DefaultDynamicHud"));
    }

    public static boolean isValidPlayerConfig(JsonElement root) {
        if (!isObject(root)) {
            return false;
        }

        JsonObject obj = root.getAsJsonObject();

        Set<String> expected = Set.of("HudComponents", "DynamicHud");
        if (!hasExactlyKeys(obj, expected)) {
            return false;
        }

        return isValidHudComponentsConfig(obj.get("HudComponents"))
                && isValidDynamicHudConfig(obj.get("DynamicHud"));
    }

    public static boolean isValidHudComponentsConfig(JsonElement root) {
        if (!isObject(root)) {
            return false;
        }

        JsonObject obj = root.getAsJsonObject();

        Set<String> expected = new HashSet<>();
        for (HudEntry entry : HudComponentRegistry.allList()) {
            expected.add(entry.staticConfigKey());
        }

        if (!hasExactlyKeys(obj, expected)) {
            return false;
        }

        for (HudEntry entry : HudComponentRegistry.allList()) {
            if (!isBoolean(obj.get(entry.staticConfigKey()))) {
                return false;
            }
        }

        return true;
    }

    public static boolean isValidDynamicHudConfig(JsonElement root) {
        if (!isObject(root)) {
            return false;
        }

        JsonObject obj = root.getAsJsonObject();

        Set<String> expected = new HashSet<>();
        for (HudEntry entry : HudComponentRegistry.dynamicList()) {
            expected.add(entry.dynamicConfigKey());
        }

        if (!hasExactlyKeys(obj, expected)) {
            return false;
        }

        for (HudEntry entry : HudComponentRegistry.dynamicList()) {
            if (!isValidDynamicHudRuleConfig(obj.get(entry.dynamicConfigKey()))) {
                return false;
            }
        }

        return true;
    }

    public static boolean isValidDynamicHudRuleConfig(JsonElement root) {
        if (!isObject(root)) {
            return false;
        }

        JsonObject obj = root.getAsJsonObject();

        if (!hasExactlyKeys(obj, Set.of("Rules"))) {
            return false;
        }

        JsonElement rulesEl = obj.get("Rules");
        if (rulesEl == null || rulesEl.isJsonNull()) {
            return false;
        }

        if (!rulesEl.isJsonArray()) {
            return false;
        }

        JsonArray arr = rulesEl.getAsJsonArray();
        for (JsonElement el : arr) {
            if (!isString(el)) {
                return false;
            }

            if (DynamicHudTriggers.fromString(el.getAsString()) == null) {
                return false;
            }
        }

        return true;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean hasExactlyKeys(JsonObject obj, Set<String> expected) {
        return obj.keySet().equals(expected);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean isObject(JsonElement el) {
        return el != null && !el.isJsonNull() && el.isJsonObject();
    }

    private static boolean isString(JsonElement el) {
        return el != null && el.isJsonPrimitive() && el.getAsJsonPrimitive().isString();
    }

    private static boolean isNumber(JsonElement el) {
        return el != null && el.isJsonPrimitive() && el.getAsJsonPrimitive().isNumber();
    }

    private static boolean isBoolean(JsonElement el) {
        return el != null && el.isJsonPrimitive() && el.getAsJsonPrimitive().isBoolean();
    }
}