package thaumcraft.common.taint;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import thaumcraft.common.blocks.FluxBlock;
import thaumcraft.common.registry.ModBlocks;
import thaumcraft.common.registry.ModEffects;

/**
 * Корка порчи и порченая почва (BlockTaint оригинала, виды 0 и 1). Обе заражают землю вокруг и растят волокна.
 * Корка к тому же оседает вниз и сползает с краёв, как тяжёлая слизь, а замурованная со всех сторон или оказавшаяся
 * вне порченой земли растекается флюсовой жижей; почва вне порченой земли понемногу снова становится землёй.
 * Живое, что ходит по порче, может получить порченый яд.
 */
public class TaintBlock extends Block {
    private final boolean crust;

    public TaintBlock(Properties properties, boolean crust) {
        super(properties);
        this.crust = crust;
    }

    /** Может ли корка упасть в это место: воздух, огонь, волокна, вода, лава и всё, что заменяется; рядом не должно быть брёвен. */
    public static boolean canFallInto(Level level, BlockPos pos) {
        for (BlockPos near : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
            if (level.getBlockState(near).is(BlockTags.LOGS)) {
                return false;
            }
        }
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return true;
        }
        if (state.getBlock() instanceof FluxBlock && state.hasProperty(FluxBlock.LEVEL) && state.getValue(FluxBlock.LEVEL) >= 4) {
            return false;
        }
        return state.getBlock() instanceof FireBlock || state.getBlock() instanceof TaintFibresBlock || state.canBeReplaced()
                || state.getFluidState().is(Fluids.WATER) || state.getFluidState().is(Fluids.LAVA);
    }

    /** Корка из from срывается и падает из точки start. */
    private boolean tryToFall(ServerLevel level, BlockPos from, BlockPos start, BlockState state) {
        if (start.getY() <= level.getMinBuildHeight() || !canFallInto(level, start.below())) {
            return false;
        }
        level.removeBlock(from, false);
        FallingBlockEntity falling = FallingBlockEntity.fall(level, start, state);
        // упав на то, на чём блок не удержится, корка пропадает, а не выпадает предметом
        falling.dropItem = false;
        return true;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        Taint.biomeSpread(level, pos, random);
        if (crust) {
            if (tryToFall(level, pos, pos, state)) {
                return;
            }
            if (level.isEmptyBlock(pos.above())) {
                // край уступа высотой в четыре блока корки: верхний блок сползает вбок и падает
                Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(random);
                boolean ledge = true;
                for (int a = 0; a < 4; a++) {
                    if (!level.isEmptyBlock(pos.relative(dir).below(a)) || !level.getBlockState(pos.below(a)).is(this)) {
                        ledge = false;
                        break;
                    }
                }
                if (ledge && tryToFall(level, pos, pos.relative(dir), state)) {
                    return;
                }
            }
        }
        BlockPos target = pos.offset(random.nextInt(3) - 1, random.nextInt(5) - 3, random.nextInt(3) - 1);
        if (Taint.isTainted(level, target)) {
            Taint.spreadFibres(level, target);
            if (crust) {
                if (level.isEmptyBlock(pos.above()) && random.nextInt(200) == 0) {
                    // открытая сверху корка изредка превращается в рой спор, если в шестнадцати блоках такого ещё нет
                    if (level.getEntitiesOfClass(thaumcraft.common.entity.TaintSporeSwarmer.class,
                            new net.minecraft.world.phys.AABB(pos).inflate(16)).isEmpty()) {
                        level.removeBlock(pos, false);
                        thaumcraft.common.entity.TaintSporeSwarmer spore =
                                thaumcraft.common.registry.ModEntities.TAINT_SPORE_SWARMER.get().create(level);
                        if (spore != null) {
                            spore.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0f, 0f);
                            level.addFreshEntity(spore);
                            Taint.playRoots(level, pos);
                        }
                    }
                } else {
                    boolean buried = level.getBlockState(pos.above()).is(this);
                    for (Direction dir : Direction.Plane.HORIZONTAL) {
                        buried &= level.getBlockState(pos.relative(dir)).is(this);
                    }
                    if (buried) {
                        level.setBlock(pos, ModBlocks.FLUX_GOO.get().defaultBlockState().setValue(FluxBlock.LEVEL, 7), 3);
                    }
                }
            }
        } else if (crust && random.nextInt(20) == 0) {
            level.setBlock(pos, ModBlocks.FLUX_GOO.get().defaultBlockState().setValue(FluxBlock.LEVEL, 7), 3);
        } else if (!crust && random.nextInt(10) == 0) {
            level.setBlock(pos, Blocks.DIRT.defaultBlockState(), 3);
        }
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide && entity instanceof LivingEntity living && living.getMobType() != MobType.UNDEAD) {
            if (entity instanceof Player && level.random.nextInt(100) == 0) {
                living.addEffect(new MobEffectInstance(ModEffects.TAINT_POISON.get(), 80, 0, false, true));
            } else if (!(entity instanceof Player) && level.random.nextInt(20) == 0) {
                living.addEffect(new MobEffectInstance(ModEffects.TAINT_POISON.get(), 160, 0, false, true));
            }
        }
        super.stepOn(level, pos, state, entity);
    }

    /** С нависающей корки срываются лиловые капли. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (crust && level.isEmptyBlock(pos.below()) && random.nextInt(10) == 0) {
            level.addParticle(ParticleTypes.DRIPPING_OBSIDIAN_TEAR, pos.getX() + 0.1 + random.nextFloat() * 0.8, pos.getY() - 0.05,
                    pos.getZ() + 0.1 + random.nextFloat() * 0.8, 0, 0, 0);
        }
    }
}
