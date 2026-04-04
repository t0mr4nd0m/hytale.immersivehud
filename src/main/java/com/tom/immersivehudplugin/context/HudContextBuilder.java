package com.tom.immersivehudplugin.context;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.tom.immersivehudplugin.config.GlobalConfig;
import com.tom.immersivehudplugin.rules.DynamicHudTriggersContext;
import com.tom.immersivehudplugin.runtime.HudSignal;
import com.tom.immersivehudplugin.runtime.PlayerHudState;

import javax.annotation.Nullable;

public final class HudContextBuilder {

    private final int healthState;
    private final int staminaState;
    private final int manaState;
    private final int oxygenState;

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

        updateMovementSignals(state, tickContext, now, hideDelay);
        updateReticleSignalsIfNeeded(state, world, tickContext, global, now, hideDelay);
        updateBars(state, tickContext);

        return createDynamicHudTriggerContext(state, now);
    }

    private int hideDelayMs(GlobalConfig cfg) {
        return cfg != null ? cfg.getHideDelayMs() : GlobalConfig.HIDE_DELAY_MS;
    }

    private int intervalMs(GlobalConfig cfg) {
        return cfg != null ? cfg.getIntervalMs() : GlobalConfig.INTERVAL_MS;
    }

    private float reticleTargetRange(GlobalConfig cfg) {
        return cfg != null ? cfg.getReticleTargetRange() : GlobalConfig.RETICLE_TARGET_RANGE;
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

    private void updateMovementSignals(
            PlayerHudState state,
            PlayerTickContext tickContext,
            long now,
            int hideDelay
    ) {
        var movementStates = tickContext.movement() != null ? tickContext.movement().getMovementStates() : null;
        if (movementStates == null) {
            return;
        }

        boolean isMoving =
                movementStates.walking
                        || movementStates.running
                        || movementStates.sprinting
                        || movementStates.swimming
                        || movementStates.mounting
                        || movementStates.flying
                        || movementStates.gliding
                        || movementStates.jumping
                        || movementStates.climbing
                        || movementStates.falling
                        || movementStates.rolling;

        if (isMoving) state.t.pulse(HudSignal.PLAYER_MOVING, now, hideDelay);
        if (movementStates.walking) state.t.pulse(HudSignal.PLAYER_WALKING, now, hideDelay);
        if (movementStates.running) state.t.pulse(HudSignal.PLAYER_RUNNING, now, hideDelay);
        if (movementStates.sprinting) state.t.pulse(HudSignal.PLAYER_SPRINTING, now, hideDelay);
        if (movementStates.swimming) state.t.pulse(HudSignal.PLAYER_SWIMMING, now, hideDelay);
        if (movementStates.mounting) state.t.pulse(HudSignal.PLAYER_MOUNTING, now, hideDelay);
        if (movementStates.flying) state.t.pulse(HudSignal.PLAYER_FLYING, now, hideDelay);
        if (movementStates.gliding) state.t.pulse(HudSignal.PLAYER_GLIDING, now, hideDelay);
        if (movementStates.jumping) state.t.pulse(HudSignal.PLAYER_JUMPING, now, hideDelay);
        if (movementStates.climbing) state.t.pulse(HudSignal.PLAYER_CLIMBING, now, hideDelay);
        if (movementStates.falling) state.t.pulse(HudSignal.PLAYER_FALLING, now, hideDelay);
        if (movementStates.rolling) state.t.pulse(HudSignal.PLAYER_ROLLING, now, hideDelay);
        if (movementStates.crouching) state.t.pulse(HudSignal.PLAYER_CROUCHING, now, hideDelay);
        if (movementStates.idle) state.t.pulse(HudSignal.PLAYER_IDLE, now, hideDelay);
        if (movementStates.sitting) state.t.pulse(HudSignal.PLAYER_SITTING, now, hideDelay);
        if (movementStates.sleeping) state.t.pulse(HudSignal.PLAYER_SLEEPING, now, hideDelay);
        if (movementStates.inFluid) state.t.pulse(HudSignal.PLAYER_IN_FLUID, now, hideDelay);
        if (movementStates.onGround) state.t.pulse(HudSignal.PLAYER_ON_GROUND, now, hideDelay);
    }

    private void updateReticleSignalsIfNeeded(
            PlayerHudState state,
            World world,
            PlayerTickContext tickContext,
            GlobalConfig global,
            long now,
            int hideDelay
    ) {
        if (!shouldScanReticle(state, global, now)) {
            return;
        }

        float targetRange = reticleTargetRange(global);
        ReticleScanResult scan = scanReticle(world, tickContext, targetRange);

        if (scan.targetEntity()) {
            state.t.pulse(HudSignal.TARGET_ENTITY, now, hideDelay);
        }

        if (scan.interactableBlock()) {
            state.t.pulse(HudSignal.INTERACTABLE_BLOCK, now, hideDelay);
        }

        markReticleScanExecuted(state, now);
    }

    private boolean shouldScanReticle(
            PlayerHudState state,
            GlobalConfig global,
            long now
    ) {
        return (now - state.lastReticleScanMs) >= reticleScanIntervalMs(global);
    }

    private int reticleScanIntervalMs(GlobalConfig global) {
        return Math.max(100, intervalMs(global));
    }

    private void markReticleScanExecuted(PlayerHudState state, long now) {
        state.lastReticleScanMs = now;
    }

    private ReticleScanResult scanReticle(
            World world,
            PlayerTickContext tickContext,
            float targetRange
    ) {
        Ref<EntityStore> target = TargetUtil.getTargetEntity(
                tickContext.ref(),
                targetRange,
                tickContext.store()
        );
        boolean hasEntityTarget = target != null && !target.equals(tickContext.ref());

        boolean lookingAtInteractable = false;
        Vector3i blockPos = TargetUtil.getTargetBlock(
                tickContext.ref(),
                targetRange,
                tickContext.store()
        );

        if (blockPos != null) {
            BlockType blockType = world.getBlockType(blockPos);
            var flags = blockType != null ? blockType.getFlags() : null;
            lookingAtInteractable = flags != null && flags.isUsable;
        }

        return new ReticleScanResult(hasEntityTarget, lookingAtInteractable);
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
                state.t.active(HudSignal.HOTBAR_INPUT, now),
                (rangedWeaponInHand || meleeWeaponInHand) && state.t.active(HudSignal.CHARGING_WEAPON, now),
                state.t.active(HudSignal.CONSUMABLE_USE, now),
                state.t.active(HudSignal.TARGET_ENTITY, now),
                state.t.active(HudSignal.INTERACTABLE_BLOCK, now),

                state.t.active(HudSignal.PLAYER_MOVING, now),
                state.t.active(HudSignal.PLAYER_WALKING, now),
                state.t.active(HudSignal.PLAYER_RUNNING, now),
                state.t.active(HudSignal.PLAYER_SPRINTING, now),
                state.t.active(HudSignal.PLAYER_MOUNTING, now),
                state.t.active(HudSignal.PLAYER_SWIMMING, now),
                state.t.active(HudSignal.PLAYER_FLYING, now),
                state.t.active(HudSignal.PLAYER_GLIDING, now),
                state.t.active(HudSignal.PLAYER_JUMPING, now),
                state.t.active(HudSignal.PLAYER_CROUCHING, now),
                state.t.active(HudSignal.PLAYER_CLIMBING, now),
                state.t.active(HudSignal.PLAYER_FALLING, now),
                state.t.active(HudSignal.PLAYER_ROLLING, now),
                state.t.active(HudSignal.PLAYER_IDLE, now),
                state.t.active(HudSignal.PLAYER_SITTING, now),
                state.t.active(HudSignal.PLAYER_SLEEPING, now),
                state.t.active(HudSignal.PLAYER_IN_FLUID, now),
                state.t.active(HudSignal.PLAYER_ON_GROUND, now),

                rangedWeaponInHand,
                meleeWeaponInHand,

                state.healthBar,
                state.staminaBar,
                state.manaBar,
                state.oxygenBar
        );
    }

    private record ReticleScanResult(
            boolean targetEntity,
            boolean interactableBlock
    ) {}
}