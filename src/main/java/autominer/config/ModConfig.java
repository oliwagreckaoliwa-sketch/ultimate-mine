package autominer.config;

import autominer.selector.BlockGroup;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Dane konfiguracji zapisywane do JSON (Gson). Pola publiczne celowo, to prosty POJO. */
public class ModConfig {
    public static final List<String> DEFAULT_FILTER = List.of(
            "minecraft:iron_ore", "minecraft:raw_iron",
            "minecraft:gold_ore", "minecraft:raw_gold",
            "minecraft:diamond_ore", "minecraft:diamond",
            "minecraft:cobblestone", "minecraft:stone", "minecraft:deepslate");

    public static final String[] HUD_KEYS = {
            "status", "iron_ore", "raw_iron", "gold_ore", "raw_gold", "diamond_ore", "diamonds",
            "cobblestone", "stone", "deepslate", "blocks_mined", "session", "deposited",
            "items_collected", "mining_time", "pickaxe", "dynamic"};

    public int configVersion = 1;

    // --- Auto Miner ---
    public double miningRange = 4.5;
    public int scanIntervalTicks = 5;
    public double rotationSpeed = 20.0;
    public boolean requirePickaxe = true;
    public int minDurability = 10;
    public boolean visibleOnly = true;
    public boolean stopOnDamage = true;
    public boolean walkToTargets = false;
    public double walkRange = 10.0;
    public int idleStopSeconds = 0;

    // --- Block Selector ---
    public List<BlockGroup> groups = new ArrayList<>();
    public String activeGroup = "Default";
    public boolean showOutline = true;
    public boolean showBlockLabel = true;

    // --- Auto Deposit ---
    public boolean depositEnabled = false;
    public int depositIntervalSeconds = 60;
    public boolean depositWhenFull = true;
    public int fullFreeSlots = 0;
    public int depositClicksPerTick = 1;
    public boolean autoLearnDrops = true;
    public List<String> depositFilter = new ArrayList<>(DEFAULT_FILTER);
    public List<String> depositFilterOff = new ArrayList<>();
    public List<String> learnedDrops = new ArrayList<>();
    public boolean chestSet = false;
    public String chestWorldKey = "";
    public String chestDimension = "";
    public int chestX, chestY, chestZ;

    // --- HUD ---
    public boolean hudVisible = true;
    public int hudX = 6;
    public int hudY = 6;
    public double hudScale = 1.0;
    public double hudOpacity = 0.75;
    public Map<String, Boolean> hudStats = new LinkedHashMap<>();

    // --- Ogólne ---
    public boolean showNotifications = true;
    public double notificationSeconds = 2.5;
    public boolean animations = true;
    public int accentIndex = 0; // 0 = fiolet, 1 = niebieski
    public boolean remapPerspectiveKey = true;
    public boolean perspectiveRemapped = false;

    public boolean hudStat(String key) {
        return hudStats.getOrDefault(key, Boolean.FALSE);
    }

    /** Uzupełnia braki i przycina wartości do bezpiecznych zakresów. */
    public void sanitize() {
        miningRange = clamp(miningRange, 1.5, 6.0);
        scanIntervalTicks = clamp(scanIntervalTicks, 1, 40);
        rotationSpeed = clamp(rotationSpeed, 2.0, 90.0);
        minDurability = clamp(minDurability, 0, 500);
        walkRange = clamp(walkRange, 3.0, 24.0);
        idleStopSeconds = clamp(idleStopSeconds, 0, 3600);
        depositIntervalSeconds = clamp(depositIntervalSeconds, 5, 3600);
        fullFreeSlots = clamp(fullFreeSlots, 0, 8);
        depositClicksPerTick = clamp(depositClicksPerTick, 1, 4);
        hudScale = clamp(hudScale, 0.5, 2.0);
        hudOpacity = clamp(hudOpacity, 0.1, 1.0);
        hudX = clamp(hudX, 0, 4000);
        hudY = clamp(hudY, 0, 4000);
        notificationSeconds = clamp(notificationSeconds, 1.0, 8.0);
        accentIndex = clamp(accentIndex, 0, 1);

        if (groups == null) groups = new ArrayList<>();
        if (depositFilter == null) depositFilter = new ArrayList<>(DEFAULT_FILTER);
        if (depositFilterOff == null) depositFilterOff = new ArrayList<>();
        if (learnedDrops == null) learnedDrops = new ArrayList<>();
        if (hudStats == null) hudStats = new LinkedHashMap<>();
        if (chestWorldKey == null) chestWorldKey = "";
        if (chestDimension == null) chestDimension = "";

        for (BlockGroup g : groups) {
            if (g.name == null || g.name.isBlank()) g.name = "Group";
            if (g.blocks == null) g.blocks = new ArrayList<>();
        }
        if (groups.isEmpty()) {
            for (String n : new String[] {"Default", "Custom Rocks", "Ores", "Valuable Blocks", "Mining Area"}) {
                groups.add(new BlockGroup(n));
            }
        }
        if (activeGroup == null || groups.stream().noneMatch(g -> g.name.equals(activeGroup))) {
            activeGroup = groups.get(0).name;
        }

        boolean[] defaults = {true, true, true, true, true, true, true, true, false, false, true, true, true, false, false, false, false};
        for (int i = 0; i < HUD_KEYS.length; i++) {
            hudStats.putIfAbsent(HUD_KEYS[i], defaults[i]);
        }
    }

    private static double clamp(double v, double lo, double hi) {
        if (Double.isNaN(v)) return lo;
        return Math.max(lo, Math.min(hi, v));
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
