package thaumictinkerer.common.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;

import java.util.List;
import java.util.function.Supplier;

/** Наполненное зелье (ItemInfusedPotion): выпитое даёт на три минуты силу своей стихии. */
public class InfusedPotionItem extends Item {
    public static final int DURATION = 3600;

    private final Aspect aspect;
    private final Supplier<? extends MobEffect> effect;

    public InfusedPotionItem(Aspect aspect, Supplier<? extends MobEffect> effect) {
        super(new Item.Properties().stacksTo(1));
        this.aspect = aspect;
        this.effect = effect;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide) {
            entity.addEffect(new MobEffectInstance(effect.get(), DURATION));
        }
        if (entity instanceof ServerPlayer player) {
            player.awardStat(Stats.ITEM_USED.get(this));
        }
        if (entity instanceof Player player && player.getAbilities().instabuild) {
            return stack;
        }
        stack.shrink(1);
        if (stack.isEmpty()) {
            return new ItemStack(Items.GLASS_BOTTLE);
        }
        if (entity instanceof Player player && !player.getInventory().add(new ItemStack(Items.GLASS_BOTTLE))) {
            player.drop(new ItemStack(Items.GLASS_BOTTLE), false);
        }
        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 32;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(aspect.getName().copy().withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(effect.get().getDescriptionId()).append(" (3:00)").withStyle(ChatFormatting.BLUE));
    }
}
