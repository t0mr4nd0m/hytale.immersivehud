package com.tom.immersivehudplugin.registry;

import com.hypixel.hytale.protocol.packets.interface_.HudComponent;
import com.tom.immersivehudplugin.config.DynamicHudConfig;
import com.tom.immersivehudplugin.config.DynamicHudRuleConfig;
import com.tom.immersivehudplugin.config.HudComponentsConfig;
import com.tom.immersivehudplugin.rules.DynamicHudTriggers;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

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

        CORE("core", "Core"),
        BARS("bars", "Bars"),
        UI("ui", "UI"),
        SOCIAL("social", "Social"),
        PANELS("panels", "Panels"),
        BUILDER("builder", "Builder");

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

    public record HudEntry(
            String key,
            String label,
            Group group,
            HudComponent hudComponent,
            String staticConfigKey,
            @Nullable String dynamicConfigKey,
            BoolGetter<HudComponentsConfig> staticGetter,
            BoolSetter<HudComponentsConfig> staticSetter,
            @Nullable Function<DynamicHudConfig, DynamicHudRuleConfig> dynamicGetter,
            boolean defaultHidden,
            EnumSet<DynamicHudTriggers> defaultRules,
            @Nullable Float defaultThreshold
    ) {
        public HudEntry {
            defaultRules = defaultRules == null
                    ? EnumSet.noneOf(DynamicHudTriggers.class)
                    : EnumSet.copyOf(defaultRules);

            if (defaultThreshold != null) {
                defaultThreshold = Math.max(0f, Math.min(100f, defaultThreshold));
            }
        }

        public boolean supportsDynamicRules() {
            return dynamicGetter != null && dynamicConfigKey != null;
        }

        public boolean supportsThreshold() {
            return defaultThreshold != null;
        }
    }

    private static final List<HudEntry> ALL_LIST;
    private static final List<HudEntry> DYNAMIC_LIST;
    private static final Map<String, HudEntry> REGISTRY;
    private static final Map<String, HudEntry> DYNAMIC_REGISTRY;

    public static final List<Group> groupOrder = List.of(
            Group.CORE,
            Group.BARS,
            Group.UI,
            Group.SOCIAL,
            Group.PANELS,
            Group.BUILDER
    );

    static {
        ALL_LIST = List.copyOf(HudDefinitions.createAll());
        DYNAMIC_LIST = ALL_LIST.stream()
                .filter(HudEntry::supportsDynamicRules)
                .toList();

        REGISTRY = buildRegistry(ALL_LIST);
        DYNAMIC_REGISTRY = buildRegistry(DYNAMIC_LIST);
    }

    private static Map<String, HudEntry> buildRegistry(List<HudEntry> entries) {
        Map<String, HudEntry> map = new LinkedHashMap<>();
        for (HudEntry entry : entries) {
            map.put(normalize(entry.key()), entry);
        }
        return Collections.unmodifiableMap(map);
    }

    public static List<HudEntry> allList() {
        return ALL_LIST;
    }

    public static List<HudEntry> dynamicList() {
        return DYNAMIC_LIST;
    }

    @Nullable
    public static HudEntry find(@Nullable String key) {
        return REGISTRY.get(normalize(key));
    }

    @Nullable
    public static HudEntry findDynamic(@Nullable String key) {
        return DYNAMIC_REGISTRY.get(normalize(key));
    }

    public static String normalize(@Nullable String key) {
        return key == null ? "" : key.trim().toLowerCase(Locale.ROOT).replace("_", "");
    }
}