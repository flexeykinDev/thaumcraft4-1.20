package thaumcraft.common.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.common.registry.ModEffects;

/**
 * Порченая слизь и щупальце порчи (ItemResource оригинала, виды 11 и 12): добыча с порождений порчи. Носить их с собой
 * небезопасно — изредка они отравляют владельца порченым ядом, и тем чаще, чем больше стопка.
 */
public class TaintItem extends Item {
    public TaintItem() {
        super(new Item.Properties());
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!level.isClientSide && entity instanceof LivingEntity living && living.getMobType() != MobType.UNDEAD
                && !living.hasEffect(ModEffects.TAINT_POISON.get()) && level.random.nextInt(4321) <= stack.getCount()) {
            living.addEffect(new MobEffectInstance(ModEffects.TAINT_POISON.get(), 120, 0, false, true));
            if (entity instanceof Player player) {
                player.sendSystemMessage(Component.translatable("tc.taint_item_poison",
                        stack.getHoverName().copy().withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC)));
            }
        }
    }
}
