package autominer.deposit;

import autominer.config.ModConfig;
import autominer.selector.BlockInfo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

/** Operacje na ekwipunku gracza i otwartym kontenerze (tylko odczyt + wybor slotow do przeniesienia). */
public final class InventoryTransfer {
    private InventoryTransfer() {}

    public static Set<String> filterSet(ModConfig cfg) {
        Set<String> s = new HashSet<>(cfg.depositFilter);
        s.removeAll(cfg.depositFilterOff);
        return s;
    }

    public static Map<String, Integer> matchingCounts(ClientPlayerEntity p, Set<String> filter) {
        Map<String, Integer> m = new HashMap<>();
        for (ItemStack s : p.getInventory().main) {
            if (s.isEmpty()) continue;
            String id = BlockInfo.itemId(s.getItem());
            if (filter.contains(id)) m.merge(id, s.getCount(), Integer::sum);
        }
        return m;
    }

    public static int countMatching(ClientPlayerEntity p, Set<String> filter) {
        int n = 0;
        for (int v : matchingCounts(p, filter).values()) n += v;
        return n;
    }

    public static int freeMainSlots(ClientPlayerEntity p) {
        int n = 0;
        for (ItemStack s : p.getInventory().main) {
            if (s.isEmpty()) n++;
        }
        return n;
    }

    /** Identyfikatory slotow ekwipunku gracza (w oknie kontenera) zawierajacych przedmioty z filtra. */
    public static List<Integer> slotsToMove(ClientPlayerEntity p, ScreenHandler h, Set<String> filter) {
        List<Integer> out = new ArrayList<>();
        for (Slot s : h.slots) {
            if (s.inventory != p.getInventory() || s.getIndex() >= 36) continue;
            ItemStack st = s.getStack();
            if (st.isEmpty()) continue;
            if (filter.contains(BlockInfo.itemId(st.getItem()))) out.add(s.id);
        }
        return out;
    }

    /** Liczba pustych slotow w kontenerze (bez slotow gracza). */
    public static int containerFreeSlots(ClientPlayerEntity p, ScreenHandler h) {
        int n = 0;
        for (Slot s : h.slots) {
            if (s.inventory == p.getInventory()) continue;
            if (!s.hasStack()) n++;
        }
        return n;
    }
}
