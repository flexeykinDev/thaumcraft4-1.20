package thaumcraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectContainer;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.EssentiaTransport;
import thaumcraft.common.blockentity.CrucibleBlockEntity;
import thaumcraft.common.crafting.CrucibleRecipe;
import thaumcraft.common.menu.ThaumatoriumMenu;
import thaumcraft.common.registry.ModBlockEntities;
import thaumcraft.common.registry.ModBlocks;
import thaumcraft.common.registry.ModItems;
import thaumcraft.common.registry.ModRecipes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Тауматорий (BlockMetalDevice оригинала, мета 10 и 11) — автоматический тигель: две алхимические конструкции над
 * тиглем, которых коснулись жезлом. Ему задают рецепты тигля, кладут катализатор и подводят эссенцию по трубам;
 * готовое он выдаёт спереди — в сундук или на землю. Под тиглем должен гореть огонь; сигнал красного камня его останавливает.
 */
public class ThaumatoriumBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    /** Верхняя половина: сама ничего не делает и лишь передаёт всё нижней. */
    public static final BooleanProperty TOP = BooleanProperty.create("top");

    public ThaumatoriumBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(TOP, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, TOP);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(TOP) ? new Top(pos, state) : new Core(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return state.getValue(TOP) ? null : createTickerHelper(type, ModBlockEntities.THAUMATORIUM.get(), Core::tick);
    }

    @Nullable
    private static Core core(Level level, BlockPos pos, BlockState state) {
        return level.getBlockEntity(state.getValue(TOP) ? pos.below() : pos) instanceof Core core ? core : null;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        Core core = core(level, pos, state);
        if (core == null || player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, core, core.getBlockPos());
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Без второй половины или без тигля под собой тауматорий снова распадается на алхимические конструкции. */
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moved) {
        if (level.isClientSide) {
            return;
        }
        if (state.getValue(TOP)) {
            BlockState below = level.getBlockState(pos.below());
            if (!below.is(this) || below.getValue(TOP)) {
                level.setBlock(pos, ModBlocks.ALCHEMICAL_CONSTRUCT.get().defaultBlockState(), 3);
            }
            return;
        }
        BlockState above = level.getBlockState(pos.above());
        if (!above.is(this) || !above.getValue(TOP) || !level.getBlockState(pos.below()).is(ModBlocks.CRUCIBLE.get())) {
            if (level.getBlockEntity(pos) instanceof Core core) {
                Containers.dropContents(level, pos, core);
            }
            level.setBlock(pos, ModBlocks.ALCHEMICAL_CONSTRUCT.get().defaultBlockState(), 3);
        } else if (level.getBlockEntity(pos) instanceof Core core) {
            core.getUpgrades();
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof Core core) {
            Containers.dropContents(level, pos, core);
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, net.minecraft.world.level.BlockGetter level, BlockPos pos, Player player) {
        return new ItemStack(ModItems.ALCHEMICAL_CONSTRUCT.get());
    }

    /** Жезл на средней или верхней из двух конструкций над тиглем собирает тауматорий (createThaumatorium оригинала). */
    @Nullable
    public static BlockPos findStructure(Level level, BlockPos clicked) {
        Block construct = ModBlocks.ALCHEMICAL_CONSTRUCT.get();
        if (level.getBlockState(clicked.above()).is(construct) && level.getBlockState(clicked.below()).is(ModBlocks.CRUCIBLE.get())) {
            return clicked;
        }
        if (level.getBlockState(clicked.below()).is(construct) && level.getBlockState(clicked.below(2)).is(ModBlocks.CRUCIBLE.get())) {
            return clicked.below();
        }
        return null;
    }

    public static void assemble(ServerLevel level, BlockPos pos, Direction facing) {
        BlockState state = ModBlocks.THAUMATORIUM.get().defaultBlockState().setValue(FACING, facing);
        level.setBlock(pos, state, 3);
        level.setBlock(pos.above(), state.setValue(TOP, true), 3);
        thaumcraft.common.network.FxPacket.blockSparkle(level, pos, -9999, 2);
        thaumcraft.common.network.FxPacket.blockSparkle(level, pos.above(), -9999, 2);
        level.playSound(null, pos, thaumcraft.common.registry.ModSounds.WAND.get(), SoundSource.BLOCKS, 1f, 1f);
    }

    /** Все рецепты тигля в одном и том же порядке на сервере и клиенте: номер рецепта служит номером кнопки в окне. */
    public static List<CrucibleRecipe> allRecipes(Level level) {
        List<CrucibleRecipe> all = new ArrayList<>(level.getRecipeManager().getAllRecipesFor(ModRecipes.CRUCIBLE.get()));
        all.sort(Comparator.comparing(recipe -> recipe.getId().toString()));
        return all;
    }

    /** TileThaumatorium оригинала. */
    public static class Core extends BlockEntity implements WorldlyContainer, MenuProvider, EssentiaTransport, AspectContainer {
        private static final int[] SLOTS = {0};

        private ItemStack input = ItemStack.EMPTY;
        public AspectList essentia = new AspectList();
        /** Заданные рецепты и те, кто их задал (им засчитывается создание предмета). */
        public final List<ResourceLocation> recipes = new ArrayList<>();
        private final List<String> recipePlayers = new ArrayList<>();
        public int maxRecipes = 1;
        private int currentCraft = -1;
        @Nullable
        private CrucibleRecipe currentRecipe;
        @Nullable
        private Aspect currentSuction;
        private int counter;
        private boolean heated;
        private int venting;
        /** Окно, которое нужно известить о смене катализатора. */
        @Nullable
        public Runnable listener;

        public Core(BlockPos pos, BlockState state) {
            super(ModBlockEntities.THAUMATORIUM.get(), pos, state);
        }

        public Direction getFacing() {
            return getBlockState().getValue(FACING);
        }

        private void sync() {
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }

        @Nullable
        public CrucibleRecipe recipe(ResourceLocation id) {
            return level != null && level.getRecipeManager().byKey(id).orElse(null) instanceof CrucibleRecipe recipe ? recipe : null;
        }

        /** Добавляет рецепт или убирает уже заданный (кнопка в окне). */
        public void toggleRecipe(CrucibleRecipe recipe, Player player) {
            int index = recipes.indexOf(recipe.getId());
            if (index >= 0) {
                recipes.remove(index);
                recipePlayers.remove(index);
                currentCraft = -1;
            } else if (recipes.size() < maxRecipes) {
                recipes.add(recipe.getId());
                recipePlayers.add(player.getUUID().toString());
            }
            sync();
        }

        // ---------- работа ----------

        public static void tick(Level level, BlockPos pos, BlockState state, Core core) {
            if (level.isClientSide) {
                if (core.venting > 0) {
                    core.venting--;
                    Direction facing = core.getFacing();
                    var random = level.random;
                    level.addParticle(net.minecraft.core.particles.ParticleTypes.CLOUD,
                            pos.getX() + 0.5 + (0.1 - random.nextFloat() * 0.2) + facing.getStepX() / 2.0,
                            pos.getY() + 0.5 + (0.1 - random.nextFloat() * 0.2),
                            pos.getZ() + 0.5 + (0.1 - random.nextFloat() * 0.2) + facing.getStepZ() / 2.0,
                            facing.getStepX() / 4.0 + (0.1 - random.nextFloat() * 0.2), 0.1 - random.nextFloat() * 0.2,
                            facing.getStepZ() / 4.0 + (0.1 - random.nextFloat() * 0.2));
                }
                return;
            }
            core.serverTick();
        }

        private boolean gettingPower() {
            return level.hasNeighborSignal(worldPosition) || level.hasNeighborSignal(worldPosition.below())
                    || level.hasNeighborSignal(worldPosition.above());
        }

        private void serverTick() {
            if (counter % 40 == 0) {
                heated = CrucibleBlockEntity.isHeatSource(level.getBlockState(worldPosition.below(2)));
                getUpgrades();
            }
            counter++;
            if (!heated || gettingPower() || counter % 5 != 0 || recipes.isEmpty()) {
                return;
            }
            if (input.isEmpty()) {
                currentSuction = null;
                return;
            }
            if (currentCraft < 0 || currentCraft >= recipes.size() || currentRecipe == null || !currentRecipe.getCatalyst().test(input)) {
                currentCraft = -1;
                for (int i = 0; i < recipes.size(); i++) {
                    CrucibleRecipe recipe = recipe(recipes.get(i));
                    if (recipe != null && recipe.getCatalyst().test(input)) {
                        currentCraft = i;
                        currentRecipe = recipe;
                        break;
                    }
                }
            }
            if (currentCraft < 0 || currentRecipe == null) {
                return;
            }
            // если впереди хранилище и готовое в него не влезет — ждём
            IItemHandler target = outputHandler();
            if (target != null && !ItemHandlerHelper.insertItemStacked(target, currentRecipe.getResult().copy(), true).isEmpty()) {
                return;
            }
            boolean done = true;
            currentSuction = null;
            AspectList needed = currentRecipe.getAspects();
            for (Aspect aspect : needed.getAspectsSortedByName()) {
                if (essentia.getAmount(aspect) < needed.getAmount(aspect)) {
                    currentSuction = aspect;
                    done = false;
                    break;
                }
            }
            if (done) {
                completeRecipe();
            } else {
                fill();
            }
        }

        @Nullable
        private IItemHandler outputHandler() {
            Direction facing = getFacing();
            BlockEntity neighbour = level.getBlockEntity(worldPosition.relative(facing));
            return neighbour == null ? null : neighbour.getCapability(ForgeCapabilities.ITEM_HANDLER, facing.getOpposite()).orElse(null);
        }

        private void completeRecipe() {
            if (currentRecipe == null || !currentRecipe.matches(essentia, input)) {
                return;
            }
            ItemStack catalyst = input.copyWithCount(1);
            input.shrink(1);
            essentia = new AspectList();
            ItemStack dropped = currentRecipe.getResult().copy();
            Player maker = null;
            try {
                maker = level.getPlayerByUUID(UUID.fromString(recipePlayers.get(currentCraft)));
            } catch (IllegalArgumentException | IndexOutOfBoundsException ignored) {
                // рецепт задан до того, как стол запоминал игроков
            }
            if (maker != null) {
                net.minecraftforge.event.ForgeEventFactory.firePlayerCraftingEvent(maker, dropped, new SimpleContainer(catalyst));
            }
            Direction facing = getFacing();
            IItemHandler target = outputHandler();
            if (target != null) {
                dropped = ItemHandlerHelper.insertItemStacked(target, dropped, false);
            }
            if (!dropped.isEmpty()) {
                ItemEntity item = new ItemEntity(level, worldPosition.getX() + 0.5 + facing.getStepX() * 0.66, worldPosition.getY() + 0.33,
                        worldPosition.getZ() + 0.5 + facing.getStepZ() * 0.66, dropped);
                item.setDeltaMovement(0.075 * facing.getStepX(), 0.025, 0.075 * facing.getStepZ());
                level.addFreshEntity(item);
                level.blockEvent(worldPosition, getBlockState().getBlock(), 0, 0);
            }
            level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.25f,
                    2.6f + (level.random.nextFloat() - level.random.nextFloat()) * 0.8f);
            currentCraft = -1;
            if (listener != null) {
                listener.run();
            }
            sync();
        }

        /** Выдав предмет на землю, тауматорий пыхает паром. */
        @Override
        public boolean triggerEvent(int id, int type) {
            if (level != null && level.isClientSide) {
                venting = 7;
            }
            return true;
        }

        /** Тянет нужный аспект из труб вокруг обеих половин — отовсюду, кроме лицевой стороны и низа. */
        private void fill() {
            Direction facing = getFacing();
            for (int y = 0; y <= 1; y++) {
                for (Direction dir : Direction.values()) {
                    if (dir == facing || dir == Direction.DOWN || y == 0 && dir == Direction.UP || y == 1 && dir == Direction.DOWN) {
                        continue;
                    }
                    EssentiaTransport source = EssentiaTransport.neighbour(level, worldPosition.above(y), dir);
                    if (source == null || source instanceof Top || source == this) {
                        continue;
                    }
                    Direction back = dir.getOpposite();
                    if (source.getEssentiaAmount(back) > 0 && source.getSuctionAmount(back) < getSuctionAmount(null)
                            && getSuctionAmount(null) >= source.getMinimumSuction()) {
                        int taken = source.takeEssentia(currentSuction, 1, back);
                        if (taken > 0) {
                            addToContainer(currentSuction, taken);
                            return;
                        }
                    }
                }
            }
        }

        /** @return сколько не поместилось */
        public int addToContainer(Aspect aspect, int amount) {
            if (currentRecipe == null) {
                return amount;
            }
            int room = currentRecipe.getAspects().getAmount(aspect) - essentia.getAmount(aspect);
            if (room <= 0) {
                return amount;
            }
            int added = Math.min(room, amount);
            essentia.add(aspect, added);
            sync();
            return amount - added;
        }

        /** Каждая мнемоническая матрица, приставленная к тауматорию, даёт ему ещё два рецепта. */
        public void getUpgrades() {
            int max = 1;
            Direction facing = getFacing();
            for (int y = 0; y <= 1; y++) {
                for (Direction dir : Direction.values()) {
                    if (dir == Direction.DOWN || dir == facing || y == 0 && dir == Direction.UP) {
                        continue;
                    }
                    BlockState state = level.getBlockState(worldPosition.above(y).relative(dir));
                    if (state.is(ModBlocks.MNEMONIC_MATRIX.get()) && state.getValue(MnemonicMatrixBlock.FACING) == dir.getOpposite()) {
                        max += 2;
                    }
                }
            }
            if (max != maxRecipes) {
                maxRecipes = max;
                while (recipes.size() > maxRecipes) {
                    recipes.remove(recipes.size() - 1);
                    recipePlayers.remove(recipePlayers.size() - 1);
                }
                sync();
            }
        }

        // ---------- трубы ----------

        @Override
        public AspectList getContainedAspects() {
            return essentia;
        }

        @Override
        public boolean isConnectable(Direction side) {
            return side != getFacing();
        }

        @Override
        public boolean canInputFrom(Direction side) {
            return side != getFacing();
        }

        @Override
        public boolean canOutputTo(Direction side) {
            return false;
        }

        @Override
        public void setSuction(@Nullable Aspect aspect, int amount) {
            currentSuction = aspect;
        }

        @Nullable
        @Override
        public Aspect getSuctionType(@Nullable Direction side) {
            return currentSuction;
        }

        @Override
        public int getSuctionAmount(@Nullable Direction side) {
            return currentSuction != null ? 128 : 0;
        }

        @Override
        public int takeEssentia(Aspect aspect, int amount, Direction side) {
            return 0;
        }

        @Override
        public int addEssentia(Aspect aspect, int amount, Direction side) {
            return canInputFrom(side) ? amount - addToContainer(aspect, amount) : 0;
        }

        @Nullable
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

        // ---------- окно и ячейка ----------

        @Override
        public Component getDisplayName() {
            return Component.translatable("block.thaumcraft.thaumatorium");
        }

        @Nullable
        @Override
        public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
            return new ThaumatoriumMenu(id, inventory, this);
        }

        @Override
        public int getContainerSize() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return input.isEmpty();
        }

        @Override
        public ItemStack getItem(int slot) {
            return input;
        }

        private void inputChanged() {
            setChanged();
            if (listener != null) {
                listener.run();
            }
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            ItemStack taken = input.split(amount);
            inputChanged();
            return taken;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            ItemStack taken = input;
            input = ItemStack.EMPTY;
            return taken;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            input = stack;
            inputChanged();
        }

        @Override
        public boolean stillValid(Player player) {
            return level != null && level.getBlockEntity(worldPosition) == this
                    && player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64.0;
        }

        @Override
        public void clearContent() {
            input = ItemStack.EMPTY;
        }

        @Override
        public int[] getSlotsForFace(Direction side) {
            return SLOTS;
        }

        @Override
        public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
            return true;
        }

        @Override
        public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
            return true;
        }

        // ---------- сохранение ----------

        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            tag.putByte("MaxRecipes", (byte) maxRecipes);
            tag.put("Essentia", essentia.save(new CompoundTag()));
            ListTag ids = new ListTag();
            ListTag players = new ListTag();
            for (int i = 0; i < recipes.size(); i++) {
                ids.add(StringTag.valueOf(recipes.get(i).toString()));
                players.add(StringTag.valueOf(recipePlayers.get(i)));
            }
            tag.put("Recipes", ids);
            tag.put("RecipePlayers", players);
            if (!input.isEmpty()) {
                tag.put("Input", input.save(new CompoundTag()));
            }
        }

        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            maxRecipes = Math.max(1, tag.getByte("MaxRecipes"));
            essentia = AspectList.load(tag.getCompound("Essentia"));
            recipes.clear();
            recipePlayers.clear();
            ListTag ids = tag.getList("Recipes", Tag.TAG_STRING);
            ListTag players = tag.getList("RecipePlayers", Tag.TAG_STRING);
            for (int i = 0; i < ids.size(); i++) {
                ResourceLocation id = ResourceLocation.tryParse(ids.getString(i));
                if (id != null) {
                    recipes.add(id);
                    recipePlayers.add(i < players.size() ? players.getString(i) : "");
                }
            }
            input = tag.contains("Input") ? ItemStack.of(tag.getCompound("Input")) : ItemStack.EMPTY;
        }

        @Override
        public CompoundTag getUpdateTag() {
            return saveWithoutMetadata();
        }

        @Override
        public Packet<ClientGamePacketListener> getUpdatePacket() {
            return ClientboundBlockEntityDataPacket.create(this);
        }

        @Override
        public AABB getRenderBoundingBox() {
            return new AABB(worldPosition.offset(-1, 0, -1), worldPosition.offset(2, 2, 2));
        }
    }

    /** TileThaumatoriumTop оригинала: верхняя половина, к которой тоже можно подвести трубы и воронки. */
    public static class Top extends BlockEntity implements EssentiaTransport, AspectContainer, WorldlyContainer {
        public Top(BlockPos pos, BlockState state) {
            super(ModBlockEntities.THAUMATORIUM_TOP.get(), pos, state);
        }

        @Nullable
        private Core core() {
            return level != null && level.getBlockEntity(worldPosition.below()) instanceof Core core ? core : null;
        }

        @Override
        public AspectList getContainedAspects() {
            Core core = core();
            return core == null ? new AspectList() : core.essentia;
        }

        @Override
        public boolean isConnectable(Direction side) {
            Core core = core();
            return core != null && side != Direction.DOWN && core.isConnectable(side);
        }

        @Override
        public boolean canInputFrom(Direction side) {
            return isConnectable(side);
        }

        @Override
        public boolean canOutputTo(Direction side) {
            return false;
        }

        @Override
        public void setSuction(@Nullable Aspect aspect, int amount) {
            Core core = core();
            if (core != null) {
                core.setSuction(aspect, amount);
            }
        }

        @Nullable
        @Override
        public Aspect getSuctionType(@Nullable Direction side) {
            Core core = core();
            return core == null ? null : core.getSuctionType(side);
        }

        @Override
        public int getSuctionAmount(@Nullable Direction side) {
            Core core = core();
            return core == null ? 0 : core.getSuctionAmount(side);
        }

        @Override
        public int takeEssentia(Aspect aspect, int amount, Direction side) {
            return 0;
        }

        @Override
        public int addEssentia(Aspect aspect, int amount, Direction side) {
            Core core = core();
            return core == null || !canInputFrom(side) ? 0 : amount - core.addToContainer(aspect, amount);
        }

        @Nullable
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

        // катализатор можно подавать воронкой и в верхнюю половину

        @Override
        public int getContainerSize() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            Core core = core();
            return core == null || core.isEmpty();
        }

        @Override
        public ItemStack getItem(int slot) {
            Core core = core();
            return core == null ? ItemStack.EMPTY : core.getItem(slot);
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            Core core = core();
            return core == null ? ItemStack.EMPTY : core.removeItem(slot, amount);
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            Core core = core();
            return core == null ? ItemStack.EMPTY : core.removeItemNoUpdate(slot);
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            Core core = core();
            if (core != null) {
                core.setItem(slot, stack);
            }
        }

        @Override
        public boolean stillValid(Player player) {
            Core core = core();
            return core != null && core.stillValid(player);
        }

        @Override
        public void clearContent() {
        }

        @Override
        public int[] getSlotsForFace(Direction side) {
            return core() == null ? new int[0] : Core.SLOTS;
        }

        @Override
        public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
            return core() != null;
        }

        @Override
        public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
            return core() != null;
        }
    }
}
