package com.tom.immersivehudplugin.registry;

import com.hypixel.hytale.protocol.packets.interface_.HudComponent;
import com.tom.immersivehudplugin.config.DynamicHudConfig;
import com.tom.immersivehudplugin.config.DynamicHudRuleConfig;
import com.tom.immersivehudplugin.config.HudComponentsConfig;
import com.tom.immersivehudplugin.rules.DynamicHudTriggers;

import javax.annotation.Nonnull;
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
            @Nullable Float defaultThreshold,
            EnumSet<DynamicHudTriggers> allowedRules
    ) {
        public HudEntry {
            defaultRules = defaultRules == null
                    ? EnumSet.noneOf(DynamicHudTriggers.class)
                    : EnumSet.copyOf(defaultRules);

            allowedRules = allowedRules == null
                    ? EnumSet.noneOf(DynamicHudTriggers.class)
                    : EnumSet.copyOf(allowedRules);

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

        public boolean supportsRule(@Nonnull DynamicHudTriggers rule) {
            return allowedRules.contains(rule);
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
                "HideHealth",
                "Health",
                HudComponentsConfig::isHideHealthHud,
                HudComponentsConfig::setHideHealthHud,
                DynamicHudConfig::getHealth,
                true,
                EnumSet.of(DynamicHudTriggers.HEALTH_NOT_FULL),
                100f,
                EnumSet.of(
                        DynamicHudTriggers.HOTBAR_INPUT,
                        DynamicHudTriggers.HEALTH_NOT_FULL
                )
        ));

        register(new HudEntry(
                "stamina", "Stamina", Group.BARS, HudComponent.Stamina,
                "HideStamina",
                "Stamina",
                HudComponentsConfig::isHideStaminaHud,
                HudComponentsConfig::setHideStaminaHud,
                DynamicHudConfig::getStamina,
                true,
                EnumSet.of(DynamicHudTriggers.STAMINA_NOT_FULL),
                100f,
                EnumSet.of(
                        DynamicHudTriggers.HOTBAR_INPUT,
                        DynamicHudTriggers.STAMINA_NOT_FULL
                )
        ));

        register(new HudEntry(
                "mana", "Mana", Group.BARS, HudComponent.Mana,
                "HideMana",
                "Mana",
                HudComponentsConfig::isHideManaHud,
                HudComponentsConfig::setHideManaHud,
                DynamicHudConfig::getMana,
                true,
                EnumSet.of(DynamicHudTriggers.MANA_NOT_FULL),
                100f,
                EnumSet.of(
                        DynamicHudTriggers.HOTBAR_INPUT,
                        DynamicHudTriggers.MANA_NOT_FULL
                )
        ));

        register(new HudEntry(
                "oxygen", "Oxygen", Group.BARS, HudComponent.Oxygen,
                "HideOxygen",
                "Oxygen",
                HudComponentsConfig::isHideOxygenHud,
                HudComponentsConfig::setHideOxygenHud,
                DynamicHudConfig::getOxygen,
                true,
                EnumSet.of(DynamicHudTriggers.OXYGEN_NOT_FULL),
                100f,
                EnumSet.of(
                        DynamicHudTriggers.HOTBAR_INPUT,
                        DynamicHudTriggers.OXYGEN_NOT_FULL
                )
        ));

        // Core (dynamic-capable)

        register(new HudEntry(
                "compass", "Compass", Group.CORE, HudComponent.Compass,
                "HideCompass",
                "Compass",
                HudComponentsConfig::isHideCompassHud,
                HudComponentsConfig::setHideCompassHud,
                DynamicHudConfig::getCompass,
                true,
                EnumSet.of(DynamicHudTriggers.PLAYER_MOVING),
                null,
                EnumSet.of(
                        DynamicHudTriggers.HOTBAR_INPUT,
                        DynamicHudTriggers.PLAYER_MOVING,
                        DynamicHudTriggers.PLAYER_WALKING,
                        DynamicHudTriggers.PLAYER_RUNNING,
                        DynamicHudTriggers.PLAYER_SPRINTING,
                        DynamicHudTriggers.PLAYER_MOUNTING,
                        DynamicHudTriggers.PLAYER_SWIMMING,
                        DynamicHudTriggers.PLAYER_FLYING,
                        DynamicHudTriggers.PLAYER_GLIDING,
                        DynamicHudTriggers.PLAYER_JUMPING,
                        DynamicHudTriggers.PLAYER_CROUCHING,
                        DynamicHudTriggers.PLAYER_CLIMBING,
                        DynamicHudTriggers.PLAYER_FALLING,
                        DynamicHudTriggers.PLAYER_ROLLING,
                        DynamicHudTriggers.PLAYER_IDLE,
                        DynamicHudTriggers.PLAYER_SITTING,
                        DynamicHudTriggers.PLAYER_SLEEPING,
                        DynamicHudTriggers.PLAYER_IN_FLUID,
                        DynamicHudTriggers.PLAYER_ON_GROUND
                )
        ));

        register(new HudEntry(
                "hotbar", "Hotbar", Group.CORE, HudComponent.Hotbar,
                "HideHotbar",
                "Hotbar",
                HudComponentsConfig::isHideHotbarHud,
                HudComponentsConfig::setHideHotbarHud,
                DynamicHudConfig::getHotbar,
                true,
                EnumSet.of(DynamicHudTriggers.HOTBAR_INPUT),
                null,
                EnumSet.of(
                        DynamicHudTriggers.HOTBAR_INPUT,
                        DynamicHudTriggers.CHARGING_WEAPON,
                        DynamicHudTriggers.CONSUMABLE_USE,
                        DynamicHudTriggers.HOLDING_MELEE_WEAPON,
                        DynamicHudTriggers.HOLDING_RANGED_WEAPON
                )
        ));

        register(new HudEntry(
                "reticle", "Reticle", Group.CORE, HudComponent.Reticle,
                "HideReticle",
                "Reticle",
                HudComponentsConfig::isHideReticleHud,
                HudComponentsConfig::setHideReticleHud,
                DynamicHudConfig::getReticle,
                true,
                EnumSet.of(
                        DynamicHudTriggers.CHARGING_WEAPON,
                        DynamicHudTriggers.CONSUMABLE_USE,
                        DynamicHudTriggers.TARGET_ENTITY,
                        DynamicHudTriggers.INTERACTABLE_BLOCK,
                        DynamicHudTriggers.HOLDING_RANGED_WEAPON
                ),
                null,
                EnumSet.of(
                        DynamicHudTriggers.HOTBAR_INPUT,
                        DynamicHudTriggers.CHARGING_WEAPON,
                        DynamicHudTriggers.CONSUMABLE_USE,
                        DynamicHudTriggers.TARGET_ENTITY,
                        DynamicHudTriggers.INTERACTABLE_BLOCK,
                        DynamicHudTriggers.HOLDING_RANGED_WEAPON,
                        DynamicHudTriggers.HOLDING_MELEE_WEAPON
                )
        ));

        // Static-only

        register(new HudEntry(
                "inputbindings", "Input Bindings", Group.UI, HudComponent.InputBindings,
                "HideInputBindings",
                null,
                HudComponentsConfig::isHideInputBindingsHud,
                HudComponentsConfig::setHideInputBindingsHud,
                null,
                true,
                EnumSet.noneOf(DynamicHudTriggers.class),
                null,
                null
        ));

        register(new HudEntry(
                "notifications", "Notifications", Group.UI, HudComponent.Notifications,
                "HideNotifications",
                null,
                HudComponentsConfig::isHideNotificationsHud,
                HudComponentsConfig::setHideNotificationsHud,
                null,
                true,
                EnumSet.noneOf(DynamicHudTriggers.class),
                null,
                null
        ));

        register(new HudEntry(
                "statusicons", "Status Icons", Group.UI, HudComponent.StatusIcons,
                "HideStatusIcons",
                null,
                HudComponentsConfig::isHideStatusIconsHud,
                HudComponentsConfig::setHideStatusIconsHud,
                null,
                false,
                EnumSet.noneOf(DynamicHudTriggers.class),
                null,
                null
        ));

        register(new HudEntry(
                "speedometer", "Speedometer", Group.UI, HudComponent.Speedometer,
                "HideSpeedometer",
                null,
                HudComponentsConfig::isHideSpeedometerHud,
                HudComponentsConfig::setHideSpeedometerHud,
                null,
                false,
                EnumSet.noneOf(DynamicHudTriggers.class),
                null,
                null
        ));

        register(new HudEntry(
                "ammo", "Ammo Indicator", Group.UI, HudComponent.AmmoIndicator,
                "HideAmmoIndicator",
                null,
                HudComponentsConfig::isHideAmmoIndicatorHud,
                HudComponentsConfig::setHideAmmoIndicatorHud,
                null,
                false,
                EnumSet.noneOf(DynamicHudTriggers.class),
                null,
                null
        ));

        register(new HudEntry(
                "utilityslotselector", "Utility Slot Selector", Group.UI, HudComponent.UtilitySlotSelector,
                "HideUtilitySlotSelector",
                null,
                HudComponentsConfig::isHideUtilitySlotSelectorHud,
                HudComponentsConfig::setHideUtilitySlotSelectorHud,
                null,
                false,
                EnumSet.noneOf(DynamicHudTriggers.class),
                null,
                null
        ));

        register(new HudEntry(
                "chat", "Chat", Group.SOCIAL, HudComponent.Chat,
                "HideChat",
                null,
                HudComponentsConfig::isHideChatHud,
                HudComponentsConfig::setHideChatHud,
                null,
                false,
                EnumSet.noneOf(DynamicHudTriggers.class),
                null,
                null
        ));

        register(new HudEntry(
                "requests", "Requests", Group.SOCIAL, HudComponent.Requests,
                "HideRequests",
                null,
                HudComponentsConfig::isHideRequestsHud,
                HudComponentsConfig::setHideRequestsHud,
                null,
                false,
                EnumSet.noneOf(DynamicHudTriggers.class),
                null,
                null
        ));

        register(new HudEntry(
                "killfeed", "Kill Feed", Group.SOCIAL, HudComponent.KillFeed,
                "HideKillFeed",
                null,
                HudComponentsConfig::isHideKillFeedHud,
                HudComponentsConfig::setHideKillFeedHud,
                null,
                false,
                EnumSet.noneOf(DynamicHudTriggers.class),
                null,
                null
        ));

        register(new HudEntry(
                "playerlist", "Player List", Group.SOCIAL, HudComponent.PlayerList,
                "HidePlayerList",
                null,
                HudComponentsConfig::isHidePlayerListHud,
                HudComponentsConfig::setHidePlayerListHud,
                null,
                false,
                EnumSet.noneOf(DynamicHudTriggers.class),
                null,
                null
        ));

        register(new HudEntry(
                "eventtitle", "Event Title", Group.PANELS, HudComponent.EventTitle,
                "HideEventTitle",
                null,
                HudComponentsConfig::isHideEventTitleHud,
                HudComponentsConfig::setHideEventTitleHud,
                null,
                false,
                EnumSet.noneOf(DynamicHudTriggers.class),
                null,
                null
        ));

        register(new HudEntry(
                "objectivepanel", "Objective Panel", Group.PANELS, HudComponent.ObjectivePanel,
                "HideObjectivePanel",
                null,
                HudComponentsConfig::isHideObjectivePanelHud,
                HudComponentsConfig::setHideObjectivePanelHud,
                null,
                false,
                EnumSet.noneOf(DynamicHudTriggers.class),
                null,
                null
        ));

        register(new HudEntry(
                "portalpanel", "Portal Panel", Group.PANELS, HudComponent.PortalPanel,
                "HidePortalPanel",
                null,
                HudComponentsConfig::isHidePortalPanelHud,
                HudComponentsConfig::setHidePortalPanelHud,
                null,
                false,
                EnumSet.noneOf(DynamicHudTriggers.class),
                null,
                null
        ));

        register(new HudEntry(
                "sleep", "Sleep", Group.PANELS, HudComponent.Sleep,
                "HideSleep",
                null,
                HudComponentsConfig::isHideSleepHud,
                HudComponentsConfig::setHideSleepHud,
                null,
                false,
                EnumSet.noneOf(DynamicHudTriggers.class),
                null,
                null
        ));

        register(new HudEntry(
                "blockvariantselector", "Block Variant Selector", Group.BUILDER, HudComponent.BlockVariantSelector,
                "HideBlockVariantSelector",
                null,
                HudComponentsConfig::isHideBlockVariantSelectorHud,
                HudComponentsConfig::setHideBlockVariantSelectorHud,
                null,
                false,
                EnumSet.noneOf(DynamicHudTriggers.class),
                null,
                null
        ));

        register(new HudEntry(
                "buildertoolslegend", "Builder Tools Legend", Group.BUILDER, HudComponent.BuilderToolsLegend,
                "HideBuilderToolsLegend",
                null,
                HudComponentsConfig::isHideBuilderToolsLegendHud,
                HudComponentsConfig::setHideBuilderToolsLegendHud,
                null,
                false,
                EnumSet.noneOf(DynamicHudTriggers.class),
                null,
                null
        ));

        register(new HudEntry(
                "buildertoolsmaterialslotselector", "Builder Tools Material Slot Selector", Group.BUILDER, HudComponent.BuilderToolsMaterialSlotSelector,
                "HideBuilderToolsMaterialSlotSelector",
                null,
                HudComponentsConfig::isHideBuilderToolsMaterialSlotSelectorHud,
                HudComponentsConfig::setHideBuilderToolsMaterialSlotSelectorHud,
                null,
                false,
                EnumSet.noneOf(DynamicHudTriggers.class),
                null,
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

    public static List<Group> groupOrder = List.of(
            Group.CORE,
            Group.BARS,
            Group.UI,
            Group.SOCIAL,
            Group.PANELS,
            Group.BUILDER
    );
}