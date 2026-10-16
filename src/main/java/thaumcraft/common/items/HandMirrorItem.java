package thaumcraft.common.items;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;
import thaumcraft.common.blocks.MirrorBlock;
import thaumcraft.common.menu.HandMirrorMenu;
import thaumcraft.common.registry.ModSounds;

import java.util.List;

/**
 * Ручное зеркало — перенос ItemHandMirror оригинала. Привязанное к магическому зеркалу, оно открывает окно с одной
 * ячейкой: положенная туда вещь тут же выпадает из того зеркала, где бы оно ни стояло.
 */
public class HandMirrorItem extends Item {
    public HandMirrorItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return MirrorItem.isLinked(stack);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null || !(level.getBlockEntity(context.getClickedPos()) instanceof MirrorBlock.Mirror mirror) || mirror.essentia()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            player.swing(context.getHand());
            return InteractionResult.SUCCESS;
        }
        MirrorItem.setLink(stack, context.getClickedPos(), level.dimension().location());
        level.playSound(null, context.getClickedPos(), ModSounds.JAR.get(), SoundSource.BLOCKS, 1f, 2f);
        player.displayClientMessage(Component.translatable("thaumcraft.handmirror.linked").withStyle(ChatFormatting.DARK_PURPLE,
                ChatFormatting.ITALIC), false);
        return InteractionResult.SUCCESS;
    }

    /** Зеркало, к которому привязано ручное; если его там больше нет, связь рвётся. */
    @Nullable
    public static MirrorBlock.Mirror target(ItemStack stack, ServerPlayer player) {
        if (!MirrorItem.isLinked(stack)) {
            return null;
        }
        ResourceLocation dim = ResourceLocation.tryParse(stack.getTag().getString("linkDim"));
        ServerLevel level = dim == null ? null : player.server.getLevel(ResourceKey.create(Registries.DIMENSION, dim));
        BlockPos pos = new BlockPos(stack.getTag().getInt("linkX"), stack.getTag().getInt("linkY"), stack.getTag().getInt("linkZ"));
        if (level != null && level.getBlockEntity(pos) instanceof MirrorBlock.Mirror mirror && !mirror.essentia()) {
            return mirror;
        }
        if (level != null) {
            stack.setTag(null);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.ZAP.get(), SoundSource.PLAYERS, 1f, 0.8f);
            player.displayClientMessage(Component.translatable("thaumcraft.handmirror.error").withStyle(ChatFormatting.DARK_PURPLE,
                    ChatFormatting.ITALIC), false);
        }
        return null;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer server && MirrorItem.isLinked(stack) && target(stack, server) != null) {
            NetworkHooks.openScreen(server, new SimpleMenuProvider((id, inventory, p) -> new HandMirrorMenu(id, inventory),
                    stack.getHoverName()));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** Вещь уходит в привязанное зеркало (ItemHandMirror.transport). */
    public static boolean transport(ItemStack mirror, ItemStack items, ServerPlayer player) {
        MirrorBlock.Mirror target = target(mirror, player);
        if (target == null) {
            return false;
        }
        target.receiveFromHand(items.copy());
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.1f, 1f);
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (MirrorItem.isLinked(stack)) {
            tooltip.add(Component.translatable("thaumcraft.handmirror.linked_to").append(" " + stack.getTag().getInt("linkX") + ","
                    + stack.getTag().getInt("linkY") + "," + stack.getTag().getInt("linkZ") + " — " + stack.getTag().getString("linkDim")));
        }
    }
}
