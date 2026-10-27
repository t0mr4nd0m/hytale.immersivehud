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

        if (evaluation == null) return;

        hudVisibilityCoordinator.ensureStaticHudBuilt(
                evaluation.state(),
                evaluation.hudConfig(),
                evaluation.dynamicConfig()
        );

        if (evaluation.plan().enabled()) {
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
        HudDetectionPlan plan = state.detectionPlan(playerConfig.getHudComponents(), playerConfig.getDynamicHud());
        PlayerTickContext tickContext = tickContextFactory.build(playerRef, plan);

        if (tickContext == null) return null;

        return new TickEvaluation(
                state,
                playerConfig.getHudComponents(),
                playerConfig.getDynamicHud(),
                tickContext,
                plan
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

        /*
         * Interaction signals must be updated after refreshing the held item,
         * because their classification depends on the currently equipped item.
         */
        HudDetectionPlan plan = evaluation.plan();
        if (plan.heldItem() || plan.hotbar()) {
            heldItemSignalTracker.refreshFromInventory(state, tickContext, now, plan);
        }
        if (plan.interactions()) {
            heldItemSignalTracker.updateInteractionSignals(state, tickContext, now, plan);
        }

        hudSignalPipeline.update(state, world, tickContext, global, now, plan);

        if (plan.bars()) barUpdater.update(state, tickContext, plan);

        var dynamicContext = triggerContextFactory.create(state, now, plan);

        hudVisibilityCoordinator.rebuildDynamicHidden(
                state,
                evaluation.hudConfig(),
                evaluation.dynamicConfig(),
                dynamicContext
        );
    }

    private void applyHud(TickEvaluation evaluation) {
        PlayerHudState state = evaluation.state();

        if (!state.isRuntimeInitialized()) {
            hudVisibilityCoordinator.applyInitialHudSnapshot(evaluation.tickContext(), state);
            state.markRuntimeInitialized();
            return;
        }

        hudVisibilityCoordinator.applyHudDelta(evaluation.tickContext(), state);
    }

    private record TickEvaluation(
            PlayerHudState state,
            HudComponentsConfig hudConfig,
            DynamicHudConfig dynamicConfig,
            PlayerTickContext tickContext,
            HudDetectionPlan plan
    ) {}
}
