package thaumcraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import thaumcraft.client.fx.Fx;
import thaumcraft.common.registry.ModBlockEntities;
import thaumcraft.common.registry.ModBlocks;

/**
 * Мостовые путника и защиты — перенос blockCosmeticSolid:2 и 3 оригинала (с TileWardingStone и «защитной аурой»
 * blockAiry:4). Мостовая путника дарит идущему по ней скорость и прыгучесть. Мостовая защиты держит над собой
 * два блока невидимой стены, сквозь которую не пройти ни одному существу, кроме игроков, и сталкивает тех, кто
 * оказался над ней в прыжке; сигнал красного камня снимает защиту.
 */
public final class PavingStoneBlock {
    private PavingStoneBlock() {
    }

    /** Мостовая путника. */
    public static class Travel extends Block {
        public Travel(Properties properties) {
            super(properties);
        }

        @Override
        public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
            if (entity instanceof LivingEntity living) {
                if (level.isClientSide) {
                    Fx.blockSparkle(pos.getX(), pos.getY(), pos.getZ(), 32768, 5);
                }
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 1));
                living.addEffect(new MobEffectInstance(MobEffects.JUMP, 40, 0));
            }
            super.stepOn(level, pos, state, entity);
        }
    }

    /** Мостовая защиты. */
    public static class Ward extends BaseEntityBlock {
        public Ward(Properties properties) {
            super(properties);
        }

        @Override
        public RenderShape getRenderShape(BlockState state) {
            return RenderShape.MODEL;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new Stone(pos, state);
        }

        @Nullable
        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
            return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.WARDING_STONE.get(), Stone::tick);
        }

        /** Руны над камнем: синие — защита снята сигналом, красные — над камнем нечем стоять стене, лиловые — кто-то упёрся. */
        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            double x = pos.getX();
            double y = pos.getY() + 0.7;
            double z = pos.getZ();
            if (level.hasNeighborSignal(pos)) {
                for (int i = 0; i < 2; i++) {
                    Fx.blockRunes(x, y, z, 0.2f + random.nextFloat() * 0.4f, random.nextFloat() * 0.3f, 0.8f + random.nextFloat() * 0.2f, 20, -0.02f);
                }
            } else if (!level.getBlockState(pos.above()).is(ModBlocks.WARD_AURA.get()) && level.getBlockState(pos.above()).canBeReplaced()
                    || !level.getBlockState(pos.above(2)).is(ModBlocks.WARD_AURA.get()) && level.getBlockState(pos.above()).canBeReplaced()) {
                for (int i = 0; i < 3; i++) {
                    Fx.blockRunes(x, y, z, 0.9f + random.nextFloat() * 0.1f, random.nextFloat() * 0.3f, random.nextFloat() * 0.3f, 24, -0.02f);
                }
            } else {
                for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(1.0))) {
                    if (!(entity instanceof Player)) {
                        Fx.blockRunes(x, pos.getY() + 0.6 + random.nextFloat() * Math.max(0.8f, entity.getEyeHeight()), z,
                                0.6f + random.nextFloat() * 0.4f, 0f, 0.3f + random.nextFloat() * 0.7f, 20, 0f);
                        break;
                    }
                }
            }
        }
    }

    public static class Stone extends BlockEntity {
        private int count;

        public Stone(BlockPos pos, BlockState state) {
            super(ModBlockEntities.WARDING_STONE.get(), pos, state);
        }

        static void tick(Level level, BlockPos pos, BlockState state, Stone stone) {
            if (stone.count == 0) {
                stone.count = level.random.nextInt(100);
            }
            boolean powered = level.hasNeighborSignal(pos);
            if (stone.count % 5 == 0 && !powered) {
                // сталкиваем тех, кто завис над камнем в прыжке
                for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).expandTowards(0, 2, 0).inflate(0.1))) {
                    if (!entity.onGround() && !(entity instanceof Player)) {
                        float yaw = (entity.getYRot() + 180f) * ((float) Math.PI / 180f);
                        entity.push(-Math.sin(yaw) * 0.2, -0.1, Math.cos(yaw) * 0.2);
                    }
                }
            }
            if (++stone.count % 100 == 0) {
                for (int up = 1; up <= 2; up++) {
                    BlockPos above = pos.above(up);
                    BlockState there = level.getBlockState(above);
                    if (!there.is(ModBlocks.WARD_AURA.get()) && there.canBeReplaced() && there.getFluidState().isEmpty()) {
                        level.setBlock(above, ModBlocks.WARD_AURA.get().defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    /** Невидимая стена над мостовой защиты (blockAiry:4 оригинала). */
    public static class Aura extends Block {
        public Aura(Properties properties) {
            super(properties);
        }

        @Nullable
        private static BlockPos stone(BlockGetter level, BlockPos pos) {
            for (int down = 1; down <= 2; down++) {
                if (level.getBlockState(pos.below(down)).is(ModBlocks.WARDING_PAVING.get())) {
                    return pos.below(down);
                }
            }
            return null;
        }

        private static boolean open(BlockGetter level, BlockPos pos) {
            BlockPos stone = stone(level, pos);
            return stone == null || level instanceof Level real && real.hasNeighborSignal(stone);
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
        public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            if (context instanceof EntityCollisionContext entityContext && entityContext.getEntity() instanceof LivingEntity living
                    && !(living instanceof Player) && !open(level, pos)) {
                return Shapes.block();
            }
            return Shapes.empty();
        }

        /** Для поиска пути стена проходима, только пока защита снята. */
        @Override
        public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
            return open(level, pos);
        }

        @Override
        public boolean canBeReplaced(BlockState state, net.minecraft.world.item.context.BlockPlaceContext context) {
            return true;
        }

        @Override
        public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
            level.scheduleTick(pos, this, 100);
        }

        /** Без камня под собой стена исчезает (TileWardingStoneFence). */
        @Override
        public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            if (stone(level, pos) == null) {
                level.removeBlock(pos, false);
            } else {
                level.scheduleTick(pos, this, 100);
            }
        }
    }
}
