package com.tom.immersivehudplugin.rules;

import com.tom.immersivehudplugin.utils.HudBarState;

public record DynamicHudTriggersContext(boolean hotbarInput, boolean chargingWeapon, boolean consumableUse,
                                        boolean targetEntity, boolean interactableBlock, boolean playerMoving,
                                        boolean playerWalking, boolean playerRunning, boolean playerSprinting,
                                        boolean playerMounting, boolean playerSwimming, boolean holdingRangedWeapon,
                                        boolean holdingMeleeWeapon, HudBarState healthBar, HudBarState staminaBar,
                                        HudBarState manaBar) {
}