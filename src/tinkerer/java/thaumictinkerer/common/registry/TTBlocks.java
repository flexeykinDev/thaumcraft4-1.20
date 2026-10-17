package thaumictinkerer.common.registry;

import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import thaumictinkerer.ThaumicTinkerer;

/** Блоки Thaumic Tinkerer. */
public final class TTBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, ThaumicTinkerer.MODID);

    private TTBlocks() {
    }
}
