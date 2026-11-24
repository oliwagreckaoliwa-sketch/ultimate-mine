package autominer.gui;

import autominer.AutoMiner;
import autominer.config.ConfigManager;
import autominer.config.ModConfig;
import autominer.mining.AutoMiningManager;
import net.minecraft.client.MinecraftClient;

/** Zakladka "Auto Mining": ustawienia kopania i sterowanie. */
public final class MiningScreen extends GuiPanel {
    private static final int ROW = 20;
    private int contentH = 300;

    @Override
    public String title() {
        return "Auto Mining";
    }

    @Override
    public void render(Ui ui, int x, int y, int w, int h, MinecraftClient c) {
        ModConfig cfg = ConfigManager.get();
        AutoMiningManager m = AutoMiner.mining;
        int off = ui.scrollBegin("mining", x, y, w, h, contentH);
        int cw = w - 8;
        int top = y - off;
        int cy = top + 2;

        boolean active = m.isActive();
        int half = (cw - 8) / 2;
        ui.button("m-start", x, cy, half, 24, active ? "STOP" : "START",
                active ? Ui.Style.DANGER : Ui.Style.SUCCESS, true, () -> {
                    if (m.isActive()) {
                        m.stop("");
                    } else if (m.start()) {
                        c.setScreen(null);
                    }
                });
        ui.button("m-pause", x + half + 8, cy, half, 24,
                m.state() == AutoMiningManager.State.PAUSED ? "RESUME" : "PAUSE",
                Ui.Style.GHOST, active, m::togglePause);
        cy += 30;
        ui.text("Status: " + m.detailText(), x + 2, cy, Draw.MUTED);
        cy += 12;
        ui.text("Selected blocks: " + AutoMiner.selection.selectedCount() + "  (edit in Block Selector)", x + 2, cy, Draw.MUTED);
        cy += 16;

        int rows = 11;
        ui.card(x, cy, cw, rows * ROW + 14);
        int ix = x + 10;
        int iw = cw - 20;
        int ry = cy + 8;
        stepD(ui, "range", ix, ry, iw, "Mining range (blocks)", cfg.miningRange, 0.5, 1.5, 6.0, "%.1f", v -> cfg.miningRange = v);
        ry += ROW;
        stepI(ui, "scan", ix, ry, iw, "Target scan interval (ticks)", cfg.scanIntervalTicks, 1, 1, 40, "", v -> cfg.scanIntervalTicks = v);
        ry += ROW;
        stepD(ui, "rot", ix, ry, iw, "Rotation speed (deg/tick)", cfg.rotationSpeed, 5.0, 2.0, 90.0, "%.0f", v -> cfg.rotationSpeed = v);
        ry += ROW;
        stepI(ui, "dur", ix, ry, iw, "Stop at durability left", cfg.minDurability, 5, 0, 500, "", v -> cfg.minDurability = v);
        ry += ROW;
        stepI(ui, "idle", ix, ry, iw, "Stop when idle (0 = never)", cfg.idleStopSeconds, 10, 0, 3600, " s", v -> cfg.idleStopSeconds = v);
        ry += ROW;
        stepD(ui, "walkr", ix, ry, iw, "Walk search range", cfg.walkRange, 1.0, 3.0, 24.0, "%.0f", v -> cfg.walkRange = v);
        ry += ROW;
        tog(ui, "reqpick", ix, ry, iw, "Require a pickaxe (stop if none)", cfg.requirePickaxe, b -> cfg.requirePickaxe = b);
        ry += ROW;
        tog(ui, "vis", ix, ry, iw, "Only blocks in line of sight", cfg.visibleOnly, b -> cfg.visibleOnly = b);
        ry += ROW;
        tog(ui, "dmg", ix, ry, iw, "Stop when taking damage", cfg.stopOnDamage, b -> cfg.stopOnDamage = b);
        ry += ROW;
        tog(ui, "walk", ix, ry, iw, "Walk to next block (experimental)", cfg.walkToTargets, b -> cfg.walkToTargets = b);
        ry += ROW;
        ui.text("Only hotbar pickaxes are used. Server rules always apply.", ix, ry + 2, Draw.MUTED);
        cy += rows * ROW + 20;

        contentH = cy - top + 6;
        ui.scrollEnd();
    }
}
