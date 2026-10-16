package thaumcraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.visnet.VisNetHandler;
import thaumcraft.api.visnet.VisNode;
import thaumcraft.api.visnet.VisNodeLink;
import thaumcraft.common.registry.ModBlockEntities;
import thaumcraft.common.registry.ModSounds;

import java.util.EnumMap;
import java.util.Map;

/**
 * Реле вис (BlockMetalDevice оригинала, мета 14). Цепляется к заряженному узлу или другому реле в пределах
 * 8 блоков прямой видимости и передаёт вис приборам рядом с собой. Касание жезлом настраивает реле на один
 * из шести первичных аспектов: настроенные по-разному реле друг с другом не соединяются.
 */
public class VisRelayBlock extends BaseEntityBlock {
    /** Куда смотрит кристалл: от опоры наружу. */
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

    static {
        SHAPES.put(Direction.UP, box(5, 0, 5, 11, 8, 11));
        SHAPES.put(Direction.DOWN, box(5, 8, 5, 11, 16, 11));
        SHAPES.put(Direction.SOUTH, box(5, 5, 0, 11, 11, 8));
        SHAPES.put(Direction.NORTH, box(5, 5, 8, 11, 11, 16));
        SHAPES.put(Direction.EAST, box(0, 5, 5, 8, 11, 11));
        SHAPES.put(Direction.WEST, box(8, 5, 5, 16, 11, 11));
    }

    public VisRelayBlock(Properties properties) {
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
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Relay(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.VIS_RELAY.get(), level.isClientSide ? Relay::clientTick : Relay::serverTick);
    }

    @Override
    public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
        return false;
    }

    /** TileVisRelay оригинала. */
    public static class Relay extends BlockEntity implements VisNode {
        /** Цвета шести настроек: воздух, огонь, вода, земля, порядок, энтропия. */
        public static final int[] COLORS = {0xffff7e, 0xff3c01, 0x0090ff, 0x00a000, 0xeeccff, 0x555577};
        private static final Aspect[] ATTUNED = {Aspect.AIR, Aspect.FIRE, Aspect.WATER, Aspect.EARTH, Aspect.ORDER, Aspect.ENTROPY};

        private final VisNodeLink link = new VisNodeLink();
        private byte color = -1;
        // положение родителя относительно реле: по нему клиент рисует луч
        private BlockPos parentOffset = BlockPos.ZERO;
        // только на клиенте: цвет и сила вспышки, пробегающей по лучу, когда через реле идёт вис
        public int pulse;
        public float pRed = 0.5f;
        public float pGreen = 0.5f;
        public float pBlue = 0.5f;

        public Relay(BlockPos pos, BlockState state) {
            this(ModBlockEntities.VIS_RELAY.get(), pos, state);
        }

        /** Для разновидностей реле со своим типом (зарядное реле верстака). */
        protected Relay(BlockEntityType<?> type, BlockPos pos, BlockState state) {
            super(type, pos, state);
        }

        public byte getColor() {
            return color;
        }

        public Direction getFacing() {
            return getBlockState().getValue(FACING);
        }

        /** Есть ли у реле связь с сетью. */
        public boolean isConnected() {
            return !parentOffset.equals(BlockPos.ZERO);
        }

        /** Реле, к которому подключено это (на клиенте), либо null. */
        @Nullable
        public BlockPos getParentPos() {
            return isConnected() ? worldPosition.subtract(parentOffset) : null;
        }

        // ---------- звено сети ----------

        @Override
        public VisNodeLink visLink() {
            return link;
        }

        @Override
        public BlockPos visPos() {
            return worldPosition;
        }

        @Override
        public Level visLevel() {
            return level;
        }

        @Override
        public boolean isVisNodeRemoved() {
            return isRemoved();
        }

        @Override
        public int getVisRange() {
            return 8;
        }

        @Override
        public boolean isVisSource() {
            return false;
        }

        @Override
        public byte getAttunement() {
            return color;
        }

        @Override
        public int consumeVis(Aspect aspect, int amount) {
            return link.consumeFromParent(this, aspect, amount);
        }

        @Override
        public void parentChanged() {
            VisNode parent = link.getParent();
            BlockPos offset = parent == null ? BlockPos.ZERO : worldPosition.subtract(parent.visPos());
            if (!offset.equals(parentOffset)) {
                parentOffset = offset;
                sync();
            }
        }

        /** Через реле прошла вис: по лучу к источнику пробегает вспышка цвета аспекта. */
        @Override
        public void triggerConsumeEffect(Aspect aspect) {
            if (level == null || pulse > 0) {
                return;
            }
            for (int i = 0; i < ATTUNED.length; i++) {
                if (ATTUNED[i] == aspect) {
                    pulse = 5;
                    level.blockEvent(worldPosition, getBlockState().getBlock(), 0, i);
                    return;
                }
            }
        }

        @Override
        public boolean triggerEvent(int id, int type) {
            if (id != 0) {
                return super.triggerEvent(id, type);
            }
            if (level != null && level.isClientSide && type >= 0 && type < COLORS.length) {
                setPulse(COLORS[type]);
                // вспышка идёт по всей цепочке до источника
                BlockPos up = getParentPos();
                int guard = 0;
                while (up != null && guard++ < 64 && level.getBlockEntity(up) instanceof Relay relay && relay.pulse == 0) {
                    relay.setPulse(COLORS[type]);
                    up = relay.getParentPos();
                }
            }
            return true;
        }

        private void setPulse(int rgb) {
            pulse = 5;
            pRed = (rgb >> 16 & 255) / 255f;
            pGreen = (rgb >> 8 & 255) / 255f;
            pBlue = (rgb & 255) / 255f;
        }

        // ---------- работа ----------

        private void sync() {
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }

        public static void serverTick(Level level, BlockPos pos, BlockState state, Relay relay) {
            if (relay.pulse > 0) {
                relay.pulse--;
            }
            relay.link.serverTick(relay);
            // раз в секунду реле записывает за собой игроков в пяти блоках, если оно к ним ближе прежнего
            if (++relay.playerScan % 20 == 0) {
                net.minecraft.world.phys.Vec3 centre = net.minecraft.world.phys.Vec3.atCenterOf(pos);
                for (net.minecraft.world.entity.player.Player player : level.getEntitiesOfClass(
                        net.minecraft.world.entity.player.Player.class, new net.minecraft.world.phys.AABB(pos).inflate(5.0))) {
                    Relay known = nearestTo(player);
                    if (known == null || !(known.distanceSqTo(player) < player.distanceToSqr(centre))) {
                        NEARBY_PLAYERS.put(player.getUUID(), new java.lang.ref.WeakReference<>(relay));
                    }
                }
            }
        }

        /** nearbyPlayers оригинала: к какому реле привязан игрок. */
        private static final Map<java.util.UUID, java.lang.ref.WeakReference<Relay>> NEARBY_PLAYERS = new java.util.HashMap<>();
        private int playerScan;

        private double distanceSqTo(net.minecraft.world.entity.Entity entity) {
            return entity.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(worldPosition));
        }

        /** Реле, от которого может заряжаться амулет вис этого существа: записанное за ним и не дальше пяти блоков. */
        @Nullable
        public static Relay nearestTo(net.minecraft.world.entity.Entity entity) {
            java.lang.ref.WeakReference<Relay> ref = NEARBY_PLAYERS.get(entity.getUUID());
            if (ref == null) {
                return null;
            }
            Relay relay = ref.get();
            if (relay == null || relay.isRemoved() || relay.getLevel() != entity.level() || !(relay.distanceSqTo(entity) < 26.0)) {
                NEARBY_PLAYERS.remove(entity.getUUID());
                return null;
            }
            return relay;
        }

        public static void clientTick(Level level, BlockPos pos, BlockState state, Relay relay) {
            BlockPos parent = relay.getParentPos();
            if (parent != null) {
                Direction own = relay.getFacing();
                double sx = parent.getX() + 0.5;
                double sy = parent.getY() + 0.5;
                double sz = parent.getZ() + 0.5;
                if (level.getBlockEntity(parent) instanceof Relay other) {
                    Direction d = other.getFacing();
                    sx -= d.getStepX() * 0.05;
                    sy -= d.getStepY() * 0.05;
                    sz -= d.getStepZ() * 0.05;
                }
                thaumcraft.client.fx.Fx.powerBeam(pos.asLong(), sx, sy, sz, pos.getX() + 0.5 - own.getStepX() * 0.05,
                        pos.getY() + 0.5 - own.getStepY() * 0.05, pos.getZ() + 0.5 - own.getStepZ() * 0.05,
                        relay.pRed, relay.pGreen, relay.pBlue, relay.pulse > 0);
            }
            relay.pRed = Math.min(1f, relay.pRed + 0.025f);
            relay.pGreen = Math.min(1f, relay.pGreen + 0.025f);
            relay.pBlue = Math.min(1f, relay.pBlue + 0.025f);
            if (relay.pulse > 0) {
                relay.pulse--;
            }
        }

        /** Касание жезлом: следующая настройка по кругу. Реле при этом переподключается. */
        public void cycleColor() {
            if (level == null || level.isClientSide) {
                return;
            }
            color++;
            if (color > 5) {
                color = -1;
            }
            link.remove(this);
            link.refresh = true;
            VisNetHandler.invalidate();
            sync();
            level.playSound(null, worldPosition, ModSounds.CRYSTAL.get(), SoundSource.BLOCKS, 0.2f, 1f);
        }

        @Override
        public void setRemoved() {
            if (level != null && !level.isClientSide) {
                link.remove(this);
                VisNetHandler.invalidate();
            }
            super.setRemoved();
        }

        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            tag.putByte("color", color);
            tag.putByte("px", (byte) parentOffset.getX());
            tag.putByte("py", (byte) parentOffset.getY());
            tag.putByte("pz", (byte) parentOffset.getZ());
        }

        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            color = tag.getByte("color");
            parentOffset = new BlockPos(tag.getByte("px"), tag.getByte("py"), tag.getByte("pz"));
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
