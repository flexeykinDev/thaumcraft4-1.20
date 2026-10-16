package thaumcraft.common.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.blockentity.EssentiaBlockEntity;

import java.util.List;

/**
 * Стеклянная колба (ItemEssence оригинала). Пустая набирает 8 единиц эссенции из перегонного куба или банки,
 * полная выливается в банку. Аспект содержимого лежит в NBT, пустые колбы NBT не имеют и складываются.
 */
public class PhialItem extends Item {
    public static final int SIZE = 8;
    private static final String TAG_ASPECT = "Aspect";

    public PhialItem() {
        super(new Item.Properties());
    }

    @Nullable
    public static Aspect getAspect(ItemStack stack) {
        return stack.hasTag() ? Aspect.byTag(stack.getTag().getString(TAG_ASPECT)) : null;
    }

    public static ItemStack filled(Item phial, Aspect aspect) {
        ItemStack stack = new ItemStack(phial);
        stack.getOrCreateTag().putString(TAG_ASPECT, aspect.getTag());
        return stack;
    }

    @Override
    public String getDescriptionId(ItemStack stack) {
        return getAspect(stack) == null ? super.getDescriptionId(stack) : super.getDescriptionId(stack) + ".filled";
    }

    /** Сосуд обрабатывается раньше блока, чтобы клик колбой не открывал и не простукивал его. */
    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null || !(level.getBlockEntity(context.getClickedPos()) instanceof EssentiaBlockEntity vessel)) {
            return InteractionResult.PASS;
        }
        Aspect held = getAspect(stack);
        boolean jar = vessel.getCapacity() == EssentiaBlockEntity.JAR_CAPACITY;
        ItemStack result;
        if (held == null) {
            Aspect aspect = vessel.getAspect();
            if (aspect == null || vessel.getAmount() < SIZE) {
                return InteractionResult.PASS;
            }
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            vessel.take(aspect, SIZE);
            result = filled(this, aspect);
        } else {
            // вылить можно только в банку, и только если колба войдёт целиком
            if (!jar || !vessel.accepts(held) || vessel.getAmount() > vessel.getCapacity() - SIZE) {
                return InteractionResult.PASS;
            }
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            vessel.add(held, SIZE);
            result = new ItemStack(this);
        }
        if (!player.getAbilities().instabuild || held == null) {
            stack.shrink(1);
            if (!player.getInventory().add(result)) {
                player.drop(result, false);
            }
        }
        level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_SWIM, SoundSource.PLAYERS, 0.25f, 1f);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Aspect aspect = getAspect(stack);
        if (aspect != null && level != null && level.isClientSide) {
            boolean known = thaumcraft.client.ClientKnowledge.get().hasDiscovered(aspect);
            tooltip.add((known ? aspect.getName().copy().append(" x " + SIZE) : Component.translatable("tc.aspect.unknown"))
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
