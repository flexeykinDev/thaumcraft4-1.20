package thaumcraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import thaumcraft.common.registry.ModBlockEntities;
import thaumcraft.common.registry.ModSounds;

import java.util.List;

/**
 * Мозг в банке — перенос blockJar:1 и TileJarBrain оригинала. Мозг подтягивает к себе сферы опыта в шести блоках
 * и съедает их, копя до 2000 единиц; щелчок по банке выплёвывает обратно до 64 единиц за раз, а разбитая банка
 * отдаёт всё. Мозг поворачивается к ближайшему игроку и изредка вздыхает.
 */
public class BrainJarBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 12, 13);

    public BrainJarBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Brain(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.BRAIN_JAR.get(), level.isClientSide ? Brain::clientTick : Brain::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof Brain brain) {
            brain.eatDelay = 40;
            if (level.isClientSide) {
                level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, ModSounds.JAR.get(), SoundSource.BLOCKS, 0.2f, 1f, false);
            } else {
                int amount = level.random.nextInt(Math.min(brain.xp + 1, 64));
                if (amount > 0) {
                    brain.xp -= amount;
                    ExperienceOrb.award((ServerLevel) level, Vec3.atCenterOf(pos), amount);
                    brain.sync();
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Разбитая банка отдаёт весь накопленный опыт. */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel server && level.getBlockEntity(pos) instanceof Brain brain && brain.xp > 0) {
            ExperienceOrb.award(server, Vec3.atLowerCornerOf(pos), brain.xp);
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof Brain brain) {
            return Mth.floor((float) brain.xp / Brain.MAX * 14f) + (brain.xp > 0 ? 1 : 0);
        }
        return 0;
    }

    public static class Brain extends BlockEntity {
        public static final int MAX = 2000;
        public int xp;
        int eatDelay;
        /** Поворот мозга (rota и rotb оригинала) и угол, к которому он тянется. */
        public float rota;
        public float rotb;
        private float target;
        private long lastSigh = System.currentTimeMillis() + 1500L;

        public Brain(BlockPos pos, BlockState state) {
            super(ModBlockEntities.BRAIN_JAR.get(), pos, state);
        }

        void sync() {
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
            }
        }

        @Nullable
        private ExperienceOrb closestOrb() {
            ExperienceOrb closest = null;
            double best = Double.MAX_VALUE;
            for (ExperienceOrb orb : level.getEntitiesOfClass(ExperienceOrb.class, new AABB(worldPosition).inflate(6.0))) {
                double distance = orb.distanceToSqr(Vec3.atCenterOf(worldPosition));
                if (distance < best) {
                    best = distance;
                    closest = orb;
                }
            }
            return closest;
        }

        /** Сферы опыта притягиваются к банке. */
        private Entity pull() {
            if (xp >= MAX) {
                return null;
            }
            ExperienceOrb orb = closestOrb();
            if (orb != null && eatDelay == 0) {
                double dx = (worldPosition.getX() + 0.5 - orb.getX()) / 7.0;
                double dy = (worldPosition.getY() + 0.5 - orb.getY()) / 7.0;
                double dz = (worldPosition.getZ() + 0.5 - orb.getZ()) / 7.0;
                double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                double strength = 1.0 - distance;
                if (strength > 0.0 && distance > 0.0) {
                    strength *= strength;
                    orb.setDeltaMovement(orb.getDeltaMovement().add(dx / distance * strength * 0.15, dy / distance * strength * 0.33,
                            dz / distance * strength * 0.15));
                }
            }
            return orb;
        }

        static void serverTick(Level level, BlockPos pos, BlockState state, Brain brain) {
            brain.xp = Math.min(brain.xp, MAX);
            brain.pull();
            if (brain.eatDelay > 0) {
                brain.eatDelay--;
                return;
            }
            if (brain.xp >= MAX) {
                return;
            }
            List<ExperienceOrb> orbs = level.getEntitiesOfClass(ExperienceOrb.class, new AABB(pos).inflate(0.1));
            if (!orbs.isEmpty()) {
                for (ExperienceOrb orb : orbs) {
                    brain.xp += orb.getValue();
                    level.playSound(null, orb.getX(), orb.getY(), orb.getZ(), SoundEvents.GENERIC_EAT, SoundSource.BLOCKS, 0.1f,
                            (level.random.nextFloat() - level.random.nextFloat()) * 0.2f + 1f);
                    orb.discard();
                }
                brain.sync();
            }
        }

        static void clientTick(Level level, BlockPos pos, BlockState state, Brain brain) {
            if (brain.eatDelay > 0) {
                brain.eatDelay--;
            }
            brain.rotb = brain.rota;
            Entity watched = brain.pull();
            if (watched == null) {
                watched = level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6.0, false);
                if (watched != null && brain.lastSigh < System.currentTimeMillis()) {
                    level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, ModSounds.BRAIN.get(), SoundSource.BLOCKS, 0.15f,
                            0.8f + level.random.nextFloat() * 0.4f, false);
                    brain.lastSigh = System.currentTimeMillis() + 5000L + level.random.nextInt(25000);
                }
            }
            if (watched != null) {
                brain.target = (float) Math.atan2(watched.getZ() - (pos.getZ() + 0.5), watched.getX() - (pos.getX() + 0.5));
            } else {
                brain.target += 0.01f;
            }
            brain.rota = Mth.wrapDegrees(brain.rota * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD;
            brain.target = Mth.wrapDegrees(brain.target * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD;
            float turn = Mth.wrapDegrees((brain.target - brain.rota) * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD;
            brain.rota += turn * 0.04f;
        }

        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            tag.putInt("XP", xp);
        }

        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            xp = tag.getInt("XP");
        }

        @Override
        public CompoundTag getUpdateTag() {
            return saveWithoutMetadata();
        }

        @Override
        public Packet<ClientGamePacketListener> getUpdatePacket() {
            return ClientboundBlockEntityDataPacket.create(this);
        }
    }
}
