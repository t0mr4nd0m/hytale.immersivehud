package com.tom.immersivehudplugin.runtime.context;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.tom.immersivehudplugin.config.GlobalConfig;
import com.tom.immersivehudplugin.hud.trigger.HudTriggerContext;
import com.tom.immersivehudplugin.hud.trigger.HudTrigger;
import com.tom.immersivehudplugin.runtime.PlayerHudState;
import com.tom.immersivehudplugin.runtime.signal.MovementSignalTracker;
import com.tom.immersivehudplugin.runtime.signal.ReticleSignalTracker;

import javax.annotation.Nullable;
import java.util.EnumSet;

public final class HudContextBuilder {

    private final int healthState;
    private final int staminaState;
    private final int manaState;
    private final int oxygenState;

    private final MovementSignalTracker movementSignalTracker = new MovementSignalTracker();
    private final ReticleSignalTracker reticleSignalTracker = new ReticleSignalTracker();

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

    private HudTriggerContext createDynamicHudTriggerContext(
            PlayerHudState state,
            long now
    ) {
        EnumSet<HudTrigger> activeSignals = EnumSet.noneOf(HudTrigger.class);

        addIfActive(activeSignals, state, HudTrigger.HOTBAR_INPUT, now);
        addIfActive(activeSignals, state, HudTrigger.CONSUMABLE_USE, now);
        addIfActive(activeSignals, state, HudTrigger.TARGET_ENTITY, now);
        addIfActive(activeSignals, state, HudTrigger.INTERACTABLE_BLOCK, now);

        addIfActive(activeSignals, state, HudTrigger.PLAYER_MOVING, now);
        addIfActive(activeSignals, state, HudTrigger.PLAYER_WALKING, now);
        addIfActive(activeSignals, state, HudTrigger.PLAYER_RUNNING, now);
        addIfActive(activeSignals, state, HudTrigger.PLAYER_SPRINTING, now);
        addIfActive(activeSignals, state, HudTrigger.PLAYER_MOUNTING, now);
        addIfActive(activeSignals, state, HudTrigger.PLAYER_SWIMMING, now);
        addIfActive(activeSignals, state, HudTrigger.PLAYER_FLYING, now);
        addIfActive(activeSignals, state, HudTrigger.PLAYER_GLIDING, now);
        addIfActive(activeSignals, state, HudTrigger.PLAYER_JUMPING, now);
        addIfActive(activeSignals, state, HudTrigger.PLAYER_CROUCHING, now);
        addIfActive(activeSignals, state, HudTrigger.PLAYER_CLIMBING, now);
        addIfActive(activeSignals, state, HudTrigger.PLAYER_FALLING, now);
        addIfActive(activeSignals, state, HudTrigger.PLAYER_ROLLING, now);
        addIfActive(activeSignals, state, HudTrigger.PLAYER_IDLE, now);
        addIfActive(activeSignals, state, HudTrigger.PLAYER_SITTING, now);
        addIfActive(activeSignals, state, HudTrigger.PLAYER_SLEEPING, now);
        addIfActive(activeSignals, state, HudTrigger.PLAYER_IN_FLUID, now);
        addIfActive(activeSignals, state, HudTrigger.PLAYER_ON_GROUND, now);

        if ((state.rangedWeaponInHand || state.meleeWeaponInHand)
                && state.t.active(HudTrigger.CHARGING_WEAPON, now)) {
            activeSignals.add(HudTrigger.CHARGING_WEAPON);
        }

        if ((state.rangedWeaponInHand || state.meleeWeaponInHand)
                && state.t.active(HudTrigger.BLOCKING_ATTACK, now)) {
            activeSignals.add(HudTrigger.BLOCKING_ATTACK);
        }

        if (state.rangedWeaponInHand) {
            activeSignals.add(HudTrigger.HOLDING_RANGED_WEAPON);
        }

        if (state.meleeWeaponInHand) {
            activeSignals.add(HudTrigger.HOLDING_MELEE_WEAPON);
        }

        return new HudTriggerContext(
                activeSignals,
                state.healthBar,
                state.staminaBar,
                state.manaBar,
                state.oxygenBar
        );
    }

    private void addIfActive(
            EnumSet<HudTrigger> activeSignals,
            PlayerHudState state,
            HudTrigger trigger,
            long now
    ) {
        if (state.t.active(trigger, now)) {
            activeSignals.add(trigger);
        }
    }
}