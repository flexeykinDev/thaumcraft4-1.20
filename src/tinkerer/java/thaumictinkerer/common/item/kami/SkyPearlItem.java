package thaumictinkerer.common.item.kami;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import thaumcraft.common.Res;
import thaumictinkerer.common.registry.TTBlocks;

import java.util.List;

/** Небесная жемчужина (ItemSkyPearl): ПКМ по вратам искривления запоминает их; Shift+ПКМ в воздух стирает запись. */
public class SkyPearlItem extends Item {
    private static final String TAG = "gate";

    public SkyPearlItem() {
        super(new Item.Properties().stacksTo(1).rarity(Kami.RARITY));
    }

    public static boolean isAttuned(ItemStack stack) {
        return stack.getItem() instanceof SkyPearlItem && stack.hasTag() && stack.getTag().contains(TAG);
    }

    @Nullable
    public static BlockPos pos(ItemStack stack) {
        if (!isAttuned(stack)) {
            return null;
        }
        CompoundTag tag = stack.getTag().getCompound(TAG);
        return new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
    }

    @Nullable
    public static ResourceKey<Level> dimension(ItemStack stack) {
        return isAttuned(stack) ? ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                Res.parse(stack.getTag().getCompound(TAG).getString("dim"))) : null;
    }

    public static void attune(ItemStack stack, BlockPos pos, ResourceKey<Level> dimension) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("x", pos.getX());
        tag.putInt("y", pos.getY());
        tag.putInt("z", pos.getZ());
        tag.putString("dim", dimension.location().toString());
        stack.getOrCreateTag().put(TAG, tag);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        Level level = context.getLevel();
        if (!level.getBlockState(context.getClickedPos()).is(TTBlocks.WARP_GATE.get()) || isAttuned(stack)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            attune(stack, context.getClickedPos(), level.dimension());
            level.playSound(null, context.getClickedPos(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.3f, 0.1f);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && isAttuned(stack)) {
            if (!level.isClientSide) {
                stack.getTag().remove(TAG);
                level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.3f, 0.1f);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return isAttuned(stack);
    }

    /** addInfo: координаты, а в том же измерении — и расстояние. */
    public static void describe(ItemStack stack, @Nullable Level level, @Nullable Player player, List<Component> tooltip) {
        BlockPos pos = pos(stack);
        if (pos == null) {
            return;
        }
        tooltip.add(Component.literal("X: " + pos.getX() + "  Y: " + pos.getY() + "  Z: " + pos.getZ()).withStyle(ChatFormatting.GRAY));
        if (level != null && !level.dimension().equals(dimension(stack))) {
            tooltip.add(Component.translatable("ttmisc.differentDim").withStyle(ChatFormatting.RED));
        } else if (player != null) {
            tooltip.add(Component.translatable("ttmisc.distance").append(": " + Math.round(Math.sqrt(player.blockPosition().distSqr(pos))) + "m")
                    .withStyle(ChatFormatting.BLUE));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        describe(stack, level, net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient() ? ClientPlayer.get() : null, tooltip);
    }

    /** Игрок этого клиента — только для подсказки. */
    static final class ClientPlayer {
        private ClientPlayer() {
        }

        @Nullable
        static Player get() {
            return net.minecraft.client.Minecraft.getInstance().player;
        }
    }
}
