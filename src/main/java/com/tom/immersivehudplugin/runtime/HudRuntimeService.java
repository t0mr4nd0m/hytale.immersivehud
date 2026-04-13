package com.tom.immersivehudplugin.runtime;

import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChains;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketWatcher;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.tom.immersivehudplugin.config.GlobalConfig;
import com.tom.immersivehudplugin.config.PlayerConfig;
import com.tom.immersivehudplugin.config.PlayerConfigStore;
import com.tom.immersivehudplugin.runtime.context.HudBarStateUpdater;
import com.tom.immersivehudplugin.runtime.context.HudTriggerContextFactory;
import com.tom.immersivehudplugin.runtime.context.PlayerTickContextFactory;
import com.tom.immersivehudplugin.runtime.signal.HeldItemSignalTracker;
import com.tom.immersivehudplugin.runtime.signal.MovementSignalTracker;
import com.tom.immersivehudplugin.runtime.signal.ReticleSignalTracker;
import com.tom.immersivehudplugin.runtime.visibility.HudVisibilityCoordinator;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Level;

public final class HudRuntimeService {

    private final JavaPlugin plugin;
    private final PlayerConfigStore playerConfigStore;
    private final Supplier<GlobalConfig> globalConfigSupplier;

    private final HeldItemSignalTracker heldItemSignalTracker;

    private final HudTickProcessor hudTickProcessor;

    private final Map<UUID, PlayerHudState> playerState = new ConcurrentHashMap<>();

    private volatile boolean inboundRegistered;
    private volatile boolean playerEventsRegistered;
    private volatile ScheduledFuture<?> tickTask;
    private volatile int runningIntervalMs = -1;

    public HudRuntimeService(
            JavaPlugin plugin,
            PlayerConfigStore playerConfigStore,
            HudVisibilityCoordinator hudVisibilityCoordinator,
            Supplier<GlobalConfig> globalConfigSupplier,
            int healthState,
            int staminaState,
            int manaState,
            int oxygenState
    ) {
        this.plugin = plugin;
        this.playerConfigStore = playerConfigStore;
        this.globalConfigSupplier = globalConfigSupplier;

        this.heldItemSignalTracker = new HeldItemSignalTracker();
        PlayerTickContextFactory tickContextFactory = new PlayerTickContextFactory();
        MovementSignalTracker movementSignalTracker = new MovementSignalTracker();
        ReticleSignalTracker reticleSignalTracker = new ReticleSignalTracker();
        HudBarStateUpdater barUpdater = new HudBarStateUpdater(
                healthState,
                staminaState,
                manaState,
                oxygenState
        );
        HudTriggerContextFactory triggerContextFactory = new HudTriggerContextFactory();

        this.hudTickProcessor = new HudTickProcessor(
                tickContextFactory,
                movementSignalTracker,
                reticleSignalTracker,
                barUpdater,
                triggerContextFactory,
                hudVisibilityCoordinator,
                heldItemSignalTracker
        );
    }

    public void start() {
        registerInboundWatcher();
        registerPlayerEvents();
        restartTickTaskIfNeeded();
    }

    public void shutdown() {
        ScheduledFuture<?> task = tickTask;
        tickTask = null;

        if (task != null) {
            task.cancel(true);
        }

        playerState.forEach((uuid, state) -> playerConfigStore.save(uuid));

        playerState.clear();
    }

    public void restartTickTaskIfNeeded() {
        GlobalConfig global = getGlobalConfig();
        int wantedInterval = intervalMs(global);
        if (wantedInterval <= 0) {
            wantedInterval = GlobalConfig.INTERVAL_MS;
        }

        if (isTickTaskAlreadyRunningFor(wantedInterval)) {
            return;
        }

        cancelCurrentTickTask();

        runningIntervalMs = wantedInterval;
        tickTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(() -> {
            try {
                tickReadyPlayers();
            } catch (Throwable t) {
                plugin.getLogger().at(Level.WARNING).withCause(t).log("ImmersiveHud tick crashed");
            }
        }, 0, wantedInterval, TimeUnit.MILLISECONDS);
    }

    public void markPlayerStaticHudDirty(@Nullable PlayerRef playerRef) {
        if (playerRef == null) {
            return;
        }

        PlayerHudState state = stateFor(playerRef.getUuid());
        state.markStaticHudDirty();
        state.invalidateDynamicHudEnabledCache();
    }

    public void markPlayerConfigDirty(UUID uuid) {
        playerConfigStore.markDirty(uuid);

        PlayerHudState state = playerState.get(uuid);
        if (state != null) {
            state.invalidateDynamicHudEnabledCache();
        }
    }

    @Nullable
    public PlayerConfig requirePlayerConfig(@Nullable PlayerRef playerRef) {
        if (playerRef == null) {
            return null;
        }

        return getOrLoadPlayerConfig(playerRef.getUuid());
    }

    public PlayerConfig getOrLoadPlayerConfig(UUID uuid) {
        PlayerConfig cached = playerConfigStore.getCached(uuid);
        if (cached != null) {
            return cached;
        }

        return playerConfigStore.loadOrCreate(uuid, getGlobalConfig());
    }

    public void applyAndSavePlayerConfig(PlayerRef playerRef) {
        UUID uuid = playerRef.getUuid();
        markPlayerConfigDirty(uuid);
        playerConfigStore.saveAsync(uuid);
        markPlayerStaticHudDirty(playerRef);
    }

    private void registerInboundWatcher() {
        if (inboundRegistered) {
            return;
        }
        inboundRegistered = true;

        PacketAdapters.registerInbound((PlayerPacketWatcher) (playerRef, packet) -> {
            if (!(packet instanceof SyncInteractionChains updates)) {
                return;
            }

            long now = nowMs();
            PlayerHudState state = stateFor(playerRef.getUuid());

            heldItemSignalTracker.applyPacketBatch(state, updates, now);
            heldItemSignalTracker.cleanupWeaponSignals(state);
        });
    }

    private void registerPlayerEvents() {
        if (playerEventsRegistered) {
            return;
        }
        playerEventsRegistered = true;

        plugin.getEventRegistry().registerGlobal(PlayerReadyEvent.class, event -> {
            UUID uuid = event.getPlayer().getUuid();
            int hideDelay = hideDelayMs(getGlobalConfig());

            getOrLoadPlayerConfig(uuid);

            PlayerHudState state = stateFor(uuid);
            state.reset(hideDelay);
        });

        plugin.getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, event -> {
            UUID uuid = event.getPlayerRef().getUuid();
            playerState.remove(uuid);
            playerConfigStore.saveAndUnload(uuid);
        });
    }

    private void tickReadyPlayers() {
        Universe universe = Universe.get();
        GlobalConfig global = getGlobalConfig();

        playerState.forEach((uuid, state) -> processReadyPlayerTick(universe, uuid, state, global));
    }

    private void processReadyPlayerTick(
            Universe universe,
            UUID uuid,
            PlayerHudState state,
            GlobalConfig global
    ) {
        state.hideDelayMsHint = hideDelayMs(global);

        ResolvedPlayerWorld resolved = resolvePlayerWorld(universe, uuid);
        if (resolved == null) {
            return;
        }

        resolved.world().execute(() ->
                processReadyPlayerTickOnWorldThread(resolved.uuid(), resolved.worldUuid(), global)
        );
    }

    private void processReadyPlayerTickOnWorldThread(
            UUID uuid,
            UUID expectedWorldUuid,
            GlobalConfig global
    ) {
        ResolvedPlayerWorld resolved = revalidatePlayerWorldOnWorldThread(uuid, expectedWorldUuid);
        if (resolved == null) {
            return;
        }

        long now = nowMs();
        PlayerHudState state = stateFor(uuid);
        PlayerConfig playerConfig = getOrLoadPlayerConfig(uuid);

        hudTickProcessor.processPlayerTick(
                resolved.playerRef(),
                resolved.world(),
                global,
                now,
                state,
                playerConfig
        );
    }

    private boolean isTickTaskAlreadyRunningFor(int wantedInterval) {
        return tickTask != null
                && !tickTask.isCancelled()
                && !tickTask.isDone()
                && runningIntervalMs == wantedInterval;
    }

    private void cancelCurrentTickTask() {
        ScheduledFuture<?> current = tickTask;
        if (current != null) {
            current.cancel(false);
        }
    }

    private PlayerHudState stateFor(UUID uuid) {
        return playerState.computeIfAbsent(uuid, ignored -> new PlayerHudState());
    }

    private GlobalConfig getGlobalConfig() {
        return globalConfigSupplier.get();
    }

    private int hideDelayMs(GlobalConfig config) {
        return config != null ? config.getHideDelayMs() : GlobalConfig.HIDE_DELAY_MS;
    }

    private int intervalMs(GlobalConfig config) {
        return config != null ? config.getIntervalMs() : GlobalConfig.INTERVAL_MS;
    }

    private static long nowMs() {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime());
    }

    private record ResolvedPlayerWorld(
            UUID uuid,
            PlayerRef playerRef,
            UUID worldUuid,
            World world
    ) {}

    @Nullable
    private ResolvedPlayerWorld resolvePlayerWorld(Universe universe, UUID uuid) {
        PlayerRef playerRef = universe.getPlayer(uuid);
        if (playerRef == null || !playerRef.isValid()) {
            return null;
        }

        UUID worldUuid = playerRef.getWorldUuid();
        if (worldUuid == null) {
            return null;
        }

        World world = universe.getWorld(worldUuid);
        if (world == null || !world.isAlive()) {
            return null;
        }

        return new ResolvedPlayerWorld(uuid, playerRef, worldUuid, world);
    }

    @Nullable
    private ResolvedPlayerWorld revalidatePlayerWorldOnWorldThread(
            UUID uuid,
            UUID expectedWorldUuid
    ) {
        Universe universe = Universe.get();

        PlayerRef playerRef = universe.getPlayer(uuid);
        if (playerRef == null || !playerRef.isValid()) {
            return null;
        }

        if (!expectedWorldUuid.equals(playerRef.getWorldUuid())) {
            return null;
        }

        World world = universe.getWorld(expectedWorldUuid);
        if (world == null || !world.isAlive()) {
            return null;
        }

        return new ResolvedPlayerWorld(uuid, playerRef, expectedWorldUuid, world);
    }
}