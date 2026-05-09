package com.tom.immersivehudplugin.runtime.signal;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.asset.type.attitude.Attitude;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.blackboard.view.combat.CombatViewSystems;
import com.hypixel.hytale.server.npc.blackboard.view.combat.InterpretedCombatData;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;

import java.util.List;

public final class CombatSignalScanner {

    private static final String LOCKED_TARGET_SLOT = "LockedTarget";

    public CombatScanResult scanNpcCombat(
            Store<EntityStore> store,
            Ref<EntityStore> playerRef,
            Ref<EntityStore> currentCombatTargetRef,
            float range
    ) {
        if (store == null || playerRef == null || !playerRef.isValid()) {
            return CombatScanResult.none();
        }

        TransformComponent playerTransform =
                store.getComponent(playerRef, TransformComponent.getComponentType());

        if (playerTransform == null) {
            return CombatScanResult.none();
        }

        final CombatScanResult[] result = { CombatScanResult.none() };

        store.forEachChunk(
                Query.and(
                        NPCEntity.getComponentType(),
                        TransformComponent.getComponentType()
                ),
                (chunk, _) -> {
                    for (int i = 0; i < chunk.size(); i++) {

                        Ref<EntityStore> npcRef = chunk.getReferenceTo(i);
                        if (!npcRef.isValid()) {
                            continue;
                        }

                        TransformComponent npcTransform =
                                chunk.getComponent(i, TransformComponent.getComponentType());

                        if (!isWithinRange(playerTransform, npcTransform, range)) {
                            continue;
                        }

                        NPCEntity npc = chunk.getComponent(i, NPCEntity.getComponentType());
                        if (npc == null) {
                            continue;
                        }

                        Role role = npc.getRole();
                        if (role == null) {
                            continue;
                        }

                        CombatScanResult scan = scanNpc(npcRef, role, playerRef, currentCombatTargetRef, store);

                        if (scan.active()) {
                            result[0] = scan;
                            return;
                        }
                    }
                }
        );

        return result[0];
    }

    private CombatScanResult scanNpc(
            Ref<EntityStore> npcRef,
            Role role,
            Ref<EntityStore> playerRef,
            Ref<EntityStore> currentCombatTargetRef,
            Store<EntityStore> store
    ) {
        boolean hostile = isHostileTowardsPlayer(role, npcRef, playerRef, store);
        boolean attacking = isNpcPerformingAttack(npcRef, store);
        boolean targetingPlayer = isNpcTargetingPlayer(role, playerRef);
        boolean pursuingPlayer = isNpcPursuingPlayer(role, playerRef);

        boolean sameCombatTarget = isSameValidRef(npcRef, currentCombatTargetRef);

        boolean engagedWithPlayer = targetingPlayer || pursuingPlayer;

        if (hostile && engagedWithPlayer) {
            return new CombatScanResult(true, npcRef, attacking);
        }

        if (hostile && sameCombatTarget) {
            return new CombatScanResult(true, npcRef, false);
        }

        return CombatScanResult.none();
    }

    private boolean isNpcTargetingPlayer(
            Role role,
            Ref<EntityStore> playerRef
    ) {

        return role != null && (hasLockedTarget(role, playerRef)
                || hasDesiredTarget(role, playerRef)
                || isExecutingAttack(role));
    }

    private boolean hasLockedTarget(Role role, Ref<EntityStore> playerRef) {
        var markedEntitySupport = role.getMarkedEntitySupport();

        Ref<EntityStore> lockedTarget =
                markedEntitySupport.getMarkedEntityRef(LOCKED_TARGET_SLOT);

        return playerRef.equals(lockedTarget);
    }

    private boolean hasDesiredTarget(Role role, Ref<EntityStore> playerRef) {
        var bodyMotion = role.getLastBodySteeringMotion();
        if (bodyMotion == null) {
            return false;
        }

        Ref<EntityStore> desiredTarget = bodyMotion.getDesiredTargetEntity();
        return isSameValidRef(desiredTarget, playerRef);
    }

    private boolean isExecutingAttack(Role role) {
        var combatSupport = role.getCombatSupport();
        return combatSupport.isExecutingAttack();
    }

    private boolean isNpcPursuingPlayer(Role role, Ref<EntityStore> playerRef) {
        var bodyMotion = role.getLastBodySteeringMotion();
        if (bodyMotion == null) {
            return false;
        }

        return isSameValidRef(bodyMotion.getDesiredTargetEntity(), playerRef);
    }

    private boolean isSameValidRef(
            Ref<EntityStore> a,
            Ref<EntityStore> b
    ) {
        return a != null
                && b != null
                && a.isValid()
                && b.isValid()
                && a.equals(b);
    }

    private boolean isWithinRange(
            TransformComponent playerTransform,
            TransformComponent npcTransform,
            float range
    ) {
        if (playerTransform == null || npcTransform == null || range <= 0f) {
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

    public record CombatScanResult(
            boolean active,
            Ref<EntityStore> npcRef,
            boolean attacking
    ) {
        public static CombatScanResult none() {
            return new CombatScanResult(false, null, false);
        }
    }

    private boolean isNpcPerformingAttack(
            Ref<EntityStore> npcRef,
            Store<EntityStore> store
    ) {
        List<InterpretedCombatData> combatData =
                CombatViewSystems.getCombatData(npcRef, store);

        for (InterpretedCombatData data : combatData) {
            if (data.isPerformingMeleeAttack()
                    || data.isPerformingRangedAttack()
                    || data.isCharging()) {
                return true;
            }
        }

        return false;
    }

    private boolean isHostileTowardsPlayer(
            Role role,
            Ref<EntityStore> npcRef,
            Ref<EntityStore> playerRef,
            Store<EntityStore> store
    ) {
        try {
            return role.getWorldSupport().getAttitude(npcRef, playerRef, store) == Attitude.HOSTILE;
        } catch (RuntimeException ex) {
            return true;
        }
    }
}