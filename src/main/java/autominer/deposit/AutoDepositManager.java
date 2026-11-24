package autominer.deposit;

import autominer.AutoMiner;
import autominer.config.ConfigManager;
import autominer.config.ModConfig;
import autominer.selector.BlockInfo;
import autominer.util.AimHelper;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Automat stanow: IDLE -> AIM -> OPENING -> TRANSFER -> CONFIRM -> CLOSING.
 * Uzywa wylacznie zwyklych akcji klienta (klikniecie bloku, shift-klik na slocie).
 */
public final class AutoDepositManager {
    public enum Phase { IDLE, AIM, OPENING, TRANSFER, CONFIRM, CLOSING }

    private static final int AIM_TIMEOUT = 80;
    private static final int OPEN_TIMEOUT = 50;
    private static final int CONFIRM_WAIT = 8;

    private Phase phase = Phase.IDLE;
    private long phaseStart;
    private long lastAttempt;
    private long cooldownUntil;
    private long warnUntil;
    private boolean manual;
    private int failures;
    private int lastFreeSlots = -1;
    private String lastResult = "";

    private ScreenHandler handler;
    private final Deque<Integer> queue = new ArrayDeque<>();
    private Map<String, Integer> before = new HashMap<>();

    public Phase phase() {
        return phase;
    }

    public boolean busy() {
        return phase != Phase.IDLE;
    }

    /** Liczba wolnych slotow skrzynki zaobserwowana przy ostatnim otwarciu (-1 = nieznana). */
    public int lastFreeSlots() {
        return lastFreeSlots;
    }

    public String lastResult() {
        return lastResult;
    }

    public void requestNow() {
        manual = true;
    }

    public void toggleEnabled() {
        ModConfig cfg = ConfigManager.get();
        cfg.depositEnabled = !cfg.depositEnabled;
        if (cfg.depositEnabled) {
            failures = 0;
            cooldownUntil = 0;
        }
        ConfigManager.markDirty();
        AutoMiner.notifications.info("AUTO DEPOSIT: " + (cfg.depositEnabled ? "ON" : "OFF"));
    }

    public int pendingItems(MinecraftClient c) {
        if (c.player == null) return 0;
        return InventoryTransfer.countMatching(c.player, InventoryTransfer.filterSet(ConfigManager.get()));
    }

    public void tick(MinecraftClient c, long tick) {
        if (c.player == null || c.world == null || c.interactionManager == null) {
            if (phase != Phase.IDLE) reset();
            manual = false;
            return;
        }
        switch (phase) {
            case IDLE -> idle(c, tick);
            case AIM -> aim(c, tick);
            case OPENING -> opening(c, tick);
            case TRANSFER -> transfer(c, tick);
            case CONFIRM -> confirm(c, tick);
            case CLOSING -> closing(c, tick);
        }
    }

    private void idle(MinecraftClient c, long tick) {
        ModConfig cfg = ConfigManager.get();
        if (c.currentScreen != null) return;
        boolean auto = cfg.depositEnabled && tick >= cooldownUntil;
        if (!manual && !auto) return;
        ClientPlayerEntity p = c.player;
        Set<String> filter = InventoryTransfer.filterSet(cfg);
        int pending = InventoryTransfer.countMatching(p, filter);

        if (!cfg.chestSet) {
            if (manual) AutoMiner.notifications.warn("NO DEPOSIT CHEST SELECTED");
            manual = false;
            return;
        }
        if (!manual) {
            boolean timeUp = tick - lastAttempt >= cfg.depositIntervalSeconds * 20L;
            boolean full = cfg.depositWhenFull && InventoryTransfer.freeMainSlots(p) <= cfg.fullFreeSlots;
            if (pending == 0 || !(timeUp || full)) return;
        } else if (pending == 0) {
            AutoMiner.notifications.info("NOTHING TO DEPOSIT");
            manual = false;
            return;
        }

        ChestManager.State s = AutoMiner.chests.state(c);
        if (s != ChestManager.State.READY) {
            if (manual || tick >= warnUntil) {
                switch (s) {
                    case OUT_OF_RANGE -> AutoMiner.notifications.warn("CHEST OUT OF REACH - move closer");
                    case MISSING -> AutoMiner.notifications.error("NO CHEST AT SAVED POSITION");
                    case OTHER_WORLD -> AutoMiner.notifications.warn("DEPOSIT CHEST IS IN ANOTHER WORLD");
                    default -> { }
                }
                warnUntil = tick + 600;
            }
            manual = false;
            cooldownUntil = tick + 100;
            return;
        }
        manual = false;
        lastAttempt = tick;
        phase = Phase.AIM;
        phaseStart = tick;
        AutoMiner.mining.setSuspended(true);
    }

    private void aim(MinecraftClient c, long tick) {
        if (c.currentScreen != null) {
            fail(c, tick, "DEPOSIT INTERRUPTED");
            return;
        }
        if (AutoMiner.chests.state(c) != ChestManager.State.READY) {
            fail(c, tick, "CHEST NOT REACHABLE");
            return;
        }
        if (tick - phaseStart > AIM_TIMEOUT) {
            fail(c, tick, "CHEST NOT IN VIEW");
            return;
        }
        BlockPos pos = AutoMiner.chests.position();
        double speed = Math.max(ConfigManager.get().rotationSpeed, 15.0);
        double err = AimHelper.apply(c.player, Vec3d.ofCenter(pos), speed);
        HitResult h = c.crosshairTarget;
        if (err < 4.0 && h instanceof BlockHitResult bhr && h.getType() == HitResult.Type.BLOCK
                && AutoMiner.chests.matches(c, bhr.getBlockPos())) {
            ActionResult r = c.interactionManager.interactBlock(c.player, Hand.MAIN_HAND, bhr);
            if (r.shouldSwingHand()) c.player.swingHand(Hand.MAIN_HAND);
            phase = Phase.OPENING;
            phaseStart = tick;
        }
    }

    private void opening(MinecraftClient c, long tick) {
        if (c.currentScreen instanceof GenericContainerScreen gs) {
            handler = gs.getScreenHandler();
            ModConfig cfg = ConfigManager.get();
            Set<String> filter = InventoryTransfer.filterSet(cfg);
            lastFreeSlots = InventoryTransfer.containerFreeSlots(c.player, handler);
            before = InventoryTransfer.matchingCounts(c.player, filter);
            queue.clear();
            queue.addAll(InventoryTransfer.slotsToMove(c.player, handler, filter));
            phase = Phase.TRANSFER;
            phaseStart = tick;
        } else if (c.currentScreen != null) {
            fail(c, tick, "UNSUPPORTED CONTAINER");
        } else if (tick - phaseStart > OPEN_TIMEOUT) {
            fail(c, tick, "SERVER BLOCKED CHEST INTERACTION");
        }
    }

    private void transfer(MinecraftClient c, long tick) {
        if (!(c.currentScreen instanceof GenericContainerScreen gs) || gs.getScreenHandler() != handler) {
            fail(c, tick, "CONTAINER CLOSED UNEXPECTEDLY");
            return;
        }
        ModConfig cfg = ConfigManager.get();
        Set<String> filter = InventoryTransfer.filterSet(cfg);
        int n = cfg.depositClicksPerTick;
        while (n-- > 0 && !queue.isEmpty()) {
            int id = queue.pollFirst();
            Slot s = handler.getSlot(id);
            if (!s.hasStack() || !filter.contains(BlockInfo.itemId(s.getStack().getItem()))) continue;
            c.interactionManager.clickSlot(handler.syncId, id, 0, SlotActionType.QUICK_MOVE, c.player);
        }
        if (queue.isEmpty()) {
            phase = Phase.CONFIRM;
            phaseStart = tick;
        }
    }

    private void confirm(MinecraftClient c, long tick) {
        if (tick - phaseStart < CONFIRM_WAIT) return;
        ModConfig cfg = ConfigManager.get();
        Set<String> filter = InventoryTransfer.filterSet(cfg);
        Map<String, Integer> after = InventoryTransfer.matchingCounts(c.player, filter);
        Map<String, Integer> moved = new HashMap<>();
        int total = 0;
        for (Map.Entry<String, Integer> e : before.entrySet()) {
            int diff = e.getValue() - after.getOrDefault(e.getKey(), 0);
            if (diff > 0) {
                moved.put(e.getKey(), diff);
                total += diff;
            }
        }
        int left = 0;
        for (int v : after.values()) left += v;
        if (total > 0) {
            AutoMiner.stats.onDeposited(moved);
            failures = 0;
            lastResult = "Deposited " + total + " items";
            if (left > 0) {
                AutoMiner.notifications.warn("CHEST FULL - " + left + " items kept");
            } else {
                AutoMiner.notifications.success("DEPOSITED " + total + " ITEMS");
            }
        } else {
            lastResult = "Nothing moved";
            failures++;
            AutoMiner.notifications.warn("CHEST FULL OR SERVER REFUSED - ITEMS KEPT");
        }
        phase = Phase.CLOSING;
        phaseStart = tick;
    }

    private void closing(MinecraftClient c, long tick) {
        if (c.currentScreen instanceof HandledScreen<?>) c.player.closeHandledScreen();
        finish(tick, failures >= 3 ? tick + 1200 : tick + 40);
        if (failures >= 3) {
            ConfigManager.get().depositEnabled = false;
            ConfigManager.markDirty();
            AutoMiner.notifications.error("AUTO DEPOSIT DISABLED after repeated failures");
            failures = 0;
        }
    }

    private void fail(MinecraftClient c, long tick, String message) {
        AutoMiner.notifications.error(message);
        lastResult = message;
        if (c.player != null && c.currentScreen instanceof HandledScreen<?>) c.player.closeHandledScreen();
        failures++;
        boolean disable = failures >= 3;
        finish(tick, tick + 600);
        if (disable) {
            ConfigManager.get().depositEnabled = false;
            ConfigManager.markDirty();
            AutoMiner.notifications.error("AUTO DEPOSIT DISABLED after repeated failures");
            failures = 0;
        }
    }

    private void finish(long tick, long cooldown) {
        phase = Phase.IDLE;
        cooldownUntil = cooldown;
        handler = null;
        queue.clear();
        AutoMiner.mining.setSuspended(false);
    }

    /** Przerwanie bez komunikatow (np. rozlaczenie z serwerem). */
    public void reset() {
        phase = Phase.IDLE;
        handler = null;
        queue.clear();
        manual = false;
        AutoMiner.mining.setSuspended(false);
    }
}
