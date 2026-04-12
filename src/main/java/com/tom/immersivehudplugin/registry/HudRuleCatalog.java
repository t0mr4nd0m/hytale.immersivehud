package com.tom.immersivehudplugin.registry;

import com.tom.immersivehudplugin.rules.DynamicHudTriggers;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;

public final class HudRuleCatalog {

    private static final Map<String, EnumSet<DynamicHudTriggers>> ALLOWED_RULES_BY_COMPONENT;

    static {
        Map<String, EnumSet<DynamicHudTriggers>> map = new LinkedHashMap<>();

        map.put("health", EnumSet.of(
                DynamicHudTriggers.HOTBAR_INPUT,
                DynamicHudTriggers.HEALTH_NOT_FULL
        ));

        map.put("stamina", EnumSet.of(
                DynamicHudTriggers.HOTBAR_INPUT,
                DynamicHudTriggers.STAMINA_NOT_FULL
        ));

        map.put("mana", EnumSet.of(
                DynamicHudTriggers.HOTBAR_INPUT,
                DynamicHudTriggers.MANA_NOT_FULL
        ));

        map.put("oxygen", EnumSet.of(
                DynamicHudTriggers.HOTBAR_INPUT,
                DynamicHudTriggers.OXYGEN_NOT_FULL
        ));

        map.put("compass", EnumSet.of(
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
        ));

        map.put("hotbar", EnumSet.of(
                DynamicHudTriggers.HOTBAR_INPUT,
                DynamicHudTriggers.CONSUMABLE_USE,
                DynamicHudTriggers.HOLDING_MELEE_WEAPON,
                DynamicHudTriggers.HOLDING_RANGED_WEAPON
        ));

        map.put("reticle", EnumSet.of(
                DynamicHudTriggers.HOTBAR_INPUT,
                DynamicHudTriggers.CHARGING_WEAPON,
                DynamicHudTriggers.CONSUMABLE_USE,
                DynamicHudTriggers.TARGET_ENTITY,
                DynamicHudTriggers.INTERACTABLE_BLOCK,
                DynamicHudTriggers.HOLDING_RANGED_WEAPON,
                DynamicHudTriggers.HOLDING_MELEE_WEAPON
        ));

        Map<String, EnumSet<DynamicHudTriggers>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, EnumSet<DynamicHudTriggers>> entry : map.entrySet()) {
            copy.put(entry.getKey(), EnumSet.copyOf(entry.getValue()));
        }

        ALLOWED_RULES_BY_COMPONENT = Collections.unmodifiableMap(copy);
    }

    private HudRuleCatalog() {}

    @Nonnull
    public static EnumSet<DynamicHudTriggers> getAllowedRules(
            @Nonnull HudEntry entry
    ) {
        EnumSet<DynamicHudTriggers> rules = ALLOWED_RULES_BY_COMPONENT.get(HudComponentRegistry.normalize(entry.key()));
        return rules == null
                ? EnumSet.noneOf(DynamicHudTriggers.class)
                : EnumSet.copyOf(rules);
    }

    public static boolean supportsRule(
            @Nonnull HudEntry entry,
            @Nonnull DynamicHudTriggers rule
    ) {
        return getAllowedRules(entry).contains(rule);
    }
}