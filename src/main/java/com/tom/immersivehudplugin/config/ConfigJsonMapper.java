package com.tom.immersivehudplugin.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tom.immersivehudplugin.registry.HudComponentRegistry;
import com.tom.immersivehudplugin.registry.HudComponentRegistry.HudEntry;
import com.tom.immersivehudplugin.rules.DynamicHudTriggers;

import java.util.EnumSet;

public final class ConfigJsonMapper {

    private ConfigJsonMapper() {
    }

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

        cfg.setConfigVersion(root.get("ConfigVersion").getAsString());
        cfg.setIntervalMs(root.get("IntervalMs").getAsInt());
        cfg.setHideDelayMs(root.get("HideDelayMs").getAsInt());
        cfg.setReticleTargetRange(root.get("ReticleTargetRange").getAsFloat());
        cfg.setDefaultHudComponents(fromJsonHudComponents(root.getAsJsonObject("DefaultHudComponents")));
        cfg.setDefaultDynamicHud(fromJsonDynamicHud(root.getAsJsonObject("DefaultDynamicHud")));

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
        cfg.setHudComponents(fromJsonHudComponents(root.getAsJsonObject("HudComponents")));
        cfg.setDynamicHud(fromJsonDynamicHud(root.getAsJsonObject("DynamicHud")));
        return cfg;
    }

    public static JsonObject toJson(HudComponentsConfig cfg) {
        JsonObject obj = new JsonObject();

        for (HudEntry entry : HudComponentRegistry.allList()) {
            obj.addProperty(entry.staticConfigKey(), entry.staticGetter().get(cfg));
        }

        return obj;
    }

    public static HudComponentsConfig fromJsonHudComponents(JsonObject obj) {
        HudComponentsConfig cfg = HudComponentRegistry.buildDefaultHudComponents();

        for (HudEntry entry : HudComponentRegistry.allList()) {
            boolean value = obj.get(entry.staticConfigKey()).getAsBoolean();
            entry.staticSetter().set(cfg, value);
        }

        return cfg;
    }

    @SuppressWarnings("DataFlowIssue")
    public static JsonObject toJson(DynamicHudConfig cfg) {
        JsonObject obj = new JsonObject();

        for (HudEntry entry : HudComponentRegistry.dynamicList()) {
            DynamicHudRuleConfig ruleCfg = entry.dynamicGetter().apply(cfg);
            obj.add(entry.dynamicConfigKey(), toJson(ruleCfg));
        }

        return obj;
    }

    public static DynamicHudConfig fromJsonDynamicHud(JsonObject obj) {
        DynamicHudConfig cfg = HudComponentRegistry.buildDefaultDynamicHud();

        for (HudEntry entry : HudComponentRegistry.dynamicList()) {
            JsonElement el = obj.get(entry.dynamicConfigKey());
            JsonObject rulesObj = el != null && el.isJsonObject() ? el.getAsJsonObject() : new JsonObject();
            DynamicHudRuleConfig loaded = fromJsonDynamicHudRuleConfig(rulesObj);

            DynamicHudRuleConfig target = entry.dynamicGetter() != null ? entry.dynamicGetter().apply(cfg) : null;
            if (target != null) {
                target.setRules(loaded.getRules());
            }
        }

        return cfg;
    }

    public static JsonObject toJson(DynamicHudRuleConfig cfg) {
        JsonObject obj = new JsonObject();
        JsonArray arr = new JsonArray();

        for (String name : cfg.getRuleNames()) {
            arr.add(name);
        }

        obj.add("Rules", arr);
        return obj;
    }

    public static DynamicHudRuleConfig fromJsonDynamicHudRuleConfig(JsonObject obj) {

        DynamicHudRuleConfig cfg = new DynamicHudRuleConfig();
        EnumSet<DynamicHudTriggers> rules = EnumSet.noneOf(DynamicHudTriggers.class);

        JsonElement rulesEl = obj.get("Rules");

        if (rulesEl == null || rulesEl.isJsonNull()) {
            cfg.setRules(rules);
            return cfg;
        }

        JsonArray arr = rulesEl.getAsJsonArray();

        for (JsonElement el : arr) {
            if (!el.isJsonPrimitive()) {
                continue; // ignore bad entries instead of crashing
            }

            DynamicHudTriggers trigger = DynamicHudTriggers.fromString(el.getAsString());
            if (trigger != null) {
                rules.add(trigger);
            }
        }

        cfg.setRules(rules);
        return cfg;
    }
}