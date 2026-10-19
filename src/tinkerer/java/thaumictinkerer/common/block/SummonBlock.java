package thaumictinkerer.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.blockentity.PedestalBlockEntity;
import thaumcraft.common.network.FxPacket;
import thaumictinkerer.common.item.SoulAspectItem;
import thaumictinkerer.common.registry.TTBlockEntities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Пентакль призыва (BlockSummon оригинала). Раз в 15 секунд, если на него не подан сигнал, ищет на пьедесталах вокруг
 * (до пяти блоков на той же высоте) три аспекта души, из которых состоит какое-нибудь существо, и призывает его.
 * Обычные аспекты при этом пропадают; наполненные остаются, но срабатывают лишь раз в минуту.
 */
public class SummonBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 2, 16);

    public SummonBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Tile(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, TTBlockEntities.SUMMON.get(), (l, p, s, tile) -> tile.tick());
    }

    /** TileSummon оригинала. */
    public static class Tile extends BlockEntity {
        public Tile(BlockPos pos, BlockState state) {
            super(TTBlockEntities.SUMMON.get(), pos, state);
        }

        void tick() {
            if (level.getGameTime() % 300 == 0 && !level.hasNeighborSignal(worldPosition)) {
                summon();
            }
        }

        /** @return true, если существо призвано */
        public boolean summon() {
            List<PedestalBlockEntity> pedestals = new ArrayList<>();
            for (int dx = -5; dx < 5; dx++) {
                for (int dz = -5; dz < 5; dz++) {
                    if (level.getBlockEntity(worldPosition.offset(dx, 0, dz)) instanceof PedestalBlockEntity pedestal
                            && pedestal.getItem().getItem() instanceof SoulAspectItem && SoulAspectItem.aspect(pedestal.getItem()) != null) {
                        pedestals.add(pedestal);
                    }
                }
            }
            for (int a = 0; a < pedestals.size(); a++) {
                for (int b = a + 1; b < pedestals.size(); b++) {
                    for (int c = b + 1; c < pedestals.size(); c++) {
                        PedestalBlockEntity[] three = {pedestals.get(a), pedestals.get(b), pedestals.get(c)};
                        EntityType<?> type = match(three);
                        if (type == null) {
                            continue;
                        }
                        boolean infused = Arrays.stream(three).allMatch(p -> ((SoulAspectItem) p.getItem().getItem()).kind()
                                == SoulAspectItem.Kind.INFUSED);
                        if (infused && level.getGameTime() % 1200 != 0) {
                            return false;
                        }
                        if (!infused) {
                            for (PedestalBlockEntity pedestal : three) {
                                pedestal.setItem(ItemStack.EMPTY);
                            }
                        }
                        spawn(type);
                        for (PedestalBlockEntity pedestal : three) {
                            FxPacket.send((ServerLevel) level, FxPacket.ESSENTIA, Vec3.atCenterOf(pedestal.getBlockPos()).add(0, 0.6, 0),
                                    Vec3.atCenterOf(worldPosition), 0x880000, 0);
                        }
                        return true;
                    }
                }
            }
            return false;
        }

        @Nullable
        private static EntityType<?> match(PedestalBlockEntity[] three) {
            List<Aspect> found = new ArrayList<>();
            for (PedestalBlockEntity pedestal : three) {
                found.add(SoulAspectItem.aspect(pedestal.getItem()));
            }
            found.sort((x, y) -> x.getTag().compareTo(y.getTag()));
            for (Map.Entry<Supplier<? extends EntityType<?>>, Aspect[]> mob : SoulAspectItem.MOBS.entrySet()) {
                List<Aspect> need = new ArrayList<>(Arrays.asList(mob.getValue()));
                need.sort((x, y) -> x.getTag().compareTo(y.getTag()));
                if (need.equals(found)) {
                    return mob.getKey().get();
                }
            }
            return null;
        }

        private void spawn(EntityType<?> type) {
            var entity = type.create(level);
            if (entity == null) {
                return;
            }
            entity.moveTo(worldPosition.getX() + 0.5, worldPosition.getY() + 1, worldPosition.getZ() + 0.5, 0, 0);
            if (entity instanceof Mob mob) {
                mob.finalizeSpawn((ServerLevel) level, level.getCurrentDifficultyAt(worldPosition), MobSpawnType.SPAWNER, null, null);
                mob.playAmbientSound();
            }
            level.addFreshEntity(entity);
        }
    }
}
