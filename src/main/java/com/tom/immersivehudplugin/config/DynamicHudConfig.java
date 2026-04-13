package com.tom.immersivehudplugin.config;

import com.tom.immersivehudplugin.hud.component.HudComponentRegistry;
import com.tom.immersivehudplugin.hud.component.HudComponent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public final class DynamicHudConfig {

    private final Map<String, DynamicHudRuleConfig> rulesByKey = new LinkedHashMap<>();

    public DynamicHudConfig() {
        ensureAllDynamicEntries();
    }

    @Nonnull
    public DynamicHudRuleConfig getHotbar() {
        return getByKey("hotbar");
    }

    public void setHotbar(@Nullable DynamicHudRuleConfig value) {
        setByKey("hotbar", value);
    }

    @Nonnull
    public DynamicHudRuleConfig getReticle() {
        return getByKey("reticle");
    }

    public void setReticle(@Nullable DynamicHudRuleConfig value) {
        setByKey("reticle", value);
    }

    @Nonnull
    public DynamicHudRuleConfig getCompass() {
        return getByKey("compass");
    }

    public void setCompass(@Nullable DynamicHudRuleConfig value) {
        setByKey("compass", value);
    }

    @Nonnull
    public DynamicHudRuleConfig getHealth() {
        return getByKey("health");
    }

    public void setHealth(@Nullable DynamicHudRuleConfig value) {
        setByKey("health", value);
    }

    @Nonnull
    public DynamicHudRuleConfig getStamina() {
        return getByKey("stamina");
    }

    public void setStamina(@Nullable DynamicHudRuleConfig value) {
        setByKey("stamina", value);
    }

    @Nonnull
    public DynamicHudRuleConfig getMana() {
        return getByKey("mana");
    }

    public void setMana(@Nullable DynamicHudRuleConfig value) {
        setByKey("mana", value);
    }

    @Nonnull
    public DynamicHudRuleConfig getOxygen() {
        return getByKey("oxygen");
    }

    public void setOxygen(@Nullable DynamicHudRuleConfig value) {
        setByKey("oxygen", value);
    }

    @Nonnull
    public DynamicHudRuleConfig getByKey(@Nullable String key) {
        String normalized = normalizeKey(key);
        if (normalized.isEmpty()) {
            return new DynamicHudRuleConfig();
        }
        return rulesByKey.computeIfAbsent(normalized, ignored -> new DynamicHudRuleConfig());
    }

    public void setByKey(@Nullable String key, @Nullable DynamicHudRuleConfig value) {
        String normalized = normalizeKey(key);
        if (normalized.isEmpty()) {
            return;
        }

        rulesByKey.put(normalized, value != null ? value : new DynamicHudRuleConfig());
    }

    public boolean sanitize() {
        boolean changed = false;

        for (HudComponent entry : HudComponentRegistry.dynamicList()) {
            String key = normalizeKey(entry.key());
            DynamicHudRuleConfig ruleCfg = rulesByKey.get(key);

            if (ruleCfg == null) {
                rulesByKey.put(key, new DynamicHudRuleConfig());
                changed = true;
            } else {
                changed |= ruleCfg.sanitize();
            }
        }

        Set<String> validKeys = validDynamicKeys();

        Iterator<Map.Entry<String, DynamicHudRuleConfig>> it = rulesByKey.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, DynamicHudRuleConfig> entry = it.next();
            if (!validKeys.contains(entry.getKey())) {
                it.remove();
                changed = true;
            }
        }

        return changed;
    }

    public DynamicHudConfig copy() {
        DynamicHudConfig copy = new DynamicHudConfig();
        copy.rulesByKey.clear();

        for (Map.Entry<String, DynamicHudRuleConfig> entry : rulesByKey.entrySet()) {
            copy.rulesByKey.put(
                    entry.getKey(),
                    entry.getValue() != null ? entry.getValue().copy() : new DynamicHudRuleConfig()
            );
        }

        copy.ensureAllDynamicEntries();
        return copy;
    }

    private void ensureAllDynamicEntries() {
        for (HudComponent entry : HudComponentRegistry.dynamicList()) {
            String key = normalizeKey(entry.key());
            rulesByKey.computeIfAbsent(key, ignored -> new DynamicHudRuleConfig());
        }
    }

    private static String normalizeKey(@Nullable String key) {
        return HudComponentRegistry.normalize(key);
    }

    private static Set<String> validDynamicKeys() {
        return HudComponentRegistry.dynamicList().stream()
                .map(HudComponent::key)
                .map(HudComponentRegistry::normalize)
                .collect(Collectors.toSet());
    }

    public boolean hasRules(@Nullable String key) {
        return !getByKey(key).getRules().isEmpty();
    }
}