package thaumcraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.blockentity.NodeBlockEntity;
import thaumcraft.common.items.WispEssenceItem;
import thaumcraft.common.registry.ModItems;

/**
 * onBlockHarvested оригинала для блоков с узлом внутри (свободный узел, сучок серебряного дерева, заряженный
 * обсидиановый тотем): разрушенный игроком узел рассыпается эфирными эссенциями — по одной на каждые десять
 * единиц аспекта (и ещё одной сверх того), если аспекта в узле хотя бы пять.
 */
public final class NodeHarvest {
    private NodeHarvest() {
    }

    public static void dropEssences(Level level, BlockPos pos, Player player) {
        if (level.isClientSide || player.getAbilities().instabuild || !(level.getBlockEntity(pos) instanceof NodeBlockEntity node)) {
            return;
        }
        for (Aspect aspect : node.getAspects()) {
            int amount = node.getAmount(aspect);
            if (amount < 5) {
                continue;
            }
            for (int a = 0; a <= amount / 10; a++) {
                ItemStack essence = WispEssenceItem.of(ModItems.WISP_ESSENCE.get(), aspect);
                Block.popResource(level, pos, essence);
            }
        }
    }
}
