package autominer.notifications;

import autominer.config.ConfigManager;
import autominer.gui.Draw;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/** Wlasne powiadomienia (toasty) rysowane na HUD i w GUI moda. */
public final class NotificationManager {
    public enum Type {
        INFO(Draw.VIOLET), SUCCESS(Draw.ON), WARN(Draw.WARN), ERROR(Draw.OFF);

        final int color;

        Type(int color) {
            this.color = color;
        }
    }

    private static final int MAX = 5;
    private static final long FADE_IN = 220;
    private static final long FADE_OUT = 320;

    private static final class Note {
        String text;
        Type type;
        long created;
    }

    private final List<Note> notes = new ArrayList<>();

    public synchronized void push(String text, Type type) {
        if (!ConfigManager.get().showNotifications) return;
        long now = System.currentTimeMillis();
        for (Note n : notes) {
            if (n.text.equals(text) && now - n.created < 600) {
                n.created = now;
                return;
            }
        }
        Note n = new Note();
        n.text = text;
        n.type = type;
        n.created = now;
        notes.add(n);
        while (notes.size() > MAX) notes.remove(0);
    }

    public void info(String t) {
        push(t, Type.INFO);
    }

    public void success(String t) {
        push(t, Type.SUCCESS);
    }

    public void warn(String t) {
        push(t, Type.WARN);
    }

    public void error(String t) {
        push(t, Type.ERROR);
    }

    public synchronized void render(DrawContext ctx, TextRenderer tr, int screenW) {
        if (notes.isEmpty()) return;
        long now = System.currentTimeMillis();
        long life = (long) (ConfigManager.get().notificationSeconds * 1000);
        notes.removeIf(n -> now - n.created > life + FADE_OUT);
        int y = 10;
        for (Note n : notes) {
            long age = now - n.created;
            float in = Draw.easeOut(age / (float) FADE_IN);
            float out = age > life ? 1f - (age - life) / (float) FADE_OUT : 1f;
            float a = Math.max(0f, Math.min(1f, in * out));
            if (a < 0.03f) continue;
            int w = tr.getWidth(n.text) + 26;
            int h = 20;
            int x = (screenW - w) / 2;
            int yy = y - (int) ((1f - in) * 14);
            Draw.card(ctx, x, yy, w, h, 5, Draw.PANEL, a);
            Draw.round(ctx, x + 1, yy + 4, 3, h - 8, 1, Draw.alpha(n.type.color, a));
            ctx.drawText(tr, n.text, x + 12, yy + 6, Draw.alpha(Draw.TEXT, a), false);
            y += h + 4;
        }
    }
}
