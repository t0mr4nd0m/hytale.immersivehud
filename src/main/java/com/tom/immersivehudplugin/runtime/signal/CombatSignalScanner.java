package com.tom.immersivehudplugin.runtime.signal;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.asset.type.attitude.Attitude;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.hypixel.hytale.server.npc.blackboard.view.combat.CombatViewSystems;
import com.hypixel.hytale.server.npc.blackboard.view.combat.InterpretedCombatData;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import com.tom.immersivehudplugin.config.GlobalConfig;
import org.joml.Vector3i;

import java.util.List;

public final class CombatSignalScanner {

    private static final String LOCKED_TARGET_SLOT = "LockedTarget";

    private double playerCombatHorizontalFovDegrees (GlobalConfig cfg) {
        return cfg != null
                ? cfg.getPlayerCombatHorizontalFovDegrees()
                : GlobalConfig.PLAYER_COMBAT_HORIZONTAL_FOV_DEGREES;
    }

    private double playerCombatVerticalFovDegrees (GlobalConfig cfg) {
        return cfg != null
                ? cfg.getPlayerCombatVerticalFovDegrees()
                : GlobalConfig.PLAYER_COMBAT_VERTICAL_FOV_DEGREES;
    }

    private double losNpcTargetHeight (GlobalConfig cfg) {
        return cfg != null
                ? cfg.getLosNpcTargetHeight()
                : GlobalConfig.LOS_NPC_TARGET_HEIGHT;
    }

    private double losTargetEpsilon(GlobalConfig cfg) {
        return cfg != null
                ? cfg.getLosTargetEpsilon()
                : GlobalConfig.LOS_TARGET_EPSILON;
    }

    public CombatScanResult scanNpcCombat(
            Store<EntityStore> store,
            Ref<EntityStore> playerRef,
            float scanRange,
            GlobalConfig global
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

                        CombatGeometry geometry = checkCombatGeometry(playerRef, store, npcTransform, global);

                        CombatScanResult scan = scanNpc(
                                npcRef,
                                role,
                                playerRef,
                                store,
                                npcTransform,
                                geometry,
                                global
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
            Store<EntityStore> store,
            TransformComponent npcTransform,
            CombatGeometry geometry,
            GlobalConfig global
    ) {
        boolean hostile = isHostileTowardsPlayer(role, npcRef, playerRef, store);

        boolean lockedTarget = hasLockedTarget(role, playerRef);
        boolean desiredTarget = hasDesiredTarget(role, playerRef);
        boolean targetingPlayer = lockedTarget || desiredTarget;

        boolean performingAttack = isNpcPerformingAttack(npcRef, store);
        boolean executingAttack = isExecutingAttack(role);
        boolean attackingPlayer = hostile && targetingPlayer && (performingAttack || executingAttack);

        boolean visibleHostileCandidate =
                hostile
                        && geometry.insideHorizontalFov()
                        && geometry.insideVerticalFov();

        boolean visibleHostile =
                visibleHostileCandidate
                        && hasLineOfSightToNpc(playerRef, store, npcTransform, global);

        if (attackingPlayer) {
            return new CombatScanResult(
                    true,
                    npcRef,
                    true,
                    CombatActivationReason.ATTACKING_PLAYER
            );
        }

        if (visibleHostile) {
            return new CombatScanResult(
                    true,
                    npcRef,
                    performingAttack || executingAttack,
                    CombatActivationReason.VISIBLE_HOSTILE
            );
        }

        return CombatScanResult.none();
    }

    private CombatGeometry checkCombatGeometry(
            Ref<EntityStore> playerRef,
            Store<EntityStore> store,
            TransformComponent npcTransform,
            GlobalConfig global
    ) {
        if (playerRef == null || store == null || npcTransform == null) {
            return CombatGeometry.outside();
        }

        var npcPos = npcTransform.getPosition();

        var look = TargetUtil.getLook(playerRef, store);
        var lookPos = look.getPosition();
        var lookDir = look.getDirection();

        double rawDx = npcPos.x - lookPos.x;
        double rawDy = npcPos.y + losNpcTargetHeight(global) - lookPos.y;
        double rawDz = npcPos.z - lookPos.z;

        double horizontalDistance = Math.sqrt(rawDx * rawDx + rawDz * rawDz);

        if (horizontalDistance <= 0.0001d) {
            return new CombatGeometry(
                    true,
                    false
            );
        }

        double dirToNpcX = rawDx / horizontalDistance;
        double dirToNpcZ = rawDz / horizontalDistance;

        double forwardHorizontalLength = Math.sqrt(lookDir.x * lookDir.x + lookDir.z * lookDir.z);
        if (forwardHorizontalLength <= 0.0001d) {
            return CombatGeometry.outside();
        }

        double forwardX = lookDir.x / forwardHorizontalLength;
        double forwardZ = lookDir.z / forwardHorizontalLength;

        double dot = forwardX * dirToNpcX + forwardZ * dirToNpcZ;
        dot = Math.max(-1d, Math.min(1d, dot));

        double horizontalAngle = Math.toDegrees(Math.acos(dot));
        boolean insideHorizontal =
                horizontalAngle <= (playerCombatHorizontalFovDegrees(global) * 0.5d);

        double verticalAngle = Math.toDegrees(Math.atan2(rawDy, horizontalDistance));
        boolean insideVertical =
                Math.abs(verticalAngle) <= (playerCombatVerticalFovDegrees(global) * 0.5d);

        return new CombatGeometry(
                insideHorizontal,
                insideVertical
        );
    }

    private boolean hasLineOfSightToNpc(
            Ref<EntityStore> playerRef,
            Store<EntityStore> store,
            TransformComponent npcTransform,
            GlobalConfig global
    ) {
        try {
            if (playerRef == null || store == null || npcTransform == null) {
                return false;
            }

            EntityStore entityStore = store.getExternalData();

            World world = entityStore.getWorld();

            var look = TargetUtil.getLook(playerRef, store);
            var playerPos = look.getPosition();
            var npcPos = npcTransform.getPosition();

            double originX = playerPos.x;
            double originY = playerPos.y;
            double originZ = playerPos.z;

            double targetX = npcPos.x;
            double targetY = npcPos.y + losNpcTargetHeight(global);
            double targetZ = npcPos.z;

            double dx = targetX - originX;
            double dy = targetY - originY;
            double dz = targetZ - originZ;

            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (distance <= 0.0001d) {
                return true;
            }

            double dirX = dx / distance;
            double dirY = dy / distance;
            double dirZ = dz / distance;

            double maxDistance = Math.max(0d, distance - losTargetEpsilon(global));

            Vector3i blockingBlock = TargetUtil.getTargetBlock(
                    world,
                    (blockId, _) -> blockId != 0,
                    originX,
                    originY,
                    originZ,
                    dirX,
                    dirY,
                    dirZ,
                    maxDistance
            );

            return blockingBlock == null;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private boolean hasLockedTarget(
            Role role,
            Ref<EntityStore> playerRef
    ) {
        try {
            if (role == null || playerRef == null) {
                return false;
            }

            Ref<EntityStore> lockedTarget = role.getMarkedEntitySupport().getMarkedEntityRef(LOCKED_TARGET_SLOT);

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

            Ref<EntityStore> desiredTarget = role.getLastBodySteeringMotion().getDesiredTargetEntity();

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
        /** Hostile NPC in front of the player and not blocked by terrain. */
        VISIBLE_HOSTILE,
        /** NPC is actively attacking while it has the player as target. */
        ATTACKING_PLAYER
    }

    private record CombatGeometry(
            boolean insideHorizontalFov,
            boolean insideVerticalFov
    ) {
        private static CombatGeometry outside() {
            return new CombatGeometry(false, false);
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