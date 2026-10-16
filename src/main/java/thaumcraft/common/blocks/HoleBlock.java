package thaumcraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import thaumcraft.Thaumcraft;
import thaumcraft.common.network.FxPacket;
import thaumcraft.common.registry.ModBlockEntities;
import thaumcraft.common.registry.ModBlocks;

/**
 * Дыра от фокуса «Портативная дыра» (BlockHole и TileHole оригинала): на несколько секунд занимает место блока,
 * пропускает сквозь себя всё и всех, а потом возвращает прежний блок. Дыра сама растёт вглубь и вширь:
 * каждая клетка тоннеля окружает себя дырами 3x3 и передаёт эстафету дальше.
 */
public class HoleBlock extends BaseEntityBlock {
    /** Блоки, которые дыра не трогает (portableHoleBlackList оригинала): кровати, поршни, двери и подобное. */
    public static final TagKey<Block> BLACKLIST = TagKey.create(Registries.BLOCK, Thaumcraft.id("portable_hole_blacklist"));

    public HoleBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Hole(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.HOLE.get(), level.isClientSide ? Hole::clientTick : Hole::serverTick);
    }

    /** Можно ли продырявить этот блок. */
    public static boolean canPierce(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return !state.isAir() && !state.is(BLACKLIST) && !state.is(Blocks.BEDROCK) && !state.is(ModBlocks.HOLE.get())
                && state.getDestroySpeed(level, pos) != -1f;
    }

    /**
     * createHole оригинала.
     *
     * @param direction куда тоннель растёт дальше (внутрь от задетой грани); null — клетка-стенка, дальше не растёт
     * @param count     сколько клеток тоннеля ещё осталось, считая эту
     * @param duration  через сколько тиков блок вернётся
     */
    public static boolean createHole(Level level, BlockPos pos, @Nullable Direction direction, int count, int duration) {
        if (level.isClientSide || level.getBlockEntity(pos) != null || !canPierce(level, pos)) {
            return false;
        }
        BlockState old = level.getBlockState(pos);
        // без оповещения соседей: факелы и рычаги на исчезнувшем блоке остаются висеть, как в оригинале
        if (!level.setBlock(pos, ModBlocks.HOLE.get().defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE)) {
            return false;
        }
        if (level.getBlockEntity(pos) instanceof Hole hole) {
            hole.oldState = old;
            hole.direction = direction;
            hole.count = count;
            hole.countdownMax = duration;
            hole.setChanged();
            level.sendBlockUpdated(pos, old, level.getBlockState(pos), 3);
        }
        FxPacket.blockSparkle((ServerLevel) level, pos, 0x400040, 1);
        return true;
    }

    /** TileHole оригинала. */
    public static class Hole extends BlockEntity {
        private BlockState oldState = Blocks.AIR.defaultBlockState();
        private int countdown;
        private int countdownMax = 120;
        private int count;
        @Nullable
        private Direction direction;

        public Hole(BlockPos pos, BlockState state) {
            super(ModBlockEntities.HOLE.get(), pos, state);
        }

        public static void serverTick(Level level, BlockPos pos, BlockState state, Hole hole) {
            if (hole.countdown == 0 && hole.count > 1 && hole.direction != null) {
                // стенки тоннеля: восемь клеток вокруг, в плоскости поперёк направления
                for (int a = -1; a <= 1; a++) {
                    for (int b = -1; b <= 1; b++) {
                        if (a == 0 && b == 0) {
                            continue;
                        }
                        BlockPos ring = switch (hole.direction.getAxis()) {
                            case Y -> pos.offset(a, 0, b);
                            case Z -> pos.offset(a, b, 0);
                            case X -> pos.offset(0, a, b);
                        };
                        createHole(level, ring, null, 1, hole.countdownMax);
                    }
                }
                if (!createHole(level, pos.relative(hole.direction), hole.direction, hole.count - 1, hole.countdownMax)) {
                    hole.count = 0;
                }
            }
            if (++hole.countdown >= hole.countdownMax) {
                FxPacket.blockSparkle((ServerLevel) level, pos, 0x400040, 1);
                BlockState back = hole.oldState;
                level.setBlock(pos, back, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                level.scheduleTick(pos, back.getBlock(), 2);
            }
        }

        /** Искры по кромкам дыры, там где она граничит с целой стеной (surroundwithsparkles оригинала). */
        public static void clientTick(Level level, BlockPos pos, BlockState state, Hole hole) {
            for (Direction wall : Direction.values()) {
                BlockPos wallPos = pos.relative(wall);
                if (!level.getBlockState(wallPos).isSolidRender(level, wallPos)) {
                    continue;
                }
                for (Direction open : Direction.values()) {
                    if (open.getAxis() == wall.getAxis() || level.random.nextInt(4) != 0) {
                        continue;
                    }
                    BlockPos openPos = pos.relative(open);
                    BlockState openState = level.getBlockState(openPos);
                    if (openState.isSolidRender(level, openPos) || openState.is(ModBlocks.HOLE.get())) {
                        continue;
                    }
                    // случайная точка на ребре между стеной и открытой стороной
                    double x = pos.getX() + 0.5 + wall.getStepX() * 0.5 + open.getStepX() * 0.5;
                    double y = pos.getY() + 0.5 + wall.getStepY() * 0.5 + open.getStepY() * 0.5;
                    double z = pos.getZ() + 0.5 + wall.getStepZ() * 0.5 + open.getStepZ() * 0.5;
                    double along = level.random.nextFloat() - 0.5;
                    if (wall.getAxis() != Direction.Axis.X && open.getAxis() != Direction.Axis.X) {
                        x += along;
                    } else if (wall.getAxis() != Direction.Axis.Y && open.getAxis() != Direction.Axis.Y) {
                        y += along;
                    } else {
                        z += along;
                    }
                    thaumcraft.client.fx.Fx.sparkle(x, y, z, 1f, 2, 0f);
                }
            }
        }

        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            tag.put("OldState", NbtUtils.writeBlockState(oldState));
            tag.putInt("Countdown", countdown);
            tag.putInt("CountdownMax", countdownMax);
            tag.putInt("Count", count);
            tag.putInt("Direction", direction == null ? -1 : direction.get3DDataValue());
        }

        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            if (level != null) {
                oldState = NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK), tag.getCompound("OldState"));
            } else {
                oldState = NbtUtils.readBlockState(net.minecraft.core.registries.BuiltInRegistries.BLOCK.asLookup(), tag.getCompound("OldState"));
            }
            countdown = tag.getInt("Countdown");
            countdownMax = tag.contains("CountdownMax") ? tag.getInt("CountdownMax") : 120;
            count = tag.getInt("Count");
            int side = tag.getInt("Direction");
            direction = side < 0 ? null : Direction.from3DDataValue(side);
        }

        @Override
        public CompoundTag getUpdateTag() {
            return saveWithoutMetadata();
        }

        @Override
        public Packet<ClientGamePacketListener> getUpdatePacket() {
            return ClientboundBlockEntityDataPacket.create(this);
        }
    }
}
