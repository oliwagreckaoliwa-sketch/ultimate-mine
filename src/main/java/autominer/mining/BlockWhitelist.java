package autominer.mining;

import autominer.selector.BlockSelectionManager;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/** Zbior obiektow Block odpowiadajacy zaznaczonym identyfikatorom (odswiezany tylko po zmianie). */
public final class BlockWhitelist {
    private final BlockSelectionManager selection;
    private Set<Block> blocks = Set.of();
    private int version = -1;

    public BlockWhitelist(BlockSelectionManager selection) {
        this.selection = selection;
    }

    private void refresh() {
        if (selection.version() == version) return;
        version = selection.version();
        Set<Block> s = new HashSet<>();
        for (String id : selection.selectedIds()) {
            Identifier i = Identifier.tryParse(id);
            if (i == null) continue;
            Registries.BLOCK.getOrEmpty(i).ifPresent(s::add);
        }
        s.remove(Blocks.AIR);
        blocks = s;
    }

    public boolean contains(Block b) {
        refresh();
        return blocks.contains(b);
    }

    public boolean isEmpty() {
        refresh();
        return blocks.isEmpty();
    }
}
