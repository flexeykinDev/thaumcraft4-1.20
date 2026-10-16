package thaumcraft.common.items;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import thaumcraft.api.RunicArmor;
import thaumcraft.common.runic.RunicShield;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/**
 * Украшения рунной защиты (ItemRingRunic, ItemAmuletRunic, ItemGirdleRunic оригинала): каждое прибавляет к рунному
 * щиту владельца свои единицы, а улучшенные дают ещё и особое свойство, срабатывающее, когда щит пробит.
 */
public class RunicBaubleItem extends Item implements RunicArmor, ICurioItem {
    /** Особое свойство улучшенного украшения. */
    public enum Kind {
        PLAIN,
        /** Заряженное кольцо: щит восстанавливается быстрее. */
        CHARGED,
        /** Живительное кольцо: пробитый щит даёт регенерацию. */
        HEALING,
        /** Амулет спасения: пробитый щит раз в минуту мгновенно восстанавливается. */
        EMERGENCY,
        /** Кинетический пояс: пробитый щит взрывается, отбрасывая врагов. */
        KINETIC
    }

    private final int charge;
    private final Kind kind;

    public RunicBaubleItem(int charge, Kind kind, Rarity rarity) {
        super(new Item.Properties().stacksTo(1).rarity(rarity));
        this.charge = charge;
        this.kind = kind;
    }

    public Kind getKind() {
        return kind;
    }

    @Override
    public int getRunicCharge(ItemStack stack) {
        return charge;
    }

    @Override
    public void onEquip(SlotContext context, ItemStack previous, ItemStack stack) {
        RunicShield.markDirty(context.entity());
    }

    @Override
    public void onUnequip(SlotContext context, ItemStack next, ItemStack stack) {
        RunicShield.markDirty(context.entity());
    }
}
