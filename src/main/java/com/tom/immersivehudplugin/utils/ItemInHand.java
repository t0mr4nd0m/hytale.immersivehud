package com.tom.immersivehudplugin.utils;

import com.hypixel.hytale.server.core.asset.type.item.config.Item;

import java.util.Set;

public final class ItemInHand {

    private static final Set<String> RANGED_WEAPONS = Set.of("Bow", "Crossbow", "Staff", "Arrow");

    private static final Set<String> MELEE_WEAPONS = Set.of("Dagger", "Sword", "Axe", "Hammer", "Mace", "Spear");

    public static boolean isRangedWeapon(Item item) {

        if (item == null) {
            return false;
        }

        String[] families = item.getData().getRawTags().get("Family");
        if (families == null) {
            return false;
        }

        for (String family : families) {

            if (family == null) {
                continue;
            }

            String s = family.trim();

            return RANGED_WEAPONS.stream().anyMatch(s::equalsIgnoreCase);
        }
        return false;
    }

    public static boolean isMeleeWeapon(Item item) {

        if (item == null) {
            return false;
        }

        String[] families = item.getData().getRawTags().get("Family");
        if (families == null) {
            return false;
        }

        for (String family : families) {

            if (family == null) {
                continue;
            }

            String s = family.trim();
            return MELEE_WEAPONS.stream().anyMatch(s::equalsIgnoreCase);
        }
        return false;
    }
}