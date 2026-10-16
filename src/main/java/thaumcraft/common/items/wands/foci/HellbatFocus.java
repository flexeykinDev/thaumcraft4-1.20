package thaumcraft.common.items.wands.foci;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.wands.FocusUpgrade;
import thaumcraft.common.entity.FireBat;
import thaumcraft.common.items.wands.FocusItem;
import thaumcraft.common.items.wands.FocusManager;
import thaumcraft.common.items.wands.WandItem;
import thaumcraft.common.registry.ModEntities;
import thaumcraft.common.registry.ModSounds;

/**
 * Фокус «Девять кругов ада» (ItemFocusHellbat оригинала): призывает огненную летучую мышь, которая летит
 * к существу, на которое смотрит игрок (до 32 блоков), кусает и поджигает его, а иногда взрывается.
 */
public class HellbatFocus extends FocusItem {
    private static final AspectList COST_BASE = new AspectList().add(Aspect.FIRE, 200).add(Aspect.ENTROPY, 100).add(Aspect.AIR, 100);
    private static final AspectList COST_BOMB = new AspectList().add(Aspect.FIRE, 100).add(Aspect.ENTROPY, 200).add(Aspect.AIR, 100);
    private static final AspectList COST_DEVIL = new AspectList().add(Aspect.FIRE, 100).add(Aspect.ENTROPY, 100).add(Aspect.AIR, 100)
            .add(Aspect.EARTH, 100);
    private static final ResourceLocation ORNAMENT = Thaumcraft.id("textures/item/focus_hellbat_orn.png");

    @Override
    protected String sortingPrefix() {
        return "HH";
    }

    @Override
    public int getFocusColor(ItemStack focus) {
        return 0xdc3602;
    }

    @Override
    public ResourceLocation getOrnament(ItemStack focus) {
        return ORNAMENT;
    }

    @Override
    public AspectList getVisCost(ItemStack focus) {
        return isUpgradedWith(focus, FocusUpgrade.BAT_BOMBS) ? COST_BOMB : isUpgradedWith(focus, FocusUpgrade.DEVIL_BATS) ? COST_DEVIL : COST_BASE;
    }

    @Override
    public int getActivationCooldown(ItemStack focus) {
        return 1000;
    }

    @Override
    public boolean onFocusRightClick(ItemStack wand, Level level, Player player, InteractionHand hand) {
        Entity pointed = FocusManager.getPointedEntity(player, 32, 1.1);
        if (!(pointed instanceof LivingEntity target) || pointed instanceof FireBat) {
            return false;
        }
        if (level instanceof ServerLevel server) {
            if (target instanceof Player && !server.getServer().isPvpAllowed()) {
                return true;
            }
            ItemStack focus = WandItem.getFocusStack(wand);
            float yaw = player.getYRot() * Mth.DEG_TO_RAD;
            Vec3 look = player.getLookAngle();
            FireBat bat = new FireBat(ModEntities.FIRE_BAT.get(), level);
            bat.moveTo(player.getX() - Mth.cos(yaw) * 0.16f + look.x * 0.5,
                    player.getY() + player.getBbHeight() / 2f + 0.2 + look.y * 0.5 + bat.getBbHeight(),
                    player.getZ() - Mth.sin(yaw) * 0.16f + look.z * 0.5, player.getYRot(), 0f);
            bat.setTarget(target);
            bat.damBonus = WandItem.getFocusPotency(wand);
            bat.owner = player;
            bat.setSummoned(true);
            bat.setResting(false);
            bat.setPersistenceRequired();
            if (isUpgradedWith(focus, FocusUpgrade.DEVIL_BATS)) {
                bat.setDevil(true);
            }
            if (isUpgradedWith(focus, FocusUpgrade.BAT_BOMBS)) {
                bat.setExplosive(true);
            }
            if (isUpgradedWith(focus, FocusUpgrade.VAMPIRE_BATS)) {
                bat.setVampire(true);
            }
            if (WandItem.consumeAllCentiVis(wand, player, getVisCost(focus), true) && level.addFreshEntity(bat)) {
                level.levelEvent(2004, BlockPos.containing(bat.position()), 0);
                level.playSound(null, bat, ModSounds.ICE.get(), SoundSource.PLAYERS, 0.2f, 0.95f + level.random.nextFloat() * 0.1f);
            } else {
                level.playSound(null, player, ModSounds.WAND_FAIL.get(), SoundSource.PLAYERS, 0.1f, 0.8f + level.random.nextFloat() * 0.1f);
            }
        }
        player.swing(hand);
        return true;
    }

    @Override
    public FocusUpgrade[] getPossibleUpgradesByRank(ItemStack focus, int rank) {
        return switch (rank) {
            case 1, 2, 4 -> new FocusUpgrade[]{FocusUpgrade.FRUGAL, FocusUpgrade.POTENCY};
            case 3 -> new FocusUpgrade[]{FocusUpgrade.FRUGAL, FocusUpgrade.POTENCY, FocusUpgrade.BAT_BOMBS, FocusUpgrade.DEVIL_BATS};
            case 5 -> new FocusUpgrade[]{FocusUpgrade.FRUGAL, FocusUpgrade.POTENCY, FocusUpgrade.VAMPIRE_BATS};
            default -> new FocusUpgrade[0];
        };
    }

    @Override
    public boolean canApplyUpgrade(ItemStack focus, Player player, FocusUpgrade type, int rank) {
        return type != FocusUpgrade.VAMPIRE_BATS
                || thaumcraft.common.research.PlayerKnowledge.get(player).isResearchComplete("VAMPBAT");
    }
}
