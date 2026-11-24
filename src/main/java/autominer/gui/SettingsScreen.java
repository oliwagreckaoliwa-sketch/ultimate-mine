package autominer.gui;

import autominer.AutoMiner;
import autominer.config.ConfigManager;
import autominer.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.option.KeybindsScreen;

/** Zakladka "General Settings". */
public final class SettingsScreen extends GuiPanel {
    private static final int ROW = 20;
    private int contentH = 300;

    @Override
    public String title() {
        return "General Settings";
    }

    @Override
    public void render(Ui ui, int x, int y, int w, int h, MinecraftClient c) {
        ModConfig cfg = ConfigManager.get();
        int off = ui.scrollBegin("settings", x, y, w, h, contentH);
        int cw = w - 8;
        int top = y - off;
        int cy = top + 2;

        int rows = 8;
        ui.card(x, cy, cw, rows * ROW + 14);
        int ix = x + 10;
        int iw = cw - 20;
        int ry = cy + 8;
        tog(ui, "notif", ix, ry, iw, "Notifications", cfg.showNotifications, b -> cfg.showNotifications = b);
        ry += ROW;
        stepD(ui, "notifsec", ix, ry, iw, "Notification duration (s)", cfg.notificationSeconds, 0.5, 1.0, 8.0, "%.1f", v -> cfg.notificationSeconds = v);
        ry += ROW;
        tog(ui, "anim", ix, ry, iw, "UI animations", cfg.animations, b -> cfg.animations = b);
        ry += ROW;
        tog(ui, "outline", ix, ry, iw, "Block outline preview (green / gray)", cfg.showOutline, b -> cfg.showOutline = b);
        ry += ROW;
        tog(ui, "label", ix, ry, iw, "Block name under crosshair", cfg.showBlockLabel, b -> cfg.showBlockLabel = b);
        ry += ROW;
        tog(ui, "persp", ix, ry, iw, "Move vanilla F5 to F4 (restart)", cfg.remapPerspectiveKey, b -> {
            cfg.remapPerspectiveKey = b;
            cfg.perspectiveRemapped = false;
        });
        ry += ROW;
        ui.text("Accent color", ix, ry + 4, Draw.TEXT);
        int bw = 56;
        ui.button("acc0", ix + iw - bw * 2 - 6, ry, bw, 16, "Violet", cfg.accentIndex == 0 ? Ui.Style.PRIMARY : Ui.Style.GHOST, true, () -> {
            cfg.accentIndex = 0;
            dirty();
        });
        ui.button("acc1", ix + iw - bw, ry, bw, 16, "Blue", cfg.accentIndex == 1 ? Ui.Style.PRIMARY : Ui.Style.GHOST, true, () -> {
            cfg.accentIndex = 1;
            dirty();
        });
        ry += ROW;
        int half = (iw - 8) / 2;
        ui.button("keys", ix, ry, half, 16, "SHORTCUTS (CONTROLS)", Ui.Style.GHOST, true,
                () -> c.setScreen(new KeybindsScreen(new MainScreen(), c.options)));
        ui.button("savenow", ix + half + 8, ry, half, 16, "SAVE NOW", Ui.Style.PRIMARY, true, () -> {
            ConfigManager.save();
            AutoMiner.stats.save();
            AutoMiner.notifications.success("SETTINGS AND STATISTICS SAVED");
        });
        cy += rows * ROW + 24;

        ui.text("All shortcuts can be changed in Options > Controls > AutoMiner Ultimate.", x + 2, cy, Draw.MUTED);
        cy += 12;
        ui.text("Config: config/autominer-ultimate/config.json", x + 2, cy, Draw.MUTED);
        cy += 12;
        ui.text("Statistics: config/autominer-ultimate/stats.json", x + 2, cy, Draw.MUTED);
        cy += 14;

        contentH = cy - top + 6;
        ui.scrollEnd();
    }
}
