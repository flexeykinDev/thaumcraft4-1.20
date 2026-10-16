package thaumcraft.common.items.wands.foci;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.wands.FocusUpgrade;
import thaumcraft.common.entity.PrimalOrbEntity;
import thaumcraft.common.items.wands.FocusItem;
import thaumcraft.common.items.wands.WandItem;
import thaumcraft.common.registry.ModSounds;

import java.util.Random;

/**
 * Первозданный фокус (ItemFocusPrimal оригинала): выпускает медленную взрывную сферу чистой магии.
 * Цена непостоянна: каждую пятую долю секунды на каждый из шести аспектов выпадает от 0.5 до 2.5 вис.
 */
public class PrimalFocus extends FocusItem {
    private static final ResourceLocation DEPTH = Thaumcraft.id("textures/unstitched/item/focus_primal_depth.png");

    @Override
    protected String sortingPrefix() {
        return "FP";
    }

    @Override
    public int getFocusColor(ItemStack focus) {
        return 0xa5a1c1;
    }

    @Override
    public ResourceLocation getDepthTexture(ItemStack focus) {
        return DEPTH;
    }

    @Override
    public int getDepthFrames(ItemStack focus) {
        return 30;
    }

    @Override
    public AspectList getVisCost(ItemStack focus) {
        Random random = new Random(System.currentTimeMillis() / 200L);
        return new AspectList().add(Aspect.WATER, 50 + random.nextInt(5) * 50).add(Aspect.AIR, 50 + random.nextInt(5) * 50)
                .add(Aspect.EARTH, 50 + random.nextInt(5) * 50).add(Aspect.FIRE, 50 + random.nextInt(5) * 50)
                .add(Aspect.ORDER, 50 + random.nextInt(5) * 50).add(Aspect.ENTROPY, 50 + random.nextInt(5) * 50);
    }

    @Override
    public int getActivationCooldown(ItemStack focus) {
        return 500;
    }

    @Override
    public boolean onFocusRightClick(ItemStack wand, Level level, Player player, InteractionHand hand) {
        ItemStack focus = WandItem.getFocusStack(wand);
        if (!level.isClientSide && WandItem.consumeAllCentiVis(wand, player, getVisCost(focus), true)) {
            PrimalOrbEntity orb = new PrimalOrbEntity(level, player, isUpgradedWith(focus, FocusUpgrade.SEEKER));
            level.addFreshEntity(orb);
            level.playSound(null, orb, ModSounds.ICE.get(), SoundSource.PLAYERS, 0.3f, 0.8f + level.random.nextFloat() * 0.1f);
        }
        player.swing(hand);
        return true;
    }

    @Override
    public FocusUpgrade[] getPossibleUpgradesByRank(ItemStack focus, int rank) {
        return switch (rank) {
            case 1, 2, 4, 5 -> new FocusUpgrade[]{FocusUpgrade.FRUGAL};
            case 3 -> new FocusUpgrade[]{FocusUpgrade.FRUGAL, FocusUpgrade.SEEKER};
            default -> new FocusUpgrade[0];
        };
    }
}
