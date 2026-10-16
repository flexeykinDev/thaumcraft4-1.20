package thaumcraft.common.entity.golem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.EssentiaTransport;
import thaumcraft.common.blockentity.EssentiaBlockEntity;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Задачи голема с ядром алхимии — перенос AIEssentiaGather, AIEssentiaGoto и AIEssentiaEmpty оригинала: он забирает
 * эссенцию из перегонных кубов над печью, у которой поставлен (или прямо из сосуда-дома), и разносит её по
 * отмеченным банкам, выбирая сперва банки с ярлыком и уже начатые.
 */
public final class GolemEssentiaGoals {
    private GolemEssentiaGoals() {
    }

    private static void splash(Golem golem, float volume) {
        golem.playSound(SoundEvents.GENERIC_SWIM, volume, 1f + (golem.getRandom().nextFloat() - golem.getRandom().nextFloat()) * 0.3f);
    }

    private static void collectJars(Level level, BlockPos pos, Map<BlockPos, EssentiaBlockEntity> found) {
        for (Direction dir : Direction.values()) {
            BlockPos next = pos.relative(dir);
            if (!found.containsKey(next) && level.getBlockEntity(next) instanceof EssentiaBlockEntity jar && jar.isJar()) {
                found.put(next, jar);
                collectJars(level, next, found);
            }
        }
    }

    /**
     * Куда нести эссенцию (findJarWithRoom оригинала). Годятся отмеченные банки и все банки, стоящие вплотную к ним,
     * а также отмеченные устройства, которые сейчас тянут этот аспект. Порядок выбора банок: начатая с ярлыком,
     * пустая с ярлыком, полная банка пустоты с ярлыком, начатая без ярлыка, пустая без ярлыка, банки пустоты.
     */
    @Nullable
    public static BlockPos findJarWithRoom(Golem golem) {
        Level level = golem.level();
        BlockPos home = golem.getHome();
        float reach = golem.getRange() * golem.getRange();
        Aspect carried = golem.essentiaAmount > 0 ? golem.essentia : null;
        Map<BlockPos, EssentiaBlockEntity> jars = new LinkedHashMap<>();
        List<BlockPos> candidates = new ArrayList<>();
        for (GolemMarker marker : golem.getMarkers()) {
            BlockPos pos = marker.pos();
            BlockEntity entity = level.getBlockEntity(pos);
            if (pos.distSqr(home) > reach) {
                continue;
            }
            if (entity instanceof EssentiaBlockEntity jar && jar.isJar()) {
                jars.put(pos, jar);
                collectJars(level, pos, jars);
            } else if (entity instanceof EssentiaTransport transport && carried != null) {
                Direction side = Direction.from3DDataValue(marker.side());
                if (transport.canInputFrom(side) && transport.getSuctionAmount(side) > 0
                        && (transport.getSuctionType(side) == null || transport.getSuctionType(side) == carried)) {
                    candidates.add(pos);
                }
            }
        }
        for (int tier = 0; tier < 7 && candidates.stream().noneMatch(jars::containsKey); tier++) {
            for (Map.Entry<BlockPos, EssentiaBlockEntity> entry : jars.entrySet()) {
                EssentiaBlockEntity jar = entry.getValue();
                boolean labelled = jar.getFilter() != null;
                boolean empty = jar.getAmount() == 0;
                boolean started = carried != null && jar.getAspect() == carried && jar.getAmount() < jar.getCapacity();
                boolean fullVoid = carried != null && jar.isVoid() && jar.getAspect() == carried && jar.getAmount() >= jar.getCapacity();
                boolean accepts = carried == null ? labelled || empty : jar.getFilter() == null || jar.getFilter() == carried;
                boolean fits = switch (tier) {
                    case 0 -> started && labelled;
                    case 1 -> empty && labelled;
                    case 2 -> fullVoid && labelled;
                    case 3 -> started && !labelled;
                    case 4 -> empty && !labelled && !jar.isVoid();
                    case 5 -> jar.isVoid() && !labelled && carried != null && jar.getAspect() == carried;
                    default -> empty && !labelled && jar.isVoid();
                };
                if (fits && accepts) {
                    candidates.add(entry.getKey());
                }
            }
        }
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos pos : candidates) {
            double distance = pos.distSqr(home);
            // банка пустоты — крайний случай: она дальше любой настоящей
            if (jars.containsKey(pos) && jars.get(pos).isVoid()) {
                distance += reach;
            }
            if (distance < bestDistance) {
                bestDistance = distance;
                best = pos;
            }
        }
        return best;
    }

    /** AIEssentiaGather: у дома голем раз в секунду набирает эссенцию — из сосуда-дома или из самого полного куба над печью. */
    public static class Gather extends Goal {
        private final Golem golem;
        private final Level level;
        private long delay;
        private int start;

        public Gather(Golem golem) {
            this.golem = golem;
            this.level = golem.level();
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        private boolean wants(@Nullable Aspect aspect) {
            return golem.essentiaAmount == 0 || (golem.essentia == null || golem.essentia == aspect) && golem.essentiaAmount < golem.getCarryLimit();
        }

        @Override
        public boolean canUse() {
            if (!golem.getNavigation().isDone() || delay > level.getGameTime()) {
                return false;
            }
            BlockPos home = GolemHelper.homeBlock(golem);
            if (golem.distanceToSqr(home.getX() + 0.5, home.getY() + 0.5, home.getZ() + 0.5) > 6.0) {
                return false;
            }
            Direction facing = GolemHelper.homeSide(golem);
            BlockEntity entity = level.getBlockEntity(home);
            if (entity == null) {
                return false;
            }
            start = 0;
            if (entity instanceof EssentiaTransport transport) {
                boolean vessel = entity instanceof EssentiaBlockEntity;
                Direction side = vessel ? Direction.UP : facing;
                if ((vessel || transport.canOutputTo(facing)) && transport.getEssentiaAmount(side) > 0 && wants(transport.getEssentiaType(side))) {
                    delay = level.getGameTime() + 20;
                    return true;
                }
                return false;
            }
            // дом — печь: выбираем самый полный из кубов, поставленных над ней столбиком
            start = -1;
            int most = -1;
            for (int up = 5; up >= 0; up--) {
                if (level.getBlockEntity(home.above(up)) instanceof EssentiaBlockEntity alembic && !alembic.isJar() && wants(alembic.getAspect())
                        && alembic.getAmount() > most) {
                    delay = level.getGameTime() + 20;
                    start = up;
                    most = alembic.getAmount();
                }
            }
            return start >= 0 && most > 0;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            BlockPos home = GolemHelper.homeBlock(golem).above(Math.max(0, start));
            if (!(level.getBlockEntity(home) instanceof EssentiaTransport source)) {
                return;
            }
            Direction side = source instanceof EssentiaBlockEntity ? Direction.UP : GolemHelper.homeSide(golem);
            Aspect aspect = source.getEssentiaType(side);
            int available = source.getEssentiaAmount(side);
            if (aspect == null || available == 0 || !source.canOutputTo(side) || !wants(aspect)) {
                return;
            }
            int amount = Math.min(available, golem.getCarryLimit() - golem.essentiaAmount);
            int taken = source.takeEssentia(aspect, amount, side);
            if (taken > 0) {
                golem.essentia = aspect;
                golem.essentiaAmount += taken;
                splash(golem, 0.05f);
                golem.updateCarried();
            }
            delay = level.getGameTime() + 2;
        }
    }

    /** AIEssentiaGoto: голем с эссенцией идёт к выбранной банке. */
    public static class Goto extends Goal {
        private final Golem golem;
        private BlockPos jar = BlockPos.ZERO;
        private int count;
        private BlockPos previous = BlockPos.ZERO;

        public Goto(Golem golem) {
            this.golem = golem;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (golem.tickCount % GolemHelper.DELAY != 0 || golem.essentia == null || golem.essentiaAmount == 0) {
                return false;
            }
            BlockPos found = findJarWithRoom(golem);
            if (found == null) {
                return false;
            }
            jar = found;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return count > 0 && !golem.getNavigation().isDone();
        }

        @Override
        public void start() {
            count = 200;
            previous = golem.blockPosition();
            GolemHelper.walkTo(golem, jar.getX(), jar.getY(), jar.getZ());
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

    /** AIEssentiaEmpty: стоя у банки или устройства, голем сливает в него эссенцию. */
    public static class Empty extends Goal {
        private final Golem golem;
        private BlockPos jar = BlockPos.ZERO;

        public Empty(Golem golem) {
            this.golem = golem;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!golem.getNavigation().isDone() || golem.essentia == null || golem.essentiaAmount == 0) {
                return false;
            }
            BlockPos found = findJarWithRoom(golem);
            if (found == null || golem.distanceToSqr(found.getX() + 0.5, found.getY() + 0.5, found.getZ() + 0.5) > 4.0) {
                return false;
            }
            jar = found;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            BlockEntity entity = golem.level().getBlockEntity(jar);
            int before = golem.essentiaAmount;
            if (entity instanceof EssentiaBlockEntity vessel && vessel.isJar()) {
                golem.essentiaAmount = vessel.add(golem.essentia, golem.essentiaAmount);
            } else if (entity instanceof EssentiaTransport transport) {
                for (Direction side : GolemHelper.markedSides(golem, jar, (byte) -1)) {
                    if (transport.canInputFrom(side) && transport.getSuctionAmount(side) > 0
                            && (transport.getSuctionType(side) == null || transport.getSuctionType(side) == golem.essentia)) {
                        int added = transport.addEssentia(golem.essentia, golem.essentiaAmount, side);
                        if (added > 0) {
                            golem.essentiaAmount -= added;
                            break;
                        }
                    }
                }
            }
            if (golem.essentiaAmount != before) {
                if (golem.essentiaAmount == 0) {
                    golem.essentia = null;
                }
                splash(golem, 0.2f);
                golem.updateCarried();
            }
        }
    }
}
