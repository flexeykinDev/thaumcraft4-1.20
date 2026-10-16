package thaumcraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import thaumcraft.common.registry.ModBlockEntities;
import thaumcraft.common.registry.ModItems;
import thaumcraft.common.registry.ModSounds;
import thaumcraft.common.world.outer.BossRooms;

/**
 * Древний запирающий механизм — перенос BlockEldritch (мета 8) и TileEldritchLock оригинала: замок в середине
 * проёма, ведущего в зал хранителя. Проём затянут звёздной бездной и непроходимым полем. В замок вставляют рунную
 * скрижаль — четыре его зубца с гулом втягиваются, и через пять секунд замок исчезает вместе с полем, а в зале
 * пробуждается хранитель: голем Элдрича, страж-надзиратель, багровый портал культа или гнездо гигантских
 * щупалец порчи — по очереди, с небольшой случайностью. Сам замок не сломать.
 */
public class AncientLockBlock extends Block implements EntityBlock {
    /** Куда обращён замок (наружу из зала). */
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public AncientLockBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Lock(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == ModBlockEntities.ANCIENT_LOCK.get() ? (l, p, s, be) -> ((Lock) be).tick() : null;
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean isValidSpawn(BlockState state, BlockGetter level, BlockPos pos, net.minecraft.world.entity.SpawnPlacements.Type type,
                                net.minecraft.world.entity.EntityType<?> entityType) {
        return false;
    }

    /** onBlockActivated оригинала: рунная скрижаль запускает замок. */
    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        if (!held.is(ModItems.RUNED_TABLET.get()) || !(level.getBlockEntity(pos) instanceof Lock lock) || lock.count >= 0) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            lock.count = 0;
            lock.sync();
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
            level.playSound(null, pos, ModSounds.RUNIC_SHIELD_CHARGE.get(), SoundSource.BLOCKS, 1f, 1f);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** randomDisplayTick оригинала: пока скрижаль не вставлена, по замку бегают голубые искры. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof Lock lock && lock.count < 0) {
            thaumcraft.client.fx.Fx.spark(pos.getX() + random.nextFloat(), pos.getY() + random.nextFloat(), pos.getZ() + random.nextFloat(), 0.5f,
                    0.65f + random.nextFloat() * 0.1f, 1f, 1f, 0.8f);
        }
    }

    public static class Lock extends BlockEntity {
        /** -1 — заперт; иначе сколько тиков прошло с тех пор, как вставили скрижаль. */
        public int count = -1;

        public Lock(BlockPos pos, BlockState state) {
            super(ModBlockEntities.ANCIENT_LOCK.get(), pos, state);
        }

        public Direction facing() {
            return getBlockState().getValue(FACING);
        }

        void sync() {
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }

        void tick() {
            if (count == -1) {
                return;
            }
            count++;
            if (level.isClientSide) {
                return;
            }
            if (count % 5 == 0) {
                level.playSound(null, worldPosition, ModSounds.PUMP.get(), SoundSource.BLOCKS, 1f, 1f);
            }
            if (count > 100) {
                level.playSound(null, worldPosition, ModSounds.ICE.get(), SoundSource.BLOCKS, 1f, 1f);
                BossRooms.open((ServerLevel) level, worldPosition);
            }
        }

        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            tag.putShort("count", (short) count);
        }

        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            count = tag.contains("count") ? tag.getShort("count") : -1;
        }

        @Override
        public CompoundTag getUpdateTag() {
            CompoundTag tag = super.getUpdateTag();
            tag.putShort("count", (short) count);
            return tag;
        }

        @Override
        public Packet<ClientGamePacketListener> getUpdatePacket() {
            return ClientboundBlockEntityDataPacket.create(this);
        }

        @Override
        public AABB getRenderBoundingBox() {
            return new AABB(worldPosition).inflate(2.25, 2.25, 2.25).expandTowards(1, 1, 1);
        }
    }
}
