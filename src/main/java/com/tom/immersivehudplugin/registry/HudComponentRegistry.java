package com.tom.immersivehudplugin.registry;

import com.hypixel.hytale.protocol.packets.interface_.HudComponent;
import com.tom.immersivehudplugin.config.DynamicHudConfig;
import com.tom.immersivehudplugin.config.DynamicHudRuleConfig;
import com.tom.immersivehudplugin.config.HudComponentsConfig;

import javax.annotation.Nullable;
import java.util.Collections;
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
    }

    public record HudEntry(
            String key,
            String label,
            Group group,
            HudComponent hudComponent,
            BoolGetter<HudComponentsConfig> staticGetter,
            BoolSetter<HudComponentsConfig> staticSetter,
            @Nullable Function<DynamicHudConfig, DynamicHudRuleConfig> dynamicGetter
    ) {
        public boolean supportsDynamicRules() {
            return dynamicGetter != null;
        }
    }

    private static final Map<String, HudEntry> REGISTRY = new LinkedHashMap<>();
    private static final List<HudEntry> ALL_LIST;
    private static final List<HudEntry> DYNAMIC_LIST;
    private static final Map<String, HudEntry> DYNAMIC_REGISTRY;

    static {

        // Bars (dynamic-capable)

        register(new HudEntry(
                "health", "Health", Group.BARS, HudComponent.Health,
                HudComponentsConfig::isHideHealthHud,
                HudComponentsConfig::setHideHealthHud,
                DynamicHudConfig::getHealth
        ));

        register(new HudEntry(
                "stamina", "Stamina", Group.BARS, HudComponent.Stamina,
                HudComponentsConfig::isHideStaminaHud,
                HudComponentsConfig::setHideStaminaHud,
                DynamicHudConfig::getStamina
        ));

        register(new HudEntry(
                "mana", "Mana", Group.BARS, HudComponent.Mana,
                HudComponentsConfig::isHideManaHud,
                HudComponentsConfig::setHideManaHud,
                DynamicHudConfig::getMana
        ));

        // Core (dynamic-capable)

        register(new HudEntry(
                "compass", "Compass", Group.CORE, HudComponent.Compass,
                HudComponentsConfig::isHideCompassHud,
                HudComponentsConfig::setHideCompassHud,
                DynamicHudConfig::getCompass
        ));

        register(new HudEntry(
                "hotbar", "Hotbar", Group.CORE, HudComponent.Hotbar,
                HudComponentsConfig::isHideHotbarHud,
                HudComponentsConfig::setHideHotbarHud,
                DynamicHudConfig::getHotbar
        ));

        register(new HudEntry(
                "reticle", "Reticle", Group.CORE, HudComponent.Reticle,
                HudComponentsConfig::isHideReticleHud,
                HudComponentsConfig::setHideReticleHud,
                DynamicHudConfig::getReticle
        ));

        // Static-only

        register(new HudEntry(
                "inputbindings", "Input Bindings", Group.UI, HudComponent.InputBindings,
                HudComponentsConfig::isHideInputBindingsHud,
                HudComponentsConfig::setHideInputBindingsHud,
                null
        ));

        register(new HudEntry(
                "notifications", "Notifications", Group.UI, HudComponent.Notifications,
                HudComponentsConfig::isHideNotificationsHud,
                HudComponentsConfig::setHideNotificationsHud,
                null
        ));

        register(new HudEntry(
                "statusicons", "Status Icons", Group.UI, HudComponent.StatusIcons,
                HudComponentsConfig::isHideStatusIconsHud,
                HudComponentsConfig::setHideStatusIconsHud,
                null
        ));

        register(new HudEntry(
                "speedometer", "Speedometer", Group.UI, HudComponent.Speedometer,
                HudComponentsConfig::isHideSpeedometerHud,
                HudComponentsConfig::setHideSpeedometerHud,
                null
        ));

        register(new HudEntry(
                "ammo", "Ammo Indicator", Group.UI, HudComponent.AmmoIndicator,
                HudComponentsConfig::isHideAmmoIndicatorHud,
                HudComponentsConfig::setHideAmmoIndicatorHud,
                null
        ));

        register(new HudEntry(
                "oxygen", "Oxygen", Group.UI, HudComponent.Oxygen,
                HudComponentsConfig::isHideOxygenHud,
                HudComponentsConfig::setHideOxygenHud,
                null
        ));

        register(new HudEntry(
                "chat", "Chat", Group.SOCIAL, HudComponent.Chat,
                HudComponentsConfig::isHideChatHud,
                HudComponentsConfig::setHideChatHud,
                null
        ));

        register(new HudEntry(
                "requests", "Requests", Group.SOCIAL, HudComponent.Requests,
                HudComponentsConfig::isHideRequestsHud,
                HudComponentsConfig::setHideRequestsHud,
                null
        ));

        register(new HudEntry(
                "killfeed", "Kill Feed", Group.SOCIAL, HudComponent.KillFeed,
                HudComponentsConfig::isHideKillFeedHud,
                HudComponentsConfig::setHideKillFeedHud,
                null
        ));

        register(new HudEntry(
                "playerlist", "Player List", Group.SOCIAL, HudComponent.PlayerList,
                HudComponentsConfig::isHidePlayerListHud,
                HudComponentsConfig::setHidePlayerListHud,
                null
        ));

        register(new HudEntry(
                "eventtitle", "Event Title", Group.PANELS, HudComponent.EventTitle,
                HudComponentsConfig::isHideEventTitleHud,
                HudComponentsConfig::setHideEventTitleHud,
                null
        ));

        register(new HudEntry(
                "objectivepanel", "Objective Panel", Group.PANELS, HudComponent.ObjectivePanel,
                HudComponentsConfig::isHideObjectivePanelHud,
                HudComponentsConfig::setHideObjectivePanelHud,
                null
        ));

        register(new HudEntry(
                "portalpanel", "Portal Panel", Group.PANELS, HudComponent.PortalPanel,
                HudComponentsConfig::isHidePortalPanelHud,
                HudComponentsConfig::setHidePortalPanelHud,
                null
        ));

        register(new HudEntry(
                "sleep", "Sleep", Group.PANELS, HudComponent.Sleep,
                HudComponentsConfig::isHideSleepHud,
                HudComponentsConfig::setHideSleepHud,
                null
        ));

        register(new HudEntry(
                "utilityslotselector", "Utility Slot Selector", Group.BUILDER, HudComponent.UtilitySlotSelector,
                HudComponentsConfig::isHideUtilitySlotSelectorHud,
                HudComponentsConfig::setHideUtilitySlotSelectorHud,
                null
        ));

        register(new HudEntry(
                "blockvariantselector", "Block Variant Selector", Group.BUILDER, HudComponent.BlockVariantSelector,
                HudComponentsConfig::isHideBlockVariantSelectorHud,
                HudComponentsConfig::setHideBlockVariantSelectorHud,
                null
        ));

        register(new HudEntry(
                "buildertoolslegend", "Builder Tools Legend", Group.BUILDER, HudComponent.BuilderToolsLegend,
                HudComponentsConfig::isHideBuilderToolsLegendHud,
                HudComponentsConfig::setHideBuilderToolsLegendHud,
                null
        ));

        register(new HudEntry(
                "buildertoolsmaterialslotselector", "Builder Tools Material Slot Selector", Group.BUILDER, HudComponent.BuilderToolsMaterialSlotSelector,
                HudComponentsConfig::isHideBuilderToolsMaterialSlotSelectorHud,
                HudComponentsConfig::setHideBuilderToolsMaterialSlotSelectorHud,
                null
        ));

        ALL_LIST = List.copyOf(REGISTRY.values());
        DYNAMIC_LIST = ALL_LIST.stream()
                .filter(HudEntry::supportsDynamicRules)
                .toList();

        Map<String, HudEntry> dynamicMap = new LinkedHashMap<>();
        for (HudEntry entry : DYNAMIC_LIST) {
            dynamicMap.put(normalize(entry.key()), entry);
        }
        DYNAMIC_REGISTRY = Collections.unmodifiableMap(dynamicMap);
    }

    private static void register(HudEntry entry) {
        REGISTRY.put(normalize(entry.key()), entry);
    }

    public static Map<String, HudEntry> all() {
        return Collections.unmodifiableMap(REGISTRY);
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