package thaumcraft.common.items.wands.foci;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.wands.FocusUpgrade;
import thaumcraft.common.entity.EmberEntity;
import thaumcraft.common.items.wands.FocusItem;
import thaumcraft.common.items.wands.FocusManager;
import thaumcraft.common.items.wands.WandItem;
import thaumcraft.common.registry.ModSounds;

/**
 * Фокус огня (ItemFocusFire оригинала): пока зажата кнопка, жезл извергает широкую струю пламени —
 * по два уголька за тик, каждый бьёт на 2 и поджигает. Улучшение «Огненный луч» сужает струю и усиливает её.
 */
public class FireFocus extends FocusItem {
    private static final AspectList COST_BASE = new AspectList().add(Aspect.FIRE, 10);
    private static final AspectList COST_BEAM = new AspectList().add(Aspect.FIRE, 10).add(Aspect.ORDER, 3);
    private static final AspectList COST_BALL = new AspectList().add(Aspect.FIRE, 66).add(Aspect.ENTROPY, 33);

    @Override
    protected String sortingPrefix() {
        return "AF";
    }

    @Override
    public Animation getAnimation(ItemStack focus) {
        return isUpgradedWith(focus, FocusUpgrade.FIREBALL) ? Animation.WAVE : Animation.CHARGE;
    }

    @Override
    public int getFocusColor(ItemStack focus) {
        return 0xe55104;
    }

    @Override
    public AspectList getVisCost(ItemStack focus) {
        return isUpgradedWith(focus, FocusUpgrade.FIREBEAM) ? COST_BEAM : isUpgradedWith(focus, FocusUpgrade.FIREBALL) ? COST_BALL : COST_BASE;
    }

    @Override
    public int getActivationCooldown(ItemStack focus) {
        return isUpgradedWith(focus, FocusUpgrade.FIREBALL) ? 1000 : 0;
    }

    @Override
    public boolean isVisCostPerTick(ItemStack focus) {
        return true;
    }

    @Override
    public boolean onFocusRightClick(ItemStack wand, Level level, Player player, InteractionHand hand) {
        ItemStack focus = WandItem.getFocusStack(wand);
        if (isUpgradedWith(focus, FocusUpgrade.FIREBALL)) {
            // «Огненный шар»: одно нажатие — один взрывной шар
            if (WandItem.consumeAllCentiVis(wand, player, getVisCost(focus), !level.isClientSide)) {
                if (!level.isClientSide) {
                    thaumcraft.common.entity.FocusOrbEntity orb = new thaumcraft.common.entity.FocusOrbEntity(level, player, false);
                    orb.strength += WandItem.getFocusPotency(wand) * 0.4f;
                    orb.onFire = isUpgradedWith(focus, FocusUpgrade.ALCHEMISTS_FIRE);
                    level.addFreshEntity(orb);
                    level.playSound(null, player, net.minecraft.sounds.SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1f, 1f);
                }
                player.swing(hand);
            }
            return true;
        }
        player.startUsingItem(hand);
        FocusManager.setCooldown(player, -1);
        return true;
    }

    @Override
    public void onUsingFocusTick(ItemStack wand, Player player, int count) {
        ItemStack focus = WandItem.getFocusStack(wand);
        AspectList cost = getVisCost(focus);
        if (!WandItem.consumeAllCentiVis(wand, player, cost, false)) {
            player.stopUsingItem();
            return;
        }
        Level level = player.level();
        if (level.isClientSide) {
            return;
        }
        if (count % 10 == 0) {
            level.playSound(null, player, ModSounds.FIRE_LOOP.get(), SoundSource.PLAYERS, 0.33f, 2f);
        }
        if (WandItem.consumeAllCentiVis(wand, player, cost, true)) {
            boolean beam = isUpgradedWith(focus, FocusUpgrade.FIREBEAM);
            int potency = WandItem.getFocusPotency(wand);
            for (int i = 0; i < 2 + potency; i++) {
                EmberEntity ember = new EmberEntity(level, player, beam ? 0.25f : 15f);
                ember.damage = 2 + potency;
                if (beam) {
                    ember.damage += 0.5f;
                    ember.damage *= 1.5f;
                    ember.setDuration(30);
                }
                ember.firey = getUpgradeLevel(focus, FocusUpgrade.ALCHEMISTS_FIRE);
                ember.setPos(ember.position().add(ember.getDeltaMovement()));
                level.addFreshEntity(ember);
            }
        }
    }

    @Override
    public FocusUpgrade[] getPossibleUpgradesByRank(ItemStack focus, int rank) {
        return switch (rank) {
            case 1, 5 -> new FocusUpgrade[]{FocusUpgrade.FRUGAL, FocusUpgrade.POTENCY};
            case 2, 4 -> new FocusUpgrade[]{FocusUpgrade.FRUGAL, FocusUpgrade.POTENCY, FocusUpgrade.ALCHEMISTS_FIRE};
            case 3 -> new FocusUpgrade[]{FocusUpgrade.FRUGAL, FocusUpgrade.POTENCY, FocusUpgrade.FIREBALL, FocusUpgrade.FIREBEAM};
            default -> new FocusUpgrade[0];
        };
    }

    @Override
    public boolean canApplyUpgrade(ItemStack focus, Player player, FocusUpgrade type, int rank) {
        return type != FocusUpgrade.ALCHEMISTS_FIRE || !isUpgradedWith(focus, FocusUpgrade.FIREBALL)
                || !isUpgradedWith(focus, FocusUpgrade.ALCHEMISTS_FIRE);
    }
}
