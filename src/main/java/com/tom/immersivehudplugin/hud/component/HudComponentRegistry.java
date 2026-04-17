package com.tom.immersivehudplugin.hud.component;

import com.tom.immersivehudplugin.config.DynamicHudConfig;
import com.tom.immersivehudplugin.config.DynamicHudRuleConfig;
import com.tom.immersivehudplugin.config.HudComponentsConfig;
import com.tom.immersivehudplugin.hud.trigger.HudTrigger;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
                if (normalize(g.key).equals(normalized)) {
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
        ALL_LIST = List.copyOf(HudComponentCatalog.createAll());
        DYNAMIC_LIST = ALL_LIST.stream()
                .filter(HudComponent::supportsDynamicRules)
                .toList();

        REGISTRY = buildRegistry(ALL_LIST);
        DYNAMIC_REGISTRY = buildRegistry(DYNAMIC_LIST);
        GROUPED = ALL_LIST.stream()
                .collect(Collectors.groupingBy(
                        HudComponent::group,
                        LinkedHashMap::new,
                        Collectors.toUnmodifiableList()
                ));
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

    @Nullable
    public static Group findGroup(@Nullable String key) {
        return Group.fromKey(key);
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
            DynamicHudRuleConfig ruleCfg = entry.getDynamicRuleConfig(cfg);
            if (ruleCfg == null) {
                continue;
            }

            EnumSet<HudTrigger> defaultRules = entry.defaultRules();
            ruleCfg.setRules(defaultRules != null ? defaultRules : EnumSet.noneOf(HudTrigger.class));

            if (entry.supportsThreshold()) {
                ruleCfg.setThreshold(entry.defaultThreshold());
            }
        }

        return cfg;
    }

    private static String joinSortedKeys(Stream<String> keys) {
        return keys.sorted().collect(Collectors.joining(", "));
    }

    public static String availableDynamicComponentsText() {
        return joinSortedKeys(dynamicList().stream().map(HudComponent::key));
    }

    public static String availableComponentsText() {
        return joinSortedKeys(allList().stream().map(HudComponent::key));
    }

    public static String availableGroupsText() {
        return joinSortedKeys(Arrays.stream(Group.values()).map(g -> g.key));
    }
}