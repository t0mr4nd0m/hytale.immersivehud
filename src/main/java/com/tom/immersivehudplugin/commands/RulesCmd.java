package com.tom.immersivehudplugin.commands;

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
import com.tom.immersivehudplugin.commands.validation.AllowedValuesValidator;
import com.tom.immersivehudplugin.commands.validation.DynamicHudComponentValidator;
import com.tom.immersivehudplugin.config.DynamicHudRuleConfig;
import com.tom.immersivehudplugin.config.PlayerConfig;
import com.tom.immersivehudplugin.config.PlayerConfigService;
import com.tom.immersivehudplugin.hud.component.HudComponent;
import com.tom.immersivehudplugin.hud.component.HudComponentRegistry;
import com.tom.immersivehudplugin.hud.trigger.HudRuleSupport;
import com.tom.immersivehudplugin.hud.trigger.HudTrigger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.Color;
import java.util.EnumSet;

public final class RulesCmd extends AbstractPlayerCommand {

    private static final Color INFO_COLOR = Color.LIGHT_GRAY;
    private static final Color WARN_COLOR = Color.YELLOW;
    private static final Color ERROR_COLOR = Color.RED;
    private static final Color HIDE_COLOR = Color.RED;
    private static final Color SUCCESS_COLOR = Color.GREEN;

    public RulesCmd(PlayerConfigService playerConfigService) {
        super("rules", "List, add, remove, clear or configure dynamic HUD rules.");

        addUsageVariant(new TwoArgVariant(playerConfigService));
        addUsageVariant(new ThreeArgVariant(playerConfigService));
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
        context.sendMessage(Message.raw("Dynamic components: " + HudComponentRegistry.availableDynamicComponentsText()).color(INFO_COLOR));
        context.sendMessage(Message.raw("Rules: " + HudTrigger.availableRulesText()).color(INFO_COLOR));
    }

    private static final class TwoArgVariant extends AbstractPlayerCommand {
        private final PlayerConfigService playerConfigService;
        private final RequiredArg<String> componentArg;
        private final RequiredArg<String> actionArg;

        TwoArgVariant(PlayerConfigService playerConfigService) {
            super("List or clear rules");
            this.playerConfigService = playerConfigService;

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
                    playerConfigService,
                    playerRef,
                    componentArg.get(context),
                    context
            );
            if (resolved == null) {
                return;
            }

            String action = HudComponentRegistry.normalize(actionArg.get(context));

            if ("list".equals(action)) {
                sendListMessage(context, resolved);
                return;
            }

            if (resolved.rules().getRules().isEmpty()) {
                context.sendMessage(Message.raw(
                        resolved.messagePrefix() + " already has no rules."
                ).color(WARN_COLOR));
                return;
            }

            float resetThreshold = clearResolvedRules(playerConfigService, playerRef, resolved);

            if (resolved.supportsThreshold()) {
                context.sendMessage(Message.raw(
                        resolved.messagePrefix()
                                + " rules cleared. Threshold reset to "
                                + HudRuleSupport.formatThreshold(resetThreshold)
                                + "%."
                ).color(SUCCESS_COLOR));
            } else {
                context.sendMessage(Message.raw(
                        resolved.messagePrefix() + " rules cleared."
                ).color(SUCCESS_COLOR));
            }
        }
    }

    private static final class ThreeArgVariant extends AbstractPlayerCommand {
        private final PlayerConfigService playerConfigService;
        private final RequiredArg<String> componentArg;
        private final RequiredArg<String> actionArg;
        private final RequiredArg<String> valueArg;

        ThreeArgVariant(PlayerConfigService playerConfigService) {
            super("Add, remove rule or configure threshold");
            this.playerConfigService = playerConfigService;

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
                    playerConfigService,
                    playerRef,
                    componentArg.get(context),
                    context
            );
            if (resolved == null) {
                return;
            }

            String action = HudComponentRegistry.normalize(actionArg.get(context));
            String rawValue = valueArg.get(context);

            if ("threshold".equals(action)) {
                handleThresholdAction(
                        playerConfigService,
                        playerRef,
                        context,
                        resolved,
                        rawValue
                );
                return;
            }

            HudTrigger rule = HudTrigger.fromString(rawValue);
            if (rule == null) {
                context.sendMessage(Message.raw(
                        "Unknown rule: " + rawValue + ". Available rules: " + HudTrigger.availableRulesText()
                ).color(ERROR_COLOR));
                return;
            }

            if ("add".equals(action)) {
                handleAddAction(
                        playerConfigService,
                        playerRef,
                        context,
                        resolved,
                        rule
                );
                return;
            }

            handleRemoveAction(
                    playerConfigService,
                    playerRef,
                    context,
                    resolved,
                    rule
            );
        }
    }

    private record ResolvedRules(
            HudComponent entry,
            DynamicHudRuleConfig rules
    ) {
        String messagePrefix() {
            return "ImmersiveHud " + entry.label();
        }

        boolean supportsThreshold() {
            return entry.supportsThreshold();
        }

        float defaultThresholdOrDefault() {
            Float value = entry.defaultThreshold();
            return value != null ? value : DynamicHudRuleConfig.DEFAULT_THRESHOLD;
        }
    }

    private static RequiredArg<String> dynamicComponentArg(AbstractPlayerCommand cmd) {
        return cmd.withRequiredArg("component", "Dynamic HUD component", ArgTypes.STRING)
                .addValidator(new DynamicHudComponentValidator());
    }

    private static RequiredArg<String> genericValueArg(AbstractPlayerCommand cmd) {
        return cmd.withRequiredArg("value", "Rule or threshold", ArgTypes.STRING);
    }

    @SuppressWarnings("SameParameterValue")
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
            PlayerConfigService playerConfigService,
            PlayerRef playerRef,
            String rawComponent,
            CommandContext context
    ) {
        String componentKey = HudComponentRegistry.normalize(rawComponent);
        HudComponent entry = HudComponentRegistry.findDynamic(componentKey);

        if (entry == null) {
            context.sendMessage(Message.raw(
                    "Unknown dynamic HUD component: " + componentKey
            ).color(ERROR_COLOR));
            return null;
        }

        PlayerConfig playerCfg = playerConfigService.requirePlayerConfig(playerRef);
        if (playerCfg == null) {
            context.sendMessage(Message.raw(
                    "Failed to load your ImmersiveHud profile."
            ).color(ERROR_COLOR));
            return null;
        }

        DynamicHudRuleConfig rules = entry.getDynamicRuleConfig(playerCfg.getDynamicHud());
        if (rules == null) {
            context.sendMessage(Message.raw(
                    "No dynamic rules found for " + entry.label() + "."
            ).color(ERROR_COLOR));
            return null;
        }

        return new ResolvedRules(entry, rules);
    }

    private static void sendListMessage(
            CommandContext context,
            ResolvedRules resolved
    ) {
        String rulesText = HudRuleSupport.formatRules(resolved.rules().getRules());

        if (resolved.supportsThreshold()) {
            context.sendMessage(Message.raw(
                    resolved.messagePrefix()
                            + " rules: "
                            + rulesText
                            + " | threshold: "
                            + HudRuleSupport.formatThreshold(resolved.rules().getThreshold())
                            + "%"
            ).color(INFO_COLOR));
            return;
        }

        context.sendMessage(Message.raw(
                resolved.messagePrefix() + " rules: " + rulesText
        ).color(INFO_COLOR));
    }

    private static void handleThresholdAction(
            PlayerConfigService playerConfigService,
            PlayerRef playerRef,
            CommandContext context,
            ResolvedRules resolved,
            String rawValue
    ) {
        if (!resolved.supportsThreshold()) {
            context.sendMessage(Message.raw(
                    resolved.messagePrefix() + " does not support threshold."
            ).color(ERROR_COLOR));
            return;
        }

        Float threshold = HudRuleSupport.parseThreshold(rawValue);
        if (threshold == null) {
            context.sendMessage(Message.raw(
                    "Invalid threshold. Use a value between 0 and 100."
            ).color(ERROR_COLOR));
            return;
        }

        boolean hasActiveThresholdRule = setResolvedThreshold(
                playerConfigService,
                playerRef,
                resolved,
                threshold
        );

        if (!hasActiveThresholdRule) {
            context.sendMessage(Message.raw(
                    resolved.messagePrefix()
                            + " threshold set to "
                            + HudRuleSupport.formatThreshold(threshold)
                            + "%, but no threshold-based rule is currently active."
            ).color(WARN_COLOR));
            return;
        }

        context.sendMessage(Message.raw(
                resolved.messagePrefix()
                        + " threshold set to "
                        + HudRuleSupport.formatThreshold(threshold)
                        + "%."
        ).color(SUCCESS_COLOR));
    }

    private static void handleAddAction(
            PlayerConfigService playerConfigService,
            PlayerRef playerRef,
            CommandContext context,
            ResolvedRules resolved,
            HudTrigger rule
    ) {
        if (!resolved.entry().supportsRule(rule)) {
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

        persistResolvedRules(playerConfigService, playerRef, resolved);

        context.sendMessage(Message.join(
                Message.raw(resolved.messagePrefix() + " Added rule ").color(INFO_COLOR),
                Message.raw(rule.name()).color(SUCCESS_COLOR),
                Message.raw(" -> " + HudRuleSupport.formatRules(resolved.rules().getRules())).color(INFO_COLOR)
        ));
    }

    private static void handleRemoveAction(
            PlayerConfigService playerConfigService,
            PlayerRef playerRef,
            CommandContext context,
            ResolvedRules resolved,
            HudTrigger rule
    ) {
        boolean changed = resolved.rules().removeRule(rule);
        if (!changed) {
            context.sendMessage(Message.raw(
                    resolved.messagePrefix() + " Rule not present: " + rule.name()
            ).color(WARN_COLOR));
            return;
        }

        persistResolvedRules(playerConfigService, playerRef, resolved);

        context.sendMessage(Message.join(
                Message.raw(resolved.messagePrefix() + " Removed rule ").color(INFO_COLOR),
                Message.raw(rule.name()).color(HIDE_COLOR),
                Message.raw(" -> " + HudRuleSupport.formatRules(resolved.rules().getRules())).color(INFO_COLOR)
        ));
    }

    private static void persistResolvedRules(
            PlayerConfigService playerConfigService,
            PlayerRef playerRef,
            ResolvedRules resolved
    ) {
        playerConfigService.updateDynamicHud(playerRef, dynamicCfg -> {
            DynamicHudRuleConfig cfg = resolved.entry().getDynamicRuleConfig(dynamicCfg);
            if (cfg == null) {
                return;
            }

            cfg.setRules(EnumSet.copyOf(resolved.rules().getRules()));
            cfg.setThreshold(resolved.rules().getThreshold());
        });
    }

    private static float clearResolvedRules(
            PlayerConfigService playerConfigService,
            PlayerRef playerRef,
            ResolvedRules resolved
    ) {
        resolved.rules().setRules(EnumSet.noneOf(HudTrigger.class));

        float resetThreshold = resolved.defaultThresholdOrDefault();
        if (resolved.supportsThreshold()) {
            resolved.rules().setThreshold(resetThreshold);
        }

        persistResolvedRules(playerConfigService, playerRef, resolved);
        return resetThreshold;
    }

    private static boolean setResolvedThreshold(
            PlayerConfigService playerConfigService,
            PlayerRef playerRef,
            ResolvedRules resolved,
            float threshold
    ) {
        resolved.rules().setThreshold(threshold);
        persistResolvedRules(playerConfigService, playerRef, resolved);
        return resolved.rules().hasActiveThresholdRule();
    }
}