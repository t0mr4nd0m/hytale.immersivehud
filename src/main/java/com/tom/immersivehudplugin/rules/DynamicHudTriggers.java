package com.tom.immersivehudplugin.rules;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public enum DynamicHudTriggers {

    HOTBAR_INPUT(DynamicHudTriggerCategory.INTERACTION, DynamicHudTriggersContext::hotbarInput),
    CHARGING_WEAPON(DynamicHudTriggerCategory.COMBAT, DynamicHudTriggersContext::chargingWeapon),
    CONSUMABLE_USE(DynamicHudTriggerCategory.INTERACTION, DynamicHudTriggersContext::consumableUse),
    TARGET_ENTITY(DynamicHudTriggerCategory.INTERACTION, DynamicHudTriggersContext::targetEntity),
    INTERACTABLE_BLOCK(DynamicHudTriggerCategory.INTERACTION, DynamicHudTriggersContext::interactableBlock),

    PLAYER_MOVING(DynamicHudTriggerCategory.MOVEMENT, DynamicHudTriggersContext::playerMoving),
    PLAYER_WALKING(DynamicHudTriggerCategory.MOVEMENT, DynamicHudTriggersContext::playerWalking),
    PLAYER_RUNNING(DynamicHudTriggerCategory.MOVEMENT, DynamicHudTriggersContext::playerRunning),
    PLAYER_SPRINTING(DynamicHudTriggerCategory.MOVEMENT, DynamicHudTriggersContext::playerSprinting),
    PLAYER_MOUNTING(DynamicHudTriggerCategory.MOVEMENT, DynamicHudTriggersContext::playerMounting),
    PLAYER_SWIMMING(DynamicHudTriggerCategory.MOVEMENT, DynamicHudTriggersContext::playerSwimming),
    PLAYER_FLYING(DynamicHudTriggerCategory.MOVEMENT, DynamicHudTriggersContext::playerFlying),
    PLAYER_GLIDING(DynamicHudTriggerCategory.MOVEMENT, DynamicHudTriggersContext::playerGliding),
    PLAYER_JUMPING(DynamicHudTriggerCategory.MOVEMENT, DynamicHudTriggersContext::playerJumping),
    PLAYER_CROUCHING(DynamicHudTriggerCategory.MOVEMENT, DynamicHudTriggersContext::playerCrouching),
    PLAYER_CLIMBING(DynamicHudTriggerCategory.MOVEMENT, DynamicHudTriggersContext::playerClimbing),
    PLAYER_FALLING(DynamicHudTriggerCategory.MOVEMENT, DynamicHudTriggersContext::playerFalling),
    PLAYER_ROLLING(DynamicHudTriggerCategory.MOVEMENT, DynamicHudTriggersContext::playerRolling),
    PLAYER_IDLE(DynamicHudTriggerCategory.MOVEMENT, DynamicHudTriggersContext::playerIdle),
    PLAYER_SITTING(DynamicHudTriggerCategory.MOVEMENT, DynamicHudTriggersContext::playerSitting),
    PLAYER_SLEEPING(DynamicHudTriggerCategory.MOVEMENT, DynamicHudTriggersContext::playerSleeping),
    PLAYER_IN_FLUID(DynamicHudTriggerCategory.MOVEMENT, DynamicHudTriggersContext::playerInFluid),
    PLAYER_ON_GROUND(DynamicHudTriggerCategory.MOVEMENT, DynamicHudTriggersContext::playerOnGround),

    HOLDING_RANGED_WEAPON(DynamicHudTriggerCategory.COMBAT, DynamicHudTriggersContext::holdingRangedWeapon),
    HOLDING_MELEE_WEAPON(DynamicHudTriggerCategory.COMBAT, DynamicHudTriggersContext::holdingMeleeWeapon),

    HEALTH_NOT_FULL(DynamicHudTriggerCategory.STATUS, _ -> true),
    STAMINA_NOT_FULL(DynamicHudTriggerCategory.STATUS, _ -> true),
    MANA_NOT_FULL(DynamicHudTriggerCategory.STATUS, _ -> true),
    OXYGEN_NOT_FULL(DynamicHudTriggerCategory.STATUS, _ -> true);

    private final DynamicHudTriggerCategory category;
    private final Predicate<DynamicHudTriggersContext> predicate;

    DynamicHudTriggers(
            DynamicHudTriggerCategory category,
            Predicate<DynamicHudTriggersContext> predicate
    ) {
        this.category = category;
        this.predicate = predicate;
    }

    public DynamicHudTriggerCategory category() {
        return category;
    }

    public boolean test(DynamicHudTriggersContext ctx) {
        return predicate.test(ctx);
    }

    public static DynamicHudTriggers fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim()
                .toUpperCase()
                .replace('-', '_')
                .replace(' ', '_');

        normalized = switch (normalized) {
            case "HEALTH_LOW", "HEALTH_CRITICAL" -> "HEALTH_NOT_FULL";
            case "STAMINA_LOW", "STAMINA_CRITICAL" -> "STAMINA_NOT_FULL";
            case "MANA_LOW", "MANA_CRITICAL" -> "MANA_NOT_FULL";
            default -> normalized;
        };

        try {
            return DynamicHudTriggers.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public static String prettyName(DynamicHudTriggers trigger) {
        return Arrays.stream(trigger.name().split("_"))
                .map(String::toUpperCase)
                .collect(Collectors.joining(" "));
    }

    public static List<DynamicHudTriggerCategory> displayCategoryOrder() {
        return List.of(
                DynamicHudTriggerCategory.INTERACTION,
                DynamicHudTriggerCategory.COMBAT,
                DynamicHudTriggerCategory.STATUS,
                DynamicHudTriggerCategory.MOVEMENT
        );
    }
}