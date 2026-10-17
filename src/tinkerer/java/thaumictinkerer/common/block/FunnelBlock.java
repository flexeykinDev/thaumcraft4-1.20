package thaumictinkerer.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.blockentity.EssentiaBlockEntity;
import thaumcraft.common.items.JarBlockItem;
import thaumictinkerer.common.registry.TTBlockEntities;

/**
 * Воронка эссенции (BlockFunnel оригинала): ставится только на воронку-хоппер. В неё кладут банку с эссенцией, и каждый
 * тик воронка переливает по единице в банку-блок, в которую смотрит хоппер.
 */
public class FunnelBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 2, 16);

    public FunnelBlock(Properties properties) {
        super(properties);
    }

    public static boolean isFilledJar(ItemStack stack) {
        return stack.getItem() instanceof JarBlockItem && JarBlockItem.getAspect(stack) != null;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).is(Blocks.HOPPER);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbour, BlockPos from, boolean moving) {
        if (!canSurvive(state, level, pos)) {
            level.destroyBlock(pos, true);
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof Tile funnel)) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);
        if (funnel.jar.isEmpty()) {
            if (!isFilledJar(held)) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide) {
                funnel.jar = held.split(1);
                funnel.changed();
            }
        } else if (!level.isClientSide) {
            if (!player.getInventory().add(funnel.jar)) {
                player.drop(funnel.jar, false);
            }
            funnel.jar = ItemStack.EMPTY;
            funnel.changed();
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof Tile funnel) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), funnel.jar);
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Tile(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, TTBlockEntities.FUNNEL.get(), (l, p, s, tile) -> tile.serverTick());
    }

    /** TileFunnel оригинала. */
    public static class Tile extends BlockEntity implements WorldlyContainer, thaumcraft.api.aspects.AspectContainer {
        private ItemStack jar = ItemStack.EMPTY;

        public Tile(BlockPos pos, BlockState state) {
            super(TTBlockEntities.FUNNEL.get(), pos, state);
        }

        public ItemStack getJar() {
            return jar;
        }

        /** Сквозь очки видно, что осталось в банке. */
        @Nullable
        @Override
        public thaumcraft.api.aspects.AspectList getContainedAspects() {
            Aspect aspect = JarBlockItem.getAspect(jar);
            return aspect == null ? null : new thaumcraft.api.aspects.AspectList().add(aspect, JarBlockItem.getAmount(jar));
        }

        void serverTick() {
            Aspect aspect = JarBlockItem.getAspect(jar);
            if (aspect == null) {
                return;
            }
            BlockPos hopper = worldPosition.below();
            BlockState below = level.getBlockState(hopper);
            if (!below.is(Blocks.HOPPER)) {
                return;
            }
            Direction facing = below.getValue(HopperBlock.FACING);
            if (level.getBlockEntity(hopper.relative(facing)) instanceof EssentiaBlockEntity target && target.isJar()
                    && target.add(aspect, 1) == 0) {
                int left = JarBlockItem.getAmount(jar) - 1;
                if (left > 0) {
                    JarBlockItem.setContents(jar, aspect, left);
                } else {
                    jar.removeTagKey("BlockEntityTag");
                }
                changed();
            }
        }

        void changed() {
            setChanged();
            if (level != null) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            }
        }

        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            if (!jar.isEmpty()) {
                tag.put("Jar", jar.save(new CompoundTag()));
            }
        }

        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            jar = tag.contains("Jar") ? ItemStack.of(tag.getCompound("Jar")) : ItemStack.EMPTY;
        }

        @Override
        public CompoundTag getUpdateTag() {
            return saveWithoutMetadata();
        }

        @Override
        public Packet<ClientGamePacketListener> getUpdatePacket() {
            return ClientboundBlockEntityDataPacket.create(this);
        }

        // хоппер и трубы могут вставлять и забирать банку сверху и с боков
        @Override
        public int[] getSlotsForFace(Direction side) {
            return side == Direction.DOWN ? new int[0] : new int[]{0};
        }

        @Override
        public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
            return isFilledJar(stack);
        }

        @Override
        public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
            return side != Direction.DOWN;
        }

        @Override
        public int getContainerSize() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return jar.isEmpty();
        }

        @Override
        public ItemStack getItem(int slot) {
            return jar;
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            ItemStack taken = jar.split(amount);
            changed();
            return taken;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            ItemStack taken = jar;
            jar = ItemStack.EMPTY;
            return taken;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            jar = stack;
            changed();
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public boolean stillValid(Player player) {
            return false;
        }

        @Override
        public void clearContent() {
            jar = ItemStack.EMPTY;
        }
    }
}
