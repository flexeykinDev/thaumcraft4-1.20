package thaumcraft.common.items.golem;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Украшение голема (ItemGolemDecoration оригинала). Кроме вида, каждое что-то даёт: цилиндр — здоровье, феска —
 * быстрое восстановление, очки — дальность, визор — броню и добычу как от хозяина, бабочка — скорость,
 * латы — броню ценой скорости, дротикомёт — дальний бой, булава — урон. Цилиндр и феска, очки и визор,
 * бабочка и латы друг с другом не сочетаются.
 */
public class GolemDecorationItem extends Item {
    /** Имена предметов и буквы, которыми украшения записаны у голема, по номеру оригинала. */
    public static final String[] NAMES = {"tophat", "glasses", "bowtie", "fez", "dart", "visor", "armor", "mace"};
    public static final String LETTERS = "HGBFRVPM";

    private final int index;

    public GolemDecorationItem(int index) {
        super(new Item.Properties());
        this.index = index;
    }

    public String getLetter() {
        return String.valueOf(LETTERS.charAt(index));
    }

    /** «Аксессуар: Цилиндр», как в оригинале. */
    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.ItemGolemDecoration.name").append(": ")
                .append(Component.translatable("item.ItemGolemDecoration." + index + ".name"));
    }
}
