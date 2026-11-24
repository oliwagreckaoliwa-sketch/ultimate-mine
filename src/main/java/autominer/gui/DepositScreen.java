package autominer.gui;

import autominer.AutoMiner;
import autominer.config.ConfigManager;
import autominer.config.ModConfig;
import autominer.deposit.AutoDepositManager;
import autominer.deposit.ChestManager;
import autominer.selector.BlockInfo;
import autominer.util.Fmt;
import java.util.ArrayList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;

/** Zakladka "Auto Deposit": skrzynka, status, filtry i ustawienia depozytowania. */
public final class DepositScreen extends GuiPanel {
    private static final int ROW = 20;
    private int contentH = 500;

    @Override
    public String title() {
        return "Auto Deposit";
    }

    @Override
    public void render(Ui ui, int x, int y, int w, int h, MinecraftClient c) {
        ModConfig cfg = ConfigManager.get();
        AutoDepositManager d = AutoMiner.deposit;
        ChestManager ch = AutoMiner.chests;
        int off = ui.scrollBegin("deposit", x, y, w, h, contentH);
        int cw = w - 8;
        int top = y - off;
        int cy = top + 2;
        int ix = x + 10;
        int iw = cw - 20;

        // --- informacje o skrzynce ---
        ChestManager.State st = ch.state(c);
        String stText;
        int stCol;
        if (d.busy()) {
            stText = "Working: " + d.phase();
            stCol = ui.accent;
        } else {
            switch (st) {
                case READY -> {
                    stText = "Ready";
                    stCol = Draw.ON;
                }
                case OUT_OF_RANGE -> {
                    stText = "Out of reach - move closer";
                    stCol = Draw.WARN;
                }
                case OTHER_WORLD -> {
                    stText = "Chest is in another world";
                    stCol = Draw.WARN;
                }
                case MISSING -> {
                    stText = "No chest at saved position";
                    stCol = Draw.OFF;
                }
                default -> {
                    stText = "No chest selected";
                    stCol = Draw.WARN;
                }
            }
        }
        ui.card(x, cy, cw, 82);
        int ry = cy + 8;
        ui.text(cfg.chestSet ? "Chest: " + cfg.chestX + ", " + cfg.chestY + ", " + cfg.chestZ : "Chest: not selected", ix, ry, Draw.TEXT);
        ui.text(ui.fit(cfg.chestSet ? cfg.chestDimension + " | " + cfg.chestWorldKey : "", iw - 150), ix + 150, ry, Draw.MUTED);
        ry += 13;
        ui.text("Connection: " + stText, ix, ry, stCol);
        ry += 13;
        int free = d.lastFreeSlots();
        ui.text("Free chest slots: " + (free < 0 ? "unknown (seen when opened)" : String.valueOf(free)), ix, ry, Draw.TEXT);
        ry += 13;
        ui.text("Pending items: " + Fmt.num(ui.num("pending", d.pendingItems(c))), ix, ry, Draw.TEXT);
        ry += 13;
        ui.text("Last result: " + (d.lastResult().isEmpty() ? "-" : d.lastResult()), ix, ry, Draw.MUTED);
        cy += 90;

        int third = (cw - 16) / 3;
        ui.button("selchest", x, cy, third, 22, "SELECT CHEST", Ui.Style.PRIMARY, true, () -> {
            c.setScreen(null);
            ch.startPicking();
        });
        ui.button("clrchest", x + third + 8, cy, third, 22, "CLEAR CHEST", Ui.Style.GHOST, cfg.chestSet, ch::clear);
        ui.button("depnow", x + (third + 8) * 2, cy, cw - (third + 8) * 2, 22, "DEPOSIT NOW", Ui.Style.SUCCESS, !d.busy(), () -> {
            d.requestNow();
            c.setScreen(null);
        });
        cy += 30;

        // --- ustawienia ---
        int rows = 6;
        ui.card(x, cy, cw, rows * ROW + 14);
        ry = cy + 8;
        tog(ui, "depon", ix, ry, iw, "Auto Deposit enabled (F7)", cfg.depositEnabled, b -> AutoMiner.deposit.toggleEnabled());
        ry += ROW;
        int stepSec = cfg.depositIntervalSeconds < 60 ? 5 : cfg.depositIntervalSeconds < 600 ? 30 : 60;
        stepI(ui, "depint", ix, ry, iw, "Deposit interval", cfg.depositIntervalSeconds, stepSec, 5, 3600, " s", v -> cfg.depositIntervalSeconds = v);
        ry += ROW;
        tog(ui, "depfull", ix, ry, iw, "Deposit when inventory is full", cfg.depositWhenFull, b -> cfg.depositWhenFull = b);
        ry += ROW;
        stepI(ui, "depfree", ix, ry, iw, "Treat as full at free slots <=", cfg.fullFreeSlots, 1, 0, 8, "", v -> cfg.fullFreeSlots = v);
        ry += ROW;
        stepI(ui, "depclk", ix, ry, iw, "Item moves per tick", cfg.depositClicksPerTick, 1, 1, 4, "", v -> cfg.depositClicksPerTick = v);
        ry += ROW;
        tog(ui, "deplearn", ix, ry, iw, "Learn drops of newly mined blocks", cfg.autoLearnDrops, b -> cfg.autoLearnDrops = b);
        cy += rows * ROW + 22;

        // --- filtry ---
        section(ui, "Item filters (" + cfg.depositFilter.size() + ")", x + 2, cy + 4);
        ui.button("addheld", x + cw - 130, cy, 130, 16, "ADD HELD ITEM", Ui.Style.GHOST, true, () -> {
            if (c.player == null) return;
            ItemStack held = c.player.getMainHandStack();
            if (held.isEmpty()) {
                AutoMiner.notifications.warn("HOLD AN ITEM FIRST");
                return;
            }
            String id = BlockInfo.itemId(held.getItem());
            if (cfg.depositFilter.contains(id)) {
                AutoMiner.notifications.warn("ITEM ALREADY IN FILTER");
            } else {
                cfg.depositFilter.add(id);
                dirty();
                AutoMiner.notifications.success("FILTER ADDED: " + BlockInfo.itemName(id));
            }
        });
        cy += 22;
        for (String id : new ArrayList<>(cfg.depositFilter)) {
            boolean on = !cfg.depositFilterOff.contains(id);
            boolean hov = ui.hover(x, cy, cw, 22);
            ui.rect(x, cy, cw, 22, 4, hov ? Draw.CARD_HOVER : Draw.CARD);
            ui.item(BlockInfo.itemIcon(id), x + 6, cy + 3);
            int tw = cw - 30 - 120;
            ui.text(ui.fit(BlockInfo.itemName(id), tw), x + 30, cy + 3, on ? Draw.TEXT : Draw.MUTED);
            ui.text(ui.fit(id + (cfg.learnedDrops.contains(id) ? "  [AUTO]" : ""), tw), x + 30, cy + 12, Draw.MUTED);
            ui.toggle("flt-" + id, x + cw - 100, cy + 4, on, b -> {
                if (b) {
                    cfg.depositFilterOff.remove(id);
                } else if (!cfg.depositFilterOff.contains(id)) {
                    cfg.depositFilterOff.add(id);
                }
                dirty();
            });
            ui.button("fdel-" + id, x + cw - 62, cy + 3, 56, 16, "REMOVE", Ui.Style.DANGER, true, () -> {
                cfg.depositFilter.remove(id);
                cfg.depositFilterOff.remove(id);
                cfg.learnedDrops.remove(id);
                dirty();
            });
            cy += 24;
        }
        ui.text("Items are only moved by normal chest clicks; nothing is dropped or deleted.", x + 2, cy + 2, Draw.MUTED);
        cy += 16;

        contentH = cy - top + 8;
        ui.scrollEnd();
    }
}
