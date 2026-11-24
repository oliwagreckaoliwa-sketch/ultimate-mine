package autominer.gui;

import autominer.config.ConfigManager;
import autominer.config.ModConfig;
import autominer.hud.AutoMinerHud;
import autominer.hud.HudRenderer;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/** Tryb edycji HUD: przeciaganie myszka, kolko = skala, ESC = zapis i powrot do ustawien. */
public final class HudEditScreen extends Screen {
    private boolean dragging;
    private int offX;
    private int offY;
    private int sizeW = 100;
    private int sizeH = 60;

    public HudEditScreen() {
        super(Text.literal("HUD position"));
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ModConfig cfg = ConfigManager.get();
        ctx.fill(0, 0, width, height, 0x66000000);
        List<AutoMinerHud.Line> lines = AutoMinerHud.lines(client);
        int[] size = HudRenderer.measure(textRenderer, lines, (float) cfg.hudScale);
        sizeW = size[0];
        sizeH = size[1];
        HudRenderer.draw(ctx, textRenderer, lines, cfg.hudX, cfg.hudY, (float) cfg.hudScale,
                Math.max(0.5f, (float) cfg.hudOpacity));
        ctx.drawBorder(cfg.hudX - 1, cfg.hudY - 1, sizeW + 2, sizeH + 2, Draw.alpha(Draw.accent(cfg.accentIndex), 0.9f));
        ctx.drawCenteredTextWithShadow(textRenderer, "Drag the HUD to move it | scroll to resize | ESC to finish",
                width / 2, height - 22, Draw.TEXT);
        ctx.drawCenteredTextWithShadow(textRenderer, String.format("Scale %.2f", cfg.hudScale),
                width / 2, height - 11, Draw.MUTED);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        ModConfig cfg = ConfigManager.get();
        if (button == 0 && mouseX >= cfg.hudX && mouseX < cfg.hudX + sizeW
                && mouseY >= cfg.hudY && mouseY < cfg.hudY + sizeH) {
            dragging = true;
            offX = (int) mouseX - cfg.hudX;
            offY = (int) mouseY - cfg.hudY;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging) {
            ModConfig cfg = ConfigManager.get();
            cfg.hudX = Math.max(0, Math.min(width - sizeW, (int) mouseX - offX));
            cfg.hudY = Math.max(0, Math.min(height - sizeH, (int) mouseY - offY));
            ConfigManager.markDirty();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        ModConfig cfg = ConfigManager.get();
        double s = cfg.hudScale + Math.signum(verticalAmount) * 0.05;
        cfg.hudScale = Math.max(0.5, Math.min(2.0, Math.round(s * 20) / 20.0));
        ConfigManager.markDirty();
        return true;
    }

    @Override
    public void close() {
        client.setScreen(new MainScreen(5));
    }

    @Override
    public void removed() {
        ConfigManager.save();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
