package autominer.hud;

import autominer.AutoMiner;
import autominer.config.ConfigManager;
import autominer.config.ModConfig;
import autominer.gui.Draw;
import autominer.gui.MainScreen;
import autominer.selector.BlockInfo;
import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.client.gui.screen.Screen;
import autominer.selector.BlockSelector;

/** Rysuje HUD, podpis bloku pod celownikiem i powiadomienia podczas normalnej gry. */
public final class HudRenderer {
    private static final int PAD = 5;
    private static final int LINE_H = 10;

    private HudRenderer() {}

    public static void register() {
        HudRenderCallback.EVENT.register((ctx, tickCounter) -> {
            MinecraftClient c = MinecraftClient.getInstance();
            if (c.player == null || c.world == null || c.options.hudHidden) return;
            Screen s = c.currentScreen;
            if (s instanceof MainScreen) return; // GUI rysuje wlasne powiadomienia
            ModConfig cfg = ConfigManager.get();
            TextRenderer tr = c.textRenderer;
            if (cfg.hudVisible) {
                draw(ctx, tr, AutoMinerHud.lines(c), cfg.hudX, cfg.hudY, (float) cfg.hudScale, (float) cfg.hudOpacity);
            }
            if (s == null && cfg.showBlockLabel) drawBlockLabel(ctx, tr, c);
            AutoMiner.notifications.render(ctx, tr, ctx.getScaledWindowWidth());
        });
    }

    /** Rozmiar HUD w pikselach ekranu (z uwzglednieniem skali). */
    public static int[] measure(TextRenderer tr, List<AutoMinerHud.Line> lines, float scale) {
        int w = 0;
        int h = PAD * 2;
        for (AutoMinerHud.Line l : lines) {
            if (l.gap()) {
                h += 5;
            } else {
                w = Math.max(w, tr.getWidth(l.text()));
                h += LINE_H;
            }
        }
        return new int[] {(int) ((w + PAD * 2) * scale), (int) (h * scale)};
    }

    public static void draw(DrawContext ctx, TextRenderer tr, List<AutoMinerHud.Line> lines,
                            int x, int y, float scale, float opacity) {
        int[] size = measure(tr, lines, 1f);
        ctx.getMatrices().push();
        ctx.getMatrices().translate(x, y, 0);
        ctx.getMatrices().scale(scale, scale, 1f);
        Draw.card(ctx, 0, 0, size[0], size[1], 5, 0xFF111317, opacity);
        int yy = PAD;
        for (AutoMinerHud.Line l : lines) {
            if (l.gap()) {
                yy += 5;
                continue;
            }
            ctx.drawText(tr, l.text(), PAD, yy, Draw.alpha(l.color(), Math.min(1f, opacity + 0.25f)), false);
            yy += LINE_H;
        }
        ctx.getMatrices().pop();
    }

    private static void drawBlockLabel(DrawContext ctx, TextRenderer tr, MinecraftClient c) {
        BlockHitResult hit = BlockSelector.targetedBlock(c);
        if (hit == null) return;
        String id = BlockInfo.idOf(c.world.getBlockState(hit.getBlockPos()).getBlock());
        boolean sel = AutoMiner.selection.isSelected(id);
        int cx = ctx.getScaledWindowWidth() / 2;
        int y = ctx.getScaledWindowHeight() / 2 + 14;
        String name = BlockInfo.name(id);
        String info = sel ? "AUTO MINER: SELECTED" : "AUTO MINER: not selected  [F5]";
        int w = Math.max(tr.getWidth(name), tr.getWidth(info)) + 12;
        Draw.card(ctx, cx - w / 2, y - 3, w, 24, 4, 0xFF111317, 0.8f);
        ctx.drawCenteredTextWithShadow(tr, name, cx, y, Draw.TEXT);
        ctx.drawCenteredTextWithShadow(tr, info, cx, y + 10, sel ? Draw.ON : Draw.MUTED);
    }
}
