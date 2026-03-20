package com.tom.immersivehudplugin.config;

import com.tom.immersivehudplugin.registry.HudComponentRegistry;
import com.tom.immersivehudplugin.registry.HudComponentRegistry.HudEntry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;

public final class DynamicHudConfig {

    private final Map<String, DynamicHudRuleConfig> rulesByKey = new LinkedHashMap<>();

    public DynamicHudConfig() {
        ensureAllDynamicEntries();
    }

    @Nonnull
    public DynamicHudRuleConfig getHotbar() {
        return getByKey("hotbar");
    }

    public void setHotbar(@Nullable DynamicHudRuleConfig v) {
        setByKey("hotbar", v);
    }

    @Nonnull
    public DynamicHudRuleConfig getReticle() {
        return getByKey("reticle");
    }

    public void setReticle(@Nullable DynamicHudRuleConfig v) {
        setByKey("reticle", v);
    }

    @Nonnull
    public DynamicHudRuleConfig getCompass() {
        return getByKey("compass");
    }

    public void setCompass(@Nullable DynamicHudRuleConfig v) {
        setByKey("compass", v);
    }

    @Nonnull
    public DynamicHudRuleConfig getHealth() {
        return getByKey("health");
    }

    public void setHealth(@Nullable DynamicHudRuleConfig v) {
        setByKey("health", v);
    }

    @Nonnull
    public DynamicHudRuleConfig getStamina() {
        return getByKey("stamina");
    }

    public void setStamina(@Nullable DynamicHudRuleConfig v) {
        setByKey("stamina", v);
    }

    @Nonnull
    public DynamicHudRuleConfig getMana() {
        return getByKey("mana");
    }

    public void setMana(@Nullable DynamicHudRuleConfig v) {
        setByKey("mana", v);
    }

    @Nonnull
    public DynamicHudRuleConfig getByKey(@Nullable String key) {
        String normalized = HudComponentRegistry.normalize(key);
        ensureAllDynamicEntries();
        return rulesByKey.computeIfAbsent(normalized, k -> new DynamicHudRuleConfig());
    }

    public void setByKey(@Nullable String key, @Nullable DynamicHudRuleConfig value) {
        String normalized = HudComponentRegistry.normalize(key);
        if (normalized.isEmpty()) {
            return;
        }

        rulesByKey.put(normalized, value != null ? value : new DynamicHudRuleConfig());
    }

    @Nonnull
    public Map<String, DynamicHudRuleConfig> asMap() {
        ensureAllDynamicEntries();
        return java.util.Collections.unmodifiableMap(rulesByKey);
    }

    public boolean sanitize() {
        boolean changed = false;

        for (HudEntry entry : HudComponentRegistry.dynamicList()) {
            String key = HudComponentRegistry.normalize(entry.key());
            DynamicHudRuleConfig ruleCfg = rulesByKey.get(key);

            if (ruleCfg == null) {
                rulesByKey.put(key, new DynamicHudRuleConfig());
                changed = true;
            } else {
                changed |= ruleCfg.sanitize();
            }
        }

        java.util.Set<String> validKeys = HudComponentRegistry.dynamicList().stream()
                .map(HudEntry::key)
                .map(HudComponentRegistry::normalize)
                .collect(java.util.stream.Collectors.toSet());

        java.util.Iterator<Map.Entry<String, DynamicHudRuleConfig>> it = rulesByKey.entrySet().iterator();
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
        DynamicHudConfig c = new DynamicHudConfig();
        c.rulesByKey.clear();

        for (Map.Entry<String, DynamicHudRuleConfig> entry : rulesByKey.entrySet()) {
            c.rulesByKey.put(
                    entry.getKey(),
                    entry.getValue() != null ? entry.getValue().copy() : new DynamicHudRuleConfig()
            );
        }

        c.ensureAllDynamicEntries();
        return c;
    }

    private void ensureAllDynamicEntries() {
        for (HudEntry entry : HudComponentRegistry.dynamicList()) {
            String key = HudComponentRegistry.normalize(entry.key());
            rulesByKey.computeIfAbsent(key, k -> new DynamicHudRuleConfig());
        }
    }
}