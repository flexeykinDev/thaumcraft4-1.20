package thaumcraft.common.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Общие для обоих деревьев операции: постановка брёвен и листвы и расчёт расстояний, чтобы листва не опадала. */
final class TreeBuilder {
    private final WorldGenLevel level;
    private final Block log;
    private final Block leaves;
    private final Set<BlockPos> logs = new HashSet<>();
    private final Set<BlockPos> leafBlocks = new HashSet<>();

    TreeBuilder(WorldGenLevel level, Block log, Block leaves) {
        this.level = level;
        this.log = log;
        this.leaves = leaves;
    }

    boolean isOwnLeaves(BlockPos pos) {
        return level.getBlockState(pos).is(leaves);
    }

    boolean isAirOrOwnLeaves(BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.is(leaves);
    }

    /** Воздух, листва и всё, что можно заменить (трава, снег). */
    boolean isReplaceable(BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.is(BlockTags.LEAVES) || state.canBeReplaced();
    }

    boolean isSoil(BlockPos pos) {
        return level.getBlockState(pos).is(BlockTags.DIRT);
    }

    void placeLog(BlockPos pos, Direction.Axis axis) {
        BlockPos fixed = pos.immutable();
        level.setBlock(fixed, log.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis), 19);
        logs.add(fixed);
        leafBlocks.remove(fixed);
    }

    /** Особый блок ствола (сучок с узлом): для листвы он считается бревном. */
    void placeTrunkBlock(BlockPos pos, BlockState state) {
        BlockPos fixed = pos.immutable();
        level.setBlock(fixed, state, 19);
        logs.add(fixed);
        leafBlocks.remove(fixed);
    }

    void placeLeaves(BlockPos pos) {
        BlockPos fixed = pos.immutable();
        if (logs.contains(fixed)) {
            return;
        }
        level.setBlock(fixed, leaves.defaultBlockState(), 19);
        leafBlocks.add(fixed);
    }

    /**
     * При генерации мира соседи не обновляются, и у листвы остаётся расстояние до бревна 7, то есть она
     * считается оторванной и опадает. Считаем расстояния сами, волной от брёвен.
     */
    void finish() {
        Map<BlockPos, Integer> distance = new HashMap<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        for (BlockPos logPos : logs) {
            distance.put(logPos, 0);
            queue.add(logPos);
        }
        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            int next = distance.get(pos) + 1;
            if (next > 6) {
                continue;
            }
            for (Direction direction : Direction.values()) {
                BlockPos neighbor = pos.relative(direction);
                if (leafBlocks.contains(neighbor) && !distance.containsKey(neighbor)) {
                    distance.put(neighbor, next);
                    queue.add(neighbor);
                }
            }
        }
        for (BlockPos pos : leafBlocks) {
            BlockState state = level.getBlockState(pos);
            if (state.is(leaves)) {
                // У великого древа часть листвы висит дальше 6 блоков от веток. В оригинале она не опадала,
                // пока дерево не трогали, поэтому даём ей 6: проверка сработает только при обновлении соседей.
                level.setBlock(pos, state.setValue(LeavesBlock.DISTANCE, distance.getOrDefault(pos, 6)), 19);
            }
        }
    }
}
