package thaumcraft.common.items;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.blocks.FluxBlock;
import thaumcraft.common.registry.ModBlocks;

/** Ведро жидкой смерти (ItemBucketDeath оригинала): выливает полный блок туда, куда смотрит игрок. */
public class LiquidDeathBucketItem extends Item {
    public LiquidDeathBucketItem() {
        super(new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET).rarity(Rarity.RARE));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos();
        if (!level.getBlockState(pos).canBeReplaced()) {
            pos = pos.relative(context.getClickedFace());
        }
        BlockState there = level.getBlockState(pos);
        if (!there.canBeReplaced() || player != null && !player.mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand())) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            level.destroyBlock(pos, true);
            level.setBlock(pos, ModBlocks.LIQUID_DEATH.get().defaultBlockState().setValue(FluxBlock.LEVEL, 7), 3);
            level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1f, 1f);
            if (player != null && !player.getAbilities().instabuild) {
                player.setItemInHand(context.getHand(), new ItemStack(Items.BUCKET));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
