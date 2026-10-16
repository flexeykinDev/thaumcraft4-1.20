package thaumcraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.registry.ModBlocks;
import thaumcraft.common.registry.ModEffects;

/**
 * Флюс — магические отходы небрежной алхимии (BlockFluxGoo и BlockFluxGas оригинала). Это «конечная жидкость»:
 * блок хранит от 1 до 8 долей, доли перетекают к соседям и нигде не берутся из воздуха. Жижа стекает вниз,
 * газ поднимается вверх. Оба вызывают «флюсовую лихорадку», из-за которой жезл тратит больше вис.
 */
public class FluxBlock extends Block {
    /** Число долей минус одна (как метаданные оригинала). */
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, 7);

    private final boolean gas;

    public FluxBlock(boolean gas, Properties properties) {
        super(properties);
        this.gas = gas;
        registerDefaultState(stateDefinition.any().setValue(LEVEL, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL);
    }

    private float fraction(BlockState state) {
        return (state.getValue(LEVEL) + 1) / 8f;
    }

    // ---------- форма: как у жидкости, сквозь флюс проходят и по нему нельзя кликнуть ----------

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    /** Соседние блоки того же флюса не рисуют грани друг между другом. */
    @Override
    public boolean skipRendering(BlockState state, BlockState neighbor, Direction side) {
        return neighbor.is(this) && (gas || side.getAxis().isHorizontal() && neighbor.getValue(LEVEL) >= state.getValue(LEVEL))
                || super.skipRendering(state, neighbor, side);
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    /** Как в оригинале: тонкий слой можно застроить; газ вытесняется любым блоком. */
    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return gas || state.getValue(LEVEL) < 2;
    }

    @Override
    public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
        return true;
    }

    // ---------- течение ----------

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        level.scheduleTick(pos, this, gas ? 10 : 5);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moved) {
        level.scheduleTick(pos, this, gas ? 10 : 5);
    }

    private boolean canFlowInto(BlockState state) {
        return state.isAir();
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int quanta = state.getValue(LEVEL) + 1;

        // сначала по вертикали: жижа падает, газ всплывает
        BlockPos next = gas ? pos.above() : pos.below();
        if (!level.isOutsideBuildHeight(next)) {
            BlockState target = level.getBlockState(next);
            if (canFlowInto(target)) {
                level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                level.setBlock(next, state, 3);
                return;
            }
            if (target.is(this) && target.getValue(LEVEL) < 7) {
                int moved = Math.min(quanta, 7 - target.getValue(LEVEL));
                level.setBlock(next, target.setValue(LEVEL, target.getValue(LEVEL) + moved), 3);
                quanta -= moved;
                if (quanta <= 0) {
                    level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                    return;
                }
                state = state.setValue(LEVEL, quanta - 1);
                level.setBlock(pos, state, 3);
            }
        } else if (gas) {
            // газ, поднявшийся до края мира, улетучивается
            level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            return;
        }

        // затем в стороны: одна доля уходит к самому «пустому» соседу, пока уровни не сравняются
        if (quanta > 1) {
            BlockPos best = null;
            int lowest = quanta - 1;
            for (Direction side : Direction.Plane.HORIZONTAL.shuffledCopy(random)) {
                BlockPos near = pos.relative(side);
                BlockState there = level.getBlockState(near);
                int amount = canFlowInto(there) ? 0 : there.is(this) ? there.getValue(LEVEL) + 1 : 9;
                if (amount < lowest) {
                    lowest = amount;
                    best = near;
                }
            }
            if (best != null) {
                level.setBlock(best, defaultBlockState().setValue(LEVEL, lowest), 3);
                level.setBlock(pos, state.setValue(LEVEL, quanta - 2), 3);
            }
        }
    }

    // ---------- распад ----------

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int amount = state.getValue(LEVEL);
        if (gas) {
            // В оригинале газ исчезал, только отравив кого-нибудь, а под открытым небом поднимался до потолка мира и
            // висел там вечно. Здесь под открытым небом он рассеивается сам; в помещении ведёт себя как в оригинале.
            if (level.canSeeSky(pos) && random.nextInt(3) == 0) {
                level.setBlock(pos, amount == 0 ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()
                        : state.setValue(LEVEL, amount - 1), 3);
            }
            return;
        }
        // лужа под открытым верхом изредка собирается в таумического слизня: большая — в слизня покрупнее
        if (amount >= 2 && level.isEmptyBlock(pos.above()) && random.nextInt(25) == 0) {
            level.removeBlock(pos, false);
            thaumcraft.common.entity.ThaumicSlime slime = thaumcraft.common.registry.ModEntities.THAUMIC_SLIME.get().create(level);
            if (slime != null) {
                slime.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0f, 0f);
                slime.setSlimeSize(amount >= 6 ? 2 : 1);
                level.addFreshEntity(slime);
                slime.playSound(thaumcraft.common.registry.ModSounds.GORE.get(), 1f, 1f);
            }
            return;
        }
        // почти полная лужа под открытым верхом изредка заражает землю порчей и сама становится волокнами
        if (amount >= 6 && level.isEmptyBlock(pos.above()) && random.nextInt(50) == 0) {
            thaumcraft.common.taint.Taint.setTainted(level, pos);
            level.setBlock(pos, thaumcraft.common.taint.TaintFibresBlock.stateFor(level, pos), 3);
            thaumcraft.common.taint.Taint.playRoots(level, pos);
            return;
        }
        if (random.nextInt(30) == 0) {
            if (amount == 0) {
                level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            } else {
                level.setBlock(pos, state.setValue(LEVEL, amount - 1), 3);
                if (random.nextBoolean() && level.isEmptyBlock(pos.above())) {
                    level.setBlock(pos.above(), ModBlocks.FLUX_GAS.get().defaultBlockState(), 3);
                }
            }
        }
    }

    // ---------- действие на существ ----------

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        int amount = state.getValue(LEVEL);
        if (gas) {
            if (level.isClientSide || level.random.nextInt(10) != 0 || !(entity instanceof LivingEntity living)
                    || living.isInvertedHealAndHarm() || living.hasEffect(ModEffects.VIS_EXHAUSTION.get())
                    || living.hasEffect(MobEffects.CONFUSION)) {
                return;
            }
            if (level.random.nextBoolean()) {
                living.addEffect(exhaustion(1200, amount / 3));
            } else {
                living.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80 + amount * 20, 0));
            }
            // отравив кого-то, газ тратится
            level.setBlock(pos, amount > 0 ? state.setValue(LEVEL, amount - 1)
                    : net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            return;
        }
        if (entity.getY() >= pos.getY() + fraction(state)) {
            return;
        }
        // чем глубже жижа, тем сильнее она вяжет ноги
        float free = 1f - fraction(state);
        entity.setDeltaMovement(entity.getDeltaMovement().multiply(free, 1.0, free));
        if (!level.isClientSide && entity instanceof LivingEntity living) {
            living.addEffect(exhaustion(600, amount / 3));
        }
    }

    /** Флюсовая лихорадка: молоком не лечится, частиц не даёт. */
    private static MobEffectInstance exhaustion(int duration, int amplifier) {
        MobEffectInstance effect = new MobEffectInstance(ModEffects.VIS_EXHAUSTION.get(), duration, amplifier, true, true);
        effect.setCurativeItems(new java.util.ArrayList<>());
        return effect;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!gas && random.nextInt(40) <= state.getValue(LEVEL)) {
            thaumcraft.client.fx.Fx.crucibleBubble(pos.getX() + random.nextFloat(), pos.getY() + fraction(state),
                    pos.getZ() + random.nextFloat(), 1f, 0f, 0.5f);
        }
    }
}
