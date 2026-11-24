package autominer.keybinds;

import autominer.AutoMiner;
import autominer.config.ConfigManager;
import autominer.config.ModConfig;
import autominer.gui.MainScreen;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/** Rejestracja skrotow (konfigurowalnych w Opcje -> Sterowanie) i ich obsluga. */
public final class KeybindManager {
    private static final String CATEGORY = "category.autominer";

    public static KeyBinding addBlock;
    public static KeyBinding toggleMiner;
    public static KeyBinding toggleDeposit;
    public static KeyBinding toggleHud;
    public static KeyBinding openGui;
    public static KeyBinding pauseMiner;
    public static KeyBinding selectChest;
    public static KeyBinding depositNow;

    private KeybindManager() {}

    private static KeyBinding reg(String id, int key) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding(id, InputUtil.Type.KEYSYM, key, CATEGORY));
    }

    public static void register() {
        addBlock = reg("key.autominer.add_block", GLFW.GLFW_KEY_F5);
        toggleMiner = reg("key.autominer.toggle_miner", GLFW.GLFW_KEY_F6);
        toggleDeposit = reg("key.autominer.toggle_deposit", GLFW.GLFW_KEY_F7);
        toggleHud = reg("key.autominer.toggle_hud", GLFW.GLFW_KEY_F8);
        openGui = reg("key.autominer.open_gui", GLFW.GLFW_KEY_F9);
        pauseMiner = reg("key.autominer.pause_miner", GLFW.GLFW_KEY_UNKNOWN);
        selectChest = reg("key.autominer.select_chest", GLFW.GLFW_KEY_UNKNOWN);
        depositNow = reg("key.autominer.deposit_now", GLFW.GLFW_KEY_UNKNOWN);
    }

    public static void tick(MinecraftClient c) {
        boolean inGame = c.player != null && c.world != null;
        boolean free = inGame && c.currentScreen == null;

        while (addBlock.wasPressed()) {
            if (free) AutoMiner.selector.handleKey(c, Screen.hasShiftDown());
        }
        while (toggleMiner.wasPressed()) {
            if (inGame) AutoMiner.mining.toggle();
        }
        while (pauseMiner.wasPressed()) {
            if (inGame) AutoMiner.mining.togglePause();
        }
        while (toggleDeposit.wasPressed()) {
            if (inGame) AutoMiner.deposit.toggleEnabled();
        }
        while (depositNow.wasPressed()) {
            if (inGame) AutoMiner.deposit.requestNow();
        }
        while (selectChest.wasPressed()) {
            if (free) AutoMiner.chests.startPicking();
        }
        while (toggleHud.wasPressed()) {
            ModConfig cfg = ConfigManager.get();
            cfg.hudVisible = !cfg.hudVisible;
            ConfigManager.markDirty();
            if (inGame) AutoMiner.notifications.info("HUD: " + (cfg.hudVisible ? "VISIBLE" : "HIDDEN"));
        }
        while (openGui.wasPressed()) {
            if (free) c.setScreen(new MainScreen());
        }
    }

    /**
     * Vanilla "Przelacz widok" domyslnie tez uzywa F5, a Minecraft przypisuje jeden klawisz
     * do jednego KeyBinding, wiec jeden z nich przestalby dzialac. Przy pierwszym uruchomieniu
     * przenosimy vanilla na F4 (mozna wylaczyc w ustawieniach ogolnych). NIEZWERYFIKOWANE API:
     * GameOptions.togglePerspectiveKey, KeyBinding.getBoundKeyTranslationKey/setBoundKey/updateKeysByCode.
     */
    public static void remapPerspective(MinecraftClient c) {
        ModConfig cfg = ConfigManager.get();
        if (!cfg.remapPerspectiveKey || cfg.perspectiveRemapped || c.options == null) return;
        KeyBinding vanilla = c.options.togglePerspectiveKey;
        if ("key.keyboard.f5".equals(vanilla.getBoundKeyTranslationKey())) {
            vanilla.setBoundKey(InputUtil.fromKeyCode(GLFW.GLFW_KEY_F4, 0));
            KeyBinding.updateKeysByCode();
            c.options.write();
            AutoMiner.LOGGER.info("Vanilla 'Toggle Perspective' przeniesiono z F5 na F4");
        }
        cfg.perspectiveRemapped = true;
        ConfigManager.markDirty();
    }
}
