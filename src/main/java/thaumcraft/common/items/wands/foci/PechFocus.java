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
import thaumcraft.common.entity.PechBlastEntity;
import thaumcraft.common.items.wands.FocusItem;
import thaumcraft.common.items.wands.WandItem;
import thaumcraft.common.registry.ModSounds;

/**
 * Фокус «Проклятие печа» (ItemFocusPech оригинала): бросает сгусток, который ранит на 2 и травит,
 * замедляет или ослабляет всех рядом с местом удара. Рецепта нет — фокус выменивают у печей.
 */
public class PechFocus extends FocusItem {
    private static final AspectList COST = new AspectList().add(Aspect.EARTH, 10).add(Aspect.ENTROPY, 10).add(Aspect.WATER, 10);
    private static final AspectList COST_ALL = new AspectList().add(Aspect.AIR, 10).add(Aspect.FIRE, 10).add(Aspect.EARTH, 10)
            .add(Aspect.ORDER, 10).add(Aspect.ENTROPY, 10).add(Aspect.WATER, 10);
    private static final ResourceLocation DEPTH = Thaumcraft.id("textures/unstitched/item/focus_pech_depth.png");

    @Override
    protected String sortingPrefix() {
        return "PP";
    }

    @Override
    public int getFocusColor(ItemStack focus) {
        return 0x229944;
    }

    @Override
    public ResourceLocation getDepthTexture(ItemStack focus) {
        return DEPTH;
    }

    @Override
    public int getDepthFrames(ItemStack focus) {
        return 9;
    }

    @Override
    public AspectList getVisCost(ItemStack focus) {
        return isUpgradedWith(focus, FocusUpgrade.NIGHTSHADE) ? COST_ALL : COST;
    }

    @Override
    public int getActivationCooldown(ItemStack focus) {
        return 250;
    }

    @Override
    public boolean onFocusRightClick(ItemStack wand, Level level, Player player, InteractionHand hand) {
        ItemStack focus = WandItem.getFocusStack(wand);
        if (!level.isClientSide && WandItem.consumeAllCentiVis(wand, player, getVisCost(focus), true)) {
            PechBlastEntity blast = new PechBlastEntity(level, player, WandItem.getFocusPotency(wand),
                    getUpgradeLevel(focus, FocusUpgrade.EXTEND), isUpgradedWith(focus, FocusUpgrade.NIGHTSHADE));
            level.addFreshEntity(blast);
            level.playSound(null, blast, ModSounds.ICE.get(), SoundSource.PLAYERS, 0.4f, 1f + level.random.nextFloat() * 0.1f);
        }
        player.swing(hand);
        return true;
    }

    @Override
    public FocusUpgrade[] getPossibleUpgradesByRank(ItemStack focus, int rank) {
        return switch (rank) {
            case 1, 3 -> new FocusUpgrade[]{FocusUpgrade.FRUGAL, FocusUpgrade.POTENCY};
            case 2, 4 -> new FocusUpgrade[]{FocusUpgrade.FRUGAL, FocusUpgrade.POTENCY, FocusUpgrade.EXTEND};
            case 5 -> new FocusUpgrade[]{FocusUpgrade.FRUGAL, FocusUpgrade.POTENCY, FocusUpgrade.NIGHTSHADE};
            default -> new FocusUpgrade[0];
        };
    }
}
