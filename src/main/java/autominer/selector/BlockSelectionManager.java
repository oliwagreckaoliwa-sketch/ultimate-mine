package autominer.selector;

import autominer.AutoMiner;
import autominer.config.ConfigManager;
import autominer.config.ModConfig;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.util.Identifier;

/** Lista zaznaczonych blokow (grupy) + zapisywanie/wczytywanie list do plikow. */
public final class BlockSelectionManager {
    public enum Result { ADDED, ALREADY, REMOVED, NOT_SELECTED, INVALID }

    private static final class SavedList {
        String activeGroup;
        List<BlockGroup> groups = new ArrayList<>();
    }

    private int version;
    private int cachedVersion = -1;
    private Set<String> cached = Set.of();

    private ModConfig cfg() {
        return ConfigManager.get();
    }

    /** Zwieksza sie przy kazdej zmianie zaznaczenia; inne moduly na tej podstawie odswiezaja cache. */
    public int version() {
        return version;
    }

    private void changed() {
        version++;
        ConfigManager.markDirty();
    }

    public List<BlockGroup> groups() {
        return cfg().groups;
    }

    public BlockGroup find(String name) {
        for (BlockGroup g : cfg().groups) {
            if (g.name.equalsIgnoreCase(name)) return g;
        }
        return null;
    }

    public BlockGroup active() {
        BlockGroup g = find(cfg().activeGroup);
        return g != null ? g : cfg().groups.get(0);
    }

    public static String normalize(String id) {
        if (id == null) return null;
        Identifier i = Identifier.tryParse(id.trim().toLowerCase(Locale.ROOT));
        return i == null ? null : i.toString();
    }

    /** Suma blokow ze wszystkich wlaczonych grup. */
    public Set<String> selectedIds() {
        if (cachedVersion != version) {
            Set<String> s = new HashSet<>();
            for (BlockGroup g : cfg().groups) {
                if (g.enabled) s.addAll(g.blocks);
            }
            cached = s;
            cachedVersion = version;
        }
        return cached;
    }

    public int selectedCount() {
        return selectedIds().size();
    }

    public boolean isSelected(String id) {
        return selectedIds().contains(id);
    }

    public Result add(String rawId) {
        String id = normalize(rawId);
        if (id == null) return Result.INVALID;
        BlockGroup g = active();
        if (g.blocks.contains(id)) return Result.ALREADY;
        g.blocks.add(id);
        changed();
        return Result.ADDED;
    }

    /** Usuwa dany rodzaj bloku ze wszystkich grup. */
    public Result remove(String rawId) {
        String id = normalize(rawId);
        if (id == null) return Result.INVALID;
        boolean any = false;
        for (BlockGroup g : cfg().groups) {
            any |= g.blocks.remove(id);
        }
        if (!any) return Result.NOT_SELECTED;
        changed();
        return Result.REMOVED;
    }

    public boolean addTo(BlockGroup g, String id) {
        if (g.blocks.contains(id)) return false;
        g.blocks.add(id);
        changed();
        return true;
    }

    public boolean removeFrom(BlockGroup g, String id) {
        boolean r = g.blocks.remove(id);
        if (r) changed();
        return r;
    }

    // --- grupy ---

    public static String cleanName(String raw) {
        if (raw == null) return "";
        String s = raw.trim().replaceAll("\\s+", " ");
        return s.length() > 24 ? s.substring(0, 24).trim() : s;
    }

    public boolean createGroup(String raw) {
        String n = cleanName(raw);
        if (n.isEmpty() || find(n) != null) return false;
        cfg().groups.add(new BlockGroup(n));
        changed();
        return true;
    }

    public boolean deleteGroup(BlockGroup g) {
        if (cfg().groups.size() <= 1) return false;
        cfg().groups.remove(g);
        if (find(cfg().activeGroup) == null) cfg().activeGroup = cfg().groups.get(0).name;
        changed();
        return true;
    }

    public void setActive(BlockGroup g) {
        cfg().activeGroup = g.name;
        ConfigManager.markDirty();
    }

    public void setEnabled(BlockGroup g, boolean enabled) {
        g.enabled = enabled;
        changed();
    }

    // --- zapisane listy ---

    private Path listsDir() {
        return ConfigManager.dir().resolve("lists");
    }

    public static String cleanFileName(String raw) {
        String s = raw == null ? "" : raw.trim().replaceAll("[^A-Za-z0-9 _-]", "");
        return s.length() > 32 ? s.substring(0, 32).trim() : s;
    }

    public boolean saveList(String raw) {
        String name = cleanFileName(raw);
        if (name.isEmpty()) return false;
        SavedList sl = new SavedList();
        sl.activeGroup = cfg().activeGroup;
        sl.groups = cfg().groups;
        try {
            Files.createDirectories(listsDir());
            try (Writer w = Files.newBufferedWriter(listsDir().resolve(name + ".json"), StandardCharsets.UTF_8)) {
                ConfigManager.GSON.toJson(sl, w);
            }
            return true;
        } catch (IOException e) {
            AutoMiner.LOGGER.error("Zapis listy nie powiodl sie", e);
            return false;
        }
    }

    public boolean loadList(String raw) {
        String name = cleanFileName(raw);
        if (name.isEmpty()) return false;
        Path f = listsDir().resolve(name + ".json");
        if (!Files.exists(f)) return false;
        try (Reader r = Files.newBufferedReader(f, StandardCharsets.UTF_8)) {
            SavedList sl = ConfigManager.GSON.fromJson(r, SavedList.class);
            if (sl == null || sl.groups == null || sl.groups.isEmpty()) return false;
            for (BlockGroup g : sl.groups) {
                if (g.name == null || g.name.isBlank()) g.name = "Group";
                if (g.blocks == null) g.blocks = new ArrayList<>();
                List<String> fixed = new ArrayList<>();
                for (String b : g.blocks) {
                    String n = normalize(b);
                    if (n != null && !fixed.contains(n)) fixed.add(n);
                }
                g.blocks = fixed;
            }
            cfg().groups = sl.groups;
            cfg().activeGroup = sl.activeGroup != null ? sl.activeGroup : sl.groups.get(0).name;
            cfg().sanitize();
            changed();
            return true;
        } catch (IOException | JsonParseException e) {
            AutoMiner.LOGGER.error("Wczytanie listy nie powiodlo sie", e);
            return false;
        }
    }

    public List<String> savedLists() {
        List<String> out = new ArrayList<>();
        if (!Files.isDirectory(listsDir())) return out;
        try (Stream<Path> s = Files.list(listsDir())) {
            s.map(p -> p.getFileName().toString())
                    .filter(n -> n.endsWith(".json"))
                    .map(n -> n.substring(0, n.length() - 5))
                    .sorted(Comparator.comparing(String::toLowerCase))
                    .forEach(out::add);
        } catch (IOException e) {
            AutoMiner.LOGGER.warn("Nie mozna odczytac katalogu list", e);
        }
        return out;
    }
}
