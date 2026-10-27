package com.tom.immersivehudplugin.ui;

import com.hypixel.hytale.protocol.packets.serveraccess.Access;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Owns only pauses initiated by the configuration UI. */
final class HudConfigPauseController {
    interface PauseTarget {
        boolean isPaused();
        void setPaused(boolean paused);
        void execute(@Nonnull Runnable action);
    }

    private final Map<UUID, PauseTarget> ownedPauses = new HashMap<>();

    static boolean canPause(boolean singleplayer, boolean owner, @Nullable Access access,
                            @Nullable Access requestedAccess, int playerCount) {
        // SingleplayerModule leaves access fields null until sharing is configured.
        return singleplayer && owner && (access == null || access == Access.Private)
                && (requestedAccess == null || requestedAccess == Access.Private) && playerCount == 1;
    }

    synchronized void pause(UUID playerId, PauseTarget target, boolean permitted) {
        if (!permitted || ownedPauses.containsKey(playerId) || target.isPaused()) return;

        target.setPaused(true);
        ownedPauses.put(playerId, target);
    }

    synchronized void release(UUID playerId) {
        PauseTarget target = ownedPauses.remove(playerId);
        if (target == null) return;

        target.execute(() -> {
            if (target.isPaused()) target.setPaused(false);
        });
    }

    synchronized void releaseAll() {
        for (UUID playerId : new ArrayList<>(ownedPauses.keySet())) {
            release(playerId);
        }
    }
}
