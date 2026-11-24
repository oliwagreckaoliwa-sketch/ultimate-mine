package autominer.gui;

import autominer.config.ConfigManager;
import autominer.config.ModConfig;
import java.util.Locale;
import net.minecraft.client.MinecraftClient;

/** Zakladka "HUD Settings": widocznosc, skala, przezroczystosc, pozycja i wybor statystyk. */
public final class HudSettingsScreen extends GuiPanel {
    private static final int ROW = 20;
    private static final String[] LABELS = {
            "Status", "Iron Ore", "Raw Iron", "Gold Ore", "Raw Gold", "Diamond Ore", "Diamonds",
            "Cobblestone", "Stone", "Deepslate", "Blocks mined", "Session time", "Deposited",
            "Items collected", "Mining time", "Pickaxe condition", "Custom block counters"};

    private int contentH = 400;

    @Override
    public String title() {
        return "HUD Settings";
    }

    @Override
    public void render(Ui ui, int x, int y, int w, int h, MinecraftClient c) {
        ModConfig cfg = ConfigManager.get();
        int off = ui.scrollBegin("hud", x, y, w, h, contentH);
        int cw = w - 8;
        int top = y - off;
        int cy = top + 2;

        ui.card(x, cy, cw, 4 * ROW + 14);
        int ix = x + 10;
        int iw = cw - 20;
        int ry = cy + 8;
        tog(ui, "hudvis", ix, ry, iw, "HUD visible (F8)", cfg.hudVisible, b -> cfg.hudVisible = b);
        ry += ROW;
        ui.text("Scale: " + String.format(Locale.ROOT, "%.2f", cfg.hudScale), ix, ry + 2, Draw.TEXT);
        ui.slider("hudscale", ix + 90, ry, iw - 90, cfg.hudScale, 0.5, 2.0, v -> {
            cfg.hudScale = Math.round(v * 20) / 20.0;
            dirty();
        });
        ry += ROW;
        ui.text("Opacity: " + Math.round(cfg.hudOpacity * 100) + "%", ix, ry + 2, Draw.TEXT);
        ui.slider("hudop", ix + 90, ry, iw - 90, cfg.hudOpacity, 0.1, 1.0, v -> {
            cfg.hudOpacity = Math.round(v * 20) / 20.0;
            dirty();
        });
        ry += ROW;
        int half = (iw - 8) / 2;
        ui.button("hudedit", ix, ry, half, 16, "EDIT POSITION", Ui.Style.PRIMARY, true, () -> c.setScreen(new HudEditScreen()));
        ui.button("hudreset", ix + half + 8, ry, half, 16, "RESET POSITION", Ui.Style.GHOST, true, () -> {
            cfg.hudX = 6;
            cfg.hudY = 6;
            cfg.hudScale = 1.0;
            dirty();
        });
        cy += 4 * ROW + 24;

        section(ui, "Displayed statistics", x + 2, cy);
        cy += 14;
        int n = ModConfig.HUD_KEYS.length;
        ui.card(x, cy, cw, n * ROW + 14);
        ry = cy + 8;
        for (int i = 0; i < n; i++) {
            String key = ModConfig.HUD_KEYS[i];
            tog(ui, "hs-" + key, ix, ry, iw, LABELS[i], cfg.hudStat(key), b -> cfg.hudStats.put(key, b));
            ry += ROW;
        }
        cy += n * ROW + 24;

        contentH = cy - top + 6;
        ui.scrollEnd();
    }
}
