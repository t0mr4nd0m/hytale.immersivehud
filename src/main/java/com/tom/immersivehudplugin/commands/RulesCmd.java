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
import com.tom.immersivehudplugin.commands.validation.CommandValidators;
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
import java.util.Set;

public final class RulesCmd extends AbstractPlayerCommand {

    private static final Color INFO_COLOR = Color.LIGHT_GRAY;
    private static final Color WARN_COLOR = Color.YELLOW;
    private static final Color ERROR_COLOR = Color.RED;
    private static final Color REMOVE_COLOR = Color.RED;
    private static final Color SUCCESS_COLOR = Color.GREEN;

    private static final String ACTION_LIST = "list";
    private static final String ACTION_CLEAR = "clear";
    private static final String ACTION_ADD = "add";
    private static final String ACTION_REMOVE = "remove";
    private static final String ERROR_MESSAGE = "Unknown rules action";

    public RulesCmd(PlayerConfigService playerConfigService) {
        super("rules", "List, clear, add, remove, or configure dynamic HUD rules.");

        addUsageVariant(new ListClearVariant(playerConfigService));
        addUsageVariant(new RuleMutationVariant(playerConfigService));
        addUsageVariant(new ThresholdVariant(playerConfigService));
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
        sendUsage(context);
    }

    private static void sendUsage(@Nonnull CommandContext context) {
        context.sendMessage(Message.raw("Usage: /ihud rules <component> list").color(WARN_COLOR));
        context.sendMessage(Message.raw("   or: /ihud rules <component> clear").color(WARN_COLOR));
        context.sendMessage(Message.raw("   or: /ihud rules <component> add <rule>").color(WARN_COLOR));
        context.sendMessage(Message.raw("   or: /ihud rules <component> remove <rule>").color(WARN_COLOR));
        context.sendMessage(Message.raw("   or: /ihud rules <component> threshold <0-100>").color(WARN_COLOR));
        context.sendMessage(Message.raw("Dynamic components: " + HudComponentRegistry.availableDynamicComponentsText()).color(INFO_COLOR));
        context.sendMessage(Message.raw("Rules: " + HudTrigger.availableRulesText()).color(INFO_COLOR));
    }

    private static final class ListClearVariant extends AbstractPlayerCommand {
        private final PlayerConfigService playerConfigService;
        private final RequiredArg<String> componentArg;
        private final RequiredArg<String> actionArg;

        private final static Set<String> ACTIONS = Set.of("list", "clear");

        ListClearVariant(PlayerConfigService playerConfigService) {
            super("List or clear rules");
            this.playerConfigService = playerConfigService;

            this.componentArg = dynamicComponentArg(this);
            this.actionArg = withRequiredArg("action", String.join("/", ACTIONS), ArgTypes.STRING)
                    .addValidator(CommandValidators.validateArguments(ACTIONS, ERROR_MESSAGE));
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

            switch (action) {
                case ACTION_LIST -> sendListMessage(context, resolved);
                case ACTION_CLEAR -> handleClearAction(playerConfigService, playerRef, context, resolved);
                default -> sendUsage(context);
            }
        }
    }

    private static final class RuleMutationVariant extends AbstractPlayerCommand {
        private final PlayerConfigService playerConfigService;
        private final RequiredArg<String> componentArg;
        private final RequiredArg<String> actionArg;
        private final RequiredArg<String> ruleArg;

        private static final Set<String> ACTIONS = Set.of("add", "remove");

        RuleMutationVariant(PlayerConfigService playerConfigService) {
            super("Add or remove a dynamic rule");
            this.playerConfigService = playerConfigService;

            this.componentArg = dynamicComponentArg(this);
            this.actionArg = withRequiredArg("action", String.join("/", ACTIONS), ArgTypes.STRING)
                    .addValidator(CommandValidators.validateArguments(ACTIONS, ERROR_MESSAGE));
            this.ruleArg = ruleArg(this);
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
            String rawRule = ruleArg.get(context);

            switch (action) {
                case ACTION_ADD -> handleAddAction(playerConfigService, playerRef, context, resolved, rawRule);
                case ACTION_REMOVE -> handleRemoveAction(playerConfigService, playerRef, context, resolved, rawRule);
                default -> sendUsage(context);
            }
        }
    }

    private static final class ThresholdVariant extends AbstractPlayerCommand {
        private final PlayerConfigService playerConfigService;
        private final RequiredArg<String> componentArg;
        private final RequiredArg<String> actionArg;
        private final RequiredArg<String> thresholdArg;

        private final static Set<String> ACTIONS = Set.of("threshold");

        ThresholdVariant(PlayerConfigService playerConfigService) {
            super("Configure threshold");
            this.playerConfigService = playerConfigService;

            this.componentArg = dynamicComponentArg(this);
            this.actionArg = withRequiredArg("action", String.join("/", ACTIONS), ArgTypes.STRING)
                    .addValidator(CommandValidators.validateArguments(ACTIONS, ERROR_MESSAGE));
            this.thresholdArg = thresholdArg(this);
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
            if (resolved == null) { return; }

            handleThresholdCommand(
                    playerConfigService,
                    playerRef,
                    context,
                    resolved,
                    thresholdArg.get(context)
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
                .addValidator(CommandValidators.dynamicComponent());
    }

    private static RequiredArg<String> ruleArg(AbstractPlayerCommand cmd) {
        return cmd.withRequiredArg("rule", "Dynamic rule", ArgTypes.STRING)
                .addValidator(CommandValidators.rule());
    }

    private static RequiredArg<String> thresholdArg(AbstractPlayerCommand cmd) {
        return cmd.withRequiredArg("threshold", "Threshold (0-100)", ArgTypes.STRING);
    }

    @Nullable
    private static ResolvedRules resolveRules(
            PlayerConfigService playerConfigService,
            PlayerRef playerRef,
            String rawComponent,
            CommandContext context
    ) {
        HudComponent entry = HudComponentRegistry.findDynamic(rawComponent);

        if (entry == null) {
            context.sendMessage(Message.raw(
                    "Unknown dynamic HUD component: " + rawComponent
            ).color(ERROR_COLOR));
            return null;
        }

        PlayerConfig playerCfg = playerConfigService.requirePlayerConfig(playerRef);
        if (playerCfg == null) {
            context.sendMessage(Message.raw(
                    "Failed to load your ImmersiveHud configuration."
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

    private static void handleClearAction(
            PlayerConfigService playerConfigService,
            PlayerRef playerRef,
            CommandContext context,
            ResolvedRules resolved
    ) {
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
            return;
        }

        context.sendMessage(Message.raw(
                resolved.messagePrefix() + " rules cleared."
        ).color(SUCCESS_COLOR));
    }

    private static void handleThresholdCommand(
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

    private static @Nullable HudTrigger parseRuleOrSendError(CommandContext context, String rawValue) {
        HudTrigger result = HudTrigger.fromString(rawValue);
        if (result == null) {
            context.sendMessage(Message.raw(
                    "Unknown rule: " + rawValue + ". Available rules: " + HudTrigger.availableRulesText()
            ).color(ERROR_COLOR));
        }
        return result;
    }

    private static void handleAddAction(
            PlayerConfigService playerConfigService,
            PlayerRef playerRef,
            CommandContext context,
            ResolvedRules resolved,
            String rawValue
    ) {
        HudTrigger rule = parseRuleOrSendError(context, rawValue);
        if (rule == null) {
            return;
        }

        if (!resolved.entry().supportsRule(rule)) {
            context.sendMessage(Message.raw(
                    "Rule " + rule.name() + " is not valid for " + resolved.entry().label() + "."
            ).color(ERROR_COLOR));
            return;
        }

        boolean changed = resolved.rules().addRule(rule);
        if (!changed) {
            context.sendMessage(Message.raw(
                    resolved.messagePrefix() + " rule already present: " + rule.name()
            ).color(WARN_COLOR));
            return;
        }

        persistResolvedRules(playerConfigService, playerRef, resolved);

        context.sendMessage(Message.join(
                Message.raw(resolved.messagePrefix() + " added rule ").color(INFO_COLOR),
                Message.raw(rule.name()).color(SUCCESS_COLOR),
                Message.raw(" -> " + HudRuleSupport.formatRules(resolved.rules().getRules())).color(INFO_COLOR)
        ));
    }

    private static void handleRemoveAction(
            PlayerConfigService playerConfigService,
            PlayerRef playerRef,
            CommandContext context,
            ResolvedRules resolved,
            String rawValue
    ) {
        HudTrigger rule = parseRuleOrSendError(context, rawValue);
        if (rule == null) {
            return;
        }

        boolean changed = resolved.rules().removeRule(rule);
        if (!changed) {
            context.sendMessage(Message.raw(
                    resolved.messagePrefix() + " rule not present: " + rule.name()
            ).color(WARN_COLOR));
            return;
        }

        persistResolvedRules(playerConfigService, playerRef, resolved);

        context.sendMessage(Message.join(
                Message.raw(resolved.messagePrefix() + " removed rule ").color(INFO_COLOR),
                Message.raw(rule.name()).color(REMOVE_COLOR),
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