package thaumcraft.common.entity.golem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Задачи голема с ядром переливания — перенос AILiquidGoto, AILiquidGather и AILiquidEmpty оригинала: он черпает
 * жидкость из отмеченных баков или прямо из отмеченных источников в мире и носит её в бак, у которого поставлен.
 * Образцы-вёдра в окне ограничивают, какие жидкости носить; с улучшением энтропии он вычерпывает весь водоём,
 * начиная с дальнего края.
 */
public final class GolemLiquidGoals {
    private GolemLiquidGoals() {
    }

    @Nullable
    static IFluidHandler tank(Level level, BlockPos pos, @Nullable Direction side) {
        BlockEntity entity = level.getBlockEntity(pos);
        return entity == null ? null : entity.getCapability(ForgeCapabilities.FLUID_HANDLER, side).orElse(null);
    }

    @Nullable
    static IFluidHandler homeTank(Golem golem) {
        return tank(golem.level(), GolemHelper.homeBlock(golem), GolemHelper.homeSide(golem));
    }

    /** Источник жидкости в мире: блок, который можно зачерпнуть ведром. */
    static boolean isSource(Level level, BlockPos pos, Fluid fluid) {
        FluidState state = level.getFluidState(pos);
        return state.isSource() && state.getType().isSame(fluid) && level.getBlockState(pos).getBlock() instanceof BucketPickup;
    }

    /**
     * Какие жидкости голему сейчас стоит носить (getMissingLiquids оригинала): те, что есть у его отметок, принимает
     * домашний бак и разрешают образцы. Пока голем что-то несёт, годится только та же жидкость.
     */
    static List<Fluid> missingLiquids(Golem golem) {
        List<Fluid> out = new ArrayList<>();
        IFluidHandler home = homeTank(golem);
        if (home == null) {
            return out;
        }
        Level level = golem.level();
        Set<Fluid> offered = new LinkedHashSet<>();
        for (GolemMarker marker : golem.getMarkers()) {
            IFluidHandler tank = tank(level, marker.pos(), Direction.from3DDataValue(marker.side()));
            if (tank != null) {
                for (int i = 0; i < tank.getTanks(); i++) {
                    if (!tank.getFluidInTank(i).isEmpty()) {
                        offered.add(tank.getFluidInTank(i).getFluid());
                    }
                }
            } else if (level.getFluidState(marker.pos()).isSource()) {
                offered.add(level.getFluidState(marker.pos()).getType());
            }
        }
        if (!golem.fluidCarried.isEmpty()) {
            offered.add(golem.fluidCarried.getFluid());
        }
        List<Fluid> allowed = new ArrayList<>();
        for (ItemStack sample : GolemHelper.samples(golem)) {
            FluidUtil.getFluidContained(sample).ifPresent(stack -> allowed.add(stack.getFluid()));
        }
        for (Fluid fluid : offered) {
            if (!golem.fluidCarried.isEmpty() && !golem.fluidCarried.getFluid().isSame(fluid)) {
                continue;
            }
            if (!golem.inventory.isEmpty() && allowed.stream().noneMatch(fluid::isSame)) {
                continue;
            }
            if (home.fill(new FluidStack(fluid, 1000), IFluidHandler.FluidAction.SIMULATE) > 0) {
                out.add(fluid);
            }
        }
        return out;
    }

    /** Отметки на баках рядом с големом, из которых можно взять эту жидкость. */
    static List<GolemMarker> tanksAdjacent(Golem golem, Fluid fluid) {
        List<GolemMarker> out = new ArrayList<>();
        for (GolemMarker marker : golem.getMarkers()) {
            BlockPos pos = marker.pos();
            IFluidHandler tank = tank(golem.level(), pos, Direction.from3DDataValue(marker.side()));
            if (tank != null && golem.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < 4.0
                    && !tank.drain(new FluidStack(fluid, 1), IFluidHandler.FluidAction.SIMULATE).isEmpty()) {
                out.add(marker);
            }
        }
        return out;
    }

    /** Ближайшее отмеченное место в пределах дальности, где есть эта жидкость: сначала баки, потом источники в мире. */
    @Nullable
    static BlockPos findPossibleLiquid(Golem golem, Fluid fluid) {
        float reach = golem.getRange();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int pass = 0; pass < 2 && best == null; pass++) {
            for (GolemMarker marker : golem.getMarkers()) {
                BlockPos pos = marker.pos();
                boolean has;
                if (pass == 0) {
                    IFluidHandler tank = tank(golem.level(), pos, Direction.from3DDataValue(marker.side()));
                    has = tank != null && !tank.drain(new FluidStack(fluid, 1), IFluidHandler.FluidAction.SIMULATE).isEmpty();
                } else {
                    has = isSource(golem.level(), pos, fluid);
                }
                double distance = golem.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                if (has && distance <= reach * reach && distance < bestDistance) {
                    bestDistance = distance;
                    best = pos;
                }
            }
        }
        return best;
    }

    private static void splash(Golem golem, float volume) {
        golem.playSound(SoundEvents.GENERIC_SWIM, volume, 1f + (golem.getRandom().nextFloat() - golem.getRandom().nextFloat()) * 0.3f);
    }

    /** AILiquidEmpty: у дома голем выливает ношу в домашний бак. */
    public static class Empty extends Goal {
        private final Golem golem;

        public Empty(Golem golem) {
            this.golem = golem;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            BlockPos home = golem.getHome();
            if (!golem.getNavigation().isDone() || golem.fluidCarried.isEmpty()
                    || golem.distanceToSqr(home.getX() + 0.5, home.getY() + 0.5, home.getZ() + 0.5) > 5.0) {
                return false;
            }
            for (Fluid fluid : missingLiquids(golem)) {
                if (fluid.isSame(golem.fluidCarried.getFluid())) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            IFluidHandler home = homeTank(golem);
            if (home == null) {
                return;
            }
            int poured = home.fill(golem.fluidCarried.copy(), IFluidHandler.FluidAction.EXECUTE);
            golem.fluidCarried.shrink(poured);
            if (poured > 200) {
                splash(golem, Math.min(0.2f, 0.2f * poured / golem.getFluidCarryLimit()));
            }
            golem.updateCarried();
            golem.fluidWatched = null;
        }
    }

    /** AILiquidGoto: голем с местом в ведре идёт туда, где есть нужная жидкость. */
    public static class Goto extends Goal {
        private final Golem golem;
        private Vec3 target = Vec3.ZERO;
        private int count;
        private BlockPos previous = BlockPos.ZERO;

        public Goto(Golem golem) {
            this.golem = golem;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (golem.tickCount % GolemHelper.DELAY != 0 || golem.fluidCarried.getAmount() > golem.getFluidCarryLimit() - 1000) {
                return false;
            }
            for (Fluid fluid : missingLiquids(golem)) {
                BlockPos found = findPossibleLiquid(golem, fluid);
                if (found == null) {
                    continue;
                }
                golem.fluidWatched = fluid;
                // встаём на ближайший к голему твёрдый блок рядом с жидкостью
                Vec3 best = new Vec3(found.getX(), found.getY(), found.getZ());
                double bestDistance = Math.sqrt(golem.distanceToSqr(best));
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockPos near = found.offset(dx, 0, dz);
                        double distance = Math.sqrt(golem.distanceToSqr(near.getX(), near.getY(), near.getZ()));
                        if (distance < bestDistance && golem.level().getBlockState(near).isRedstoneConductor(golem.level(), near)) {
                            // цель — место на берегу, куда можно встать, а не сам блок берега
                            best = new Vec3(near.getX(), near.getY() + 1, near.getZ());
                            bestDistance = distance;
                        }
                    }
                }
                target = best;
                return true;
            }
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            return count > 0 && !golem.getNavigation().isDone();
        }

        @Override
        public void start() {
            count = 200;
            previous = golem.blockPosition();
            GolemHelper.walkTo(golem, target.x, target.y, target.z);
        }

        @Override
        public void stop() {
            count = 0;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            count--;
            if (count == 0 && previous.equals(golem.blockPosition())) {
                Vec3 around = DefaultRandomPos.getPos(golem, 2, 1);
                if (around != null) {
                    count = 20;
                    golem.getNavigation().moveTo(around.x, around.y, around.z, 1.0);
                }
            }
        }
    }

    /** AILiquidGather: стоя у отмеченного бака или источника, голем раз в полсекунды черпает из него. */
    public static class Gather extends Goal {
        private final Golem golem;
        private final Level level;
        private int count;
        /** Очереди источников для вычерпывания водоёма целиком: от дальних к ближним. */
        private final Map<BlockPos, List<BlockPos>> queue = new HashMap<>();

        public Gather(Golem golem) {
            this.golem = golem;
            this.level = golem.level();
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (golem.fluidWatched == null || !golem.getNavigation().isDone()) {
                return false;
            }
            int space = golem.getFluidCarryLimit() - golem.fluidCarried.getAmount();
            for (Fluid fluid : missingLiquids(golem)) {
                if (!tanksAdjacent(golem, fluid).isEmpty()) {
                    return true;
                }
                for (BlockPos pos : GolemHelper.markedBlocksAdjacent(golem, (byte) -1)) {
                    if (isSource(level, pos, fluid)) {
                        return space >= 1000;
                    }
                }
            }
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            return count < 20 && golem.fluidWatched != null;
        }

        @Override
        public boolean isInterruptable() {
            return false;
        }

        @Override
        public void start() {
            count = 0;
        }

        @Override
        public void stop() {
            count = 0;
            golem.fluidWatched = null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        private void take(FluidStack taken) {
            if (golem.fluidCarried.isEmpty()) {
                golem.fluidCarried = taken.copy();
            } else {
                golem.fluidCarried.grow(taken.getAmount());
            }
            golem.updateCarried();
            if (golem.fluidCarried.getAmount() > golem.getFluidCarryLimit() - 1000) {
                golem.fluidWatched = null;
            }
            count = 0;
        }

        @Override
        public void tick() {
            if (++count < 10) {
                return;
            }
            int max = golem.getFluidCarryLimit();
            for (Fluid fluid : missingLiquids(golem)) {
                for (GolemMarker marker : tanksAdjacent(golem, fluid)) {
                    IFluidHandler tank = tank(level, marker.pos(), Direction.from3DDataValue(marker.side()));
                    int space = max - golem.fluidCarried.getAmount();
                    if (tank == null || space <= 0) {
                        continue;
                    }
                    FluidStack drained = tank.drain(new FluidStack(fluid, space), IFluidHandler.FluidAction.EXECUTE);
                    if (!drained.isEmpty()) {
                        if (drained.getAmount() > 200) {
                            splash(golem, 0.2f * drained.getAmount() / max);
                        }
                        take(drained);
                    }
                }
                for (BlockPos marked : GolemHelper.markedBlocksAdjacent(golem, (byte) -1)) {
                    if (!isSource(level, marked, fluid) || max - golem.fluidCarried.getAmount() < 1000) {
                        continue;
                    }
                    BlockPos from = marked;
                    if (golem.getUpgradeAmount(Golem.UPGRADE_ENTROPY) > 0) {
                        List<BlockPos> sources = queue.get(marked);
                        if (sources == null || sources.isEmpty()) {
                            sources = connectedSources(marked, fluid);
                            queue.put(marked, sources);
                        }
                        while (!sources.isEmpty()) {
                            BlockPos candidate = sources.remove(0);
                            if (isSource(level, candidate, fluid)) {
                                from = candidate;
                                break;
                            }
                        }
                    }
                    BlockState state = level.getBlockState(from);
                    if (state.getBlock() instanceof BucketPickup pickup && !pickup.pickupBlock(level, from, state).isEmpty()) {
                        splash(golem, 0.2f);
                        take(new FluidStack(fluid, 1000));
                    }
                }
            }
        }

        /** Все источники той же жидкости, связанные с отмеченным, в пределах дальности голема — дальние первыми. */
        private List<BlockPos> connectedSources(BlockPos origin, Fluid fluid) {
            float reach = golem.getRange() * golem.getRange();
            List<BlockPos> sources = new ArrayList<>();
            Set<BlockPos> seen = new HashSet<>();
            ArrayDeque<BlockPos> open = new ArrayDeque<>();
            open.add(origin);
            seen.add(origin);
            while (!open.isEmpty() && seen.size() < 4096) {
                BlockPos at = open.poll();
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            BlockPos next = at.offset(dx, dy, dz);
                            if (next.distSqr(origin) > reach || !level.getFluidState(next).getType().isSame(fluid) || !seen.add(next)) {
                                continue;
                            }
                            if (isSource(level, next, fluid)) {
                                sources.add(next);
                            }
                            open.add(next);
                        }
                    }
                }
            }
            sources.sort((a, b) -> Double.compare(b.distSqr(origin), a.distSqr(origin)));
            return sources;
        }
    }
}
