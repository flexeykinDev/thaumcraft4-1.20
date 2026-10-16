package thaumcraft.common.entity.golem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;
import thaumcraft.common.items.golem.GolemCoreItem;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Общие приёмы големов-носильщиков — перенос GolemHelper и InventoryUtils оригинала. Вместо старых IInventory здесь
 * хранилища Forge: у двойного сундука оно одно на обе половины, а хранилища других модов работают без особых правил.
 */
public final class GolemHelper {
    /** golemDelay оригинала: раз во сколько тиков голем осматривается в поисках дела. */
    public static final int DELAY = 5;
    /** golemIgnoreDelay оригинала (10 секунд): на столько голем забывает о предмете, который некуда деть. */
    private static final int IGNORE_TICKS = 200;

    private record Timeout(UUID golem, ItemStack stack, long until) {
    }

    private static final List<Timeout> TIMEOUTS = new ArrayList<>();

    private GolemHelper() {
    }

    // ---------- ходьба ----------

    /**
     * Идти вплотную к цели. Обычное moveTo считает путь законченным уже в блоке от цели, и голем, которому для дела
     * нужно стоять ближе двух блоков, мог остановиться на шаг дальше и топтаться там вечно.
     */
    public static boolean walkTo(Golem golem, double x, double y, double z) {
        var path = golem.getNavigation().createPath(BlockPos.containing(x, y, z), 0);
        return path != null && golem.getNavigation().moveTo(path, 1.0);
    }

    public static boolean walkTo(Golem golem, net.minecraft.world.entity.Entity target) {
        var path = golem.getNavigation().createPath(target, 0);
        return path != null && golem.getNavigation().moveTo(path, 1.0);
    }

    // ---------- сравнение предметов ----------

    /** Одинаковы ли предметы с точки зрения голема (его переключатели решают, смотреть ли на теги, износ и данные). */
    public static boolean matches(Golem golem, ItemStack a, ItemStack b) {
        return matches(a, b, golem.checkOreDict(), golem.ignoreDamage(), golem.ignoreNBT());
    }

    public static boolean matches(ItemStack a, ItemStack b, boolean useTags, boolean ignoreDamage, boolean ignoreNbt) {
        if (a.isEmpty() || b.isEmpty()) {
            return false;
        }
        if (useTags && shareTag(a, b)) {
            return true;
        }
        if (a.getItem() != b.getItem()) {
            return false;
        }
        if (ignoreNbt) {
            return true;
        }
        if (ignoreDamage && a.isDamageableItem()) {
            ItemStack x = a.copy();
            ItemStack y = b.copy();
            x.setDamageValue(0);
            y.setDamageValue(0);
            return ItemStack.isSameItemSameTags(x, y);
        }
        return ItemStack.isSameItemSameTags(a, b);
    }

    /** Замена словаря руд: предметы взаимозаменяемы, если у них есть общий тег Forge (слитки меди, доски и так далее). */
    public static boolean shareTag(ItemStack a, ItemStack b) {
        return a.getTags().anyMatch(tag -> tag.location().getNamespace().equals("forge") && b.is(tag));
    }

    // ---------- образцы в «памяти» голема ----------

    public static boolean hasSomething(SimpleContainer samples) {
        return !samples.isEmpty();
    }

    /** Сколько таких предметов голему велено держать: сумма по всем подходящим ячейкам-образцам. */
    public static int amountNeeded(Golem golem, ItemStack stack) {
        boolean fuzzy = golem.getUpgradeAmount(Golem.UPGRADE_ENTROPY) > 0;
        int amount = 0;
        for (int slot = 0; slot < golem.inventory.getContainerSize(); slot++) {
            ItemStack sample = golem.inventory.getItem(slot);
            if (!sample.isEmpty() && (ItemStack.isSameItemSameTags(sample, stack) || fuzzy && (sample.is(stack.getItem()) || shareTag(sample, stack)))) {
                amount += sample.getCount();
            }
        }
        return amount;
    }

    public static List<ItemStack> samples(Golem golem) {
        List<ItemStack> list = new ArrayList<>();
        for (int slot = 0; slot < golem.inventory.getContainerSize(); slot++) {
            if (!golem.inventory.getItem(slot).isEmpty()) {
                list.add(golem.inventory.getItem(slot).copy());
            }
        }
        return list;
    }

    /** Цвета ячеек, образцы в которых подходят к предмету; у голема без образцов — цвета всех ячеек. */
    public static List<Byte> colorsMatching(Golem golem, ItemStack match) {
        List<Byte> colors = new ArrayList<>();
        boolean allEmpty = true;
        int size = golem.inventory.getContainerSize();
        for (int slot = 0; slot < size; slot++) {
            ItemStack sample = golem.inventory.getItem(slot);
            if (!sample.isEmpty()) {
                allEmpty = false;
            }
            if (matches(golem, sample, match)) {
                colors.add((byte) golem.getColors(slot));
            }
        }
        if (allEmpty) {
            for (int slot = 0; slot < size; slot++) {
                colors.add((byte) golem.getColors(slot));
            }
        }
        return colors;
    }

    // ---------- хранилища ----------

    /** Блок, у которого голема поставили: его «домашний» сундук, если это хранилище. */
    public static BlockPos homeBlock(Golem golem) {
        return golem.getHome().relative(homeSide(golem).getOpposite());
    }

    /** Сторона домашнего блока, по которой щёлкнули, ставя голема. */
    public static Direction homeSide(Golem golem) {
        return Direction.from3DDataValue(golem.homeFacing);
    }

    @Nullable
    public static IItemHandler handler(Level level, BlockPos pos, @Nullable Direction side) {
        BlockEntity entity = level.getBlockEntity(pos);
        return entity == null ? null : entity.getCapability(ForgeCapabilities.ITEM_HANDLER, side).orElse(null);
    }

    public static boolean isContainer(Level level, BlockPos pos) {
        return handler(level, pos, null) != null;
    }

    @Nullable
    public static IItemHandler homeHandler(Golem golem) {
        return handler(golem.level(), homeBlock(golem), homeSide(golem));
    }

    /** @return то, что не поместилось */
    public static ItemStack insert(IItemHandler handler, ItemStack stack, boolean doit) {
        return ItemHandlerHelper.insertItemStacked(handler, stack.copy(), !doit);
    }

    public static boolean hasRoom(IItemHandler handler, ItemStack stack) {
        return !stack.isEmpty() && insert(handler, stack, false).getCount() < stack.getCount();
    }

    /** Достаёт до amount предметов, похожих на образец; пустой стек, если таких нет. */
    public static ItemStack extract(Golem golem, IItemHandler handler, ItemStack pattern, int amount, boolean doit) {
        ItemStack result = ItemStack.EMPTY;
        for (int slot = 0; slot < handler.getSlots() && amount > 0; slot++) {
            ItemStack there = handler.getStackInSlot(slot);
            if (there.isEmpty() || !matches(golem, there, pattern) || !result.isEmpty() && !ItemStack.isSameItemSameTags(result, there)) {
                continue;
            }
            ItemStack taken = handler.extractItem(slot, Math.min(amount, there.getMaxStackSize() - result.getCount()), !doit);
            if (taken.isEmpty()) {
                continue;
            }
            if (result.isEmpty()) {
                result = taken.copy();
            } else {
                result.grow(taken.getCount());
            }
            amount -= taken.getCount();
            if (result.getCount() >= result.getMaxStackSize()) {
                break;
            }
        }
        return result;
    }

    /** Сколько предметов, похожих на образец, лежит в хранилище. */
    public static int count(Golem golem, IItemHandler handler, ItemStack pattern) {
        int found = 0;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack there = handler.getStackInSlot(slot);
            if (matches(golem, there, pattern)) {
                found += there.getCount();
            }
        }
        return found;
    }

    /** Сундук приоткрывает крышку, когда голем в нём роется (golemChestInteract оригинала). */
    public static void openChest(Level level, BlockPos pos, boolean open) {
        if (level.getBlockEntity(pos) instanceof ChestBlockEntity) {
            level.blockEvent(pos, level.getBlockState(pos).getBlock(), 1, open ? 1 : 0);
        }
    }

    // ---------- отметки ----------

    /** Отмеченные хранилища, каждое по разу. */
    public static Set<BlockPos> markedContainers(Golem golem) {
        Set<BlockPos> result = new LinkedHashSet<>();
        for (GolemMarker marker : golem.getMarkers()) {
            if (isContainer(golem.level(), marker.pos())) {
                result.add(marker.pos());
            }
        }
        return result;
    }

    public static List<BlockPos> markedContainersAdjacent(Golem golem) {
        List<BlockPos> result = new ArrayList<>();
        for (BlockPos pos : markedContainers(golem)) {
            if (golem.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < 4.0) {
                result.add(pos);
            }
        }
        return result;
    }

    /** Отмеченные места рядом с големом, которые хранилищами не являются: туда предметы просто бросают. */
    public static List<BlockPos> markedBlocksAdjacent(Golem golem, byte color) {
        List<BlockPos> result = new ArrayList<>();
        for (GolemMarker marker : golem.getMarkers()) {
            BlockPos pos = marker.pos();
            if ((marker.color() == color || color == -1) && !isContainer(golem.level(), pos)
                    && golem.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < 4.0) {
                result.add(pos);
            }
        }
        return result;
    }

    /** Стороны блока, отмеченные нужным цветом (отметка без цвета годится для любого). */
    public static List<Direction> markedSides(Golem golem, BlockPos pos, byte color) {
        List<Direction> sides = new ArrayList<>();
        String dimension = golem.level().dimension().location().toString();
        for (Direction side : Direction.values()) {
            GolemMarker wanted = new GolemMarker(pos, dimension, (byte) side.get3DDataValue(), color);
            for (GolemMarker marker : golem.getMarkers()) {
                if (wanted.equalsFuzzy(marker)) {
                    sides.add(side);
                    break;
                }
            }
        }
        return sides;
    }

    public static List<BlockPos> containersWithRoom(Golem golem, byte color, ItemStack stack) {
        List<BlockPos> result = new ArrayList<>();
        for (BlockPos pos : markedContainers(golem)) {
            for (Direction side : markedSides(golem, pos, color)) {
                IItemHandler handler = handler(golem.level(), pos, side);
                if (handler != null && hasRoom(handler, stack)) {
                    result.add(pos);
                    break;
                }
            }
        }
        return result;
    }

    public static List<BlockPos> containersWithGoods(Golem golem, ItemStack goods, byte color) {
        List<BlockPos> result = new ArrayList<>();
        for (BlockPos pos : markedContainers(golem)) {
            for (Direction side : markedSides(golem, pos, color)) {
                IItemHandler handler = handler(golem.level(), pos, side);
                if (handler != null && !extract(golem, handler, goods, goods.getCount(), false).isEmpty()) {
                    result.add(pos);
                    break;
                }
            }
        }
        return result;
    }

    /** Ближайшее из мест в пределах дальности голема, не считая домашнего блока. */
    @Nullable
    public static BlockPos nearest(Golem golem, List<BlockPos> candidates) {
        BlockPos home = homeBlock(golem);
        float reach = golem.getRange();
        double best = Double.MAX_VALUE;
        BlockPos result = null;
        for (BlockPos pos : candidates) {
            double distance = golem.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            if (distance < best && distance <= reach * reach && !pos.equals(home)) {
                best = distance;
                result = pos;
            }
        }
        return result;
    }

    // ---------- ядро наполнения ----------

    /**
     * Чего не хватает в домашнем хранилище по сравнению с образцами (getMissingItems оригинала).
     * С включённым первым переключателем голем носит образцы без счёта. null — дома нет хранилища.
     */
    @Nullable
    public static List<ItemStack> missingItems(Golem golem) {
        if (golem.getToggles()[0]) {
            return samples(golem);
        }
        IItemHandler home = homeHandler(golem);
        if (home == null) {
            return null;
        }
        List<ItemStack> missing = new ArrayList<>();
        for (ItemStack sample : samples(golem)) {
            int found = count(golem, home, sample);
            if (found < amountNeeded(golem, sample)) {
                ItemStack lacking = sample.copy();
                lacking.setCount(Math.max(1, sample.getCount() - found));
                missing.add(lacking);
            }
        }
        return missing;
    }

    // ---------- ядро опустошения и общий выбор цели ----------

    public static boolean isOnTimeOut(Golem golem, ItemStack stack) {
        long now = golem.level().getGameTime();
        for (Iterator<Timeout> it = TIMEOUTS.iterator(); it.hasNext(); ) {
            Timeout timeout = it.next();
            if (now >= timeout.until) {
                it.remove();
            } else if (timeout.golem.equals(golem.getUUID()) && ItemStack.isSameItemSameTags(timeout.stack, stack)) {
                return true;
            }
        }
        return false;
    }

    private static void timeOut(Golem golem, ItemStack stack) {
        TIMEOUTS.add(new Timeout(golem.getUUID(), stack.copy(), golem.level().getGameTime() + IGNORE_TICKS));
    }

    /** Есть ли куда отнести предмет голему с ядром опустошения; если некуда, предмет на время забывается. */
    public static boolean findSomethingEmptyCore(Golem golem, ItemStack stack) {
        List<Byte> colors = colorsMatching(golem, stack);
        for (byte color : colors) {
            if (nearest(golem, containersWithRoom(golem, color, stack)) != null) {
                return true;
            }
        }
        for (byte color : colors) {
            for (GolemMarker marker : golem.getMarkers()) {
                if ((marker.color() == color || color == -1) && !isContainer(golem.level(), marker.pos())) {
                    return true;
                }
            }
        }
        timeOut(golem, stack);
        return false;
    }

    /** Отметка, по которой голем с ядром использования может щёлкнуть этим предметом (findSomething оригинала). */
    @Nullable
    public static GolemMarker useTarget(Golem golem, ItemStack stack) {
        boolean wantAir = golem.getToggles()[0];
        for (byte color : colorsMatching(golem, stack)) {
            for (GolemMarker marker : golem.getMarkers()) {
                if ((marker.color() == color || color == -1) && golem.level().isEmptyBlock(marker.pos()) == wantAir
                        && golem.level().isEmptyBlock(marker.pos().relative(Direction.from3DDataValue(marker.side())))) {
                    return marker;
                }
            }
        }
        return null;
    }

    public static boolean findSomethingUseCore(Golem golem, ItemStack stack) {
        if (useTarget(golem, stack) != null) {
            return true;
        }
        timeOut(golem, stack);
        return false;
    }

    /** Отмеченные хранилища с местом, в которых такой предмет уже лежит: туда его относит сортировщик. */
    public static List<BlockPos> sortTargets(Golem golem, ItemStack stack) {
        List<BlockPos> result = new ArrayList<>();
        for (BlockPos pos : containersWithRoom(golem, (byte) -1, stack)) {
            for (Direction side : markedSides(golem, pos, (byte) -1)) {
                IItemHandler handler = handler(golem.level(), pos, side);
                if (handler != null && count(golem, handler, stack) > 0) {
                    result.add(pos);
                    break;
                }
            }
        }
        return result;
    }

    public static boolean findSomethingSortCore(Golem golem, ItemStack stack) {
        if (nearest(golem, sortTargets(golem, stack)) != null) {
            return true;
        }
        timeOut(golem, stack);
        return false;
    }

    /** Есть ли у этого ядра дело для такого предмета. */
    public static boolean findSomething(Golem golem, ItemStack stack) {
        return switch (golem.getCore()) {
            case GolemCoreItem.USE -> findSomethingUseCore(golem, stack);
            case GolemCoreItem.SORTING -> findSomethingSortCore(golem, stack);
            default -> findSomethingEmptyCore(golem, stack);
        };
    }

    /** Образцы, которые голему есть куда отнести; null — образцов нет, и голем берёт всё подряд. */
    @Nullable
    public static List<ItemStack> itemsNeeded(Golem golem) {
        if (golem.getCore() == GolemCoreItem.SORTING) {
            // сортировщику образцы не нужны: он перебирает всё, что лежит дома
            IItemHandler home = homeHandler(golem);
            if (home == null) {
                return null;
            }
            List<ItemStack> out = new ArrayList<>();
            for (int slot = 0; slot < home.getSlots(); slot++) {
                ItemStack there = home.getStackInSlot(slot);
                if (!there.isEmpty() && !isOnTimeOut(golem, there) && findSomethingSortCore(golem, there)) {
                    out.add(there.copy());
                }
            }
            return out;
        }
        if (golem.getCore() != GolemCoreItem.EMPTY && golem.getCore() != GolemCoreItem.USE) {
            return null;
        }
        List<ItemStack> samples = samples(golem);
        if (samples.isEmpty()) {
            return null;
        }
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack sample : samples) {
            if (!isOnTimeOut(golem, sample) && findSomething(golem, sample)) {
                out.add(sample);
            }
        }
        return out;
    }

    public static boolean validTargetForItem(Golem golem, ItemStack stack) {
        if (isOnTimeOut(golem, stack)) {
            return false;
        }
        if (golem.getCore() == GolemCoreItem.EMPTY || golem.getCore() == GolemCoreItem.USE || golem.getCore() == GolemCoreItem.SORTING) {
            return findSomething(golem, stack);
        }
        List<ItemStack> needed = itemsNeeded(golem);
        if (needed != null) {
            for (ItemStack sample : needed) {
                if (matches(golem, sample, golem.getCarried())) {
                    return true;
                }
            }
        }
        timeOut(golem, stack);
        return false;
    }

    /** Первый предмет хранилища, о котором голем сейчас не «забыл»: столько, сколько он может унести. */
    public static ItemStack firstItemUsingTimeout(Golem golem, IItemHandler handler, boolean doit) {
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack there = handler.getStackInSlot(slot);
            if (there.isEmpty() || isOnTimeOut(golem, there)) {
                continue;
            }
            ItemStack taken = handler.extractItem(slot, golem.getCarrySpace(), !doit);
            if (!taken.isEmpty()) {
                return taken.copy();
            }
        }
        return ItemStack.EMPTY;
    }
}
