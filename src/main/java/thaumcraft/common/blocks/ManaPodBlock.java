package thaumcraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.Tags;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.items.ManaBeanItem;
import thaumcraft.common.registry.ModBlockEntities;
import thaumcraft.common.registry.ModItems;

import java.util.ArrayList;
import java.util.List;

/**
 * Стручок маны (BlockManaPod оригинала): растёт под бревном в волшебных биомах, восемь стадий. На третьей стадии
 * в нём определяется аспект: свой от посаженного зерна, смесь с аспектами соседних стручков или случайный первичный.
 * Зрелый стручок даёт одно-два зерна маны этого аспекта, недозрелый (с третьей стадии) — одно.
 */
public class ManaPodBlock extends Block implements EntityBlock {
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 7);
    private static final int[] BOTTOM = {12, 10, 8, 6, 5, 4, 3, 2};
    private static final VoxelShape[] SHAPES = new VoxelShape[8];

    static {
        for (int age = 0; age < 8; age++) {
            SHAPES[age] = box(4, BOTTOM[age], 4, 12, 16, 12);
        }
    }

    public ManaPodBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(AGE)];
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    /** Стручку нужны волшебный биом и бревно над головой. */
    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBiome(pos).is(Tags.Biomes.IS_MAGICAL) && level.getBlockState(pos.above()).is(BlockTags.LOGS);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level, BlockPos pos,
                                  BlockPos neighbourPos) {
        return canSurvive(state, level, pos) ? state : Blocks.AIR.defaultBlockState();
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!canSurvive(state, level, pos)) {
            level.destroyBlock(pos, true);
        } else if (random.nextInt(30) == 0 && level.getBlockEntity(pos) instanceof Pod pod) {
            pod.checkGrowth();
        }
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = new ArrayList<>();
        int age = state.getValue(AGE);
        if (age < 2) {
            return drops;
        }
        Aspect aspect = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof Pod pod && pod.aspect != null
                ? pod.aspect : Aspect.PLANT;
        int count = age == 7 && params.getLevel().random.nextFloat() > 0.33f ? 2 : 1;
        for (int i = 0; i < count; i++) {
            drops.add(ManaBeanItem.of(ModItems.MANA_BEAN.get(), aspect));
        }
        return drops;
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos, net.minecraft.world.entity.player.Player player) {
        Aspect aspect = level.getBlockEntity(pos) instanceof Pod pod ? pod.aspect : null;
        return aspect == null ? new ItemStack(ModItems.MANA_BEAN.get()) : ManaBeanItem.of(ModItems.MANA_BEAN.get(), aspect);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Pod(pos, state);
    }

    /** TileManaPod оригинала: помнит аспект стручка. */
    public static class Pod extends BlockEntity implements thaumcraft.api.aspects.AspectContainer {
        @Nullable
        public Aspect aspect;

        public Pod(BlockPos pos, BlockState state) {
            super(ModBlockEntities.MANA_POD.get(), pos, state);
        }

        public int getAge() {
            return getBlockState().getValue(AGE);
        }

        /** Следующая стадия роста; на третьей решается, какого аспекта будет стручок. */
        public void checkGrowth() {
            if (level == null) {
                return;
            }
            int age = getAge();
            if (age < 7) {
                age++;
                level.setBlock(worldPosition, getBlockState().setValue(AGE, age), 3);
            }
            if (age <= 2) {
                return;
            }
            RandomSource random = level.random;
            if (age == 3) {
                AspectList around = new AspectList();
                if (aspect != null) {
                    around.add(aspect, 1);
                }
                for (Direction dir : Direction.Plane.HORIZONTAL) {
                    if (level.getBlockEntity(worldPosition.relative(dir)) instanceof Pod other && other.aspect != null) {
                        around.add(other.aspect, 1);
                    }
                }
                // соседние стручки скрещиваются: может получиться и свой аспект, и чужой, и — вдвое вероятнее — их соединение
                if (around.size() > 1) {
                    List<Aspect> all = around.getAspects();
                    List<Aspect> options = new ArrayList<>();
                    for (int i = 0; i < all.size(); i++) {
                        options.add(all.get(i));
                        for (int j = 0; j < all.size(); j++) {
                            Aspect combo = i == j ? null : Aspect.combine(all.get(i), all.get(j));
                            if (combo != null) {
                                options.add(combo);
                                options.add(combo);
                            }
                        }
                    }
                    aspect = options.get(random.nextInt(options.size()));
                }
                if (around.size() >= 1 && aspect == null) {
                    aspect = around.getAspectsSortedByAmount().get(0);
                }
            }
            if (aspect == null) {
                List<Aspect> primals = Aspect.primals();
                aspect = random.nextInt(8) == 0 ? Aspect.PLANT : primals.get(random.nextInt(primals.size()));
            }
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }

        /** Аспект виден в очках и таумометру только у зрелого стручка. */
        @Override
        public AspectList getContainedAspects() {
            return aspect != null && getAge() == 7 ? new AspectList().add(aspect, 1) : new AspectList();
        }

        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            if (aspect != null) {
                tag.putString("aspect", aspect.getTag());
            }
        }

        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            aspect = Aspect.byTag(tag.getString("aspect"));
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
