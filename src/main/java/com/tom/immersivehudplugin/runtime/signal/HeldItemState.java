package com.tom.immersivehudplugin.runtime.signal;

import com.hypixel.hytale.server.core.asset.type.item.config.Item;

public final class HeldItemState {

    private HeldItemState() {}

    public static boolean isRangedWeapon(Item item) {
        return WeaponBehaviorClassifier.classify(item).ranged();
    }

    public static boolean isMeleeWeapon(Item item) {
        return WeaponBehaviorClassifier.classify(item).melee();
    }

    public static void invalidateWeaponCache() {
        WeaponBehaviorClassifier.clearCache();
    }

    public static boolean isWeapon(Item item) {
        return "Weapon".equalsIgnoreCase(getItemType(item));
    }

    public static boolean isConsumable(Item item) {
        return item != null && item.isConsumable();
    }

    public static boolean isBuildingBlock(Item item) {
        return item != null && item.hasBlockType();
    }

    public static String getItemType(Item item) {
        if (item == null || item.getData() == null) { return ""; }

        String[] type = item.getData().getRawTags().get("Type");

        if (type == null || type.length == 0 || type[0] == null) { return ""; }

        return type[0].trim();
    }

}
