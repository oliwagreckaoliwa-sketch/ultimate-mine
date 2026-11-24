package autominer.gui;

import net.minecraft.client.gui.DrawContext;

/** Proste, tanie w renderowaniu prymitywy do nowoczesnego, ciemnego interfejsu. */
public final class Draw {
    public static final int BG = 0xFF101114;
    public static final int PANEL = 0xFF15171B;
    public static final int CARD = 0xFF1C1F25;
    public static final int CARD_HOVER = 0xFF252932;
    public static final int BORDER = 0xFF2B2F38;
    public static final int TEXT = 0xFFF3F4F6;
    public static final int MUTED = 0xFF9CA3AF;
    public static final int ON = 0xFF22C55E;
    public static final int OFF = 0xFFEF4444;
    public static final int WARN = 0xFFF59E0B;
    public static final int VIOLET = 0xFF8B5CF6;
    public static final int BLUE = 0xFF3B82F6;

    private Draw() {}

    public static int alpha(int argb, float a) {
        int na = Math.max(0, Math.min(255, (int) (((argb >>> 24) & 0xFF) * a)));
        return (na << 24) | (argb & 0xFFFFFF);
    }

    public static int lerp(int c1, int c2, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int a = (int) (((c1 >>> 24) & 255) + (((c2 >>> 24) & 255) - ((c1 >>> 24) & 255)) * t);
        int r = (int) (((c1 >> 16) & 255) + (((c2 >> 16) & 255) - ((c1 >> 16) & 255)) * t);
        int g = (int) (((c1 >> 8) & 255) + (((c2 >> 8) & 255) - ((c1 >> 8) & 255)) * t);
        int b = (int) ((c1 & 255) + ((c2 & 255) - (c1 & 255)) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /** Wypelniony prostokat z zaokraglonymi rogami (rozłączne paski, bez nakladania). */
    public static void round(DrawContext c, int x, int y, int w, int h, int r, int color) {
        if (w <= 0 || h <= 0 || ((color >>> 24) & 255) == 0) return;
        r = Math.min(r, Math.min(w, h) / 2);
        if (r <= 0) {
            c.fill(x, y, x + w, y + h, color);
            return;
        }
        c.fill(x, y + r, x + w, y + h - r, color);
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = (int) Math.round(r - Math.sqrt(r * r - dy * dy));
            c.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
            c.fill(x + inset, y + h - i - 1, x + w - inset, y + h - i, color);
        }
    }

    public static void card(DrawContext c, int x, int y, int w, int h, int r, int fill, float a) {
        round(c, x + 1, y + 2, w, h, r, alpha(0x55000000, a));
        round(c, x, y, w, h, r, alpha(BORDER, a));
        round(c, x + 1, y + 1, w - 2, h - 2, Math.max(0, r - 1), alpha(fill, a));
    }

    public static int accent(int index) {
        return index == 1 ? BLUE : VIOLET;
    }

    public static float easeOut(float t) {
        t = Math.max(0f, Math.min(1f, t));
        float u = 1f - t;
        return 1f - u * u * u;
    }
}
