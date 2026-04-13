package com.tom.immersivehudplugin.runtime.context;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nullable;

public final class PlayerTickContextFactory {

    @Nullable
    public PlayerTickContext build(PlayerRef playerRef) {
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
}