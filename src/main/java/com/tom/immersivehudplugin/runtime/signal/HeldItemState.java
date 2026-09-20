package com.tom.immersivehudplugin.runtime.signal;

import com.hypixel.hytale.server.core.asset.type.item.config.Item;

import java.util.Set;

import static com.hypixel.hytale.protocol.InteractionType.Secondary;

public final class HeldItemState {

    private static final Set<String> RANGED_WEAPONS = Set.of(
            "Bow", "Crossbow", "Staff", "Arrow", "Wand", "Spear", "Bomb", "Gun", "Rifle"
    );
    private static final Set<String> MELEE_WEAPONS = Set.of(
            "Dagger", "Sword", "Axe", "Hammer", "Mace", "Spear", "Club", "Stick"
    );

    private HeldItemState() {}

    public static boolean isRangedWeapon(Item item) {
        if (item == null) return false;

        // TODO: Classify ranged weapons by their attack behavior instead of hard-coded families.
        boolean rangedFamily = isWeapon(item)
                && checkItemFamily(item, RANGED_WEAPONS);

        boolean gunAttack = item.getInteractions() != null
                && "Gun_Attack".equals(item.getInteractions().get(Secondary));

        return rangedFamily || gunAttack;
    }

    public static boolean isMeleeWeapon(Item item) {
        // TODO: Classify melee weapons by their attack behavior instead of hard-coded families.
        return isWeapon(item) && checkItemFamily(item, MELEE_WEAPONS);
    }

    public static boolean isWeapon(Item item) {
        return "Weapon".equalsIgnoreCase(getItemType(item));
    }

    public static boolean isConsumable(Item item) {
        return item != null && item.isConsumable();
    }

    public static String getItemType(Item item) {
        if (item == null || item.getData() == null) { return ""; }

        String[] type = item.getData().getRawTags().get("Type");

        if (type == null || type.length == 0 || type[0] == null) { return ""; }

        return type[0].trim();
    }

    public static boolean checkItemFamily(Item item, Set<String> familiesList) {

        if (item == null || item.getData() == null || familiesList == null) { return false; }

        String[] families = item.getData().getRawTags().get("Family");
        if (families == null) { return false; }

        for (String family : families) {

            if (family == null) { continue; }

            String s = family.trim();
            if (familiesList.stream().anyMatch(s::equalsIgnoreCase)) { return true; }

        }
        return false;
    }
}