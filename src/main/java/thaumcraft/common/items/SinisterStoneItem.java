package thaumcraft.common.items;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Зловещий камень — перенос ItemCompassStone оригинала. Тёмные узлы ауры дают о себе знать раз в две с половиной
 * секунды; камень в руке или сумке разгорается, когда игрок смотрит в сторону такого узла (в 256 блоках).
 */
public class SinisterStoneItem extends Item {
    /** Последние вести от тёмных узлов: где и когда (только на клиенте). */
    private static final Map<String, Long> SINISTER = new ConcurrentHashMap<>();
    private static boolean active;

    public SinisterStoneItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
    }

    /** Тёмный узел напомнил о себе (вызывается на клиенте). */
    public static void mark(Level level, BlockPos pos) {
        SINISTER.put(level.dimension().location() + "|" + pos.asLong(), System.currentTimeMillis());
    }

    public static boolean isActive() {
        return active;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity holder, int slot, boolean selected) {
        if (!level.isClientSide) {
            return;
        }
        boolean found = false;
        long now = System.currentTimeMillis();
        String dimension = level.dimension().location().toString();
        Vec3 eye = holder.getEyePosition();
        Vec3 look = holder.getLookAngle();
        for (Iterator<Map.Entry<String, Long>> it = SINISTER.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, Long> entry = it.next();
            if (entry.getValue() < now - 10000L) {
                it.remove();
                continue;
            }
            String[] key = entry.getKey().split("\\|");
            if (found || !key[0].equals(dimension)) {
                continue;
            }
            Vec3 node = Vec3.atCenterOf(BlockPos.of(Long.parseLong(key[1])));
            Vec3 to = node.subtract(eye);
            // узел в конусе взгляда шириной 0.66 радиана и не дальше 256 блоков (isVisibleTo оригинала)
            if (to.length() <= 256.0 && to.normalize().dot(look) > Math.cos(0.33)) {
                found = true;
            }
        }
        active = found;
    }

    /** Имя свойства модели, по которому камень светится. */
    public static final ResourceLocation ACTIVE = new ResourceLocation("thaumcraft", "active");
}
