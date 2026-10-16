package thaumcraft.common.items.golem;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Улучшение голема (ItemGolemUpgrade оригинала) — по одному на первичный аспект: воздух — скорость, земля — сила,
 * огонь — урон и вместимость, вода — чутьё и дальность, порядок — ум, энтропия — защита. Одинаковых в голема входит не больше двух.
 */
public class GolemUpgradeItem extends Item {
    public static final String[] NAMES = {"air", "earth", "fire", "water", "order", "entropy"};

    private final int upgrade;

    public GolemUpgradeItem(int upgrade) {
        super(new Item.Properties().rarity(Rarity.UNCOMMON));
        this.upgrade = upgrade;
    }

    public int getUpgrade() {
        return upgrade;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.ItemGolemUpgrade." + upgrade + ".desc").withStyle(ChatFormatting.GRAY));
    }
}
