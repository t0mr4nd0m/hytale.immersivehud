package com.tom.immersivehudplugin.runtime.context;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.tom.immersivehudplugin.config.GlobalConfig;
import com.tom.immersivehudplugin.hud.trigger.HudTriggerContext;
import com.tom.immersivehudplugin.runtime.PlayerHudState;
import com.tom.immersivehudplugin.runtime.signal.MovementSignalTracker;
import com.tom.immersivehudplugin.runtime.signal.ReticleSignalTracker;

import javax.annotation.Nullable;

public final class HudContextBuilder {

    private final PlayerTickContextFactory playerTickContextFactory = new PlayerTickContextFactory();
    private final HudBarStateUpdater hudBarStateUpdater;
    private final HudTriggerContextFactory hudTriggerContextFactory = new HudTriggerContextFactory();

    private final MovementSignalTracker movementSignalTracker = new MovementSignalTracker();
    private final ReticleSignalTracker reticleSignalTracker = new ReticleSignalTracker();

    public HudContextBuilder(
            int healthState,
            int staminaState,
            int manaState,
            int oxygenState
    ) {
        this.hudBarStateUpdater = new HudBarStateUpdater(
                healthState,
                staminaState,
                manaState,
                oxygenState
        );
    }

    @Nullable
    public PlayerTickContext buildCtx(PlayerRef playerRef) {
        return playerTickContextFactory.build(playerRef);
    }

    public HudTriggerContext buildDynamicHudTriggerContext(
            PlayerHudState state,
            World world,
            PlayerTickContext tickContext,
            GlobalConfig global,
            long now
    ) {
        int hideDelay = hideDelayMs(global);
        state.hideDelayMsHint = hideDelay;

        movementSignalTracker.updateMovementSignals(state, tickContext, now, hideDelay);
        reticleSignalTracker.updateReticleSignalsIfNeeded(state, world, tickContext, global, now, hideDelay);
        hudBarStateUpdater.update(state, tickContext);

        return hudTriggerContextFactory.create(state, now);
    }

    private int hideDelayMs(GlobalConfig cfg) {
        return cfg != null ? cfg.getHideDelayMs() : GlobalConfig.HIDE_DELAY_MS;
    }
}