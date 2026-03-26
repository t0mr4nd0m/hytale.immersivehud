package com.tom.immersivehudplugin.rules;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public enum DynamicHudTriggers {

    HEALTH_NOT_FULL(DynamicHudTriggerCategory.STATUS, ctx -> ctx.healthBar().isNotFull()),
    HEALTH_LOW(DynamicHudTriggerCategory.STATUS, ctx -> ctx.healthBar().isBelow(2f)),
    HEALTH_CRITICAL(DynamicHudTriggerCategory.STATUS, ctx -> ctx.healthBar().isBelow(4f)),
    HEALTH_FULL(DynamicHudTriggerCategory.STATUS, ctx -> ctx.healthBar().isFull()),
    STAMINA_NOT_FULL(DynamicHudTriggerCategory.STATUS, ctx -> ctx.staminaBar().isNotFull()),
    STAMINA_LOW(DynamicHudTriggerCategory.STATUS, ctx -> ctx.staminaBar().isBelow(2f)),
    STAMINA_CRITICAL(DynamicHudTriggerCategory.STATUS, ctx -> ctx.staminaBar().isBelow(4f)),
    STAMINA_FULL(DynamicHudTriggerCategory.STATUS, ctx -> ctx.staminaBar().isFull()),
    MANA_NOT_FULL(DynamicHudTriggerCategory.STATUS, ctx -> ctx.manaBar().isNotFull()),
    MANA_LOW(DynamicHudTriggerCategory.STATUS, ctx -> ctx.manaBar().isBelow(2f)),
    MANA_CRITICAL(DynamicHudTriggerCategory.STATUS, ctx -> ctx.manaBar().isBelow(4f)),
    MANA_FULL(DynamicHudTriggerCategory.STATUS, ctx -> ctx.manaBar().isFull()),

    HOTBAR_INPUT(DynamicHudTriggerCategory.INTERACTION, DynamicHudTriggersContext::hotbarInput),
    CONSUMABLE_USE(DynamicHudTriggerCategory.INTERACTION, DynamicHudTriggersContext::consumableUse),
    TARGET_ENTITY(DynamicHudTriggerCategory.INTERACTION, DynamicHudTriggersContext::targetEntity),
    INTERACTABLE_BLOCK(DynamicHudTriggerCategory.INTERACTION, DynamicHudTriggersContext::interactableBlock),

    HOLDING_RANGED_WEAPON(DynamicHudTriggerCategory.COMBAT, DynamicHudTriggersContext::holdingRangedWeapon),
    HOLDING_MELEE_WEAPON(DynamicHudTriggerCategory.COMBAT, DynamicHudTriggersContext::holdingMeleeWeapon),
    CHARGING_WEAPON(DynamicHudTriggerCategory.COMBAT, DynamicHudTriggersContext::chargingWeapon),

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
    PLAYER_IN_FLUID(DynamicHudTriggerCategory.MOVEMENT, DynamicHudTriggersContext::playerInFluid),
    PLAYER_ON_GROUND(DynamicHudTriggerCategory.MOVEMENT, DynamicHudTriggersContext::playerOnGround),
    PLAYER_FALLING(DynamicHudTriggerCategory.MOVEMENT, DynamicHudTriggersContext::playerFalling),
    PLAYER_SITTING(DynamicHudTriggerCategory.MOVEMENT, DynamicHudTriggersContext::playerSitting),
    PLAYER_ROLLING(DynamicHudTriggerCategory.MOVEMENT, DynamicHudTriggersContext::playerRolling);

    private final DynamicHudTriggerCategory category;
    private final Predicate<DynamicHudTriggersContext> predicate;
    private final long bit;

    DynamicHudTriggers(
            DynamicHudTriggerCategory category,
            Predicate<DynamicHudTriggersContext> predicate
    ) {
        this.category = category;
        this.predicate = predicate;
        this.bit = 1L << ordinal();
    }

    public DynamicHudTriggerCategory category() {
        return category;
    }

    public long bit() {
        return bit;
    }

    public boolean test(DynamicHudTriggersContext ctx) {
        return predicate.test(ctx);
    }

    public static long toMask(EnumSet<DynamicHudTriggers> rules) {
        long mask = 0L;
        for (DynamicHudTriggers trigger : rules) {
            mask |= trigger.bit();
        }
        return mask;
    }

    public static long activeMask(DynamicHudTriggersContext ctx, long requiredMask) {
        long mask = 0L;

        for (DynamicHudTriggers trigger : values()) {
            if ((requiredMask & trigger.bit()) == 0) {
                continue;
            }

            if (trigger.test(ctx)) {
                mask |= trigger.bit();
            }
        }

        return mask;
    }

    public static DynamicHudTriggers fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim()
                .toUpperCase()
                .replace('-', '_')
                .replace(' ', '_');

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
                DynamicHudTriggerCategory.MOVEMENT,
                DynamicHudTriggerCategory.SPECIAL
        );
    }
}