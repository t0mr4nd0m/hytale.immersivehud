package com.tom.immersivehudplugin.hud.component;

import com.tom.immersivehudplugin.config.DynamicHudConfig;
import com.tom.immersivehudplugin.config.DynamicHudRuleConfig;
import com.tom.immersivehudplugin.config.HudComponentsConfig;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public final class HudComponentRegistry {

    private HudComponentRegistry() {}

    @FunctionalInterface
    public interface BoolGetter<T> {
        boolean get(T value);
    }

    @FunctionalInterface
    public interface BoolSetter<T> {
        void set(T value, boolean enabled);
    }

    public enum Group {

        CORE    ("CORE",    "Core"),
        BARS    ("BARS",    "Bars"),
        UI      ("UI",      "UI"),
        SOCIAL  ("SOCIAL",  "Social"),
        PANELS  ("PANELS",  "Panels"),
        BUILDER ("BUILDER", "Builder");

        public final String key;
        public final String label;

        Group(String key, String label) {
            this.key = key;
            this.label = label;
        }

        @Nullable
        public static Group fromKey(@Nullable String key) {
            String normalized = normalize(key);
            for (Group g : values()) {
                if (g.key.equals(normalized)) {
                    return g;
                }
            }
            return null;
        }

        public String label() {
            return label;
        }
    }

    private static final List<HudComponent> ALL_LIST;
    private static final List<HudComponent> DYNAMIC_LIST;
    private static final Map<String, HudComponent> REGISTRY;
    private static final Map<String, HudComponent> DYNAMIC_REGISTRY;

    public static final List<Group> groupOrder = List.of(
            Group.CORE,
            Group.BARS,
            Group.UI,
            Group.SOCIAL,
            Group.PANELS,
            Group.BUILDER
    );

    static {
        ALL_LIST = List.copyOf(HudComponentCatalog.createAll());
        DYNAMIC_LIST = ALL_LIST.stream()
                .filter(HudComponent::supportsDynamicRules)
                .toList();

        REGISTRY = buildRegistry(ALL_LIST);
        DYNAMIC_REGISTRY = buildRegistry(DYNAMIC_LIST);
    }

    private static Map<String, HudComponent> buildRegistry(List<HudComponent> entries) {
        Map<String, HudComponent> map = new LinkedHashMap<>();
        for (HudComponent entry : entries) {
            map.put(normalize(entry.key()), entry);
        }
        return Collections.unmodifiableMap(map);
    }

    public static List<HudComponent> allList() {
        return ALL_LIST;
    }

    public static List<HudComponent> dynamicList() {
        return DYNAMIC_LIST;
    }

    @Nullable
    public static HudComponent find(@Nullable String key) {
        return REGISTRY.get(normalize(key));
    }

    @Nullable
    public static HudComponent findDynamic(@Nullable String key) {
        return DYNAMIC_REGISTRY.get(normalize(key));
    }

    public static String normalize(@Nullable String key) {
        return key == null ? "" : key.trim().toLowerCase(Locale.ROOT).replace("_", "");
    }

    public static HudComponentsConfig buildDefaultHudComponents() {
        HudComponentsConfig cfg = new HudComponentsConfig();

        for (HudComponent entry : allList()) {
            entry.setHidden(cfg, entry.defaultHidden());
        }

        return cfg;
    }

    public static DynamicHudConfig buildDefaultDynamicHud() {
        DynamicHudConfig cfg = new DynamicHudConfig();

        for (HudComponent entry : dynamicList()) {

            DynamicHudRuleConfig ruleCfg = entry.getDynamicRuleConfig(cfg);

            if (ruleCfg != null) {
                ruleCfg.setRules(EnumSet.copyOf(entry.defaultRules()));
            }

            if (entry.defaultThreshold() != null && ruleCfg != null) {
                ruleCfg.setThreshold(entry.defaultThreshold());
            }
        }

        return cfg;
    }

    public static String availableDynamicComponentsText() {
        return dynamicList().stream()
                .map(HudComponent::key)
                .sorted()
                .collect(Collectors.joining(", "));
    }
}