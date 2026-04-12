package com.tom.immersivehudplugin.rules;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class HudRuleDefinitions {

    public record RuleMeta(
            DynamicHudTriggers trigger,
            String label,
            DynamicHudTriggerCategory category
    ) {
    }

    private static final DynamicHudTriggerCategory INTERACTION = DynamicHudTriggerCategory.INTERACTION;
    private static final DynamicHudTriggerCategory COMBAT = DynamicHudTriggerCategory.COMBAT;
    private static final DynamicHudTriggerCategory STATUS = DynamicHudTriggerCategory.STATUS;
    private static final DynamicHudTriggerCategory MOVEMENT = DynamicHudTriggerCategory.MOVEMENT;

    private static final List<RuleMeta> ALL = List.of(
            // Interaction
            new RuleMeta(DynamicHudTriggers.HOTBAR_INPUT, "Hotbar Input", INTERACTION),
            new RuleMeta(DynamicHudTriggers.CONSUMABLE_USE, "Consumable Use", INTERACTION),
            new RuleMeta(DynamicHudTriggers.TARGET_ENTITY, "Target Entity", INTERACTION),
            new RuleMeta(DynamicHudTriggers.INTERACTABLE_BLOCK, "Interactable Block", INTERACTION),

            // Combat
            new RuleMeta(DynamicHudTriggers.CHARGING_WEAPON, "Charging Weapon", COMBAT),
            new RuleMeta(DynamicHudTriggers.HOLDING_RANGED_WEAPON, "Holding Ranged Weapon", COMBAT),
            new RuleMeta(DynamicHudTriggers.HOLDING_MELEE_WEAPON, "Holding Melee Weapon", COMBAT),

            // Status
            new RuleMeta(DynamicHudTriggers.HEALTH_NOT_FULL, "Health Not Full", STATUS),
            new RuleMeta(DynamicHudTriggers.STAMINA_NOT_FULL, "Stamina Not Full", STATUS),
            new RuleMeta(DynamicHudTriggers.MANA_NOT_FULL, "Mana Not Full", STATUS),
            new RuleMeta(DynamicHudTriggers.OXYGEN_NOT_FULL, "Oxygen Not Full", STATUS),

            // Movement
            new RuleMeta(DynamicHudTriggers.PLAYER_MOVING, "Player Moving", MOVEMENT),
            new RuleMeta(DynamicHudTriggers.PLAYER_WALKING, "Player Walking", MOVEMENT),
            new RuleMeta(DynamicHudTriggers.PLAYER_RUNNING, "Player Running", MOVEMENT),
            new RuleMeta(DynamicHudTriggers.PLAYER_SPRINTING, "Player Sprinting", MOVEMENT),
            new RuleMeta(DynamicHudTriggers.PLAYER_MOUNTING, "Player Mounting", MOVEMENT),
            new RuleMeta(DynamicHudTriggers.PLAYER_SWIMMING, "Player Swimming", MOVEMENT),
            new RuleMeta(DynamicHudTriggers.PLAYER_FLYING, "Player Flying", MOVEMENT),
            new RuleMeta(DynamicHudTriggers.PLAYER_GLIDING, "Player Gliding", MOVEMENT),
            new RuleMeta(DynamicHudTriggers.PLAYER_JUMPING, "Player Jumping", MOVEMENT),
            new RuleMeta(DynamicHudTriggers.PLAYER_CROUCHING, "Player Crouching", MOVEMENT),
            new RuleMeta(DynamicHudTriggers.PLAYER_CLIMBING, "Player Climbing", MOVEMENT),
            new RuleMeta(DynamicHudTriggers.PLAYER_FALLING, "Player Falling", MOVEMENT),
            new RuleMeta(DynamicHudTriggers.PLAYER_ROLLING, "Player Rolling", MOVEMENT),
            new RuleMeta(DynamicHudTriggers.PLAYER_IDLE, "Player Idle", MOVEMENT),
            new RuleMeta(DynamicHudTriggers.PLAYER_SITTING, "Player Sitting", MOVEMENT),
            new RuleMeta(DynamicHudTriggers.PLAYER_SLEEPING, "Player Sleeping", MOVEMENT),
            new RuleMeta(DynamicHudTriggers.PLAYER_IN_FLUID, "Player In Fluid", MOVEMENT),
            new RuleMeta(DynamicHudTriggers.PLAYER_ON_GROUND, "Player On Ground", MOVEMENT)
    );

    private static final Map<DynamicHudTriggers, RuleMeta> BY_TRIGGER = buildByTrigger();

    private HudRuleDefinitions() {
    }

    public static List<RuleMeta> all() {
        return ALL;
    }

    public static List<DynamicHudTriggerCategory> groupOrder() {
        return DynamicHudTriggers.displayCategoryOrder();
    }

    public static Map<DynamicHudTriggers, RuleMeta> byTrigger() {
        return BY_TRIGGER;
    }

    private static Map<DynamicHudTriggers, RuleMeta> buildByTrigger() {
        Map<DynamicHudTriggers, RuleMeta> map = new EnumMap<>(DynamicHudTriggers.class);
        for (RuleMeta meta : ALL) {
            map.put(meta.trigger(), meta);
        }
        return Map.copyOf(map);
    }
}