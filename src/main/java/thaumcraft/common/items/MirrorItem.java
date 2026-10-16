package thaumcraft.common.items;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import thaumcraft.common.blocks.MirrorBlock;
import thaumcraft.common.registry.ModSounds;

import java.util.List;

/**
 * Зеркало в виде предмета — перенос BlockMirrorItem оригинала. Щелчок зеркалом по уже стоящему свободному зеркалу
 * того же рода даёт связанное зеркало: где его ни поставь, оно станет парой первому, хоть в другом мире.
 */
public class MirrorItem extends BlockItem {
    public MirrorItem(Block block) {
        super(block, new Item.Properties().rarity(Rarity.UNCOMMON));
    }

    public static boolean isLinked(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains("linkX");
    }

    public static void setLink(ItemStack stack, BlockPos pos, ResourceLocation dimension) {
        stack.getOrCreateTag().putInt("linkX", pos.getX());
        stack.getTag().putInt("linkY", pos.getY());
        stack.getTag().putInt("linkZ", pos.getZ());
        stack.getTag().putString("linkDim", dimension.toString());
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null || !(level.getBlockEntity(context.getClickedPos()) instanceof MirrorBlock.Mirror mirror)
                || mirror.essentia() != ((MirrorBlock) getBlock()).isEssentia()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            player.swing(context.getHand());
            return InteractionResult.SUCCESS;
        }
        if (mirror.isLinkValid()) {
            player.displayClientMessage(Component.translatable("thaumcraft.mirror.already_linked")
                    .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC), false);
            return InteractionResult.SUCCESS;
        }
        ItemStack linked = stack.copyWithCount(1);
        setLink(linked, context.getClickedPos(), level.dimension().location());
        level.playSound(null, context.getClickedPos(), ModSounds.JAR.get(), SoundSource.BLOCKS, 1f, 2f);
        if (!player.getInventory().add(linked)) {
            player.drop(linked, false);
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }

    /** Поставленное связанное зеркало сразу ищет свою пару. */
    @Override
    protected boolean updateCustomBlockEntityTag(BlockPos pos, Level level, @Nullable Player player, ItemStack stack, BlockState state) {
        boolean changed = super.updateCustomBlockEntityTag(pos, level, player, stack, state);
        if (!level.isClientSide && isLinked(stack) && level.getBlockEntity(pos) instanceof MirrorBlock.Mirror mirror) {
            mirror.linkPos = new BlockPos(stack.getTag().getInt("linkX"), stack.getTag().getInt("linkY"), stack.getTag().getInt("linkZ"));
            ResourceLocation dim = ResourceLocation.tryParse(stack.getTag().getString("linkDim"));
            if (dim != null) {
                mirror.linkDim = dim;
            }
            mirror.restoreLink();
        }
        return changed;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (isLinked(stack)) {
            tooltip.add(Component.translatable("thaumcraft.handmirror.linked_to").append(" " + stack.getTag().getInt("linkX") + ","
                    + stack.getTag().getInt("linkY") + "," + stack.getTag().getInt("linkZ") + " — " + stack.getTag().getString("linkDim")));
        }
    }
}
