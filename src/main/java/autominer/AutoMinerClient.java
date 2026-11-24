package autominer;

import autominer.config.ConfigManager;
import autominer.deposit.AutoDepositManager;
import autominer.deposit.ChestManager;
import autominer.hud.HudRenderer;
import autominer.keybinds.KeybindManager;
import autominer.mining.AutoMiningManager;
import autominer.mining.BlockWhitelist;
import autominer.notifications.NotificationManager;
import autominer.selector.BlockSelectionManager;
import autominer.selector.BlockSelector;
import autominer.statistics.InventoryWatcher;
import autominer.statistics.StatisticsManager;
import java.util.Map;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;

public class AutoMinerClient implements ClientModInitializer {
    private final InventoryWatcher watcher = new InventoryWatcher();
    private int errorCount;

    @Override
    public void onInitializeClient() {
        ConfigManager.load();
        AutoMiner.notifications = new NotificationManager();
        AutoMiner.selection = new BlockSelectionManager();
        AutoMiner.whitelist = new BlockWhitelist(AutoMiner.selection);
        AutoMiner.stats = new StatisticsManager();
        AutoMiner.stats.load();
        AutoMiner.mining = new AutoMiningManager(AutoMiner.whitelist);
        AutoMiner.chests = new ChestManager();
        AutoMiner.deposit = new AutoDepositManager();
        AutoMiner.selector = new BlockSelector();

        KeybindManager.register();
        AutoMiner.selector.registerRender();
        HudRenderer.register();

        ClientLifecycleEvents.CLIENT_STARTED.register(KeybindManager::remapPerspective);
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            AutoMiner.deposit.reset();
            AutoMiner.chests.cancelPicking();
            if (AutoMiner.mining.isActive()) AutoMiner.mining.stop("DISCONNECTED");
            AutoMiner.stats.save();
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            AutoMiner.mining.controller().releaseKeys(client);
            AutoMiner.stats.shutdown();
            ConfigManager.save();
        });

        AutoMiner.LOGGER.info("AutoMiner Ultimate zaladowany");
    }

    private void onTick(MinecraftClient c) {
        AutoMiner.tick++;
        try {
            KeybindManager.tick(c);
            Map<String, Integer> gains = watcher.poll(c);
            if (!gains.isEmpty() && AutoMiner.mining.isCountingItems(c)) {
                AutoMiner.mining.onItemsGained(gains);
            }
            AutoMiner.stats.syncSelection(AutoMiner.selection);
            AutoMiner.deposit.tick(c, AutoMiner.tick);
            AutoMiner.mining.tick(c, AutoMiner.tick);
            AutoMiner.stats.tick(AutoMiner.mining.status() == AutoMiningManager.Status.MINING
                    || AutoMiner.mining.status() == AutoMiningManager.Status.SEARCHING
                    || AutoMiner.mining.status() == AutoMiningManager.Status.WALKING);
            ConfigManager.tick();
        } catch (RuntimeException e) {
            AutoMiner.LOGGER.error("Blad w ticku AutoMiner", e);
            if (++errorCount >= 3) {
                errorCount = 0;
                AutoMiner.deposit.reset();
                if (AutoMiner.mining.isActive()) AutoMiner.mining.stop("INTERNAL ERROR (see log)");
            }
        }
    }
}
