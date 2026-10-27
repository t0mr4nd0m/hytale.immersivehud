package com.tom.immersivehudplugin.runtime.signal;

import com.hypixel.hytale.builtin.abilities.interaction.LaunchAbilityProjectileInteraction;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.RaycastSelector;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.InteractionManager;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.RootInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.data.Collector;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.data.CollectorTag;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.data.StringTag;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.none.SelectInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.DamageEntityInteraction;
import com.hypixel.hytale.server.core.modules.projectile.config.BallisticDataProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumSet;
import java.util.Map;
import java.util.WeakHashMap;

/** Inspects attack definitions without executing any interaction. */
final class WeaponBehaviorClassifier {
    record Classification(boolean ranged, boolean melee) {
        static final Classification NONE = new Classification(false, false);
    }

    private static final EnumSet<InteractionType> ATTACK_INPUTS = EnumSet.of(
            InteractionType.Primary, InteractionType.Secondary,
            InteractionType.Ability1, InteractionType.Ability2,
            InteractionType.Ability3, InteractionType.Ability4
    );
    private static final Map<Item, Classification> CACHE = new WeakHashMap<>();

    static synchronized Classification classify(@Nullable Item item) {
        if (item == null) return Classification.NONE;
        return CACHE.computeIfAbsent(item, WeaponBehaviorClassifier::analyze);
    }

    static synchronized void clearCache() {
        CACHE.clear();
    }

    private static Classification analyze(Item item) {
        // Tools, consumables and placeable can have incidental combat actions.
        boolean declaredWeapon = item.getWeapon() != null || HeldItemState.isWeapon(item);
        if (!declaredWeapon && (item.getTool() != null || item.isConsumable() || item.hasBlockType())) {
            return Classification.NONE;
        }
        if (item.getInteractions() == null) return Classification.NONE;

        InteractionContext context = InteractionContext.withoutEntity();
        context.setInteractionVarsGetter(ignored -> item.getInteractionVars());
        boolean ranged = false;
        boolean melee = false;
        for (var entry : item.getInteractions().entrySet()) {
            if (!ATTACK_INPUTS.contains(entry.getKey()) || entry.getValue() == null) continue;
            AttackCollector collector = new AttackCollector();
            try {
                RootInteraction root = RootInteraction.getAssetMap().getAsset(entry.getValue());
                if (root == null || root.getInteractionIds() == null) continue;
                InteractionManager.walkInteractions(collector, context, CollectorTag.ROOT, root.getInteractionIds());
            } catch (RuntimeException ignored) {
                // Missing assets, custom walkers requiring an entity, or malformed graphs
                // must not interrupt HUD updates. Keep evidence already collected.
            }
            ranged |= collector.ranged;
            melee |= collector.melee;
        }
        return new Classification(ranged, melee);
    }

    static final class AttackCollector implements Collector {
        private static final int MAX_DEPTH = 64;
        private static final int MAX_VISITS = 2048;
        private final Deque<Frame> path = new ArrayDeque<>();
        private int visits;
        private boolean pendingMeleeTarget;
        boolean ranged;
        boolean melee;

        @Override
        public boolean collect(
                @Nonnull CollectorTag tag,
                @Nonnull InteractionContext context,
                @Nonnull Interaction interaction
        ) {
            if (++visits > MAX_VISITS || path.size() >= MAX_DEPTH) {
                throw new IllegalStateException("Attack graph traversal limit reached");
            }
            Frame parent = path.peek();
            pendingMeleeTarget = parent != null && parent.meleeTarget();
            if (parent != null && parent.interaction() instanceof SelectInteraction select) {
                // Only damage on an entity-hit branch proves a melee attack.
                boolean entityHit = StringTag.of("HitEntity").equals(tag)
                        || tag instanceof SelectInteraction.HitEntityRuleTag;
                var packet = (com.hypixel.hytale.protocol.SelectInteraction) select.toPacket();
                pendingMeleeTarget = entityHit && !(packet.selector instanceof RaycastSelector);
            }
            // Both legacy LaunchProjectile and current Projectile implement this interface.
            if (interaction instanceof BallisticDataProvider
                    || interaction instanceof LaunchAbilityProjectileInteraction) {
                ranged = true;
            }
            if (interaction instanceof DamageEntityInteraction && pendingMeleeTarget) {
                melee = true;
            }
            return false;
        }

        @Override
        public void into(@Nonnull InteractionContext context, @Nullable Interaction interaction) {
            path.push(new Frame(interaction, pendingMeleeTarget));
        }

        @Override
        public void outof() {
            path.pop();
        }

        @Override public void start() { }
        @Override public void finished() { }

        private record Frame(@Nullable Interaction interaction, boolean meleeTarget) { }
    }
}
