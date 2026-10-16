package thaumcraft.common.items.armor;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import thaumcraft.api.WarpingGear;
import thaumcraft.common.items.tools.VoidGear;
import thaumcraft.common.registry.ModMaterials;

/** Пустотная броня (ItemVoidArmor оригинала): сама чинится раз в секунду и даёт единицу искажения. */
public class VoidArmorItem extends ArmorItem implements WarpingGear {
    public VoidArmorItem(Type type) {
        super(ModMaterials.VOID_ARMOR, type, new Item.Properties().rarity(Rarity.UNCOMMON));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        VoidGear.selfRepair(stack, level, entity);
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return "thaumcraft:textures/models/" + (getType() == Type.LEGGINGS ? "void_2.png" : "void_1.png");
    }

    @Override
    public int getWarp(ItemStack stack, Player player) {
        return 1;
    }
}
