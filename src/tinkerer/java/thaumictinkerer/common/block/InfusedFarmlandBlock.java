package thaumictinkerer.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.IPlantable;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.AspectContainer;
import thaumcraft.api.aspects.AspectList;
import thaumictinkerer.common.registry.TTBlockEntities;

/**
 * Наполненная пашня (BlockInfusedFarmland + TileInfusedFarmland): не пересыхает и не вытаптывается, держит только наполненное
 * зерно и копит след аспектов собранных с неё растений (не больше 20).
 */
public class InfusedFarmlandBlock extends FarmBlock implements EntityBlock {
    public static final int MAX_ASPECTS = 20;

    public InfusedFarmlandBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(MOISTURE, 7));
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float distance) {
        entity.causeFallDamage(distance, 1f, entity.damageSources().fall());
    }

    @Override
    public boolean canSustainPlant(BlockState state, BlockGetter level, BlockPos pos, Direction facing, IPlantable plantable) {
        return plantable instanceof InfusedGrainBlock;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Tile(pos, state);
    }

    public static class Tile extends BlockEntity implements AspectContainer {
        public AspectList aspects = new AspectList();

        public Tile(BlockPos pos, BlockState state) {
            super(TTBlockEntities.INFUSED_FARMLAND.get(), pos, state);
        }

        @Nullable
        @Override
        public AspectList getContainedAspects() {
            return aspects;
        }

        void sync() {
            setChanged();
            if (level != null) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            }
        }

        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            tag.put("aspects", aspects.save(new CompoundTag()));
        }

        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            aspects = AspectList.load(tag.getCompound("aspects"));
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
