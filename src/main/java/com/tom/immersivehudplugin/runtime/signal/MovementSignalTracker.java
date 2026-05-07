package com.tom.immersivehudplugin.runtime.signal;

import com.hypixel.hytale.protocol.MovementStates;
import com.tom.immersivehudplugin.hud.trigger.HudTrigger;
import com.tom.immersivehudplugin.runtime.PlayerHudState;
import com.tom.immersivehudplugin.runtime.context.PlayerTickContext;

public final class MovementSignalTracker {

    public void updateMovementSignals(
            PlayerHudState state,
            PlayerTickContext tickContext,
            long now,
            int hideDelay
    ) {
        MovementStates movementStates = tickContext.movement() != null
                ? tickContext.movement().getMovementStates()
                : null;

        if (movementStates != null) {
            pulseDirectMovementSignals(state, movementStates, now, hideDelay);
        }
    }

    private void pulseDirectMovementSignals(
            PlayerHudState state,
            MovementStates movementStates,
            long now,
            int hideDelay
    ) {
        if (isMoving(movementStates)) state.t.pulse(HudTrigger.PLAYER_MOVING, now, hideDelay);
        if (movementStates.walking) state.t.pulse(HudTrigger.PLAYER_WALKING, now, hideDelay);
        if (movementStates.running) state.t.pulse(HudTrigger.PLAYER_RUNNING, now, hideDelay);
        if (movementStates.sprinting) state.t.pulse(HudTrigger.PLAYER_SPRINTING, now, hideDelay);
        if (movementStates.swimming) state.t.pulse(HudTrigger.PLAYER_SWIMMING, now, hideDelay);
        if (movementStates.mounting) state.t.pulse(HudTrigger.PLAYER_MOUNTING, now, hideDelay);
        if (movementStates.flying) state.t.pulse(HudTrigger.PLAYER_FLYING, now, hideDelay);
        if (movementStates.gliding) state.t.pulse(HudTrigger.PLAYER_GLIDING, now, hideDelay);
        if (movementStates.jumping) state.t.pulse(HudTrigger.PLAYER_JUMPING, now, hideDelay);
        if (movementStates.climbing) state.t.pulse(HudTrigger.PLAYER_CLIMBING, now, hideDelay);
        if (movementStates.falling) state.t.pulse(HudTrigger.PLAYER_FALLING, now, hideDelay);
        if (movementStates.rolling) state.t.pulse(HudTrigger.PLAYER_ROLLING, now, hideDelay);
        if (movementStates.crouching) state.t.pulse(HudTrigger.PLAYER_CROUCHING, now, hideDelay);
        if (movementStates.idle) state.t.pulse(HudTrigger.PLAYER_IDLE, now, hideDelay);
        if (movementStates.sitting) state.t.pulse(HudTrigger.PLAYER_SITTING, now, hideDelay);
        if (movementStates.sleeping) state.t.pulse(HudTrigger.PLAYER_SLEEPING, now, hideDelay);
        if (movementStates.inFluid) state.t.pulse(HudTrigger.PLAYER_IN_FLUID, now, hideDelay);
        if (movementStates.onGround) state.t.pulse(HudTrigger.PLAYER_ON_GROUND, now, hideDelay);
    }

    private boolean isMoving(MovementStates movementStates) {
        return movementStates.walking
                || movementStates.running
                || movementStates.sprinting
                || movementStates.swimming
                || movementStates.mounting
                || movementStates.flying
                || movementStates.gliding
                || movementStates.jumping
                || movementStates.climbing
                || movementStates.falling
                || movementStates.fallingFar
                || movementStates.rolling
                || movementStates.swimJumping
                || movementStates.mantling
                || movementStates.sliding;
    }
}