package thaumictinkerer.common.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import thaumictinkerer.common.registry.TTBlocks;
import thaumictinkerer.common.registry.TTItems;

/**
 * Гиперзаряженный нитор (ItemBrightNitor оригинала): пока лежит в инвентаре, над головой владельца висит свет,
 * который идёт следом и гаснет за спиной.
 */
public class BrightNitorItem extends Item {
    public BrightNitorItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    public static boolean carries(Player player) {
        return player.getInventory().contains(new ItemStack(TTItems.BRIGHT_NITOR.get()));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide) {
            return;
        }
        BlockPos pos = BlockPos.containing(entity.getX(), entity.getY() + 1, entity.getZ());
        BlockState there = level.getBlockState(pos);
        if (there.isAir()) {
            level.setBlock(pos, TTBlocks.NITOR_GAS.get().defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }
}
