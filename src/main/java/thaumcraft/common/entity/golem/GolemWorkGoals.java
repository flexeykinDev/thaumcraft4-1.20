package thaumcraft.common.entity.golem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import org.jetbrains.annotations.Nullable;
import thaumcraft.common.blocks.ManaPodBlock;
import thaumcraft.common.registry.ModBlocks;
import thaumcraft.common.registry.ModItems;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

/**
 * Задачи големов-работников — перенос AIGolemAttackOnCollide, AIHarvestCrops и AIHarvestLogs оригинала:
 * ближний бой стража и мясника, сбор созревшего урожая и рубка деревьев.
 */
public final class GolemWorkGoals {
    private GolemWorkGoals() {
    }

    /** Ближний бой: голем идёт к цели и бьёт раз в 20 тиков (продвинутый — раз в 18), взмахивая руками. */
    public static class AttackOnCollide extends Goal {
        private final Golem golem;
        @Nullable
        private LivingEntity target;
        @Nullable
        private Path path;
        private int attackTick;
        private int counter;

        public AttackOnCollide(Golem golem) {
            this.golem = golem;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity candidate = golem.getTarget();
            if (candidate == null) {
                return false;
            }
            if (!golem.isValidTarget(candidate)) {
                golem.setTarget(null);
                return false;
            }
            target = candidate;
            path = golem.getNavigation().createPath(candidate, 0);
            return path != null;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse() && !golem.getNavigation().isDone();
        }

        @Override
        public void start() {
            golem.getNavigation().moveTo(path, 1.0);
            counter = 0;
        }

        @Override
        public void stop() {
            target = null;
            golem.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (target == null) {
                return;
            }
            golem.getLookControl().setLookAt(target, 30f, 30f);
            if (golem.getSensing().hasLineOfSight(target) && --counter <= 0) {
                counter = 4 + golem.getRandom().nextInt(7);
                golem.getNavigation().moveTo(target, 1.0);
            }
            attackTick = Math.max(attackTick - 1, 0);
            double reach = target.getBbWidth() * 2f * target.getBbWidth() * 2f + 1.0;
            if (golem.distanceToSqr(target.getX(), target.getBoundingBox().minY, target.getZ()) <= reach && attackTick <= 0) {
                attackTick = golem.getAttackSpeed();
                golem.startActionTimer();
                golem.doHurtTarget(target);
            }
        }
    }

    /** AIDartAttack оригинала: голем с дротикомётом обстреливает цель издали, а вблизи (ближе трёх блоков) дерётся руками. */
    public static class DartAttack extends Goal {
        private final Golem golem;
        @Nullable
        private LivingEntity target;
        private final int maxTime;
        private int time;

        public DartAttack(Golem golem) {
            this.golem = golem;
            maxTime = 30 - golem.getUpgradeAmount(Golem.UPGRADE_AIR) * 8;
            time = maxTime / 2;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity candidate = golem.getTarget();
            if (candidate == null) {
                return false;
            }
            if (!golem.isValidTarget(candidate)) {
                golem.setTarget(null);
                return false;
            }
            if (golem.distanceToSqr(candidate.getX(), candidate.getBoundingBox().minY, candidate.getZ()) < 9.0) {
                return false;
            }
            target = candidate;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse() && !golem.getNavigation().isDone();
        }

        @Override
        public void start() {
            golem.getNavigation().moveTo(target, 1.0);
        }

        @Override
        public void stop() {
            target = null;
            time = maxTime / 2;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (target == null) {
                return;
            }
            double distance = golem.distanceToSqr(target.getX(), target.getBoundingBox().minY, target.getZ());
            golem.getNavigation().moveTo(target, 1.0);
            if (golem.getSensing().hasLineOfSight(target)) {
                golem.getLookControl().setLookAt(target, 30f, 30f);
                time = Math.max(time - 1, 0);
                float reach = golem.getRange() * 0.8f;
                if (time <= 0 && distance <= reach * reach) {
                    golem.performRangedAttack(target);
                    time = maxTime;
                }
            }
        }
    }

    /** Общая часть работ «подойти к блоку и поковырять его»: время зависит от силы голема и твёрдости блока. */
    private abstract static class Dig extends Goal {
        protected final Golem golem;
        protected final Level level;
        protected BlockPos pos = BlockPos.ZERO;
        protected BlockState state = Blocks.AIR.defaultBlockState();
        protected int delay = -1;
        private int maxDelay = 1;
        private int mod = 1;
        protected int count;

        Dig(Golem golem) {
            this.golem = golem;
            this.level = golem.level();
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        protected abstract BlockPos find();

        protected abstract int workTime(float hardness);

        protected abstract void finish();

        protected void aim(BlockPos target) {
            pos = target.immutable();
            state = level.getBlockState(pos);
        }

        @Override
        public boolean canUse() {
            if (delay >= 0 || golem.tickCount % GolemHelper.DELAY != 0 || !golem.getNavigation().isDone()) {
                return false;
            }
            BlockPos found = find();
            if (found == null) {
                return false;
            }
            aim(found);
            return true;
        }

        protected boolean stillThere() {
            return level.getBlockState(pos) == state;
        }

        /** Голем уже стоит у блока: идти никуда не надо, можно сразу браться за дело. */
        protected boolean inReach() {
            return golem.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 4.0;
        }

        @Override
        public boolean canContinueToUse() {
            return stillThere() && count-- > 0 && (delay > 0 || !golem.getNavigation().isDone() || inReach());
        }

        @Override
        public void start() {
            count = 200;
            GolemHelper.walkTo(golem, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        }

        @Override
        public void stop() {
            level.destroyBlockProgress(golem.getId(), pos, -1);
            delay = -1;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            golem.getLookControl().setLookAt(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 30f, 30f);
            if (golem.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 4.0) {
                return;
            }
            if (delay < 0) {
                delay = workTime(state.getDestroySpeed(level, pos));
                maxDelay = delay;
                mod = Math.max(1, delay / Math.max(1, Math.round(delay / 6f)));
            }
            if (delay > 0) {
                if (--delay > 0 && delay % mod == 0 && golem.getNavigation().isDone()) {
                    golem.startActionTimer();
                    SoundType sound = state.getSoundType(level, pos, golem);
                    level.playSound(null, pos, sound.getHitSound(), SoundSource.BLOCKS, (sound.getVolume() + 0.7f) / 8f, sound.getPitch() * 0.5f);
                    level.destroyBlockProgress(golem.getId(), pos, (int) (9f * (1f - (float) delay / maxDelay)));
                }
                if (delay == 0) {
                    count = 200;
                    finish();
                }
            }
        }

        protected boolean inArea(BlockPos candidate, float distance) {
            BlockPos home = golem.getHome();
            return Math.abs(home.getX() - candidate.getX()) <= distance && Math.abs(home.getY() - candidate.getY()) <= distance
                    && Math.abs(home.getZ() - candidate.getZ()) <= distance;
        }

        /** Берёмся за следующий блок, не возвращаясь к осмотру окрестностей. */
        protected void next(BlockPos target) {
            level.destroyBlockProgress(golem.getId(), pos, -1);
            aim(target);
            delay = -1;
            start();
        }
    }

    // ---------- урожай ----------

    /** Созрело ли то, что здесь растёт (CropUtils.isGrownCrop оригинала в нынешних понятиях). */
    public static boolean isGrownCrop(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        if (state.isAir()) {
            return false;
        }
        if (block instanceof CropBlock crop) {
            return crop.isMaxAge(state);
        }
        if (block instanceof NetherWartBlock) {
            return state.getValue(NetherWartBlock.AGE) >= 3;
        }
        if (block instanceof CocoaBlock) {
            return state.getValue(CocoaBlock.AGE) >= 2;
        }
        if (block instanceof SweetBerryBushBlock) {
            return state.getValue(SweetBerryBushBlock.AGE) >= 3;
        }
        if (block instanceof ManaPodBlock) {
            return state.getValue(ManaPodBlock.AGE) >= 7;
        }
        // тростник и кактус: срезаем всё, что выше первого блока
        if (state.is(Blocks.SUGAR_CANE) || state.is(Blocks.CACTUS)) {
            return level.getBlockState(pos.below()).is(block);
        }
        return state.is(Blocks.MELON) || state.is(Blocks.PUMPKIN);
    }

    /** AIHarvestCrops: голем обходит поле вокруг дома и собирает созревшее; с улучшением порядка — засевает снова. */
    public static class HarvestCrops extends Dig {
        private final List<BlockPos> checklist = new ArrayList<>();

        public HarvestCrops(Golem golem) {
            super(golem);
        }

        private float distance() {
            return (float) Math.ceil(golem.getRange() / 4f);
        }

        @Override
        protected int workTime(float hardness) {
            return (int) Math.max(10f, (20f - golem.getGolemStrength() * 2f) * hardness);
        }

        /** За один осмотр проверяется один случайный столбец поля. */
        @Nullable
        @Override
        protected BlockPos find() {
            BlockPos home = golem.getHome();
            if (checklist.isEmpty()) {
                int distance = (int) distance();
                for (int dx = -distance; dx <= distance; dx++) {
                    for (int dz = -distance; dz <= distance; dz++) {
                        checklist.add(new BlockPos(home.getX() + dx, 0, home.getZ() + dz));
                    }
                }
                Collections.shuffle(checklist, new java.util.Random(golem.getRandom().nextLong()));
            }
            BlockPos column = checklist.remove(0);
            for (int y = home.getY() - 3; y <= home.getY() + 3; y++) {
                BlockPos candidate = new BlockPos(column.getX(), y, column.getZ());
                if (isGrownCrop(level, candidate)) {
                    return candidate;
                }
            }
            return null;
        }

        @Override
        protected void finish() {
            harvest();
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        BlockPos near = pos.offset(dx, dy, dz);
                        if (inArea(near, distance()) && isGrownCrop(level, near)) {
                            next(near);
                            return;
                        }
                    }
                }
            }
        }

        private void harvest() {
            Block block = state.getBlock();
            if (block instanceof SweetBerryBushBlock && level instanceof ServerLevel server) {
                // ягоды снимают, не ломая куст
                FakePlayer picker = FakePlayerFactory.getMinecraft(server);
                picker.setPos(golem.getX(), golem.getY(), golem.getZ());
                state.use(level, picker, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
                golem.startActionTimer();
                return;
            }
            level.destroyBlock(pos, true, golem);
            if (golem.getUpgradeAmount(Golem.UPGRADE_ORDER) > 0) {
                replant(block);
            }
            golem.startActionTimer();
        }

        /** Голем с улучшением порядка сажает на место собранного то, что из него выпало. */
        private void replant(Block block) {
            for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, golem.getBoundingBox().inflate(6.0))) {
                if (item.tickCount < 2) {
                    // свежий урожай летит к голему, а не разлетается по полю
                    Vec3 away = item.position().subtract(golem.position()).normalize();
                    item.setDeltaMovement(-away.x / 4.0, 0.075, -away.z / 4.0);
                }
                ItemStack stack = item.getItem();
                BlockState planted = null;
                if (block instanceof ManaPodBlock ? stack.is(ModItems.MANA_BEAN.get()) : Block.byItem(stack.getItem()) == block) {
                    planted = block instanceof ManaPodBlock ? ModBlocks.MANA_POD.get().defaultBlockState() : block.defaultBlockState();
                    if (block instanceof CocoaBlock) {
                        planted = planted.setValue(CocoaBlock.FACING, state.getValue(CocoaBlock.FACING));
                    }
                }
                if (planted != null && level.isEmptyBlock(pos) && planted.canSurvive(level, pos)) {
                    level.setBlock(pos, planted, 3);
                    stack.shrink(1);
                    if (stack.isEmpty()) {
                        item.discard();
                    } else {
                        item.setItem(stack);
                    }
                    return;
                }
            }
        }
    }

    // ---------- рубка ----------

    public static boolean isWoodLog(Level level, BlockPos pos) {
        return level.getBlockState(pos).is(BlockTags.LOGS);
    }

    /**
     * BlockUtils.breakFurthestBlock оригинала: от ствола под рукой голем «добирается» по таким же брёвнам до самого
     * дальнего и ломает его — так дерево исчезает с макушки, а голем не трогается с места.
     */
    public static BlockPos furthestLog(Level level, BlockPos from, Block block) {
        BlockPos last = from;
        double lastDistance = 0.0;
        boolean moved = true;
        while (moved) {
            moved = false;
            search:
            for (int dx = -2; dx <= 2; dx++) {
                for (int dy = 2; dy >= -2; dy--) {
                    for (int dz = -2; dz <= 2; dz++) {
                        BlockPos candidate = last.offset(dx, dy, dz);
                        if (Math.abs(candidate.getX() - from.getX()) > 24 || Math.abs(candidate.getY() - from.getY()) > 48
                                || Math.abs(candidate.getZ() - from.getZ()) > 24) {
                            return last;
                        }
                        BlockState there = level.getBlockState(candidate);
                        if (there.is(block) && there.is(BlockTags.LOGS) && there.getDestroySpeed(level, candidate) >= 0f) {
                            double distance = candidate.distSqr(from);
                            if (distance > lastDistance) {
                                lastDistance = distance;
                                last = candidate;
                                moved = true;
                                break search;
                            }
                        }
                    }
                }
            }
        }
        return last;
    }

    /** AIHarvestLogs: голем ищет вокруг дома бревно и рубит дерево целиком. */
    public static class HarvestLogs extends Dig {
        public HarvestLogs(Golem golem) {
            super(golem);
        }

        private float distance() {
            return (float) Math.ceil(golem.getRange() / 3f);
        }

        @Override
        protected int workTime(float hardness) {
            return (int) Math.max(5f, (20f - golem.getGolemStrength() * 3f) * hardness);
        }

        @Nullable
        @Override
        protected BlockPos find() {
            float distance = distance();
            BlockPos home = golem.getHome();
            var random = golem.getRandom();
            for (int attempt = 0; attempt < distance * 4f; attempt++) {
                int x = (int) (home.getX() + random.nextInt((int) (1f + distance * 2f)) - distance);
                int y = (int) (home.getY() + random.nextInt((int) (1f + distance)) - distance / 2f);
                int z = (int) (home.getZ() + random.nextInt((int) (1f + distance * 2f)) - distance);
                BlockPos found = new BlockPos(x, y, z);
                if (!isWoodLog(level, found)) {
                    continue;
                }
                // спускаемся по стволу, пока это приближает бревно к голему
                double best = golem.distanceToSqr(x + 0.5, y + 0.5, z + 0.5);
                for (int down = 1; isWoodLog(level, new BlockPos(x, y - down, z)); down++) {
                    double lower = golem.distanceToSqr(x + 0.5, y - down + 0.5, z + 0.5);
                    if (lower >= best) {
                        break;
                    }
                    best = lower;
                    found = new BlockPos(x, y - down, z);
                }
                return found;
            }
            return null;
        }

        @Override
        public boolean canContinueToUse() {
            return stillThere() && count-- > 0 && (delay > 0 || isWoodLog(level, pos) || !golem.getNavigation().isDone() || inReach());
        }

        @Override
        protected void finish() {
            BlockPos target = furthestLog(level, pos, state.getBlock());
            level.destroyBlock(target, true, golem);
            golem.startActionTimer();
            if (isWoodLog(level, pos)) {
                next(pos);
                return;
            }
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        BlockPos near = pos.offset(dx, dy, dz);
                        if (inArea(near, distance()) && isWoodLog(level, near)) {
                            next(near);
                            return;
                        }
                    }
                }
            }
        }
    }
}
