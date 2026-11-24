package autominer.mining;

import net.minecraft.block.BlockState;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PickaxeItem;
import net.minecraft.registry.entry.RegistryEntry;

/** Wybor kilofa z paska szybkiego dostepu (szybkosc, Efficiency, wytrzymalosc). */
public final class ToolManager {
    private ToolManager() {}

    public record ToolInfo(boolean present, String name, int percent) {}

    public static int durabilityLeft(ItemStack s) {
        return s.isDamageable() ? s.getMaxDamage() - s.getDamage() : Integer.MAX_VALUE;
    }

    public static int efficiencyLevel(ItemStack s) {
        ItemEnchantmentsComponent ench = s.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
        for (RegistryEntry<Enchantment> e : ench.getEnchantments()) {
            if (e.matchesKey(Enchantments.EFFICIENCY)) {
                return ench.getLevel(e);
            }
        }
        return 0;
    }

    /** Indeks slotu paska (0-8) z najlepszym kilofem dla bloku albo -1, gdy brak uzywalnego kilofa. */
    public static int bestSlot(ClientPlayerEntity p, BlockState state, int minDurability) {
        int best = -1;
        float bestScore = -1f;
        for (int i = 0; i < 9; i++) {
            ItemStack s = p.getInventory().getStack(i);
            if (!(s.getItem() instanceof PickaxeItem)) continue;
            if (durabilityLeft(s) <= minDurability) continue;
            float speed = s.getMiningSpeedMultiplier(state);
            int eff = efficiencyLevel(s);
            if (speed > 1.0f && eff > 0) speed += eff * eff + 1;
            float score = speed + (s.isSuitableFor(state) ? 1000f : 0f);
            if (score > bestScore) {
                bestScore = score;
                best = i;
            }
        }
        return best;
    }

    public static boolean hasUsablePickaxe(ClientPlayerEntity p, int minDurability) {
        for (int i = 0; i < 9; i++) {
            ItemStack s = p.getInventory().getStack(i);
            if (s.getItem() instanceof PickaxeItem && durabilityLeft(s) > minDurability) return true;
        }
        return false;
    }

    /** Opis kilofa do dashboardu/HUD: aktualnie trzymany, a w razie braku - pierwszy z paska. */
    public static ToolInfo describe(ClientPlayerEntity p) {
        if (p == null) return new ToolInfo(false, "NONE", 0);
        ItemStack held = p.getInventory().getStack(p.getInventory().selectedSlot);
        ItemStack use = held.getItem() instanceof PickaxeItem ? held : ItemStack.EMPTY;
        if (use.isEmpty()) {
            for (int i = 0; i < 9; i++) {
                ItemStack s = p.getInventory().getStack(i);
                if (s.getItem() instanceof PickaxeItem) {
                    use = s;
                    break;
                }
            }
        }
        if (use.isEmpty()) return new ToolInfo(false, "NONE", 0);
        int pct = use.isDamageable() ? Math.round(100f * (use.getMaxDamage() - use.getDamage()) / use.getMaxDamage()) : 100;
        return new ToolInfo(true, use.getName().getString(), pct);
    }
}
