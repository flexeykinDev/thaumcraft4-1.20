package thaumcraft.common.taint;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import thaumcraft.Thaumcraft;
import thaumcraft.common.registry.ModBlocks;
import thaumcraft.common.registry.ModSounds;

import java.util.List;

/**
 * Порча (BlockTaintFibres и Utils.setBiomeAt оригинала): порченая земля — это отдельный биом, и порча живёт только
 * в нём. Блоки порчи понемногу превращают в него соседнюю местность, а внутри расползаются волокнами, которые
 * обращают землю в порченую почву, а деревья — в корку.
 *
 * В оригинале биом менялся по одному столбцу блоков; в новых версиях биом хранится клетками 4x4x4, поэтому
 * превращается сразу столбец 4x4, а шанс распространения во столько же раз ниже.
 */
public final class Taint {
    public static final ResourceKey<Biome> BIOME = ResourceKey.create(Registries.BIOME, Thaumcraft.id("tainted_land"));
    /** Блоки самой порчи (Config.taintMaterial оригинала). */
    public static final TagKey<Block> TAINT = TagKey.create(Registries.BLOCK, Thaumcraft.id("taint"));
    /** Что волокна превращают в корку: брёвна, тыквы, кактусы. */
    public static final TagKey<Block> TO_CRUST = TagKey.create(Registries.BLOCK, Thaumcraft.id("taint_to_crust"));
    /** Что волокна превращают в порченую почву: земля, трава, песок, глина. */
    public static final TagKey<Block> TO_SOIL = TagKey.create(Registries.BLOCK, Thaumcraft.id("taint_to_soil"));
    /** Config.taintSpreadRate оригинала: чем больше, тем медленнее порча захватывает новые земли; 0 — не захватывает. */
    public static int spreadRate = 200;
    /** Во сколько раз реже срабатывает захват из-за того, что превращается столбец 4x4, а не один блок. */
    private static final int CELL = 4;

    private Taint() {
    }

    /** Порчена ли земля в этом месте. Биом читается по клетке хранения, без сглаживания границ. */
    public static boolean isTainted(LevelReader level, BlockPos pos) {
        return level.getNoiseBiome(QuartPos.fromBlock(pos.getX()), QuartPos.fromBlock(pos.getY()), QuartPos.fromBlock(pos.getZ())).is(BIOME);
    }

    /** Превращает в порченую землю весь столбец 4x4, в который попадает точка, и сообщает об этом игрокам рядом. */
    public static void setTainted(ServerLevel level, BlockPos pos) {
        Holder<Biome> taint = level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(BIOME);
        ChunkAccess chunk = level.getChunk(pos);
        int qx = QuartPos.fromBlock(pos.getX());
        int qz = QuartPos.fromBlock(pos.getZ());
        chunk.fillBiomesFromNoise((x, y, z, sampler) -> x == qx && z == qz ? taint : chunk.getNoiseBiome(x, y, z),
                level.getChunkSource().randomState().sampler());
        chunk.setUnsaved(true);
        level.getChunkSource().chunkMap.resendBiomesForChunks(List.of(chunk));
    }

    /**
     * Выплеск порчи (взрыв порченого крипера, taintSplosion первозданной сферы): десять случайных мест вокруг, половина
     * из них заражается, и там, где есть на чём расти, появляются волокна.
     *
     * @param surface искать поверхность земли, а не брать высоту самой точки
     */
    public static void splash(ServerLevel level, BlockPos centre, float radius, boolean surface) {
        RandomSource random = level.random;
        for (int a = 0; a < 10; a++) {
            int x = centre.getX() + (int) ((random.nextFloat() - random.nextFloat()) * radius);
            int z = centre.getZ() + (int) ((random.nextFloat() - random.nextFloat()) * radius);
            BlockPos pos = surface ? level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                    new BlockPos(x, 0, z)) : new BlockPos(x, centre.getY(), z);
            if (!random.nextBoolean() || isTainted(level, pos)) {
                continue;
            }
            if (random.nextInt(CELL) == 0) {
                setTainted(level, pos);
            }
            BlockPos below = pos.below();
            if (isTainted(level, pos) && level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)
                    && level.getBlockState(pos).canBeReplaced() && level.getFluidState(pos).isEmpty()) {
                level.setBlock(pos, TaintFibresBlock.stateFor(level, pos), 3);
            }
        }
    }

    public static boolean isTaint(BlockState state) {
        return state.is(TAINT);
    }

    /** Сколько из шести соседей — корка, порченая почва или волокна. */
    public static int adjacentTaint(BlockGetter level, BlockPos pos) {
        int count = 0;
        for (Direction dir : Direction.values()) {
            BlockState state = level.getBlockState(pos.relative(dir));
            if (state.getBlock() instanceof TaintBlock || state.getBlock() instanceof TaintFibresBlock
                    || state.getBlock() instanceof TaintPlantBlock) {
                count++;
            }
        }
        return count;
    }

    /** Вокруг только воздух и сама порча: волокнам не за что держаться. */
    public static boolean onlyAdjacentToTaint(BlockGetter level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockState state = level.getBlockState(pos.relative(dir));
            if (!state.isAir() && !isTaint(state)) {
                return false;
            }
        }
        return true;
    }

    public static boolean adjacentToSolid(BlockGetter level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockPos other = pos.relative(dir);
            if (level.getBlockState(other).isFaceSturdy(level, other, dir.getOpposite())) {
                return true;
            }
        }
        return false;
    }

    /** Чавкающий звук растущей порчи. */
    public static void playRoots(Level level, BlockPos pos) {
        level.playSound(null, pos, ModSounds.ROOTS.get(), SoundSource.BLOCKS, 0.1f, 0.9f + level.random.nextFloat() * 0.2f);
    }

    /** taintBiomeSpread оригинала: блок порчи, у которого рядом ещё хотя бы два таких же, заражает соседнюю землю. */
    public static void biomeSpread(ServerLevel level, BlockPos pos, RandomSource random) {
        if (spreadRate <= 0) {
            return;
        }
        BlockPos target = pos.offset(random.nextInt(3) - 1, 0, random.nextInt(3) - 1);
        if (!isTainted(level, target) && random.nextInt(spreadRate * 5 * CELL) == 0 && adjacentTaint(level, pos) >= 2) {
            setTainted(level, target);
            playRoots(level, pos);
        }
    }

    /**
     * spreadFibres оригинала: в пустом месте рядом с твёрдым блоком вырастают волокна, а изредка, если под ними
     * твёрдая земля и сверху свободно, — порченая трава, светящееся растение или споровый стебель.
     */
    public static boolean spreadFibres(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!adjacentToSolid(level, pos) || onlyAdjacentToTaint(level, pos) || !state.getFluidState().isEmpty()
                || !(state.isAir() || state.canBeReplaced() || state.is(BlockTags.FLOWERS) || state.is(BlockTags.LEAVES))
                || isTaint(state)) {
            return false;
        }
        RandomSource random = level.random;
        BlockPos below = pos.below();
        if (random.nextInt(10) == 0 && level.isEmptyBlock(pos.above()) && level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
            Block plant;
            if (random.nextInt(10) < 9) {
                plant = ModBlocks.TAINT_GRASS.get();
            } else if (random.nextInt(12) < 10) {
                plant = ModBlocks.TAINT_PLANT.get();
            } else {
                plant = ModBlocks.SPORE_STALK.get();
            }
            level.setBlock(pos, plant.defaultBlockState(), 3);
        } else {
            level.setBlock(pos, TaintFibresBlock.stateFor(level, pos), 3);
        }
        playRoots(level, pos);
        return true;
    }

    /**
     * Общий случайный тик волокон и порченых растений: рядом с ними земля заражается, сами они разрастаются,
     * а когда вокруг набирается достаточно порчи, брёвна становятся коркой, а земля — порченой почвой.
     */
    public static void growTick(ServerLevel level, BlockPos pos, RandomSource random) {
        BlockPos target = pos.offset(random.nextInt(3) - 1, random.nextInt(5) - 3, random.nextInt(3) - 1);
        if (!isTainted(level, target) || spreadFibres(level, target)) {
            return;
        }
        int adjacent = adjacentTaint(level, target);
        BlockState state = level.getBlockState(target);
        if (adjacent >= 2 && state.is(TO_CRUST)) {
            level.setBlock(target, ModBlocks.TAINT_CRUST.get().defaultBlockState(), 3);
            playRoots(level, target);
        } else if (adjacent >= 3 && state.is(TO_SOIL)) {
            level.setBlock(target, ModBlocks.TAINTED_SOIL.get().defaultBlockState(), 3);
            playRoots(level, target);
        }
    }
}
