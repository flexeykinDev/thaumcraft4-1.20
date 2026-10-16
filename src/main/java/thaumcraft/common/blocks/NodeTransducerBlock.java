package thaumcraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.blockentity.NodeBlockEntity;
import thaumcraft.common.network.FxPacket;
import thaumcraft.common.registry.ModBlockEntities;
import thaumcraft.common.registry.ModBlocks;
import thaumcraft.common.registry.ModSounds;

import java.util.List;

/**
 * Преобразователь узла (BlockStoneDevice оригинала, мета 11). Ставится над узлом ауры, под которым стоит
 * стабилизатор. Под сигналом красного камня за 50 секунд вытягивает из узла весь запас и превращает его
 * в заряженный узел — источник сети вис. Без сигнала узел за те же 50 секунд возвращается в обычный, но пустой.
 * Убрать стабилизатор из-под заряженного узла — значит взорвать его.
 */
public class NodeTransducerBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = box(0, 4, 0, 16, 16, 16);

    public NodeTransducerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Transducer(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.NODE_TRANSDUCER.get(), Transducer::tick);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
        if (level.getBlockEntity(pos) instanceof Transducer transducer) {
            transducer.checkStatus();
        }
    }

    /** Если преобразователь сломали, пока узел заряжен, узел не выдерживает. */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos.below()) instanceof NodeBlockEntity node && node.isEnergized()) {
            explodify(level, pos.below());
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    /** BlockAiry.explodify оригинала: узел гибнет во взрыве и разбрасывает вокруг флюс. */
    public static void explodify(Level level, BlockPos pos) {
        if (level.isClientSide) {
            return;
        }
        level.removeBlock(pos, false);
        level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 3f, Level.ExplosionInteraction.NONE);
        for (int i = 0; i < 50; i++) {
            BlockPos spot = pos.offset(level.random.nextInt(8) - level.random.nextInt(8), level.random.nextInt(8) - level.random.nextInt(8),
                    level.random.nextInt(8) - level.random.nextInt(8));
            if (level.isEmptyBlock(spot)) {
                level.setBlock(spot, (spot.getY() < pos.getY() ? ModBlocks.FLUX_GOO : ModBlocks.FLUX_GAS).get().defaultBlockState(), 3);
            }
        }
    }

    /** TileNodeConverter оригинала. */
    public static class Transducer extends BlockEntity {
        /** Полный заряд: 1000 тиков. */
        public static final int FULL = 1000;
        /** 0 — простаивает, 1 — заряжает обычный узел, 2 — держит заряженный. */
        public int status;
        public int count = -1;

        public Transducer(BlockPos pos, BlockState state) {
            super(ModBlockEntities.NODE_TRANSDUCER.get(), pos, state);
        }

        private boolean powered() {
            return level != null && level.hasNeighborSignal(worldPosition);
        }

        private boolean hasStabilizer() {
            return level != null && NodeStabilizerBlock.lockAt(level, worldPosition.below(2)) > 0;
        }

        @Nullable
        private NodeBlockEntity node() {
            return level != null && level.getBlockEntity(worldPosition.below()) instanceof NodeBlockEntity node ? node : null;
        }

        private void sync() {
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }

        public void checkStatus() {
            if (level == null || level.isClientSide) {
                return;
            }
            if (count == -1) {
                count = 0;
            }
            NodeBlockEntity node = node();
            boolean energized = node != null && node.isEnergized();
            if (status == 2 && count > 50 && !(hasStabilizer() && energized)) {
                // заряженный узел остался без стабилизатора
                explodify(level, worldPosition.below());
                status = 0;
                count = 50;
                sync();
                return;
            }
            if (powered() && node != null && !energized && node.isAuraNode() && hasStabilizer()) {
                status = 1;
            } else if (energized) {
                status = 2;
                count = FULL;
            } else {
                status = 0;
            }
            sync();
        }

        private void burst() {
            if (level instanceof ServerLevel server) {
                FxPacket.blockSparkle(server, worldPosition.below(), 0xffffff, 8);
                server.playSound(null, worldPosition.below(), ModSounds.CRAFT_FAIL.get(), SoundSource.BLOCKS, 0.5f, 1f);
            }
        }

        public static void tick(Level level, BlockPos pos, BlockState state, Transducer tile) {
            if (tile.count == -1) {
                tile.checkStatus();
                if (level.isClientSide) {
                    tile.count = Math.max(0, tile.count);
                }
            }
            if (!level.isClientSide) {
                NodeBlockEntity node = tile.node();
                if (tile.status == 1 && tile.count >= FULL && node != null && !node.isEnergized()) {
                    node.energize();
                    tile.checkStatus();
                    tile.burst();
                }
                if (tile.status == 2 && tile.count <= 50 && node != null && node.isEnergized()) {
                    node.deenergize();
                    tile.status = 0;
                    tile.burst();
                    tile.sync();
                }
                // стабилизатор могли убрать или выключить без оповещения
                if (tile.status == 2 && level.getGameTime() % 20 == 0 && !tile.hasStabilizer()) {
                    tile.checkStatus();
                }
            }
            if (tile.status != 0 && tile.powered()) {
                if (tile.count < FULL) {
                    tile.count++;
                    if (!level.isClientSide) {
                        // пока идёт зарядка, преобразователь по единице выкачивает из узла его запас
                        NodeBlockEntity node = tile.node();
                        if (node != null && !node.isEnergized()) {
                            List<Aspect> aspects = node.getAspects().stream().filter(a -> node.getAmount(a) > 0).toList();
                            if (!aspects.isEmpty()) {
                                node.takeFromContainer(aspects.get(level.random.nextInt(aspects.size())), 1);
                                if (tile.count % 5 == 0 || node.visSize() == 0) {
                                    node.nodeChange();
                                }
                            }
                        }
                        if (tile.count % 20 == 0) {
                            tile.sync();
                        }
                    }
                    tile.bolts(level);
                }
            } else if (tile.count > 0) {
                tile.count--;
                if (!level.isClientSide && tile.count % 20 == 0) {
                    tile.sync();
                }
                tile.bolts(level);
            }
            if (tile.count > FULL) {
                tile.count = FULL;
            }
        }

        /** Молнии от преобразователя и от стабилизатора к узлу, пока тот заряжается или разряжается. */
        private void bolts(Level level) {
            if (!level.isClientSide || count <= 50) {
                return;
            }
            double x = worldPosition.getX();
            double y = worldPosition.getY();
            double z = worldPosition.getZ();
            if (level.random.nextBoolean()) {
                thaumcraft.client.fx.Fx.bolt(x + 0.25 + level.random.nextFloat() * 0.5, y + 0.5, z + 0.25 + level.random.nextFloat() * 0.5,
                        x + 0.5, y - 0.5, z + 0.5, 0);
            }
            if (level.random.nextBoolean() && hasStabilizer()) {
                thaumcraft.client.fx.Fx.bolt(x + 0.25 + level.random.nextFloat() * 0.5, y - 1.5, z + 0.25 + level.random.nextFloat() * 0.5,
                        x + 0.5, y - 0.5, z + 0.5, 0);
            }
        }

        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            tag.putInt("status", status);
            tag.putInt("count", count);
        }

        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            status = tag.getInt("status");
            count = tag.getInt("count");
        }

        @Override
        public CompoundTag getUpdateTag() {
            return saveWithoutMetadata();
        }

        @Override
        public Packet<ClientGamePacketListener> getUpdatePacket() {
            return ClientboundBlockEntityDataPacket.create(this);
        }

        @Override
        public AABB getRenderBoundingBox() {
            return new AABB(worldPosition.below(), worldPosition.offset(1, 1, 1));
        }

        /** Центр узла под преобразователем — для отрисовки. */
        public Vec3 nodeCenter() {
            return Vec3.atCenterOf(worldPosition.below());
        }
    }
}
