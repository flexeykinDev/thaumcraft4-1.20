package thaumcraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import thaumcraft.common.blockentity.ArcaneWorkbenchBlockEntity;

/** Магический верстак (BlockTable оригинала, мета 15). */
public class ArcaneWorkbenchBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = Shapes.or(box(0, 8, 0, 16, 16, 16), box(0, 0, 0, 16, 4, 16),
            box(1, 4, 1, 5, 8, 5), box(11, 4, 1, 15, 8, 5), box(1, 4, 11, 5, 8, 15), box(11, 4, 11, 15, 8, 15));

    public ArcaneWorkbenchBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ArcaneWorkbenchBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ArcaneWorkbenchBlockEntity workbench
                && player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, workbench, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** При разрушении всё, что лежало на столе, включая жезл, выпадает. */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ArcaneWorkbenchBlockEntity workbench) {
            Containers.dropContents(level, pos, workbench.getItems());
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}
