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
import com.tom.immersivehudplugin.config.PlayerConfigService;
import com.tom.immersivehudplugin.hud.component.HudComponent;
import com.tom.immersivehudplugin.hud.component.HudComponentRegistry;
import com.tom.immersivehudplugin.runtime.HudRuntimeService;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.util.List;
import java.util.Set;

public final class ToggleCmd extends AbstractPlayerCommand {

    private static final Color INFO_COLOR = Color.LIGHT_GRAY;
    private static final Color WARN_COLOR = Color.YELLOW;
    private static final Color ERROR_COLOR = Color.RED;
    private static final Color SUCCESS_COLOR = Color.GREEN;

    private static final String HIDE_STATE = "hide";
    private static final String SHOW_STATE = "show";
    private static final Set<String> VISIBILITY_STATES = Set.of(HIDE_STATE, SHOW_STATE);

    private static final String ERROR_MESSAGE = "Unknown visibility state";

    public ToggleCmd(
            PlayerConfigService playerConfigService,
            HudRuntimeService hudRuntimeService
    ) {
        super("toggle", "Toggle, show or hide HUD components.");

        addUsageVariant(new ToggleImplicitVariant(playerConfigService, hudRuntimeService));
        addUsageVariant(new ToggleExplicitVariant(playerConfigService, hudRuntimeService));
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
        context.sendMessage(Message.raw("Usage: /ihud toggle <component|group>").color(WARN_COLOR));
        context.sendMessage(Message.raw("   or: /ihud toggle <component|group> <hide|show>").color(WARN_COLOR));
        context.sendMessage(Message.raw("Components: " + HudComponentRegistry.availableComponentsText()).color(INFO_COLOR));
        context.sendMessage(Message.raw("Groups: " + HudComponentRegistry.availableGroupsText()).color(INFO_COLOR));
    }

    private static final class ToggleImplicitVariant extends AbstractPlayerCommand {

        private final PlayerConfigService playerConfigService;
        private final HudRuntimeService hudRuntimeService;
        private final RequiredArg<String> targetArg;

        ToggleImplicitVariant(
                PlayerConfigService playerConfigService,
                HudRuntimeService hudRuntimeService
        ) {
            super("Toggle HUD component or group");
            this.playerConfigService = playerConfigService;
            this.hudRuntimeService = hudRuntimeService;

            this.targetArg = withRequiredArg(
                    "target",
                    "HUD component or group",
                    ArgTypes.STRING
            ).addValidator(CommandValidators.componentOrGroup());
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
            String target = targetArg.get(context);

            HudComponent component = HudComponentRegistry.find(target);
            if (component != null) {
                final boolean[] nextHiddenRef = new boolean[1];

                playerConfigService.updatePlayerConfig(playerRef, cfg -> {
                    boolean nextHidden = !component.isHidden(cfg.getHudComponents());
                    component.setHidden(cfg.getHudComponents(), nextHidden);
                    nextHiddenRef[0] = nextHidden;
                });
                hudRuntimeService.onPlayerConfigChanged(playerRef);

                context.sendMessage(Message.join(
                        Message.raw("HUD component ").color(INFO_COLOR),
                        Message.raw(component.key()).color(SUCCESS_COLOR),
                        Message.raw(nextHiddenRef[0] ? " hidden." : " shown.").color(INFO_COLOR)
                ));
                return;
            }

            HudComponentRegistry.Group group = HudComponentRegistry.findGroup(target);
            if (group != null) {
                List<HudComponent> componentsByGroup = HudComponentRegistry.entriesOf(group);
                if (componentsByGroup.isEmpty()) {
                    context.sendMessage(Message.raw("HUD group is empty.").color(ERROR_COLOR));
                    return;
                }

                final boolean[] nextHiddenRef = new boolean[1];

                playerConfigService.updatePlayerConfig(playerRef, cfg -> {
                    boolean nextHidden = !componentsByGroup.get(0).isHidden(cfg.getHudComponents());
                    for (HudComponent componentOfGroup : componentsByGroup) {
                        componentOfGroup.setHidden(cfg.getHudComponents(), nextHidden);
                    }
                    nextHiddenRef[0] = nextHidden;
                });
                hudRuntimeService.onPlayerConfigChanged(playerRef);

                context.sendMessage(Message.join(
                        Message.raw("HUD group ").color(INFO_COLOR),
                        Message.raw(group.key).color(SUCCESS_COLOR),
                        Message.raw(nextHiddenRef[0] ? " hidden." : " shown.").color(INFO_COLOR)
                ));
                return;
            }

            context.sendMessage(Message.raw("Unknown HUD component or group.").color(ERROR_COLOR));
        }
    }

    private static final class ToggleExplicitVariant extends AbstractPlayerCommand {

        private final PlayerConfigService playerConfigService;
        private final HudRuntimeService hudRuntimeService;
        private final RequiredArg<String> targetArg;
        private final RequiredArg<String> stateArg;

        ToggleExplicitVariant(
                PlayerConfigService playerConfigService,
                HudRuntimeService hudRuntimeService
        ) {
            super("Show or hide HUD component or group");
            this.playerConfigService = playerConfigService;
            this.hudRuntimeService = hudRuntimeService;

            this.targetArg = withRequiredArg(
                    "target",
                    "HUD component or group",
                    ArgTypes.STRING
            ).addValidator(CommandValidators.componentOrGroup());

            this.stateArg = withRequiredArg(
                    "state",
                    "Visibility state " + String.join("/", VISIBILITY_STATES),
                    ArgTypes.STRING
            ).addValidator(CommandValidators.validateArguments(VISIBILITY_STATES, ERROR_MESSAGE));
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
            String target = targetArg.get(context);
            String state = CommandValidators.normalize(stateArg.get(context));

            boolean hidden = HIDE_STATE.equals(state);

            HudComponent component = HudComponentRegistry.find(target);
            if (component != null) {
                playerConfigService.updatePlayerConfig(playerRef, cfg ->
                        component.setHidden(cfg.getHudComponents(), hidden)
                );
                hudRuntimeService.onPlayerConfigChanged(playerRef);

                context.sendMessage(Message.join(
                        Message.raw("HUD component ").color(INFO_COLOR),
                        Message.raw(component.key()).color(SUCCESS_COLOR),
                        Message.raw(hidden ? " hidden." : " shown.").color(INFO_COLOR)
                ));
                return;
            }

            HudComponentRegistry.Group group = HudComponentRegistry.findGroup(target);
            if (group != null) {
                List<HudComponent> componentsByGroup = HudComponentRegistry.entriesOf(group);
                if (componentsByGroup.isEmpty()) {
                    context.sendMessage(Message.raw("HUD group is empty.").color(ERROR_COLOR));
                    return;
                }

                playerConfigService.updatePlayerConfig(playerRef, cfg -> {
                    for (HudComponent componentOfGroup : componentsByGroup) {
                        componentOfGroup.setHidden(cfg.getHudComponents(), hidden);
                    }
                });
                hudRuntimeService.onPlayerConfigChanged(playerRef);

                context.sendMessage(Message.join(
                        Message.raw("HUD group ").color(INFO_COLOR),
                        Message.raw(group.key).color(SUCCESS_COLOR),
                        Message.raw(hidden ? " hidden." : " shown.").color(INFO_COLOR)
                ));
                return;
            }

            context.sendMessage(Message.raw("Unknown HUD component or group.").color(ERROR_COLOR));
        }
    }
}