package com.tom.immersivehudplugin.visibility;

import com.hypixel.hytale.protocol.packets.interface_.HudComponent;
import com.tom.immersivehudplugin.context.PlayerTickContext;
import com.tom.immersivehudplugin.runtime.PlayerHudState;

public final class HudDeltaApplier {

    public void apply(PlayerTickContext tickContext, PlayerHudState state) {
        PlayerHudState.HudDelta delta = state.prepareHudDelta();

        if (!delta.changed()) {
            return;
        }

        if (!delta.toHide().isEmpty()) {
            tickContext.player().getHudManager().hideHudComponents(
                    tickContext.playerRef(),
                    delta.toHide().toArray(HudComponent[]::new)
            );
        }

        if (!delta.toShow().isEmpty()) {
            tickContext.player().getHudManager().showHudComponents(
                    tickContext.playerRef(),
                    delta.toShow().toArray(HudComponent[]::new)
            );
        }

        state.commitAppliedHidden(delta.effectiveHidden());
    }
}