package thaumictinkerer.common.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.Goggles;
import thaumcraft.api.VisDiscountGear;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.nodes.Revealer;
import thaumcraft.common.registry.ModItems;
import thaumcraft.common.registry.ModMaterials;

import java.util.List;

/**
 * Шлем откровения (ItemRevealingHelm оригинала): таумиевый шлем, в который встроены очки откровения — видны узлы
 * ауры и содержимое сосудов, заклинания на 5% дешевле. Чинится таумием.
 */
public class RevealingHelmItem extends ArmorItem implements Revealer, Goggles, VisDiscountGear {
    public RevealingHelmItem() {
        super(ModMaterials.THAUMIUM_ARMOR, Type.HELMET, new Item.Properties().durability(500).rarity(Rarity.UNCOMMON));
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return "thaumictinkerer:textures/models/armor/revealing_layer_1.png";
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        return repair.is(ModItems.THAUMIUM_INGOT.get()) || super.isValidRepairItem(stack, repair);
    }

    @Override
    public boolean showNodes(ItemStack stack, LivingEntity wearer) {
        return true;
    }

    @Override
    public boolean showIngamePopups(ItemStack stack, LivingEntity wearer) {
        return true;
    }

    @Override
    public int getVisDiscount(ItemStack stack, @Nullable Player player, @Nullable Aspect aspect) {
        return 5;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tc.visdiscount").append(": 5%").withStyle(ChatFormatting.DARK_PURPLE));
    }
}
