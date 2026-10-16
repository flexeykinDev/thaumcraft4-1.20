package thaumcraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractGlassBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.client.extensions.common.IClientBlockExtensions;

import java.util.function.Consumer;

/**
 * Защищённое стекло — перенос blockCosmeticOpaque:2 оригинала. Стёкла, стоящие рядом, сливаются в одно окно с общей
 * рунной рамкой (см. WardedGlassModel); стекло не берут взрывы, его не ломают иссушитель и дракон, а удар по нему
 * высекает руны защиты.
 */
public class WardedGlassBlock extends AbstractGlassBlock {
    public WardedGlassBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canEntityDestroy(BlockState state, BlockGetter level, BlockPos pos, Entity entity) {
        return false;
    }

    @Override
    public void onBlockExploded(BlockState state, Level level, BlockPos pos, Explosion explosion) {
    }

    @Override
    public void initializeClient(Consumer<IClientBlockExtensions> consumer) {
        consumer.accept(new IClientBlockExtensions() {
            @Override
            public boolean addHitEffects(BlockState state, Level level, HitResult target, net.minecraft.client.particle.ParticleEngine manager) {
                if (target instanceof BlockHitResult hit) {
                    BlockPos pos = hit.getBlockPos();
                    Direction side = hit.getDirection();
                    thaumcraft.client.fx.Fx.blockWard(pos.getX(), pos.getY(), pos.getZ(), side.get3DDataValue(),
                            (float) (hit.getLocation().x - pos.getX()), (float) (hit.getLocation().y - pos.getY()),
                            (float) (hit.getLocation().z - pos.getZ()));
                    return true;
                }
                return false;
            }
        });
    }
}
