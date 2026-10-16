package thaumcraft.common.items.armor;

import net.minecraft.ChatFormatting;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.RunicArmor;
import thaumcraft.api.VisDiscountGear;
import thaumcraft.api.WarpingGear;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.registry.ModMaterials;

import java.util.List;
import java.util.function.Consumer;

/**
 * Облачение Багрового культа — перенос ItemCultistRobeArmor, ItemCultistPlateArmor, ItemCultistLeaderArmor и
 * ItemCultistBoots оригинала. Ряса клирика (железная броня, скидка 1% и единица искажения за часть), латы рыцаря
 * (железная броня), латы предводителя (как крепостная тауминиевая) и сапоги культиста (скидка 1%, искажение 1).
 * Чинятся железом; надеваются как рунная броня.
 */
public class CultistArmorItem extends ArmorItem implements RunicArmor, VisDiscountGear, WarpingGear {
    public enum Kind { ROBE, PLATE, LEADER, BOOTS }

    private final Kind kind;

    public CultistArmorItem(Kind kind, Type type) {
        super(kind == Kind.LEADER ? ModMaterials.FORTRESS_ARMOR : ArmorMaterials.IRON, type,
                new Item.Properties().rarity(kind == Kind.LEADER ? Rarity.RARE : Rarity.UNCOMMON));
        this.kind = kind;
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return switch (kind) {
            case ROBE -> "thaumcraft:textures/models/cultist_robe_armor.png";
            case PLATE -> entity instanceof thaumcraft.common.entity.InhabitedZombie ? "thaumcraft:textures/models/zombie_plate_armor.png"
                    : "thaumcraft:textures/models/cultist_plate_armor.png";
            case LEADER -> "thaumcraft:textures/models/cultist_leader_armor.png";
            case BOOTS -> "thaumcraft:textures/models/cultistboots.png";
        };
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        return repair.is(Items.IRON_INGOT) || super.isValidRepairItem(stack, repair);
    }

    @Override
    public int getRunicCharge(ItemStack stack) {
        return 0;
    }

    @Override
    public int getVisDiscount(ItemStack stack, @Nullable Player player, @Nullable Aspect aspect) {
        return kind == Kind.ROBE || kind == Kind.BOOTS ? 1 : 0;
    }

    @Override
    public int getWarp(ItemStack stack, Player player) {
        return kind == Kind.ROBE || kind == Kind.BOOTS ? 1 : 0;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (kind == Kind.ROBE || kind == Kind.BOOTS) {
            tooltip.add(Component.translatable("tc.visdiscount").append(": 1%").withStyle(ChatFormatting.DARK_PURPLE));
        }
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        if (kind == Kind.BOOTS) {
            return;
        }
        consumer.accept(new IClientItemExtensions() {
            @Override
            public @NotNull HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot,
                                                                    HumanoidModel<?> original) {
                boolean chest = slot == EquipmentSlot.CHEST;
                return switch (kind) {
                    case ROBE -> thaumcraft.client.render.VoidRobeArmorModel.get(chest);
                    case PLATE -> thaumcraft.client.render.CultistKnightArmorModel.get(chest);
                    default -> thaumcraft.client.render.CultistLeaderArmorModel.get(chest);
                };
            }
        });
    }
}
