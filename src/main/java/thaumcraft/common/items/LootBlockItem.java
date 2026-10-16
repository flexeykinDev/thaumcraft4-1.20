package thaumcraft.common.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import thaumcraft.common.blocks.LootBlock;

import java.util.List;

/** BlockLootItem оригинала: урна и ящик подписаны своей редкостью. */
public class LootBlockItem extends BlockItem {
    public LootBlockItem(LootBlock block) {
        super(block, new Properties().rarity(block.tier == 2 ? Rarity.RARE : block.tier == 1 ? Rarity.UNCOMMON : Rarity.COMMON));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tc.rarity." + ((LootBlock) getBlock()).tier).withStyle(ChatFormatting.GRAY));
    }
}
