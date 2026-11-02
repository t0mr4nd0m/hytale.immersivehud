package com.tom.immersivehudplugin.ui;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Tracks received input before the world's deferred movement updates are applied. */
public final class HudShortcutInputState {
    private record Snapshot(UUID world, boolean crouching) {}
    private final Map<UUID, Snapshot> players = new ConcurrentHashMap<>();

    public void update(UUID player, UUID world, boolean crouching) {
        players.put(player, new Snapshot(world, crouching));
    }

    public boolean isCrouching(UUID player, UUID world) {
        Snapshot snapshot = players.get(player);
        return snapshot != null && snapshot.world().equals(world) && snapshot.crouching();
    }

    public void remove(UUID player) { players.remove(player); }
    public void clear() { players.clear(); }
}
