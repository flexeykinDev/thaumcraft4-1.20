package thaumcraft.common.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.common.ForgeHooks;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.aspects.ItemAspectManager;
import thaumcraft.common.menu.AlchemicalFurnaceMenu;
import thaumcraft.common.registry.ModBlockEntities;
import thaumcraft.common.registry.ModItems;

import java.util.List;

/**
 * Алхимическая печь — перенос TileAlchemyFurnace оригинала. Сжигает предметы в эссенцию (до 50 единиц внутри)
 * и раз в две секунды отдаёт по единице в перегонные кубы, стоящие столбиком над ней (до пяти).
 */
public class AlchemicalFurnaceBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider {
    public static final int INPUT_SLOT = 0;
    public static final int FUEL_SLOT = 1;
    public static final int MAX_VIS = 50;
    private static final int MAX_ALEMBICS = 5;

    private final NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);
    private AspectList aspects = new AspectList();
    private int burnTime;
    private int burnTotal;
    private int cookTime;
    private int smeltTime = 100;
    /** Алюментум в топке удваивает скорость перекачки в кубы. */
    private boolean speedBoost;
    private int count;

    /** Числа для окна: огонь, полное время топлива, ход плавки, время плавки, заполненность. */
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> burnTime;
                case 1 -> burnTotal;
                case 2 -> cookTime;
                case 3 -> smeltTime;
                default -> aspects.visSize();
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return 5;
        }
    };

    public AlchemicalFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ALCHEMICAL_FURNACE.get(), pos, state);
    }

    public AspectList getAspects() {
        return aspects;
    }

    public boolean isBurning() {
        return burnTime > 0;
    }

    private static AspectList aspectsOf(ItemStack stack) {
        return ItemAspectManager.INSTANCE.getAspects(stack);
    }

    /** Предмет можно сжечь, если в нём есть аспекты и они поместятся в печь. */
    private boolean canSmelt() {
        ItemStack input = items.get(INPUT_SLOT);
        if (input.isEmpty()) {
            return false;
        }
        int size = aspectsOf(input).visSize();
        if (size == 0 || size > MAX_VIS - aspects.visSize()) {
            return false;
        }
        // каждые мехи рядом ускоряют плавку на 1/8
        int bellows = level == null ? 0 : thaumcraft.common.blocks.BellowsBlock.count(level, worldPosition, List.of(Direction.values()));
        smeltTime = Math.max(1, (int) (size * 10 * (1f - 0.125f * bellows)));
        return true;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AlchemicalFurnaceBlockEntity furnace) {
        boolean wasBurning = furnace.burnTime > 0;
        boolean changed = false;
        furnace.count++;
        if (furnace.burnTime > 0) {
            furnace.burnTime--;
        }

        if (furnace.count % (furnace.speedBoost ? 20 : 40) == 0 && !furnace.aspects.isEmpty()) {
            furnace.feedAlembics(level);
            changed = true;
        }

        if (furnace.burnTime == 0 && furnace.canSmelt()) {
            ItemStack fuel = furnace.items.get(FUEL_SLOT);
            furnace.burnTotal = furnace.burnTime = ForgeHooks.getBurnTime(fuel, null);
            if (furnace.burnTime > 0) {
                changed = true;
                furnace.speedBoost = fuel.is(ModItems.ALUMENTUM.get());
                ItemStack remainder = fuel.getCraftingRemainingItem();
                fuel.shrink(1);
                if (fuel.isEmpty()) {
                    furnace.items.set(FUEL_SLOT, remainder);
                }
            }
        }

        if (furnace.isBurning() && furnace.canSmelt()) {
            if (++furnace.cookTime >= furnace.smeltTime) {
                furnace.cookTime = 0;
                furnace.aspects.add(aspectsOf(furnace.items.get(INPUT_SLOT)));
                furnace.items.get(INPUT_SLOT).shrink(1);
                changed = true;
            }
        } else {
            furnace.cookTime = 0;
        }

        if (wasBurning != furnace.burnTime > 0) {
            changed = true;
            level.setBlock(pos, state.setValue(BlockStateProperties.LIT, furnace.burnTime > 0), 3);
        }
        if (changed) {
            furnace.setChanged();
        }
    }

    /** Сначала доливаются уже начатые кубы, затем один пустой куб получает случайный из остальных аспектов. */
    private void feedAlembics(Level level) {
        AspectList fed = new AspectList();
        for (int up = 1; up <= MAX_ALEMBICS; up++) {
            if (!(level.getBlockEntity(worldPosition.above(up)) instanceof EssentiaBlockEntity alembic)
                    || alembic.getCapacity() != EssentiaBlockEntity.ALEMBIC_CAPACITY) {
                break;
            }
            Aspect aspect = alembic.getAspect();
            if (aspect != null && alembic.getAmount() < alembic.getCapacity() && aspects.reduce(aspect, 1)) {
                alembic.add(aspect, 1);
                fed.merge(aspect, 1);
            }
        }
        for (int up = 1; up <= MAX_ALEMBICS; up++) {
            if (!(level.getBlockEntity(worldPosition.above(up)) instanceof EssentiaBlockEntity alembic)
                    || alembic.getCapacity() != EssentiaBlockEntity.ALEMBIC_CAPACITY) {
                break;
            }
            if (alembic.getAmount() == 0) {
                Aspect aspect = null;
                if (alembic.getFilter() != null) {
                    // куб с ярлыком берёт только свой аспект
                    if (aspects.getAmount(alembic.getFilter()) > 0) {
                        aspect = alembic.getFilter();
                    }
                } else {
                    List<Aspect> candidates = aspects.getAspects().stream().filter(a -> !fed.contains(a)).toList();
                    if (!candidates.isEmpty()) {
                        aspect = candidates.get(level.random.nextInt(candidates.size()));
                    }
                }
                if (aspect != null) {
                    aspects.reduce(aspect, 1);
                    alembic.add(aspect, 1);
                    break;
                }
            }
        }
    }

    // ---------- окно и ячейки ----------

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.alchemyfurnace");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new AlchemicalFurnaceMenu(id, inventory, this, data);
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        setChanged();
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == INPUT_SLOT ? !aspectsOf(stack).isEmpty() : ForgeHooks.getBurnTime(stack, null) > 0;
    }

    /** Как в оригинале: снизу топливо, с боков сырьё, сверху ничего (там стоят кубы). */
    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? new int[]{FUEL_SLOT} : side == Direction.UP ? new int[0] : new int[]{INPUT_SLOT};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return side != Direction.UP && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return side != Direction.DOWN || slot != FUEL_SLOT || stack.is(Items.BUCKET);
    }

    // ---------- сохранение ----------

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, items);
        tag.put("Aspects", aspects.save(new CompoundTag()));
        tag.putShort("BurnTime", (short) burnTime);
        tag.putShort("BurnTotal", (short) burnTotal);
        tag.putShort("CookTime", (short) cookTime);
        tag.putBoolean("SpeedBoost", speedBoost);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        items.clear();
        ContainerHelper.loadAllItems(tag, items);
        aspects = AspectList.load(tag.getCompound("Aspects"));
        burnTime = tag.getShort("BurnTime");
        burnTotal = tag.getShort("BurnTotal");
        cookTime = tag.getShort("CookTime");
        speedBoost = tag.getBoolean("SpeedBoost");
    }
}
