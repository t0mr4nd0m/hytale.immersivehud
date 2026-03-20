package com.tom.immersivehudplugin.context;

import com.hypixel.hytale.assetstore.AssetMap;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
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
import com.tom.immersivehudplugin.runtime.PlayerTickContext;
import com.tom.immersivehudplugin.utils.ItemInHand;

import javax.annotation.Nullable;

public final class HudContextBuilder {

    private final AssetMap<String, Item> itemAssetMap;
    private final int healthState;
    private final int staminaState;
    private final int manaState;

    public HudContextBuilder(
            AssetMap<String, Item> itemAssetMap,
            int healthState,
            int staminaState,
            int manaState
    ) {
        this.itemAssetMap = itemAssetMap;
        this.healthState = healthState;
        this.staminaState = staminaState;
        this.manaState = manaState;
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

    public void refreshHeldItemStateIfNeeded(PlayerHudState st, PlayerTickContext ctx) {

        Item heldItem = getHeldItemFromInventory(ctx);
        st.heldItem = heldItem;
        st.rangedWeaponInHand = ItemInHand.isRangedWeapon(heldItem);
        st.meleeWeaponInHand = ItemInHand.isMeleeWeapon(heldItem);
        st.heldItemStateInitialized = true;
        st.heldItemRefreshRequested = false;
    }

    public DynamicHudTriggersContext buildDynamicHudTriggerContext(
            PlayerHudState st,
            World world,
            PlayerTickContext ctx,
            GlobalConfig global,
            long now
    ) {
        int hideDelay = hideDelayMs(global);
        st.hideDelayMsHint = hideDelay;

        updateMovementSignals(st, ctx, now, hideDelay);
        updateReticleSignalsIfNeeded(st, world, ctx, global, now, hideDelay);
        updateBars(st, ctx);
        refreshHeldItemStateIfNeeded(st, ctx);
        cleanupWeaponSignals(st);

        return createDynamicHudTriggerContext(st, now);
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

    private float getCurrentBar(PlayerTickContext ctx, int statIndex) {
        if (ctx.stats() == null) {
            return 0f;
        }
        var c = ctx.stats().get(statIndex);
        return (c != null) ? c.get() : 0f;
    }

    private float getMaxBar(PlayerTickContext ctx, int statIndex) {
        if (ctx.stats() == null) {
            return 0f;
        }
        var c = ctx.stats().get(statIndex);
        return (c != null) ? c.getMax() : 0f;
    }

    @Nullable
    private Item getHeldItemFromInventory(PlayerTickContext ctx) {
        try {
            var inventory = ctx.player().getInventory();
            if (inventory == null) {
                return null;
            }

            var heldStack = inventory.getActiveHotbarItem();
            if (heldStack == null) {
                return null;
            }

            String itemId = heldStack.getItemId();
            if (itemId == null || itemId.isBlank()) {
                return null;
            }

            return itemAssetMap.getAsset(itemId);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private void updateMovementSignals(
            PlayerHudState st,
            PlayerTickContext ctx,
            long now,
            int hideDelay
    ) {
        var ms = ctx.movement() != null ? ctx.movement().getMovementStates() : null;
        if (ms == null) {
            clearMovementSignals(st);
            return;
        }

        boolean moving =
                ms.walking
                        || ms.running
                        || ms.sprinting
                        || ms.swimming
                        || ms.gliding
                        || ms.flying
                        || ms.mounting
                        || ms.jumping
                        || ms.crouching
                        || ms.climbing
                        || ms.falling
                        || ms.rolling;

        setTimedState(st, HudSignal.PLAYER_MOVING, moving, now, hideDelay);
        setTimedState(st, HudSignal.PLAYER_WALKING, ms.walking, now, hideDelay);
        setTimedState(st, HudSignal.PLAYER_RUNNING, ms.running, now, hideDelay);
        setTimedState(st, HudSignal.PLAYER_SPRINTING, ms.sprinting, now, hideDelay);
        setTimedState(st, HudSignal.PLAYER_MOUNTING, ms.mounting, now, hideDelay);
        setTimedState(st, HudSignal.PLAYER_SWIMMING, ms.swimming, now, hideDelay);
        setTimedState(st, HudSignal.PLAYER_FLYING, ms.flying, now, hideDelay);
        setTimedState(st, HudSignal.PLAYER_GLIDING, ms.gliding, now, hideDelay);
        setTimedState(st, HudSignal.PLAYER_JUMPING, ms.jumping, now, hideDelay);
        setTimedState(st, HudSignal.PLAYER_CROUCHING, ms.crouching, now, hideDelay);
        setTimedState(st, HudSignal.PLAYER_CLIMBING, ms.climbing, now, hideDelay);
        setTimedState(st, HudSignal.PLAYER_IN_FLUID, ms.inFluid, now, hideDelay);
        setTimedState(st, HudSignal.PLAYER_ON_GROUND, ms.onGround, now, hideDelay);
        setTimedState(st, HudSignal.PLAYER_FALLING, ms.falling, now, hideDelay);
        setTimedState(st, HudSignal.PLAYER_SITTING, ms.sitting, now, hideDelay);
        setTimedState(st, HudSignal.PLAYER_ROLLING, ms.rolling, now, hideDelay);
    }

    private void setTimedState(
            PlayerHudState st,
            HudSignal signal,
            boolean active,
            long now,
            int hideDelay
    ) {
        if (active) {
            st.t.pulse(signal, now, hideDelay);
        } else {
            st.t.clear(signal);
        }
    }

    private void clearMovementSignals(PlayerHudState st) {
        st.t.clear(HudSignal.PLAYER_MOVING);
        st.t.clear(HudSignal.PLAYER_WALKING);
        st.t.clear(HudSignal.PLAYER_RUNNING);
        st.t.clear(HudSignal.PLAYER_SPRINTING);
        st.t.clear(HudSignal.PLAYER_MOUNTING);
        st.t.clear(HudSignal.PLAYER_SWIMMING);
        st.t.clear(HudSignal.PLAYER_FLYING);
        st.t.clear(HudSignal.PLAYER_GLIDING);
        st.t.clear(HudSignal.PLAYER_JUMPING);
        st.t.clear(HudSignal.PLAYER_CROUCHING);
        st.t.clear(HudSignal.PLAYER_CLIMBING);
        st.t.clear(HudSignal.PLAYER_IN_FLUID);
        st.t.clear(HudSignal.PLAYER_ON_GROUND);
        st.t.clear(HudSignal.PLAYER_FALLING);
        st.t.clear(HudSignal.PLAYER_SITTING);
        st.t.clear(HudSignal.PLAYER_ROLLING);
    }

    private void updateReticleSignalsIfNeeded(
            PlayerHudState st,
            World world,
            PlayerTickContext ctx,
            GlobalConfig global,
            long now,
            int hideDelay
    ) {
        int scanMs = Math.max(100, intervalMs(global));
        long lastScan = st.lastReticleScanMs;

        if ((now - lastScan) < scanMs) {
            return;
        }

        float targetRange = reticleTargetRange(global);
        ReticleScanResult scan = scanReticle(world, ctx, targetRange);

        setTimedState(st, HudSignal.TARGET_ENTITY, scan.targetEntity(), now, hideDelay);
        setTimedState(st, HudSignal.INTERACTABLE_BLOCK, scan.interactableBlock(), now, hideDelay);

        st.lastReticleScanMs = now;
    }

    private ReticleScanResult scanReticle(
            World world,
            PlayerTickContext ctx,
            float targetRange
    ) {
        Ref<EntityStore> target = TargetUtil.getTargetEntity(ctx.ref(), targetRange, ctx.store());
        boolean hasEntityTarget = target != null && !target.equals(ctx.ref());

        boolean lookingAtInteractable = false;
        Vector3i blockPos = TargetUtil.getTargetBlock(ctx.ref(), targetRange, ctx.store());
        if (blockPos != null) {
            BlockType bt = world.getBlockType(blockPos);
            var flags = bt != null ? bt.getFlags() : null;
            lookingAtInteractable = flags != null && flags.isUsable;
        }

        return new ReticleScanResult(hasEntityTarget, lookingAtInteractable);
    }

    private void updateBars(PlayerHudState st, PlayerTickContext ctx) {
        st.healthBar.update(getCurrentBar(ctx, healthState), getMaxBar(ctx, healthState));
        st.staminaBar.update(getCurrentBar(ctx, staminaState), getMaxBar(ctx, staminaState));
        st.manaBar.update(getCurrentBar(ctx, manaState), getMaxBar(ctx, manaState));
    }

    private void cleanupWeaponSignals(PlayerHudState st) {

        if (!st.meleeWeaponInHand) {
            st.t.clear(HudSignal.HOLDING_MELEE_WEAPON);
        }

        if (!st.rangedWeaponInHand) {
            st.t.clear(HudSignal.HOLDING_RANGED_WEAPON);
        }

        if (!st.meleeWeaponInHand && !st.rangedWeaponInHand) {
            st.t.clear(HudSignal.CHARGING_WEAPON);
        }
    }

    private DynamicHudTriggersContext createDynamicHudTriggerContext(
            PlayerHudState st,
            long now
    ) {
        boolean rangedWeaponInHand = st.rangedWeaponInHand;
        boolean meleeWeaponInHand = st.meleeWeaponInHand;

        return new DynamicHudTriggersContext(
                st.t.active(HudSignal.HOTBAR_INPUT, now),
                (rangedWeaponInHand || meleeWeaponInHand) && st.t.active(HudSignal.CHARGING_WEAPON, now),
                st.t.active(HudSignal.CONSUMABLE_USE, now),
                st.t.active(HudSignal.TARGET_ENTITY, now),
                st.t.active(HudSignal.INTERACTABLE_BLOCK, now),

                st.t.active(HudSignal.PLAYER_MOVING, now),
                st.t.active(HudSignal.PLAYER_WALKING, now),
                st.t.active(HudSignal.PLAYER_RUNNING, now),
                st.t.active(HudSignal.PLAYER_SPRINTING, now),
                st.t.active(HudSignal.PLAYER_MOUNTING, now),
                st.t.active(HudSignal.PLAYER_SWIMMING, now),
                st.t.active(HudSignal.PLAYER_FLYING, now),
                st.t.active(HudSignal.PLAYER_GLIDING, now),
                st.t.active(HudSignal.PLAYER_JUMPING, now),
                st.t.active(HudSignal.PLAYER_CROUCHING, now),
                st.t.active(HudSignal.PLAYER_CLIMBING, now),
                st.t.active(HudSignal.PLAYER_IN_FLUID, now),
                st.t.active(HudSignal.PLAYER_ON_GROUND, now),
                st.t.active(HudSignal.PLAYER_FALLING, now),
                st.t.active(HudSignal.PLAYER_SITTING, now),
                st.t.active(HudSignal.PLAYER_ROLLING, now),

                rangedWeaponInHand,
                meleeWeaponInHand,

                st.healthBar,
                st.staminaBar,
                st.manaBar
        );
    }

    private record ReticleScanResult(
            boolean targetEntity,
            boolean interactableBlock
    ) {}
}