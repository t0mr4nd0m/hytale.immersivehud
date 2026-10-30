package com.tom.immersivehudplugin.runtime.signal;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.spatial.SpatialStructure;
import com.hypixel.hytale.server.core.asset.type.attitude.Attitude;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.hypixel.hytale.server.npc.blackboard.view.combat.CombatViewSystems;
import com.hypixel.hytale.server.npc.blackboard.view.combat.InterpretedCombatData;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.role.support.CombatSupport;
import com.hypixel.hytale.server.npc.role.support.MarkedEntitySupport;
import com.hypixel.hytale.server.npc.role.support.WorldSupport;
import com.tom.immersivehudplugin.config.GlobalConfig;
import org.joml.Vector3i;
import org.joml.Vector3dc;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public final class CombatSignalScanner {

    private final JavaPlugin plugin;

    public CombatSignalScanner(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean scanNpcCombat(
            Store<EntityStore> store,
            Ref<EntityStore> playerRef,
            float scanRange,
            GlobalConfig global
    ) {
        if (store == null || playerRef == null || !playerRef.isValid()
                || !Float.isFinite(scanRange) || scanRange <= 0f) return false;

        TransformComponent playerTransform =
            store.getComponent(playerRef, TransformComponent.getComponentType());

        if (playerTransform == null) return false;

        var npcComponentType = NPCEntity.getComponentType();
        if (npcComponentType == null) return false;

        var spatial = store.getResource(NPCPlugin.get().getNpcSpatialResource());
        List<Ref<EntityStore>> candidates = collectNearby(spatial.getSpatialStructure(), playerTransform.getPosition(), scanRange);
        ScanContext context = new ScanContext(store, playerRef, global);
        for (Ref<EntityStore> npcRef : candidates) {
            if (!npcRef.isValid()) continue;
            TransformComponent npcTransform = store.getComponent(npcRef, TransformComponent.getComponentType());
            // Spatial data is a tick snapshot; validate the current position and components.
            if (!isWithinRange(playerTransform, npcTransform, scanRange)) continue;
            NPCEntity npc = store.getComponent(npcRef, npcComponentType);
            if (npc == null || npc.getRole() == null) continue;
            if (scanNpc(npcRef, npc.getRole(), npcTransform, context)) return true;
        }
        return false;
    }

    static <T> List<T> collectNearby(SpatialStructure<T> spatial, Vector3dc center, float range) {
        List<T> candidates = new ArrayList<>();
        // KDTree.collect uses a strict radius, whereas the existing range test is inclusive.
        spatial.collect(center, Math.nextUp((double) range), candidates);
        return candidates;
    }

    private boolean scanNpc(
            Ref<EntityStore> npcRef,
            Role role,
            TransformComponent npcTransform,
            ScanContext context
    ) {
        Ref<EntityStore> playerRef = context.playerRef;
        Store<EntityStore> store = context.store;
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

        CombatScanView view = context.view();
        return view.insideFov(npcTransform.getPosition()) && hasLineOfSightToNpc(store, npcTransform, view);
    }

    private boolean hasLineOfSightToNpc(
            Store<EntityStore> store,
            TransformComponent npcTransform,
            CombatScanView view
    ) {
        try {
            EntityStore entityStore = store.getExternalData();
            World world = entityStore.getWorld();

            var npcPos = npcTransform.getPosition();

            double originX = view.originX();
            double originY = view.originY();
            double originZ = view.originZ();

            double targetX = npcPos.x;
            double targetY = npcPos.y + view.targetHeight();
            double targetZ = npcPos.z;

            double dx = targetX - originX;
            double dy = targetY - originY;
            double dz = targetZ - originZ;

            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

            if (distance <= 0.0001d) return true;

            double dirX = dx / distance;
            double dirY = dy / distance;
            double dirZ = dz / distance;

            double maxDistance = Math.max(0d, distance - view.targetEpsilon());

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

            logFailure("checking line of sight to NPC", ex);
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

            logFailure("resolving NPC locked target", ex);
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

            logFailure("resolving NPC desired target", ex);
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

            logFailure("checking whether NPC is executing an attack", ex);
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

            logFailure("reading NPC combat data", ex);
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

            logFailure("checking if NPC attitude is hostile against player", ex);
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

    private static final class ScanContext {
        private final Store<EntityStore> store;
        private final Ref<EntityStore> playerRef;
        private final GlobalConfig config;
        private CombatScanView view;

        private ScanContext(Store<EntityStore> store, Ref<EntityStore> playerRef, GlobalConfig config) {
            this.store = store;
            this.playerRef = playerRef;
            this.config = config;
        }

        private CombatScanView view() {
            // Attackers are detected without needing geometry; resolve the look only on demand.
            if (view == null) {
                var look = TargetUtil.getLook(playerRef, store);
                view = CombatScanView.capture(look.getPosition(), look.getDirection(), config);
            }
            return view;
        }
    }

    private void logFailure(
            String operation,
            Throwable throwable
    ) {
        plugin.getLogger()
                .at(Level.WARNING)
                .withCause(throwable)
                .log("Combat signal scan failed: " + operation);
    }
}
