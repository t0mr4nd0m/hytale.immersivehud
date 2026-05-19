package com.tom.immersivehudplugin.runtime.signal;

import com.hypixel.hytale.component.ComponentType;
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

    /**
     * Full FOV angle. 120 means 60 degrees to each side.
     */
    private static final float PLAYER_COMBAT_FOV_DEGREES = 120f;

    public CombatScanResult scanNpcCombat(
            Store<EntityStore> store,
            Ref<EntityStore> playerRef,
            Ref<EntityStore> currentCombatTargetRef,
            float scanRange
    ) {
        if (store == null || playerRef == null || !playerRef.isValid()) {
            return CombatScanResult.none();
        }

        TransformComponent playerTransform =
                store.getComponent(playerRef, TransformComponent.getComponentType());

        if (playerTransform == null) {
            return CombatScanResult.none();
        }

        final CombatScanResult[] result = {CombatScanResult.none()};

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

                        if (!isWithinRange(playerTransform, npcTransform, scanRange)) {
                            continue;
                        }

                        ComponentType<EntityStore, NPCEntity> componentType = NPCEntity.getComponentType();
                        if (componentType == null) {
                            continue;
                        }

                        NPCEntity npc = chunk.getComponent(i, componentType);
                        if (npc == null) {
                            continue;
                        }

                        Role role = npc.getRole();
                        if (role == null) {
                            continue;
                        }

                        FovCheckResult fov = checkPlayerFov(playerTransform, npcTransform);

                        CombatScanResult scan = scanNpc(
                                npcRef,
                                role,
                                playerRef,
                                currentCombatTargetRef,
                                store,
                                playerTransform,
                                npcTransform,
                                fov,
                                scanRange
                        );

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
            Store<EntityStore> store,
            TransformComponent playerTransform,
            TransformComponent npcTransform,
            FovCheckResult fov,
            float scanRange
    ) {
        boolean hostile = isHostileTowardsPlayer(role, npcRef, playerRef, store);
        if (!hostile) {
            return CombatScanResult.none();
        }

        boolean lockedTarget = hasLockedTarget(role, playerRef);
        boolean desiredTarget = hasDesiredTarget(role, playerRef);
        boolean sameCombatTarget = isSameValidRef(npcRef, currentCombatTargetRef);

        boolean targetingPlayer =
                lockedTarget
                        || desiredTarget
                        || sameCombatTarget;

        boolean performingAttack = isNpcPerformingAttack(npcRef, store);
        boolean executingAttack = isExecutingAttack(role);

        boolean attackingPlayer =
                targetingPlayer
                        && (performingAttack || executingAttack);

        if (attackingPlayer) {
            return new CombatScanResult(
                    true,
                    npcRef,
                    true,
                    CombatActivationReason.ATTACKING_PLAYER
            );
        }

        if (isVisibleHostileThreat(playerTransform, npcTransform, fov, scanRange)) {
            return new CombatScanResult(
                    true,
                    npcRef,
                    performingAttack || executingAttack,
                    CombatActivationReason.VISIBLE_HOSTILE
            );
        }

        return CombatScanResult.none();
    }

    private boolean isVisibleHostileThreat(
            TransformComponent playerTransform,
            TransformComponent npcTransform,
            FovCheckResult fov,
            float range
    ) {
        return fov.inside()
                && isWithinRange(playerTransform, npcTransform, range);
    }

    private FovCheckResult checkPlayerFov(
            TransformComponent playerTransform,
            TransformComponent npcTransform
    ) {
        if (playerTransform == null || npcTransform == null) {
            return FovCheckResult.outside();
        }

        var playerPos = playerTransform.getPosition();
        var npcPos = npcTransform.getPosition();

        double dx = npcPos.x - playerPos.x;
        double dz = npcPos.z - playerPos.z;

        double length = Math.sqrt(dx * dx + dz * dz);
        if (length <= 0.0001d) {
            return new FovCheckResult(
                    true,
                    0d,
                    1d,
                    dx,
                    dz,
                    0d,
                    0d,
                    playerTransform.getRotation()
            );
        }

        dx /= length;
        dz /= length;

        var rotation = playerTransform.getRotation();
        double yawRad = rotation.y;
        double forwardX = -Math.sin(yawRad);
        double forwardZ = Math.cos(yawRad);
        double dot = forwardX * dx + forwardZ * dz;
        dot = Math.max(-1d, Math.min(1d, dot));
        double angle = Math.toDegrees(Math.acos(dot));
        boolean inside = angle <= (PLAYER_COMBAT_FOV_DEGREES * 0.5d);

        return new FovCheckResult(
                inside,
                angle,
                dot,
                dx,
                dz,
                forwardX,
                forwardZ,
                rotation
        );
    }

    private boolean hasLockedTarget(
            Role role,
            Ref<EntityStore> playerRef
    ) {
        try {
            if (role == null || playerRef == null) {
                return false;
            }

            Ref<EntityStore> lockedTarget =
                    role.getMarkedEntitySupport().getMarkedEntityRef(LOCKED_TARGET_SLOT);

            return isSameValidRef(lockedTarget, playerRef);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private boolean hasDesiredTarget(
            Role role,
            Ref<EntityStore> playerRef
    ) {
        try {
            if (role == null || playerRef == null || role.getLastBodySteeringMotion() == null) {
                return false;
            }

            Ref<EntityStore> desiredTarget =
                    role.getLastBodySteeringMotion().getDesiredTargetEntity();

            return isSameValidRef(desiredTarget, playerRef);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private boolean isExecutingAttack(Role role) {
        try {
            return role != null && role.getCombatSupport().isExecutingAttack();
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private boolean isNpcPerformingAttack(
            Ref<EntityStore> npcRef,
            Store<EntityStore> store
    ) {
        try {
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
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private boolean isHostileTowardsPlayer(
            Role role,
            Ref<EntityStore> npcRef,
            Ref<EntityStore> playerRef,
            Store<EntityStore> store
    ) {
        try {
            return role != null
                    && npcRef != null
                    && playerRef != null
                    && store != null
                    && role.getWorldSupport().getAttitude(npcRef, playerRef, store) == Attitude.HOSTILE;
        } catch (RuntimeException ex) {
            return false;
        }
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

    private enum CombatActivationReason {
        NONE,
        /** Hostile NPC in front of the player. */
        VISIBLE_HOSTILE,
        /** NPC is actively attacking while it has the player as target. */
        ATTACKING_PLAYER
    }

    private record FovCheckResult(
            boolean inside,
            double angle,
            double dot,
            double dx,
            double dz,
            double forwardX,
            double forwardZ,
            Object playerRotation
    ) {
        private static FovCheckResult outside() {
            return new FovCheckResult(
                    false,
                    999d,
                    0d,
                    0d,
                    0d,
                    0d,
                    0d,
                    null
            );
        }
    }

    public record CombatScanResult(
            boolean active,
            Ref<EntityStore> npcRef,
            boolean attacking,
            CombatActivationReason reason
    ) {
        public static CombatScanResult none() {
            return new CombatScanResult(
                    false,
                    null,
                    false,
                    CombatActivationReason.NONE
            );
        }
    }
}