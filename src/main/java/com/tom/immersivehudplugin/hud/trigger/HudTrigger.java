package com.tom.immersivehudplugin.hud.trigger;

import com.tom.immersivehudplugin.config.DynamicHudRuleConfig;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public enum HudTrigger {

    HOTBAR_INPUT            (Category.INTERACTION, Source.SIGNAL, false),
    CONSUMABLE_USE          (Category.INTERACTION, Source.SIGNAL, false),
    TARGET_ENTITY           (Category.INTERACTION, Source.SIGNAL, false),
    INTERACTABLE_BLOCK      (Category.INTERACTION, Source.SIGNAL, false),

    PLAYER_MOVING           (Category.MOVEMENT, Source.SIGNAL, false),
    PLAYER_WALKING          (Category.MOVEMENT, Source.SIGNAL, false),
    PLAYER_RUNNING          (Category.MOVEMENT, Source.SIGNAL, false),
    PLAYER_SPRINTING        (Category.MOVEMENT, Source.SIGNAL, false),
    PLAYER_MOUNTING         (Category.MOVEMENT, Source.SIGNAL, false),
    PLAYER_SWIMMING         (Category.MOVEMENT, Source.SIGNAL, false),
    PLAYER_FLYING           (Category.MOVEMENT, Source.SIGNAL, false),
    PLAYER_GLIDING          (Category.MOVEMENT, Source.SIGNAL, false),
    PLAYER_JUMPING          (Category.MOVEMENT, Source.SIGNAL, false),
    PLAYER_CROUCHING        (Category.MOVEMENT, Source.SIGNAL, false),
    PLAYER_CLIMBING         (Category.MOVEMENT, Source.SIGNAL, false),
    PLAYER_FALLING          (Category.MOVEMENT, Source.SIGNAL, false),
    PLAYER_ROLLING          (Category.MOVEMENT, Source.SIGNAL, false),
    PLAYER_IDLE             (Category.MOVEMENT, Source.SIGNAL, false),
    PLAYER_SITTING          (Category.MOVEMENT, Source.SIGNAL, false),
    PLAYER_SLEEPING         (Category.MOVEMENT, Source.SIGNAL, false),
    PLAYER_IN_FLUID         (Category.MOVEMENT, Source.SIGNAL, false),
    PLAYER_ON_GROUND        (Category.MOVEMENT, Source.SIGNAL, false),

    CHARGING_WEAPON         (Category.COMBAT, Source.SIGNAL, false),
    HOLDING_RANGED_WEAPON   (Category.COMBAT, Source.SIGNAL, false),
    HOLDING_MELEE_WEAPON    (Category.COMBAT, Source.SIGNAL, false),
    BLOCKING_ATTACK         (Category.COMBAT, Source.SIGNAL, false),

    HEALTH_NOT_FULL         (Category.HUD_BAR , Source.HUD_BAR, true),
    STAMINA_NOT_FULL        (Category.HUD_BAR , Source.HUD_BAR, true),
    MANA_NOT_FULL           (Category.HUD_BAR , Source.HUD_BAR, true),
    OXYGEN_NOT_FULL         (Category.HUD_BAR , Source.HUD_BAR, true);

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
    private final boolean usesThreshold;

    HudTrigger(Category category, Source source, Boolean usesThreshold) {
        this.category = category;
        this.source = source;
        this.usesThreshold = usesThreshold;
    }

    public Category category() {
        return category;
    }

    public Source source() {
        return source;
    }

    public boolean usesThreshold() {
        return usesThreshold;
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

    public static String displayNameUpper(HudTrigger trigger) {
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
                .map(HudTrigger::key)
                .collect(Collectors.joining(", "));
    }

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }
}