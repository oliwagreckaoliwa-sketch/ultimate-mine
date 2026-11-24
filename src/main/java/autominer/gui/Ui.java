package autominer.gui;

import autominer.config.ConfigManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;

/**
 * Lekki interfejs w trybie "immediate mode": widgety rysuja sie i rejestruja obszary klikniecia
 * w kazdej klatce, a klikniecia obslugiwane sa na podstawie obszarow z poprzedniej klatki.
 * Animacje to proste interpolacje liczone z czasu klatki (brak obciazenia FPS).
 */
public final class Ui {
    public enum Style { PRIMARY, SUCCESS, DANGER, GHOST }

    private record Hit(int x, int y, int w, int h, int[] clip, Runnable action, IntConsumer drag) {}

    private record Region(String id, int x, int y, int w, int h) {}

    public DrawContext ctx;
    public TextRenderer tr;
    public int mx;
    public int my;
    public float alpha = 1f;
    public int accent = Draw.VIOLET;
    public float dt = 0.016f;

    private long lastNs = System.nanoTime();
    private List<Hit> hits = new ArrayList<>();
    private List<Hit> prevHits = new ArrayList<>();
    private List<Region> regions = new ArrayList<>();
    private List<Region> prevRegions = new ArrayList<>();
    private final Map<String, Float> anims = new HashMap<>();
    private final Map<String, float[]> scrolls = new HashMap<>();
    private final Map<String, StringBuilder> fields = new HashMap<>();
    private String focus;
    private String enterId;
    private IntConsumer activeDrag;
    private int[] clip;
    private int[] savedClip;
    private float[] activeScroll;
    private int activeScrollContent;
    private int activeScrollH;
    private int activeScrollX;
    private int activeScrollY;
    private int activeScrollW;

    public void begin(DrawContext ctx, TextRenderer tr, int mx, int my) {
        this.ctx = ctx;
        this.tr = tr;
        this.mx = mx;
        this.my = my;
        long now = System.nanoTime();
        dt = Math.min(0.1f, (now - lastNs) / 1_000_000_000f);
        lastNs = now;
        prevHits = hits;
        hits = new ArrayList<>();
        prevRegions = regions;
        regions = new ArrayList<>();
        accent = Draw.accent(ConfigManager.get().accentIndex);
        clip = null;
        alpha = 1f;
    }

    // ---------- animacje ----------

    public float anim(String id, float target, float speed) {
        Float cur = anims.get(id);
        float v = cur == null ? target : cur;
        if (!ConfigManager.get().animations) {
            v = target;
        } else {
            v += (target - v) * Math.min(1f, speed * dt);
            if (Math.abs(target - v) < 0.003f) v = target;
        }
        anims.put(id, v);
        return v;
    }

    public void setAnim(String id, float v) {
        anims.put(id, v);
    }

    /** Plynnie aktualizowany licznik. */
    public long num(String id, long target) {
        if (target > 10_000_000L) return target;
        float v = anim("n:" + id, (float) target, 10f);
        return Math.abs(v - target) < 0.5f ? target : Math.round(v);
    }

    // ---------- prymitywy ----------

    public int col(int c) {
        return Draw.alpha(c, alpha);
    }

    public void rect(int x, int y, int w, int h, int r, int color) {
        Draw.round(ctx, x, y, w, h, r, col(color));
    }

    public void card(int x, int y, int w, int h) {
        Draw.card(ctx, x, y, w, h, 5, Draw.CARD, alpha);
    }

    public void text(String s, int x, int y, int color) {
        int c = col(color);
        if (((c >>> 24) & 0xFF) < 8) return;
        ctx.drawText(tr, s, x, y, c, false);
    }

    public void textC(String s, int cx, int y, int color) {
        text(s, cx - tr.getWidth(s) / 2, y, color);
    }

    public void textR(String s, int rx, int y, int color) {
        text(s, rx - tr.getWidth(s), y, color);
    }

    public void textScaled(String s, int x, int y, int color, float sc) {
        int c = col(color);
        if (((c >>> 24) & 0xFF) < 8) return;
        ctx.getMatrices().push();
        ctx.getMatrices().translate((float) x, (float) y, 0f);
        ctx.getMatrices().scale(sc, sc, 1f);
        ctx.drawText(tr, s, 0, 0, c, false);
        ctx.getMatrices().pop();
    }

    public String fit(String s, int maxW) {
        if (tr.getWidth(s) <= maxW) return s;
        return tr.trimToWidth(s, Math.max(0, maxW - tr.getWidth(".."))) + "..";
    }

    public void item(ItemStack st, int x, int y) {
        ctx.drawItem(st, x, y);
    }

    private static boolean inside(double px, double py, int x, int y, int w, int h) {
        return px >= x && px < x + w && py >= y && py < y + h;
    }

    private boolean clipped(double px, double py, int[] c) {
        return c == null || inside(px, py, c[0], c[1], c[2] - c[0], c[3] - c[1]);
    }

    public boolean hover(int x, int y, int w, int h) {
        return inside(mx, my, x, y, w, h) && clipped(mx, my, clip);
    }

    public void hit(int x, int y, int w, int h, Runnable action) {
        hits.add(new Hit(x, y, w, h, clip, action, null));
    }

    // ---------- widgety ----------

    public void button(String id, int x, int y, int w, int h, String label, Style st, boolean enabled, Runnable action) {
        boolean hov = enabled && hover(x, y, w, h);
        float k = anim("b:" + id, hov ? 1f : 0f, 14f);
        int base = switch (st) {
            case PRIMARY -> accent;
            case SUCCESS -> 0xFF1F9D4E;
            case DANGER -> 0xFFC93C3C;
            case GHOST -> 0xFF262A32;
        };
        int fill = !enabled ? 0xFF22252B : Draw.lerp(base, st == Style.GHOST ? 0xFF3A404C : 0xFFFFFFFF, st == Style.GHOST ? k : 0.2f * k);
        rect(x, y, w, h, 4, fill);
        int tc = !enabled ? Draw.MUTED : Draw.TEXT;
        textC(fit(label, w - 6), x + w / 2, y + (h - 8) / 2, tc);
        if (enabled && action != null) hit(x, y, w, h, action);
    }

    public void toggle(String id, int x, int y, boolean on, Consumer<Boolean> set) {
        float k = anim("t:" + id, on ? 1f : 0f, 16f);
        int w = 28;
        int h = 14;
        rect(x, y, w, h, 7, Draw.lerp(0xFF7A2B2E, 0xFF1F9D4E, k));
        rect(x + 2 + Math.round(k * (w - h)), y + 2, h - 4, h - 4, 5, 0xFFF3F4F6);
        hit(x, y, w, h, () -> set.accept(!on));
    }

    public void stepper(String id, int x, int y, int w, String label, String value, Runnable dec, Runnable inc) {
        text(label, x, y + 3, Draw.TEXT);
        int bx = x + w - 92;
        button(id + "-", bx, y, 16, 14, "-", Style.GHOST, true, dec);
        textC(value, bx + 16 + 4 + 28, y + 3, Draw.TEXT);
        button(id + "+", bx + 76, y, 16, 14, "+", Style.GHOST, true, inc);
    }

    public void slider(String id, int x, int y, int w, double v, double min, double max, Consumer<Double> set) {
        double t = Math.max(0, Math.min(1, (v - min) / (max - min)));
        rect(x, y + 4, w, 4, 2, 0xFF2A2D35);
        rect(x, y + 4, (int) (w * t), 4, 2, accent);
        int kx = x + (int) (w * t);
        rect(kx - 3, y + 1, 6, 10, 3, 0xFFF3F4F6);
        IntConsumer drag = m -> {
            double tt = Math.max(0, Math.min(1, (m - x) / (double) w));
            set.accept(min + (max - min) * tt);
        };
        hits.add(new Hit(x - 4, y, w + 8, 12, clip, null, drag));
    }

    public void textField(String id, int x, int y, int w, String hint) {
        StringBuilder sb = fields.computeIfAbsent(id, k -> new StringBuilder());
        boolean f = id.equals(focus);
        rect(x, y, w, 16, 4, f ? accent : Draw.BORDER);
        rect(x + 1, y + 1, w - 2, 14, 3, 0xFF14161A);
        boolean empty = sb.length() == 0;
        String shown = empty && !f ? hint : fit(sb.toString(), w - 10);
        text(shown, x + 5, y + 4, empty && !f ? Draw.MUTED : Draw.TEXT);
        if (f && (System.currentTimeMillis() / 500) % 2 == 0) {
            rect(x + 5 + tr.getWidth(shown), y + 3, 1, 10, 0, Draw.TEXT);
        }
        hit(x, y, w, 16, () -> focus = id);
    }

    public String fieldText(String id) {
        StringBuilder sb = fields.get(id);
        return sb == null ? "" : sb.toString();
    }

    public void clearField(String id) {
        StringBuilder sb = fields.get(id);
        if (sb != null) sb.setLength(0);
    }

    public boolean enterPressed(String id) {
        if (id.equals(enterId)) {
            enterId = null;
            return true;
        }
        return false;
    }

    // ---------- przewijanie ----------

    /** Zwraca przesuniecie (w pikselach) tresci; rysowanie musi byc w obrebie scrollBegin/scrollEnd. */
    public int scrollBegin(String id, int x, int y, int w, int h, int contentH) {
        float[] st = scrolls.computeIfAbsent(id, k -> new float[2]);
        float max = Math.max(0, contentH - h);
        st[0] = Math.max(0, Math.min(max, st[0]));
        if (ConfigManager.get().animations) {
            st[1] += (st[0] - st[1]) * Math.min(1f, 18f * dt);
            if (Math.abs(st[0] - st[1]) < 0.5f) st[1] = st[0];
        } else {
            st[1] = st[0];
        }
        regions.add(new Region(id, x, y, w, h));
        ctx.enableScissor(x, y, x + w, y + h);
        savedClip = clip;
        clip = new int[] {x, y, x + w, y + h};
        activeScroll = st;
        activeScrollContent = contentH;
        activeScrollH = h;
        activeScrollX = x;
        activeScrollY = y;
        activeScrollW = w;
        return Math.round(st[1]);
    }

    public void scrollEnd() {
        if (activeScroll != null && activeScrollContent > activeScrollH) {
            int barH = Math.max(12, activeScrollH * activeScrollH / activeScrollContent);
            float max = activeScrollContent - activeScrollH;
            int barY = activeScrollY + Math.round((activeScrollH - barH) * (activeScroll[1] / max));
            rect(activeScrollX + activeScrollW - 3, barY, 2, barH, 1, Draw.alpha(accent, 0.7f));
        }
        ctx.disableScissor();
        clip = savedClip;
        activeScroll = null;
    }

    public boolean scroll(double px, double py, double amount) {
        for (int i = prevRegions.size() - 1; i >= 0; i--) {
            Region r = prevRegions.get(i);
            if (inside(px, py, r.x(), r.y(), r.w(), r.h())) {
                float[] st = scrolls.get(r.id());
                if (st != null) st[0] -= (float) amount * 18f;
                return true;
            }
        }
        return false;
    }

    // ---------- zdarzenia wejscia ----------

    public boolean click(double px, double py) {
        focus = null;
        for (int i = prevHits.size() - 1; i >= 0; i--) {
            Hit h = prevHits.get(i);
            if (inside(px, py, h.x(), h.y(), h.w(), h.h()) && clipped(px, py, h.clip())) {
                if (h.drag() != null) {
                    activeDrag = h.drag();
                    h.drag().accept((int) px);
                }
                if (h.action() != null) h.action().run();
                return true;
            }
        }
        return false;
    }

    public boolean drag(double px) {
        if (activeDrag == null) return false;
        activeDrag.accept((int) px);
        return true;
    }

    public void release() {
        activeDrag = null;
    }

    /** @return true, jesli klawisz zostal zuzyty przez aktywne pole tekstowe. */
    public boolean key(int keyCode) {
        if (focus == null) return false;
        StringBuilder sb = fields.get(focus);
        if (keyCode == 259 && sb != null && sb.length() > 0) { // GLFW_KEY_BACKSPACE
            sb.setLength(sb.length() - 1);
        } else if (keyCode == 257 || keyCode == 335) { // ENTER, KP_ENTER
            enterId = focus;
            focus = null;
        } else if (keyCode == 256) { // ESCAPE
            focus = null;
        }
        return true;
    }

    public boolean charTyped(char ch) {
        if (focus == null) return false;
        StringBuilder sb = fields.get(focus);
        if (sb != null && ch >= 32 && ch != 127 && sb.length() < 32) sb.append(ch);
        return true;
    }

    public boolean hasFocus() {
        return focus != null;
    }
}
