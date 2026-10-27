package com.tom.immersivehudplugin.ui;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.protocol.packets.serveraccess.Access;
import com.hypixel.hytale.server.core.Constants;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.modules.singleplayer.SingleplayerModule;
import com.hypixel.hytale.server.core.modules.singleplayer.SingleplayerRequestAccessEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.tom.immersivehudplugin.config.PlayerConfig;
import com.tom.immersivehudplugin.config.PlayerConfigService;
import com.tom.immersivehudplugin.runtime.HudRuntimeService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class HudConfigUiService {

    private final PlayerConfigService playerConfigService;
    private final HudRuntimeService hudRuntimeService;

    private final Map<UUID, HudConfigUiSession> sessions = new ConcurrentHashMap<>();
    private final HudConfigPauseController pauseController = new HudConfigPauseController();

    public HudConfigUiService(
            @Nonnull PlayerConfigService playerConfigService,
            @Nonnull HudRuntimeService hudRuntimeService
    ) {
        this.playerConfigService = playerConfigService;
        this.hudRuntimeService = hudRuntimeService;
    }

    public void start(@Nonnull JavaPlugin plugin) {
        plugin.getEventRegistry().registerGlobal(PlayerDisconnectEvent.class,
                event -> discard(event.getPlayerRef()));
        plugin.getEventRegistry().registerGlobal(PlayerReadyEvent.class, event -> {
            if (Universe.get().getPlayerCount() > 1) pauseController.releaseAll();
        });
        plugin.getEventRegistry().register(SingleplayerRequestAccessEvent.class, event -> {
            if (event.getAccess() != Access.Private) pauseController.releaseAll();
        });
    }

    public void shutdown() {
        sessions.clear();
        pauseController.releaseAll();
    }

    public void open(
            @Nonnull PlayerRef playerRef,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store
    ) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) return;

        if (sessions.containsKey(playerRef.getUuid())) {
            closePage(ref, store);
            discard(playerRef);
        }

        PlayerConfig playerCfg = playerConfigService.getPlayerConfig(playerRef);
        sessions.put(playerRef.getUuid(), new HudConfigUiSession(playerCfg));
        try {
            player.getPageManager().openCustomPage(ref, store, new IHudConfigPage(this, playerRef));
            World world = player.getWorld();
            if (world != null) {
                pauseController.pause(playerRef.getUuid(), new WorldPauseTarget(world), canPause(playerRef));
            }
        } catch (RuntimeException exception) {
            discard(playerRef);
            throw exception;
        }
    }

    @Nullable
    public HudConfigUiSession getSession(@Nonnull PlayerRef playerRef) {
        return sessions.get(playerRef.getUuid());
    }

    public void discard(@Nonnull PlayerRef playerRef) {
        sessions.remove(playerRef.getUuid());
        pauseController.release(playerRef.getUuid());
    }

    public boolean apply(@Nonnull PlayerRef playerRef) {
        HudConfigUiSession session = getSession(playerRef);
        if (session == null) {
            return false;
        }

        playerConfigService.updatePlayerConfig(playerRef, config -> {
            config.setHudComponents(session.getDraftHudComponents().copy());
            config.setDynamicHud(session.getDraftDynamicHud().copy());
        });
        hudRuntimeService.onPlayerConfigChanged(playerRef);

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

    private boolean canPause(PlayerRef playerRef) {
        if (!Constants.SINGLEPLAYER) return false;
        SingleplayerModule singleplayer = SingleplayerModule.get();
        return singleplayer != null && HudConfigPauseController.canPause(
                true, SingleplayerModule.isOwner(playerRef), singleplayer.getAccess(),
                singleplayer.getRequestedAccess(), Universe.get().getPlayerCount());
    }

    private record WorldPauseTarget(World world) implements HudConfigPauseController.PauseTarget {
        @Override public boolean isPaused() { return world.isPaused(); }
        @Override public void setPaused(boolean paused) { world.setPaused(paused); }

        @Override
        public void execute(@Nonnull Runnable action) {
            if (!world.isAlive()) return;
            if (world.isInThread()) action.run();
            else world.execute(action);
        }
    }
}
