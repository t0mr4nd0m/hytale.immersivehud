package com.tom.immersivehudplugin.utils;

import com.hypixel.hytale.server.core.asset.type.item.config.Item;

import java.util.Set;

public final class ItemInHand {

    private static final Set<String> RANGED_WEAPONS = Set.of(
            "Bow", "Crossbow", "Staff", "Arrow", "Gun", "Bomb", "Wand", "Spear"
    );
    private static final Set<String> MELEE_WEAPONS = Set.of(
            "Dagger", "Sword", "Axe", "Hammer", "Mace", "Spear"
    );

    public static boolean isRangedWeapon(Item item) {
        return isWeapon(item) && checkItemFamily(item, RANGED_WEAPONS);
    }

    public static boolean isMeleeWeapon(Item item) {
        return isWeapon(item) && checkItemFamily(item, MELEE_WEAPONS);
    }

    public static boolean isWeapon(Item item) {
        return getItemType(item).equalsIgnoreCase("Weapon");
    }

    public static boolean isConsumable(Item item) {
        return item != null && item.isConsumable();
    }

    public static String getItemType(Item item) {
        return item == null? "<null>" : item.getData().getRawTags().get("Type")[0].trim();
    }

    public static boolean checkItemFamily(Item item, Set<String> familiesList) {

        if (item == null) return false;

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