package com.tom.immersivehudplugin.runtime;

import com.tom.immersivehudplugin.context.PlayerTickContext;

public final class MovementSignalTracker {

    public void updateMovementSignals(
            PlayerHudState state,
            PlayerTickContext tickContext,
            long now,
            int hideDelay
    ) {
        var movementStates = tickContext.movement() != null ? tickContext.movement().getMovementStates() : null;
        if (movementStates == null) {
            return;
        }

        boolean isMoving =
                movementStates.walking
                        || movementStates.running
                        || movementStates.sprinting
                        || movementStates.swimming
                        || movementStates.mounting
                        || movementStates.flying
                        || movementStates.gliding
                        || movementStates.jumping
                        || movementStates.climbing
                        || movementStates.falling
                        || movementStates.rolling;

        if (isMoving) state.t.pulse(HudSignal.PLAYER_MOVING, now, hideDelay);
        if (movementStates.walking) state.t.pulse(HudSignal.PLAYER_WALKING, now, hideDelay);
        if (movementStates.running) state.t.pulse(HudSignal.PLAYER_RUNNING, now, hideDelay);
        if (movementStates.sprinting) state.t.pulse(HudSignal.PLAYER_SPRINTING, now, hideDelay);
        if (movementStates.swimming) state.t.pulse(HudSignal.PLAYER_SWIMMING, now, hideDelay);
        if (movementStates.mounting) state.t.pulse(HudSignal.PLAYER_MOUNTING, now, hideDelay);
        if (movementStates.flying) state.t.pulse(HudSignal.PLAYER_FLYING, now, hideDelay);
        if (movementStates.gliding) state.t.pulse(HudSignal.PLAYER_GLIDING, now, hideDelay);
        if (movementStates.jumping) state.t.pulse(HudSignal.PLAYER_JUMPING, now, hideDelay);
        if (movementStates.climbing) state.t.pulse(HudSignal.PLAYER_CLIMBING, now, hideDelay);
        if (movementStates.falling) state.t.pulse(HudSignal.PLAYER_FALLING, now, hideDelay);
        if (movementStates.rolling) state.t.pulse(HudSignal.PLAYER_ROLLING, now, hideDelay);
        if (movementStates.crouching) state.t.pulse(HudSignal.PLAYER_CROUCHING, now, hideDelay);
        if (movementStates.idle) state.t.pulse(HudSignal.PLAYER_IDLE, now, hideDelay);
        if (movementStates.sitting) state.t.pulse(HudSignal.PLAYER_SITTING, now, hideDelay);
        if (movementStates.sleeping) state.t.pulse(HudSignal.PLAYER_SLEEPING, now, hideDelay);
        if (movementStates.inFluid) state.t.pulse(HudSignal.PLAYER_IN_FLUID, now, hideDelay);
        if (movementStates.onGround) state.t.pulse(HudSignal.PLAYER_ON_GROUND, now, hideDelay);
    }
}