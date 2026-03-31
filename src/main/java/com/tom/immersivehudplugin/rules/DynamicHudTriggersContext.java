package com.tom.immersivehudplugin.rules;

import com.tom.immersivehudplugin.utils.HudBarState;

public record DynamicHudTriggersContext(
        boolean hotbarInput,
        boolean chargingWeapon,
        boolean consumableUse,
        boolean targetEntity,
        boolean interactableBlock,

        boolean playerMoving,
        boolean playerWalking,
        boolean playerRunning,
        boolean playerSprinting,
        boolean playerMounting,
        boolean playerSwimming,
        boolean playerFlying,
        boolean playerGliding,
        boolean playerJumping,
        boolean playerCrouching,
        boolean playerClimbing,
        boolean playerFalling,
        boolean playerRolling,
        boolean playerIdle,
        boolean playerSitting,
        boolean playerSleeping,
        boolean playerInFluid,
        boolean playerOnGround,

        boolean holdingRangedWeapon,
        boolean holdingMeleeWeapon,

        HudBarState healthBar,
        HudBarState staminaBar,
        HudBarState manaBar,
        HudBarState oxygenBar
) { }