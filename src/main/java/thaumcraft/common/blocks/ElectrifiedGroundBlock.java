package thaumcraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.registry.ModSounds;

/**
 * Наэлектризованная земля (BlockAiry оригинала, мета 10): невидимый участок над землёй, по которому бегают искры.
 * Остаётся после «Земного разряда», бьёт и замедляет всех, кто в него зашёл, и сам гаснет примерно через минуту.
 */
public class ElectrifiedGroundBlock extends Block {
    public ElectrifiedGroundBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    /** Как в оригинале, участок гаснет при первом же случайном тике — в среднем через минуту с небольшим. */
    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        level.removeBlock(pos, false);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        entity.hurt(level.damageSources().magic(), 1 + level.random.nextInt(2));
        entity.setDeltaMovement(entity.getDeltaMovement().multiply(0.8, 1, 0.8));
        if (!level.isClientSide && level.random.nextInt(100) == 0) {
            level.removeBlock(pos, false);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        float height = random.nextFloat() * 0.33f;
        thaumcraft.client.fx.Fx.spark(pos.getX() + random.nextFloat(), pos.getY() + 0.1515f + height / 2f, pos.getZ() + random.nextFloat(),
                0.33f + height, 0.65f + random.nextFloat() * 0.1f, 1f, 1f, 0.8f);
        if (random.nextInt(50) == 0) {
            level.playLocalSound(pos.getX(), pos.getY(), pos.getZ(), ModSounds.JACOBS.get(), SoundSource.BLOCKS, 0.5f,
                    1f + (random.nextFloat() - random.nextFloat()) * 0.2f, false);
        }
    }
}
