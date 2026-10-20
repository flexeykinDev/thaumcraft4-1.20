package thaumictinkerer.common.item.kami;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.wands.FocusUpgrade;
import thaumcraft.common.items.wands.WandItem;
import thaumcraft.common.registry.ModSounds;
import thaumictinkerer.common.block.WarpGateBlock;
import thaumictinkerer.common.item.focus.TTFocus;
import thaumictinkerer.common.network.TTNetwork;

import java.util.ArrayList;
import java.util.List;

/** Фокусы раздела Ками (ItemFocusRecall, ItemFocusShadowbeam, ItemFocusXPDrain). */
public final class KamiFoci {
    private KamiFoci() {
    }

    /** Возврат: три секунды удержания — и игрок у врат, на которые настроена жемчужина из хотбара (в том же измерении). */
    public static class Recall extends TTFocus {
        public Recall() {
            super("focus_recall", 0x9CF8FF, new AspectList().add(Aspect.AIR, 4000).add(Aspect.EARTH, 4000).add(Aspect.ORDER, 4000), false,
                    "depth:1");
        }

        @Override
        public Rarity getRarity(ItemStack stack) {
            return Kami.RARITY;
        }

        @Override
        public boolean onFocusRightClick(ItemStack wand, Level level, Player player, InteractionHand hand) {
            player.startUsingItem(hand);
            return true;
        }

        @Override
        public void onUsingFocusTick(ItemStack wand, Player player, int count) {
            if (player.level().isClientSide || player.getTicksUsingItem() <= 60) {
                return;
            }
            for (int slot = 0; slot < 9; slot++) {
                ItemStack pearl = player.getInventory().getItem(slot);
                BlockPos target = SkyPearlItem.pos(pearl);
                if (target != null) {
                    AspectList cost = getVisCost(WandItem.getFocusStack(wand));
                    if (player.level().dimension().equals(SkyPearlItem.dimension(pearl)) && WandItem.consumeAllCentiVis(wand, player, cost, false)
                            && WarpGateBlock.teleport(player, target)) {
                        WandItem.consumeAllCentiVis(wand, player, cost, true);
                    }
                    break;
                }
            }
            player.stopUsingItem();
        }

        @Override
        public FocusUpgrade[] getPossibleUpgradesByRank(ItemStack focus, int rank) {
            return new FocusUpgrade[0];
        }
    }

    /** Высасывание опыта: пока держат, 15 очков опыта становятся пятью единицами вис того первоэлемента, которого в жезле меньше полного. */
    public static class XpDrain extends TTFocus {
        private int lastGiven;

        public XpDrain() {
            super("focus_xp_drain", 0xFFFFFF, new AspectList(), true, null);
        }

        @Override
        public Rarity getRarity(ItemStack stack) {
            return Kami.RARITY;
        }

        @Override
        public int getFocusColor(ItemStack focus) {
            return java.awt.Color.HSBtoRGB((System.currentTimeMillis() / 20 % 360) / 360f, 1f, 1f) & 0xFFFFFF;
        }

        @Override
        public boolean onFocusRightClick(ItemStack wand, Level level, Player player, InteractionHand hand) {
            player.startUsingItem(hand);
            return true;
        }

        @Override
        public void onUsingFocusTick(ItemStack wand, Player player, int count) {
            if (player.level().isClientSide) {
                return;
            }
            Aspect target = null;
            List<Aspect> primals = Aspect.primals();
            for (int i = 0; target == null && i < 7; i++) {
                lastGiven = lastGiven == 5 ? 0 : lastGiven + 1;
                Aspect aspect = primals.get(lastGiven);
                if (WandItem.getVis(wand, aspect) < WandItem.getMaxVis(wand)) {
                    target = aspect;
                }
            }
            if (target != null && totalXp(player) >= 15) {
                player.giveExperiencePoints(-15);
                WandItem.storeVis(wand, target, Math.min(WandItem.getMaxVis(wand), WandItem.getVis(wand, target) + 500));
            }
        }

        private static int totalXp(Player player) {
            int level = player.experienceLevel;
            int points = level >= 30 ? (int) (4.5 * level * level - 162.5 * level + 2220) : level >= 16
                    ? (int) (2.5 * level * level - 40.5 * level + 360) : level * level + 6 * level;
            return points + Math.round(player.experienceProgress * player.getXpNeededForNextLevel());
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.translatable("item.Focus.cost2"));
            tooltip.add(Component.literal(" ").append(Component.translatable("ttmisc.experience").withStyle(ChatFormatting.GREEN))
                    .append(Component.literal(" x 15").withStyle(ChatFormatting.WHITE)));
        }

        @Override
        public FocusUpgrade[] getPossibleUpgradesByRank(ItemStack focus, int rank) {
            return new FocusUpgrade[0];
        }
    }

    /**
     * Теневой луч: пока держат, луч длиной в 300 шагов летит от глаз, отражаясь от блоков, как от зеркал, и бьёт всех на пути
     * на 8 единиц (игроков — только с включённым PvP).
     */
    public static class Shadowbeam extends TTFocus {
        private static final int LENGTH = 298;
        private static final int OFFSET = 2;
        private static final double STEP = 0.15;

        public Shadowbeam() {
            super("focus_shadowbeam", 0x4B0053, new AspectList().add(Aspect.ORDER, 25).add(Aspect.ENTROPY, 25).add(Aspect.AIR, 15), true, "orn");
        }

        @Override
        public Rarity getRarity(ItemStack stack) {
            return Kami.RARITY;
        }

        @Override
        public boolean onFocusRightClick(ItemStack wand, Level level, Player player, InteractionHand hand) {
            player.startUsingItem(hand);
            return true;
        }

        @Override
        public void onUsingFocusTick(ItemStack wand, Player player, int count) {
            if (!(player.level() instanceof ServerLevel level)
                    || !WandItem.consumeAllCentiVis(wand, player, getVisCost(WandItem.getFocusStack(wand)), true)) {
                return;
            }
            if (level.random.nextInt(10) == 0) {
                level.playSound(null, player.blockPosition(), ModSounds.BRAIN.get(), SoundSource.PLAYERS, 0.5f, 1f);
            }
            fire(level, player);
        }

        /** Beam.updateUntilDead: весь путь луча считается за один тик. */
        public static void fire(ServerLevel level, Player player) {
            Vec3 pos = player.getEyePosition().subtract(0, 0.1, 0);
            Vec3 motion = player.getViewVector(1f).scale(STEP * 10).scale(0.1);
            List<Vec3> sparkles = new ArrayList<>();
            List<Entity> hit = new ArrayList<>();
            for (int tick = 0; tick < OFFSET + LENGTH; tick++) {
                Vec3 next = pos.add(motion);
                BlockHitResult block = level.clip(new ClipContext(pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
                AABB sweep = new AABB(pos, next).inflate(0.25);
                for (Entity entity : level.getEntities(player, sweep, e -> e instanceof LivingEntity && e.isAlive() && !hit.contains(e))) {
                    if (entity instanceof Player && !level.getServer().isPvpAllowed()) {
                        continue;
                    }
                    hit.add(entity);
                    entity.hurt(player.damageSources().playerAttack(player), 8f);
                }
                if (block.getType() == HitResult.Type.BLOCK) {
                    // отражение: v - 2(v·n)n
                    Vec3 normal = Vec3.atLowerCornerOf(block.getDirection().getNormal());
                    motion = motion.subtract(normal.scale(2 * motion.dot(normal)));
                    next = block.getLocation().add(motion.normalize().scale(0.01));
                }
                pos = next;
                if (tick > OFFSET && tick % 3 == 0) {
                    sparkles.add(pos);
                }
            }
            TTNetwork.sendBeam(level, player.position(), sparkles);
        }
    }
}
