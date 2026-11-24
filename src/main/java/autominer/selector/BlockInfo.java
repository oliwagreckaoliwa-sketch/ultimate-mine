package autominer.selector;

import autominer.util.Fmt;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/** Pomocnik: identyfikator bloku -> blok, nazwa wyswietlana, ikona. Zrodlem prawdy jest zawsze identyfikator. */
public final class BlockInfo {
    private static final Map<String, ItemStack> ICONS = new HashMap<>();

    private BlockInfo() {}

    public static String idOf(Block block) {
        return Registries.BLOCK.getId(block).toString();
    }

    public static Block block(String id) {
        Identifier i = Identifier.tryParse(id);
        if (i == null) return null;
        return Registries.BLOCK.getOrEmpty(i).orElse(null);
    }

    public static boolean registered(String id) {
        return block(id) != null;
    }

    public static String name(String id) {
        Block b = block(id);
        if (b != null) return b.getName().getString();
        Identifier i = Identifier.tryParse(id);
        return Fmt.prettify(i != null ? i.getPath() : id);
    }

    public static ItemStack icon(String id) {
        return ICONS.computeIfAbsent(id, k -> {
            Block b = block(k);
            Item item = b != null ? b.asItem() : Items.AIR;
            return new ItemStack(item == Items.AIR ? Items.BARRIER : item);
        });
    }

    // --- przedmioty ---

    public static Item item(String id) {
        Identifier i = Identifier.tryParse(id);
        if (i == null) return null;
        return Registries.ITEM.getOrEmpty(i).orElse(null);
    }

    public static String itemId(Item item) {
        return Registries.ITEM.getId(item).toString();
    }

    public static String itemName(String id) {
        Item it = item(id);
        if (it != null) return it.getName().getString();
        Identifier i = Identifier.tryParse(id);
        return Fmt.prettify(i != null ? i.getPath() : id);
    }

    public static ItemStack itemIcon(String id) {
        Item it = item(id);
        return new ItemStack(it == null || it == Items.AIR ? Items.BARRIER : it);
    }
}
