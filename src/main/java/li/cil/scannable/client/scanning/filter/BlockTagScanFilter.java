package li.cil.scannable.client.scanning.filter;

import net.minecraft.tags.Tag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Predicate;

public record BlockTagScanFilter(Tag<Block> tag) implements Predicate<BlockState> {
    @Override
    public boolean test(final BlockState state) {
        return tag.contains(state.getBlock());
    }
}