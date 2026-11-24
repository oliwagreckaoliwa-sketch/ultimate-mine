package autominer.gui;

import autominer.AutoMiner;
import autominer.selector.BlockGroup;
import autominer.selector.BlockInfo;
import autominer.selector.BlockSelectionManager;
import autominer.util.Fmt;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;

/** Zakladka "Block Selector": grupy, lista zaznaczonych blokow, zapis i wczytywanie list. */
public final class BlockSelectorScreen extends GuiPanel {
    private static final int ROW = 26;

    private int viewIdx;
    private int contentH = 400;
    private long confirmDeleteUntil;

    @Override
    public String title() {
        return "Block Selector";
    }

    @Override
    public void render(Ui ui, int x, int y, int w, int h, MinecraftClient c) {
        BlockSelectionManager sel = AutoMiner.selection;
        List<BlockGroup> groups = sel.groups();
        if (viewIdx >= groups.size()) viewIdx = 0;
        BlockGroup view = groups.get(viewIdx);
        BlockGroup active = sel.active();
        long now = System.currentTimeMillis();

        int off = ui.scrollBegin("selector", x, y, w, h, contentH);
        int cw = w - 8;
        int top = y - off;
        int cy = top + 2;

        ui.button("addblock", x, cy, cw, 24, "ADD BLOCK  (look at a block, press F5)", Ui.Style.PRIMARY, true, () -> {
            c.setScreen(null);
            AutoMiner.notifications.info("LOOK AT A BLOCK AND PRESS F5 (Shift+F5 removes)");
        });
        cy += 32;

        // --- grupy ---
        section(ui, "Groups  (* = receives F5)", x + 2, cy);
        cy += 13;
        int cx = x;
        for (int i = 0; i < groups.size(); i++) {
            BlockGroup g = groups.get(i);
            String label = (g == active ? "* " : "") + g.name + " (" + g.blocks.size() + ")" + (g.enabled ? "" : " [off]");
            int wd = ui.tr.getWidth(label) + 16;
            if (cx + wd > x + cw && cx > x) {
                cx = x;
                cy += 20;
            }
            int idx = i;
            ui.button("grp" + i, cx, cy, wd, 16, label, i == viewIdx ? Ui.Style.PRIMARY : Ui.Style.GHOST, true, () -> viewIdx = idx);
            cx += wd + 5;
        }
        cy += 24;

        // --- sterowanie grupa ---
        ui.card(x, cy, cw, 112);
        int ix = x + 10;
        int iw = cw - 20;
        int ry = cy + 8;
        ui.text("Group: " + ui.fit(view.name, iw - 10), ix, ry, Draw.TEXT);
        ry += 14;
        ui.button("setactive", ix, ry, 118, 16, view == active ? "ACTIVE FOR F5" : "SET ACTIVE (F5)", Ui.Style.GHOST,
                view != active, () -> {
                    sel.setActive(view);
                    AutoMiner.notifications.info("ACTIVE GROUP: " + view.name);
                });
        ui.text("In use:", ix + 126, ry + 4, Draw.MUTED);
        ui.toggle("grpon", ix + 164, ry + 1, view.enabled, b -> sel.setEnabled(view, b));
        boolean confirm = confirmDeleteUntil > now;
        ui.button("delgrp", ix + iw - 118, ry, 118, 16, confirm ? "CONFIRM DELETE?" : "DELETE GROUP",
                Ui.Style.DANGER, groups.size() > 1, () -> {
                    if (confirmDeleteUntil > System.currentTimeMillis()) {
                        sel.deleteGroup(view);
                        viewIdx = 0;
                        confirmDeleteUntil = 0;
                        AutoMiner.notifications.info("GROUP DELETED");
                    } else {
                        confirmDeleteUntil = System.currentTimeMillis() + 3000;
                    }
                });
        ry += 22;
        ui.textField("newGroup", ix, ry, iw - 84, "New group name");
        Runnable create = () -> {
            if (sel.createGroup(ui.fieldText("newGroup"))) {
                ui.clearField("newGroup");
                viewIdx = sel.groups().size() - 1;
                AutoMiner.notifications.success("GROUP CREATED");
            } else {
                AutoMiner.notifications.warn("GROUP NAME EMPTY OR ALREADY USED");
            }
        };
        ui.button("mkgrp", ix + iw - 78, ry, 78, 16, "CREATE", Ui.Style.PRIMARY, true, create);
        if (ui.enterPressed("newGroup")) create.run();
        ry += 22;
        ui.textField("saveList", ix, ry, iw - 84, "List name to save");
        Runnable save = () -> {
            String n = BlockSelectionManager.cleanFileName(ui.fieldText("saveList"));
            if (!n.isEmpty() && sel.saveList(n)) {
                AutoMiner.notifications.success("LIST SAVED: " + n);
            } else {
                AutoMiner.notifications.error("LIST NOT SAVED (use letters, digits, space, _ -)");
            }
        };
        ui.button("savelist", ix + iw - 78, ry, 78, 16, "SAVE LIST", Ui.Style.GHOST, true, save);
        if (ui.enterPressed("saveList")) save.run();
        ry += 22;
        ui.text("Load list:", ix, ry + 4, Draw.MUTED);
        List<String> saved = sel.savedLists();
        int lx = ix + 54;
        if (saved.isEmpty()) {
            ui.text("none saved yet", lx, ry + 4, Draw.MUTED);
        }
        for (String name : saved) {
            int wd = ui.tr.getWidth(name) + 14;
            if (lx + wd > ix + iw) break;
            ui.button("load-" + name, lx, ry, wd, 16, name, Ui.Style.GHOST, true, () -> {
                if (sel.loadList(name)) {
                    viewIdx = 0;
                    AutoMiner.notifications.success("LIST LOADED: " + name);
                } else {
                    AutoMiner.notifications.error("LIST COULD NOT BE LOADED");
                }
            });
            lx += wd + 4;
        }
        cy += 120;

        // --- lista blokow ---
        section(ui, "Blocks in " + view.name + " (" + view.blocks.size() + ")", x + 2, cy);
        cy += 14;
        List<String> ids = new ArrayList<>(view.blocks);
        if (ids.isEmpty()) {
            ui.text("Empty. Look at a block and press F5, or use ADD BLOCK.", x + 2, cy + 4, Draw.MUTED);
            cy += 18;
        }
        for (String id : ids) {
            boolean hov = ui.hover(x, cy, cw, ROW - 2);
            ui.rect(x, cy, cw, ROW - 2, 4, hov ? Draw.CARD_HOVER : Draw.CARD);
            ui.item(BlockInfo.icon(id), x + 6, cy + 4);
            int rightW = 52 + (view != active ? 48 : 0) + 56;
            int textW = cw - 34 - rightW;
            ui.text(ui.fit(BlockInfo.name(id), textW), x + 30, cy + 4, Draw.TEXT);
            ui.text(ui.fit(id + (BlockInfo.registered(id) ? "" : "  (not in this game)"), textW), x + 30, cy + 14,
                    BlockInfo.registered(id) ? Draw.MUTED : Draw.WARN);
            long mined = AutoMiner.stats.current().counters.block(id);
            ui.textR(Fmt.num(ui.num("sel-" + id, mined)), x + cw - 112 - (view != active ? 48 : 0), cy + 9, Draw.TEXT);
            int bx = x + cw - 56;
            ui.button("rm-" + id, bx, cy + 4, 50, 16, "REMOVE", Ui.Style.DANGER, true, () -> {
                sel.removeFrom(view, id);
                AutoMiner.notifications.info("BLOCK REMOVED: " + BlockInfo.name(id));
            });
            if (view != active) {
                ui.button("cp-" + id, bx - 48, cy + 4, 44, 16, "COPY", Ui.Style.GHOST, true, () -> {
                    if (sel.addTo(active, id)) {
                        AutoMiner.notifications.success("COPIED TO " + active.name);
                    } else {
                        AutoMiner.notifications.warn("BLOCK ALREADY SELECTED");
                    }
                });
            }
            cy += ROW;
        }

        contentH = cy - top + 8;
        ui.scrollEnd();
    }
}
