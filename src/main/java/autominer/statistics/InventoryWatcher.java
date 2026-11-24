package autominer.statistics;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;

/** Co tick porownuje ilosci przedmiotow w ekwipunku i zwraca faktyczne przyrosty. */
public final class InventoryWatcher {
    private Map<String, Integer> last = new HashMap<>();
    private boolean primed;

    public static Map<String, Integer> snapshot(ClientPlayerEntity p) {
        Map<String, Integer> m = new HashMap<>();
        for (ItemStack s : p.getInventory().main) {
            if (s.isEmpty()) continue;
            m.merge(Registries.ITEM.getId(s.getItem()).toString(), s.getCount(), Integer::sum);
        }
        return m;
    }

    /** Zwraca przyrosty od poprzedniego wywolania (puste przy pierwszym wywolaniu po wejsciu do swiata). */
    public Map<String, Integer> poll(MinecraftClient c) {
        Map<String, Integer> gains = new HashMap<>();
        if (c.player == null) {
            primed = false;
            last = new HashMap<>();
            return gains;
        }
        Map<String, Integer> now = snapshot(c.player);
        if (primed) {
            for (Map.Entry<String, Integer> e : now.entrySet()) {
                int diff = e.getValue() - last.getOrDefault(e.getKey(), 0);
                if (diff > 0) gains.put(e.getKey(), diff);
            }
        }
        last = now;
        primed = true;
        return gains;
    }
}
