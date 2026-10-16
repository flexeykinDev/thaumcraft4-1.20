package thaumcraft.common.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.VisDiscountGear;
import thaumcraft.api.aspects.Aspect;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

/**
 * Кольцо ученика (ItemBaubleBlanks оригинала, виды 3–8): железное кольцо одной из шести стихий, которое удешевляет
 * заклинания этой стихии на 1%. Не крафтится — находится в сундуках и мешках с добычей.
 */
public class ApprenticeRingItem extends Item implements VisDiscountGear, ICurioItem {
    private final String aspectTag;

    public ApprenticeRingItem(String aspectTag) {
        super(new Item.Properties().stacksTo(1));
        this.aspectTag = aspectTag;
    }

    public Aspect getAspect() {
        return Aspect.byTag(aspectTag);
    }

    @Override
    public int getVisDiscount(ItemStack stack, @Nullable Player player, @Nullable Aspect aspect) {
        return aspect != null && aspect == getAspect() ? 1 : 0;
    }

    /** В оригинале название одно на все шесть колец, с подстановкой имени стихии. */
    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.thaumcraft.apprentice_ring", getAspect().getName());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(getAspect().getName().copy().append(" ").append(Component.translatable("tc.discount")).append(": 1%")
                .withStyle(ChatFormatting.DARK_PURPLE));
    }
}
