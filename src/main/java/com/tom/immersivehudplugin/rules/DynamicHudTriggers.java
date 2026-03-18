package com.tom.immersivehudplugin.rules;

import java.util.EnumSet;
import java.util.function.Predicate;

public enum DynamicHudTriggers {

    HOTBAR_INPUT(DynamicHudTriggersContext::hotbarInput),
    CHARGING_WEAPON(DynamicHudTriggersContext::chargingWeapon),
    CONSUMABLE_USE(DynamicHudTriggersContext::consumableUse),
    TARGET_ENTITY(DynamicHudTriggersContext::targetEntity),
    INTERACTABLE_BLOCK(DynamicHudTriggersContext::interactableBlock),

    PLAYER_MOVING(DynamicHudTriggersContext::playerMoving),
    PLAYER_WALKING(DynamicHudTriggersContext::playerWalking),
    PLAYER_RUNNING(DynamicHudTriggersContext::playerRunning),
    PLAYER_SPRINTING(DynamicHudTriggersContext::playerSprinting),
    PLAYER_MOUNTING(DynamicHudTriggersContext::playerMounting),
    PLAYER_SWIMMING(DynamicHudTriggersContext::playerSwimming),

    HOLDING_RANGED_WEAPON(DynamicHudTriggersContext::holdingRangedWeapon),
    HOLDING_MELEE_WEAPON(DynamicHudTriggersContext::holdingMeleeWeapon),

    HEALTH_NOT_FULL(ctx -> ctx.healthBar().isNotFull()),
    HEALTH_LOW(ctx -> ctx.healthBar().isBelow(2f)),
    HEALTH_CRITICAL(ctx -> ctx.healthBar().isBelow(4f)),
    HEALTH_FULL(ctx -> ctx.healthBar().isFull()),

    STAMINA_NOT_FULL(ctx -> ctx.staminaBar().isNotFull()),
    STAMINA_LOW(ctx -> ctx.staminaBar().isBelow(2f)),
    STAMINA_CRITICAL(ctx -> ctx.staminaBar().isBelow(4f)),
    STAMINA_FULL(ctx -> ctx.staminaBar().isFull()),

    MANA_NOT_FULL(ctx -> ctx.manaBar().isNotFull()),
    MANA_LOW(ctx -> ctx.manaBar().isBelow(2f)),
    MANA_CRITICAL(ctx -> ctx.manaBar().isBelow(4f)),
    MANA_FULL(ctx -> ctx.manaBar().isFull());

    private final Predicate<DynamicHudTriggersContext> predicate;
    private final long bit;

    DynamicHudTriggers(Predicate<DynamicHudTriggersContext> predicate) {
        this.predicate = predicate;
        this.bit = 1L << ordinal();
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

    public static EnumSet<DynamicHudTriggers> parseCsv(String csv) {
        EnumSet<DynamicHudTriggers> set = EnumSet.noneOf(DynamicHudTriggers.class);

        if (csv == null || csv.isBlank()) {
            return set;
        }

        String[] parts = csv.split(",");

        for (String part : parts) {
            String name = part.trim().toUpperCase();
            if (name.isEmpty()) {
                continue;
            }

            try {
                set.add(DynamicHudTriggers.valueOf(name));
            } catch (IllegalArgumentException ignored) {
                // ignore invalid tokens
            }
        }

        return set;
    }

    public static String toCsv(EnumSet<DynamicHudTriggers> rules) {
        if (rules == null || rules.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        for (DynamicHudTriggers rule : rules) {
            if (!sb.isEmpty()) {
                sb.append(',');
            }
            sb.append(rule.name());
        }

        return sb.toString();
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
}