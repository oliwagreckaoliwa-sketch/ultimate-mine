package autominer.gui;

import autominer.AutoMiner;
import autominer.selector.BlockInfo;
import autominer.statistics.ResourceCounter;
import autominer.statistics.SessionStatistics;
import autominer.util.Fmt;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

/** Zakladka "Statistics": karty zasobow, liczniki customowych blokow, historia sesji, reset. */
public final class StatisticsScreen extends GuiPanel {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private boolean allTime;
    private int contentH = 500;
    private long confirmClearUntil;

    @Override
    public String title() {
        return "Statistics";
    }

    @Override
    public void render(Ui ui, int x, int y, int w, int h, MinecraftClient c) {
        SessionStatistics s = allTime ? AutoMiner.stats.allTime() : AutoMiner.stats.current();
        List<ResourceCounter.Standard> std = ResourceCounter.STANDARD;
        long now = System.currentTimeMillis();

        int off = ui.scrollBegin("stats", x, y, w, h, contentH);
        int cw = w - 8;
        int top = y - off;
        int cy = top + 2;

        int half = (cw - 8) / 2;
        ui.button("v-session", x, cy, half, 18, "THIS SESSION", allTime ? Ui.Style.GHOST : Ui.Style.PRIMARY, true, () -> allTime = false);
        ui.button("v-all", x + half + 8, cy, cw - half - 8, 18, "ALL TIME", allTime ? Ui.Style.PRIMARY : Ui.Style.GHOST, true, () -> allTime = true);
        cy += 26;

        int cols = cw >= 440 ? 4 : 2;
        int gap = 8;
        int cardW = (cw - gap * (cols - 1)) / cols;
        int cardH = 50;

        // --- karty glowne ---
        Object[][] main = {
                {new ItemStack(Items.IRON_ORE), "IRON ORE", s.counters.value(std.get(0)), "Raw Iron: " + Fmt.num(s.counters.value(std.get(1)))},
                {new ItemStack(Items.GOLD_ORE), "GOLD ORE", s.counters.value(std.get(2)), "Raw Gold: " + Fmt.num(s.counters.value(std.get(3)))},
                {new ItemStack(Items.DIAMOND_ORE), "DIAMOND ORE", s.counters.value(std.get(4)), "Diamonds: " + Fmt.num(s.counters.value(std.get(5)))},
                {new ItemStack(Items.COBBLESTONE), "COBBLESTONE", s.counters.value(std.get(6)),
                        "Stone: " + Fmt.num(s.counters.value(std.get(7))) + "  Deepslate: " + Fmt.num(s.counters.value(std.get(8)))}};
        cy = grid(ui, main, x, cy, cols, cardW, cardH, gap, "m");

        // --- sumy ---
        Object[][] totals = {
                {new ItemStack(Items.DIAMOND_PICKAXE), "BLOCKS MINED", s.blocksMined, "Mining " + Fmt.time(s.miningMs)},
                {new ItemStack(Items.CLOCK), allTime ? "SESSIONS" : "SESSION TIME",
                        allTime ? (long) AutoMiner.stats.history().size() + 1 : (long) -1, allTime ? "incl. current" : Fmt.time(s.elapsedMs(now))},
                {new ItemStack(Items.HOPPER), "ITEMS COLLECTED", s.itemsCollected, "Counted from inventory gains"},
                {new ItemStack(Items.CHEST), "DEPOSITED", s.itemsDeposited, s.depositOps + " successful deposits"}};
        cy = grid(ui, totals, x, cy, cols, cardW, cardH, gap, "t");

        // --- dynamiczne liczniki blokow ---
        Set<String> covered = new HashSet<>();
        for (ResourceCounter.Standard st : std) covered.addAll(st.ids());
        List<Map.Entry<String, Long>> dyn = new ArrayList<>();
        for (Map.Entry<String, Long> e : s.counters.blocks.entrySet()) {
            if (!covered.contains(e.getKey())) dyn.add(e);
        }
        dyn.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        if (!dyn.isEmpty()) {
            section(ui, "Custom blocks", x + 2, cy);
            cy += 13;
            int n = Math.min(dyn.size(), 40);
            Object[][] cards = new Object[n][];
            for (int i = 0; i < n; i++) {
                String id = dyn.get(i).getKey();
                cards[i] = new Object[] {BlockInfo.icon(id), BlockInfo.name(id).toUpperCase(), dyn.get(i).getValue(), id};
            }
            cy = grid(ui, cards, x, cy, cols, cardW, cardH, gap, "d");
        }

        // --- reset ---
        int bw = (cw - 8) / 2;
        boolean confirm = confirmClearUntil > now;
        ui.button("resetsession", x, cy, bw, 18, "RESET SESSION COUNTERS", Ui.Style.GHOST, true, () -> {
            AutoMiner.stats.resetSession();
            AutoMiner.notifications.success("SESSION ARCHIVED, COUNTERS RESET");
        });
        ui.button("clearall", x + bw + 8, cy, cw - bw - 8, 18, confirm ? "CONFIRM CLEAR ALL?" : "CLEAR ALL STATISTICS",
                Ui.Style.DANGER, true, () -> {
                    if (confirmClearUntil > System.currentTimeMillis()) {
                        AutoMiner.stats.clearAll();
                        confirmClearUntil = 0;
                        AutoMiner.notifications.warn("ALL STATISTICS CLEARED");
                    } else {
                        confirmClearUntil = System.currentTimeMillis() + 3000;
                    }
                });
        cy += 28;

        // --- historia ---
        List<SessionStatistics> hist = AutoMiner.stats.history();
        section(ui, "Session history (" + hist.size() + ")", x + 2, cy);
        cy += 13;
        if (hist.isEmpty()) {
            ui.text("No archived sessions yet. The current one is saved when you close the game.", x + 2, cy, Draw.MUTED);
            cy += 14;
        }
        for (int i = 0; i < Math.min(hist.size(), 50); i++) {
            SessionStatistics hs = hist.get(i);
            ui.rect(x, cy, cw, 16, 3, i % 2 == 0 ? Draw.CARD : 0xFF181B20);
            String date = hs.startMs > 0 ? DATE.format(ZonedDateTime.ofInstant(Instant.ofEpochMilli(hs.startMs), ZoneId.systemDefault())) : "?";
            ui.text(date, x + 6, cy + 4, Draw.TEXT);
            ui.text(Fmt.time(hs.miningMs), x + 100, cy + 4, Draw.MUTED);
            ui.text(ui.fit("blocks " + Fmt.num(hs.blocksMined) + " | items " + Fmt.num(hs.itemsCollected)
                    + " | deposited " + Fmt.num(hs.itemsDeposited), cw - 160), x + 160, cy + 4, Draw.TEXT);
            cy += 17;
        }

        contentH = cy - top + 8;
        ui.scrollEnd();
    }

    /** Rysuje siatke kart {ikona, tytul, wartosc, podpis}. Wartosc -1 oznacza "brak liczby". Zwraca nowe y. */
    private int grid(Ui ui, Object[][] cards, int x, int y, int cols, int cardW, int cardH, int gap, String idPrefix) {
        for (int i = 0; i < cards.length; i++) {
            int cx = x + (i % cols) * (cardW + gap);
            int cy = y + (i / cols) * (cardH + gap);
            ui.card(cx, cy, cardW, cardH);
            ui.item((ItemStack) cards[i][0], cx + cardW - 24, cy + 6);
            ui.text(ui.fit((String) cards[i][1], cardW - 40), cx + 8, cy + 6, Draw.MUTED);
            long v = (Long) cards[i][2];
            String big = v < 0 ? ui.fit((String) cards[i][3], cardW - 16) : Fmt.num(ui.num(idPrefix + i + cards[i][1], v));
            ui.textScaled(big, cx + 8, cy + 18, Draw.TEXT, 1.3f);
            if (v >= 0) ui.text(ui.fit((String) cards[i][3], cardW - 14), cx + 8, cy + 37, Draw.MUTED);
        }
        int rows = (cards.length + cols - 1) / cols;
        return y + rows * (cardH + gap) + 2;
    }
}
