package autominer.mining;

import autominer.AutoMiner;
import autominer.config.ConfigManager;
import autominer.config.ModConfig;
import autominer.selector.BlockInfo;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.world.GameMode;

/** Stan Auto Miner (start/stop/pauza), walidacja warunkow i spinanie z licznikami. */
public final class AutoMiningManager {
    public enum State { STOPPED, RUNNING, PAUSED }

    public enum Status { OFF, SEARCHING, MINING, WALKING, PAUSED, DEPOSITING, WAITING }

    private static final int LEARN_WINDOW_TICKS = 40;

    private final BlockWhitelist whitelist;
    private final MiningController controller;

    private State state = State.STOPPED;
    private Status status = Status.OFF;
    private String lastReason = "";
    private boolean suspended;
    private float lastHealth;
    private long lastBreakTick = -1000;

    public AutoMiningManager(BlockWhitelist whitelist) {
        this.whitelist = whitelist;
        this.controller = new MiningController(whitelist);
        this.controller.onConfirmed = (id, tick) -> {
            AutoMiner.stats.onBlockMined(id);
            lastBreakTick = tick;
        };
    }

    public State state() {
        return state;
    }

    public Status status() {
        return status;
    }

    public boolean isRunning() {
        return state == State.RUNNING;
    }

    public boolean isActive() {
        return state != State.STOPPED;
    }

    public String lastReason() {
        return lastReason;
    }

    public MiningController controller() {
        return controller;
    }

    public String statusText() {
        return switch (state) {
            case STOPPED -> "OFF";
            case PAUSED -> "PAUSED";
            case RUNNING -> "ON";
        };
    }

    public String detailText() {
        return switch (status) {
            case OFF -> lastReason.isEmpty() ? "Stopped" : lastReason;
            case SEARCHING -> "Searching for blocks";
            case MINING -> "Mining";
            case WALKING -> "Walking to next block";
            case PAUSED -> "Paused";
            case DEPOSITING -> "Depositing items";
            case WAITING -> "Waiting (screen open)";
        };
    }

    /** Pozwala modulowi depozytu wstrzymac kopanie na czas przenoszenia. */
    public void setSuspended(boolean s) {
        suspended = s;
    }

    public boolean start() {
        MinecraftClient c = MinecraftClient.getInstance();
        ModConfig cfg = ConfigManager.get();
        if (c.player == null || c.world == null || c.interactionManager == null) {
            return false;
        }
        if (whitelist.isEmpty()) {
            AutoMiner.notifications.warn("NO BLOCKS SELECTED - look at a block and press F5");
            return false;
        }
        GameMode gm = c.interactionManager.getCurrentGameMode();
        if (gm == GameMode.SPECTATOR || gm == GameMode.ADVENTURE) {
            AutoMiner.notifications.warn("GAME MODE NOT SUPPORTED");
            return false;
        }
        if (cfg.requirePickaxe && !ToolManager.hasUsablePickaxe(c.player, cfg.minDurability)) {
            AutoMiner.notifications.error("NO USABLE PICKAXE IN HOTBAR");
            return false;
        }
        controller.reset(c);
        AutoMiner.stats.ensureSessionStarted();
        lastHealth = c.player.getHealth();
        lastReason = "";
        suspended = false;
        state = State.RUNNING;
        status = Status.SEARCHING;
        AutoMiner.notifications.success("AUTO MINER: ON");
        return true;
    }

    public void stop(String reason) {
        MinecraftClient c = MinecraftClient.getInstance();
        boolean was = state != State.STOPPED;
        state = State.STOPPED;
        status = Status.OFF;
        suspended = false;
        lastReason = reason == null ? "" : reason;
        controller.reset(c);
        if (was) {
            if (reason == null || reason.isEmpty()) {
                AutoMiner.notifications.info("AUTO MINER: OFF");
            } else {
                AutoMiner.notifications.warn("AUTO MINER STOPPED: " + reason);
            }
        }
    }

    public void toggle() {
        if (state == State.STOPPED) {
            start();
        } else {
            stop("");
        }
    }

    public void togglePause() {
        if (state == State.RUNNING) {
            state = State.PAUSED;
            AutoMiner.notifications.info("AUTO MINER: PAUSED");
        } else if (state == State.PAUSED) {
            state = State.RUNNING;
            lastHealth = MinecraftClient.getInstance().player != null
                    ? MinecraftClient.getInstance().player.getHealth() : 0f;
            AutoMiner.notifications.info("AUTO MINER: RESUMED");
        }
    }

    /** Czy aktualne przyrosty w ekwipunku nalezy wliczac do statystyk. */
    public boolean isCountingItems(MinecraftClient c) {
        return state == State.RUNNING && !suspended && c.currentScreen == null;
    }

    /** Wolane po wykryciu przyrostu przedmiotow: liczy je i uczy sie dropow nowych blokow. */
    public void onItemsGained(Map<String, Integer> gains) {
        AutoMiner.stats.onItemsGained(gains);
        ModConfig cfg = ConfigManager.get();
        if (!cfg.autoLearnDrops || AutoMiner.tick - lastBreakTick > LEARN_WINDOW_TICKS) return;
        boolean changed = false;
        for (String id : gains.keySet()) {
            if (cfg.depositFilter.contains(id)) continue;
            Item item = BlockInfo.item(id);
            if (item == null || item.getMaxCount() <= 1) continue; // narzedzia/bron nie sa dropem blokow
            cfg.depositFilter.add(id);
            cfg.learnedDrops.add(id);
            AutoMiner.notifications.info("DROP LEARNED: " + BlockInfo.itemName(id));
            changed = true;
        }
        if (changed) ConfigManager.markDirty();
    }

    public void tick(MinecraftClient c, long tick) {
        controller.processPending(c, tick);

        if (state == State.STOPPED) {
            status = Status.OFF;
            return;
        }
        if (c.player == null || c.world == null || c.interactionManager == null) {
            stop("LEFT THE WORLD");
            return;
        }
        if (!c.player.isAlive()) {
            stop("PLAYER DIED");
            return;
        }
        if (state == State.PAUSED) {
            controller.releaseKeys(c);
            status = Status.PAUSED;
            return;
        }
        if (suspended) {
            controller.releaseKeys(c);
            status = Status.DEPOSITING;
            return;
        }
        if (c.currentScreen != null) {
            controller.releaseKeys(c);
            status = Status.WAITING;
            return;
        }
        ModConfig cfg = ConfigManager.get();
        GameMode gm = c.interactionManager.getCurrentGameMode();
        if (gm == GameMode.SPECTATOR || gm == GameMode.ADVENTURE) {
            stop("GAME MODE NOT SUPPORTED");
            return;
        }
        float hp = c.player.getHealth();
        if (cfg.stopOnDamage && hp < lastHealth - 0.01f) {
            stop("DAMAGE TAKEN");
            return;
        }
        lastHealth = hp;
        if (whitelist.isEmpty()) {
            stop("NO BLOCKS SELECTED");
            return;
        }

        MiningController.Outcome o = controller.tick(c, tick);
        switch (o) {
            case MINING -> status = Status.MINING;
            case SEARCHING -> status = Status.SEARCHING;
            case WALKING -> status = Status.WALKING;
            case STOP_NO_TOOL -> stop("NO USABLE PICKAXE (missing or too damaged)");
            case STOP_SERVER -> {
                stop("SERVER BLOCKED MINING");
                AutoMiner.notifications.error("The server is not accepting block breaking");
            }
            case STOP_IDLE -> stop("NO BLOCKS FOUND");
        }
    }
}
