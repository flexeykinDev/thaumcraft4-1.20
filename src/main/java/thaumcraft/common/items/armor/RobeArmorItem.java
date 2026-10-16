package thaumcraft.common.items.armor;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.VisDiscountGear;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.registry.ModItems;
import thaumcraft.common.registry.ModMaterials;

import java.util.List;

/**
 * Мантия, штаны и сапоги из зачарованной ткани — перенос ItemRobeArmor оригинала. Защищают слабо, зато удешевляют
 * заклинания жезла (2% за мантию и штаны, 1% за сапоги). Красятся, как кожаная броня, и отмываются в котле.
 */
public class RobeArmorItem extends ArmorItem implements DyeableLeatherItem, VisDiscountGear {
    /** Цвет некрашеной мантии — лиловый. */
    public static final int DEFAULT_COLOR = 6961280;

    public RobeArmorItem(Type type) {
        super(ModMaterials.SPECIAL_ARMOR, type, new Item.Properties().rarity(Rarity.UNCOMMON));
    }

    @Override
    public int getColor(ItemStack stack) {
        var display = stack.getTagElement("display");
        return display != null && display.contains("color", 99) ? display.getInt("color") : DEFAULT_COLOR;
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        String base = getType() == Type.LEGGINGS ? "robes_2" : "robes_1";
        return "thaumcraft:textures/models/" + base + ("overlay".equals(type) ? "_overlay" : "") + ".png";
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        return repair.is(ModItems.ENCHANTED_FABRIC.get()) || super.isValidRepairItem(stack, repair);
    }

    @Override
    public int getVisDiscount(ItemStack stack, @Nullable Player player, @Nullable Aspect aspect) {
        return getType() == Type.BOOTS ? 1 : 2;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tc.visdiscount").append(": " + getVisDiscount(stack, null, null) + "%")
                .withStyle(ChatFormatting.DARK_PURPLE));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
