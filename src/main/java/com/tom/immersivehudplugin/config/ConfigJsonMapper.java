package com.tom.immersivehudplugin.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tom.immersivehudplugin.hud.component.HudComponentRegistry;
import com.tom.immersivehudplugin.hud.component.HudComponent;
import com.tom.immersivehudplugin.hud.trigger.HudTrigger;

import java.util.EnumSet;

public final class ConfigJsonMapper {

    private ConfigJsonMapper() {}

    public static JsonObject toJson(GlobalConfig cfg) {

        JsonObject root = new JsonObject();

        root.addProperty("ConfigVersion", cfg.getConfigVersion());
        root.addProperty("IntervalMs", cfg.getIntervalMs());
        root.addProperty("HideDelayMs", cfg.getHideDelayMs());
        root.addProperty("ReticleTargetRange", cfg.getReticleTargetRange());
        root.add("DefaultHudComponents", toJson(cfg.getDefaultHudComponents()));
        root.add("DefaultDynamicHud", toJson(cfg.getDefaultDynamicHud()));

        return root;
    }

    public static GlobalConfig fromJsonGlobal(JsonObject root) {

        GlobalConfig cfg = new GlobalConfig();

        if (root == null) { return cfg; }

        if (isString(root, "ConfigVersion")) { cfg.setConfigVersion(root.get("ConfigVersion").getAsString()); }
        if (isNumber(root, "IntervalMs")) { cfg.setIntervalMs(root.get("IntervalMs").getAsInt()); }
        if (isNumber(root, "HideDelayMs")) { cfg.setHideDelayMs(root.get("HideDelayMs").getAsInt()); }
        if (isNumber(root, "ReticleTargetRange")) { cfg.setReticleTargetRange(root.get("ReticleTargetRange").getAsFloat()); }

        JsonObject hudObj = getObject(root, "DefaultHudComponents");
        if (hudObj != null) { cfg.setDefaultHudComponents(fromJsonHudComponents(hudObj)); }

        JsonObject dynObj = getObject(root, "DefaultDynamicHud");
        if (dynObj != null) { cfg.setDefaultDynamicHud(fromJsonDynamicHud(dynObj)); }

        return cfg;
    }

    public static JsonObject toJson(PlayerConfig cfg) {

        JsonObject root = new JsonObject();
        root.add("HudComponents", toJson(cfg.getHudComponents()));
        root.add("DynamicHud", toJson(cfg.getDynamicHud()));
        return root;
    }

    public static PlayerConfig fromJsonPlayer(JsonObject root) {

        PlayerConfig cfg = new PlayerConfig();

        if (root == null) { return cfg; }

        JsonObject hudObj = getObject(root, "HudComponents");
        if (hudObj != null) { cfg.setHudComponents(fromJsonHudComponents(hudObj)); }

        JsonObject dynObj = getObject(root, "DynamicHud");
        if (dynObj != null) { cfg.setDynamicHud(fromJsonDynamicHud(dynObj)); }

        return cfg;
    }

    public static JsonObject toJson(HudComponentsConfig cfg) {

        JsonObject obj = new JsonObject();

        for (HudComponent entry : HudComponentRegistry.allList()) {
            obj.addProperty(entry.staticConfigKey(), entry.staticGetter().get(cfg));
        }

        return obj;
    }

    public static HudComponentsConfig fromJsonHudComponents(JsonObject obj) {

        HudComponentsConfig cfg = new HudComponentsConfig();

        if (obj == null) { return cfg; }

        for (HudComponent entry : HudComponentRegistry.allList()) {
            JsonElement el = obj.get(entry.staticConfigKey());

            if (el != null
                    && !el.isJsonNull()
                    && el.isJsonPrimitive()
                    && el.getAsJsonPrimitive().isBoolean()) {
                entry.staticSetter().set(cfg, el.getAsBoolean());
            }
        }

        return cfg;
    }

    public static JsonObject toJson(DynamicHudConfig cfg) {

        JsonObject obj = new JsonObject();

        for (HudComponent entry : HudComponentRegistry.dynamicList()) {
            DynamicHudRuleConfig ruleCfg = entry.dynamicGetter() != null ? entry.dynamicGetter().apply(cfg) : null;
            if (entry.dynamicConfigKey() != null && ruleCfg != null) {
                obj.add(entry.dynamicConfigKey(), toJson(ruleCfg, entry));
            }
        }

        return obj;
    }

    public static DynamicHudConfig fromJsonDynamicHud(JsonObject obj) {

        DynamicHudConfig cfg = new DynamicHudConfig();

        if (obj == null) { return cfg; }

        for (HudComponent entry : HudComponentRegistry.dynamicList()) {
            if (entry.dynamicConfigKey() == null || entry.dynamicGetter() == null) { continue; }

            JsonElement sectionEl = obj.get(entry.dynamicConfigKey());
            if (sectionEl == null || sectionEl.isJsonNull() || !sectionEl.isJsonObject()) { continue; }

            DynamicHudRuleConfig loaded = fromJsonDynamicHudRuleConfig(sectionEl.getAsJsonObject());
            DynamicHudRuleConfig target = entry.dynamicGetter().apply(cfg);

            if (target != null) {
                target.setRules(loaded.getRules());
                target.setThreshold(loaded.getThreshold());
            }
        }

        return cfg;
    }

    public static JsonObject toJson(DynamicHudRuleConfig cfg, HudComponent entry) {

        JsonObject obj = new JsonObject();
        JsonArray arr = new JsonArray();

        for (String name : cfg.getRuleNames()) { arr.add(name); }

        obj.add("Rules", arr);

        if (entry.supportsThreshold()) {
            obj.addProperty("Threshold", cfg.getThreshold());
        }

        return obj;
    }

    public static DynamicHudRuleConfig fromJsonDynamicHudRuleConfig(JsonObject obj) {

        DynamicHudRuleConfig cfg = new DynamicHudRuleConfig();
        EnumSet<HudTrigger> rules = EnumSet.noneOf(HudTrigger.class);

        if (obj == null) { cfg.setRules(rules); return cfg; }

        JsonElement rulesEl = obj.get("Rules");
        JsonElement thresholdEl = obj.get("Threshold");

        if (rulesEl == null || rulesEl.isJsonNull()) { cfg.setRules(rules); return cfg; }

        if (rulesEl.isJsonPrimitive()) {

            String csv = rulesEl.getAsString();

            for (String ruleName : csv.split(",")) {
                HudTrigger t = HudTrigger.fromString(ruleName.trim());
                if (t != null) {
                    rules.add(t);
                }
            }

        } else if (rulesEl.isJsonArray()) {

            JsonArray arr = rulesEl.getAsJsonArray();

            for (JsonElement el : arr) {

                if (el == null || el.isJsonNull() || !el.isJsonPrimitive()) { continue; }

                HudTrigger trigger = HudTrigger.fromString(el.getAsString());
                if (trigger != null) {
                    rules.add(trigger);
                }
            }
        }

        cfg.setRules(rules);

        if (thresholdEl != null && thresholdEl.isJsonPrimitive()) {
            cfg.setThreshold(thresholdEl.getAsFloat());
        }

        return cfg;
    }

    private static boolean isString(JsonObject obj, @SuppressWarnings("SameParameterValue") String key) {

        JsonElement el = obj.get(key);
        return el != null
                && !el.isJsonNull()
                && el.isJsonPrimitive()
                && el.getAsJsonPrimitive().isString();
    }

    private static boolean isNumber(JsonObject obj, String key) {

        JsonElement el = obj.get(key);
        return el != null
                && !el.isJsonNull()
                && el.isJsonPrimitive()
                && el.getAsJsonPrimitive().isNumber();
    }

    private static JsonObject getObject(JsonObject obj, String key) {

        JsonElement el = obj.get(key);
        return (el != null && !el.isJsonNull() && el.isJsonObject())
                ? el.getAsJsonObject()
                : null;
    }
}