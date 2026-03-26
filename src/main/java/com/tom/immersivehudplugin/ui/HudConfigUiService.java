package com.tom.immersivehudplugin.ui;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.tom.immersivehudplugin.ImmersiveHudPlugin;
import com.tom.immersivehudplugin.config.PlayerConfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class HudConfigUiService {

    private final ImmersiveHudPlugin plugin;
    private final Map<UUID, HudConfigUiSession> sessions = new ConcurrentHashMap<>();

    public HudConfigUiService(@Nonnull ImmersiveHudPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(
            @Nonnull PlayerRef playerRef,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store
    ) {
        PlayerConfig playerCfg = plugin.requirePlayerConfig(playerRef);
        if (playerCfg == null) {
            return;
        }

        sessions.put(playerRef.getUuid(), new HudConfigUiSession(playerCfg));

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }

        player.getPageManager().openCustomPage(ref, store, new HudConfigPage(plugin, this, playerRef));
    }

    @Nullable
    public HudConfigUiSession getSession(@Nonnull PlayerRef playerRef) {
        return sessions.get(playerRef.getUuid());
    }

    public void discard(@Nonnull PlayerRef playerRef) {
        sessions.remove(playerRef.getUuid());
    }

    public boolean apply(@Nonnull PlayerRef playerRef) {

        HudConfigUiSession session = getSession(playerRef);
        if (session == null) {
            return false;
        }

        PlayerConfig live = plugin.requirePlayerConfig(playerRef);
        if (live == null) {
            return false;
        }

        live.setHudComponents(session.getDraftHudComponents().copy());
        live.setDynamicHud(session.getDraftDynamicHud().copy());

        plugin.markPlayerConfigDirty(playerRef.getUuid());
        plugin.savePlayerConfigAsync(playerRef.getUuid());
        plugin.markPlayerStaticHudDirty(playerRef);

        discard(playerRef);
        return true;
    }

    public void closePage(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store
    ) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player != null) {
            player.getPageManager().setPage(ref, store, Page.None);
        }
    }
}