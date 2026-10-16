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

/** Эфирная эссенция (ItemWispEssence оригинала): то, что остаётся от виспа, — две единицы его аспекта. */
public class WispEssenceItem extends Item {
    public static final int SIZE = 2;
    private static final String TAG_ASPECT = "Aspect";

    public WispEssenceItem() {
        super(new Item.Properties());
    }

    public static ItemStack of(Item essence, Aspect aspect) {
        ItemStack stack = new ItemStack(essence);
        stack.getOrCreateTag().putString(TAG_ASPECT, aspect.getTag());
        return stack;
    }

    @Nullable
    public static Aspect getAspect(ItemStack stack) {
        return stack.getItem() instanceof WispEssenceItem && stack.hasTag() ? Aspect.byTag(stack.getTag().getString(TAG_ASPECT)) : null;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Aspect aspect = getAspect(stack);
        if (aspect != null && level != null && level.isClientSide) {
            boolean known = thaumcraft.client.ClientKnowledge.get().hasDiscovered(aspect);
            tooltip.add((known ? aspect.getName().copy().append(" x" + SIZE) : Component.translatable("tc.aspect.unknown"))
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
