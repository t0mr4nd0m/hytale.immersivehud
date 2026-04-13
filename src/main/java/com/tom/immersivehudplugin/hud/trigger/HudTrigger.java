package com.tom.immersivehudplugin.hud.trigger;

import com.tom.immersivehudplugin.config.DynamicHudRuleConfig;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public enum HudTrigger {

    HOTBAR_INPUT            (Category.INTERACTION, Source.SIGNAL),
    CONSUMABLE_USE          (Category.INTERACTION, Source.SIGNAL),
    TARGET_ENTITY           (Category.INTERACTION, Source.SIGNAL),
    INTERACTABLE_BLOCK      (Category.INTERACTION, Source.SIGNAL),

    PLAYER_MOVING           (Category.MOVEMENT, Source.SIGNAL),
    PLAYER_WALKING          (Category.MOVEMENT, Source.SIGNAL),
    PLAYER_RUNNING          (Category.MOVEMENT, Source.SIGNAL),
    PLAYER_SPRINTING        (Category.MOVEMENT, Source.SIGNAL),
    PLAYER_MOUNTING         (Category.MOVEMENT, Source.SIGNAL),
    PLAYER_SWIMMING         (Category.MOVEMENT, Source.SIGNAL),
    PLAYER_FLYING           (Category.MOVEMENT, Source.SIGNAL),
    PLAYER_GLIDING          (Category.MOVEMENT, Source.SIGNAL),
    PLAYER_JUMPING          (Category.MOVEMENT, Source.SIGNAL),
    PLAYER_CROUCHING        (Category.MOVEMENT, Source.SIGNAL),
    PLAYER_CLIMBING         (Category.MOVEMENT, Source.SIGNAL),
    PLAYER_FALLING          (Category.MOVEMENT, Source.SIGNAL),
    PLAYER_ROLLING          (Category.MOVEMENT, Source.SIGNAL),
    PLAYER_IDLE             (Category.MOVEMENT, Source.SIGNAL),
    PLAYER_SITTING          (Category.MOVEMENT, Source.SIGNAL),
    PLAYER_SLEEPING         (Category.MOVEMENT, Source.SIGNAL),
    PLAYER_IN_FLUID         (Category.MOVEMENT, Source.SIGNAL),
    PLAYER_ON_GROUND        (Category.MOVEMENT, Source.SIGNAL),

    CHARGING_WEAPON         (Category.COMBAT, Source.SIGNAL),
    HOLDING_RANGED_WEAPON   (Category.COMBAT, Source.SIGNAL),
    HOLDING_MELEE_WEAPON    (Category.COMBAT, Source.SIGNAL),
    BLOCKING_ATTACK         (Category.COMBAT, Source.SIGNAL),

    HEALTH_NOT_FULL         (Category.HUD_BAR , Source.HUD_BAR),
    STAMINA_NOT_FULL        (Category.HUD_BAR , Source.HUD_BAR),
    MANA_NOT_FULL           (Category.HUD_BAR , Source.HUD_BAR),
    OXYGEN_NOT_FULL         (Category.HUD_BAR , Source.HUD_BAR);

    public enum Category {
        COMBAT      ("COMBAT"),
        INTERACTION ("INTERACTION"),
        MOVEMENT    ("MOVEMENT"),
        HUD_BAR      ("HUD BAR");

        private final String label;

        Category(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    public enum Source {
        SIGNAL,
        HUD_BAR
    }

    private final Category category;
    private final Source source;

    HudTrigger(Category category, Source source) {
        this.category = category;
        this.source = source;
    }

    public Category category() {
        return category;
    }

    public Source source() {
        return source;
    }

    public boolean matches(DynamicHudRuleConfig ruleConfig, HudTriggerContext ctx) {
        if (ctx == null || ruleConfig == null) {
            return false;
        }

        return switch (this) {
            case HEALTH_NOT_FULL    -> ctx.healthBar().isBelowPercent(ruleConfig.getThreshold());
            case STAMINA_NOT_FULL   -> ctx.staminaBar().isBelowPercent(ruleConfig.getThreshold());
            case MANA_NOT_FULL      -> ctx.manaBar().isBelowPercent(ruleConfig.getThreshold());
            case OXYGEN_NOT_FULL    -> ctx.oxygenBar().isBelowPercent(ruleConfig.getThreshold());
            default                 -> source == Source.SIGNAL && matchesSignal(ctx);
        };
    }

    public boolean matchesSignal(HudTriggerContext ctx) {
        return ctx != null && source == Source.SIGNAL && ctx.active(this);
    }

    public static HudTrigger fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim()
                .toUpperCase()
                .replace('-', '_')
                .replace(' ', '_');

        try {
            return HudTrigger.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public static String prettyName(HudTrigger trigger) {
        return Arrays.stream(trigger.name().split("_"))
                .map(String::toUpperCase)
                .collect(Collectors.joining(" "));
    }

    public static List<Category> displayCategoryOrder() {
        return List.of(
            Category.INTERACTION,
            Category.COMBAT,
            Category.HUD_BAR,
            Category.MOVEMENT
        );
    }

    public static String availableRulesText() {
        return Arrays.stream(values())
                .map(rule -> rule.name().toLowerCase(Locale.ROOT))
                .collect(Collectors.joining(", "));
    }
}