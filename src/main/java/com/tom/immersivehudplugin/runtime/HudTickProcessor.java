package com.tom.immersivehudplugin.runtime;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.tom.immersivehudplugin.config.DynamicHudConfig;
import com.tom.immersivehudplugin.config.GlobalConfig;
import com.tom.immersivehudplugin.config.HudComponentsConfig;
import com.tom.immersivehudplugin.config.PlayerConfig;
import com.tom.immersivehudplugin.context.HudContextBuilder;
import com.tom.immersivehudplugin.context.PlayerTickContext;
import com.tom.immersivehudplugin.managers.PlayerConfigManager;
import com.tom.immersivehudplugin.visibility.HudVisibilityService;

import javax.annotation.Nullable;

public final class HudTickProcessor {

    private final PlayerConfigManager playerConfigManager;
    private final HudContextBuilder hudContextBuilder;
    private final HudVisibilityService hudVisibilityService;
    private final HeldItemTracker heldItemTracker;

    public HudTickProcessor(
            PlayerConfigManager playerConfigManager,
            HudContextBuilder hudContextBuilder,
            HudVisibilityService hudVisibilityService,
            HeldItemTracker heldItemTracker
    ) {
        this.playerConfigManager = playerConfigManager;
        this.hudContextBuilder = hudContextBuilder;
        this.hudVisibilityService = hudVisibilityService;
        this.heldItemTracker = heldItemTracker;
    }

    public void processPlayerTick(
            PlayerRef playerRef,
            World world,
            GlobalConfig global,
            long now,
            PlayerHudState state,
            PlayerConfig playerConfig
    ) {
        TickEvaluation evaluation = buildTickEvaluation(playerRef, state, playerConfig);
        if (evaluation == null) {
            return;
        }

        ensureStaticHud(evaluation);

        if (shouldEvaluateDynamicHud(evaluation.state(), evaluation.hudConfig())) {
            repairHeldItemIfNeeded(evaluation);
            cleanupHeldItemSignals(evaluation);
            rebuildDynamicHud(evaluation, world, global, now);
        } else {
            clearDynamicHud(evaluation);
        }

        applyHud(evaluation);
    }

    @Nullable
    private TickEvaluation buildTickEvaluation(
            PlayerRef playerRef,
            PlayerHudState state,
            PlayerConfig playerConfig
    ) {
        PlayerTickContext tickContext = hudContextBuilder.buildCtx(playerRef);
        if (tickContext == null) {
            return null;
        }

        return new TickEvaluation(
                state,
                playerConfig.getHudComponents(),
                playerConfig.getDynamicHud(),
                tickContext
        );
    }

    private void ensureStaticHud(TickEvaluation evaluation) {
        hudVisibilityService.ensureStaticHudBuilt(
                evaluation.state(),
                evaluation.hudConfig()
        );
    }

    private boolean shouldEvaluateDynamicHud(
            PlayerHudState state,
            HudComponentsConfig hudConfig
    ) {
        return isDynamicHudEnabled(state, hudConfig);
    }

    private void repairHeldItemIfNeeded(TickEvaluation evaluation) {
        heldItemTracker.repairFromInventoryIfNeeded(
                evaluation.state(),
                evaluation.tickContext()
        );
    }

    private void clearDynamicHud(TickEvaluation evaluation) {
        hudVisibilityService.clearDynamicHiddenIfNeeded(evaluation.state());
    }

    private void cleanupHeldItemSignals(TickEvaluation evaluation) {
        heldItemTracker.cleanupWeaponSignals(evaluation.state());
    }

    private void rebuildDynamicHud(
            TickEvaluation evaluation,
            World world,
            GlobalConfig global,
            long now
    ) {
        heldItemTracker.cleanupWeaponSignals(evaluation.state());

        var dynamicContext = hudContextBuilder.buildDynamicHudTriggerContext(
                evaluation.state(),
                world,
                evaluation.tickContext(),
                global,
                now
        );

        hudVisibilityService.rebuildDynamicHidden(
                evaluation.state(),
                evaluation.hudConfig(),
                evaluation.dynamicConfig(),
                dynamicContext
        );
    }

    private void applyHud(TickEvaluation evaluation) {
        hudVisibilityService.applyHudDelta(
                evaluation.tickContext(),
                evaluation.state()
        );
    }

    private boolean isDynamicHudEnabled(PlayerHudState state, HudComponentsConfig hudConfig) {
        if (!state.hasDynamicHudEnabledCache()) {
            state.cacheDynamicHudEnabled(
                    hudVisibilityService.hasAnyDynamicHudEnabled(hudConfig)
            );
        }

        return state.isDynamicHudEnabledCached();
    }

    private record TickEvaluation(
            PlayerHudState state,
            HudComponentsConfig hudConfig,
            DynamicHudConfig dynamicConfig,
            PlayerTickContext tickContext
    ) {}
}