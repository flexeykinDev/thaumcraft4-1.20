package thaumcraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.EssentiaTransport;
import thaumcraft.api.wands.Wandable;
import thaumcraft.common.registry.ModBlockEntities;
import thaumcraft.common.registry.ModSounds;

/**
 * Основание чародейского бура — перенос blockWoodenDevice:4 и TileArcaneBoreBase оригинала. На нём (или под ним)
 * стоит сам бур; добытое основание выбрасывает в сторону своего носика — или прямо в сундук, стоящий там.
 * Со всех прочих сторон оно тянет по трубам Perditio, которая разгоняет бур. Жезлом носик поворачивают.
 */
public class ArcaneBoreBaseBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public ArcaneBoreBaseBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** Носик смотрит на поставившего (как в оригинале). */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Base(pos, state);
    }

    public static class Base extends BlockEntity implements EssentiaTransport, Wandable {
        public Base(BlockPos pos, BlockState state) {
            super(ModBlockEntities.ARCANE_BORE_BASE.get(), pos, state);
        }

        public Direction facing() {
            return getBlockState().getValue(FACING);
        }

        /** Жезлом носик поворачивается в сторону той грани, по которой щёлкнули (если она боковая). */
        @Override
        public InteractionResult onWand(UseOnContext context) {
            Direction side = context.getClickedFace();
            if (side.getAxis().isHorizontal() && level != null) {
                if (!level.isClientSide) {
                    level.setBlock(worldPosition, getBlockState().setValue(FACING, side), 3);
                    level.playSound(null, worldPosition, ModSounds.TOOL.get(), SoundSource.BLOCKS, 0.3f, 1.9f + level.random.nextFloat() * 0.2f);
                }
                context.getPlayer().swing(context.getHand());
            }
            return InteractionResult.sidedSuccess(level != null && level.isClientSide);
        }

        /** drawEssentia оригинала: единица Perditio из соседней трубы или банки. */
        boolean drawEssentia() {
            for (Direction side : Direction.values()) {
                EssentiaTransport source = EssentiaTransport.neighbour(level, worldPosition, side);
                if (source == null) {
                    continue;
                }
                if (!source.canOutputTo(side.getOpposite())) {
                    return false;
                }
                if (source.getSuctionAmount(side.getOpposite()) < getSuctionAmount(side)
                        && source.takeEssentia(Aspect.ENTROPY, 1, side.getOpposite()) == 1) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean isConnectable(Direction side) {
            return true;
        }

        @Override
        public boolean canInputFrom(Direction side) {
            return true;
        }

        @Override
        public boolean canOutputTo(Direction side) {
            return false;
        }

        @Override
        public void setSuction(@Nullable Aspect aspect, int amount) {
        }

        @Override
        public Aspect getSuctionType(@Nullable Direction side) {
            return Aspect.ENTROPY;
        }

        @Override
        public int getSuctionAmount(@Nullable Direction side) {
            return side != facing() ? 128 : 0;
        }

        @Override
        public int takeEssentia(Aspect aspect, int amount, Direction side) {
            return 0;
        }

        @Override
        public int addEssentia(Aspect aspect, int amount, Direction side) {
            return 0;
        }

        @Override
        public Aspect getEssentiaType(@Nullable Direction side) {
            return null;
        }

        @Override
        public int getEssentiaAmount(@Nullable Direction side) {
            return 0;
        }

        @Override
        public int getMinimumSuction() {
            return 0;
        }
    }
}
