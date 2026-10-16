package thaumcraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import thaumcraft.common.entity.Cultist;
import thaumcraft.common.entity.CultistCleric;
import thaumcraft.common.entity.CultistKnight;
import thaumcraft.common.registry.ModBlockEntities;
import thaumcraft.common.registry.ModEntities;
import thaumcraft.common.registry.ModItems;
import thaumcraft.common.registry.ModSounds;
import thaumcraft.common.world.outer.Labyrinth;

import java.util.List;

/**
 * Алтарь Элдрича — перенос BlockEldritch (мета 0) и TileEldritchAltar оригинала. Стоит в середине колец Элдрича,
 * под парящим обелиском. Некоторые алтари призывают Багровый культ (четырёх клириков для обряда, потом рыцарей
 * охраны, пока жив хоть один клирик) или стражей Элдрича. В алтарь вставляют четыре ока Элдрича (после третьего
 * он начинает призывать стражей); алтарь с четырьмя очами и тёмным узлом над ним жезл открывает в портал
 * Запредельных земель. Каждое око готовит там лабиринт.
 */
public class EldritchAltarBlock extends BaseEntityBlock {
    public EldritchAltarBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Altar(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.ELDRITCH_ALTAR.get(), (l, p, s, altar) -> altar.serverTick());
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of();
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return ItemStack.EMPTY;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        super.onRemove(state, level, pos, newState, moving);
        if (!newState.is(this) && !level.isClientSide) {
            EldritchObeliskBlock.collapse(level, pos);
        }
    }

    /** onBlockActivated оригинала: око Элдрича вставляется в алтарь. */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        if (player.isShiftKeyDown() || !held.is(ModItems.ELDRITCH_EYE.get()) || !(level.getBlockEntity(pos) instanceof Altar altar)) {
            return InteractionResult.PASS;
        }
        if (altar.eyes >= 4) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            if (altar.eyes >= 2) {
                altar.spawner = true;
                altar.spawnType = 1;
            }
            altar.eyes++;
            altar.checkForMaze();
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
            altar.sync();
            level.playSound(null, pos, ModSounds.CRYSTAL.get(), SoundSource.BLOCKS, 0.2f, 1f);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public static class Altar extends BlockEntity {
        public boolean spawner;
        public boolean open;
        private boolean spawnedClerics;
        public byte spawnType;
        public byte eyes;
        private int counter;

        public Altar(BlockPos pos, BlockState state) {
            super(ModBlockEntities.ELDRITCH_ALTAR.get(), pos, state);
        }

        void sync() {
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }

        void serverTick() {
            if (spawner && counter++ >= 80 && counter % 40 == 0) {
                if (spawnType == 0) {
                    if (!spawnedClerics) {
                        spawnClerics();
                    } else {
                        spawnGuards();
                    }
                } else if (spawnType == 1) {
                    spawnGuardian();
                }
            }
        }

        private static int offset(net.minecraft.util.RandomSource random, int min, int max) {
            return Mth.randomBetweenInclusive(random, min, max) * Mth.randomBetweenInclusive(random, -1, 1);
        }

        /** Рыцари охраны, пока у алтаря жив хоть один клирик и культистов меньше восьми. */
        private void spawnGuards() {
            AABB area = new AABB(worldPosition).inflate(24, 16, 24);
            if (level.getEntitiesOfClass(CultistCleric.class, area).isEmpty()) {
                spawner = false;
                setChanged();
                return;
            }
            if (level.getEntitiesOfClass(Cultist.class, area).size() >= 8) {
                return;
            }
            var random = level.random;
            BlockPos at = worldPosition.offset(offset(random, 4, 10), offset(random, 0, 3), offset(random, 4, 10));
            CultistKnight knight = ModEntities.CULTIST_KNIGHT.get().create(level);
            if (knight != null && level.getBlockState(at.below()).isFaceSturdy(level, at.below(), net.minecraft.core.Direction.UP)) {
                knight.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, random.nextFloat() * 360f, 0f);
                if (level.noCollision(knight) && !level.containsAnyLiquid(knight.getBoundingBox())) {
                    knight.finalizeSpawn((ServerLevel) level, level.getCurrentDifficultyAt(at), MobSpawnType.STRUCTURE, null, null);
                    knight.spawnAnim();
                    knight.restrictTo(worldPosition, 16);
                    level.addFreshEntity(knight);
                }
            }
        }

        private void spawnGuardian() {
            var random = level.random;
            BlockPos at = worldPosition.offset(offset(random, 4, 10), offset(random, 0, 3), offset(random, 4, 10));
            var guardian = ModEntities.ELDRITCH_GUARDIAN.get().create(level);
            if (guardian != null && level.getBlockState(at.below()).isFaceSturdy(level, at.below(), net.minecraft.core.Direction.UP)) {
                guardian.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, random.nextFloat() * 360f, 0f);
                if (guardian.checkSpawnObstruction(level) && level.noCollision(guardian)) {
                    guardian.finalizeSpawn((ServerLevel) level, level.getCurrentDifficultyAt(at), MobSpawnType.STRUCTURE, null, null);
                    guardian.spawnAnim();
                    guardian.restrictTo(worldPosition, 16);
                    level.addFreshEntity(guardian);
                }
            }
        }

        /** Четыре клирика по углам алтаря становятся на обряд. */
        private void spawnClerics() {
            int success = 0;
            int[][] corners = {{-2, -2}, {-2, 2}, {2, -2}, {2, 2}};
            for (int[] c : corners) {
                BlockPos at = worldPosition.offset(c[0], 0, c[1]);
                if (!level.getBlockState(at.below()).isFaceSturdy(level, at.below(), net.minecraft.core.Direction.UP)) {
                    continue;
                }
                CultistCleric cleric = ModEntities.CULTIST_CLERIC.get().create(level);
                if (cleric == null) {
                    continue;
                }
                cleric.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0f, 0f);
                if (level.noCollision(cleric) && !level.containsAnyLiquid(cleric.getBoundingBox())) {
                    cleric.setAltar(worldPosition);
                    cleric.finalizeSpawn((ServerLevel) level, level.getCurrentDifficultyAt(at), MobSpawnType.STRUCTURE, null, null);
                    cleric.spawnAnim();
                    if (level.addFreshEntity(cleric)) {
                        success++;
                        cleric.setRitualist(true);
                    }
                }
            }
            if (success > 2) {
                spawnedClerics = true;
                setChanged();
            }
        }

        /** checkForMaze оригинала: если поблизости в Запредельных землях ещё нет лабиринта — заложить его. */
        public boolean checkForMaze() {
            if (!(level instanceof ServerLevel server)) {
                return false;
            }
            int w = 15 + level.random.nextInt(8) * 2;
            int h = 15 + level.random.nextInt(8) * 2;
            Labyrinth maze = Labyrinth.get(server.getServer());
            if (!maze.mazesInRange(worldPosition.getX() >> 4, worldPosition.getZ() >> 4, w, h)) {
                maze.generate(worldPosition.getX() >> 4, worldPosition.getZ() >> 4, w, h, level.random.nextLong());
                return false;
            }
            return true;
        }

        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            tag.putByte("eyes", eyes);
            tag.putBoolean("open", open);
            tag.putBoolean("spawnedClerics", spawnedClerics);
            tag.putBoolean("spawner", spawner);
            tag.putByte("spawntype", spawnType);
        }

        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            eyes = tag.getByte("eyes");
            open = tag.getBoolean("open");
            spawnedClerics = tag.getBoolean("spawnedClerics");
            spawner = tag.getBoolean("spawner");
            spawnType = tag.getByte("spawntype");
        }

        @Override
        public CompoundTag getUpdateTag() {
            CompoundTag tag = super.getUpdateTag();
            tag.putByte("eyes", eyes);
            tag.putBoolean("open", open);
            return tag;
        }

        @Override
        public Packet<ClientGamePacketListener> getUpdatePacket() {
            return ClientboundBlockEntityDataPacket.create(this);
        }
    }
}
