package thaumictinkerer.common.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumictinkerer.common.block.InfusedGrainBlock;
import thaumictinkerer.common.registry.TTBlocks;
import thaumictinkerer.common.registry.TTItems;

import java.util.List;

/** Наполненные семена (ItemInfusedSeeds): несут аспект растения и его склонности; сажаются на пашню, делая её наполненной. */
public class InfusedSeedsItem extends Item {
    public InfusedSeedsItem() {
        super(new Item.Properties().rarity(Rarity.RARE));
    }

    public static ItemStack of(Aspect aspect, AspectList tendencies) {
        ItemStack stack = new ItemStack(TTItems.INFUSED_SEEDS.get());
        stack.getOrCreateTag().putString("aspect", aspect.getTag());
        stack.getTag().put("tendencies", tendencies.save(new CompoundTag()));
        return stack;
    }

    @Nullable
    public static Aspect aspect(ItemStack stack) {
        return stack.hasTag() ? Aspect.byTag(stack.getTag().getString("aspect")) : null;
    }

    public static AspectList tendencies(ItemStack stack) {
        return stack.hasTag() ? AspectList.load(stack.getTag().getCompound("tendencies")) : new AspectList();
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState soil = level.getBlockState(pos);
        Aspect aspect = aspect(context.getItemInHand());
        boolean infused = soil.is(TTBlocks.INFUSED_FARMLAND.get());
        if (context.getClickedFace() != Direction.UP || aspect == null || !(infused || soil.is(Blocks.FARMLAND))
                || !level.isEmptyBlock(pos.above()) || context.getPlayer() != null
                && !context.getPlayer().mayUseItemAt(pos.above(), Direction.UP, context.getItemInHand())) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            if (!infused) {
                level.setBlock(pos, TTBlocks.INFUSED_FARMLAND.get().defaultBlockState().setValue(FarmBlock.MOISTURE,
                        soil.getValue(FarmBlock.MOISTURE)), Block.UPDATE_ALL);
            }
            level.setBlock(pos.above(), TTBlocks.INFUSED_GRAIN.get().defaultBlockState()
                    .setValue(InfusedGrainBlock.TEXTURE, InfusedGrainBlock.texture(aspect)), Block.UPDATE_ALL);
            if (level.getBlockEntity(pos.above()) instanceof InfusedGrainBlock.Tile tile) {
                tile.setup(aspect, tendencies(context.getItemInHand()));
            }
            level.gameEvent(context.getPlayer(), GameEvent.BLOCK_PLACE, pos.above());
            if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Aspect aspect = aspect(stack);
        if (aspect != null) {
            tooltip.add(aspect.getName().copy().withStyle(ChatFormatting.GRAY));
        }
        AspectList tendencies = tendencies(stack);
        for (Aspect tendency : tendencies.getAspects()) {
            tooltip.add(tendency.getName().copy().append(": " + tendencies.getAmount(tendency)).withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
