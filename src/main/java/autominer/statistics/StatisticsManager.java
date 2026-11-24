package autominer.statistics;

import autominer.AutoMiner;
import autominer.config.ConfigManager;
import autominer.selector.BlockSelectionManager;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Zarzadza biezaca sesja, historia sesji i zapisem na dysk. */
public final class StatisticsManager {
    private static final int MAX_HISTORY = 100;

    private static final class StatsFile {
        long savedAtMs;
        SessionStatistics current;
        List<SessionStatistics> history = new ArrayList<>();
    }

    private SessionStatistics current = new SessionStatistics();
    private final List<SessionStatistics> history = new ArrayList<>();
    private boolean dirty;
    private long lastSaveMs = System.currentTimeMillis();
    private long lastTickNs = System.nanoTime();
    private int syncedSelectionVersion = -1;

    private Path file() {
        return ConfigManager.dir().resolve("stats.json");
    }

    public SessionStatistics current() {
        return current;
    }

    public List<SessionStatistics> history() {
        return history;
    }

    /** Suma historii i biezacej sesji. */
    public SessionStatistics allTime() {
        SessionStatistics all = new SessionStatistics();
        for (SessionStatistics h : history) all.mergeFrom(h);
        all.mergeFrom(current);
        return all;
    }

    public void ensureSessionStarted() {
        if (current.startMs <= 0) {
            current.startMs = System.currentTimeMillis();
            dirty = true;
        }
    }

    /** Wywolywane co tick klienta; liczy czas dzialania Auto Miner i okresowo zapisuje dane. */
    public void tick(boolean miningActive) {
        long now = System.nanoTime();
        long dtMs = (now - lastTickNs) / 1_000_000L;
        lastTickNs = now;
        if (miningActive && dtMs > 0 && dtMs < 1000) {
            current.miningMs += dtMs;
            dirty = true;
        }
        if (dirty && System.currentTimeMillis() - lastSaveMs > 30_000) {
            save();
        }
    }

    /** Rejestruje dynamiczne liczniki dla nowo zaznaczonych blokow. */
    public void syncSelection(BlockSelectionManager sel) {
        if (sel.version() == syncedSelectionVersion) return;
        syncedSelectionVersion = sel.version();
        for (String id : sel.selectedIds()) {
            current.counters.ensureBlock(id);
        }
    }

    /** Wolane dopiero po potwierdzeniu (po stronie serwera) wykopania bloku. */
    public void onBlockMined(String blockId) {
        ensureSessionStarted();
        current.blocksMined++;
        current.counters.addBlock(blockId);
        dirty = true;
    }

    public void onItemsGained(Map<String, Integer> gains) {
        ensureSessionStarted();
        for (Map.Entry<String, Integer> e : gains.entrySet()) {
            current.itemsCollected += e.getValue();
            current.counters.addItem(e.getKey(), e.getValue());
        }
        dirty = true;
    }

    public void onDeposited(Map<String, Integer> moved) {
        long total = 0;
        for (int n : moved.values()) total += n;
        if (total <= 0) return;
        current.itemsDeposited += total;
        current.depositOps++;
        dirty = true;
    }

    /** Archiwizuje biezaca sesje (jesli cokolwiek zawiera) i zaczyna nowa. */
    public void resetSession() {
        archiveCurrent(System.currentTimeMillis());
        current = new SessionStatistics();
        syncedSelectionVersion = -1;
        dirty = true;
        save();
    }

    public void clearAll() {
        history.clear();
        current = new SessionStatistics();
        syncedSelectionVersion = -1;
        dirty = true;
        save();
    }

    private void archiveCurrent(long endMs) {
        if (current.isEmpty()) return;
        if (current.startMs <= 0) current.startMs = endMs - current.miningMs;
        current.endMs = endMs;
        history.add(0, current);
        while (history.size() > MAX_HISTORY) history.remove(history.size() - 1);
    }

    public synchronized void load() {
        Path f = file();
        if (!Files.exists(f)) return;
        try (Reader r = Files.newBufferedReader(f, StandardCharsets.UTF_8)) {
            StatsFile sf = ConfigManager.GSON.fromJson(r, StatsFile.class);
            if (sf == null) return;
            if (sf.history != null) history.addAll(sf.history);
            // Poprzednia (nieudokumentowana jako zakonczona) sesja trafia do historii.
            if (sf.current != null && !sf.current.isEmpty()) {
                if (sf.current.counters == null) sf.current.counters = new ResourceCounter();
                if (sf.current.startMs <= 0) sf.current.startMs = sf.savedAtMs - sf.current.miningMs;
                sf.current.endMs = sf.savedAtMs > 0 ? sf.savedAtMs : System.currentTimeMillis();
                history.add(0, sf.current);
            }
            while (history.size() > MAX_HISTORY) history.remove(history.size() - 1);
            dirty = true;
        } catch (IOException | JsonParseException e) {
            AutoMiner.LOGGER.error("Nie udalo sie wczytac statystyk", e);
            try {
                Files.copy(f, f.resolveSibling("stats.json.broken-" + System.currentTimeMillis()));
            } catch (IOException ignored) {
                // kopia awaryjna jest opcjonalna
            }
        }
    }

    public synchronized void save() {
        Path f = file();
        Path tmp = f.resolveSibling("stats.json.tmp");
        StatsFile sf = new StatsFile();
        sf.savedAtMs = System.currentTimeMillis();
        sf.current = current;
        sf.history = history;
        try {
            Files.createDirectories(f.getParent());
            try (Writer w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                ConfigManager.GSON.toJson(sf, w);
            }
            try {
                Files.move(tmp, f, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, f, StandardCopyOption.REPLACE_EXISTING);
            }
            dirty = false;
            lastSaveMs = System.currentTimeMillis();
        } catch (IOException e) {
            AutoMiner.LOGGER.error("Zapis statystyk nie powiodl sie", e);
        }
    }

    /** Wywolywane przy zamykaniu gry: zamyka biezaca sesje i zapisuje. */
    public void shutdown() {
        if (!current.isEmpty()) {
            current.endMs = System.currentTimeMillis();
        }
        save();
    }
}
