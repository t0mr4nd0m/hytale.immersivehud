package com.tom.immersivehudplugin.rules;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public enum DynamicHudTriggers {

    HOTBAR_INPUT(Category.INTERACTION, DynamicHudTriggersContext::hotbarInput),
    CONSUMABLE_USE(Category.INTERACTION, DynamicHudTriggersContext::consumableUse),
    TARGET_ENTITY(Category.INTERACTION, DynamicHudTriggersContext::targetEntity),
    INTERACTABLE_BLOCK(Category.INTERACTION, DynamicHudTriggersContext::interactableBlock),

    PLAYER_MOVING(Category.MOVEMENT, DynamicHudTriggersContext::playerMoving),
    PLAYER_WALKING(Category.MOVEMENT, DynamicHudTriggersContext::playerWalking),
    PLAYER_RUNNING(Category.MOVEMENT, DynamicHudTriggersContext::playerRunning),
    PLAYER_SPRINTING(Category.MOVEMENT, DynamicHudTriggersContext::playerSprinting),
    PLAYER_MOUNTING(Category.MOVEMENT, DynamicHudTriggersContext::playerMounting),
    PLAYER_SWIMMING(Category.MOVEMENT, DynamicHudTriggersContext::playerSwimming),
    PLAYER_FLYING(Category.MOVEMENT, DynamicHudTriggersContext::playerFlying),
    PLAYER_GLIDING(Category.MOVEMENT, DynamicHudTriggersContext::playerGliding),
    PLAYER_JUMPING(Category.MOVEMENT, DynamicHudTriggersContext::playerJumping),
    PLAYER_CROUCHING(Category.MOVEMENT, DynamicHudTriggersContext::playerCrouching),
    PLAYER_CLIMBING(Category.MOVEMENT, DynamicHudTriggersContext::playerClimbing),
    PLAYER_FALLING(Category.MOVEMENT, DynamicHudTriggersContext::playerFalling),
    PLAYER_ROLLING(Category.MOVEMENT, DynamicHudTriggersContext::playerRolling),
    PLAYER_IDLE(Category.MOVEMENT, DynamicHudTriggersContext::playerIdle),
    PLAYER_SITTING(Category.MOVEMENT, DynamicHudTriggersContext::playerSitting),
    PLAYER_SLEEPING(Category.MOVEMENT, DynamicHudTriggersContext::playerSleeping),
    PLAYER_IN_FLUID(Category.MOVEMENT, DynamicHudTriggersContext::playerInFluid),
    PLAYER_ON_GROUND(Category.MOVEMENT, DynamicHudTriggersContext::playerOnGround),

    CHARGING_WEAPON(Category.COMBAT, DynamicHudTriggersContext::chargingWeapon),
    HOLDING_RANGED_WEAPON(Category.COMBAT, DynamicHudTriggersContext::holdingRangedWeapon),
    HOLDING_MELEE_WEAPON(Category.COMBAT, DynamicHudTriggersContext::holdingMeleeWeapon),

    HEALTH_NOT_FULL(Category.STATUS, _ -> true),
    STAMINA_NOT_FULL(Category.STATUS, _ -> true),
    MANA_NOT_FULL(Category.STATUS, _ -> true),
    OXYGEN_NOT_FULL(Category.STATUS, _ -> true);

    public enum Category {
        COMBAT      ("COMBAT"),
        INTERACTION ("INTERACTION"),
        MOVEMENT    ("MOVEMENT"),
        STATUS      ("STATUS");

        private final String label;

        Category(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    private final Category category;
    private final Predicate<DynamicHudTriggersContext> predicate;

    DynamicHudTriggers(
            Category category,
            Predicate<DynamicHudTriggersContext> predicate
    ) {
        this.category = category;
        this.predicate = predicate;
    }

    public Category category() {
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

    public static List<Category> displayCategoryOrder() {
        return List.of(
                Category.INTERACTION,
                Category.COMBAT,
                Category.STATUS,
                Category.MOVEMENT
        );
    }
}