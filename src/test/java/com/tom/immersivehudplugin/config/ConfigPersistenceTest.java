package com.tom.immersivehudplugin.config;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class ConfigPersistenceTest {
    private static final Gson GSON = new Gson();
    private static int checks;

    static void main() throws Exception {
        Path root = Files.createTempDirectory(Path.of(".").toAbsolutePath(), "ihud-persistence-");
        try {
            verifyUnifiedHideDelay();
            verifyConfigShortcut();
            verifyReticleRangeValidation();
            verifyJsonNumericOverflow();
            verifyAtomicFiles(root.resolve("atomic"));
            verifySnapshotsAndCoalescing(root.resolve("coalescing"));
            verifyConcurrentSaves(root.resolve("concurrent"));
            verifyDisconnectOrdering(root.resolve("disconnect"));
            verifyFailures(root.resolve("failures"));
            System.out.println("Passed " + checks + " configuration persistence regression checks.");
        } finally {
            try (var files = Files.walk(root)) {
                for (Path file : files.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(file);
            }
        }
    }

    private static void verifyConfigShortcut() {
        GlobalConfig legacy = ConfigJsonMapper.fromJsonGlobal(
                JsonParser.parseString("{\"HideDelayMs\":750}").getAsJsonObject());
        verify("legacy config gains persisted shortcut defaults", legacy.sanitize());
        verify("default shortcut is disabled", !legacy.getConfigShortcut().isEnabled()
                && !legacy.getConfigShortcut().matches(true));
        verify("migration preserves existing settings", legacy.getHideDelayMs() == 750);
        verify("shortcut migration is stable", !legacy.sanitize());
        verify("default action remains map", legacy.getConfigShortcut().getAction().equals("OPEN_MAP"));

        for (boolean enabled : new boolean[]{true, false}) {
            var json = ConfigJsonMapper.toJson(new GlobalConfig());
            json.getAsJsonObject("configShortcut").addProperty("enabled", enabled);
            json.getAsJsonObject("configShortcut").addProperty("action", "OPEN_POCKET_CRAFTING");
            GlobalConfig loaded = ConfigJsonMapper.fromJsonGlobal(json);
            verify("pocket crafting action is accepted", !loaded.sanitize());
            verify("pocket crafting action persists", ConfigJsonMapper.toJson(loaded).equals(json));
            verify("pocket crafting honors crouch and enabled", !loaded.getConfigShortcut().matches(false)
                    && loaded.getConfigShortcut().matches(true) == enabled);
        }
        verifyShortcutPackets();
        verifyShortcutInputState();

        for (boolean enabled : new boolean[]{true, false}) {
            var json = ConfigJsonMapper.toJson(new GlobalConfig());
            json.getAsJsonObject("configShortcut").addProperty("enabled", enabled);
            GlobalConfig loaded = ConfigJsonMapper.fromJsonGlobal(json);
            verify("valid shortcut is unchanged", !loaded.sanitize());
            verify("shortcut persists exactly", ConfigJsonMapper.toJson(loaded).equals(json));
            verify("shortcut requires crouch", !loaded.getConfigShortcut().matches(false));
            verify("shortcut honors enable setting", loaded.getConfigShortcut().matches(true) == enabled);
            var minimalJson = json.deepCopy();
            minimalJson.getAsJsonObject("configShortcut").remove("modifier");
            minimalJson.getAsJsonObject("configShortcut").remove("action");
            GlobalConfig minimal = ConfigJsonMapper.fromJsonGlobal(minimalJson);
            verify("missing bindings are restored", minimal.sanitize());
            verify("restoring bindings preserves enabled", ConfigJsonMapper.toJson(minimal).equals(json));
            for (String modifier : new String[]{"SPRINT", "NONE"}) {
                var oldJson = json.deepCopy();
                oldJson.getAsJsonObject("configShortcut").addProperty("modifier", modifier);
                oldJson.getAsJsonObject("configShortcut").addProperty("action", "OPEN_MAP");
                GlobalConfig migrated = ConfigJsonMapper.fromJsonGlobal(oldJson);
                verify("unsupported bindings trigger migration", migrated.sanitize());
                verify("migration restores supported bindings and preserves enabled",
                        ConfigJsonMapper.toJson(migrated).equals(json));
                verify("migrated shortcut requires crouch", !migrated.getConfigShortcut().matches(false)
                        && migrated.getConfigShortcut().matches(true) == enabled);
                verify("migrated config is stable", !migrated.sanitize());
            }
        }
        for (String value : new String[]{"null", "17", "{}", "{\"enabled\":\"false\"}",
                "{\"enabled\":true,\"modifier\":false,\"action\":[]}",
                "{\"enabled\":true,\"modifier\":\"CROUCH\",\"action\":\"CHANGE_VIEW\"}"}) {
            var json = JsonParser.parseString("{\"HideDelayMs\":750,\"configShortcut\":" + value + "}")
                    .getAsJsonObject();
            verify("invalid shortcut does not invalidate global config", ConfigSchemaValidator.isValidGlobalConfig(json));
            GlobalConfig loaded = ConfigJsonMapper.fromJsonGlobal(json);
            verify("invalid shortcut is repaired", loaded.sanitize());
            verify("invalid shortcut preserves other settings", loaded.getHideDelayMs() == 750);
            boolean explicitlyEnabled = json.get("configShortcut").isJsonObject()
                    && new com.google.gson.JsonPrimitive(true).equals(json.getAsJsonObject("configShortcut").get("enabled"));
            verify("shortcut repair preserves explicit opt-in and defaults to disabled",
                    loaded.getConfigShortcut().matches(true) == explicitlyEnabled
                    && !loaded.getConfigShortcut().matches(false));
            GlobalConfig restored = ConfigJsonMapper.fromJsonGlobal(ConfigJsonMapper.toJson(loaded));
            verify("repaired shortcut round trips", !restored.sanitize());
        }
    }

    private static void verifyShortcutPackets() {
        verify("map opening is recognized", "OPEN_MAP".equals(
                com.tom.immersivehudplugin.ui.HudConfigMapShortcut.actionForPacket(
                        new com.hypixel.hytale.protocol.packets.worldmap.UpdateWorldMapVisible(true))));
        verify("map closing is ignored", com.tom.immersivehudplugin.ui.HudConfigMapShortcut.actionForPacket(
                new com.hypixel.hytale.protocol.packets.worldmap.UpdateWorldMapVisible(false)) == null);
        for (var type : com.hypixel.hytale.protocol.packets.window.WindowType.values()) {
            String action = com.tom.immersivehudplugin.ui.HudConfigMapShortcut.actionForPacket(
                    new com.hypixel.hytale.protocol.packets.window.ClientOpenWindow(type));
            verify("only pocket crafting windows are recognized: " + type,
                    type == com.hypixel.hytale.protocol.packets.window.WindowType.PocketCrafting
                            ? "OPEN_POCKET_CRAFTING".equals(action) : action == null);
        }
        verify("unrelated packets are ignored", com.tom.immersivehudplugin.ui.HudConfigMapShortcut.actionForPacket(
                new com.hypixel.hytale.protocol.packets.interface_.ChatMessage()) == null);
    }

    private static void verifyShortcutInputState() {
        var input = new com.tom.immersivehudplugin.ui.HudShortcutInputState();
        UUID player = UUID.randomUUID();
        UUID otherPlayer = UUID.randomUUID();
        UUID world = UUID.randomUUID();
        UUID otherWorld = UUID.randomUUID();
        verify("unknown input cannot trigger shortcut", !input.isCrouching(player, world));
        input.update(player, world, true);
        verify("crouch press is available before movement tick", input.isCrouching(player, world));
        verify("input belongs to the player", !input.isCrouching(otherPlayer, world));
        verify("input does not leak across worlds", !input.isCrouching(player, otherWorld));
        input.update(player, world, false);
        verify("crouch release clears shortcut before movement tick", !input.isCrouching(player, world));
        input.update(player, otherWorld, true);
        verify("new world input replaces old world input", input.isCrouching(player, otherWorld)
                && !input.isCrouching(player, world));
        input.remove(player);
        verify("disconnect clears shortcut input", !input.isCrouching(player, otherWorld));
        input.update(player, world, true);
        input.clear();
        verify("shutdown clears shortcut input", !input.isCrouching(player, world));
    }
    private static void verifyUnifiedHideDelay() {
        for (int delay : new int[]{0, 750, 3000}) {
            var json = JsonParser.parseString("{\"HideDelayMs\":" + delay
                    + ",\"InitialHudGraceMs\":1234}").getAsJsonObject();
            verify("legacy configuration remains valid", ConfigSchemaValidator.isValidGlobalConfig(json));
            GlobalConfig config = ConfigJsonMapper.fromJsonGlobal(json);
            config.sanitize();
            verify("legacy initial delay does not override hide delay", config.getHideDelayMs() == delay);
            var state = new com.tom.immersivehudplugin.runtime.PlayerHudState();
            state.startInitialHudVisibleGrace(10000, config.getHideDelayMs());
            verify("initial grace uses hide delay", state.isInitialHudVisibleGraceActive(10000) == (delay > 0));
            verify("initial grace expires at hide delay", !state.isInitialHudVisibleGraceActive(10000 + delay));
            if (delay > 0) verify("initial grace lasts until hide delay",
                    state.isInitialHudVisibleGraceActive(10000 + delay - 1));
            var saved = ConfigJsonMapper.toJson(config);
            verify("saved configuration only contains unified delay",
                    !saved.has("InitialHudGraceMs") && saved.get("HideDelayMs").getAsInt() == delay);
        }
    }

    private static void verifyReticleRangeValidation() {
        for (float value : new float[]{0f, -1f, Math.nextUp(24f),
                Float.MAX_VALUE}) {
            GlobalConfig config = new GlobalConfig();
            config.setReticleTargetRange(value);
            verify("invalid reticle range is reported: " + value, config.sanitize());
            verify("invalid reticle range resets to default: " + value,
                    config.getReticleTargetRange() == GlobalConfig.RETICLE_TARGET_RANGE);
            verify("corrected reticle range is stable: " + value, !config.sanitize());
        }
        for (float value : new float[]{0.5f, GlobalConfig.RETICLE_TARGET_RANGE, 24f}) {
            GlobalConfig config = new GlobalConfig();
            config.setReticleTargetRange(value);
            verify("valid reticle range is unchanged: " + value,
                    !config.sanitize() && config.getReticleTargetRange() == value);
        }
        // A valid JSON number can overflow when converted to a float.
        GlobalConfig overflow = ConfigJsonMapper.fromJsonGlobal(
                JsonParser.parseString("{\"ReticleTargetRange\":1e100}").getAsJsonObject());
        verify("JSON numeric overflow is corrected", overflow.sanitize()
                && overflow.getReticleTargetRange() == GlobalConfig.RETICLE_TARGET_RANGE);
        GlobalConfig restored = ConfigJsonMapper.fromJsonGlobal(ConfigJsonMapper.toJson(overflow));
        verify("corrected reticle range survives serialization", !restored.sanitize()
                && restored.getReticleTargetRange() == GlobalConfig.RETICLE_TARGET_RANGE);
    }

    private static void verifyJsonNumericOverflow() {
        String[] keys = {"ReticleTargetRange", "CombatScanRange", "PlayerCombatHorizontalFovDegrees",
                "PlayerCombatVerticalFovDegrees", "LosNpcTargetHeight", "LosTargetEpsilon"};
        var defaults = ConfigJsonMapper.toJson(new GlobalConfig());
        for (String key : keys) {
            GlobalConfig overflow = ConfigJsonMapper.fromJsonGlobal(
                    JsonParser.parseString("{\"" + key + "\":1e1000}").getAsJsonObject());
            verify(key + " corrects JSON overflow", overflow.sanitize()
                    && ConfigJsonMapper.toJson(overflow).get(key).equals(defaults.get(key)));
        }
    }
    private static void verifyAtomicFiles(Path directory) throws Exception {
        Files.createDirectories(directory);
        Path file = directory.resolve("config.json");
        Files.writeString(file, "{\"value\":1}");
        try {
            ConfigSupport.writeAtomically(file, writer -> {
                writer.write("{\"value\":");
                writer.flush();
                verify("original remains readable during temporary write", Files.readString(file).equals("{\"value\":1}"));
                throw new IOException("simulated disk failure");
            });
            throw new AssertionError("Expected write failure");
        } catch (IOException expected) {
            verify("failed write preserves original bytes", Files.readString(file).equals("{\"value\":1}"));
        }
        verify("failed write cleans temporary file", temporaryCount(directory) == 0);
        ConfigSupport.writeAtomically(file, writer -> writer.write("{\"value\":2}"));
        verify("successful atomic replacement", JsonParser.parseString(Files.readString(file)).getAsJsonObject()
                .get("value").getAsInt() == 2);
        verify("successful replacement leaves no temporary file", temporaryCount(directory) == 0);

        Path missing = directory.resolve("new.json");
        try {
            ConfigSupport.writeAtomically(missing, _ -> { throw new IOException("simulated failure"); });
            throw new AssertionError("Expected write failure");
        } catch (IOException expected) {
            verify("failed first write does not publish an incomplete file", !Files.exists(missing));
        }
        Path blocked = directory.resolve("blocked.json");
        Files.createDirectory(blocked);
        Path marker = blocked.resolve("marker");
        Files.writeString(marker, "keep");
        try {
            ConfigSupport.writeAtomically(blocked, writer -> writer.write("{}"));
            throw new AssertionError("Expected replacement failure");
        } catch (IOException expected) {
            verify("replacement failure leaves destination intact", Files.readString(marker).equals("keep"));
            verify("replacement failure cleans temporary file", temporaryCount(directory) == 0);
        }
    }

    private static void verifySnapshotsAndCoalescing(Path directory) throws Exception {
        var tasks = new ManualExecutor();
        var fixture = new Fixture(directory, tasks);
        PlayerConfig original = fixture.load();
        fixture.writes.clear();
        fixture.update(25f);
        verify("updates do not mutate previously published configs", original.getDynamicHud().getHealth().getThreshold() == 100f);
        fixture.update(50f);
        verify("multiple edits share one pending task", tasks.size() == 1);
        tasks.drain();
        verify("coalesced save writes latest revision", fixture.writes.size() == 1 && fixture.readThreshold() == 50f);
        verify("clean save is a no-op", fixture.store.save(fixture.id) && fixture.writes.size() == 1);

        PlayerConfig beforeFailure = fixture.store.getCached(fixture.id);
        try {
            fixture.store.update(fixture.id, fixture.global, config -> {
                config.getDynamicHud().getHealth().setThreshold(12f);
                throw new IllegalStateException("invalid edit");
            });
            throw new AssertionError("Expected mutator failure");
        } catch (IllegalStateException expected) {
            verify("failed mutation does not publish partial config", fixture.store.getCached(fixture.id) == beforeFailure);
            verify("failed mutation does not schedule a write", tasks.size() == 0);
        }
    }

    private static void verifyConcurrentSaves(Path directory) throws Exception {
        var tasks = new ManualExecutor();
        var fixture = new Fixture(directory, tasks);
        fixture.load();
        UUID other = UUID.randomUUID();
        fixture.store.loadOrCreate(other, fixture.global);
        fixture.writes.clear();
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var active = new AtomicInteger();
        var maximum = new AtomicInteger();
        fixture.behavior = (path, json) -> {
            if (path.equals(fixture.store.pathFor(fixture.id))) {
                int count = active.incrementAndGet();
                maximum.accumulateAndGet(count, Math::max);
                try {
                    if (threshold(json) == 25f) {
                        entered.countDown();
                        await(release);
                    }
                    write(path, json);
                } finally {
                    active.decrementAndGet();
                }
            } else write(path, json);
        };
        try (var workers = Executors.newFixedThreadPool(3)) {
            fixture.update(25f);
            Future<?> first = workers.submit(tasks::runOne);
            try {
                await(entered);
                fixture.update(50f);
                PlayerConfig updated = fixture.store.getCached(fixture.id);
                verify("world-thread edits are not blocked by disk I/O", updated != null
                        && updated.getDynamicHud().getHealth().getThreshold() == 50f);
                fixture.store.update(other, fixture.global, config -> config.getDynamicHud().getHealth().setThreshold(77f));
                verify("other players can save independently", workers.submit(() -> fixture.store.save(other)).get(5, TimeUnit.SECONDS));
                verify("other player's file is complete", threshold(JsonParser.parseString(Files.readString(fixture.store.pathFor(other)))) == 77f);
                Future<Boolean> overlapping = workers.submit(() -> fixture.store.save(fixture.id));
                release.countDown();
                first.get(5, TimeUnit.SECONDS);
                verify("concurrent explicit save succeeds", overlapping.get(5, TimeUnit.SECONDS));
                tasks.drain();
                verify("same-player writes never overlap", maximum.get() == 1);
                verify("newer edits survive completion of older save", fixture.readThreshold() == 50f);
                List<Float> revisions = fixture.writes.stream().filter(json -> threshold(json) != 77f)
                        .map(ConfigPersistenceTest::threshold).toList();
                verify("same-player revisions are written in order", revisions.equals(List.of(25f, 50f)));
            } finally {
                release.countDown();
            }
        }
    }

    private static void verifyDisconnectOrdering(Path directory) throws Exception {
        var tasks = new ManualExecutor();
        var fixture = new Fixture(directory, tasks);
        fixture.load();
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        fixture.behavior = (path, json) -> {
            if (threshold(json) == 25f) { entered.countDown(); await(release); }
            write(path, json);
        };
        try (var workers = Executors.newFixedThreadPool(2)) {
            fixture.update(25f);
            Future<?> first = workers.submit(tasks::runOne);
            try {
                await(entered);
                fixture.update(50f);
                Future<Boolean> disconnect = workers.submit(() -> fixture.store.saveAndUnload(fixture.id));
                release.countDown();
                first.get(5, TimeUnit.SECONDS);
                verify("disconnect waits for ordered save", disconnect.get(5, TimeUnit.SECONDS));
                verify("disconnect persists latest revision", fixture.readThreshold() == 50f);
                verify("successful disconnect unloads config", fixture.store.getCached(fixture.id) == null);
            } finally {
                release.countDown();
            }
        }
        fixture.behavior = ConfigPersistenceTest::write;
        fixture.load();
        fixture.update(60f);
        verify("save before queued task can unload safely", fixture.store.saveAndUnload(fixture.id));
        fixture.load();
        fixture.update(90f);
        fixture.writes.clear();
        tasks.drain();
        verify("old queued tasks cannot overwrite reconnected session", fixture.readThreshold() == 90f
                && fixture.writes.size() == 1);
    }

    private static void verifyFailures(Path directory) throws Exception {
        var tasks = new ManualExecutor();
        var fixture = new Fixture(directory.resolve("disconnect"), tasks);
        fixture.load();
        fixture.behavior = (_, _) -> { throw new IOException("disk full"); };
        fixture.update(35f);
        tasks.drain();
        verify("failed async save is reported without a retry loop", fixture.failures.size() == 1 && tasks.size() == 0);
        verify("failure preserves last good file", fixture.readThreshold() == 100f);
        verify("failed disconnect reports failure", !fixture.store.saveAndUnload(fixture.id));
        PlayerConfig pending = fixture.store.getCached(fixture.id);
        verify("failed disconnect retains unsaved configuration", pending != null
                && pending.getDynamicHud().getHealth().getThreshold() == 35f);
        fixture.behavior = ConfigPersistenceTest::write;
        fixture.store.flush();
        verify("shutdown flush retries disconnected dirty players", fixture.readThreshold() == 35f);
        verify("successful retry completes deferred unload", fixture.store.getCached(fixture.id) == null);

        var reconnect = new Fixture(directory.resolve("reconnect"), new ManualExecutor());
        reconnect.load();
        reconnect.behavior = (_, _) -> { throw new IOException("disk full"); };
        reconnect.update(65f);
        reconnect.store.saveAndUnload(reconnect.id);
        var service = new PlayerConfigService(reconnect.store, () -> reconnect.global);
        PlayerConfig retained = service.getOrLoadPlayerConfig(reconnect.id);
        verify("reconnect recovers unsaved edits instead of stale disk config", retained.getDynamicHud().getHealth().getThreshold() == 65f);
        reconnect.behavior = ConfigPersistenceTest::write;
        reconnect.store.flush();
        verify("reconnect cancels deferred unload", reconnect.store.getCached(reconnect.id) != null
                && reconnect.readThreshold() == 65f);

        var rejected = new Fixture(directory.resolve("rejected"), _ -> { throw new RejectedExecutionException("stopped"); });
        rejected.load();
        rejected.update(45f);
        verify("executor rejection is reported", rejected.failures.size() == 1);
        verify("rejected tasks retain edits for synchronous flush", rejected.store.save(rejected.id) && rejected.readThreshold() == 45f);
    }

    private static void write(Path path, JsonElement json) throws IOException {
        ConfigSupport.writeAtomically(path, writer -> GSON.toJson(json, writer));
    }

    private static float threshold(JsonElement json) {
        return ConfigJsonMapper.fromJsonPlayer(json.getAsJsonObject()).getDynamicHud().getHealth().getThreshold();
    }

    private static long temporaryCount(Path directory) throws IOException {
        try (var files = Files.list(directory)) {
            return files.filter(path -> path.getFileName().toString().startsWith(".ihud-")).count();
        }
    }

    private static void await(CountDownLatch latch) throws InterruptedException {
        if (!latch.await(5, TimeUnit.SECONDS)) throw new AssertionError("Timed out waiting for test coordination");
    }

    private static void verify(String name, boolean condition) {
        if (!condition) throw new AssertionError(name);
        checks++;
    }

    private static final class ManualExecutor implements Executor {
        private final Queue<Runnable> tasks = new ConcurrentLinkedQueue<>();
        @Override public void execute(@Nonnull Runnable task) { tasks.add(task); }
        int size() { return tasks.size(); }
        void runOne() {
            Runnable task = tasks.poll();
            if (task == null) throw new AssertionError("No pending save");
            task.run();
        }
        void drain() {
            int runs = 0;
            while (!tasks.isEmpty()) {
                if (++runs > 20) throw new AssertionError("Unexpected save retry loop");
                runOne();
            }
        }
    }

    private static final class Fixture {
        final UUID id = UUID.randomUUID();
        final GlobalConfig global = new GlobalConfig();
        final List<JsonElement> writes = new CopyOnWriteArrayList<>();
        final List<Exception> failures = new ArrayList<>();
        final PlayerConfigStore store;
        volatile PlayerConfigStore.JsonWriter behavior = ConfigPersistenceTest::write;

        Fixture(Path directory, Executor executor) {
            store = new PlayerConfigStore(directory, (uuid, defaults) -> {
                Path file = directory.resolve(uuid + ".json");
                try {
                    if (Files.exists(file)) return new ConfigSupport.LoadResult<>(
                            ConfigJsonMapper.fromJsonPlayer(JsonParser.parseString(Files.readString(file)).getAsJsonObject()), false, false);
                } catch (IOException failure) {
                    throw new IllegalStateException(failure);
                }
                return new ConfigSupport.LoadResult<>(PlayerConfig.fromDefaults(
                        defaults.getDefaultHudComponents(), defaults.getDefaultDynamicHud()), true, true);
            }, (path, json) -> {
                writes.add(json.deepCopy());
                behavior.write(path, json);
            }, executor, (_, failure) -> failures.add(failure));
        }
        PlayerConfig load() { return store.loadOrCreate(id, global); }
        void update(float threshold) {
            store.update(id, global, config -> config.getDynamicHud().getHealth().setThreshold(threshold));
        }
        float readThreshold() throws IOException {
            return threshold(JsonParser.parseString(Files.readString(store.pathFor(id))));
        }
    }
}
