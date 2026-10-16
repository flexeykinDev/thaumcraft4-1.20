package thaumcraft.common.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;

import java.util.List;

/**
 * Ярлык для банок (ItemResource оригинала, мета 13). Наклеенный на банку или перегонный куб, закрепляет за сосудом
 * один аспект. Чистый ярлык берёт аспект у сосуда; ярлык с аспектом (получается на верстаке из ярлыка и колбы
 * эссенции) можно наклеить и на пустой сосуд.
 */
public class LabelItem extends Item {
    private static final String TAG_ASPECT = "Aspect";

    public LabelItem() {
        super(new Item.Properties());
    }

    @Nullable
    public static Aspect getAspect(ItemStack stack) {
        return stack.hasTag() ? Aspect.byTag(stack.getTag().getString(TAG_ASPECT)) : null;
    }

    public static ItemStack withAspect(Item label, Aspect aspect) {
        ItemStack stack = new ItemStack(label);
        stack.getOrCreateTag().putString(TAG_ASPECT, aspect.getTag());
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Aspect aspect = getAspect(stack);
        if (aspect != null && level != null && level.isClientSide) {
            boolean known = thaumcraft.client.ClientKnowledge.get().hasDiscovered(aspect);
            tooltip.add((known ? aspect.getName().copy() : Component.translatable("tc.aspect.unknown")).withStyle(ChatFormatting.GRAY));
        }
    }
}
