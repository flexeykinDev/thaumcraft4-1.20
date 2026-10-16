package thaumcraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.champion.LootBags;

import java.util.ArrayList;
import java.util.List;

/**
 * Старая урна и брошенный ящик (BlockLoot оригинала): хрупкие, разбиваются почти от касания и высыпают
 * от 1 до 3 предметов (больше у редких) из списков мешков с добычей той же редкости. Встречаются в курганах,
 * на развалинах культистов и в Запредельных землях.
 */
public class LootBlock extends Block {
    private static final VoxelShape URN = Shapes.or(box(3, 0, 3, 13, 1, 13), box(2, 1, 2, 14, 13, 14), box(4, 13, 4, 12, 16, 12));
    private static final VoxelShape CRATE = box(1, 0, 1, 15, 14, 15);
    /** 0 — обычная, 1 — необычная, 2 — редкая. */
    public final int tier;
    private final boolean urn;

    public LootBlock(Properties properties, int tier, boolean urn) {
        super(properties);
        this.tier = tier;
        this.urn = urn;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return urn ? URN : CRATE;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = new ArrayList<>();
        var random = params.getLevel().random;
        int count = 1 + tier + random.nextInt(3);
        for (int a = 0; a < count; a++) {
            ItemStack loot = LootBags.generate(tier, random);
            if (!loot.isEmpty()) {
                drops.add(loot.copy());
            }
        }
        return drops;
    }
}
