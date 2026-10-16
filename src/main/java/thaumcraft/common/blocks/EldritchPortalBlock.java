package thaumcraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.util.ITeleporter;
import org.jetbrains.annotations.Nullable;
import thaumcraft.common.registry.ModBlockEntities;
import thaumcraft.common.registry.ModSounds;
import thaumcraft.common.research.PlayerKnowledge;
import thaumcraft.common.research.ResearchHelper;
import thaumcraft.common.world.outer.Cell;
import thaumcraft.common.world.outer.Labyrinth;
import thaumcraft.common.world.outer.OuterLands;

import java.util.function.Function;

/**
 * Портал Элдрича — перенос BlockEldritchPortal, TileEldritchPortal и TeleporterThaumcraft оригинала. Появляется над
 * алтарём с четырьмя очами, когда жезл раскрывает тёмный узел (исследование OCULUS), и в зале-портале лабиринта.
 * Игрок, вошедший в него, переносится в Запредельные земли, к порталу своего лабиринта (впервые — с открытием
 * ENTEROUTER), а из Запредельных земель — обратно к алтарю. Портал держится, только пока над и под ним стоят
 * блоки Элдрича.
 */
public class EldritchPortalBlock extends Block implements EntityBlock {
    public EldritchPortalBlock(Properties properties) {
        super(properties);
    }

    @Override
    @SuppressWarnings("deprecation")
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return ItemStack.EMPTY;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean moving) {
        if (!isEldritch(level.getBlockState(pos.above())) || !isEldritch(level.getBlockState(pos.below()))) {
            level.removeBlock(pos, false);
        }
    }

    private static boolean isEldritch(BlockState state) {
        return state.getBlock() instanceof EldritchAltarBlock || state.getBlock() instanceof EldritchObeliskBlock;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Portal(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == ModBlockEntities.ELDRITCH_PORTAL.get() ? (l, p, s, be) -> ((Portal) be).tick() : null;
    }

    public static class Portal extends BlockEntity {
        /** Сколько тиков портал раскрывается (на клиенте, до 30). */
        public int opencount = -1;
        private int count;

        public Portal(BlockPos pos, BlockState state) {
            super(ModBlockEntities.ELDRITCH_PORTAL.get(), pos, state);
        }

        void tick() {
            count++;
            if (level.isClientSide) {
                if (count % 250 == 0 || count == 1) {
                    level.playLocalSound(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, ModSounds.EVIL_PORTAL.get(),
                            SoundSource.BLOCKS, 1f, 1f, false);
                }
                if (opencount < 30) {
                    opencount++;
                }
                return;
            }
            if (count % 5 != 0) {
                return;
            }
            for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, new AABB(worldPosition).inflate(0.5, 1.0, 0.5))) {
                if (player.isPassenger() || player.isVehicle()) {
                    continue;
                }
                // timeUntilPortal = 100 оригинала: пока игрок стоит у портала, он не перенесётся обратно
                if (player.isOnPortalCooldown()) {
                    player.setPortalCooldown(100);
                    continue;
                }
                player.setPortalCooldown(100);
                ServerLevel target = player.server.getLevel(OuterLands.is(level) ? Level.OVERWORLD : OuterLands.LEVEL);
                if (target == null) {
                    continue;
                }
                boolean entering = !OuterLands.is(level);
                player.changeDimension(target, new Teleporter(worldPosition));
                if (entering && !PlayerKnowledge.get(player).isResearchComplete("ENTEROUTER")) {
                    ResearchHelper.complete(player, "ENTEROUTER");
                }
            }
        }

        @Override
        public AABB getRenderBoundingBox() {
            return new AABB(worldPosition).inflate(1.0);
        }
    }

    /**
     * TeleporterThaumcraft: ищет портал на той стороне. Порталы лабиринта и алтаря стоят в одном и том же чанке
     * (лабиринт строится вокруг чанка кольца), поэтому искать надо рядом — в Запредельных землях по клеткам лабиринта,
     * дома — среди блоков-сущностей окрестных чанков. Игрок встаёт на блок в сторону от портала.
     */
    private record Teleporter(BlockPos from) implements ITeleporter {
        @Override
        public Entity placeEntity(Entity entity, ServerLevel current, ServerLevel destination, float yaw, Function<Boolean, Entity> repositionEntity) {
            return repositionEntity.apply(false);
        }

        @Override
        public PortalInfo getPortalInfo(Entity entity, ServerLevel destination, Function<ServerLevel, PortalInfo> defaultPortalInfo) {
            BlockPos found = OuterLands.is(destination) ? findOuter(destination) : findHome(destination);
            Vec3 at;
            if (found != null) {
                at = new Vec3(found.getX() + 0.5 + (destination.random.nextBoolean() ? 1 : -1), found.getY(),
                        found.getZ() + 0.5 + (destination.random.nextBoolean() ? 1 : -1));
            } else {
                // портала нет — домой к точке возрождения
                BlockPos spawn = destination.getSharedSpawnPos();
                at = Vec3.atBottomCenterOf(destination.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, spawn));
            }
            return new PortalInfo(at, Vec3.ZERO, entity.getYRot(), entity.getXRot());
        }

        /** Ближайший к точке входа зал-портал лабиринта (особенность 1) в пределах 128 блоков. */
        @Nullable
        private BlockPos findOuter(ServerLevel destination) {
            Labyrinth maze = Labyrinth.get(destination.getServer());
            int cx = from.getX() >> 4;
            int cz = from.getZ() >> 4;
            BlockPos best = null;
            double bestDistance = Double.MAX_VALUE;
            for (int x = -8; x <= 8; x++) {
                for (int z = -8; z <= 8; z++) {
                    Cell cell = maze.get(cx + x, cz + z);
                    if (cell != null && cell.feature == 1) {
                        BlockPos portal = new BlockPos((cx + x) * 16 + 8, OuterLands.FLOOR + 3, (cz + z) * 16 + 8);
                        double d = portal.distSqr(from);
                        if (d < bestDistance) {
                            bestDistance = d;
                            best = portal;
                        }
                    }
                }
            }
            if (best != null) {
                // зал строится вместе с чанком; заранее загружаем его, чтобы игрок не упал в пустоту
                destination.getChunk(best);
            }
            return best;
        }

        /** Портал у алтаря в окрестных чанках. */
        @Nullable
        private BlockPos findHome(ServerLevel destination) {
            int cx = from.getX() >> 4;
            int cz = from.getZ() >> 4;
            BlockPos best = null;
            double bestDistance = Double.MAX_VALUE;
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    LevelChunk chunk = destination.getChunk(cx + x, cz + z);
                    for (BlockEntity be : chunk.getBlockEntities().values()) {
                        if (be instanceof Portal) {
                            double d = be.getBlockPos().distSqr(new BlockPos(from.getX(), be.getBlockPos().getY(), from.getZ()));
                            if (d < bestDistance) {
                                bestDistance = d;
                                best = be.getBlockPos();
                            }
                        }
                    }
                }
            }
            return best;
        }
    }
}
