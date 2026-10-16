package thaumcraft.common.entity.golem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.IItemHandler;

import java.util.List;

/**
 * Задачи голема с ядром сортировки — перенос AISortingGoto, AISortingPlace и AIHomeTakeSorting оригинала: он берёт
 * из домашнего хранилища предметы и раскладывает их по отмеченным хранилищам, в которых такие же уже лежат.
 */
public final class GolemSortGoals {
    private GolemSortGoals() {
    }

    /** Берёт из дома первый предмет, которому нашлось место среди отмеченных хранилищ. */
    public static class HomeTake extends GolemCarryGoals.Base {
        private int count;

        public HomeTake(Golem golem) {
            super(golem);
        }

        private ItemStack take(boolean doit) {
            IItemHandler home = GolemHelper.homeHandler(golem);
            List<ItemStack> needed = GolemHelper.itemsNeeded(golem);
            if (home == null || needed == null) {
                return ItemStack.EMPTY;
            }
            for (ItemStack sample : needed) {
                ItemStack taken = GolemHelper.extract(golem, home, sample, golem.getCarrySpace(), doit);
                if (!taken.isEmpty()) {
                    return taken;
                }
            }
            return ItemStack.EMPTY;
        }

        @Override
        public boolean canUse() {
            return golem.getCarried().isEmpty() && scanTick() && idle() && nearHome() && !take(false).isEmpty();
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
            ItemStack taken = take(true);
            if (!taken.isEmpty()) {
                golem.setCarried(taken);
                open(GolemHelper.homeBlock(golem));
            }
        }
    }

    /** Идёт к ближайшему отмеченному хранилищу, где такой предмет уже есть и ещё осталось место. */
    public static class Goto extends GolemCarryGoals.Walk {
        public Goto(Golem golem) {
            super(golem);
        }

        @Override
        public boolean canUse() {
            ItemStack carried = golem.getCarried();
            if (carried.isEmpty() || !scanTick()) {
                return false;
            }
            BlockPos dest = GolemHelper.nearest(golem, GolemHelper.sortTargets(golem, carried));
            if (dest == null) {
                return false;
            }
            target = new Vec3(dest.getX(), dest.getY(), dest.getZ());
            return true;
        }
    }

    /** Стоя у такого хранилища, кладёт в него ношу. */
    public static class Place extends GolemCarryGoals.Base {
        private int count;
        private BlockPos found = BlockPos.ZERO;

        public Place(Golem golem) {
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
                for (Direction side : GolemHelper.markedSides(golem, pos, (byte) -1)) {
                    IItemHandler handler = GolemHelper.handler(golem.level(), pos, side);
                    if (handler != null && GolemHelper.hasRoom(handler, carried) && GolemHelper.count(golem, handler, carried) > 0) {
                        found = pos;
                        return true;
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
            for (Direction side : GolemHelper.markedSides(golem, found, (byte) -1)) {
                IItemHandler handler = GolemHelper.handler(golem.level(), found, side);
                if (handler != null && GolemHelper.hasRoom(handler, golem.getCarried())) {
                    golem.setCarried(GolemHelper.insert(handler, golem.getCarried(), true));
                    open(found);
                    if (golem.getCarried().isEmpty()) {
                        break;
                    }
                }
            }
        }
    }
}
