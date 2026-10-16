package thaumcraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import thaumcraft.common.network.FxPacket;
import thaumcraft.common.registry.ModBlockEntities;
import thaumcraft.common.registry.ModSounds;
import thaumcraft.common.research.Warp;

/**
 * Рунный камень — перенос BlockEldritch (мета 10) и TileEldritchTrap оригинала: ловушка в стенах лабиринта.
 * Игрока ближе трёх блоков он то и дело бьёт разрядом (2 урона магией) и через раз насылает 1–2 временного
 * искажения. Вокруг в воздухе проступают голубые руны.
 */
public class RunedStoneBlock extends Block implements EntityBlock {
    public RunedStoneBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int getExpDrop(BlockState state, LevelReader level, RandomSource random, BlockPos pos, int fortune, int silkTouch) {
        return silkTouch == 0 ? UniformInt.of(1, 4).sample(random) : 0;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Trap(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide || type != ModBlockEntities.RUNED_STONE.get() ? null : (l, p, s, be) -> ((Trap) be).serverTick();
    }

    @Override
    public boolean isValidSpawn(BlockState state, BlockGetter level, BlockPos pos, net.minecraft.world.entity.SpawnPlacements.Type type,
                                net.minecraft.world.entity.EntityType<?> entityType) {
        return false;
    }

    /** randomDisplayTick оригинала: руны в пустом воздухе рядом с камнем. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        BlockPos at = pos.offset(r.nextInt(2) - r.nextInt(2), r.nextInt(2) - r.nextInt(2), r.nextInt(2) - r.nextInt(2));
        if (level.isEmptyBlock(at)) {
            thaumcraft.client.fx.Fx.blockRunes(at.getX() + r.nextFloat(), at.getY() + r.nextFloat(), at.getZ() + r.nextFloat(), 0.5f + r.nextFloat() * 0.5f,
                    r.nextFloat() * 0.3f, 0.9f + r.nextFloat() * 0.1f, 16 + r.nextInt(4), 0f);
        }
    }

    public static class Trap extends BlockEntity {
        private int count = 20;

        public Trap(BlockPos pos, BlockState state) {
            super(ModBlockEntities.RUNED_STONE.get(), pos, state);
        }

        void serverTick() {
            if (count-- > 0) {
                return;
            }
            count = 10 + level.random.nextInt(25);
            Player player = level.getNearestPlayer(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, 3.0, true);
            if (player == null) {
                return;
            }
            player.hurt(level.damageSources().magic(), 2f);
            if (level.random.nextBoolean() && player instanceof ServerPlayer server) {
                Warp.add(server, 1 + level.random.nextInt(2), true);
            }
            // PacketFXBlockZap: молния из камня в глаза игроку и тихий треск
            FxPacket.send((ServerLevel) level, FxPacket.BOLT, Vec3.atCenterOf(worldPosition),
                    new Vec3(player.getX(), player.getBoundingBox().minY + player.getEyeHeight(), player.getZ()), 0, 0);
            level.playSound(null, worldPosition, ModSounds.ZAP.get(), SoundSource.BLOCKS, 0.1f, 1f + level.random.nextFloat() * 0.2f);
        }
    }
}
