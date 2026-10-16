package thaumcraft.api.visnet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.network.FxPacket;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Сеть вис — перенос VisNetHandler оригинала. Хранит источники каждого мира, подбирает новому реле ближайшего
 * видимого соседа и находит для прибора звенья, из которых тот может тянуть вис.
 */
public final class VisNetHandler {
    private static final Map<ResourceKey<Level>, Map<BlockPos, WeakReference<VisNode>>> SOURCES = new HashMap<>();
    /** Для каждого прибора — звенья, до которых он достаёт; сбрасывается при любом изменении сети. */
    private static final Map<GlobalPos, List<WeakReference<VisNode>>> NEARBY = new HashMap<>();

    private VisNetHandler() {
    }

    private static boolean valid(@Nullable VisNode node) {
        return node != null && !node.isVisNodeRemoved() && node.visLevel() != null;
    }

    /**
     * Прибор в точке pos тянет из сети вис одного аспекта.
     *
     * @param amount сколько нужно, в сотых долях вис
     * @return сколько удалось получить
     */
    public static int drainVis(Level level, BlockPos pos, Aspect aspect, int amount) {
        GlobalPos drainer = GlobalPos.of(level.dimension(), pos);
        List<WeakReference<VisNode>> nodes = NEARBY.computeIfAbsent(drainer, k -> calculateNearbyNodes(level, pos));
        int drained = 0;
        for (WeakReference<VisNode> ref : nodes) {
            VisNode node = ref.get();
            if (!valid(node)) {
                continue;
            }
            int got = node.consumeVis(aspect, amount);
            drained += got;
            amount -= got;
            if (got > 0 && level instanceof ServerLevel server) {
                // струйка вис от звена к прибору (PacketFXVisDrain оригинала)
                FxPacket.send(server, FxPacket.ESSENTIA, Vec3.atCenterOf(node.visPos()), Vec3.atCenterOf(pos), aspect.getColor(), 0);
            }
            if (amount <= 0) {
                break;
            }
        }
        return drained;
    }

    public static void addSource(VisNode source) {
        Level level = source.visLevel();
        if (level != null) {
            SOURCES.computeIfAbsent(level.dimension(), k -> new HashMap<>()).put(source.visPos().immutable(), new WeakReference<>(source));
            NEARBY.clear();
        }
    }

    public static void removeSource(VisNode source) {
        Level level = source.visLevel();
        if (level != null) {
            Map<BlockPos, WeakReference<VisNode>> sources = SOURCES.get(level.dimension());
            if (sources != null) {
                sources.remove(source.visPos());
            }
        }
        NEARBY.clear();
    }

    /** Сеть изменилась: приборам надо заново искать свои звенья. */
    public static void invalidate() {
        NEARBY.clear();
    }

    private static double distanceIfInRange(BlockPos a, BlockPos b, int range) {
        double distance = a.distSqr(b);
        return distance > range * range ? -1 : distance;
    }

    private record Candidate(VisNode node, double distance) {
    }

    /** Ищет новому реле родителя: ближайшее видимое звено любой из сетей, подходящее по настройке. */
    @Nullable
    public static VisNode addNode(VisNode node) {
        Level level = node.visLevel();
        Map<BlockPos, WeakReference<VisNode>> sources = level == null ? null : SOURCES.get(level.dimension());
        if (sources == null) {
            return null;
        }
        List<Candidate> nearby = new ArrayList<>();
        for (WeakReference<VisNode> ref : sources.values()) {
            VisNode source = ref.get();
            if (!valid(source)) {
                continue;
            }
            double r = distanceIfInRange(node.visPos(), source.visPos(), node.getVisRange());
            if (r > 0) {
                // источник предпочтительнее любого реле на том же расстоянии
                nearby.add(new Candidate(source, r - node.getVisRange() * 2));
            }
            findClosestNodes(node, source, nearby, new HashSet<>());
        }
        VisNode closest = null;
        double best = Double.MAX_VALUE;
        for (Candidate candidate : nearby) {
            VisNode other = candidate.node();
            boolean attuned = node.getAttunement() == -1 || other.getAttunement() == -1 || node.getAttunement() == other.getAttunement();
            if (candidate.distance() < best && attuned && canNodeBeSeen(node, other)) {
                best = candidate.distance();
                closest = other;
            }
        }
        if (closest != null) {
            closest.visLink().getChildren().add(new WeakReference<>(node));
            NEARBY.clear();
        }
        return closest;
    }

    private static void findClosestNodes(VisNode target, VisNode parent, List<Candidate> out, Set<VisNode> seen) {
        if (seen.size() > 512 || !seen.add(parent)) {
            return;
        }
        for (WeakReference<VisNode> ref : parent.visLink().getChildren()) {
            VisNode child = ref.get();
            if (valid(child) && child != target && child != parent) {
                double r = distanceIfInRange(child.visPos(), target.visPos(), target.getVisRange());
                if (r > 0) {
                    out.add(new Candidate(child, r));
                }
                findClosestNodes(target, child, out, seen);
            }
        }
    }

    /** В каждой сети прибор выбирает одно, ближайшее к себе звено. */
    private static List<WeakReference<VisNode>> calculateNearbyNodes(Level level, BlockPos pos) {
        List<WeakReference<VisNode>> out = new ArrayList<>();
        Map<BlockPos, WeakReference<VisNode>> sources = SOURCES.get(level.dimension());
        if (sources == null) {
            return out;
        }
        for (WeakReference<VisNode> ref : sources.values()) {
            VisNode source = ref.get();
            if (!valid(source)) {
                continue;
            }
            VisNode closest = null;
            double range = Double.MAX_VALUE;
            double r = distanceIfInRange(pos, source.visPos(), source.getVisRange());
            if (r > 0) {
                range = r;
                closest = source;
            }
            List<VisNode> all = new ArrayList<>();
            collectChildren(source, all, new HashSet<>());
            for (VisNode child : all) {
                double r2 = distanceIfInRange(child.visPos(), pos, child.getVisRange());
                if (r2 > 0 && r2 < range) {
                    range = r2;
                    closest = child;
                }
            }
            if (closest != null) {
                out.add(new WeakReference<>(closest));
            }
        }
        return out;
    }

    private static void collectChildren(VisNode node, List<VisNode> out, Set<VisNode> seen) {
        if (!seen.add(node)) {
            return;
        }
        for (WeakReference<VisNode> ref : node.visLink().getChildren()) {
            VisNode child = ref.get();
            if (valid(child) && child.visLevel().isLoaded(child.visPos())) {
                out.add(child);
                collectChildren(child, out, seen);
            }
        }
    }

    /** Между звеньями не должно быть блоков: луч идёт от центра к центру, сами звенья не в счёт. */
    public static boolean canNodeBeSeen(VisNode source, VisNode target) {
        Level level = source.visLevel();
        if (level == null) {
            return false;
        }
        Vec3 from = Vec3.atCenterOf(source.visPos());
        Vec3 to = Vec3.atCenterOf(target.visPos());
        // начало луча отодвигается от центра, чтобы он не упёрся в блок самого звена
        Vec3 step = to.subtract(from);
        if (step.lengthSqr() > 1) {
            from = from.add(step.normalize().scale(0.7));
        }
        BlockHitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null));
        return hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(target.visPos()) || hit.getBlockPos().equals(source.visPos());
    }
}
