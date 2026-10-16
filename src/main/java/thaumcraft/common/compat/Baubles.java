package thaumcraft.common.compat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Украшения игрока. В оригинале их держал мод Baubles (амулет, два кольца, пояс); здесь те же ячейки
 * даёт Curios API. Весь доступ к Curios собран в этом классе.
 */
public final class Baubles {
    private Baubles() {
    }

    /** Надетые украшения, подходящие под условие. Стеки настоящие: их изменения сохраняются. */
    public static List<ItemStack> find(LivingEntity entity, Predicate<ItemStack> filter) {
        List<ItemStack> out = new ArrayList<>();
        CuriosApi.getCuriosInventory(entity).ifPresent(handler -> {
            for (SlotResult result : handler.findCurios(filter)) {
                out.add(result.stack());
            }
        });
        return out;
    }
}
