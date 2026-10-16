package thaumcraft.client.render;

import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.extensions.common.IClientBlockExtensions;
import thaumcraft.client.fx.Fx;
import thaumcraft.common.registry.ModSounds;

/** addDestroyEffects оригинала: разрушенный узел лопается вспышкой с треском неудавшегося заклинания. */
public final class NodeDestroyEffects implements IClientBlockExtensions {
    public static final NodeDestroyEffects INSTANCE = new NodeDestroyEffects();

    private NodeDestroyEffects() {
    }

    @Override
    public boolean addDestroyEffects(BlockState state, Level level, BlockPos pos, ParticleEngine manager) {
        Fx.burst(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1f);
        level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, ModSounds.CRAFT_FAIL.get(), SoundSource.BLOCKS, 1f, 1f,
                false);
        return false;
    }
}
