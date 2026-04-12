package com.tom.immersivehudplugin.context;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.tom.immersivehudplugin.config.GlobalConfig;
import com.tom.immersivehudplugin.rules.DynamicHudTriggersContext;
import com.tom.immersivehudplugin.rules.DynamicHudTriggers;
import com.tom.immersivehudplugin.runtime.MovementSignalTracker;
import com.tom.immersivehudplugin.runtime.PlayerHudState;
import com.tom.immersivehudplugin.runtime.ReticleTracker;

import javax.annotation.Nullable;

public final class HudContextBuilder {

    private final int healthState;
    private final int staminaState;
    private final int manaState;
    private final int oxygenState;

    private final MovementSignalTracker movementSignalTracker = new MovementSignalTracker();
    private final ReticleTracker reticleTracker = new ReticleTracker();

    public HudContextBuilder(
            int healthState,
            int staminaState,
            int manaState,
            int oxygenState
    ) {
        this.healthState = healthState;
        this.staminaState = staminaState;
        this.manaState = manaState;
        this.oxygenState = oxygenState;
    }

    @Nullable
    public PlayerTickContext buildCtx(PlayerRef playerRef) {
        try {
            Ref<EntityStore> ref = playerRef.getReference();
            if (ref == null) {
                return null;
            }

            Store<EntityStore> store = ref.getStore();

            Player player = store.getComponent(ref, Player.getComponentType());
            if (player == null) {
                return null;
            }

            EntityStatMap stats = store.getComponent(ref, EntityStatMap.getComponentType());
            MovementStatesComponent movement = store.getComponent(ref, MovementStatesComponent.getComponentType());

            return new PlayerTickContext(playerRef, ref, store, player, stats, movement);

        } catch (IllegalStateException ex) {
            return null;
        }
    }

    public DynamicHudTriggersContext buildDynamicHudTriggerContext(
            PlayerHudState state,
            World world,
            PlayerTickContext tickContext,
            GlobalConfig global,
            long now
    ) {
        int hideDelay = hideDelayMs(global);
        state.hideDelayMsHint = hideDelay;

        movementSignalTracker.updateMovementSignals(state, tickContext, now, hideDelay);
        reticleTracker.updateReticleSignalsIfNeeded(state, world, tickContext, global, now, hideDelay);
        updateBars(state, tickContext);

        return createDynamicHudTriggerContext(state, now);
    }

    private int hideDelayMs(GlobalConfig cfg) {
        return cfg != null ? cfg.getHideDelayMs() : GlobalConfig.HIDE_DELAY_MS;
    }

    private float getCurrentBar(PlayerTickContext tickContext, int statIndex) {
        if (tickContext.stats() == null) {
            return 0f;
        }

        var component = tickContext.stats().get(statIndex);
        return component != null ? component.get() : 0f;
    }

    private float getMaxBar(PlayerTickContext tickContext, int statIndex) {
        if (tickContext.stats() == null) {
            return 0f;
        }

        var component = tickContext.stats().get(statIndex);
        return component != null ? component.getMax() : 0f;
    }

    private void updateBars(PlayerHudState state, PlayerTickContext tickContext) {
        state.healthBar.update(getCurrentBar(tickContext, healthState), getMaxBar(tickContext, healthState));
        state.staminaBar.update(getCurrentBar(tickContext, staminaState), getMaxBar(tickContext, staminaState));
        state.manaBar.update(getCurrentBar(tickContext, manaState), getMaxBar(tickContext, manaState));
        state.oxygenBar.update(getCurrentBar(tickContext, oxygenState), getMaxBar(tickContext, oxygenState));
    }

    private DynamicHudTriggersContext createDynamicHudTriggerContext(
            PlayerHudState state,
            long now
    ) {
        boolean rangedWeaponInHand = state.rangedWeaponInHand;
        boolean meleeWeaponInHand = state.meleeWeaponInHand;

        return new DynamicHudTriggersContext(
                state.t.active(DynamicHudTriggers.HOTBAR_INPUT, now),
                (rangedWeaponInHand || meleeWeaponInHand) && state.t.active(DynamicHudTriggers.CHARGING_WEAPON, now),
                state.t.active(DynamicHudTriggers.CONSUMABLE_USE, now),
                state.t.active(DynamicHudTriggers.TARGET_ENTITY, now),
                state.t.active(DynamicHudTriggers.INTERACTABLE_BLOCK, now),

                state.t.active(DynamicHudTriggers.PLAYER_MOVING, now),
                state.t.active(DynamicHudTriggers.PLAYER_WALKING, now),
                state.t.active(DynamicHudTriggers.PLAYER_RUNNING, now),
                state.t.active(DynamicHudTriggers.PLAYER_SPRINTING, now),
                state.t.active(DynamicHudTriggers.PLAYER_MOUNTING, now),
                state.t.active(DynamicHudTriggers.PLAYER_SWIMMING, now),
                state.t.active(DynamicHudTriggers.PLAYER_FLYING, now),
                state.t.active(DynamicHudTriggers.PLAYER_GLIDING, now),
                state.t.active(DynamicHudTriggers.PLAYER_JUMPING, now),
                state.t.active(DynamicHudTriggers.PLAYER_CROUCHING, now),
                state.t.active(DynamicHudTriggers.PLAYER_CLIMBING, now),
                state.t.active(DynamicHudTriggers.PLAYER_FALLING, now),
                state.t.active(DynamicHudTriggers.PLAYER_ROLLING, now),
                state.t.active(DynamicHudTriggers.PLAYER_IDLE, now),
                state.t.active(DynamicHudTriggers.PLAYER_SITTING, now),
                state.t.active(DynamicHudTriggers.PLAYER_SLEEPING, now),
                state.t.active(DynamicHudTriggers.PLAYER_IN_FLUID, now),
                state.t.active(DynamicHudTriggers.PLAYER_ON_GROUND, now),

                rangedWeaponInHand,
                meleeWeaponInHand,

                state.healthBar,
                state.staminaBar,
                state.manaBar,
                state.oxygenBar
        );
    }
}