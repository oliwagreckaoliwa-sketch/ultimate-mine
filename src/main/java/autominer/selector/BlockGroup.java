package autominer.selector;

import java.util.ArrayList;
import java.util.List;

/** Grupa bloków (np. "Ores"). Przechowuje identyfikatory typu "minecraft:stone". */
public class BlockGroup {
    public String name = "Default";
    public boolean enabled = true;
    public List<String> blocks = new ArrayList<>();

    public BlockGroup() {}

    public BlockGroup(String name) {
        this.name = name;
    }

    public boolean contains(String id) {
        return blocks.contains(id);
    }
}
