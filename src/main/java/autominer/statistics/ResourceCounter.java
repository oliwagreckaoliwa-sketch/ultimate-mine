package autominer.statistics;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Liczniki rozdzielone na dwie niezalezne mapy: wykopane BLOKI i uzyskane PRZEDMIOTY.
 * Dzieki temu Iron Ore (blok) nie miesza sie z Raw Iron (przedmiot).
 */
public class ResourceCounter {

    /** Standardowy licznik z listy w specyfikacji. */
    public record Standard(String key, String label, boolean block, List<String> ids) {}

    public static final List<Standard> STANDARD = List.of(
            new Standard("iron_ore", "Iron Ore", true, List.of("minecraft:iron_ore", "minecraft:deepslate_iron_ore")),
            new Standard("raw_iron", "Raw Iron", false, List.of("minecraft:raw_iron")),
            new Standard("gold_ore", "Gold Ore", true, List.of("minecraft:gold_ore", "minecraft:deepslate_gold_ore")),
            new Standard("raw_gold", "Raw Gold", false, List.of("minecraft:raw_gold")),
            new Standard("diamond_ore", "Diamond Ore", true, List.of("minecraft:diamond_ore", "minecraft:deepslate_diamond_ore")),
            new Standard("diamonds", "Diamonds", false, List.of("minecraft:diamond")),
            new Standard("cobblestone", "Cobblestone", false, List.of("minecraft:cobblestone")),
            new Standard("stone", "Stone", true, List.of("minecraft:stone")),
            new Standard("deepslate", "Deepslate", true, List.of("minecraft:deepslate")));

    public Map<String, Long> blocks = new LinkedHashMap<>();
    public Map<String, Long> items = new LinkedHashMap<>();

    public void ensureBlock(String id) {
        blocks.putIfAbsent(id, 0L);
    }

    public void addBlock(String id) {
        blocks.merge(id, 1L, Long::sum);
    }

    public void addItem(String id, long n) {
        if (n > 0) items.merge(id, n, Long::sum);
    }

    public long block(String id) {
        return blocks.getOrDefault(id, 0L);
    }

    public long item(String id) {
        return items.getOrDefault(id, 0L);
    }

    public long value(Standard s) {
        long sum = 0;
        for (String id : s.ids()) {
            sum += s.block() ? block(id) : item(id);
        }
        return sum;
    }

    public void mergeFrom(ResourceCounter o) {
        o.blocks.forEach((k, v) -> blocks.merge(k, v, Long::sum));
        o.items.forEach((k, v) -> items.merge(k, v, Long::sum));
    }

    public void clear() {
        blocks.clear();
        items.clear();
    }
}
