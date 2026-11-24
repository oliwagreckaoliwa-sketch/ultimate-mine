package autominer.gui;

import autominer.config.ConfigManager;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import net.minecraft.client.MinecraftClient;

/** Zakladka glownego okna. Rysuje sie w prostokacie (x, y, w, h) przy uzyciu {@link Ui}. */
public abstract class GuiPanel {
    public abstract String title();

    public abstract void render(Ui ui, int x, int y, int w, int h, MinecraftClient c);

    protected static void dirty() {
        ConfigManager.markDirty();
    }

    protected static void stepD(Ui ui, String id, int x, int y, int w, String label, double val,
                                double step, double min, double max, String fmt, DoubleConsumer set) {
        ui.stepper(id, x, y, w, label, String.format(Locale.ROOT, fmt, val),
                () -> {
                    set.accept(Math.max(min, Math.round((val - step) * 1000) / 1000.0));
                    dirty();
                },
                () -> {
                    set.accept(Math.min(max, Math.round((val + step) * 1000) / 1000.0));
                    dirty();
                });
    }

    protected static void stepI(Ui ui, String id, int x, int y, int w, String label, int val,
                                int step, int min, int max, String suffix, IntConsumer set) {
        ui.stepper(id, x, y, w, label, val + suffix,
                () -> {
                    set.accept(Math.max(min, val - step));
                    dirty();
                },
                () -> {
                    set.accept(Math.min(max, val + step));
                    dirty();
                });
    }

    protected static void tog(Ui ui, String id, int x, int y, int w, String label, boolean v, Consumer<Boolean> set) {
        ui.text(label, x, y + 3, Draw.TEXT);
        ui.toggle(id, x + w - 28, y, v, b -> {
            set.accept(b);
            dirty();
        });
    }

    protected static void section(Ui ui, String title, int x, int y) {
        ui.text(title.toUpperCase(Locale.ROOT), x, y, ui.accent);
    }
}
