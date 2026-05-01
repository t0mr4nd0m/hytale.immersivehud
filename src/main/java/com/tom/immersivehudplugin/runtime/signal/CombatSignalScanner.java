package com.tom.immersivehudplugin.runtime.signal;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.role.support.MarkedEntitySupport;

import java.util.concurrent.atomic.AtomicBoolean;

public final class CombatSignalScanner {

    private static final String LOCKED_TARGET_SLOT = "LockedTarget";

    public boolean hasNpcTargetingPlayer(
            Store<EntityStore> store,
            Ref<EntityStore> playerRef,
            float range
    ) {
        if (store == null || playerRef == null || !playerRef.isValid()) {
            return false;
        }

        TransformComponent playerTransform =
                store.getComponent(playerRef, TransformComponent.getComponentType());

        if (playerTransform == null) {
            return false;
        }

        AtomicBoolean found = new AtomicBoolean(false);

        store.forEachChunk(
                Query.and(
                        NPCEntity.getComponentType(),
                        TransformComponent.getComponentType()
                ),
                (chunk, commandBuffer) -> {
                    if (found.get()) {
                        return;
                    }

                    for (int i = 0; i < chunk.size(); i++) {
                        Ref<EntityStore> npcRef = chunk.getReferenceTo(i);
                        if (npcRef == null || !npcRef.isValid()) {
                            continue;
                        }

                        TransformComponent npcTransform =
                                chunk.getComponent(i, TransformComponent.getComponentType());

                        if (!isWithinRange(playerTransform, npcTransform, range)) {
                            continue;
                        }

                        NPCEntity npc =
                                chunk.getComponent(i, NPCEntity.getComponentType());

                        if (isNpcTargetingPlayer(npc, playerRef)) {
                            found.set(true);
                            return;
                        }
                    }
                }
        );

        return found.get();
    }

    private boolean isNpcTargetingPlayer(
            NPCEntity npc,
            Ref<EntityStore> playerRef
    ) {
        Role role = npc.getRole();
        if (role == null) {
            return false;
        }

        MarkedEntitySupport markedEntitySupport = role.getMarkedEntitySupport();
        if (markedEntitySupport == null) {
            return false;
        }

        Ref<EntityStore> lockedTarget =
                markedEntitySupport.getMarkedEntityRef(LOCKED_TARGET_SLOT);

        return playerRef.equals(lockedTarget);
    }

    private boolean isWithinRange(
            TransformComponent playerTransform,
            TransformComponent npcTransform,
            float range
    ) {
        if (npcTransform == null || range <= 0f) {
            return false;
        }

        var playerPos = playerTransform.getPosition();
        var npcPos = npcTransform.getPosition();

        double dx = playerPos.x - npcPos.x;
        double dy = playerPos.y - npcPos.y;
        double dz = playerPos.z - npcPos.z;

        double rangeSq = (double) range * range;

        return (dx * dx + dy * dy + dz * dz) <= rangeSq;
    }
}