package thaumcraft.common.entity.golem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Задачи големов-носильщиков — перенос AIHome*, AIFill*, AIEmpty* и AIItemPickup оригинала. Ядро наполнения приносит
 * домой недостающее из отмеченных хранилищ, ядро опустошения разносит из дома по отметкам, ядро сбора подбирает
 * лежащие вокруг предметы и складывает их дома.
 */
public final class GolemCarryGoals {
    private GolemCarryGoals() {
    }

    abstract static class Base extends Goal {
        protected final Golem golem;
        /** Сколько тиков сундук ещё держит крышку открытой. */
        protected int countChest;
        @Nullable
        protected BlockPos chest;

        Base(Golem golem) {
            this.golem = golem;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        protected boolean scanTick() {
            return golem.tickCount % GolemHelper.DELAY == 0;
        }

        protected boolean idle() {
            return golem.getNavigation().isDone();
        }

        protected boolean nearHome() {
            BlockPos home = golem.getHome();
            return golem.distanceToSqr(home.getX() + 0.5, home.getY() + 0.5, home.getZ() + 0.5) <= 5.0;
        }

        protected void open(BlockPos pos) {
            GolemHelper.openChest(golem.level(), pos, true);
            chest = pos;
            countChest = 5;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void stop() {
            if (chest != null) {
                GolemHelper.openChest(golem.level(), chest, false);
                chest = null;
            }
        }
    }

    /** Общая часть переходов: идём к цели до 200 тиков, а упёршись — пробуем обойти. */
    abstract static class Walk extends Base {
        protected Vec3 target = Vec3.ZERO;
        protected int count;
        private BlockPos previous = BlockPos.ZERO;

        Walk(Golem golem) {
            super(golem);
        }

        @Override
        public boolean canContinueToUse() {
            return count > 0 && !golem.getNavigation().isDone();
        }

        @Override
        public void start() {
            count = 200;
            previous = golem.blockPosition();
            GolemHelper.walkTo(golem, target.x, target.y, target.z);
        }

        @Override
        public void tick() {
            count--;
            if (count == 0 && previous.equals(golem.blockPosition())) {
                Vec3 around = DefaultRandomPos.getPos(golem, 2, 1);
                if (around != null) {
                    count = 20;
                    golem.getNavigation().moveTo(around.x, around.y, around.z, 1.0);
                }
            }
        }

        @Override
        public void stop() {
            count = 0;
            super.stop();
        }
    }

    /** Бросает ношу в сторону блока. */
    private static void toss(Golem golem, BlockPos towards) {
        ItemEntity item = new ItemEntity(golem.level(), golem.getX(), golem.getY() + golem.getBbHeight() / 2f, golem.getZ(),
                golem.getCarried().copy());
        double distance = Math.sqrt(golem.distanceToSqr(towards.getX() + 0.5, towards.getY() + 0.5, towards.getZ() + 0.5));
        item.setDeltaMovement((towards.getX() + 0.5 - golem.getX()) * (distance / 3.0),
                0.1 + (towards.getY() + 0.5 - (golem.getY() + golem.getBbHeight() / 2f)) * (distance / 3.0),
                (towards.getZ() + 0.5 - golem.getZ()) * (distance / 3.0));
        item.setPickUpDelay(10);
        golem.level().addFreshEntity(item);
        golem.setCarried(ItemStack.EMPTY);
        golem.startActionTimer();
    }

    // ---------- дом ----------

    /** AIHomeReplace: голем у дома с ношей, которую некуда деть, кладёт её обратно в домашнее хранилище. */
    public static class HomeReplace extends Base {
        public HomeReplace(Golem golem) {
            super(golem);
        }

        @Override
        public boolean canUse() {
            ItemStack carried = golem.getCarried();
            if (carried.isEmpty() || !scanTick() || !idle() || !nearHome()) {
                return false;
            }
            if (GolemHelper.isOnTimeOut(golem, carried)) {
                return true;
            }
            if (golem.getCore() == thaumcraft.common.items.golem.GolemCoreItem.EMPTY
                    || golem.getCore() == thaumcraft.common.items.golem.GolemCoreItem.USE
                    || golem.getCore() == thaumcraft.common.items.golem.GolemCoreItem.SORTING) {
                return !GolemHelper.findSomething(golem, carried);
            }
            List<ItemStack> needed = GolemHelper.itemsNeeded(golem);
            if (needed == null || needed.isEmpty()) {
                return false;
            }
            for (ItemStack sample : needed) {
                if (GolemHelper.matches(golem, sample, carried)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse() || countChest > 0;
        }

        @Override
        public void tick() {
            countChest--;
        }

        @Override
        public void start() {
            IItemHandler home = GolemHelper.homeHandler(golem);
            if (home != null && GolemHelper.hasRoom(home, golem.getCarried())) {
                golem.setCarried(GolemHelper.insert(home, golem.getCarried(), true));
                open(GolemHelper.homeBlock(golem));
            }
        }
    }

    /** AIHomePlace: голем у дома кладёт ношу в домашнее хранилище. */
    public static class HomePlace extends Base {
        public HomePlace(Golem golem) {
            super(golem);
        }

        @Override
        public boolean canUse() {
            if (golem.getCarried().isEmpty() || !scanTick() || !idle() || !nearHome()) {
                return false;
            }
            IItemHandler home = GolemHelper.homeHandler(golem);
            return home != null && GolemHelper.hasRoom(home, golem.getCarried());
        }

        @Override
        public boolean canContinueToUse() {
            return canUse() || countChest > 0;
        }

        @Override
        public void tick() {
            countChest--;
        }

        @Override
        public void start() {
            IItemHandler home = GolemHelper.homeHandler(golem);
            if (home != null && GolemHelper.hasRoom(home, golem.getCarried())) {
                golem.setCarried(GolemHelper.insert(home, golem.getCarried(), true));
                open(GolemHelper.homeBlock(golem));
            }
        }
    }

    /** AIHomeDrop: если дома нет хранилища, голем бросает ношу к домашнему блоку. */
    public static class HomeDrop extends Base {
        private int count;

        public HomeDrop(Golem golem) {
            super(golem);
        }

        @Override
        public boolean canUse() {
            return !golem.getCarried().isEmpty() && idle() && nearHome() && !GolemHelper.isContainer(golem.level(), GolemHelper.homeBlock(golem));
        }

        @Override
        public boolean canContinueToUse() {
            return count > 0 && canUse();
        }

        @Override
        public void tick() {
            count--;
        }

        @Override
        public void start() {
            count = 200;
            toss(golem, GolemHelper.homeBlock(golem));
        }
    }

    /** AIHomeTake: голем с ядром опустошения берёт из домашнего хранилища то, что есть куда отнести. */
    public static class HomeTake extends Base {
        public HomeTake(Golem golem) {
            super(golem);
        }

        @Override
        public boolean canUse() {
            if (!golem.getCarried().isEmpty() || !scanTick() || !idle() || !nearHome()) {
                return false;
            }
            IItemHandler home = GolemHelper.homeHandler(golem);
            if (home == null) {
                return false;
            }
            List<ItemStack> needed = GolemHelper.itemsNeeded(golem);
            if (needed == null) {
                // образцов нет: берём первое, что есть куда пристроить
                while (true) {
                    ItemStack first = GolemHelper.firstItemUsingTimeout(golem, home, false);
                    if (first.isEmpty()) {
                        return false;
                    }
                    if (GolemHelper.validTargetForItem(golem, first)) {
                        golem.setCarried(GolemHelper.firstItemUsingTimeout(golem, home, true));
                        open(GolemHelper.homeBlock(golem));
                        return true;
                    }
                }
            }
            for (ItemStack sample : needed) {
                if (GolemHelper.validTargetForItem(golem, sample)) {
                    ItemStack taken = GolemHelper.extract(golem, home, sample, golem.getCarrySpace(), true);
                    if (!taken.isEmpty()) {
                        golem.setCarried(taken);
                        open(GolemHelper.homeBlock(golem));
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            return countChest > 0;
        }

        @Override
        public void tick() {
            countChest--;
        }
    }

    // ---------- ядро наполнения ----------

    /** AIFillTake: стоя у отмеченного хранилища, голем берёт из него то, за чем пришёл. */
    public static class FillTake extends Base {
        private int count;

        public FillTake(Golem golem) {
            super(golem);
        }

        @Override
        public boolean canUse() {
            if (!golem.getCarried().isEmpty() || golem.itemWatched.isEmpty() || !idle() || !GolemHelper.hasSomething(golem.inventory)) {
                return false;
            }
            BlockPos home = GolemHelper.homeBlock(golem);
            for (BlockPos pos : GolemHelper.markedContainersAdjacent(golem)) {
                if (pos.equals(home)) {
                    continue;
                }
                for (byte color : GolemHelper.colorsMatching(golem, golem.itemWatched)) {
                    for (Direction side : GolemHelper.markedSides(golem, pos, color)) {
                        IItemHandler handler = GolemHelper.handler(golem.level(), pos, side);
                        if (handler == null) {
                            continue;
                        }
                        int amount = golem.getToggles()[0] ? golem.getCarrySpace() : Math.min(golem.itemWatched.getCount(), golem.getCarrySpace());
                        ItemStack taken = GolemHelper.extract(golem, handler, golem.itemWatched, amount, true);
                        if (!taken.isEmpty()) {
                            golem.setCarried(taken);
                            open(pos);
                            count = 200;
                            golem.itemWatched = ItemStack.EMPTY;
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            return count > 0 && (!idle() || countChest > 0);
        }

        @Override
        public void tick() {
            count--;
            countChest--;
        }
    }

    /** AIFillGoto: голем ищет, чего дома не хватает, и идёт к ближайшему отмеченному хранилищу, где это есть. */
    public static class FillGoto extends Walk {
        public FillGoto(Golem golem) {
            super(golem);
        }

        @Override
        public boolean canUse() {
            if (!golem.getCarried().isEmpty() || !scanTick() || !GolemHelper.hasSomething(golem.inventory)) {
                return false;
            }
            List<ItemStack> missing = GolemHelper.missingItems(golem);
            if (missing == null || missing.isEmpty()) {
                return false;
            }
            List<BlockPos> found = new ArrayList<>();
            for (ItemStack stack : missing) {
                golem.itemWatched = stack.copy();
                for (byte color : GolemHelper.colorsMatching(golem, stack)) {
                    found = GolemHelper.containersWithGoods(golem, stack, color);
                    if (!found.isEmpty()) {
                        break;
                    }
                }
                if (!found.isEmpty()) {
                    break;
                }
            }
            BlockPos dest = GolemHelper.nearest(golem, found);
            if (dest == null) {
                return false;
            }
            target = new Vec3(dest.getX(), dest.getY(), dest.getZ());
            return true;
        }
    }

    // ---------- ядро опустошения ----------

    /** AIEmptyPlace: стоя у отмеченного хранилища, голем кладёт в него ношу. */
    public static class EmptyPlace extends Base {
        private int count;
        private BlockPos found = BlockPos.ZERO;

        public EmptyPlace(Golem golem) {
            super(golem);
        }

        @Override
        public boolean canUse() {
            ItemStack carried = golem.getCarried();
            if (carried.isEmpty() || !idle()) {
                return false;
            }
            BlockPos home = GolemHelper.homeBlock(golem);
            for (BlockPos pos : GolemHelper.markedContainersAdjacent(golem)) {
                if (pos.equals(home)) {
                    continue;
                }
                for (byte color : GolemHelper.colorsMatching(golem, carried)) {
                    for (Direction side : GolemHelper.markedSides(golem, pos, color)) {
                        IItemHandler handler = GolemHelper.handler(golem.level(), pos, side);
                        if (handler != null && GolemHelper.hasRoom(handler, carried)) {
                            found = pos;
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            return count > 0 && (canUse() || countChest > 0);
        }

        @Override
        public void tick() {
            count--;
            countChest--;
        }

        @Override
        public void start() {
            count = 200;
            boolean placed = false;
            for (byte color : GolemHelper.colorsMatching(golem, golem.getCarried())) {
                for (Direction side : GolemHelper.markedSides(golem, found, color)) {
                    IItemHandler handler = GolemHelper.handler(golem.level(), found, side);
                    if (handler != null && GolemHelper.hasRoom(handler, golem.getCarried())) {
                        golem.setCarried(GolemHelper.insert(handler, golem.getCarried(), true));
                        placed = true;
                        if (golem.getCarried().isEmpty()) {
                            break;
                        }
                    }
                }
                if (placed) {
                    open(found);
                    break;
                }
            }
        }
    }

    /** AIEmptyDrop: у отметки, за которой нет хранилища, голем бросает ношу в её сторону. */
    public static class EmptyDrop extends Base {
        private int count;

        public EmptyDrop(Golem golem) {
            super(golem);
        }

        @Nullable
        private BlockPos spot() {
            for (byte color : GolemHelper.colorsMatching(golem, golem.getCarried())) {
                for (BlockPos pos : GolemHelper.markedBlocksAdjacent(golem, color)) {
                    return pos;
                }
            }
            return null;
        }

        @Override
        public boolean canUse() {
            return !golem.getCarried().isEmpty() && idle() && spot() != null;
        }

        @Override
        public boolean canContinueToUse() {
            return count > 0 && canUse();
        }

        @Override
        public void tick() {
            count--;
        }

        @Override
        public void start() {
            count = 200;
            BlockPos pos = spot();
            if (pos != null) {
                toss(golem, pos);
            }
        }
    }

    /** AIEmptyGoto: голем с ношей идёт к ближайшему отмеченному хранилищу с местом, а нет такого — к отметке без хранилища. */
    public static class EmptyGoto extends Walk {
        public EmptyGoto(Golem golem) {
            super(golem);
        }

        @Override
        public boolean canUse() {
            ItemStack carried = golem.getCarried();
            if (carried.isEmpty() || !scanTick()) {
                return false;
            }
            List<Byte> colors = GolemHelper.colorsMatching(golem, carried);
            for (byte color : colors) {
                BlockPos dest = GolemHelper.nearest(golem, GolemHelper.containersWithRoom(golem, color, carried));
                if (dest != null) {
                    target = new Vec3(dest.getX(), dest.getY(), dest.getZ());
                    return true;
                }
            }
            for (byte color : colors) {
                for (GolemMarker marker : golem.getMarkers()) {
                    if ((marker.color() == color || color == -1) && !GolemHelper.isContainer(golem.level(), marker.pos())) {
                        target = new Vec3(marker.pos().getX(), marker.pos().getY(), marker.pos().getZ());
                        return true;
                    }
                }
            }
            return false;
        }
    }

    // ---------- ядро сбора ----------

    /** AIItemPickup: голем подбирает лежащие в пределах своей дальности предметы, подходящие под образцы. */
    public static class ItemPickup extends Goal {
        private final Golem golem;
        @Nullable
        private ItemEntity target;
        private int count;

        public ItemPickup(Golem golem) {
            this.golem = golem;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (golem.tickCount % GolemHelper.DELAY != 0) {
                return false;
            }
            float reach = golem.getRange();
            BlockPos home = golem.getHome();
            double best = Double.MAX_VALUE;
            target = null;
            boolean any = golem.inventory.isEmpty();
            for (ItemEntity item : golem.level().getEntitiesOfClass(ItemEntity.class, new AABB(home).inflate(reach))) {
                ItemStack stack = item.getItem();
                if (!item.isAlive() || item.hasPickUpDelay() || !(any || GolemHelper.amountNeeded(golem, stack) > 0)) {
                    continue;
                }
                ItemStack carried = golem.getCarried();
                if (!carried.isEmpty() && (!ItemStack.isSameItemSameTags(carried, stack) || golem.getCarrySpace() <= 0)) {
                    continue;
                }
                double fromHome = item.distanceToSqr(home.getX() + 0.5, home.getY() + 0.5, home.getZ() + 0.5);
                double fromGolem = item.distanceToSqr(golem);
                if (fromGolem < best && fromHome <= reach * reach) {
                    best = fromGolem;
                    target = item;
                }
            }
            return target != null;
        }

        @Override
        public boolean canContinueToUse() {
            return count-- > 0 && !golem.getNavigation().isDone() && target != null && target.isAlive();
        }

        @Override
        public void start() {
            count = 200;
            GolemHelper.walkTo(golem, target);
        }

        @Override
        public void stop() {
            count = 0;
            target = null;
            golem.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (target == null) {
                return;
            }
            golem.getLookControl().setLookAt(target, 30f, 30f);
            if (golem.distanceToSqr(target) <= 2.0) {
                ItemStack stack = target.getItem();
                int amount = Math.min(stack.getCount(), golem.getCarrySpace());
                if (amount <= 0) {
                    return;
                }
                ItemStack carried = golem.getCarried();
                if (carried.isEmpty()) {
                    golem.setCarried(stack.copyWithCount(amount));
                } else {
                    carried.grow(amount);
                    golem.updateCarried();
                }
                stack.shrink(amount);
                if (stack.isEmpty()) {
                    target.discard();
                } else {
                    target.setItem(stack);
                }
                golem.playSound(SoundEvents.ITEM_PICKUP, 0.2f, ((golem.getRandom().nextFloat() - golem.getRandom().nextFloat()) * 0.7f + 1f) * 2f);
            }
        }
    }
}
