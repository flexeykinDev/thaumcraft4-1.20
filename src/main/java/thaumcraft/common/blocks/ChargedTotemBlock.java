package thaumcraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import thaumcraft.common.blockentity.NodeBlockEntity;
import thaumcraft.common.registry.ModBlockEntities;

/**
 * Заряженный обсидиановый тотем (blockCosmeticSolid:8 оригинала): тотем, внутри которого живёт тёмный узел.
 * Встречается только в мире, на вершине тотемных столбов; разбитый, отдаёт обсидиановую плитку и эссенции узла.
 */
public class ChargedTotemBlock extends ObsidianTotemBlock implements EntityBlock {
    public ChargedTotemBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NodeBlockEntity(pos, state);
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

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        NodeHarvest.dropEssences(level, pos, player);
        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientBlockExtensions> consumer) {
        consumer.accept(thaumcraft.client.render.NodeDestroyEffects.INSTANCE);
    }
}
