package com.tom.immersivehudplugin.runtime.context;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.tom.immersivehudplugin.runtime.HudDetectionPlan;

import javax.annotation.Nullable;

public final class PlayerTickContextFactory {

    @Nullable
    public PlayerTickContext build(PlayerRef playerRef, HudDetectionPlan plan) {
        try {
            Ref<EntityStore> ref = playerRef.getReference();
            if (ref == null || !ref.isValid()) {
                return null;
            }

            Store<EntityStore> store = ref.getStore();

            Player player = store.getComponent(ref, Player.getComponentType());
            if (player == null) {
                return null;
            }

            EntityStatMap stats = plan.bars() ? store.getComponent(ref, EntityStatMap.getComponentType()) : null;
            MovementStatesComponent movement = plan.movement()
                    ? store.getComponent(ref, MovementStatesComponent.getComponentType()) : null;

            return new PlayerTickContext(playerRef, ref, store, player, stats, movement);

        } catch (IllegalStateException ex) {
            return null;
        }
    }
}
