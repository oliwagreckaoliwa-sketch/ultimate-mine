package autominer.config;

import autominer.AutoMiner;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import net.fabricmc.loader.api.FabricLoader;

/** Wczytywanie i zapisywanie konfiguracji (atomowy zapis, kopia przy uszkodzonym pliku). */
public final class ConfigManager {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static ModConfig config = new ModConfig();
    private static boolean dirty;
    private static long lastSaveMs;

    private ConfigManager() {}

    public static ModConfig get() {
        return config;
    }

    public static Path dir() {
        return FabricLoader.getInstance().getConfigDir().resolve("autominer-ultimate");
    }

    public static Path file() {
        return dir().resolve("config.json");
    }

    public static synchronized void load() {
        Path f = file();
        ModConfig loaded = null;
        if (Files.exists(f)) {
            try (Reader r = Files.newBufferedReader(f, StandardCharsets.UTF_8)) {
                loaded = GSON.fromJson(r, ModConfig.class);
            } catch (IOException | JsonParseException e) {
                AutoMiner.LOGGER.error("Nie udalo sie wczytac konfiguracji, uzywam domyslnej", e);
                try {
                    Files.copy(f, f.resolveSibling("config.json.broken-" + System.currentTimeMillis()));
                } catch (IOException ignored) {
                    // kopia awaryjna jest opcjonalna
                }
            }
        }
        config = loaded != null ? loaded : new ModConfig();
        config.sanitize();
        if (loaded == null) {
            dirty = true;
        }
    }

    public static void markDirty() {
        dirty = true;
    }

    /** Wywolywane co tick: zapisuje zmiany najwyzej raz na 2 sekundy. */
    public static void tick() {
        if (dirty && System.currentTimeMillis() - lastSaveMs > 2000) {
            save();
        }
    }

    public static synchronized void save() {
        Path f = file();
        Path tmp = f.resolveSibling("config.json.tmp");
        try {
            Files.createDirectories(f.getParent());
            try (Writer w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                GSON.toJson(config, w);
            }
            try {
                Files.move(tmp, f, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, f, StandardCopyOption.REPLACE_EXISTING);
            }
            dirty = false;
            lastSaveMs = System.currentTimeMillis();
        } catch (IOException e) {
            AutoMiner.LOGGER.error("Zapis konfiguracji nie powiodl sie", e);
        }
    }
}
