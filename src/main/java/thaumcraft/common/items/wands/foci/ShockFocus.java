package thaumcraft.common.items.wands.foci;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.wands.FocusUpgrade;
import thaumcraft.common.items.wands.FocusItem;
import thaumcraft.common.items.wands.FocusManager;
import thaumcraft.common.items.wands.WandItem;
import thaumcraft.common.network.FxPacket;
import thaumcraft.common.registry.ModSounds;

import java.util.ArrayList;
import java.util.List;

/**
 * Фокус разряда (ItemFocusShock оригинала): пока зажата кнопка, четыре раза в секунду бьёт молнией туда,
 * куда смотрит игрок, — до 20 блоков, 4 урона. «Цепная молния» перескакивает на соседние цели.
 */
public class ShockFocus extends FocusItem {
    private static final AspectList COST_BASE = new AspectList().add(Aspect.AIR, 25);
    private static final AspectList COST_CHAIN = new AspectList().add(Aspect.AIR, 40).add(Aspect.WATER, 10);
    private static final AspectList COST_GROUND = new AspectList().add(Aspect.AIR, 75).add(Aspect.EARTH, 25);

    @Override
    protected String sortingPrefix() {
        return "BL";
    }

    @Override
    public Animation getAnimation(ItemStack focus) {
        return isUpgradedWith(focus, FocusUpgrade.EARTH_SHOCK) ? Animation.WAVE : Animation.CHARGE;
    }

    @Override
    public int getFocusColor(ItemStack focus) {
        return 0x9fb3ff;
    }

    @Override
    public AspectList getVisCost(ItemStack focus) {
        return isUpgradedWith(focus, FocusUpgrade.CHAIN_LIGHTNING) ? COST_CHAIN
                : isUpgradedWith(focus, FocusUpgrade.EARTH_SHOCK) ? COST_GROUND : COST_BASE;
    }

    @Override
    public int getActivationCooldown(ItemStack focus) {
        return isUpgradedWith(focus, FocusUpgrade.CHAIN_LIGHTNING) ? 500 : isUpgradedWith(focus, FocusUpgrade.EARTH_SHOCK) ? 1000 : 250;
    }

    @Override
    public boolean onFocusRightClick(ItemStack wand, Level level, Player player, InteractionHand hand) {
        ItemStack focus = WandItem.getFocusStack(wand);
        if (isUpgradedWith(focus, FocusUpgrade.EARTH_SHOCK)) {
            // «Земной разряд»: шар, который бьёт всех вокруг места удара и электризует землю
            if (WandItem.consumeAllCentiVis(wand, player, getVisCost(focus), !level.isClientSide)) {
                if (!level.isClientSide) {
                    thaumcraft.common.entity.FocusOrbEntity orb = new thaumcraft.common.entity.FocusOrbEntity(level, player, true);
                    orb.area += getUpgradeLevel(focus, FocusUpgrade.ENLARGE) * 2;
                    orb.damage = (int) (orb.damage + WandItem.getFocusPotency(wand) * 1.33);
                    level.addFreshEntity(orb);
                    level.playSound(null, orb, ModSounds.ZAP.get(), SoundSource.PLAYERS, 1f,
                            1f + (level.random.nextFloat() - level.random.nextFloat()) * 0.2f);
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
        Level level = player.level();
        if (!WandItem.consumeAllCentiVis(wand, player, getVisCost(focus), !level.isClientSide)) {
            player.stopUsingItem();
            return;
        }
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        int potency = WandItem.getFocusPotency(wand);
        Entity pointed = FocusManager.getPointedEntity(player, 20, 1.1);
        Vec3 target = pointed != null ? pointed.getBoundingBox().getCenter() : FocusManager.lookTarget(player, 20);
        // молния и искры в месте удара видны всем вокруг
        FxPacket.send(server, FxPacket.BOLT, FocusManager.wandTip(player), target, 2, 1);
        FxPacket.send(server, FxPacket.SPARKLES, target, Vec3.ZERO, 2, pointed != null ? 60 : 30);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.SHOCK.get(), SoundSource.PLAYERS, 0.25f, 1f);
        if (!(pointed instanceof LivingEntity victim) || victim instanceof Player && !server.getServer().isPvpAllowed()) {
            return;
        }
        int chain = getUpgradeLevel(focus, FocusUpgrade.CHAIN_LIGHTNING) * 2;
        victim.hurt(player.damageSources().playerAttack(player), (chain > 0 ? 6 : 4) + potency);
        if (chain > 0) {
            chain += getUpgradeLevel(focus, FocusUpgrade.ENLARGE) * 2;
            LivingEntity center = victim;
            List<Integer> struck = new ArrayList<>();
            struck.add(victim.getId());
            while (chain-- > 0) {
                LivingEntity closest = null;
                double best = Double.MAX_VALUE;
                for (LivingEntity near : level.getEntitiesOfClass(LivingEntity.class, center.getBoundingBox().inflate(8),
                        e -> e != player && e.isAlive())) {
                    if (struck.contains(near.getId()) || near instanceof Player && !server.getServer().isPvpAllowed()) {
                        continue;
                    }
                    double distance = near.distanceToSqr(center);
                    if (distance < best) {
                        closest = near;
                        best = distance;
                    }
                }
                if (closest != null) {
                    FxPacket.send(server, FxPacket.BOLT, center.getBoundingBox().getCenter(), closest.getBoundingBox().getCenter(), 2, 1);
                    struck.add(closest.getId());
                    closest.hurt(player.damageSources().playerAttack(player), 4 + potency);
                    center = closest;
                }
            }
        }
    }

    @Override
    public boolean canApplyUpgrade(ItemStack focus, Player player, FocusUpgrade type, int rank) {
        return type != FocusUpgrade.ENLARGE || isUpgradedWith(focus, FocusUpgrade.CHAIN_LIGHTNING)
                || isUpgradedWith(focus, FocusUpgrade.EARTH_SHOCK);
    }

    @Override
    public FocusUpgrade[] getPossibleUpgradesByRank(ItemStack focus, int rank) {
        return switch (rank) {
            case 1, 2 -> new FocusUpgrade[]{FocusUpgrade.FRUGAL, FocusUpgrade.POTENCY};
            case 3 -> new FocusUpgrade[]{FocusUpgrade.FRUGAL, FocusUpgrade.POTENCY, FocusUpgrade.CHAIN_LIGHTNING, FocusUpgrade.EARTH_SHOCK};
            case 4, 5 -> new FocusUpgrade[]{FocusUpgrade.FRUGAL, FocusUpgrade.POTENCY, FocusUpgrade.ENLARGE};
            default -> new FocusUpgrade[0];
        };
    }
}
