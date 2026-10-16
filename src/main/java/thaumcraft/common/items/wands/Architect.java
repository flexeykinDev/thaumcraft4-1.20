package thaumcraft.common.items.wands;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.api.wands.FocusUpgrade;

import java.util.List;

/**
 * Фокус с улучшением «Архитектор» (IArchitect оригинала): действует не на один блок, а на отмеченную область.
 * Размеры области по трём осям хранит жезл; их меняет клавиша «Переключатель жезла» (в оригинале G):
 * нажатие увеличивает размер, с приседанием — выбирает, какую ось менять.
 */
public interface Architect {
    /** Блоки, на которые подействует фокус, если нажать на грань side блока pos. */
    List<BlockPos> getArchitectBlocks(ItemStack wand, Level level, BlockPos pos, Direction side, Player player);

    /** Показывать ли стрелки вдоль этой оси: они отмечают, какой размер сейчас меняет клавиша. */
    boolean showAxis(ItemStack wand, Direction side, Direction.Axis axis);

    /** Наибольший размер области по одной оси (в блоках от центра). */
    int getMaxAreaSize(ItemStack focus);

    /** Сколько осей можно менять по отдельности: 2 у обмена (область плоская), 3 у защиты. */
    default int getAreaDimensions() {
        return 3;
    }

    /** Надет ли на жезл фокус-архитектор с поставленным улучшением. */
    static boolean isActive(ItemStack wand) {
        if (!(wand.getItem() instanceof WandItem)) {
            return false;
        }
        ItemStack focus = WandItem.getFocusStack(wand);
        return focus.getItem() instanceof Architect && FocusItem.isUpgradedWith(focus, FocusUpgrade.ARCHITECT);
    }

    private static int max(ItemStack wand) {
        ItemStack focus = WandItem.getFocusStack(wand);
        return focus.getItem() instanceof Architect architect ? architect.getMaxAreaSize(focus) : 0;
    }

    private static int area(ItemStack wand, String key) {
        int max = max(wand);
        return wand.hasTag() && wand.getTag().contains(key) ? Math.min(wand.getTag().getInt(key), max) : max;
    }

    static int getAreaX(ItemStack wand) {
        return area(wand, "areax");
    }

    static int getAreaY(ItemStack wand) {
        return area(wand, "areay");
    }

    static int getAreaZ(ItemStack wand) {
        return area(wand, "areaz");
    }

    /** Какую ось меняет клавиша: 0 — все сразу, 1, 2, 3 — по одной. */
    static int getAreaDim(ItemStack wand) {
        return wand.hasTag() ? wand.getTag().getInt("aread") : 0;
    }

    /** WandManager.toggleMisc оригинала. */
    static void toggle(ItemStack wand, Player player) {
        if (!isActive(wand)) {
            return;
        }
        Architect architect = (Architect) WandItem.getFocusStack(wand).getItem();
        CompoundTag tag = wand.getOrCreateTag();
        int dim = getAreaDim(wand);
        if (player.isShiftKeyDown()) {
            dim++;
            if (dim > architect.getAreaDimensions()) {
                dim = 0;
            }
            tag.putInt("aread", dim);
        } else {
            int max = max(wand);
            int x = getAreaX(wand);
            int y = getAreaY(wand);
            int z = getAreaZ(wand);
            if (dim == 0) {
                x++;
                y++;
                z++;
            } else if (dim == 1) {
                x++;
            } else if (dim == 2) {
                z++;
            } else {
                y++;
            }
            tag.putInt("areax", x > max ? 0 : x);
            tag.putInt("areay", y > max ? 0 : y);
            tag.putInt("areaz", z > max ? 0 : z);
        }
        player.displayClientMessage(Component.literal(getAreaX(wand) + " x " + getAreaY(wand) + " x " + getAreaZ(wand)), true);
    }
}
