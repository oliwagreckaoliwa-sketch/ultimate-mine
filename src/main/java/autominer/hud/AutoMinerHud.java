package autominer.hud;

import autominer.AutoMiner;
import autominer.config.ConfigManager;
import autominer.config.ModConfig;
import autominer.gui.Draw;
import autominer.mining.AutoMiningManager;
import autominer.mining.ToolManager;
import autominer.selector.BlockInfo;
import autominer.statistics.ResourceCounter;
import autominer.statistics.SessionStatistics;
import autominer.util.Fmt;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.MinecraftClient;

/** Model HUD: sklada linie tekstu na podstawie konfiguracji i aktualnych statystyk. */
public final class AutoMinerHud {
    public record Line(String text, int color) {
        public static final Line GAP = new Line("", 0);

        public boolean gap() {
            return text.isEmpty();
        }
    }

    private AutoMinerHud() {}

    public static List<Line> lines(MinecraftClient c) {
        ModConfig cfg = ConfigManager.get();
        SessionStatistics s = AutoMiner.stats.current();
        List<Line> out = new ArrayList<>();
        int accent = Draw.accent(cfg.accentIndex);
        out.add(new Line("AUTOMINER", accent));

        if (cfg.hudStat("status")) {
            AutoMiningManager m = AutoMiner.mining;
            int col = m.state() == AutoMiningManager.State.RUNNING ? Draw.ON
                    : m.state() == AutoMiningManager.State.PAUSED ? Draw.WARN : Draw.OFF;
            out.add(new Line("STATUS: " + m.statusText(), col));
        }

        List<Line> counters = new ArrayList<>();
        Set<String> covered = new HashSet<>();
        for (ResourceCounter.Standard st : ResourceCounter.STANDARD) {
            covered.addAll(st.ids());
            if (cfg.hudStat(st.key())) {
                counters.add(new Line(st.label() + ": " + Fmt.num(s.counters.value(st)), Draw.TEXT));
            }
        }
        if (cfg.hudStat("dynamic")) {
            int shown = 0;
            int extra = 0;
            for (String id : AutoMiner.selection.selectedIds()) {
                if (covered.contains(id)) continue;
                if (shown < 6) {
                    counters.add(new Line(BlockInfo.name(id) + ": " + Fmt.num(s.counters.block(id)), Draw.TEXT));
                    shown++;
                } else {
                    extra++;
                }
            }
            if (extra > 0) counters.add(new Line("+" + extra + " more", Draw.MUTED));
        }
        if (!counters.isEmpty()) {
            out.add(Line.GAP);
            out.addAll(counters);
        }

        List<Line> totals = new ArrayList<>();
        long now = System.currentTimeMillis();
        if (cfg.hudStat("blocks_mined")) totals.add(new Line("Blocks mined: " + Fmt.num(s.blocksMined), Draw.TEXT));
        if (cfg.hudStat("session")) totals.add(new Line("Session: " + Fmt.time(s.elapsedMs(now)), Draw.TEXT));
        if (cfg.hudStat("mining_time")) totals.add(new Line("Mining time: " + Fmt.time(s.miningMs), Draw.TEXT));
        if (cfg.hudStat("deposited")) totals.add(new Line("Deposited: " + Fmt.num(s.itemsDeposited), Draw.TEXT));
        if (cfg.hudStat("items_collected")) totals.add(new Line("Items: " + Fmt.num(s.itemsCollected), Draw.TEXT));
        if (cfg.hudStat("pickaxe")) {
            ToolManager.ToolInfo t = ToolManager.describe(c.player);
            totals.add(new Line("Pickaxe: " + (t.present() ? t.percent() + "%" : "NONE"),
                    !t.present() ? Draw.OFF : t.percent() < 15 ? Draw.WARN : Draw.TEXT));
        }
        if (!totals.isEmpty()) {
            out.add(Line.GAP);
            out.addAll(totals);
        }
        return out;
    }
}
