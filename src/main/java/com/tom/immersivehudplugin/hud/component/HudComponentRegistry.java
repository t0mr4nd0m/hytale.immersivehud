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

        CORE    ("core",    "Core"),
        BARS    ("bars",    "Bars"),
        UI      ("ui",      "UI"),
        SOCIAL  ("social",  "Social"),
        PANELS  ("panels",  "Panels"),
        BUILDER ("builder", "Builder");

        public final String key;
        public final String label;

        Group(String key, String label) {
            this.key = key;
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    private static final List<HudComponent> ALL_LIST;
    private static final List<HudComponent> DYNAMIC_LIST;
    private static final Map<String, HudComponent> REGISTRY;
    private static final Map<Group, List<HudComponent>> GROUPED;

    public static final List<Group> GROUP_ORDER = List.of(
            Group.CORE,
            Group.BARS,
            Group.UI,
            Group.SOCIAL,
            Group.PANELS,
            Group.BUILDER
    );

    static {
        ALL_LIST = List.copyOf(sortByGroupOrder(HudComponentCatalog.createAll()));
        DYNAMIC_LIST = ALL_LIST.stream()
                .filter(HudComponent::supportsDynamicRules)
                .toList();

        REGISTRY = buildRegistry(ALL_LIST);
        GROUPED = ALL_LIST.stream()
                .collect(Collectors.groupingBy(
                        HudComponent::group,
                        LinkedHashMap::new,
                        Collectors.toUnmodifiableList()
                ));
    }

    private static List<HudComponent> sortByGroupOrder(List<HudComponent> entries) {
        Map<Group, Integer> groupIndex = new LinkedHashMap<>();
        for (int i = 0; i < GROUP_ORDER.size(); i++) {
            groupIndex.put(GROUP_ORDER.get(i), i);
        }

        return entries.stream()
                .sorted((a, b) -> {
                    int ga = groupIndex.getOrDefault(a.group(), Integer.MAX_VALUE);
                    int gb = groupIndex.getOrDefault(b.group(), Integer.MAX_VALUE);
                    return Integer.compare(ga, gb);
                })
                .toList();
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

    public static List<HudComponent> entriesOf(Group group) {
        return GROUPED.getOrDefault(group, List.of());
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
            DynamicHudRuleConfig ruleCfg = entry.requireDynamicRuleConfig(cfg);

            ruleCfg.setRules(EnumSet.copyOf(entry.defaultRules()));

            if (entry.supportsThreshold()) {
                ruleCfg.setThreshold(entry.defaultThreshold());
            }
        }

        return cfg;
    }
}