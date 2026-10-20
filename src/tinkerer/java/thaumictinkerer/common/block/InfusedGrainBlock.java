package thaumictinkerer.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectContainer;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.network.FxPacket;
import thaumictinkerer.common.AspectCropLoot;
import thaumictinkerer.common.item.InfusedSeedsItem;
import thaumictinkerer.common.registry.TTBlockEntities;
import thaumictinkerer.common.registry.TTBlocks;
import thaumictinkerer.common.registry.TTItems;

import java.util.ArrayList;
import java.util.List;

/**
 * Наполненное зерно (BlockInfusedGrain + TileInfusedGrain). Растёт только на наполненной пашне при свете от 9, быстрее
 * со склонностью к воде. Склонности (не больше 50 на растение) перетекают к нему от соседних первичных и несущих склонности
 * растений, быстрее со склонностью к воздуху. Спелое зерно при сборе оставляет аспект в пашне (чаще с землёй), даёт добычу
 * своего аспекта (больше с порядком) и семена; семена получают сложный аспект, если в пашне много подходящего (чаще с огнём),
 * а с энтропией их выпадает больше.
 */
public class InfusedGrainBlock extends CropBlock implements EntityBlock {
    /** Какую из семи раскрасок брать: аер, игнис, аква, терра, ордо, пердицио, прочие. */
    public static final IntegerProperty TEXTURE = IntegerProperty.create("texture", 0, 6);
    public static final int MAX_TENDENCIES = 50;

    public InfusedGrainBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(TEXTURE, 6));
    }

    public static int texture(@Nullable Aspect aspect) {
        if (aspect == Aspect.AIR) {
            return 0;
        } else if (aspect == Aspect.FIRE) {
            return 1;
        } else if (aspect == Aspect.WATER) {
            return 2;
        } else if (aspect == Aspect.EARTH) {
            return 3;
        } else if (aspect == Aspect.ORDER) {
            return 4;
        }
        return aspect == Aspect.ENTROPY ? 5 : 6;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(TEXTURE);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(TTBlocks.INFUSED_FARMLAND.get());
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return TTItems.INFUSED_SEEDS.get();
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return level.getBlockEntity(pos) instanceof Tile tile && tile.aspect != null
                ? InfusedSeedsItem.of(tile.aspect, tile.tendencies) : new ItemStack(TTItems.INFUSED_SEEDS.get());
    }

    /** Обычного роста по случайным тикам нет: зерно растит его собственный тайл. */
    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
    }

    /** Костная мука растит, не сбрасывая раскраску. */
    @Override
    public void growCrops(Level level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state.setValue(AGE, Math.min(getMaxAge(), getAge(state) + getBonemealAgeIncrease(level))), Block.UPDATE_CLIENTS);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return mayPlaceOn(level.getBlockState(pos.below()), level, pos.below());
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!newState.is(this) && level instanceof ServerLevel server && level.getBlockEntity(pos) instanceof Tile tile) {
            tile.dropHarvest(server, state);
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Tile(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide || type != TTBlockEntities.INFUSED_GRAIN.get() ? null
                : (l, p, s, tile) -> ((Tile) tile).tick(s);
    }

    public static class Tile extends BlockEntity implements AspectContainer {
        @Nullable
        public Aspect aspect;
        public AspectList tendencies = new AspectList();

        public Tile(BlockPos pos, BlockState state) {
            super(TTBlockEntities.INFUSED_GRAIN.get(), pos, state);
        }

        public void setup(Aspect aspect, AspectList tendencies) {
            this.aspect = aspect;
            this.tendencies = tendencies.copy();
            sync();
        }

        void tick(BlockState state) {
            if (aspect == null) {
                return;
            }
            RandomSource random = level.random;
            InfusedGrainBlock block = (InfusedGrainBlock) state.getBlock();
            int water = tendencies.getAmount(Aspect.WATER);
            if (level.getMaxLocalRawBrightness(worldPosition.above()) >= 9 && block.getAge(state) < block.getMaxAge()
                    && random.nextInt((2510 - water * water) * 6) == 0) {
                level.setBlock(worldPosition, state.setValue(AGE, block.getAge(state) + 1), Block.UPDATE_CLIENTS);
            }
            int air = tendencies.getAmount(Aspect.AIR);
            if (!aspect.isPrimal() && random.nextInt((2550 - air * air) * 10) == 0) {
                absorb(random);
            }
        }

        /** Сложное растение перенимает склонность у соседа: у первичного — его аспект (до 5), у сложного — одну из его склонностей. */
        private void absorb(RandomSource random) {
            for (Direction direction : Direction.values()) {
                if (!(level.getBlockEntity(worldPosition.relative(direction)) instanceof Tile near) || near.aspect == null) {
                    continue;
                }
                Aspect taken;
                if (near.aspect.isPrimal()) {
                    if (tendencies.getAmount(near.aspect) >= 5) {
                        continue;
                    }
                    taken = near.aspect;
                } else {
                    List<Aspect> list = near.tendencies.getAspects();
                    if (list.isEmpty()) {
                        return;
                    }
                    taken = list.get(random.nextInt(list.size()));
                    if (near.tendencies.getAmount(taken) < tendencies.getAmount(taken)) {
                        return;
                    }
                    near.tendencies.reduce(taken, 1);
                    near.sync();
                }
                tendencies.add(taken, 1);
                saturate(tendencies, MAX_TENDENCIES, random);
                sync();
                FxPacket.send((ServerLevel) level, FxPacket.ESSENTIA, Vec3.atCenterOf(near.worldPosition), Vec3.atCenterOf(worldPosition),
                        taken.getColor(), 0);
                return;
            }
        }

        /** Что выпадает, когда зерно убрано: семена всегда, у спелого — добыча аспекта и след в пашне. */
        void dropHarvest(ServerLevel level, BlockState state) {
            if (aspect == null) {
                return;
            }
            RandomSource random = level.random;
            boolean ripe = ((InfusedGrainBlock) state.getBlock()).isMaxAge(state);
            List<ItemStack> drops = new ArrayList<>();
            ItemStack seeds = InfusedSeedsItem.of(dropAspect(ripe, random), tendencies);
            int entropy = tendencies.getAmount(Aspect.ENTROPY);
            while (random.nextInt(10000) < entropy * entropy && seeds.getCount() < seeds.getMaxStackSize()) {
                seeds.grow(1);
            }
            drops.add(seeds);
            if (ripe) {
                if (level.getBlockEntity(worldPosition.below()) instanceof InfusedFarmlandBlock.Tile soil) {
                    do {
                        soil.aspects.add(aspect, 1);
                        saturate(soil.aspects, InfusedFarmlandBlock.MAX_ASPECTS, random);
                    } while (random.nextInt(55) < tendencies.getAmount(Aspect.EARTH));
                    soil.sync();
                }
                do {
                    drops.add(AspectCropLoot.get(aspect, random));
                } while (random.nextInt(75) < tendencies.getAmount(Aspect.ORDER));
            }
            for (ItemStack drop : drops) {
                if (drop.isEmpty()) {
                    continue;
                }
                ItemEntity item = new ItemEntity(level, worldPosition.getX() + 0.15 + random.nextFloat() * 0.7,
                        worldPosition.getY() + 0.15 + random.nextFloat() * 0.7, worldPosition.getZ() + 0.15 + random.nextFloat() * 0.7, drop);
                item.setPickUpDelay(10);
                level.addFreshEntity(item);
            }
        }

        /** Сложный аспект из аспекта зерна и следа в пашне: шанс растёт с квадратом следа и со склонностью к огню. */
        private Aspect dropAspect(boolean ripe, RandomSource random) {
            if (ripe && level.getBlockEntity(worldPosition.below()) instanceof InfusedFarmlandBlock.Tile soil) {
                int fire = tendencies.getAmount(Aspect.FIRE);
                for (Aspect residue : soil.aspects.getAspects()) {
                    int amount = soil.aspects.getAmount(residue);
                    Aspect result = Aspect.combine(residue, aspect);
                    if (result != null && random.nextInt(10) < (fire + 1) * amount * amount) {
                        return result;
                    }
                }
            }
            return aspect;
        }

        /** Сверх предела склонности снимаются по одной со случайных аспектов. */
        static void saturate(AspectList list, int max, RandomSource random) {
            int total = list.getAspects().stream().mapToInt(list::getAmount).sum();
            for (int extra = total - max; extra > 0; extra--) {
                List<Aspect> aspects = list.getAspects();
                list.reduce(aspects.get(random.nextInt(aspects.size())), 1);
            }
        }

        @Nullable
        @Override
        public AspectList getContainedAspects() {
            return aspect == null ? null : new AspectList().add(aspect, 1);
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
            if (aspect != null) {
                tag.putString("aspect", aspect.getTag());
            }
            tag.put("tendencies", tendencies.save(new CompoundTag()));
        }

        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            aspect = Aspect.byTag(tag.getString("aspect"));
            tendencies = AspectList.load(tag.getCompound("tendencies"));
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
