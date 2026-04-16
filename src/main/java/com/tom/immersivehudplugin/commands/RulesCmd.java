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
import com.tom.immersivehudplugin.runtime.HudRuntimeService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.Color;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

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
    private static final String ACTION_THRESHOLD = "threshold";
    private static final String ERROR_MESSAGE = "Unknown rules action";

    public RulesCmd(
            PlayerConfigService playerConfigService,
            HudRuntimeService hudRuntimeService
    ) {
        super("rules", "List, clear, add, remove, or configure dynamic HUD rules.");

        addUsageVariant(new ListClearVariant(playerConfigService, hudRuntimeService));
        addUsageVariant(new ActionValueVariant(playerConfigService, hudRuntimeService));
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
        private final HudRuntimeService hudRuntimeService;
        private final RequiredArg<String> componentArg;
        private final RequiredArg<String> actionArg;

        private static final Set<String> ACTIONS = Set.of(ACTION_LIST, ACTION_CLEAR);

        ListClearVariant(
                PlayerConfigService playerConfigService,
                HudRuntimeService hudRuntimeService
        ) {
            super("List or clear rules");
            this.playerConfigService = playerConfigService;
            this.hudRuntimeService = hudRuntimeService;

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
                case ACTION_LIST -> sendListMessage(playerConfigService, playerRef, context, resolved);
                case ACTION_CLEAR -> handleClearAction(playerConfigService, hudRuntimeService, playerRef, context, resolved);
                default -> sendUsage(context);
            }
        }
    }

    private static final class ActionValueVariant extends AbstractPlayerCommand {
        private final PlayerConfigService playerConfigService;
        private final HudRuntimeService hudRuntimeService;
        private final RequiredArg<String> componentArg;
        private final RequiredArg<String> actionArg;
        private final RequiredArg<String> valueArg;

        private static final Set<String> ACTIONS = Set.of(ACTION_ADD, ACTION_REMOVE, ACTION_THRESHOLD);

        ActionValueVariant(
                PlayerConfigService playerConfigService,
                HudRuntimeService hudRuntimeService
        ) {
            super("Add, remove, or configure threshold");
            this.playerConfigService = playerConfigService;
            this.hudRuntimeService = hudRuntimeService;

            this.componentArg = dynamicComponentArg(this);
            this.actionArg = withRequiredArg("action", String.join("/", ACTIONS), ArgTypes.STRING)
                    .addValidator(CommandValidators.validateArguments(ACTIONS, ERROR_MESSAGE));
            this.valueArg = withRequiredArg("value", "Rule or threshold", ArgTypes.STRING);
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
            String value = valueArg.get(context);

            switch (action) {
                case ACTION_ADD -> handleAddAction(playerConfigService, hudRuntimeService, playerRef, context, resolved, value);
                case ACTION_REMOVE -> handleRemoveAction(playerConfigService, hudRuntimeService, playerRef, context, resolved, value);
                case ACTION_THRESHOLD -> handleThresholdCommand(playerConfigService, hudRuntimeService, playerRef, context, resolved, value);
                default -> sendUsage(context);
            }
        }
    }

    private static void sendListMessage(
            PlayerConfigService playerConfigService,
            PlayerRef playerRef,
            CommandContext context,
            ResolvedRules resolved
    ) {
        DynamicHudRuleConfig rules = getCurrentRulesConfig(playerConfigService, playerRef, resolved);
        if (rules == null) {
            context.sendMessage(Message.raw(
                    "No dynamic rules found for " + resolved.entry().label() + "."
            ).color(ERROR_COLOR));
            return;
        }

        String rulesText = HudRuleSupport.formatRules(rules.getRules());

        if (resolved.supportsThreshold()) {
            context.sendMessage(Message.raw(
                    resolved.messagePrefix()
                            + " rules: "
                            + rulesText
                            + " | threshold: "
                            + HudRuleSupport.formatThreshold(rules.getThreshold())
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
            HudRuntimeService hudRuntimeService,
            PlayerRef playerRef,
            CommandContext context,
            ResolvedRules resolved
    ) {
        DynamicHudRuleConfig current = getCurrentRulesConfig(playerConfigService, playerRef, resolved);
        if (current == null) {
            context.sendMessage(Message.raw(
                    "No dynamic rules found for " + resolved.entry().label() + "."
            ).color(ERROR_COLOR));
            return;
        }

        if (current.getRules().isEmpty()) {
            context.sendMessage(Message.raw(
                    resolved.messagePrefix() + " already has no rules."
            ).color(WARN_COLOR));
            return;
        }

        float resetThreshold = resolved.defaultThresholdOrDefault();

        RuleMutationResult result = mutateRules(
                playerConfigService,
                hudRuntimeService,
                playerRef,
                resolved,
                cfg -> {
                    cfg.setRules(EnumSet.noneOf(HudTrigger.class));
                    if (resolved.supportsThreshold()) {
                        cfg.setThreshold(resetThreshold);
                    }
                    return snapshotResult(cfg, true);
                }
        );

        if (result == null) {
            context.sendMessage(Message.raw(
                    "Failed to update dynamic rules for " + resolved.entry().label() + "."
            ).color(ERROR_COLOR));
            return;
        }

        if (resolved.supportsThreshold()) {
            context.sendMessage(Message.raw(
                    resolved.messagePrefix()
                            + " rules cleared. Threshold reset to "
                            + HudRuleSupport.formatThreshold(result.threshold())
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
            HudRuntimeService hudRuntimeService,
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

        RuleMutationResult result = mutateRules(
                playerConfigService,
                hudRuntimeService,
                playerRef,
                resolved,
                cfg -> {
                    boolean changed = Float.compare(cfg.getThreshold(), threshold) != 0;
                    cfg.setThreshold(threshold);
                    return snapshotResult(cfg, changed);
                }
        );

        if (result == null) {
            context.sendMessage(Message.raw(
                    "Failed to update dynamic rules for " + resolved.entry().label() + "."
            ).color(ERROR_COLOR));
            return;
        }

        if (!result.changed()) {
            context.sendMessage(Message.raw(
                    resolved.messagePrefix()
                            + " threshold already set to "
                            + HudRuleSupport.formatThreshold(result.threshold())
                            + "%."
            ).color(WARN_COLOR));
            return;
        }

        if (!result.hasActiveThresholdRule()) {
            context.sendMessage(Message.raw(
                    resolved.messagePrefix()
                            + " threshold set to "
                            + HudRuleSupport.formatThreshold(result.threshold())
                            + "%, but no threshold-based rule is currently active."
            ).color(WARN_COLOR));
            return;
        }

        context.sendMessage(Message.raw(
                resolved.messagePrefix()
                        + " threshold set to "
                        + HudRuleSupport.formatThreshold(result.threshold())
                        + "%."
        ).color(SUCCESS_COLOR));
    }

    private static void handleAddAction(
            PlayerConfigService playerConfigService,
            HudRuntimeService hudRuntimeService,
            PlayerRef playerRef,
            CommandContext context,
            ResolvedRules resolved,
            String rawValue
    ) {
        handleRuleToggleAction(
                playerConfigService,
                hudRuntimeService,
                playerRef,
                context,
                resolved,
                rawValue,
                true,
                cfgRule -> cfgRule.addRule(parseRuleUnchecked(rawValue)),
                "added",
                SUCCESS_COLOR,
                "rule already present"
        );
    }

    private static void handleRemoveAction(
            PlayerConfigService playerConfigService,
            HudRuntimeService hudRuntimeService,
            PlayerRef playerRef,
            CommandContext context,
            ResolvedRules resolved,
            String rawValue
    ) {
        handleRuleToggleAction(
                playerConfigService,
                hudRuntimeService,
                playerRef,
                context,
                resolved,
                rawValue,
                false,
                cfgRule -> cfgRule.removeRule(parseRuleUnchecked(rawValue)),
                "removed",
                REMOVE_COLOR,
                "rule not present"
        );
    }

    private static void handleRuleToggleAction(
            PlayerConfigService playerConfigService,
            HudRuntimeService hudRuntimeService,
            PlayerRef playerRef,
            CommandContext context,
            ResolvedRules resolved,
            String rawValue,
            boolean validateSupportedRule,
            Function<DynamicHudRuleConfig, Boolean> mutation,
            String successVerb,
            Color ruleColor,
            String unchangedMessage
    ) {
        HudTrigger rule = parseRuleOrSendError(context, rawValue);
        if (rule == null) {
            return;
        }

        if (validateSupportedRule && !resolved.entry().supportsRule(rule)) {
            context.sendMessage(Message.raw(
                    "Rule " + rule.name() + " is not valid for " + resolved.entry().label() + "."
            ).color(ERROR_COLOR));
            return;
        }

        RuleMutationResult result = mutateRules(
                playerConfigService,
                hudRuntimeService,
                playerRef,
                resolved,
                cfg -> snapshotResult(cfg, Boolean.TRUE.equals(mutation.apply(cfg)))
        );

        if (result == null) {
            context.sendMessage(Message.raw(
                    "Failed to update dynamic rules for " + resolved.entry().label() + "."
            ).color(ERROR_COLOR));
            return;
        }

        if (!result.changed()) {
            context.sendMessage(Message.raw(
                    resolved.messagePrefix() + " " + unchangedMessage + ": " + rule.name()
            ).color(WARN_COLOR));
            return;
        }

        context.sendMessage(Message.join(
                Message.raw(resolved.messagePrefix() + " " + successVerb + " rule ").color(INFO_COLOR),
                Message.raw(rule.name()).color(ruleColor),
                Message.raw(" -> " + result.rulesText()).color(INFO_COLOR)
        ));
    }

    private static RequiredArg<String> dynamicComponentArg(AbstractPlayerCommand cmd) {
        return cmd.withRequiredArg("component", "Dynamic HUD component", ArgTypes.STRING)
                .addValidator(CommandValidators.dynamicComponent());
    }

    private record ResolvedRules(HudComponent entry) {
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

    private record RuleMutationResult(
            boolean changed,
            String rulesText,
            float threshold,
            boolean hasActiveThresholdRule
    ) {}

    @Nullable
    private static HudTrigger parseRuleOrSendError(CommandContext context, String rawValue) {
        HudTrigger result = HudTrigger.fromString(rawValue);
        if (result == null) {
            context.sendMessage(Message.raw(
                    "Unknown rule: " + rawValue + ". Available rules: " + HudTrigger.availableRulesText()
            ).color(ERROR_COLOR));
        }
        return result;
    }

    @Nullable
    private static ResolvedRules resolveRules(
            PlayerConfigService playerConfigService,
            PlayerRef playerRef,
            String rawComponent,
            CommandContext context
    ) {
        HudComponent component = HudComponentRegistry.findDynamic(rawComponent);
        if (component == null) {
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

        DynamicHudRuleConfig rules = component.getDynamicRuleConfig(playerCfg.getDynamicHud());
        if (rules == null) {
            context.sendMessage(Message.raw(
                    "No dynamic rules found for " + component.label() + "."
            ).color(ERROR_COLOR));
            return null;
        }

        return new ResolvedRules(component);
    }

    @Nullable
    private static DynamicHudRuleConfig getCurrentRulesConfig(
            PlayerConfigService playerConfigService,
            PlayerRef playerRef,
            ResolvedRules resolved
    ) {
        PlayerConfig playerCfg = playerConfigService.requirePlayerConfig(playerRef);
        if (playerCfg == null) {
            return null;
        }

        return resolved.entry().getDynamicRuleConfig(playerCfg.getDynamicHud());
    }

    private static HudTrigger parseRuleUnchecked(String rawValue) {
        HudTrigger rule = HudTrigger.fromString(rawValue);
        if (rule == null) {
            throw new IllegalStateException("Expected valid rule: " + rawValue);
        }
        return rule;
    }

    private static RuleMutationResult snapshotResult(
            DynamicHudRuleConfig cfg,
            boolean changed
    ) {
        return new RuleMutationResult(
                changed,
                HudRuleSupport.formatRules(cfg.getRules()),
                cfg.getThreshold(),
                cfg.hasActiveThresholdRule()
        );
    }

    @Nullable
    private static RuleMutationResult mutateRules(
            PlayerConfigService playerConfigService,
            HudRuntimeService hudRuntimeService,
            PlayerRef playerRef,
            ResolvedRules resolved,
            Function<DynamicHudRuleConfig, RuleMutationResult> mutator
    ) {
        AtomicReference<RuleMutationResult> resultRef = new AtomicReference<>();

        playerConfigService.updateDynamicHud(playerRef, dynamicCfg -> {
            DynamicHudRuleConfig cfg = resolved.entry().getDynamicRuleConfig(dynamicCfg);
            if (cfg == null) {
                return;
            }

            RuleMutationResult result = mutator.apply(cfg);
            if (result != null) {
                resultRef.set(result);
            }
        });

        RuleMutationResult result = resultRef.get();
        if (result != null && result.changed()) {
            hudRuntimeService.onPlayerConfigChanged(playerRef);
        }

        return result;
    }
}