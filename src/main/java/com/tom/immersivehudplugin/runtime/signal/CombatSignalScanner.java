package com.tom.immersivehudplugin.runtime.signal;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.asset.type.attitude.Attitude;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.hypixel.hytale.server.npc.blackboard.view.combat.CombatViewSystems;
import com.hypixel.hytale.server.npc.blackboard.view.combat.InterpretedCombatData;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.role.support.CombatSupport;
import com.hypixel.hytale.server.npc.role.support.MarkedEntitySupport;
import com.hypixel.hytale.server.npc.role.support.WorldSupport;
import com.tom.immersivehudplugin.config.GlobalConfig;
import org.joml.Vector3i;

import java.util.List;
import java.util.logging.Level;

public final class CombatSignalScanner {

    private final JavaPlugin plugin;

    public CombatSignalScanner(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    private double playerCombatHorizontalFovDegrees(GlobalConfig cfg) {
        return cfg != null
                ? cfg.getPlayerCombatHorizontalFovDegrees()
                : GlobalConfig.PLAYER_COMBAT_HORIZONTAL_FOV_DEGREES;
    }

    private double playerCombatVerticalFovDegrees(GlobalConfig cfg) {
        return cfg != null
                ? cfg.getPlayerCombatVerticalFovDegrees()
                : GlobalConfig.PLAYER_COMBAT_VERTICAL_FOV_DEGREES;
    }

    private double losNpcTargetHeight(GlobalConfig cfg) {
        return cfg != null
                ? cfg.getLosNpcTargetHeight()
                : GlobalConfig.LOS_NPC_TARGET_HEIGHT;
    }

    private double losTargetEpsilon(GlobalConfig cfg) {
        return cfg != null
                ? cfg.getLosTargetEpsilon()
                : GlobalConfig.LOS_TARGET_EPSILON;
    }

    public boolean scanNpcCombat(
            Store<EntityStore> store,
            Ref<EntityStore> playerRef,
            float scanRange,
            GlobalConfig global
    ) {
        if (store == null || playerRef == null || !playerRef.isValid()) return false;

        TransformComponent playerTransform =
            store.getComponent(playerRef, TransformComponent.getComponentType());

        if (playerTransform == null) return false;

        final boolean[] combatDetected = {false};

        store.forEachChunk(
            Query.and(NPCEntity.getComponentType(), TransformComponent.getComponentType()),
            (chunk, _) -> {
                if (combatDetected[0]) return;

                for (int i = 0; i < chunk.size() && !combatDetected[0]; i++) {
                    Ref<EntityStore> npcRef = chunk.getReferenceTo(i);

                    if (!npcRef.isValid()) continue;

                    TransformComponent npcTransform =
                            chunk.getComponent(i, TransformComponent.getComponentType());

                    if (!isWithinRange(playerTransform, npcTransform, scanRange)) continue;

                    @SuppressWarnings("DataFlowIssue")
                    NPCEntity npc = chunk.getComponent(i, NPCEntity.getComponentType());

                    if (npc == null) continue;

                    Role role = npc.getRole();

                    if (role == null) continue;

                    if (scanNpc(npcRef, role, playerRef, store, npcTransform, global)) combatDetected[0] = true;
                }
            }
        );

        return combatDetected[0];
    }

    private boolean scanNpc(
            Ref<EntityStore> npcRef,
            Role role,
            Ref<EntityStore> playerRef,
            Store<EntityStore> store,
            TransformComponent npcTransform,
            GlobalConfig global
    ) {
        boolean hostile =
                isHostileTowardsPlayer(npcRef, playerRef, store);

        if (!hostile) return false;

        boolean lockedTarget = hasLockedTarget(npcRef, playerRef, store);
        boolean desiredTarget = hasDesiredTarget(role, playerRef);
        boolean targetingPlayer = lockedTarget || desiredTarget;
        boolean performingAttack = isNpcPerformingAttack(npcRef, store);
        boolean executingAttack = isExecutingAttack(npcRef, store);
        boolean attackingPlayer = targetingPlayer && (performingAttack || executingAttack);

        if (attackingPlayer) return true;

        CombatGeometry geometry = checkCombatGeometry(playerRef, store, npcTransform, global);
        boolean visibleHostileCandidate = geometry.insideHorizontalFov() && geometry.insideVerticalFov();

        return visibleHostileCandidate && hasLineOfSightToNpc(playerRef, store, npcTransform, global);
    }

    private CombatGeometry checkCombatGeometry(
            Ref<EntityStore> playerRef,
            Store<EntityStore> store,
            TransformComponent npcTransform,
            GlobalConfig global
    ) {
        if (playerRef == null || store == null || npcTransform == null) return CombatGeometry.outside();

        var npcPos = npcTransform.getPosition();
        var look = TargetUtil.getLook(playerRef, store);
        var lookPos = look.getPosition();
        var lookDir = look.getDirection();

        double rawDx = npcPos.x - lookPos.x;
        double rawDy = npcPos.y + losNpcTargetHeight(global) - lookPos.y;
        double rawDz = npcPos.z - lookPos.z;
        double horizontalDistance = Math.sqrt(rawDx * rawDx + rawDz * rawDz);
        double lookHorizontalLength = Math.sqrt(lookDir.x * lookDir.x + lookDir.z * lookDir.z);
        boolean insideHorizontal;

        if (horizontalDistance <= 0.0001d) insideHorizontal = true;
        else if (lookHorizontalLength <= 0.0001d) insideHorizontal = true;
        else {
            double dirToNpcX = rawDx / horizontalDistance;
            double dirToNpcZ = rawDz / horizontalDistance;
            double forwardX = lookDir.x / lookHorizontalLength;
            double forwardZ = lookDir.z / lookHorizontalLength;
            double horizontalDot = forwardX * dirToNpcX + forwardZ * dirToNpcZ;
            horizontalDot = Math.clamp(horizontalDot, -1d, 1d);
            double horizontalAngle = Math.toDegrees(Math.acos(horizontalDot));
            insideHorizontal = horizontalAngle <= playerCombatHorizontalFovDegrees(global) * 0.5d;
        }

        double targetElevation = Math.atan2(rawDy, horizontalDistance);
        double lookElevation = Math.atan2(lookDir.y, lookHorizontalLength);
        double verticalAngleDelta = Math.toDegrees(targetElevation - lookElevation);
        boolean insideVertical = Math.abs(verticalAngleDelta) <= playerCombatVerticalFovDegrees(global) * 0.5d;

        return new CombatGeometry(insideHorizontal, insideVertical);
    }

    private boolean hasLineOfSightToNpc(
            Ref<EntityStore> playerRef,
            Store<EntityStore> store,
            TransformComponent npcTransform,
            GlobalConfig global
    ) {
        try {
            if (playerRef == null || store == null || npcTransform == null) return false;

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

            if (distance <= 0.0001d) return true;

            double dirX = dx / distance;
            double dirY = dy / distance;
            double dirZ = dz / distance;

            double maxDistance = Math.max(0d, distance - losTargetEpsilon(global));

            Vector3i blockingBlock = TargetUtil.getTargetBlock(
                world.getChunkStore(),
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

            logFailure(Level.FINE, "checking line of sight to NPC", ex);
            return false;
        }
    }

    private boolean hasLockedTarget(
            Ref<EntityStore> npcRef,
            Ref<EntityStore> playerRef,
            Store<EntityStore> store
    ) {
        try {
            if (npcRef == null || playerRef == null || store == null) return false;

            MarkedEntitySupport markedEntitySupport = MarkedEntitySupport.get(npcRef, store);

            Ref<EntityStore> lockedTarget = markedEntitySupport
                    .getMarkedEntityRef(MarkedEntitySupport.DEFAULT_TARGET_SLOT);

            return isSameValidRef(lockedTarget, playerRef);

        } catch (RuntimeException | AssertionError ex) {

            logFailure(Level.FINE, "resolving NPC locked target", ex);
            return false;
        }
    }

    private boolean hasDesiredTarget(
            Role role,
            Ref<EntityStore> playerRef
    ) {
        try {
            if (role == null || playerRef == null || role.getLastBodySteeringMotion() == null) return false;

            Ref<EntityStore> desiredTarget = role.getLastBodySteeringMotion().getDesiredTargetEntity();

            return isSameValidRef(desiredTarget, playerRef);

        } catch (RuntimeException ex) {

            logFailure(Level.FINE, "resolving NPC desired target", ex);
            return false;
        }
    }

    private boolean isExecutingAttack(
            Ref<EntityStore> npcRef,
            Store<EntityStore> store
    ) {
        try {
            if (npcRef == null || store == null) return false;

            CombatSupport combatSupport = store.getComponent(npcRef, CombatSupport.getComponentType());

            return combatSupport != null && combatSupport.isExecutingAttack();

        } catch (RuntimeException ex) {

            logFailure(Level.FINE, "checking whether NPC is executing an attack", ex);
            return false;
        }
    }

    private boolean isNpcPerformingAttack(
            Ref<EntityStore> npcRef,
            Store<EntityStore> store
    ) {
        try {
            List<InterpretedCombatData> combatData = CombatViewSystems.getCombatData(npcRef, store);

            for (InterpretedCombatData data : combatData) {
                if (data.isPerformingMeleeAttack() || data.isPerformingRangedAttack() || data.isCharging()) return true;
            }

            return false;

        } catch (RuntimeException ex) {

            logFailure(Level.FINE, "reading NPC combat data", ex);
            return false;
        }
    }

    private boolean isHostileTowardsPlayer(
            Ref<EntityStore> npcRef,
            Ref<EntityStore> playerRef,
            Store<EntityStore> store
    ) {
        try {
            if (npcRef == null || playerRef == null || store == null) return false;

            WorldSupport worldSupport = store.getComponent(npcRef, WorldSupport.getComponentType());

            return worldSupport != null && worldSupport.getAttitude(npcRef, playerRef, store) == Attitude.HOSTILE;

        } catch (RuntimeException ex) {

            logFailure(Level.FINE, "checking if NPC attitude is hostile against player", ex);
            return false;
        }
    }

    private boolean isWithinRange(
            TransformComponent playerTransform,
            TransformComponent npcTransform,
            float range
    ) {
        if (playerTransform == null || npcTransform == null || range <= 0f) return false;

        var playerPos = playerTransform.getPosition();
        var npcPos = npcTransform.getPosition();

        double dx = playerPos.x - npcPos.x;
        double dy = playerPos.y - npcPos.y;
        double dz = playerPos.z - npcPos.z;

        double rangeSq = (double) range * range;

        return dx * dx + dy * dy + dz * dz <= rangeSq;
    }

    private boolean isSameValidRef(
            Ref<EntityStore> a,
            Ref<EntityStore> b
    ) {
        return a != null && b != null && a.isValid() && b.isValid() && a.equals(b);
    }

    private record CombatGeometry(
            boolean insideHorizontalFov,
            boolean insideVerticalFov
    ) {
        private static CombatGeometry outside() {
            return new CombatGeometry(false, false);
        }
    }

    private void logFailure(
            Level level,
            String operation,
            Throwable throwable
    ) {
        plugin.getLogger()
                .at(level)
                .withCause(throwable)
                .log("Combat signal scan failed: " + operation);
    }
}