package com.tom.immersivehudplugin.runtime;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.tom.immersivehudplugin.config.DynamicHudConfig;
import com.tom.immersivehudplugin.config.GlobalConfig;
import com.tom.immersivehudplugin.config.HudComponentsConfig;
import com.tom.immersivehudplugin.config.PlayerConfig;
import com.tom.immersivehudplugin.runtime.context.HudBarStateUpdater;
import com.tom.immersivehudplugin.runtime.context.HudTriggerContextFactory;
import com.tom.immersivehudplugin.runtime.context.PlayerTickContext;
import com.tom.immersivehudplugin.runtime.context.PlayerTickContextFactory;
import com.tom.immersivehudplugin.runtime.signal.HeldItemSignalTracker;
import com.tom.immersivehudplugin.runtime.signal.HudSignalPipeline;
import com.tom.immersivehudplugin.runtime.visibility.HudVisibilityCoordinator;

import javax.annotation.Nullable;

public final class HudTickProcessor {

    private final PlayerTickContextFactory tickContextFactory;
    private final HudSignalPipeline hudSignalPipeline;
    private final HudBarStateUpdater barUpdater;
    private final HudTriggerContextFactory triggerContextFactory;

    private final HudVisibilityCoordinator hudVisibilityCoordinator;
    private final HeldItemSignalTracker heldItemSignalTracker;

    public HudTickProcessor(
            PlayerTickContextFactory tickContextFactory,
            HudSignalPipeline hudSignalPipeline,
            HudBarStateUpdater barUpdater,
            HudTriggerContextFactory triggerContextFactory,
            HudVisibilityCoordinator hudVisibilityCoordinator,
            HeldItemSignalTracker heldItemSignalTracker
    ) {
        this.tickContextFactory = tickContextFactory;
        this.hudSignalPipeline = hudSignalPipeline;
        this.barUpdater = barUpdater;
        this.triggerContextFactory = triggerContextFactory;
        this.hudVisibilityCoordinator = hudVisibilityCoordinator;
        this.heldItemSignalTracker = heldItemSignalTracker;
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

        hudVisibilityCoordinator.ensureStaticHudBuilt(
                evaluation.state(),
                evaluation.hudConfig()
        );

        if (shouldEvaluateDynamicHud(evaluation)) {
            repairHeldItemIfNeeded(evaluation);
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
        PlayerTickContext tickContext = tickContextFactory.build(playerRef);
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

    private boolean shouldEvaluateDynamicHud(TickEvaluation evaluation) {
        return isDynamicHudEnabled(evaluation);
    }

    private void repairHeldItemIfNeeded(TickEvaluation evaluation) {
        heldItemSignalTracker.repairFromInventoryIfNeeded(
                evaluation.state(),
                evaluation.tickContext()
        );
    }

    private void clearDynamicHud(TickEvaluation evaluation) {
        hudVisibilityCoordinator.clearDynamicHidden(evaluation.state());
    }

    private void rebuildDynamicHud(
            TickEvaluation evaluation,
            World world,
            GlobalConfig global,
            long now
    ) {
        PlayerHudState state = evaluation.state();
        PlayerTickContext tickContext = evaluation.tickContext();

        hudSignalPipeline.update(state, world, tickContext, global, now);

        barUpdater.update(state, tickContext);

        var dynamicContext = triggerContextFactory.create(state, now);

        hudVisibilityCoordinator.rebuildDynamicHidden(
                state,
                evaluation.hudConfig(),
                evaluation.dynamicConfig(),
                dynamicContext
        );
    }

    private void applyHud(TickEvaluation evaluation) {
        hudVisibilityCoordinator.applyHudDelta(
                evaluation.tickContext(),
                evaluation.state()
        );
    }

    private boolean isDynamicHudEnabled(TickEvaluation evaluation) {
        PlayerHudState state = evaluation.state();

        if (!state.hasDynamicHudEnabledCache()) {
            state.cacheDynamicHudEnabled(
                    hudVisibilityCoordinator.hasAnyDynamicHudEnabled(
                            evaluation.hudConfig(),
                            evaluation.dynamicConfig()
                    )
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