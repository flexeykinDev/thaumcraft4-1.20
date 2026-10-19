package thaumictinkerer.common.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Литейная форма души (ItemSoulMould оригинала): щелчок по существу запоминает его вид; Shift+щелчок в воздух —
 * стирает. Вставленная в магнит для существ, ограничивает его этим видом.
 */
public class SoulMouldItem extends Item {
    public SoulMouldItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Nullable
    public static EntityType<?> pattern(ItemStack stack) {
        if (!stack.hasTag() || !stack.getTag().contains("pattern")) {
            return null;
        }
        ResourceLocation id = ResourceLocation.tryParse(stack.getTag().getString("pattern"));
        return id == null ? null : BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (target instanceof Player) {
            return InteractionResult.PASS;
        }
        if (!player.level().isClientSide) {
            player.getItemInHand(hand).getOrCreateTag().putString("pattern", BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString());
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && stack.hasTag()) {
            stack.getTag().remove("pattern");
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        EntityType<?> type = pattern(stack);
        tooltip.add((type == null ? Component.translatable("ttmisc.soulMould.nonAssigned")
                : Component.translatable("ttmisc.soulMould.pattern", type.getDescription())).withStyle(ChatFormatting.GRAY));
    }
}
