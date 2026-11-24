package autominer.gui;

import autominer.AutoMiner;
import autominer.config.ConfigManager;
import autominer.keybinds.KeybindManager;
import autominer.mining.AutoMiningManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

/** Glowne okno moda (F9): sidebar z zakladkami i plynne animacje. */
public final class MainScreen extends Screen {
    private static int lastTab = 0;

    private static final String[] NAMES = {
            "Dashboard", "Auto Mining", "Block Selector", "Auto Deposit", "Statistics", "HUD Settings", "General Settings"};
    private static final int SIDEBAR_W = 108;

    private final GuiPanel[] panels = {
            new DashboardScreen(), new MiningScreen(), new BlockSelectorScreen(), new DepositScreen(),
            new StatisticsScreen(), new HudSettingsScreen(), new SettingsScreen()};
    private final ItemStack[] icons = {
            new ItemStack(Items.COMPASS), new ItemStack(Items.DIAMOND_PICKAXE), new ItemStack(Items.GRASS_BLOCK),
            new ItemStack(Items.CHEST), new ItemStack(Items.WRITABLE_BOOK), new ItemStack(Items.ITEM_FRAME),
            new ItemStack(Items.COMPARATOR)};
    private final Ui ui = new Ui();
    private int tab;

    public MainScreen() {
        this(lastTab);
    }

    public MainScreen(int tab) {
        super(Text.literal("AutoMiner Ultimate"));
        this.tab = Math.max(0, Math.min(panels.length - 1, tab));
        lastTab = this.tab;
        ui.setAnim("open", 0f);
        ui.setAnim("tab", 1f);
    }

    private void switchTab(int i) {
        if (i == tab) return;
        tab = i;
        lastTab = i;
        ui.setAnim("tab", 0f);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ui.begin(ctx, textRenderer, mouseX, mouseY);
        float open = ui.anim("open", 1f, 9f);
        ctx.fill(0, 0, width, height, Draw.alpha(0xAA05060A, open));

        int pw = Math.min(width - 8, Math.max(380, Math.min(600, width - 24)));
        int ph = Math.min(height - 8, Math.max(220, Math.min(340, height - 24)));
        int px = (width - pw) / 2;
        int py = (height - ph) / 2 + Math.round((1f - open) * 12f);

        ui.alpha = open;
        Draw.card(ctx, px, py, pw, ph, 8, Draw.PANEL, open);

        // naglowek
        ui.text("AUTOMINER ULTIMATE", px + 14, py + 10, ui.accent);
        ui.text(NAMES[tab], px + 14 + textRenderer.getWidth("AUTOMINER ULTIMATE") + 10, py + 10, Draw.MUTED);
        AutoMiningManager m = AutoMiner.mining;
        String st = m.statusText();
        int col = m.state() == AutoMiningManager.State.RUNNING ? Draw.ON
                : m.state() == AutoMiningManager.State.PAUSED ? Draw.WARN : Draw.OFF;
        int pillW = textRenderer.getWidth(st) + 24;
        int pillX = px + pw - pillW - 32;
        ui.rect(pillX, py + 7, pillW, 14, 7, Draw.alpha(col, 0.22f));
        ui.rect(pillX + 6, py + 12, 4, 4, 2, col);
        ui.text(st, pillX + 14, py + 10, col);
        ui.button("close", px + pw - 24, py + 6, 16, 16, "x", Ui.Style.GHOST, true, this::close);
        ui.rect(px + 8, py + 27, pw - 16, 1, 0, Draw.BORDER);

        // sidebar
        int sx = px + 8;
        for (int i = 0; i < panels.length; i++) {
            int ry = py + 34 + i * 26;
            boolean act = i == tab;
            float hov = ui.anim("nav" + i, ui.hover(sx, ry, SIDEBAR_W, 22) ? 1f : 0f, 14f);
            ui.rect(sx, ry, SIDEBAR_W, 22, 5, act ? Draw.alpha(ui.accent, 0.22f) : Draw.alpha(Draw.CARD_HOVER, hov));
            if (act) ui.rect(sx, ry + 4, 3, 14, 1, ui.accent);
            ui.item(icons[i], sx + 8, ry + 3);
            ui.text(NAMES[i], sx + 30, ry + 7, act ? Draw.TEXT : Draw.MUTED);
            int idx = i;
            ui.hit(sx, ry, SIDEBAR_W, 22, () -> switchTab(idx));
        }

        // tresc
        float te = Draw.easeOut(ui.anim("tab", 1f, 7f));
        ui.alpha = open * te;
        int cx = px + 8 + SIDEBAR_W + 10;
        int cy = py + 34;
        int cw = pw - (8 + SIDEBAR_W + 10) - 10;
        int ch = ph - 34 - 10;
        int slide = Math.round((1f - te) * 16f);
        panels[tab].render(ui, cx + slide, cy, cw - slide, ch, client);
        ui.alpha = 1f;

        AutoMiner.notifications.render(ctx, textRenderer, width);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && ui.click(mouseX, mouseY)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (ui.drag(mouseX)) return true;
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        ui.release();
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (ui.scroll(mouseX, mouseY, verticalAmount)) return true;
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ui.key(keyCode)) return true;
        if (KeybindManager.openGui.matchesKey(keyCode, scanCode)) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (ui.charTyped(chr)) return true;
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void removed() {
        ConfigManager.save();
    }
}
