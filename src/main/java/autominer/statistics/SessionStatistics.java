package autominer.statistics;

/** Statystyki jednej sesji (od pierwszego startu Auto Miner do zamkniecia gry lub resetu). */
public class SessionStatistics {
    public long startMs;
    public long endMs;
    public long miningMs;
    public long blocksMined;
    public long itemsCollected;
    public long itemsDeposited;
    public int depositOps;
    public ResourceCounter counters = new ResourceCounter();

    public boolean isEmpty() {
        return blocksMined == 0 && itemsCollected == 0 && itemsDeposited == 0 && miningMs < 1000;
    }

    /** Czas trwania sesji (zegarowy). Zero, dopoki sesja sie nie zaczela. */
    public long elapsedMs(long now) {
        if (startMs <= 0) return 0;
        return Math.max(0, (endMs > 0 ? endMs : now) - startMs);
    }

    public void mergeFrom(SessionStatistics o) {
        miningMs += o.miningMs;
        blocksMined += o.blocksMined;
        itemsCollected += o.itemsCollected;
        itemsDeposited += o.itemsDeposited;
        depositOps += o.depositOps;
        counters.mergeFrom(o.counters);
        if (o.startMs > 0 && (startMs == 0 || o.startMs < startMs)) startMs = o.startMs;
    }
}
