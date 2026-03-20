package com.tom.immersivehudplugin.runtime;

import com.hypixel.hytale.assetstore.AssetMap;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChain;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChains;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketWatcher;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.tom.immersivehudplugin.config.DynamicHudConfig;
import com.tom.immersivehudplugin.config.GlobalConfig;
import com.tom.immersivehudplugin.config.HudComponentsConfig;
import com.tom.immersivehudplugin.config.PlayerConfig;
import com.tom.immersivehudplugin.context.HudContextBuilder;
import com.tom.immersivehudplugin.managers.PlayerConfigManager;
import com.tom.immersivehudplugin.utils.ItemInHand;
import com.tom.immersivehudplugin.visibility.HudVisibilityService;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.function.Supplier;

public final class HudRuntimeService {

    private final JavaPlugin plugin;
    private final PlayerConfigManager playerConfigManager;
    private final HudContextBuilder hudContextBuilder;
    private final HudVisibilityService hudVisibilityService;
    private final AssetMap<String, Item> itemAssetMap;
    private final Supplier<GlobalConfig> globalConfigSupplier;

    private final Map<UUID, PlayerHudState> playerState = new ConcurrentHashMap<>();

    private volatile boolean inboundRegistered;
    private volatile ScheduledFuture<?> tickTask;
    private volatile int runningIntervalMs = -1;

    public HudRuntimeService(
            JavaPlugin plugin,
            PlayerConfigManager playerConfigManager,
            HudContextBuilder hudContextBuilder,
            HudVisibilityService hudVisibilityService,
            AssetMap<String, Item> itemAssetMap,
            Supplier<GlobalConfig> globalConfigSupplier
    ) {
        this.plugin = plugin;
        this.playerConfigManager = playerConfigManager;
        this.hudContextBuilder = hudContextBuilder;
        this.hudVisibilityService = hudVisibilityService;
        this.itemAssetMap = itemAssetMap;
        this.globalConfigSupplier = globalConfigSupplier;
    }

    public void start() {
        registerInboundWatcher();
        registerPlayerEvents();
        restartTickTaskIfNeeded();
    }

    public void shutdown() {
        ScheduledFuture<?> t = tickTask;
        tickTask = null;

        if (t != null) {
            t.cancel(true);
        }

        for (UUID uuid : playerState.keySet()) {
            playerConfigManager.save(uuid);
        }

        playerState.clear();
    }

    public void restartTickTaskIfNeeded() {

        GlobalConfig cfg = getGlobalConfig();
        int wanted = intervalMs(cfg);
        if (wanted <= 0) {
            wanted = GlobalConfig.INTERVAL_MS;
        }

        if (tickTask != null
                && !tickTask.isCancelled()
                && !tickTask.isDone()
                && runningIntervalMs == wanted) {
            return;
        }

        ScheduledFuture<?> old = tickTask;
        if (old != null) {
            old.cancel(false);
        }

        runningIntervalMs = wanted;
        tickTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(() -> {
            try {
                tickReadyPlayers();
            } catch (Throwable t) {
                plugin.getLogger().at(Level.WARNING).withCause(t).log("ImmersiveHud tick crashed");
            }
        }, 0, wanted, TimeUnit.MILLISECONDS);
    }

    public void markPlayerStaticHudDirty(@Nullable PlayerRef playerRef) {
        if (playerRef == null) {
            return;
        }

        PlayerHudState st = stateFor(playerRef.getUuid());
        st.staticHudInitialized = false;
        st.staticDirty = true;
        st.dynamicHudEnabledKnown = false;
    }

    public void markPlayerConfigDirty(UUID uuid) {
        playerConfigManager.markDirty(uuid);

        PlayerHudState st = playerState.get(uuid);
        if (st != null) {
            st.dynamicHudEnabledKnown = false;
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
        PlayerConfig cached = playerConfigManager.getCached(uuid);
        if (cached != null) {
            return cached;
        }
        return playerConfigManager.loadOrCreate(uuid, getGlobalConfig());
    }

    private void registerInboundWatcher() {

        if (inboundRegistered) { return; }
        inboundRegistered = true;

        PacketAdapters.registerInbound((PlayerPacketWatcher) (playerRef, packet) -> {

            if (!(packet instanceof SyncInteractionChains sic)) { return; }

            long now = nowMs();
            PlayerHudState st = stateFor(playerRef.getUuid());

            for (SyncInteractionChain u : sic.updates) {

                boolean isChargingStart = (u.interactionType == InteractionType.Primary);
                boolean isChargingEnd = (u.interactionType == InteractionType.ProjectileHit
                        || u.interactionType == InteractionType.ProjectileBounce
                        || u.interactionType == InteractionType.ProjectileMiss);
                boolean isSecondaryStart = (u.interactionType == InteractionType.Secondary);

                if (u.itemInHandId != null) {
                    Item item = itemAssetMap.getAsset(u.itemInHandId);

                    st.heldItem = item;
                    st.rangedWeaponInHand = ItemInHand.isRangedWeapon(item);
                    st.meleeWeaponInHand = ItemInHand.isMeleeWeapon(item);
                    st.heldItemStateInitialized = true;
                    st.heldItemRefreshRequested = false;

                    if (isSecondaryStart && item != null && item.isConsumable()) {
                        st.t.pulse(HudSignal.CONSUMABLE_USE, now, st.hideDelayMsHint);
                    }
                }

                int slot = u.activeHotbarSlot;
                if (slot >= 0 && slot <= 8) {
                    int prev = st.lastSeenActiveHotbarSlot;
                    st.lastSeenActiveHotbarSlot = slot;

                    if (prev != -1 && slot != prev) {
                        st.heldItemRefreshRequested = true;
                        st.heldItemStateInitialized = false;
                        st.heldItem = null;
                        st.rangedWeaponInHand = false;
                        st.meleeWeaponInHand = false;

                        st.t.clear(HudSignal.HOLDING_RANGED_WEAPON);
                        st.t.clear(HudSignal.HOLDING_MELEE_WEAPON);
                        st.t.clear(HudSignal.CHARGING_WEAPON);

                        isChargingStart = false;
                        isChargingEnd = true;

                        st.t.pulse(HudSignal.HOTBAR_INPUT, now, st.hideDelayMsHint);
                    }
                }

                /*if (st.rangedWeaponInHand) {
                    st.t.pulse(HudSignal.HOLDING_RANGED_WEAPON, now, st.hideDelayMsHint);
                }*/

                /*if (st.meleeWeaponInHand) {
                    st.t.pulse(HudSignal.HOLDING_MELEE_WEAPON, now, st.hideDelayMsHint);
                }*/

                if (isChargingStart && (st.rangedWeaponInHand || st.meleeWeaponInHand)) {
                    st.t.pulse(HudSignal.CHARGING_WEAPON, now, st.hideDelayMsHint);
                }

                if (isChargingEnd) {
                    st.t.clear(HudSignal.CHARGING_WEAPON);
                }
            }

            if (!st.meleeWeaponInHand) {
                st.t.clear(HudSignal.HOLDING_MELEE_WEAPON);
            }

            if (!st.rangedWeaponInHand) {
                st.t.clear(HudSignal.HOLDING_RANGED_WEAPON);
            }

            if (!st.meleeWeaponInHand && !st.rangedWeaponInHand) {
                st.t.clear(HudSignal.CHARGING_WEAPON);
            }
        });
    }

    private void registerPlayerEvents() {
        plugin.getEventRegistry().registerGlobal(PlayerReadyEvent.class, e -> {
            @SuppressWarnings("removal")
            UUID uuid = e.getPlayer().getUuid();

            long now = nowMs();
            int hideDelay = hideDelayMs(getGlobalConfig());

            getOrLoadPlayerConfig(uuid);

            PlayerHudState st = stateFor(uuid);
            resetPlayerRuntimeState(st, hideDelay, now);
        });

        plugin.getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, e -> {
            UUID uuid = e.getPlayerRef().getUuid();
            playerState.remove(uuid);
            playerConfigManager.saveAndUnload(uuid);
        });
    }

    private void resetPlayerRuntimeState(PlayerHudState st, int hideDelay, long now) {
        st.reset(hideDelay);

        st.t.pulse(HudSignal.READY_GRACE, now, hideDelay);
        st.t.pulse(HudSignal.HOTBAR_INPUT, now, hideDelay);
    }

    private void tickReadyPlayers() {
        Universe universe = Universe.get();
        long now = nowMs();
        GlobalConfig global = getGlobalConfig();

        for (Map.Entry<UUID, PlayerHudState> entry : playerState.entrySet()) {
            UUID uuid = entry.getKey();
            PlayerHudState st = entry.getValue();

            st.hideDelayMsHint = hideDelayMs(global);

            if (st.t.active(HudSignal.READY_GRACE, now)) {
                continue;
            }

            PlayerRef playerRef = universe.getPlayer(uuid);
            if (playerRef == null || !playerRef.isValid()) {
                playerState.remove(uuid, st);
                continue;
            }

            UUID worldUuid = playerRef.getWorldUuid();
            if (worldUuid == null) {
                continue;
            }

            World world = universe.getWorld(worldUuid);
            if (world == null || !world.isAlive()) {
                continue;
            }

            world.execute(() -> {
                PlayerRef pr = Universe.get().getPlayer(uuid);
                if (pr == null || !pr.isValid()) {
                    return;
                }
                if (!worldUuid.equals(pr.getWorldUuid())) {
                    return;
                }

                long tickNow = nowMs();
                checkAndToggle(pr, world, global, tickNow);
            });
        }
    }

    private void checkAndToggle(
            PlayerRef playerRef,
            World world,
            GlobalConfig global,
            long now
    ) {
        PlayerTickContext ctx = hudContextBuilder.buildCtx(playerRef);
        if (ctx == null) {
            return;
        }

        UUID uuid = playerRef.getUuid();
        PlayerHudState st = stateFor(uuid);
        PlayerConfig playerCfg = getOrLoadPlayerConfig(uuid);

        HudComponentsConfig hc = playerCfg.getHudComponents();
        DynamicHudConfig dh = playerCfg.getDynamicHud();

        hudVisibilityService.ensureStaticHudBuilt(st, hc);

        if (!isDynamicHudEnabled(st, hc)) {
            hudVisibilityService.clearDynamicHiddenIfNeeded(st);
            hudVisibilityService.applyHudDelta(ctx, st);
            return;
        }

        var dyn = hudContextBuilder.buildDynamicHudTriggerContext(st, world, ctx, global, now);
        hudVisibilityService.rebuildDynamicHidden(st, hc, dh, dyn);
        hudVisibilityService.applyHudDelta(ctx, st);
    }

    private PlayerHudState stateFor(UUID uuid) {
        return playerState.computeIfAbsent(uuid, ignored -> new PlayerHudState());
    }

    private GlobalConfig getGlobalConfig() {
        return globalConfigSupplier.get();
    }

    private int hideDelayMs(GlobalConfig cfg) {
        return cfg != null ? cfg.getHideDelayMs() : GlobalConfig.HIDE_DELAY_MS;
    }

    private int intervalMs(GlobalConfig cfg) {
        return cfg != null ? cfg.getIntervalMs() : GlobalConfig.INTERVAL_MS;
    }

    private static long nowMs() {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime());
    }

    private boolean isDynamicHudEnabled(PlayerHudState st, HudComponentsConfig hc) {
        if (!st.dynamicHudEnabledKnown) {
            st.dynamicHudEnabled = hudVisibilityService.hasAnyDynamicHudEnabled(hc);
            st.dynamicHudEnabledKnown = true;
        }
        return st.dynamicHudEnabled;
    }
}