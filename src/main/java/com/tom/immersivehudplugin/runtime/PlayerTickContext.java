package com.tom.immersivehudplugin.runtime;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public record PlayerTickContext(
        PlayerRef playerRef,
        Ref<EntityStore> ref,
        Store<EntityStore> store,
        Player player,
        EntityStatMap stats,
        MovementStatesComponent movement
) {}