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
 * Кристаллизованная эссенция (ItemCrystalEssence оригинала): единица эссенции одного аспекта в твёрдом виде.
 * Получается в кристаллизаторе; годится в тигель и в алхимическую печь как чистый источник своего аспекта.
 */
public class CrystalEssenceItem extends Item {
    private static final String TAG_ASPECT = "Aspect";

    public CrystalEssenceItem() {
        super(new Item.Properties());
    }

    @Nullable
    public static Aspect getAspect(ItemStack stack) {
        return stack.hasTag() ? Aspect.byTag(stack.getTag().getString(TAG_ASPECT)) : null;
    }

    public static ItemStack of(Item crystal, Aspect aspect) {
        ItemStack stack = new ItemStack(crystal);
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
