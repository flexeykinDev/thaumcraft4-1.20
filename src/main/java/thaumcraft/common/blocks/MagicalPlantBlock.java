package thaumcraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Мерцалист, огнежемчуг и вишрум (в оригинале BlockCustomPlant, мета 2, 3 и 5). Все три светятся; мерцалист и вишрум
 * испускают огоньки, огнежемчуг дымит и тлеет, а вишрум ещё и дурманит всякого, кто через него пройдёт.
 */
public class MagicalPlantBlock extends BushBlock {
    public enum Kind {
        SHIMMERLEAF, CINDERPEARL, VISHROOM
    }

    private static final VoxelShape SHAPE = box(5, 0, 5, 11, 10, 11);

    private final Kind kind;

    public MagicalPlantBlock(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** Огнежемчуг растёт на песке, остальные — на земле и траве. */
    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return kind == Kind.CINDERPEARL ? state.is(BlockTags.SAND) : super.mayPlaceOn(state, level, pos);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (kind == Kind.VISHROOM && !level.isClientSide && entity instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0));
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double x = pos.getX() + 0.5;
        double z = pos.getZ() + 0.5;
        if (kind == Kind.SHIMMERLEAF && random.nextInt(3) == 0) {
            thaumcraft.client.fx.Fx.wispColor(x + (random.nextFloat() - random.nextFloat()) * 0.1f,
                    pos.getY() + 0.5 + (random.nextFloat() - random.nextFloat()) * 0.15f, z + (random.nextFloat() - random.nextFloat()) * 0.1f,
                    0.2f, 0.3f + random.nextFloat() * 0.3f, 0.7f + random.nextFloat() * 0.3f, 0.7f + random.nextFloat() * 0.3f, false, 0f);
        } else if (kind == Kind.CINDERPEARL && random.nextBoolean()) {
            double px = x + (random.nextFloat() - random.nextFloat()) * 0.1f;
            double py = pos.getY() + 0.6 + (random.nextFloat() - random.nextFloat()) * 0.1f;
            double pz = z + (random.nextFloat() - random.nextFloat()) * 0.1f;
            level.addParticle(ParticleTypes.SMOKE, px, py, pz, 0, 0, 0);
            level.addParticle(ParticleTypes.FLAME, px, py, pz, 0, 0, 0);
        } else if (kind == Kind.VISHROOM && random.nextInt(3) == 0) {
            thaumcraft.client.fx.Fx.wispColor(x + (random.nextFloat() - random.nextFloat()) * 0.4f, pos.getY() + 0.3,
                    z + (random.nextFloat() - random.nextFloat()) * 0.4f, 0.1f, 0.5f, 0.3f, 0.8f, true, 0.015f);
        }
    }
}
