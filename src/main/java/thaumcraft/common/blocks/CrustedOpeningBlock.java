package thaumcraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import thaumcraft.common.entity.EldritchCrab;
import thaumcraft.common.registry.ModBlockEntities;
import thaumcraft.common.registry.ModEntities;
import thaumcraft.common.registry.ModSounds;

/**
 * Покрытое коркой отверстие — перенос BlockEldritch (мета 9) и TileEldritchCrabSpawner оригинала: из стен
 * лабиринта Запредельных земель торчит мясистая отдушина. Пока рядом (16 блоков) есть игрок, а крабов в округе
 * не больше пяти, она раз в восемь–двенадцать секунд с шипением и паром выплёвывает краба Элдрича без шлема.
 * Ломается тяжело и даёт опыт.
 */
public class CrustedOpeningBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public CrustedOpeningBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public int getExpDrop(BlockState state, LevelReader level, net.minecraft.util.RandomSource random, BlockPos pos, int fortune, int silkTouch) {
        return silkTouch == 0 ? UniformInt.of(6, 10).sample(random) : 0;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Spawner(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == ModBlockEntities.CRUSTED_OPENING.get() ? (l, p, s, be) -> ((Spawner) be).tick() : null;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
        super.triggerEvent(state, level, pos, id, param);
        BlockEntity be = level.getBlockEntity(pos);
        return be != null && be.triggerEvent(id, param);
    }

    @Override
    public boolean isValidSpawn(BlockState state, BlockGetter level, BlockPos pos, net.minecraft.world.entity.SpawnPlacements.Type type,
                                net.minecraft.world.entity.EntityType<?> entityType) {
        return false;
    }

    public static class Spawner extends BlockEntity {
        private int count = 150;
        private int ticks;
        private int venting;

        public Spawner(BlockPos pos, BlockState state) {
            super(ModBlockEntities.CRUSTED_OPENING.get(), pos, state);
        }

        public Direction facing() {
            return getBlockState().getValue(FACING);
        }

        void tick() {
            if (ticks == 0) {
                ticks = level.random.nextInt(500);
            }
            ticks++;
            if (!level.isClientSide) {
                count--;
                if (count < 0) {
                    count = 50 + level.random.nextInt(50);
                    return;
                }
                if (count == 15 && isActivated() && !maxEntitiesReached()) {
                    level.blockEvent(worldPosition, getBlockState().getBlock(), 1, 0);
                    level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5f, 1f);
                }
                if (count <= 0 && isActivated() && !maxEntitiesReached()) {
                    count = 150 + level.random.nextInt(100);
                    spawnCrab();
                    level.playSound(null, worldPosition, ModSounds.GORE.get(), SoundSource.BLOCKS, 0.5f, 1f);
                }
            } else if (venting > 0) {
                venting--;
                for (int a = 0; a < 3; a++) {
                    drawVent();
                }
            } else if (level.random.nextInt(20) == 0) {
                drawVent();
            }
        }

        /** drawVentParticles оригинала: розоватый пар (цвет 10061994) из отдушины. */
        private void drawVent() {
            Direction dir = facing();
            var random = level.random;
            float fx = 0.15f - random.nextFloat() * 0.3f;
            float fy = 0.15f - random.nextFloat() * 0.3f;
            float fz = 0.15f - random.nextFloat() * 0.3f;
            float fx2 = 0.1f - random.nextFloat() * 0.2f;
            float fy2 = 0.1f - random.nextFloat() * 0.2f;
            float fz2 = 0.1f - random.nextFloat() * 0.2f;
            thaumcraft.client.fx.Fx.vent(worldPosition.getX() + 0.5 + fx + dir.getStepX() / 2.1, worldPosition.getY() + 0.5 + fy + dir.getStepY() / 2.1,
                    worldPosition.getZ() + 0.5 + fz + dir.getStepZ() / 2.1, dir.getStepX() / 3.0 + fx2, dir.getStepY() / 3.0 + fy2,
                    dir.getStepZ() / 3.0 + fz2, 10061994, 2f);
        }

        @Override
        public boolean triggerEvent(int id, int param) {
            if (id == 1) {
                venting = 20;
                return true;
            }
            return super.triggerEvent(id, param);
        }

        private boolean maxEntitiesReached() {
            return level.getEntitiesOfClass(EldritchCrab.class, new AABB(worldPosition).inflate(32.0)).size() > 5;
        }

        private boolean isActivated() {
            return level.getNearestPlayer(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, 16.0, false) != null;
        }

        private void spawnCrab() {
            Direction dir = facing();
            EldritchCrab crab = ModEntities.ELDRITCH_CRAB.get().create(level);
            if (crab == null) {
                return;
            }
            BlockPos at = worldPosition.relative(dir);
            crab.moveTo(at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 0f, 0f);
            crab.finalizeSpawn((net.minecraft.server.level.ServerLevel) level, level.getCurrentDifficultyAt(at), MobSpawnType.SPAWNER, null, null);
            crab.setHelm(false);
            crab.setDeltaMovement(dir.getStepX() * 0.2, dir.getStepY() * 0.2, dir.getStepZ() * 0.2);
            level.addFreshEntity(crab);
        }

        @Override
        public AABB getRenderBoundingBox() {
            return new AABB(worldPosition).inflate(1.0);
        }
    }
}
