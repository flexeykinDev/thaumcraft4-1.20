package thaumcraft.common.items;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import thaumcraft.common.champion.LootBags;
import thaumcraft.common.registry.ModSounds;

import java.util.List;

/** Мешок с добычей (ItemLootBag оригинала): щелчок высыпает под ноги от 8 до 12 случайных предметов. */
public class LootBagItem extends Item {
    private final int tier;

    /** @param tier 0 — обычный, 1 — необычный, 2 — редкий */
    public LootBagItem(int tier) {
        super(new Item.Properties().rarity(tier == 2 ? Rarity.RARE : tier == 1 ? Rarity.UNCOMMON : Rarity.COMMON));
        this.tier = tier;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            int count = 8 + level.random.nextInt(5);
            for (int i = 0; i < count; i++) {
                ItemStack loot = LootBags.generate(tier, level.random);
                if (!loot.isEmpty()) {
                    level.addFreshEntity(new ItemEntity(level, player.getX(), player.getY(), player.getZ(), loot));
                }
            }
            level.playSound(null, player, ModSounds.COINS.get(), SoundSource.PLAYERS, 0.75f, 1f);
        }
        stack.shrink(1);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tc.lootbag"));
    }
}
