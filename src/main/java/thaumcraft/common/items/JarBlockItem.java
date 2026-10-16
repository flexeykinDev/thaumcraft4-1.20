package thaumcraft.common.items;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.blockentity.EssentiaBlockEntity;

import java.util.List;

/**
 * Запечатанная банка в виде предмета (BlockJarItem и ItemJarFilled оригинала). Содержимое хранится так же,
 * как у поставленной банки (BlockEntityTag), поэтому сломанная банка не теряет эссенцию.
 * Банкой в руке можно набрать эссенцию прямо из перегонного куба.
 */
public class JarBlockItem extends BlockItem {
    public JarBlockItem(Block block) {
        super(block, new Item.Properties());
    }

    @Nullable
    public static Aspect getAspect(ItemStack stack) {
        CompoundTag data = BlockItem.getBlockEntityData(stack);
        return data == null || data.getShort("Amount") <= 0 ? null : Aspect.byTag(data.getString("Aspect"));
    }

    public static int getAmount(ItemStack stack) {
        CompoundTag data = BlockItem.getBlockEntityData(stack);
        return data == null || Aspect.byTag(data.getString("Aspect")) == null ? 0 : data.getShort("Amount");
    }

    private static void setContents(ItemStack stack, Aspect aspect, int amount) {
        CompoundTag data = new CompoundTag();
        data.putString("Aspect", aspect.getTag());
        data.putShort("Amount", (short) amount);
        stack.getOrCreateTag().put("BlockEntityTag", data);
    }

    /** Полные банки не складываются: у каждой своё содержимое. */
    @Override
    public int getMaxStackSize(ItemStack stack) {
        return getAmount(stack) > 0 ? 1 : super.getMaxStackSize(stack);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null || !(level.getBlockEntity(context.getClickedPos()) instanceof EssentiaBlockEntity alembic)
                || alembic.getCapacity() != EssentiaBlockEntity.ALEMBIC_CAPACITY) {
            return InteractionResult.PASS;
        }
        Aspect aspect = alembic.getAspect();
        Aspect own = getAspect(stack);
        int stored = getAmount(stack);
        if (aspect == null || own != null && own != aspect || stored >= EssentiaBlockEntity.JAR_CAPACITY) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        int moved = Math.min(alembic.getAmount(), EssentiaBlockEntity.JAR_CAPACITY - stored);
        alembic.take(aspect, moved);
        ItemStack filled = new ItemStack(this);
        setContents(filled, aspect, stored + moved);
        stack.shrink(1);
        if (stack.isEmpty()) {
            player.setItemInHand(context.getHand(), filled);
        } else if (!player.getInventory().add(filled)) {
            player.drop(filled, false);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_SWIM, SoundSource.PLAYERS, 0.25f, 1f);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Aspect aspect = getAspect(stack);
        if (aspect != null && level != null && level.isClientSide) {
            boolean known = thaumcraft.client.ClientKnowledge.get().hasDiscovered(aspect);
            tooltip.add((known ? aspect.getName().copy().append(" x " + getAmount(stack)) : Component.translatable("tc.aspect.unknown"))
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
