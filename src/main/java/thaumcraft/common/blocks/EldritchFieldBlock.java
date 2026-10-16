package thaumcraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.entity.EldritchMob;
import thaumcraft.common.registry.ModSounds;

/**
 * Поля Запредельных земель (BlockAiry оригинала):
 * <ul>
 * <li>истощающее поле (мета 11) — тёмные искры над полом, которыми хранитель Элдрича опутывает своё логово:
 * всех, кроме существ Элдрича, вязнет в нём, слабеет и изредка иссыхает; гаснет само;</li>
 * <li>непроходимое (мета 12) — невидимая стена, запечатывающая проходы в залы хранителей, пока не открыт замок.</li>
 * </ul>
 */
public class EldritchFieldBlock extends Block {
    private final boolean sapping;

    public EldritchFieldBlock(Properties properties, boolean sapping) {
        super(properties);
        this.sapping = sapping;
    }

    @Override
    @SuppressWarnings("deprecation")
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return sapping ? Shapes.empty() : Shapes.block();
    }

    @Override
    @SuppressWarnings("deprecation")
    public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1f;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return ItemStack.EMPTY;
    }

    /** Поля, раскиданные бурей хранителя, гаснут в назначенный срок. */
    @Override
    @SuppressWarnings("deprecation")
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (sapping) {
            level.removeBlock(pos, false);
        }
    }

    /** Истощающее поле гаснет при первом же случайном тике. */
    @Override
    @SuppressWarnings("deprecation")
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (sapping) {
            level.removeBlock(pos, false);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!sapping || entity instanceof EldritchMob) {
            return;
        }
        if (level.random.nextInt(100) == 0) {
            entity.hurt(level.damageSources().wither(), 1f);
        }
        entity.setDeltaMovement(entity.getDeltaMovement().multiply(0.66, 1, 0.66));
        if (entity instanceof Player player) {
            player.causeFoodExhaustion(0.05f);
        }
        if (entity instanceof LivingEntity living && !level.isClientSide) {
            living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1, true, true));
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!sapping) {
            return;
        }
        float height = random.nextFloat() * 0.33f;
        thaumcraft.client.fx.Fx.spark(pos.getX() + random.nextFloat(), pos.getY() + 0.1515f + height / 2f, pos.getZ() + random.nextFloat(),
                0.33f + height, 0.3f - random.nextFloat() * 0.1f, 0f, 0.5f + random.nextFloat() * 0.2f, 1f);
        if (random.nextInt(50) == 0) {
            level.playLocalSound(pos.getX(), pos.getY(), pos.getZ(), ModSounds.JACOBS.get(), SoundSource.BLOCKS, 0.5f,
                    1f + (random.nextFloat() - random.nextFloat()) * 0.2f, false);
        }
    }
}
