package thaumcraft.common.items.wands.foci;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.wands.FocusUpgrade;
import thaumcraft.common.items.wands.FocusItem;
import thaumcraft.common.items.wands.WandItem;
import thaumcraft.common.registry.ModSounds;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Фокус раскопок (ItemFocusExcavation оригинала): зелёный луч, который за несколько мгновений разрушает блок
 * и роняет добычу. Камень, землю и песок берёт быстро, всё остальное — впятеро медленнее.
 * Вис тратятся только за разрушенный блок.
 */
public class ExcavationFocus extends FocusItem {
    private static final AspectList COST = new AspectList().add(Aspect.EARTH, 15);
    private static final AspectList COST_SILK = new AspectList().add(Aspect.AIR, 1).add(Aspect.FIRE, 1).add(Aspect.EARTH, 16)
            .add(Aspect.WATER, 1).add(Aspect.ORDER, 1).add(Aspect.ENTROPY, 1);
    private static final AspectList COST_DOWSING = new AspectList().add(Aspect.FIRE, 2).add(Aspect.ORDER, 2).add(Aspect.EARTH, 15);

    /** Что и как долго сверлит каждый игрок; своя запись на клиенте и на сервере, как в оригинале. */
    private static final class Progress {
        BlockPos pos;
        float count;
        long soundAt;
    }

    private static final Map<String, Progress> PROGRESS = new HashMap<>();

    private static Progress progress(Player player) {
        return PROGRESS.computeIfAbsent((player.level().isClientSide ? "R" : "S") + player.getUUID(), k -> new Progress());
    }

    @Override
    protected String sortingPrefix() {
        return "BE";
    }

    @Override
    public Animation getAnimation(ItemStack focus) {
        return Animation.CHARGE;
    }

    @Override
    public int getFocusColor(ItemStack focus) {
        return 0x064006;
    }

    @Override
    public AspectList getVisCost(ItemStack focus) {
        return isUpgradedWith(focus, FocusUpgrade.SILK_TOUCH) ? COST_SILK : isUpgradedWith(focus, FocusUpgrade.DOWSING) ? COST_DOWSING : COST;
    }

    @Override
    public boolean isVisCostPerTick(ItemStack focus) {
        return true;
    }

    @Override
    public boolean onFocusRightClick(ItemStack wand, Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
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
        Progress progress = progress(player);
        Vec3 eye = player.getEyePosition();
        BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(player.getLookAngle().scale(10)),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        boolean struck = hit.getType() == HitResult.Type.BLOCK;
        Vec3 target = struck ? hit.getLocation() : eye.add(player.getLookAngle().scale(10));
        if (struck) {
            if (!level.isClientSide && progress.soundAt < System.currentTimeMillis()) {
                level.playSound(null, target.x, target.y, target.z, ModSounds.RUMBLE.get(), SoundSource.PLAYERS, 0.3f, 1f);
                progress.soundAt = System.currentTimeMillis() + 1200L;
            }
        } else {
            progress.soundAt = 0L;
        }
        if (level.isClientSide) {
            thaumcraft.client.fx.Fx.wandBeam(player.getId(), target.x, target.y, target.z, 2, 0x00ff66, false, struck ? 2f : 0f, struck ? 5 : 0);
        }

        BlockPos pos = hit.getBlockPos();
        BlockState state = struck ? level.getBlockState(pos) : Blocks.AIR.defaultBlockState();
        float hardness = struck ? state.getDestroySpeed(level, pos) : -1f;
        if (!struck || !level.mayInteract(player, pos) || state.isAir() || hardness < 0f) {
            reset(player, progress);
            return;
        }
        int potency = WandItem.getFocusPotency(wand);
        float speed = 0.05f + potency * 0.1f;
        // камень, земля, трава и песок оригинала: «каменные» блоки и всё, что копается лопатой
        if (state.instrument() == NoteBlockInstrument.BASEDRUM || state.is(BlockTags.MINEABLE_WITH_SHOVEL)) {
            speed = 0.25f + potency * 0.25f;
        }
        if (state.is(Blocks.OBSIDIAN)) {
            speed *= 3f;
        }
        if (!pos.equals(progress.pos)) {
            reset(player, progress);
            progress.pos = pos.immutable();
            return;
        }
        if (level.isClientSide) {
            if (progress.count > 0f) {
                thaumcraft.client.fx.Fx.blockCracks(player.getId(), pos.getX(), pos.getY(), pos.getZ(), (int) (progress.count / hardness * 9f));
            }
            progress.count = progress.count >= hardness ? 0f : progress.count + speed;
            return;
        }
        // остальным игрокам трещины рассылает сервер
        level.destroyBlockProgress(player.getId(), pos, (int) (progress.count / hardness * 9f));
        if (progress.count >= hardness && WandItem.consumeAllCentiVis(wand, player, cost, true)) {
            if (excavate((ServerLevel) level, focus, (ServerPlayer) player, state, pos)) {
                for (int i = 0; i < getUpgradeLevel(focus, FocusUpgrade.ENLARGE); i++) {
                    if (WandItem.consumeAllCentiVis(wand, player, cost, false) && breakNeighbour((ServerPlayer) player, pos, state, focus)) {
                        WandItem.consumeAllCentiVis(wand, player, cost, true);
                    }
                }
            }
            reset(player, progress);
        } else {
            progress.count += speed;
        }
    }

    private static void reset(Player player, Progress progress) {
        if (progress.pos != null) {
            if (player.level().isClientSide) {
                thaumcraft.client.fx.Fx.blockCracks(player.getId(), progress.pos.getX(), progress.pos.getY(), progress.pos.getZ(), -1);
            } else {
                player.level().destroyBlockProgress(player.getId(), progress.pos, -1);
            }
        }
        progress.pos = null;
        progress.count = 0f;
    }

    /** Ломает блок и роняет добычу — любую, какой бы инструмент блоку ни требовался. */
    private boolean excavate(ServerLevel level, ItemStack focus, ServerPlayer player, BlockState state, BlockPos pos) {
        if (ForgeHooks.onBlockBreakEvent(level, player.gameMode.getGameModeForPlayer(), player, pos) < 0) {
            return false;
        }
        int fortune = getUpgradeLevel(focus, FocusUpgrade.TREASURE);
        boolean silk = isUpgradedWith(focus, FocusUpgrade.SILK_TOUCH);
        // добыча считается по таблицам блока так, будто его сломали лучшей киркой с нужным зачарованием
        ItemStack tool = new ItemStack(Items.NETHERITE_PICKAXE);
        if (silk) {
            tool.enchant(Enchantments.SILK_TOUCH, 1);
        } else if (fortune > 0) {
            tool.enchant(Enchantments.BLOCK_FORTUNE, fortune);
        }
        // «Лозоходство»: с шансом 20% (плюс 7.5% за уровень «Сокровища») сырая руда выпадает самородной гроздью
        boolean dowsing = !silk && isUpgradedWith(focus, FocusUpgrade.DOWSING);
        float chance = 0.2f + fortune * 0.075f;
        for (ItemStack drop : Block.getDrops(state, level, pos, level.getBlockEntity(pos), player, tool)) {
            if (dowsing) {
                ItemStack special = thaumcraft.common.items.tools.ToolHelper.specialMiningResult(drop, chance, level.random);
                if (special != drop) {
                    drop = special;
                    level.playSound(null, pos, net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.2f,
                            0.7f + level.random.nextFloat() * 0.2f);
                }
            }
            Block.popResource(level, pos, drop);
        }
        state.spawnAfterBreak(level, pos, tool, false);
        int xp = state.getExpDrop(level, level.random, pos, silk ? 0 : fortune, silk ? 1 : 0);
        if (xp > 0) {
            state.getBlock().popExperience(level, pos, xp);
        }
        level.removeBlock(pos, false);
        level.levelEvent(2001, pos, Block.getId(state));
        return true;
    }

    /** «Расширение»: заодно ломает один соседний блок того же вида. */
    private boolean breakNeighbour(ServerPlayer player, BlockPos pos, BlockState state, ItemStack focus) {
        List<Direction> directions = new ArrayList<>(Arrays.asList(Direction.values()));
        Collections.shuffle(directions, new java.util.Random(player.level().random.nextLong()));
        for (Direction direction : directions) {
            BlockPos near = pos.relative(direction);
            if (player.level().getBlockState(near) == state && excavate(player.serverLevel(), focus, player, state, near)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onStoppedUsingFocus(ItemStack wand, Level level, Player player, int count) {
        reset(player, progress(player));
    }

    @Override
    public FocusUpgrade[] getPossibleUpgradesByRank(ItemStack focus, int rank) {
        return switch (rank) {
            case 1 -> new FocusUpgrade[]{FocusUpgrade.FRUGAL, FocusUpgrade.POTENCY, FocusUpgrade.TREASURE};
            case 2, 4 -> new FocusUpgrade[]{FocusUpgrade.FRUGAL, FocusUpgrade.POTENCY, FocusUpgrade.ENLARGE};
            case 3 -> new FocusUpgrade[]{FocusUpgrade.FRUGAL, FocusUpgrade.POTENCY, FocusUpgrade.TREASURE, FocusUpgrade.DOWSING};
            case 5 -> new FocusUpgrade[]{FocusUpgrade.FRUGAL, FocusUpgrade.POTENCY, FocusUpgrade.TREASURE, FocusUpgrade.SILK_TOUCH};
            default -> new FocusUpgrade[0];
        };
    }
}
