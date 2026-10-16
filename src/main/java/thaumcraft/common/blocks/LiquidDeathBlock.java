package thaumcraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.Thaumcraft;
import thaumcraft.client.fx.Fx;

/**
 * Жидкая смерть (BlockFluidDeath оригинала): «конечная» жидкость, как флюс, — растекается, но не прибывает.
 * Растворяет всё живое; чем глубже лужа, тем сильнее (от 1 до 4 урона за раз). Сама не исчезает.
 */
public class LiquidDeathBlock extends FluxBlock {
    /** DamageSourceThaumcraft.dissolve оригинала. */
    public static final ResourceKey<DamageType> DISSOLVE = ResourceKey.create(Registries.DAMAGE_TYPE, Thaumcraft.id("dissolve"));

    public LiquidDeathBlock(Properties properties) {
        super(false, properties);
    }

    private static float height(BlockState state) {
        return (state.getValue(LEVEL) + 1) / 8f;
    }

    /** Ни слизней, ни порчи, ни испарения: смерть просто лежит. */
    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide && entity instanceof LivingEntity living && entity.getY() < pos.getY() + height(state)) {
            living.hurt(new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DISSOLVE)),
                    state.getValue(LEVEL) / 2 + 1);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        Fx.crucibleBubble(pos.getX() + random.nextFloat(), pos.getY() + height(state), pos.getZ() + random.nextFloat(),
                0.3f - random.nextFloat() * 0.1f, 0f, 0.4f + random.nextFloat() * 0.1f);
        if (random.nextInt(50) == 0) {
            level.playLocalSound(pos.getX() + random.nextFloat(), pos.getY() + 1.0, pos.getZ() + random.nextFloat(), SoundEvents.LAVA_POP,
                    SoundSource.BLOCKS, 0.1f + random.nextFloat() * 0.1f, 0.9f + random.nextFloat() * 0.15f, false);
        }
    }
}
