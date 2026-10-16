package thaumcraft.common.items.armor;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.Goggles;
import thaumcraft.api.VisDiscountGear;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.nodes.Revealer;
import thaumcraft.common.registry.ModMaterials;

import java.util.List;

/**
 * Очки откровения (ItemGoggles оригинала): шлем, сквозь который видны узлы ауры с 64 блоков и лучи сети вис,
 * а над сосудами с эссенцией появляются значки содержимого. Заклинания жезла дешевле на 5%.
 */
public class GogglesItem extends ArmorItem implements VisDiscountGear, Revealer, Goggles {
    public GogglesItem() {
        super(ModMaterials.SPECIAL_ARMOR, Type.HELMET, new Item.Properties().durability(350).rarity(Rarity.RARE));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tc.visdiscount").append(": " + getVisDiscount(stack, null, null) + "%")
                .withStyle(ChatFormatting.DARK_PURPLE));
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return "thaumcraft:textures/models/goggles.png";
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        return repair.is(Items.GOLD_INGOT) || super.isValidRepairItem(stack, repair);
    }

    @Override
    public int getVisDiscount(ItemStack stack, @Nullable Player player, @Nullable Aspect aspect) {
        return 5;
    }

    @Override
    public boolean showNodes(ItemStack stack, LivingEntity wearer) {
        return true;
    }

    @Override
    public boolean showIngamePopups(ItemStack stack, LivingEntity wearer) {
        return true;
    }
}
