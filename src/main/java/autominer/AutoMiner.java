package autominer;

import autominer.deposit.AutoDepositManager;
import autominer.deposit.ChestManager;
import autominer.mining.AutoMiningManager;
import autominer.mining.BlockWhitelist;
import autominer.notifications.NotificationManager;
import autominer.selector.BlockSelectionManager;
import autominer.selector.BlockSelector;
import autominer.statistics.StatisticsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Stałe moda i referencje do usług (ustawiane w {@link AutoMinerClient}). */
public final class AutoMiner {
    public static final String MOD_ID = "autominer";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static NotificationManager notifications;
    public static BlockSelectionManager selection;
    public static BlockWhitelist whitelist;
    public static BlockSelector selector;
    public static StatisticsManager stats;
    public static AutoMiningManager mining;
    public static ChestManager chests;
    public static AutoDepositManager deposit;

    /** Licznik ticków klienta (rośnie w END_CLIENT_TICK). */
    public static long tick;

    private AutoMiner() {}
}
