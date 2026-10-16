package thaumcraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import thaumcraft.common.blockentity.NodeBlockEntity;
import thaumcraft.common.registry.ModBlockEntities;

/** Сучок серебряного дерева (в оригинале BlockMagicalLog, мета 2): бревно, внутри которого живёт чистый узел. */
public class SilverwoodKnotBlock extends RotatedPillarBlock implements EntityBlock {

    public SilverwoodKnotBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NodeBlockEntity(pos, state);
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.player.Player player) {
        NodeHarvest.dropEssences(level, pos, player);
        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientBlockExtensions> consumer) {
        consumer.accept(thaumcraft.client.render.NodeDestroyEffects.INSTANCE);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != ModBlockEntities.NODE.get()) {
            return null;
        }
        BlockEntityTicker<NodeBlockEntity> ticker = level.isClientSide ? NodeBlockEntity::clientTick : NodeBlockEntity::serverTick;
        return (BlockEntityTicker<T>) ticker;
    }
}
