package thaumictinkerer.common.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import thaumcraft.common.registry.ModEffects;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;
import java.util.Set;

/**
 * Талисман исцеления (ItemCleansingTalisman оригинала): надетый на шею и включённый (Shift+щелчок), раз в секунду
 * тушит огонь или снимает одно вредное действие. Каждое снятие стоит прочности: обычное — 1, а недуги искажения
 * Thaumcraft — 10.
 */
public class CleansingTalismanItem extends Item implements ICurioItem {
    public CleansingTalismanItem() {
        super(new Item.Properties().durability(100).rarity(Rarity.UNCOMMON));
    }

    public static boolean isEnabled(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean("enabled");
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }
        stack.getOrCreateTag().putBoolean("enabled", !isEnabled(stack));
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.3f, 0.1f);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void curioTick(SlotContext context, ItemStack stack) {
        LivingEntity wearer = context.entity();
        if (!isEnabled(stack) || wearer.level().isClientSide || wearer.tickCount % 20 != 0 || !(wearer instanceof Player player)) {
            return;
        }
        int cost = 0;
        if (player.isOnFire()) {
            player.clearFire();
            cost = 1;
        } else {
            for (MobEffectInstance effect : List.copyOf(player.getActiveEffects())) {
                MobEffect type = effect.getEffect();
                if (type.getCategory() == MobEffectCategory.HARMFUL && type != ModEffects.WARP_WARD.get()) {
                    player.removeEffect(type);
                    cost = warpEffects().contains(type) ? 10 : 1;
                    break;
                }
            }
        }
        if (cost > 0) {
            stack.hurtAndBreak(cost, player, broken -> { });
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), thaumcraft.common.registry.ModSounds.WAND.get(),
                    SoundSource.PLAYERS, 0.3f, 0.1f);
        }
    }

    private static Set<MobEffect> warpEffects() {
        return Set.of(ModEffects.BLURRED_VISION.get(), ModEffects.DEATH_GAZE.get(), ModEffects.INFECTIOUS_VIS_EXHAUSTION.get(),
                ModEffects.SUN_SCORNED.get(), ModEffects.UNNATURAL_HUNGER.get());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(isEnabled(stack) ? "ttmisc.active" : "ttmisc.inactive").withStyle(ChatFormatting.GRAY));
    }
}
