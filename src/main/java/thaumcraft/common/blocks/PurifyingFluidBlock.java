package thaumcraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import thaumcraft.client.fx.Fx;
import thaumcraft.common.registry.ModEffects;
import thaumcraft.common.research.PlayerKnowledge;

import java.util.function.Supplier;

/**
 * Очищающая жидкость (BlockFluidPure оригинала). Игрок, вошедший в источник, получает защиту от искажения —
 * тем короче, чем больше у него постоянного искажения, — а сам источник при этом расходуется.
 */
public class PurifyingFluidBlock extends LiquidBlock {
    public PurifyingFluidBlock(Supplier<? extends FlowingFluid> fluid, Properties properties) {
        super(fluid, properties);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide || !state.getFluidState().isSource() || !(entity instanceof ServerPlayer player)
                || player.hasEffect(ModEffects.WARP_WARD.get())) {
            return;
        }
        int warp = PlayerKnowledge.get(player).getWarpPerm();
        int div = warp > 0 ? Math.max(1, (int) Math.sqrt(warp)) : 1;
        player.addEffect(new MobEffectInstance(ModEffects.WARP_WARD.get(), Math.min(32000, 200000 / div), 0, true, true));
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        Fx.crucibleBubble(pos.getX() + random.nextFloat(), pos.getY() + state.getFluidState().getOwnHeight(), pos.getZ() + random.nextFloat(),
                1f, 1f, 1f);
        if (random.nextInt(25) == 0) {
            level.playLocalSound(pos.getX() + random.nextFloat(), pos.getY() + 1.0, pos.getZ() + random.nextFloat(), SoundEvents.LAVA_POP,
                    SoundSource.BLOCKS, 0.1f + random.nextFloat() * 0.1f, 0.9f + random.nextFloat() * 0.15f, false);
        }
        super.animateTick(state, level, pos, random);
    }
}
