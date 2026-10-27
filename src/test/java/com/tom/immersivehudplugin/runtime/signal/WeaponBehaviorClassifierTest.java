package com.tom.immersivehudplugin.runtime.signal;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemTool;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.RootInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.none.ReplaceInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.none.SelectInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.selector.StabSelector;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.DamageEntityInteraction;
import com.hypixel.hytale.server.core.modules.projectile.interaction.ProjectileInteraction;

import java.util.Map;
import java.util.ArrayList;
import java.util.List;

/** No test framework dependency; uses real server interaction graphs without starting a world. */
public final class WeaponBehaviorClassifierTest {
    private static final InteractionMap INTERACTIONS = new InteractionMap();
    private static final RootMap ROOTS = new RootMap();
    private static int checks;

    static void main() throws Exception {
        var output = System.out;
        try {
            runChecks();
        } catch (Throwable error) {
            error.printStackTrace(output);
            throw error;
        }
    }

    private static void runChecks() throws Exception {
        com.hypixel.hytale.server.core.Options.parse(new String[0]);
        installStore(Interaction.class,
                HytaleAssetStore.builder(Interaction.class, INTERACTIONS)
                        .setCodec(Interaction.CODEC).setKeyFunction(Interaction::getId)
                        .setReplaceOnRemove(SimpleInteraction::new).build());
        installStore(RootInteraction.class,
                HytaleAssetStore.builder(RootInteraction.class, ROOTS)
                        .setCodec(RootInteraction.CODEC).setKeyFunction(RootInteraction::getId)
                        .setReplaceOnRemove(RootInteraction::new).build());

        INTERACTIONS.add("shot", new ProjectileInteraction());
        INTERACTIONS.add("ballisticShot", new BallisticShot());
        INTERACTIONS.add("damage", new DamageEntityInteraction());
        INTERACTIONS.add("strike", new Strike("damageRoot"));
        INTERACTIONS.add("replace", new Replacement("Attack", "empty"));
        INTERACTIONS.add("defaultShot", new Replacement("Attack", "shotRoot"));
        INTERACTIONS.add("abilityShot", new com.hypixel.hytale.builtin.abilities.interaction.LaunchAbilityProjectileInteraction());
        INTERACTIONS.add("nested", new Next("replace"));
        INTERACTIONS.add("cycle", new Next("cycle"));
        ROOTS.add("shotRoot", "shot");
        ROOTS.add("ballisticShotRoot", "ballisticShot");
        ROOTS.add("damageRoot", "damage");
        ROOTS.add("strikeRoot", "strike");
        ROOTS.add("variableRoot", "nested");
        ROOTS.add("defaultShotRoot", "defaultShot");
        ROOTS.add("abilityShotRoot", "abilityShot");
        ROOTS.add("empty");
        ROOTS.add("cycleRoot", "cycle");

        expect("empty hand", null, false, false);
        expect("unfamiliar ranged weapon without tags", new TestItem("shotRoot"), true, false);
        expect("ballistic provider without concrete projectile class", new TestItem("ballisticShotRoot"), true, false);
        expect("unfamiliar melee weapon without tags", new TestItem("strikeRoot"), false, true);
        TestItem hybrid = new TestItem("strikeRoot");
        hybrid.actions(Map.of(InteractionType.Primary, "strikeRoot", InteractionType.Secondary, "shotRoot"));
        expect("hybrid weapon", hybrid, true, true);
        expect("damage alone is not melee", new TestItem("damageRoot"), false, false);
        TestItem variable = new TestItem("variableRoot");
        variable.variables(Map.of("Attack", "shotRoot"));
        expect("nested item variable", variable, true, false);
        variable.variables(Map.of("Attack", "strikeRoot"));
        HeldItemState.invalidateWeaponCache();
        expect("cache refresh after asset reload", variable, false, true);
        expect("default variable", new TestItem("variableRoot"), false, false);
        expect("default projectile variable", new TestItem("defaultShotRoot"), true, false);
        TestItem override = new TestItem("defaultShotRoot");
        override.variables(Map.of("Attack", "empty"));
        expect("overridden default does not count", override, false, false);
        TestItem ability = new TestItem("empty");
        ability.actions(Map.of(InteractionType.Ability1, "abilityShotRoot"));
        expect("ability projectile", ability, true, false);
        expect("missing asset", new TestItem("missing"), false, false);
        expect("cyclic graph", new TestItem("cycleRoot"), false, false);
        TestItem legacyBow = new TestItem("missing");
        legacyBow.family("Bow");
        expect("ranged family alone does not classify a weapon", legacyBow, false, false);
        TestItem legacySword = new TestItem("missing");
        legacySword.family("Sword");
        expect("melee family alone does not classify a weapon", legacySword, false, false);
        TestItem tool = new TestItem("strikeRoot");
        tool.asTool();
        expect("incidental tool damage", tool, false, false);
        TestItem placeable = new TestItem("shotRoot");
        placeable.asBlock();
        expect("placeable projectile action", placeable, false, false);
        TestItem consumable = new TestItem("shotRoot");
        consumable.asConsumable();
        expect("consumable projectile action", consumable, false, false);
        TestItem passive = new TestItem("empty");
        passive.actions(Map.of(InteractionType.Held, "shotRoot"));
        expect("passive effect is not an attack", passive, false, false);
        TestItem oldGun = new TestItem("empty");
        oldGun.actions(Map.of(InteractionType.Secondary, "Gun_Attack"));
        expect("Gun_Attack name alone does not classify a weapon", oldGun, false, false);
        System.out.println("Passed " + checks + " weapon classification regression checks.");
    }

    private static void expect(String name, Item item, boolean ranged, boolean melee) {
        if (HeldItemState.isRangedWeapon(item) != ranged || HeldItemState.isMeleeWeapon(item) != melee) {
            throw new AssertionError(name + ": unexpected classification");
        }
        checks++;
    }

    // Inject isolated stores so these checks do not need a running HytaleServer/event bus.
    private static void installStore(Class<?> assetClass, Object store) throws Exception {
        var field = assetClass.getDeclaredField("ASSET_STORE");
        field.setAccessible(true);
        field.set(null, store);
    }

    private static final class InteractionMap extends IndexedLookupTableAssetMap<String, Interaction> {
        private final List<String> ids = new ArrayList<>();
        InteractionMap() { super(Interaction[]::new); }
        void add(String id, Interaction interaction) { assetMap.put(id, interaction); ids.add(id); }
        @Override public int getIndex(String id) { return ids.indexOf(id); }
        @Override public Interaction getAsset(int index) {
            return index < 0 || index >= ids.size() ? null : assetMap.get(ids.get(index));
        }
    }

    private static final class RootMap extends IndexedLookupTableAssetMap<String, RootInteraction> {
        private final List<String> ids = new ArrayList<>();
        RootMap() { super(RootInteraction[]::new); }
        void add(String id, String... interactions) {
            assetMap.put(id, new RootInteraction(id, interactions)); ids.add(id);
        }
        @Override public int getIndex(String id) { return ids.indexOf(id); }
        @Override public RootInteraction getAsset(int index) {
            return index < 0 || index >= ids.size() ? null : assetMap.get(ids.get(index));
        }
    }

    private static final class TestItem extends Item {
        TestItem(String root) {
            super("test");
            interactions = Map.of(InteractionType.Primary, root);
        }
        void actions(Map<InteractionType, String> actions) { interactions = actions; }
        void variables(Map<String, String> variables) { interactionVars = variables; }
        void family(String family) {
            data = new AssetExtraInfo.Data(Item.class, "test", null);
            data.putTags(Map.of("Type", new String[]{"Weapon"}, "Family", new String[]{family}));
        }
        void asTool() {
            tool = new ItemTool(
                    new com.hypixel.hytale.server.core.asset.type.item.config.ItemToolSpec[0],
                    1f, new ItemTool.DurabilityLossBlockTypes[0]);
        }
        void asBlock() { blockId = "testBlock"; }
        @Override public boolean isConsumable() { return consumable; }
        private boolean consumable;
        void asConsumable() { consumable = true; }
    }

    private static final class Strike extends SelectInteraction {
        Strike(String hitRoot) {
            selector = new StabSelector();
            hitEntity = hitRoot;
        }
    }

    private static final class Next extends SimpleInteraction {
        Next(String nextId) { next = nextId; }
    }

    private static final class BallisticShot extends SimpleInteraction
            implements com.hypixel.hytale.server.core.modules.projectile.config.BallisticDataProvider {
        @Override
        public com.hypixel.hytale.server.core.modules.projectile.config.BallisticData getBallisticData() {
            throw new AssertionError("Classification must not resolve projectile data");
        }
    }

    private static final class Replacement extends ReplaceInteraction {
        Replacement(String name, String fallback) {
            variable = name;
            defaultValue = fallback;
        }
    }
}
