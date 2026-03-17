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
import com.tom.immersivehudplugin.ImmersiveHudPlugin;
import com.tom.immersivehudplugin.config.DynamicHudRuleConfig;
import com.tom.immersivehudplugin.config.PlayerConfig;
import com.tom.immersivehudplugin.registry.HudComponentRegistry;
import com.tom.immersivehudplugin.rules.DynamicHudTriggers;

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
    
    public RulesCmd(ImmersiveHudPlugin plugin) {
        super("rules", "List, add, remove or clear dynamic HUD rules.");

        addUsageVariant(new TwoArgVariant(plugin));
        addUsageVariant(new ThreeArgVariant(plugin));
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
        context.sendMessage(Message.raw("Dynamic components: " + availableComponents()).color(INFO_COLOR));
        context.sendMessage(Message.raw("Rules: " + availableRules()).color(INFO_COLOR));
    }

    private static final class TwoArgVariant extends AbstractPlayerCommand {
        private final ImmersiveHudPlugin plugin;
        private final RequiredArg<String> componentArg;
        private final RequiredArg<String> actionArg;

        TwoArgVariant(ImmersiveHudPlugin plugin) {
            super("List or clear rules");
            this.plugin = plugin;

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
            ResolvedRules resolved = resolveRules(plugin, playerRef, componentArg.get(context), context);
            if (resolved == null) {
                return;
            }

            String action = normalize(actionArg.get(context));

            if ("list".equals(action)) {
                context.sendMessage(Message.raw(resolved.messagePrefix() + " rules: ").color(INFO_COLOR));
                context.sendMessage(Message.raw(formatRules(resolved.rules().getRules())).color(INFO_COLOR));
                return;
            }

            if (resolved.rules().getRules().isEmpty()) {
                context.sendMessage(Message.raw(resolved.messagePrefix() + " already has no rules.").color(WARN_COLOR));
                return;
            }

            resolved.rules().setRules(EnumSet.noneOf(DynamicHudTriggers.class));
            saveRulesChange(plugin, playerRef);

            context.sendMessage(Message.raw(resolved.messagePrefix() + " rules cleared.").color(Color.GREEN));
        }
    }

    private static final class ThreeArgVariant extends AbstractPlayerCommand {
        private final ImmersiveHudPlugin plugin;
        private final RequiredArg<String> componentArg;
        private final RequiredArg<String> actionArg;
        private final RequiredArg<String> ruleArg;

        ThreeArgVariant(ImmersiveHudPlugin plugin) {
            super("Add or remove rule");
            this.plugin = plugin;

            this.componentArg = dynamicComponentArg(this);
            this.actionArg = allowedArg(this, "action", "add/remove", "add", "remove");
            this.ruleArg = dynamicRuleArg(this);
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
            ResolvedRules resolved = resolveRules(plugin, playerRef, componentArg.get(context), context);
            if (resolved == null) {
                return;
            }

            String action = normalize(actionArg.get(context));
            DynamicHudTriggers rule = DynamicHudTriggers.fromString(ruleArg.get(context));
            if (rule == null) {
                return;
            }

            if ("add".equals(action)) {
                boolean changed = resolved.rules().addRule(rule);
                if (!changed) {
                    context.sendMessage(Message.raw(
                            "Rule already present on " + resolved.entry().label() + ": " + rule.name()
                    ).color(WARN_COLOR));
                    return;
                }

                saveRulesChange(plugin, playerRef);

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

            saveRulesChange(plugin, playerRef);

            context.sendMessage(Message.join(
                    Message.raw(resolved.messagePrefix() + " Removed rule ").color(INFO_COLOR),
                    Message.raw(rule.name()).color(HIDE_COLOR),
                    Message.raw(" -> " + formatRules(resolved.rules().getRules())).color(INFO_COLOR)
            ));
        }
    }

    private record ResolvedRules(
            HudComponentRegistry.HudEntry entry,
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

    private static RequiredArg<String> dynamicRuleArg(AbstractPlayerCommand cmd) {
        return cmd.withRequiredArg("rule", "Rule", ArgTypes.STRING)
                .addValidator(new DynamicHudRuleValidator());
    }

    private static RequiredArg<String> allowedArg(
            AbstractPlayerCommand cmd,
            @SuppressWarnings("SameParameterValue") String name,
            String description,
            String... allowedValues
    ) {
        return cmd.withRequiredArg(name, description, ArgTypes.STRING)
                .addValidator(new AllowedValuesValidator(allowedValues));
    }

    @Nullable
    private static ResolvedRules resolveRules(
            ImmersiveHudPlugin plugin,
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

        DynamicHudRuleConfig rules = resolveRulesConfig(
                plugin,
                playerRef,
                entry.label(),
                entry.dynamicGetter(),
                context
        );

        if (rules == null) {
            return null;
        }

        return new ResolvedRules(entry, rules);
    }

    private static void saveRulesChange(ImmersiveHudPlugin plugin, PlayerRef playerRef) {
        plugin.markPlayerConfigDirty(playerRef.getUuid());
        plugin.savePlayerConfigAsync(playerRef.getUuid());
    }

    @Nullable
    private static DynamicHudRuleConfig resolveRulesConfig(
            ImmersiveHudPlugin plugin,
            PlayerRef playerRef,
            String label,
            java.util.function.Function<com.tom.immersivehudplugin.config.DynamicHudConfig, DynamicHudRuleConfig> getter,
            CommandContext context
    ) {
        PlayerConfig playerCfg = plugin.requirePlayerConfig(playerRef);
        if (playerCfg == null) {
            context.sendMessage(Message.raw("Failed to load your ImmersiveHud profile.").color(ERROR_COLOR));
            return null;
        }

        DynamicHudRuleConfig rules = getter.apply(playerCfg.getDynamicHud());
        if (rules == null) {
            context.sendMessage(Message.raw("No dynamic rules found for " + label + ".").color(ERROR_COLOR));
            return null;
        }

        return rules;
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
                .map(HudComponentRegistry.HudEntry::key)
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

    private static final class DynamicHudRuleValidator implements Validator<String> {
        @Override
        public void accept(@Nullable String input, @Nonnull ValidationResults results) {
            if (DynamicHudTriggers.fromString(input) == null) {
                results.fail("Unknown rule: " + input + ". Available rules: " + availableRules());
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