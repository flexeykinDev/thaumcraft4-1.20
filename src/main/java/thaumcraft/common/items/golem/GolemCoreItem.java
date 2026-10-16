package thaumcraft.common.items.golem;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Ядро оживления голема (ItemGolemCore оригинала): задаёт, чем голем занят. Пустое ядро (-1) — заготовка для остальных.
 */
public class GolemCoreItem extends Item {
    public static final int BLANK = -1;
    public static final int FILL = 0;
    public static final int EMPTY = 1;
    public static final int GATHER = 2;
    public static final int HARVEST = 3;
    public static final int GUARD = 4;
    public static final int LIQUID = 5;
    public static final int ALCHEMY = 6;
    public static final int LUMBER = 7;
    public static final int USE = 8;
    public static final int BUTCHER = 9;
    public static final int SORTING = 10;
    public static final int FISHING = 11;

    /** Имена предметов по номеру ядра. */
    public static final String[] NAMES = {"fill", "empty", "gather", "harvest", "guard", "liquid", "alchemy", "lumber", "use", "butcher",
            "sorting", "fishing"};

    private final int core;

    public GolemCoreItem(int core) {
        super(new Item.Properties().rarity(core == BLANK ? Rarity.COMMON : Rarity.UNCOMMON));
        this.core = core;
    }

    public int getCore() {
        return core;
    }

    /** Название ядра для подсказок: «Наполнение», «Сбор» и так далее. */
    public static Component coreName(int core) {
        return Component.translatable("item.ItemGolemCore." + (core < 0 ? 100 : core) + ".name");
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(coreName(core).copy().withStyle(ChatFormatting.GRAY));
    }

    /** У голема с таким ядром есть окно настроек. */
    public static boolean hasGUI(int core) {
        return core == FILL || core == EMPTY || core == GATHER || core == GUARD || core == LIQUID || core == USE || core == SORTING;
    }

    /** Голем с таким ядром различает цвета отметок. */
    public static boolean canSort(int core) {
        return core == FILL || core == EMPTY || core == GATHER || core == USE || core == SORTING;
    }

    /** У голема с таким ядром есть ячейки-образцы. */
    public static boolean hasInventory(int core) {
        return core == FILL || core == EMPTY || core == GATHER || core == LIQUID || core == USE;
    }
}
