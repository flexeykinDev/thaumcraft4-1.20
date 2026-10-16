package thaumcraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.EssentiaTransport;
import thaumcraft.api.crafting.SmeltingBonus;
import thaumcraft.api.visnet.VisNetHandler;
import thaumcraft.common.registry.ModBlockEntities;
import thaumcraft.common.registry.ModBlocks;
import thaumcraft.common.registry.ModSounds;

import java.util.List;
import java.util.Optional;

/**
 * Адская печь — перенос BlockArcaneFurnace, TileArcaneFurnace и TileArcaneFurnaceNozzle оригинала. Куб 3×3×3 из
 * незерского кирпича (углы) и обсидиана с лавой в середине и железной решёткой в одной из боковых граней; жезл
 * превращает его в печь. Брошенное в отверстие сверху плавится без топлива и вылетает из решётки, изредка
 * с бонусом (самородки из руды, мясные кусочки из мяса). Огненный вис из сети или эссенция Ignis через сопла
 * ускоряет плавку, мехи у граней — тоже, и они же повышают шанс бонуса. Сломанная печь рассыпается обратно
 * в исходные блоки, лава пропадает, а из неё вырывается ифрит.
 */
public class InfernalFurnaceBlock extends BaseEntityBlock {
    /** Место блока в кубе: слой·9 + z·3 + x, слой 0 — нижний. 13 — лавовое ядро, 22 — пустота над ним. */
    public static final IntegerProperty POS = IntegerProperty.create("pos", 0, 26);
    /** Сторона, где стоит решётка. */
    public static final DirectionProperty FRONT = BlockStateProperties.HORIZONTAL_FACING;
    public static final int CORE = 13;
    private static final VoxelShape CORE_SHAPE = box(0, 0, 0, 16, 4, 16);

    public InfernalFurnaceBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(POS, 0).setValue(FRONT, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POS, FRONT);
    }

    public static int pos(int x, int y, int z) {
        return y * 9 + z * 3 + x;
    }

    /** Номер места решётки для данной стороны. */
    public static int bars(Direction front) {
        return pos(1 + front.getStepX(), 1, 1 + front.getStepZ());
    }

    public static boolean isCore(BlockState state) {
        return state.getValue(POS) == CORE;
    }

    public static boolean isBars(BlockState state) {
        return state.getValue(POS) == bars(state.getValue(FRONT));
    }

    /** Нижний северо-западный угол куба. */
    public static BlockPos origin(BlockPos pos, BlockState state) {
        int p = state.getValue(POS);
        return pos.offset(-(p % 3), -(p / 9), -(p / 3 % 3));
    }

    public static int light(BlockState state) {
        return isCore(state) || isBars(state) ? 13 : 3;
    }

    // ---------- сборка ----------

    /**
     * WandManager.fitArcaneFurnace и replaceArcaneFurnace оригинала: ищет куб вокруг блока, которого коснулся жезл,
     * и превращает его в печь. Возвращает нижний угол куба или null.
     */
    @Nullable
    public static BlockPos find(Level level, BlockPos clicked) {
        for (int x = -2; x <= 0; x++) {
            for (int y = -2; y <= 0; y++) {
                for (int z = -2; z <= 0; z++) {
                    BlockPos origin = clicked.offset(x, y, z);
                    if (frontOf(level, origin) != null) {
                        return origin;
                    }
                }
            }
        }
        return null;
    }

    /** Сторона решётки, если куб собран правильно (ровно одна решётка в середине боковой грани), иначе null. */
    @Nullable
    public static Direction frontOf(Level level, BlockPos origin) {
        Direction front = null;
        for (int y = 0; y < 3; y++) {
            for (int z = 0; z < 3; z++) {
                for (int x = 0; x < 3; x++) {
                    BlockState state = level.getBlockState(origin.offset(x, y, z));
                    boolean corner = x != 1 && z != 1;
                    boolean centre = x == 1 && z == 1;
                    if (centre) {
                        boolean ok = switch (y) {
                            case 0 -> state.is(Blocks.OBSIDIAN);
                            case 1 -> state.is(Blocks.LAVA) && state.getFluidState().isSource();
                            default -> state.isAir();
                        };
                        if (!ok) {
                            return null;
                        }
                    } else if (corner) {
                        if (!state.is(Blocks.NETHER_BRICKS)) {
                            return null;
                        }
                    } else if (!state.is(Blocks.OBSIDIAN)) {
                        if (y != 1 || front != null || !state.is(Blocks.IRON_BARS)) {
                            return null;
                        }
                        front = Direction.fromDelta(x - 1, 0, z - 1);
                    }
                }
            }
        }
        return front;
    }

    public static void assemble(ServerLevel level, BlockPos origin, Direction front) {
        BlockState base = ModBlocks.INFERNAL_FURNACE.get().defaultBlockState().setValue(FRONT, front);
        for (int y = 0; y < 3; y++) {
            for (int z = 0; z < 3; z++) {
                for (int x = 0; x < 3; x++) {
                    BlockPos pos = origin.offset(x, y, z);
                    if (!level.getBlockState(pos).isAir()) {
                        level.setBlock(pos, base.setValue(POS, pos(x, y, z)), 2);
                        thaumcraft.common.network.FxPacket.blockSparkle(level, pos, 16736256, 5);
                    }
                }
            }
        }
        for (int y = 0; y < 3; y++) {
            for (int z = 0; z < 3; z++) {
                for (int x = 0; x < 3; x++) {
                    BlockPos pos = origin.offset(x, y, z);
                    level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
                    level.updateNeighborsAt(pos, ModBlocks.INFERNAL_FURNACE.get());
                }
            }
        }
        level.playSound(null, origin.offset(1, 1, 1), ModSounds.WAND.get(), SoundSource.BLOCKS, 1f, 1f);
    }

    /** Чем был блок до превращения: углы — незерский кирпич, решётка — решётка, ядро — ничто (лава пропадает). */
    public static BlockState original(BlockState state) {
        int p = state.getValue(POS);
        if (p == CORE || p == 22) {
            return Blocks.AIR.defaultBlockState();
        }
        if (isBars(state)) {
            return Blocks.IRON_BARS.defaultBlockState();
        }
        int x = p % 3;
        int z = p / 3 % 3;
        return x != 1 && z != 1 ? Blocks.NETHER_BRICKS.defaultBlockState() : Blocks.OBSIDIAN.defaultBlockState();
    }

    /** restoreBlocks оригинала: вся печь рассыпается обратно в исходные блоки. */
    private static void restore(Level level, BlockPos origin) {
        for (int y = 0; y < 3; y++) {
            for (int z = 0; z < 3; z++) {
                for (int x = 0; x < 3; x++) {
                    BlockPos pos = origin.offset(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (state.getBlock() instanceof InfernalFurnaceBlock) {
                        level.setBlock(pos, original(state), 3);
                    }
                }
            }
        }
    }

    // ---------- поведение блока ----------

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!newState.is(this) && !level.isClientSide) {
            BlockPos origin = origin(pos, state);
            if (isCore(state)) {
                // onBlockPreDestroy оригинала: из погасшей печи вырывается ифрит
                Blaze blaze = EntityType.BLAZE.create(level);
                if (blaze != null) {
                    blaze.moveTo(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 0f, 0f);
                    blaze.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 6000, 2));
                    blaze.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 12000, 0));
                    level.addFreshEntity(blaze);
                }
                if (level.getBlockEntity(pos) instanceof Core core) {
                    core.dropContents();
                }
                super.onRemove(state, level, pos, newState, moving);
                restore(level, origin);
                return;
            }
            // сломан кусок оболочки: ядро гаснет, и печь рассыпается
            BlockPos core = origin.offset(1, 1, 1);
            BlockState coreState = level.getBlockState(core);
            if (coreState.is(this) && isCore(coreState)) {
                super.onRemove(state, level, pos, newState, moving);
                level.setBlock(core, Blocks.AIR.defaultBlockState(), 3);
                return;
            }
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        BlockState original = original(state);
        return original.isAir() ? List.of() : List.of(new ItemStack(original.getBlock()));
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        BlockState original = original(state);
        return original.isAir() ? ItemStack.EMPTY : new ItemStack(original.getBlock());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return isCore(state) ? CORE_SHAPE : Shapes.block();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (isBars(state)) {
            // решётка утоплена: перед ней полблока пустоты
            Direction front = state.getValue(FRONT);
            return switch (front) {
                case NORTH -> box(0, 0, 8, 16, 16, 16);
                case SOUTH -> box(0, 0, 0, 16, 16, 8);
                case WEST -> box(8, 0, 0, 16, 16, 16);
                default -> box(0, 0, 0, 8, 16, 16);
            };
        }
        return getShape(state, level, pos, context);
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return isCore(state) || isBars(state) ? Shapes.empty() : Shapes.block();
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /** onEntityCollidedWithBlock оригинала: предметы тянет к середине ядра и затягивает внутрь, живое обжигает. */
    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!isCore(state)) {
            return;
        }
        Vec3 motion = entity.getDeltaMovement();
        double mx = motion.x;
        double mz = motion.z;
        if (entity.getX() < pos.getX() + 0.3) {
            mx += 1.0E-4;
        }
        if (entity.getX() > pos.getX() + 0.7) {
            mx -= 1.0E-4;
        }
        if (entity.getZ() < pos.getZ() + 0.3) {
            mz += 1.0E-4;
        }
        if (entity.getZ() > pos.getZ() + 0.7) {
            mz -= 1.0E-4;
        }
        if (entity instanceof ItemEntity item) {
            entity.setDeltaMovement(mx, 0.025, mz);
            if (!level.isClientSide && entity.onGround() && level.getBlockEntity(pos) instanceof Core core
                    && core.addItemsToInventory(item.getItem())) {
                item.discard();
            }
        } else {
            entity.setDeltaMovement(mx, motion.y, mz);
            if (entity instanceof LivingEntity && !entity.fireImmune()) {
                entity.hurt(level.damageSources().lava(), 3f);
                entity.setSecondsOnFire(10);
            }
        }
    }

    /** randomDisplayTick оригинала: из отверстия над ядром валит дым. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, net.minecraft.util.RandomSource random) {
        if (isCore(state) && level.isEmptyBlock(pos.above())) {
            for (int a = 0; a < 3; a++) {
                level.addParticle(ParticleTypes.LARGE_SMOKE, pos.getX() + random.nextFloat(), pos.getY() + 1.0 + random.nextFloat() * 0.5,
                        pos.getZ() + random.nextFloat(), 0, 0, 0);
            }
        }
    }

    /** Событие 3 — предмет выплавился: брызги лавы из решётки и бульканье (furnaceLavaFx оригинала). */
    @Override
    public boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
        if (id != 3) {
            return super.triggerEvent(state, level, pos, id, param);
        }
        if (level.isClientSide) {
            Direction front = state.getValue(FRONT);
            var random = level.random;
            for (int a = 0; a < 5; a++) {
                thaumcraft.client.fx.Fx.furnaceLava(pos, front.getStepX(), front.getStepZ());
                level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.LAVA_POP, SoundSource.BLOCKS,
                        0.1f + random.nextFloat() * 0.1f, 0.9f + random.nextFloat() * 0.15f, false);
            }
        }
        return true;
    }

    // ---------- сущности блока ----------

    /** Сопла — середины боковых граней среднего слоя (кроме решётки) и середина дна: они касаются ядра. */
    private static boolean isNozzle(BlockState state) {
        int p = state.getValue(POS);
        return !isBars(state) && (p == 4 || p == 10 || p == 12 || p == 14 || p == 16);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if (isCore(state)) {
            return new Core(pos, state);
        }
        return isNozzle(state) ? new Nozzle(pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        if (type == ModBlockEntities.INFERNAL_FURNACE.get()) {
            return createTickerHelper(type, ModBlockEntities.INFERNAL_FURNACE.get(), (l, p, s, core) -> core.serverTick());
        }
        return createTickerHelper(type, ModBlockEntities.INFERNAL_FURNACE_NOZZLE.get(), (l, p, s, nozzle) -> nozzle.serverTick());
    }

    /** TileArcaneFurnace оригинала: лавовое ядро с 32 ячейками того, что плавится. */
    public static class Core extends BlockEntity {
        private final NonNullList<ItemStack> items = NonNullList.withSize(32, ItemStack.EMPTY);
        public int cookTime;
        public int maxCookTime;
        public int speedyTime;

        public Core(BlockPos pos, BlockState state) {
            super(ModBlockEntities.INFERNAL_FURNACE.get(), pos, state);
        }

        private Direction front() {
            return getBlockState().getValue(FRONT);
        }

        void dropContents() {
            for (ItemStack stack : items) {
                if (!stack.isEmpty()) {
                    net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1.2,
                            worldPosition.getZ() + 0.5, stack);
                }
            }
            items.clear();
        }

        void serverTick() {
            boolean cooked = false;
            if (cookTime > 0) {
                cookTime--;
                cooked = true;
            }
            if (cooked && speedyTime > 0) {
                speedyTime--;
            }
            if (speedyTime <= 0) {
                speedyTime = VisNetHandler.drainVis(level, worldPosition, Aspect.FIRE, 5);
            }
            if (maxCookTime == 0) {
                maxCookTime = calcCookTime();
            }
            if (cookTime > maxCookTime) {
                cookTime = maxCookTime;
            }
            if (cookTime == 0 && cooked) {
                for (int a = 0; a < items.size(); a++) {
                    ItemStack stack = items.get(a);
                    if (stack.isEmpty()) {
                        continue;
                    }
                    Optional<SmeltingRecipe> recipe = recipe(stack);
                    if (recipe.isPresent()) {
                        eject(recipe.get().getResultItem(level.registryAccess()).copy(), stack, recipe.get().getExperience());
                        level.blockEvent(worldPosition, getBlockState().getBlock(), 3, 0);
                        stack.shrink(1);
                        setChanged();
                        break;
                    }
                }
            }
            if (cookTime == 0 && !cooked) {
                for (ItemStack stack : items) {
                    if (!stack.isEmpty() && recipe(stack).isPresent()) {
                        maxCookTime = calcCookTime();
                        cookTime = maxCookTime;
                        break;
                    }
                }
            }
        }

        private Optional<SmeltingRecipe> recipe(ItemStack stack) {
            return level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SimpleContainer(stack), level);
        }

        /** getBellows оригинала: мехи через блок от ядра, дующие на печь со всех сторон, кроме верха (не больше трёх). */
        private int bellows() {
            int count = 0;
            for (Direction side : Direction.values()) {
                if (side != Direction.UP) {
                    count += BellowsBlock.count(level, worldPosition.relative(side), List.of(side));
                }
            }
            return Math.min(3, count);
        }

        private int calcCookTime() {
            return (speedyTime > 0 ? 80 : 140) - 20 * bellows();
        }

        /** addItemsToInventory оригинала: то, что не плавится, сгорает с шипением. */
        public boolean addItemsToInventory(ItemStack stack) {
            for (int a = 0; a < items.size(); a++) {
                ItemStack slot = items.get(a);
                if (!slot.isEmpty() && ItemStack.isSameItemSameTags(slot, stack) && slot.getCount() + stack.getCount() <= stack.getMaxStackSize()) {
                    slot.grow(stack.getCount());
                    if (recipe(slot).isEmpty()) {
                        destroyItem(a);
                    }
                    setChanged();
                    return true;
                }
                if (slot.isEmpty()) {
                    items.set(a, stack.copy());
                    if (recipe(stack).isEmpty()) {
                        destroyItem(a);
                    }
                    setChanged();
                    return true;
                }
            }
            return false;
        }

        private void destroyItem(int slot) {
            items.set(slot, ItemStack.EMPTY);
            level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.3f,
                    2.6f + (level.random.nextFloat() - level.random.nextFloat()) * 0.8f);
            ((ServerLevel) level).sendParticles(ParticleTypes.LAVA, worldPosition.getX() + level.random.nextFloat(), worldPosition.getY() + 1,
                    worldPosition.getZ() + level.random.nextFloat(), 1, 0, 0, 0, 0);
        }

        private ItemEntity spit(ItemStack stack) {
            Direction front = front();
            double lx = 0.5 + front.getStepX() * 1.2;
            double lz = 0.5 + front.getStepZ() * 1.2;
            ItemEntity item = new ItemEntity(level, worldPosition.getX() + lx, worldPosition.getY() + 0.4, worldPosition.getZ() + lz, stack);
            item.setDeltaMovement(sideways(front.getStepX(), 0.03f), 0, sideways(front.getStepZ(), 0.03f));
            level.addFreshEntity(item);
            return item;
        }

        private double sideways(int step, float spread) {
            return step == 0 ? (level.random.nextFloat() - level.random.nextFloat()) * spread : step * 0.13f;
        }

        /** ejectItem оригинала: результат, иногда бонус и опыт вылетают из решётки. */
        private void eject(ItemStack result, ItemStack input, float experience) {
            int bellows = bellows();
            spit(result.copy());
            ItemStack bonus = SmeltingBonus.get(input);
            if (!bonus.isEmpty()) {
                int count = 0;
                if (bellows == 0) {
                    if (level.random.nextInt(4) == 0) {
                        count++;
                    }
                } else {
                    for (int a = 0; a < bellows; a++) {
                        if (level.random.nextFloat() < 0.44f) {
                            count++;
                        }
                    }
                }
                if (count > 0) {
                    spit(bonus.copyWithCount(count));
                }
            }
            int xp = result.getCount();
            if (experience == 0f) {
                xp = 0;
            } else if (experience < 1f) {
                int whole = Mth.floor(xp * experience);
                if (whole < Mth.ceil(xp * experience) && Math.random() < xp * experience - whole) {
                    whole++;
                }
                xp = whole;
            }
            Direction front = front();
            while (xp > 0) {
                int split = ExperienceOrb.getExperienceValue(xp);
                xp -= split;
                ExperienceOrb orb = new ExperienceOrb(level, worldPosition.getX() + 0.5 + front.getStepX() * 1.2, worldPosition.getY() + 0.4,
                        worldPosition.getZ() + 0.5 + front.getStepZ() * 1.2, split);
                orb.setDeltaMovement(sideways(front.getStepX(), 0.025f), 0, sideways(front.getStepZ(), 0.025f));
                level.addFreshEntity(orb);
            }
        }

        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            tag.putShort("CookTime", (short) cookTime);
            tag.putShort("SpeedyTime", (short) speedyTime);
            ContainerHelper.saveAllItems(tag, items);
        }

        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            cookTime = tag.getShort("CookTime");
            speedyTime = tag.getShort("SpeedyTime");
            items.clear();
            ContainerHelper.loadAllItems(tag, items);
        }

        /** Для проверки: сколько предметов ждёт плавки. */
        public int queued() {
            return items.stream().mapToInt(ItemStack::getCount).sum();
        }
    }

    /** TileArcaneFurnaceNozzle оригинала: тянет из труб эссенцию Ignis, каждая единица ускоряет печь на 600 тиков. */
    public static class Nozzle extends BlockEntity implements EssentiaTransport {
        private int drawDelay;

        public Nozzle(BlockPos pos, BlockState state) {
            super(ModBlockEntities.INFERNAL_FURNACE_NOZZLE.get(), pos, state);
        }

        /** Сторона, обращённая наружу (от ядра). */
        private Direction facing() {
            BlockPos core = origin(worldPosition, getBlockState()).offset(1, 1, 1);
            return Direction.fromDelta(worldPosition.getX() - core.getX(), worldPosition.getY() - core.getY(), worldPosition.getZ() - core.getZ());
        }

        @Nullable
        private Core furnace() {
            return level != null && level.getBlockEntity(origin(worldPosition, getBlockState()).offset(1, 1, 1)) instanceof Core core ? core : null;
        }

        void serverTick() {
            Core furnace = furnace();
            if (furnace != null && furnace.speedyTime < 60 && drawEssentia()) {
                furnace.speedyTime += 600;
            }
        }

        private boolean drawEssentia() {
            if (++drawDelay % 5 != 0) {
                return false;
            }
            Direction facing = facing();
            if (facing == null) {
                return false;
            }
            EssentiaTransport source = EssentiaTransport.neighbour(level, worldPosition, facing);
            if (source == null || !source.canOutputTo(facing.getOpposite())) {
                return false;
            }
            return source.getSuctionAmount(facing.getOpposite()) < getSuctionAmount(facing)
                    && source.takeEssentia(Aspect.FIRE, 1, facing.getOpposite()) == 1;
        }

        @Override
        public boolean isConnectable(Direction side) {
            return side == facing();
        }

        @Override
        public boolean canInputFrom(Direction side) {
            return side == facing();
        }

        @Override
        public boolean canOutputTo(Direction side) {
            return false;
        }

        @Override
        public void setSuction(@Nullable Aspect aspect, int amount) {
        }

        @Override
        public Aspect getSuctionType(@Nullable Direction side) {
            return Aspect.FIRE;
        }

        @Override
        public int getSuctionAmount(@Nullable Direction side) {
            Core furnace = furnace();
            return furnace != null && furnace.speedyTime < 40 ? 128 : 0;
        }

        @Override
        public int takeEssentia(Aspect aspect, int amount, Direction side) {
            return 0;
        }

        @Override
        public int addEssentia(Aspect aspect, int amount, Direction side) {
            return 0;
        }

        @Override
        public Aspect getEssentiaType(@Nullable Direction side) {
            return null;
        }

        @Override
        public int getEssentiaAmount(@Nullable Direction side) {
            return 0;
        }

        @Override
        public int getMinimumSuction() {
            return 0;
        }
    }
}
