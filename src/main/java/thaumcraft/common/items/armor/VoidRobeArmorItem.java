package thaumcraft.common.items.armor;

import net.minecraft.ChatFormatting;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import thaumcraft.Thaumcraft;
import thaumcraft.api.Goggles;
import thaumcraft.api.RunicArmor;
import thaumcraft.api.VisDiscountGear;
import thaumcraft.api.WarpingGear;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.nodes.Revealer;
import thaumcraft.common.items.tools.VoidGear;
import thaumcraft.common.registry.ModMaterials;

import java.util.List;
import java.util.function.Consumer;

/**
 * Пустотная мантия — перенос ItemVoidRobeArmor оригинала: капюшон, мантия и поножи из пустотного металла и
 * зачарованной ткани. Защищают лучше алмазной брони, в том числе от магии; каждая часть удешевляет заклинания на 5%,
 * сама чинится и даёт 2 единицы искажения; капюшон работает как очки откровения. Красится, как кожаная броня.
 */
public class VoidRobeArmorItem extends ArmorItem implements DyeableLeatherItem, VisDiscountGear, Goggles, Revealer, RunicArmor, WarpingGear {
    /** Цвет некрашеной мантии. */
    public static final int DEFAULT_COLOR = 6961280;

    public VoidRobeArmorItem(Type type) {
        super(ModMaterials.VOID_FORTRESS_ARMOR, type, new Item.Properties().rarity(Rarity.EPIC));
    }

    @Override
    public int getColor(ItemStack stack) {
        var display = stack.getTagElement("display");
        return display != null && display.contains("color", 99) ? display.getInt("color") : DEFAULT_COLOR;
    }

    /** Как в оригинале: окрашиваемый слой — *_overlay, поверх него неокрашенный рисунок. */
    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return "overlay".equals(type) ? "thaumcraft:textures/models/void_robe_armor.png" : "thaumcraft:textures/models/void_robe_armor_overlay.png";
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        VoidGear.selfRepair(stack, level, entity);
    }

    @Override
    public int getVisDiscount(ItemStack stack, @Nullable Player player, @Nullable Aspect aspect) {
        return 5;
    }

    @Override
    public boolean showIngamePopups(ItemStack stack, LivingEntity wearer) {
        return getType() == Type.HELMET;
    }

    @Override
    public boolean showNodes(ItemStack stack, LivingEntity wearer) {
        return getType() == Type.HELMET;
    }

    @Override
    public int getRunicCharge(ItemStack stack) {
        return 0;
    }

    @Override
    public int getWarp(ItemStack stack, Player player) {
        return 2;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tc.visdiscount").append(": 5%").withStyle(ChatFormatting.DARK_PURPLE));
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public @NotNull HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot,
                                                                    HumanoidModel<?> original) {
                return thaumcraft.client.render.VoidRobeArmorModel.get(slot == EquipmentSlot.CHEST);
            }
        });
    }

    @Mod.EventBusSubscriber(modid = Thaumcraft.MODID)
    public static final class VoidRobeEvents {
        private VoidRobeEvents() {
        }

        /** ISpecialArmor оригинала: от магии мантия защищает на защиту/35 за каждую часть (обычная броня от магии не спасает). */
        @SubscribeEvent
        public static void hurt(LivingHurtEvent event) {
            DamageSource source = event.getSource();
            if (!source.is(DamageTypeTags.WITCH_RESISTANT_TO) || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                return;
            }
            double ratio = 0.0;
            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS}) {
                if (event.getEntity().getItemBySlot(slot).getItem() instanceof VoidRobeArmorItem armor) {
                    ratio += armor.getDefense() / 35.0;
                }
            }
            if (ratio > 0.0) {
                event.setAmount((float) (event.getAmount() * (1.0 - Math.min(1.0, ratio))));
            }
        }
    }
}
