package thaumcraft.common.items.wands.foci;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.wands.FocusUpgrade;
import thaumcraft.common.entity.FrostShardEntity;
import thaumcraft.common.items.wands.FocusItem;
import thaumcraft.common.items.wands.WandItem;
import thaumcraft.common.registry.ModSounds;

/**
 * Фокус мороза (ItemFocusFrost оригинала): каждое нажатие выпускает ледяной осколок на 3 урона, который
 * рикошетит от стен. «Картечь» даёт веер хрупких осколков, «Ледяная глыба» — тяжёлый прыгучий снаряд.
 */
public class FrostFocus extends FocusItem {
    private static final AspectList COST_BASE = new AspectList().add(Aspect.WATER, 5).add(Aspect.FIRE, 2).add(Aspect.ENTROPY, 2);
    private static final AspectList COST_SCATTER = new AspectList().add(Aspect.WATER, 20).add(Aspect.FIRE, 2)
            .add(Aspect.ENTROPY, 2).add(Aspect.AIR, 5);
    private static final AspectList COST_BOULDER = new AspectList().add(Aspect.WATER, 20).add(Aspect.FIRE, 2)
            .add(Aspect.ENTROPY, 2).add(Aspect.EARTH, 5);

    @Override
    protected String sortingPrefix() {
        return "BF";
    }

    @Override
    public int getFocusColor(ItemStack focus) {
        return 0x4f69cc;
    }

    @Override
    public AspectList getVisCost(ItemStack focus) {
        return isUpgradedWith(focus, FocusUpgrade.SCATTERSHOT) ? COST_SCATTER
                : isUpgradedWith(focus, FocusUpgrade.ICEBOULDER) ? COST_BOULDER : COST_BASE;
    }

    @Override
    public int getActivationCooldown(ItemStack focus) {
        return isUpgradedWith(focus, FocusUpgrade.SCATTERSHOT) || isUpgradedWith(focus, FocusUpgrade.ICEBOULDER) ? 500 : 200;
    }

    @Override
    public boolean onFocusRightClick(ItemStack wand, Level level, Player player, InteractionHand hand) {
        ItemStack focus = WandItem.getFocusStack(wand);
        if (!level.isClientSide && WandItem.consumeAllCentiVis(wand, player, getVisCost(focus), true)) {
            int frosty = getUpgradeLevel(focus, FocusUpgrade.ALCHEMISTS_FROST);
            int potency = WandItem.getFocusPotency(wand);
            FrostShardEntity shard;
            if (isUpgradedWith(focus, FocusUpgrade.SCATTERSHOT)) {
                shard = null;
                for (int i = 0; i < 5 + potency * 2; i++) {
                    shard = new FrostShardEntity(level, player, 8f);
                    shard.setDamage(1f);
                    shard.fragile = true;
                    shard.setFrosty(frosty);
                    level.addFreshEntity(shard);
                }
            } else if (isUpgradedWith(focus, FocusUpgrade.ICEBOULDER)) {
                shard = new FrostShardEntity(level, player, 1f);
                shard.setDamage(4 + potency * 2);
                shard.bounce = 0.8;
                shard.bounceLimit = 6;
                shard.setFrosty(frosty);
                level.addFreshEntity(shard);
            } else {
                shard = new FrostShardEntity(level, player, 1f);
                shard.setDamage(3f + potency * 1.5f);
                shard.setFrosty(frosty);
                level.addFreshEntity(shard);
            }
            level.playSound(null, shard, ModSounds.ICE.get(), SoundSource.PLAYERS, 0.4f, 1f + level.random.nextFloat() * 0.1f);
        }
        player.swing(hand);
        return true;
    }

    @Override
    public FocusUpgrade[] getPossibleUpgradesByRank(ItemStack focus, int rank) {
        return switch (rank) {
            case 1, 5 -> new FocusUpgrade[]{FocusUpgrade.FRUGAL, FocusUpgrade.POTENCY, FocusUpgrade.ALCHEMISTS_FROST};
            case 2, 4 -> new FocusUpgrade[]{FocusUpgrade.FRUGAL, FocusUpgrade.POTENCY};
            case 3 -> new FocusUpgrade[]{FocusUpgrade.FRUGAL, FocusUpgrade.POTENCY, FocusUpgrade.SCATTERSHOT, FocusUpgrade.ICEBOULDER,
                    FocusUpgrade.ALCHEMISTS_FROST};
            default -> new FocusUpgrade[0];
        };
    }
}
