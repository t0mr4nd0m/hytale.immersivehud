package com.tom.immersivehudplugin.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tom.immersivehudplugin.hud.component.HudComponent;
import com.tom.immersivehudplugin.hud.component.HudComponentRegistry;
import com.tom.immersivehudplugin.hud.trigger.HudTrigger;

import java.util.EnumSet;

public final class ConfigJsonMapper {

    private ConfigJsonMapper() {}

    private static final String CONFIG_VERSION = "ConfigVersion";
    private static final String INTERVAL_MS = "IntervalMs";
    private static final String HIDE_DELAY_MS = "HideDelayMs";
    private static final String RETICLE_TARGET_RANGE = "ReticleTargetRange";
    private static final String DEFAULT_HUD_COMPONENTS = "DefaultHudComponents";
    private static final String DEFAULT_DYNAMIC_HUD = "DefaultDynamicHud";
    private static final String HUD_COMPONENTS = "HudComponents";
    private static final String DYNAMIC_HUD = "DynamicHud";
    private static final String RULES = "Rules";
    private static final String THRESHOLD = "Threshold";
    private static final String HIDE_PREFIX = "Hide";

    public static JsonObject toJson(GlobalConfig cfg) {
        JsonObject root = new JsonObject();

        root.addProperty(CONFIG_VERSION, cfg.getConfigVersion());
        root.addProperty(INTERVAL_MS, cfg.getIntervalMs());
        root.addProperty(HIDE_DELAY_MS, cfg.getHideDelayMs());
        root.addProperty(RETICLE_TARGET_RANGE, cfg.getReticleTargetRange());
        root.add(DEFAULT_HUD_COMPONENTS, toJson(cfg.getDefaultHudComponents()));
        root.add(DEFAULT_DYNAMIC_HUD, toJson(cfg.getDefaultDynamicHud()));

        return root;
    }

    public static GlobalConfig fromJsonGlobal(JsonObject root) {
        GlobalConfig cfg = new GlobalConfig();

        if (root == null) {
            return cfg;
        }

        if (isString(root, CONFIG_VERSION)) {
            cfg.setConfigVersion(root.get(CONFIG_VERSION).getAsString());
        }
        if (isNumber(root, INTERVAL_MS)) {
            cfg.setIntervalMs(root.get(INTERVAL_MS).getAsInt());
        }
        if (isNumber(root, HIDE_DELAY_MS)) {
            cfg.setHideDelayMs(root.get(HIDE_DELAY_MS).getAsInt());
        }
        if (isNumber(root, RETICLE_TARGET_RANGE)) {
            cfg.setReticleTargetRange(root.get(RETICLE_TARGET_RANGE).getAsFloat());
        }

        JsonObject hudObj = getObject(root, DEFAULT_HUD_COMPONENTS);
        if (hudObj != null) {
            cfg.setDefaultHudComponents(fromJsonHudComponents(hudObj));
        }

        JsonObject dynObj = getObject(root, DEFAULT_DYNAMIC_HUD);
        if (dynObj != null) {
            cfg.setDefaultDynamicHud(fromJsonDynamicHud(dynObj));
        }

        return cfg;
    }

    public static JsonObject toJson(PlayerConfig cfg) {
        JsonObject root = new JsonObject();
        root.add(HUD_COMPONENTS, toJson(cfg.getHudComponents()));
        root.add(DYNAMIC_HUD, toJson(cfg.getDynamicHud()));
        return root;
    }

    public static PlayerConfig fromJsonPlayer(JsonObject root) {
        PlayerConfig cfg = new PlayerConfig();

        if (root == null) {
            return cfg;
        }

        JsonObject hudObj = getObject(root, HUD_COMPONENTS);
        if (hudObj != null) {
            cfg.setHudComponents(fromJsonHudComponents(hudObj));
        }

        JsonObject dynObj = getObject(root, DYNAMIC_HUD);
        if (dynObj != null) {
            cfg.setDynamicHud(fromJsonDynamicHud(dynObj));
        }

        return cfg;
    }

    public static JsonObject toJson(HudComponentsConfig cfg) {
        JsonObject obj = new JsonObject();

        for (HudComponent entry : HudComponentRegistry.allList()) {
            obj.addProperty(staticConfigKey(entry.key()), entry.staticGetter().get(cfg));
        }

        return obj;
    }

    public static HudComponentsConfig fromJsonHudComponents(JsonObject obj) {
        HudComponentsConfig cfg = new HudComponentsConfig();

        if (obj == null) {
            return cfg;
        }

        for (HudComponent entry : HudComponentRegistry.allList()) {
            JsonElement el = obj.get(staticConfigKey(entry.key()));

            if (isBoolean(el)) {
                entry.staticSetter().set(cfg, el.getAsBoolean());
            }
        }

        return cfg;
    }

    public static JsonObject toJson(DynamicHudConfig cfg) {
        JsonObject obj = new JsonObject();

        for (HudComponent entry : HudComponentRegistry.dynamicList()) {
            DynamicHudRuleConfig ruleCfg = entry.getDynamicRuleConfig(cfg);
            obj.add(dynamicConfigKey(entry.key()), toJson(ruleCfg, entry));
        }

        return obj;
    }

    public static DynamicHudConfig fromJsonDynamicHud(JsonObject obj) {
        DynamicHudConfig cfg = new DynamicHudConfig();

        if (obj == null) {
            return cfg;
        }

        for (HudComponent entry : HudComponentRegistry.dynamicList()) {
            JsonElement sectionEl = obj.get(dynamicConfigKey(entry.key()));
            if (sectionEl == null || sectionEl.isJsonNull() || !sectionEl.isJsonObject()) {
                continue;
            }

            DynamicHudRuleConfig loaded = fromJsonDynamicHudRuleConfig(sectionEl.getAsJsonObject());
            DynamicHudRuleConfig target = entry.getDynamicRuleConfig(cfg);

            target.setRules(loaded.getRules());
            target.setThreshold(loaded.getThreshold());
        }

        return cfg;
    }

    public static JsonObject toJson(DynamicHudRuleConfig cfg, HudComponent entry) {
        JsonObject obj = new JsonObject();
        JsonArray arr = new JsonArray();

        for (String name : cfg.getRuleNames()) {
            arr.add(name);
        }

        obj.add(RULES, arr);

        if (entry.supportsThreshold()) {
            obj.addProperty(THRESHOLD, cfg.getThreshold());
        }

        return obj;
    }

    public static DynamicHudRuleConfig fromJsonDynamicHudRuleConfig(JsonObject obj) {
        DynamicHudRuleConfig cfg = new DynamicHudRuleConfig();
        EnumSet<HudTrigger> rules = EnumSet.noneOf(HudTrigger.class);

        if (obj == null) {
            cfg.setRules(rules);
            return cfg;
        }

        JsonElement rulesEl = obj.get(RULES);
        JsonElement thresholdEl = obj.get(THRESHOLD);

        if (rulesEl == null || rulesEl.isJsonNull()) {
            cfg.setRules(rules);
            return cfg;
        }

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
                if (el == null || el.isJsonNull() || !el.isJsonPrimitive()) {
                    continue;
                }

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

    private static boolean isString(JsonObject obj, String key) {
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

    private static boolean isBoolean(JsonElement el) {
        return el != null
                && !el.isJsonNull()
                && el.isJsonPrimitive()
                && el.getAsJsonPrimitive().isBoolean();
    }

    private static JsonObject getObject(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        return (el != null && !el.isJsonNull() && el.isJsonObject())
                ? el.getAsJsonObject()
                : null;
    }

    private static String capitalizeKey(String key) {
        return (key != null && !key.isEmpty())
                ? Character.toUpperCase(key.charAt(0)) + key.substring(1)
                : "";
    }

    private static String staticConfigKey(String key) {
        return HIDE_PREFIX + capitalizeKey(key);
    }

    private static String dynamicConfigKey(String key) {
        return capitalizeKey(key);
    }
}