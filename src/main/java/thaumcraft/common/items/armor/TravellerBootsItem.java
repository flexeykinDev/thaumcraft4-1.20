package thaumcraft.common.items.armor;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thaumcraft.Thaumcraft;
import thaumcraft.common.registry.ModMaterials;

import java.util.UUID;

/**
 * Сапоги странника — перенос ItemBootsTraveller оригинала. Идущий вперёд в них бежит быстрее, перешагивает
 * целый блок, прыгает выше, лучше управляется в прыжке и мягче падает. Крадучись — шагает как обычно.
 */
public class TravellerBootsItem extends ArmorItem {
    private static final UUID STEP = UUID.fromString("b4e1c2a6-7d3f-4c55-9d4b-1a9e0c5f3e21");

    public TravellerBootsItem() {
        super(ModMaterials.SPECIAL_ARMOR, Type.BOOTS, new Item.Properties().durability(350).rarity(Rarity.RARE));
    }

    /** Шаг через целый блок (stepHeight = 1 оригинала): прибавка к высоте шага, пока сапоги надеты. */
    @Override
    public com.google.common.collect.Multimap<net.minecraft.world.entity.ai.attributes.Attribute, AttributeModifier> getAttributeModifiers(
            EquipmentSlot slot, ItemStack stack) {
        var modifiers = super.getAttributeModifiers(slot, stack);
        if (slot != EquipmentSlot.FEET) {
            return modifiers;
        }
        var result = com.google.common.collect.ImmutableMultimap.<net.minecraft.world.entity.ai.attributes.Attribute, AttributeModifier>builder();
        result.putAll(modifiers);
        result.put(ForgeMod.STEP_HEIGHT_ADDITION.get(), new AttributeModifier(STEP, "Boots of the Traveller", 0.4,
                AttributeModifier.Operation.ADDITION));
        return result.build();
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return "thaumcraft:textures/models/bootstraveler.png";
    }

    @Override
    public void onArmorTick(ItemStack stack, Level level, Player player) {
        boolean flying = player.getAbilities().flying;
        if (!flying && player.zza > 0f) {
            if (player.onGround()) {
                float bonus = player.isInWater() ? 0.055f / 4f : 0.055f;
                player.moveRelative(bonus, new Vec3(0.0, 0.0, 1.0));
            } else {
                // в прыжке управление как в оригинале: 0.05 вместо обычных 0.02 (в парении — 0.03)
                player.moveRelative(HoverHarnessItem.isHovering(player) ? 0.01f : 0.03f, new Vec3(0.0, 0.0, 1.0));
            }
        }
        if (player.fallDistance > 0f) {
            player.fallDistance = Math.max(0f, player.fallDistance - 0.25f);
        }
    }

    private static boolean wearing(Player player) {
        return player.getItemBySlot(EquipmentSlot.FEET).getItem() instanceof TravellerBootsItem;
    }

    @Mod.EventBusSubscriber(modid = Thaumcraft.MODID)
    public static final class Events {
        private Events() {
        }

        /** Прыжок в сапогах выше на 0.275 (как в оригинале). */
        @SubscribeEvent
        public static void jump(LivingEvent.LivingJumpEvent event) {
            if (event.getEntity() instanceof Player player && wearing(player)) {
                player.setDeltaMovement(player.getDeltaMovement().add(0.0, 0.275, 0.0));
            }
        }
    }
}
