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
import com.tom.immersivehudplugin.config.HudComponentsConfig;
import com.tom.immersivehudplugin.config.PlayerConfig;
import com.tom.immersivehudplugin.config.HudSettingsService;
import com.tom.immersivehudplugin.registry.HudComponentRegistry;
import com.tom.immersivehudplugin.registry.HudEntry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.Color;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public final class ToggleCmd extends AbstractPlayerCommand {

    private static final Color INFO_COLOR = Color.LIGHT_GRAY;
    private static final Color WARN_COLOR = Color.YELLOW;
    private static final Color ERROR_COLOR = Color.RED;
    private static final Color SHOW_COLOR = Color.GREEN;
    private static final Color HIDE_COLOR = Color.RED;

    private final HudSettingsService hudSettingsService;

    public ToggleCmd(
            HudSettingsService hudSettingsService
    ) {
        super("toggle", "Toggle your HUD components; set component/group hide|show");

        this.hudSettingsService = hudSettingsService;

        addUsageVariant(new ToggleOneVariant(hudSettingsService));
        addUsageVariant(new SetStateVariant(hudSettingsService));
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
        context.sendMessage(Message.raw("Usage: /ihud toggle <component>").color(WARN_COLOR));
        context.sendMessage(Message.raw("   or: /ihud toggle <component|group> <hide|show>").color(WARN_COLOR));
        context.sendMessage(Message.raw("Groups: " + availableGroups()).color(INFO_COLOR));
        context.sendMessage(Message.raw("Components: " + availableComponents()).color(INFO_COLOR));
        context.sendMessage(Message.raw("Use /ihud rules <component> <add|remove> <rule> to update component visibility rules.").color(INFO_COLOR));
        context.sendMessage(Message.raw("Use /ihud profile <profile> to apply a quick configuration based on profiles.").color(INFO_COLOR));
        context.sendMessage(Message.raw("Use /ihud status to view your current configuration.").color(INFO_COLOR));
    }

    private static RequiredArg<String> componentOnlyArg(AbstractPlayerCommand cmd) {
        return cmd.withRequiredArg("target", "Component key", ArgTypes.STRING)
                .addValidator(new ComponentOnlyValidator());
    }

    private static RequiredArg<String> componentOrGroupArg(AbstractPlayerCommand cmd) {
        return cmd.withRequiredArg("target", "Component/group", ArgTypes.STRING)
                .addValidator(new ComponentOrGroupValidator());
    }

    private static RequiredArg<String> hideShowArg(AbstractPlayerCommand cmd) {
        return cmd.withRequiredArg("state", "hide/show", ArgTypes.STRING)
                .addValidator(new HideShowValidator());
    }

    private static boolean isValidState(@Nullable String raw) {
        String value = normalize(raw);
        return "hide".equals(value) || "show".equals(value);
    }

    private static boolean toHidden(@Nonnull String rawState) {
        return "hide".equals(normalize(rawState));
    }

    private static boolean isValidComponent(@Nullable String raw) {
        String key = normalize(raw);
        return HudComponentRegistry.Group.fromKey(key) == null
                && HudComponentRegistry.find(key) != null;
    }

    private static boolean isValidComponentOrGroup(@Nullable String raw) {
        String key = normalize(raw);
        return HudComponentRegistry.Group.fromKey(key) != null
                || HudComponentRegistry.find(key) != null;
    }

    private static void sendSingleResult(
            @Nonnull CommandContext context,
            @Nonnull String label,
            boolean hidden
    ) {
        context.sendMessage(Message.join(
                Message.raw("ImmersiveHud " + label + " -> ").color(INFO_COLOR),
                Message.raw(hidden ? "hide" : "show").color(hidden ? HIDE_COLOR : SHOW_COLOR)
        ));
    }

    private static void sendGroupResult(
            @Nonnull CommandContext context,
            @Nonnull String groupLabel,
            boolean hidden,
            int changed
    ) {
        context.sendMessage(Message.join(
                Message.raw("ImmersiveHud " + groupLabel + " -> ").color(INFO_COLOR),
                Message.raw(hidden ? "hide" : "show").color(hidden ? HIDE_COLOR : SHOW_COLOR),
                Message.raw(" (" + changed + " components)").color(INFO_COLOR)
        ));
    }

    private static String availableComponents() {
        return HudComponentRegistry.allList().stream()
                .map(HudEntry::key)
                .sorted()
                .collect(Collectors.joining(", "));
    }

    private static String availableGroups() {
        return Arrays.stream(HudComponentRegistry.Group.values())
                .map(group -> group.key)
                .sorted()
                .collect(Collectors.joining(", "));
    }

    private static String normalize(@Nullable String s) {
        return HudComponentRegistry.normalize(s);
    }

    private static final class ToggleOneVariant extends AbstractPlayerCommand {
        private final HudSettingsService hudSettingsService;
        private final RequiredArg<String> targetArg;

        ToggleOneVariant(HudSettingsService hudSettingsService) {
            super("Toggle a component");
            this.hudSettingsService = hudSettingsService;
            this.targetArg = componentOnlyArg(this);
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
            String key = normalize(targetArg.get(context));
            var entry = HudComponentRegistry.find(key);

            if (entry == null) {
                context.sendMessage(Message.raw("Unknown HUD component: " + key).color(ERROR_COLOR));
                return;
            }

            PlayerConfig playerCfg = hudSettingsService.requirePlayerConfig(playerRef);
            if (playerCfg == null) {
                context.sendMessage(Message.raw("Failed to load your ImmersiveHud profile.").color(ERROR_COLOR));
                return;
            }

            HudComponentsConfig hud = playerCfg.getHudComponents();
            boolean nextHidden = !entry.isHidden(hud);

            hudSettingsService.updateHudComponents(playerRef, cfg -> {
                boolean currentHidden = entry.isHidden(cfg);
                entry.setHidden(cfg, !currentHidden);
            });

            sendSingleResult(context, entry.label(), nextHidden);
        }
    }

    private static final class SetStateVariant extends AbstractPlayerCommand {
        private final HudSettingsService hudSettingsService;
        private final RequiredArg<String> targetArg;
        private final RequiredArg<String> stateArg;

        SetStateVariant(HudSettingsService hudSettingsService) {
            super("Set state");
            this.hudSettingsService = hudSettingsService;
            this.targetArg = componentOrGroupArg(this);
            this.stateArg = hideShowArg(this);
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
            String target = normalize(targetArg.get(context));
            boolean desiredHidden = toHidden(stateArg.get(context));

            PlayerConfig playerCfg = hudSettingsService.requirePlayerConfig(playerRef);
            if (playerCfg == null) {
                context.sendMessage(Message.raw("Failed to load your ImmersiveHud profile.").color(ERROR_COLOR));
                return;
            }

            HudComponentsConfig hud = playerCfg.getHudComponents();

            HudComponentRegistry.Group group = HudComponentRegistry.Group.fromKey(target);
            if (group != null) {
                int changed = 0;

                for (var entry : HudComponentRegistry.allList()) {
                    if (entry.group() != group) {
                        continue;
                    }
                    boolean currentHidden = entry.isHidden(hud);
                    if (currentHidden != desiredHidden) {
                        changed++;
                    }
                }

                hudSettingsService.updateHudComponents(playerRef, cfg -> {
                    for (var entry : HudComponentRegistry.allList()) {
                        if (entry.group() == group) {
                            entry.setHidden(cfg, desiredHidden);
                        }
                    }
                });

                sendGroupResult(context, group.label, desiredHidden, changed);
                return;
            }

            var entry = HudComponentRegistry.find(target);
            if (entry == null) {
                context.sendMessage(Message.raw("Unknown target: " + target).color(ERROR_COLOR));
                return;
            }

            boolean currentHidden = entry.isHidden(hud);
            if (currentHidden == desiredHidden) {
                context.sendMessage(Message.join(
                        Message.raw("ImmersiveHud " + entry.label() + " already set to ").color(INFO_COLOR),
                        Message.raw(desiredHidden ? "hide" : "show").color(desiredHidden ? HIDE_COLOR : SHOW_COLOR)
                ));
                return;
            }

            hudSettingsService.updateHudComponents(playerRef, cfg ->
                    entry.setHidden(cfg, desiredHidden)
            );

            sendSingleResult(context, entry.label(), desiredHidden);
        }
    }

    private static final class ComponentOnlyValidator implements Validator<String> {
        @Override
        public void accept(@Nullable String input, @Nonnull ValidationResults results) {
            String value = normalize(input);

            if (HudComponentRegistry.Group.fromKey(value) != null) {
                results.fail("Groups cannot be toggled directly. Use '/ihud toggle <group> <hide|show>'. Available groups: " + availableGroups());
                return;
            }

            if (!isValidComponent(input)) {
                results.fail("Unknown HUD component: " + input + ". Available components: " + availableComponents());
            }
        }

        @Override
        public void updateSchema(SchemaContext context, @Nonnull Schema target) {
        }
    }

    private static final class ComponentOrGroupValidator implements Validator<String> {
        @Override
        public void accept(@Nullable String input, @Nonnull ValidationResults results) {
            if (!isValidComponentOrGroup(input)) {
                results.fail(
                        "Unknown target: " + input
                                + ". Available groups: " + availableGroups()
                                + ". Available components: " + availableComponents()
                );
            }
        }

        @Override
        public void updateSchema(SchemaContext context, @Nonnull Schema target) {
        }
    }

    private static final class HideShowValidator implements Validator<String> {
        private final Set<String> allowed = new LinkedHashSet<>(Arrays.asList("hide", "show"));

        @Override
        public void accept(@Nullable String input, @Nonnull ValidationResults results) {
            if (!isValidState(input)) {
                results.fail("State must be one of: " + String.join(", ", allowed));
            }
        }

        @Override
        public void updateSchema(SchemaContext context, @Nonnull Schema target) {
        }
    }
}