package autominer.gui;

import autominer.AutoMiner;
import autominer.config.ConfigManager;
import autominer.config.ModConfig;
import autominer.mining.AutoMiningManager;
import autominer.mining.ToolManager;
import autominer.statistics.SessionStatistics;
import autominer.util.Fmt;
import net.minecraft.client.MinecraftClient;

/** Dashboard: statusy, liczniki sesji i duzy przycisk START / STOP. */
public final class DashboardScreen extends GuiPanel {
    private int contentH = 240;

    @Override
    public String title() {
        return "Dashboard";
    }

    @Override
    public void render(Ui ui, int x, int y, int w, int h, MinecraftClient c) {
        AutoMiningManager m = AutoMiner.mining;
        ModConfig cfg = ConfigManager.get();
        SessionStatistics s = AutoMiner.stats.current();
        int off = ui.scrollBegin("dash", x, y, w, h, contentH);
        int cw = w - 8;
        int top = y - off;
        int cy = top + 2;

        boolean active = m.isActive();
        int bigW = cw * 62 / 100;
        ui.button("startstop", x, cy, bigW, 30, active ? "STOP" : "START",
                active ? Ui.Style.DANGER : Ui.Style.SUCCESS, true, () -> {
                    if (m.isActive()) {
                        m.stop("");
                    } else if (m.start()) {
                        c.setScreen(null);
                    }
                });
        ui.button("pause", x + bigW + 8, cy, cw - bigW - 8, 30,
                m.state() == AutoMiningManager.State.PAUSED ? "RESUME" : "PAUSE",
                Ui.Style.GHOST, active, m::togglePause);
        cy += 40;

        int cols = cw >= 360 ? 3 : 2;
        int gap = 8;
        int cardW = (cw - gap * (cols - 1)) / cols;
        int cardH = 44;

        int stCol = m.state() == AutoMiningManager.State.RUNNING ? Draw.ON
                : m.state() == AutoMiningManager.State.PAUSED ? Draw.WARN : Draw.OFF;
        ToolManager.ToolInfo tool = ToolManager.describe(c.player);
        long now = System.currentTimeMillis();

        String[] labels = {"AUTO MINER", "AUTO DEPOSIT", "SELECTED BLOCKS", "BLOCKS MINED", "SESSION TIME", "DEPOSITED", "PICKAXE"};
        String[] values = {
                m.statusText(),
                cfg.depositEnabled ? "ON" : "OFF",
                String.valueOf(AutoMiner.selection.selectedCount()),
                Fmt.num(ui.num("mined", s.blocksMined)),
                Fmt.time(s.elapsedMs(now)),
                Fmt.num(ui.num("dep", s.itemsDeposited)),
                tool.present() ? tool.percent() + "%" : "NONE"};
        String[] subs = {
                m.detailText(),
                AutoMiner.deposit.busy() ? "Working: " + AutoMiner.deposit.phase() : "Interval " + cfg.depositIntervalSeconds + "s",
                AutoMiner.selection.groups().size() + " groups",
                "Mining " + Fmt.time(s.miningMs),
                "Items collected: " + Fmt.num(s.itemsCollected),
                s.depositOps + " deposit ops",
                tool.present() ? tool.name() : "No pickaxe in hotbar"};
        int[] colors = {stCol, cfg.depositEnabled ? Draw.ON : Draw.OFF, Draw.TEXT, Draw.TEXT, Draw.TEXT, Draw.TEXT,
                !tool.present() ? Draw.OFF : tool.percent() < 15 ? Draw.WARN : Draw.TEXT};

        for (int i = 0; i < labels.length; i++) {
            int cx = x + (i % cols) * (cardW + gap);
            int ry = cy + (i / cols) * (cardH + gap);
            ui.card(cx, ry, cardW, cardH);
            ui.text(labels[i], cx + 8, ry + 6, Draw.MUTED);
            ui.textScaled(ui.fit(values[i], cardW - 50), cx + 8, ry + 17, colors[i], 1.4f);
            ui.text(ui.fit(subs[i], cardW - 14), cx + 8, ry + 33, Draw.MUTED);
            if (i == 1) {
                ui.toggle("dash-dep", cx + cardW - 36, ry + 6, cfg.depositEnabled, b -> AutoMiner.deposit.toggleEnabled());
            }
        }
        int rows = (labels.length + cols - 1) / cols;
        cy += rows * (cardH + gap);

        ui.text("Shortcuts: F5 add block | Shift+F5 remove | F6 miner | F7 deposit | F8 HUD | F9 menu", x + 2, cy, Draw.MUTED);
        cy += 14;

        contentH = cy - top + 6;
        ui.scrollEnd();
    }
}
