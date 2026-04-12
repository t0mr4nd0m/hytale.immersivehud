package com.tom.immersivehudplugin.commands;

import com.hypixel.hytale.codec.schema.SchemaContext;
import com.hypixel.hytale.codec.schema.config.Schema;
import com.hypixel.hytale.codec.validation.ValidationResults;
import com.hypixel.hytale.codec.validation.Validator;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.tom.immersivehudplugin.config.DynamicHudRuleConfig;
import com.tom.immersivehudplugin.config.PlayerConfig;
import com.tom.immersivehudplugin.registry.HudComponentRegistry;
import com.tom.immersivehudplugin.registry.HudEntry;
import com.tom.immersivehudplugin.registry.HudRuleCatalog;
import com.tom.immersivehudplugin.rules.DynamicHudTriggers;
import com.tom.immersivehudplugin.hud.HudSettingsService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.Color;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public final class RulesCmd extends AbstractPlayerCommand {

    private static final Color INFO_COLOR = Color.LIGHT_GRAY;
    private static final Color WARN_COLOR = Color.YELLOW;
    private static final Color ERROR_COLOR = Color.RED;
    private static final Color HIDE_COLOR = Color.RED;

    private final HudSettingsService hudSettingsService;

    public RulesCmd(HudSettingsService hudSettingsService) {
        super("rules", "List, add, remove, clear or configure dynamic HUD rules.");

        this.hudSettingsService = hudSettingsService;

        addUsageVariant(new TwoArgVariant(hudSettingsService));
        addUsageVariant(new ThreeArgVariant(hudSettingsService));
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    @Override
    protected void execute(
            @Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world
    ) {
        context.sendMessage(Message.raw("Usage: /ihud rules <component> list").color(WARN_COLOR));
        context.sendMessage(Message.raw("   or: /ihud rules <component> clear").color(WARN_COLOR));
        context.sendMessage(Message.raw("   or: /ihud rules <component> add <rule>").color(WARN_COLOR));
        context.sendMessage(Message.raw("   or: /ihud rules <component> remove <rule>").color(WARN_COLOR));
        context.sendMessage(Message.raw("   or: /ihud rules <component> threshold <0-100>").color(WARN_COLOR));
        context.sendMessage(Message.raw("Dynamic components: " + availableComponents()).color(INFO_COLOR));
        context.sendMessage(Message.raw("Rules: " + availableRules()).color(INFO_COLOR));
    }

    private static final class TwoArgVariant extends AbstractPlayerCommand {
        private final HudSettingsService hudSettingsService;
        private final RequiredArg<String> componentArg;
        private final RequiredArg<String> actionArg;

        TwoArgVariant(HudSettingsService hudSettingsService) {
            super("List or clear rules");
            this.hudSettingsService = hudSettingsService;

            this.componentArg = dynamicComponentArg(this);
            this.actionArg = allowedArg(this, "action", "list/clear", "list", "clear");
        }

        @Override
        protected boolean canGeneratePermission() {
            return false;
        }

        @Override
        protected void execute(
                @Nonnull CommandContext context,
                @Nonnull Store<EntityStore> store,
                @Nonnull Ref<EntityStore> ref,
                @Nonnull PlayerRef playerRef,
                @Nonnull World world
        ) {
            ResolvedRules resolved = resolveRules(
                    hudSettingsService,
                    playerRef,
                    componentArg.get(context),
                    context
            );
            if (resolved == null) {
                return;
            }

            String action = normalize(actionArg.get(context));

            if ("list".equals(action)) {
                String rulesText = formatRules(resolved.rules().getRules());

                if (resolved.entry().supportsThreshold()) {
                    context.sendMessage(Message.raw(
                            resolved.messagePrefix()
                                    + " rules: "
                                    + rulesText
                                    + " | threshold: "
                                    + formatThreshold(resolved.rules().getThreshold())
                                    + "%"
                    ).color(INFO_COLOR));
                } else {
                    context.sendMessage(Message.raw(
                            resolved.messagePrefix() + " rules: " + rulesText
                    ).color(INFO_COLOR));
                }
                return;
            }

            if (resolved.rules().getRules().isEmpty()) {
                context.sendMessage(Message.raw(resolved.messagePrefix() + " already has no rules.").color(WARN_COLOR));
                return;
            }

            hudSettingsService.updateDynamicHud(playerRef, dynamicCfg -> {
                DynamicHudRuleConfig cfg = resolved.entry().dynamicGetter().apply(dynamicCfg);
                cfg.setRules(EnumSet.noneOf(DynamicHudTriggers.class));

                if (resolved.entry().supportsThreshold()) {
                    Float defaultThreshold = resolved.entry().defaultThreshold();
                    cfg.setThreshold(defaultThreshold != null ? defaultThreshold : 100f);
                }
            });

            if (resolved.entry().supportsThreshold()) {
                Float defaultThreshold = resolved.entry().defaultThreshold();
                float resetValue = defaultThreshold != null ? defaultThreshold : 100f;

                context.sendMessage(Message.raw(
                        resolved.messagePrefix()
                                + " rules cleared. Threshold reset to "
                                + formatThreshold(resetValue)
                                + "%."
                ).color(Color.GREEN));
            } else {
                context.sendMessage(Message.raw(
                        resolved.messagePrefix() + " rules cleared."
                ).color(Color.GREEN));
            }
        }
    }

    private static final class ThreeArgVariant extends AbstractPlayerCommand {
        private final HudSettingsService hudSettingsService;
        private final RequiredArg<String> componentArg;
        private final RequiredArg<String> actionArg;
        private final RequiredArg<String> valueArg;

        ThreeArgVariant(HudSettingsService hudSettingsService) {
            super("Add, remove rule or configure threshold");
            this.hudSettingsService = hudSettingsService;

            this.componentArg = dynamicComponentArg(this);
            this.actionArg = allowedArg(this, "action", "add/remove/threshold", "add", "remove", "threshold");
            this.valueArg = genericValueArg(this);
        }

        @Override
        protected boolean canGeneratePermission() {
            return false;
        }

        @Override
        protected void execute(
                @Nonnull CommandContext context,
                @Nonnull Store<EntityStore> store,
                @Nonnull Ref<EntityStore> ref,
                @Nonnull PlayerRef playerRef,
                @Nonnull World world
        ) {
            ResolvedRules resolved = resolveRules(
                    hudSettingsService,
                    playerRef,
                    componentArg.get(context),
                    context
            );
            if (resolved == null) {
                return;
            }

            String action = normalize(actionArg.get(context));
            String rawValue = valueArg.get(context);

            if ("threshold".equals(action)) {
                if (!resolved.entry().supportsThreshold()) {
                    context.sendMessage(Message.raw(
                            resolved.messagePrefix() + " does not support threshold."
                    ).color(ERROR_COLOR));
                    return;
                }

                Float threshold = parseThreshold(rawValue);
                if (threshold == null) {
                    context.sendMessage(Message.raw(
                            "Invalid threshold. Use a value between 0 and 100."
                    ).color(ERROR_COLOR));
                    return;
                }

                hudSettingsService.updateDynamicHud(playerRef, dynamicCfg -> {
                    DynamicHudRuleConfig cfg = resolved.entry().dynamicGetter().apply(dynamicCfg);
                    cfg.setThreshold(threshold);
                });

                if (!hasMatchingThresholdRule(resolved.entry(), resolved.rules())) {
                    context.sendMessage(Message.raw(
                            resolved.messagePrefix()
                                    + " threshold set to "
                                    + formatThreshold(threshold)
                                    + "%, but no threshold-based rule is currently active."
                    ).color(WARN_COLOR));
                    return;
                }

                context.sendMessage(Message.raw(
                        resolved.messagePrefix()
                                + " threshold set to "
                                + formatThreshold(threshold)
                                + "%."
                ).color(Color.GREEN));
                return;
            }

            DynamicHudTriggers rule = DynamicHudTriggers.fromString(rawValue);
            if (rule == null) {
                context.sendMessage(Message.raw(
                        "Unknown rule: " + rawValue + ". Available rules: " + availableRules()
                ).color(ERROR_COLOR));
                return;
            }

            if ("add".equals(action)) {
                if (!HudRuleCatalog.supportsRule(resolved.entry(), rule)) {
                    context.sendMessage(Message.raw(
                            "Rule " + rule.name() + " is not valid for " + resolved.entry().label() + "."
                    ).color(ERROR_COLOR));
                    return;
                }

                boolean changed = resolved.rules().addRule(rule);
                if (!changed) {
                    context.sendMessage(Message.raw(
                            "Rule already present on " + resolved.entry().label() + ": " + rule.name()
                    ).color(WARN_COLOR));
                    return;
                }

                hudSettingsService.updateDynamicHud(playerRef, dynamicCfg -> {
                    DynamicHudRuleConfig cfg = resolved.entry().dynamicGetter().apply(dynamicCfg);
                    cfg.setRules(EnumSet.copyOf(resolved.rules().getRules()));
                    cfg.setThreshold(resolved.rules().getThreshold());
                });

                context.sendMessage(Message.join(
                        Message.raw(resolved.messagePrefix() + " Added rule ").color(INFO_COLOR),
                        Message.raw(rule.name()).color(Color.GREEN),
                        Message.raw(" -> " + formatRules(resolved.rules().getRules())).color(INFO_COLOR)
                ));
                return;
            }

            boolean changed = resolved.rules().removeRule(rule);
            if (!changed) {
                context.sendMessage(Message.raw(
                        resolved.messagePrefix() + " Rule not present: " + rule.name()
                ).color(WARN_COLOR));
                return;
            }

            hudSettingsService.updateDynamicHud(playerRef, dynamicCfg -> {
                DynamicHudRuleConfig cfg = resolved.entry().dynamicGetter().apply(dynamicCfg);
                cfg.setRules(EnumSet.copyOf(resolved.rules().getRules()));
                cfg.setThreshold(resolved.rules().getThreshold());
            });

            context.sendMessage(Message.join(
                    Message.raw(resolved.messagePrefix() + " Removed rule ").color(INFO_COLOR),
                    Message.raw(rule.name()).color(HIDE_COLOR),
                    Message.raw(" -> " + formatRules(resolved.rules().getRules())).color(INFO_COLOR)
            ));
        }
    }

    private record ResolvedRules(
            HudEntry entry,
            DynamicHudRuleConfig rules
    ) {
        String messagePrefix() {
            return "ImmersiveHud " + entry.label();
        }
    }

    private static RequiredArg<String> dynamicComponentArg(AbstractPlayerCommand cmd) {
        return cmd.withRequiredArg("component", "Dynamic HUD component", ArgTypes.STRING)
                .addValidator(new ManagedHudComponentValidator());
    }

    private static RequiredArg<String> genericValueArg(AbstractPlayerCommand cmd) {
        return cmd.withRequiredArg("value", "Rule or threshold", ArgTypes.STRING);
    }

    private static RequiredArg<String> allowedArg(
            AbstractPlayerCommand cmd,
            String name,
            String description,
            String... allowedValues
    ) {
        return cmd.withRequiredArg(name, description, ArgTypes.STRING)
                .addValidator(new AllowedValuesValidator(allowedValues));
    }

    @Nullable
    private static ResolvedRules resolveRules(
            HudSettingsService hudSettingsService,
            PlayerRef playerRef,
            String rawComponent,
            CommandContext context
    ) {
        String componentKey = HudComponentRegistry.normalize(rawComponent);
        var entry = HudComponentRegistry.findDynamic(componentKey);

        if (entry == null) {
            context.sendMessage(Message.raw(
                    "Unknown dynamic HUD component: " + componentKey
            ).color(ERROR_COLOR));
            return null;
        }

        PlayerConfig playerCfg = hudSettingsService.requirePlayerConfig(playerRef);
        if (playerCfg == null) {
            context.sendMessage(Message.raw("Failed to load your ImmersiveHud profile.").color(ERROR_COLOR));
            return null;
        }

        DynamicHudRuleConfig rules = entry.dynamicGetter().apply(playerCfg.getDynamicHud());
        if (rules == null) {
            context.sendMessage(Message.raw("No dynamic rules found for " + entry.label() + ".").color(ERROR_COLOR));
            return null;
        }

        return new ResolvedRules(entry, rules);
    }

    private static boolean hasMatchingThresholdRule(
            HudEntry entry,
            DynamicHudRuleConfig ruleCfg
    ) {
        return switch (entry.key()) {
            case "health" -> ruleCfg.getRules().contains(DynamicHudTriggers.HEALTH_NOT_FULL);
            case "stamina" -> ruleCfg.getRules().contains(DynamicHudTriggers.STAMINA_NOT_FULL);
            case "mana" -> ruleCfg.getRules().contains(DynamicHudTriggers.MANA_NOT_FULL);
            case "oxygen" -> ruleCfg.getRules().contains(DynamicHudTriggers.OXYGEN_NOT_FULL);
            default -> false;
        };
    }

    @Nullable
    private static Float parseThreshold(@Nullable String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        try {
            float value = Float.parseFloat(raw.trim());
            if (Float.isNaN(value) || Float.isInfinite(value)) {
                return null;
            }
            if (value < 0f || value > 100f) {
                return null;
            }
            return value;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String formatThreshold(float value) {
        if (value == (long) value) {
            return Long.toString((long) value);
        }
        return Float.toString(value);
    }

    private static String formatRules(EnumSet<DynamicHudTriggers> rules) {
        if (rules == null || rules.isEmpty()) {
            return "(none)";
        }

        return rules.stream()
                .map(Enum::name)
                .collect(Collectors.joining(", "));
    }

    private static String availableComponents() {
        return HudComponentRegistry.dynamicList().stream()
                .map(HudEntry::key)
                .sorted()
                .collect(Collectors.joining(", "));
    }

    private static String availableRules() {
        return Arrays.stream(DynamicHudTriggers.values())
                .map(rule -> rule.name().toLowerCase(Locale.ROOT))
                .collect(Collectors.joining(", "));
    }

    private static String normalize(@Nullable String s) {
        return HudComponentRegistry.normalize(s);
    }

    private static final class ManagedHudComponentValidator implements Validator<String> {
        @Override
        public void accept(@Nullable String input, @Nonnull ValidationResults results) {
            String key = HudComponentRegistry.normalize(input);
            if (HudComponentRegistry.findDynamic(key) == null) {
                results.fail("Unknown dynamic HUD component: " + input + ". Available components: " + availableComponents());
            }
        }

        @Override
        public void updateSchema(SchemaContext context, @Nonnull Schema target) {
        }
    }

    private static final class AllowedValuesValidator implements Validator<String> {
        private final Set<String> allowed;
        private final String display;

        AllowedValuesValidator(String... allowedValues) {
            this.allowed = Arrays.stream(allowedValues)
                    .map(v -> v.toLowerCase(Locale.ROOT))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            this.display = String.join(", ", allowedValues);
        }

        @Override
        public void accept(@Nullable String input, @Nonnull ValidationResults results) {
            String normalized = normalize(input);
            if (!allowed.contains(normalized)) {
                results.fail("Allowed values: " + display);
            }
        }

        @Override
        public void updateSchema(SchemaContext context, @Nonnull Schema target) {
        }
    }
}