package thaumictinkerer.common.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;

import java.util.List;

/** Наполненное зерно (ItemInfusedGrain): добыча спелого первичного растения, из него в тигле варят зелье. */
public class InfusedGrainItem extends Item {
    private final Aspect aspect;

    public InfusedGrainItem(Aspect aspect) {
        super(new Item.Properties());
        this.aspect = aspect;
    }

    public Aspect aspect() {
        return aspect;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(aspect.getName().copy().withStyle(ChatFormatting.GRAY));
    }
}
