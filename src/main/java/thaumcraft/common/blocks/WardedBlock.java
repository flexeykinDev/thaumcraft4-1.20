package thaumcraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.client.extensions.common.IClientBlockExtensions;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.Nullable;
import thaumcraft.common.network.FxPacket;
import thaumcraft.common.registry.ModBlockEntities;
import thaumcraft.common.registry.ModBlocks;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Защищённый блок (BlockWarded и TileWarded оригинала). Фокус «Защита» подменяет им обычный блок: выглядит,
 * звучит и светится он как прежний, но его не сломать ни инструментом, ни взрывом, ни поршнем.
 * Снять защиту может только тот, кто её поставил.
 */
public class WardedBlock extends BaseEntityBlock {
    /** Какой блок изображает защищённый — для модели (см. WardedModel). */
    public static final ModelProperty<BlockState> MIMIC = new ModelProperty<>();

    public WardedBlock(Properties properties) {
        super(properties);
    }

    /** Защитить можно только обычный цельный непрозрачный блок без собственных данных. */
    public static boolean canWard(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return level.getBlockEntity(pos) == null && !state.isAir() && state.isSolidRender(level, pos)
                && state.isCollisionShapeFullBlock(level, pos) && state.getDestroySpeed(level, pos) >= 0f
                && !state.is(ModBlocks.WARDED.get());
    }

    public static boolean ward(ServerLevel level, BlockPos pos, Player owner) {
        if (!canWard(level, pos)) {
            return false;
        }
        BlockState old = level.getBlockState(pos);
        int light = old.getLightEmission(level, pos);
        level.setBlock(pos, ModBlocks.WARDED.get().defaultBlockState(), 3);
        if (!(level.getBlockEntity(pos) instanceof Warded warded)) {
            return false;
        }
        warded.mimic = old;
        warded.owner = owner.getUUID();
        warded.light = light;
        warded.setChanged();
        level.sendBlockUpdated(pos, old, level.getBlockState(pos), 3);
        level.getChunkSource().getLightEngine().checkBlock(pos);
        FxPacket.blockSparkle(level, pos, 0xfc9f40, 4);
        return true;
    }

    /** Возвращает исходный блок, если защиту снимает её хозяин. */
    public static boolean unward(ServerLevel level, BlockPos pos, Player player) {
        if (level.getBlockEntity(pos) instanceof Warded warded && player.getUUID().equals(warded.owner)) {
            level.setBlock(pos, warded.mimic, 3);
            FxPacket.blockSparkle(level, pos, 0xfc9f40, 4);
            return true;
        }
        return false;
    }

    private static BlockState mimic(BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof Warded warded ? warded.mimic : Blocks.STONE.defaultBlockState();
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Warded(pos, state);
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof Warded warded ? warded.light : 0;
    }

    @Override
    public SoundType getSoundType(BlockState state, LevelReader level, BlockPos pos, @Nullable Entity entity) {
        BlockState mimic = mimic(level, pos);
        return mimic.is(this) ? SoundType.STONE : mimic.getSoundType(level, pos, entity);
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos, Player player) {
        return new ItemStack(mimic(level, pos).getBlock().asItem());
    }

    @Override
    public boolean canEntityDestroy(BlockState state, BlockGetter level, BlockPos pos, Entity entity) {
        return false;
    }

    /** Взрыв защищённый блок не трогает вовсе. */
    @Override
    public void onBlockExploded(BlockState state, Level level, BlockPos pos, Explosion explosion) {
    }

    @Override
    public boolean isValidSpawn(BlockState state, BlockGetter level, BlockPos pos, SpawnPlacements.Type type, EntityType<?> entityType) {
        return false;
    }

    /** Удар по блоку вместо крошек даёт вспышку рунного щита (FXBlockWard оригинала). */
    @Override
    public void initializeClient(Consumer<IClientBlockExtensions> consumer) {
        consumer.accept(new IClientBlockExtensions() {
            @Override
            public boolean addHitEffects(BlockState state, Level level, HitResult target, net.minecraft.client.particle.ParticleEngine manager) {
                if (target instanceof BlockHitResult hit) {
                    BlockPos pos = hit.getBlockPos();
                    thaumcraft.client.fx.Fx.blockWard(pos.getX(), pos.getY(), pos.getZ(), hit.getDirection().get3DDataValue(),
                            (float) (hit.getLocation().x - pos.getX()), (float) (hit.getLocation().y - pos.getY()),
                            (float) (hit.getLocation().z - pos.getZ()));
                }
                return true;
            }

            @Override
            public boolean addDestroyEffects(BlockState state, Level level, BlockPos pos, net.minecraft.client.particle.ParticleEngine manager) {
                return true;
            }
        });
    }

    /** TileWarded оригинала. */
    public static class Warded extends BlockEntity {
        private BlockState mimic = Blocks.STONE.defaultBlockState();
        @Nullable
        private UUID owner;
        private int light;

        public Warded(BlockPos pos, BlockState state) {
            super(ModBlockEntities.WARDED.get(), pos, state);
        }

        public BlockState getMimic() {
            return mimic;
        }

        @Nullable
        public UUID getOwner() {
            return owner;
        }

        @Override
        public ModelData getModelData() {
            return ModelData.builder().with(MIMIC, mimic).build();
        }

        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            tag.put("Mimic", NbtUtils.writeBlockState(mimic));
            if (owner != null) {
                tag.putUUID("Owner", owner);
            }
            tag.putInt("Light", light);
        }

        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            mimic = NbtUtils.readBlockState(level != null ? level.holderLookup(Registries.BLOCK) : BuiltInRegistries.BLOCK.asLookup(),
                    tag.getCompound("Mimic"));
            if (mimic.isAir() || mimic.is(ModBlocks.WARDED.get())) {
                mimic = Blocks.STONE.defaultBlockState();
            }
            owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
            light = tag.getInt("Light");
            if (level != null && level.isClientSide) {
                // на клиенте блок надо перерисовать: его модель зависит от того, что он изображает
                requestModelDataUpdate();
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 8);
                level.getChunkSource().getLightEngine().checkBlock(worldPosition);
            }
        }

        @Override
        public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet) {
            super.onDataPacket(connection, packet);
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
