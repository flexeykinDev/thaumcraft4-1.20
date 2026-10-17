package thaumictinkerer.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import thaumictinkerer.common.registry.TTBlockEntities;

/**
 * Эфирная платформа (BlockPlatform оригинала): на неё можно встать, но снизу сквозь неё проходят насквозь, а с Shift
 * с неё спрыгивают вниз. Щелчок блоком в руке маскирует платформу под этот блок (BlockCamo), пустой рукой —
 * снимает маскировку.
 */
public class PlatformBlock extends BaseEntityBlock {
    public static final BooleanProperty CAMO = BooleanProperty.create("camo");

    public PlatformBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(CAMO, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CAMO);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (context.isDescending() || !context.isAbove(Shapes.block(), pos, true)) {
            return Shapes.empty();
        }
        return Shapes.block();
    }

    /** Под маскировку годится только обычный полный блок без своего содержимого (validRenderTypes оригинала). */
    public static boolean canDisguiseAs(BlockState state, BlockGetter level, BlockPos pos) {
        return !state.isAir() && state.getRenderShape() == RenderShape.MODEL && !(state.getBlock() instanceof EntityBlock)
                && state.isCollisionShapeFullBlock(level, pos);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof Tile platform)) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);
        BlockState camo = null;
        if (!held.isEmpty()) {
            if (!(held.getItem() instanceof BlockItem blockItem)) {
                return InteractionResult.PASS;
            }
            camo = blockItem.getBlock().defaultBlockState();
            if (!canDisguiseAs(camo, level, pos)) {
                return InteractionResult.PASS;
            }
        }
        if (!level.isClientSide) {
            platform.camo = camo;
            level.setBlock(pos, state.setValue(CAMO, camo != null), Block.UPDATE_ALL);
            platform.setChanged();
            level.sendBlockUpdated(pos, state, level.getBlockState(pos), Block.UPDATE_CLIENTS);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Tile(pos, state);
    }

    /** TileCamo оригинала: какой блок изображает платформа. */
    public static class Tile extends BlockEntity {
        @Nullable
        private BlockState camo;

        public Tile(BlockPos pos, BlockState state) {
            super(TTBlockEntities.PLATFORM.get(), pos, state);
        }

        @Nullable
        public BlockState getCamo() {
            return camo;
        }

        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            if (camo != null) {
                tag.put("Camo", NbtUtils.writeBlockState(camo));
            }
        }

        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            camo = tag.contains("Camo") ? NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag.getCompound("Camo")) : null;
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
