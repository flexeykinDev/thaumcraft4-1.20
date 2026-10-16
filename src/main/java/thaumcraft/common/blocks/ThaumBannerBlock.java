package thaumcraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.items.PhialItem;
import thaumcraft.common.registry.ModBlockEntities;

import java.util.List;

/**
 * Знамя — перенос blockWoodenDevice:8 и TileBanner оригинала: полотнище одного из шестнадцати цветов на древке
 * или на стене. Фиал с эссенцией наносит на полотнище знак аспекта (фиал расходуется), крадучись знак стирается.
 * Знамя без цвета — знамя Багрового культа.
 */
public class ThaumBannerBlock extends BaseEntityBlock {
    public static final IntegerProperty ROTATION = BlockStateProperties.ROTATION_16;
    public static final BooleanProperty WALL = BooleanProperty.create("wall");
    /** Цвета полотнищ (Utils.colors оригинала), по порядку красителей. */
    public static final int[] COLORS = {15790320, 15435844, 12801229, 6719955, 14602026, 4312372, 14188952, 4408131, 10526880, 2651799,
            8073150, 2437522, 5320730, 3887386, 11743532, 1973019};

    public ThaumBannerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ROTATION, 0).setValue(WALL, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ROTATION, WALL);
    }

    /** На пол — древко поворачивается вслед за взглядом (16 положений), на стену — полотнище прижимается к ней. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction side = context.getClickedFace();
        if (side.getAxis() == Direction.Axis.Y) {
            return defaultBlockState().setValue(ROTATION, Mth.floor((context.getRotation() + 180f) * 16f / 360f + 0.5) & 15);
        }
        int rotation = switch (side) {
            case NORTH -> 8;
            case WEST -> 4;
            case EAST -> 12;
            default -> 0;
        };
        return defaultBlockState().setValue(WALL, true).setValue(ROTATION, rotation);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (!state.getValue(WALL)) {
            return Block.box(5.28, 0, 5.28, 10.56, 16, 10.56);
        }
        return switch (state.getValue(ROTATION)) {
            case 4 -> Block.box(12, 0, 0, 16, 16, 16);
            case 8 -> Block.box(0, 0, 12, 16, 16, 16);
            case 12 -> Block.box(0, 0, 0, 4, 16, 16);
            default -> Block.box(0, 0, 0, 16, 16, 4);
        };
    }

    /** Сквозь знамя проходят, как сквозь знамёна 1.20. */
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Banner(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (level.getBlockEntity(pos) instanceof Banner banner) {
            banner.color = getColor(stack);
            banner.aspect = stack.hasTag() ? Aspect.byTag(stack.getTag().getString("aspect")) : null;
            banner.sync();
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        if (!(level.getBlockEntity(pos) instanceof Banner banner) || banner.color < 0
                || !player.isShiftKeyDown() && PhialItem.getAspect(held) == null) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            if (player.isShiftKeyDown()) {
                banner.aspect = null;
            } else {
                banner.aspect = PhialItem.getAspect(held);
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
            }
            banner.sync();
            level.playSound(null, pos, SoundEvents.WOOL_STEP, SoundSource.BLOCKS, 1f, 1f);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Цвет знамени в предмете: -1 — знамя культа. */
    public static int getColor(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains("color") ? stack.getTag().getByte("color") : -1;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        ItemStack drop = new ItemStack(this);
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof Banner banner && (banner.color >= 0 || banner.aspect != null)) {
            drop.getOrCreateTag().putByte("color", (byte) banner.color);
            if (banner.aspect != null) {
                drop.getTag().putString("aspect", banner.aspect.getTag());
            }
        }
        return List.of(drop);
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        ItemStack stack = new ItemStack(this);
        if (level.getBlockEntity(pos) instanceof Banner banner && banner.color >= 0) {
            stack.getOrCreateTag().putByte("color", (byte) banner.color);
        }
        return stack;
    }

    public static class Banner extends BlockEntity {
        public int color = -1;
        @Nullable
        public Aspect aspect;

        public Banner(BlockPos pos, BlockState state) {
            super(ModBlockEntities.THAUM_BANNER.get(), pos, state);
        }

        void sync() {
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }

        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            tag.putByte("color", (byte) color);
            tag.putString("aspect", aspect == null ? "" : aspect.getTag());
        }

        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            color = tag.getByte("color");
            aspect = Aspect.byTag(tag.getString("aspect"));
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
        public net.minecraft.world.phys.AABB getRenderBoundingBox() {
            return new net.minecraft.world.phys.AABB(worldPosition, worldPosition.offset(1, 2, 1));
        }
    }
}
