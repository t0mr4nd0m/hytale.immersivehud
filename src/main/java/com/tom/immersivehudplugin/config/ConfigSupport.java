package com.tom.immersivehudplugin.config;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tom.immersivehudplugin.ImmersiveHudPlugin;

import java.io.Reader;
import java.io.Writer;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Level;

public final class ConfigSupport {

    private static final DateTimeFormatter BACKUP_TS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss");

    private final ImmersiveHudPlugin plugin;
    private final Gson gson;

    public ConfigSupport(ImmersiveHudPlugin plugin, Gson gson) {
        this.plugin = plugin;
        this.gson = gson;
    }

    public JsonElement readJson(Path path) throws Exception {
        try (Reader reader = Files.newBufferedReader(path)) {
            return JsonParser.parseReader(reader);
        }
    }

    public void writeJson(Path path, JsonElement json) throws IOException {
        writeAtomically(path, writer -> gson.toJson(json, writer));
    }

    @FunctionalInterface
    interface WriteAction {
        void write(Writer writer) throws IOException;
    }

    static void writeAtomically(Path path, WriteAction action) throws IOException {
        Path target = path.toAbsolutePath().normalize();
        Path parent = target.getParent();
        if (parent == null) throw new IOException("Configuration path must name a file: " + target);
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, ".ihud-" + target.getFileName() + "-", ".tmp");
        try {
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                action.write(writer);
            }
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                channel.force(true);
            }
            // Do not truncate the destination if atomic replacement is unsupported.
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException | RuntimeException | Error failure) {
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException | RuntimeException cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
            throw failure;
        }
    }

    public void backupFile(Path file) {
        if (file == null || !Files.exists(file)) {
            return;
        }

        try {
            String timestamp = LocalDateTime.now().format(BACKUP_TS);
            Path backup = file.resolveSibling(file.getFileName() + "." + timestamp + ".bkp");

            Files.move(file, backup, StandardCopyOption.REPLACE_EXISTING);

            plugin.getLogger().at(Level.WARNING).log(
                    " Backed up config to {}",
                    backup
            );

        } catch (Throwable t) {
            plugin.getLogger().at(Level.WARNING).log(
                    " Failed to back up config {} [{}]: {}",
                    file,
                    t.getClass().getSimpleName(),
                    t.getMessage()
            );
        }
    }

    public <T> LoadResult<T> loadOrCreate(
            Path file,
            Supplier<T> defaultFactory,
            Predicate<JsonElement> validator,
            Function<JsonObject, T> mapper,
            Predicate<T> sanitizer,
            String invalidMessage
    ) {
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            boolean existed = Files.exists(file);
            T config;
            boolean changed = false;

            if (!existed) {
                config = defaultFactory.get();
                changed = true;
            } else {
                JsonElement root = readJson(file);

                if (!validator.test(root)) {
                    throw new IllegalStateException(invalidMessage);
                }

                config = mapper.apply(root.getAsJsonObject());
            }

            changed |= sanitizer.test(config);

            return new LoadResult<>(config, changed, !existed);

        } catch (Throwable t) {
            plugin.getLogger().at(Level.WARNING).log(
                    " Failed to load config {} [{}]: {}",
                    file,
                    t.getClass().getSimpleName(),
                    t.getMessage()
            );

            backupFile(file);

            T fallback = defaultFactory.get();
            sanitizer.test(fallback);

            return new LoadResult<>(fallback, true, true);
        }
    }

    public record LoadResult<T>(
            T config,
            boolean changed,
            boolean created
    ) {}
}
