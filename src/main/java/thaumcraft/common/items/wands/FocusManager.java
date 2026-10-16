package thaumcraft.common.items.wands;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import thaumcraft.common.registry.ModSounds;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/** Та часть WandManager оригинала, что касается фокусов: паузы между выстрелами, смена фокуса, прицеливание. */
public final class FocusManager {
    /** Особый ключ пакета смены фокуса: снять фокус с жезла. */
    public static final String REMOVE = "REMOVE";

    private static final Map<Integer, Long> COOLDOWN_SERVER = new HashMap<>();
    private static final Map<Integer, Long> COOLDOWN_CLIENT = new HashMap<>();

    private FocusManager() {
    }

    private static Map<Integer, Long> cooldowns(LivingEntity entity) {
        return entity.level().isClientSide ? COOLDOWN_CLIENT : COOLDOWN_SERVER;
    }

    public static boolean isOnCooldown(LivingEntity entity) {
        Long until = cooldowns(entity).get(entity.getId());
        return until != null && until > System.currentTimeMillis();
    }

    /** Сколько секунд осталось до следующего выстрела. */
    public static float getCooldown(LivingEntity entity) {
        Long until = cooldowns(entity).get(entity.getId());
        return until == null ? 0f : Math.max(0f, (until - System.currentTimeMillis()) / 1000f);
    }

    public static void setCooldown(LivingEntity entity, int millis) {
        if (millis == 0) {
            cooldowns(entity).remove(entity.getId());
        } else {
            cooldowns(entity).put(entity.getId(), System.currentTimeMillis() + millis);
        }
    }

    /**
     * Смена фокуса (WandManager.changeFocus оригинала). Фокусы ищутся в инвентаре игрока и упорядочены по ключу;
     * берётся фокус с названным ключом, а если его нет — следующий по порядку. Прежний фокус возвращается в инвентарь.
     */
    public static void changeFocus(ServerPlayer player, String key) {
        ItemStack wand = player.getMainHandItem();
        if (!(wand.getItem() instanceof WandItem) || WandItem.isSceptre(wand)) {
            return;
        }
        // где лежит каждый фокус: в ячейке инвентаря (pouch == null) либо в ячейке одной из сумок
        record Place(@Nullable ItemStack pouch, int slot) {
        }
        TreeMap<String, Place> foci = new TreeMap<>();
        List<ItemStack> pouches = FocusPouchItem.findPouches(player);
        for (ItemStack pouch : pouches) {
            List<ItemStack> inside = FocusPouchItem.getInventory(pouch);
            for (int slot = 0; slot < inside.size(); slot++) {
                if (inside.get(slot).getItem() instanceof FocusItem focus) {
                    foci.put(focus.getSortingHelper(inside.get(slot)), new Place(pouch, slot));
                }
            }
        }
        for (int slot = 0; slot < player.getInventory().items.size(); slot++) {
            ItemStack item = player.getInventory().items.get(slot);
            if (item.getItem() instanceof FocusItem focus) {
                foci.put(focus.getSortingHelper(item), new Place(null, slot));
            }
        }
        ItemStack current = WandItem.getFocusStack(wand);
        if (REMOVE.equals(key) || foci.isEmpty()) {
            if (!current.isEmpty() && (FocusPouchItem.addFocus(player, current) || player.getInventory().add(current.copy()))) {
                WandItem.setFocus(wand, ItemStack.EMPTY);
                player.level().playSound(null, player, ModSounds.CAMERA_TICKS.get(), SoundSource.PLAYERS, 0.3f, 0.9f);
            }
            return;
        }
        String chosen = key;
        if (!foci.containsKey(chosen)) {
            chosen = foci.higherKey(key);
        }
        if (chosen == null) {
            chosen = foci.firstKey();
        }
        Place place = foci.get(chosen);
        ItemStack item;
        if (place.pouch() == null) {
            item = player.getInventory().items.get(place.slot()).copy();
            player.getInventory().items.set(place.slot(), ItemStack.EMPTY);
        } else {
            List<ItemStack> inside = FocusPouchItem.getInventory(place.pouch());
            item = inside.get(place.slot()).copy();
            inside.set(place.slot(), ItemStack.EMPTY);
            FocusPouchItem.setInventory(place.pouch(), inside);
        }
        player.level().playSound(null, player, ModSounds.CAMERA_TICKS.get(), SoundSource.PLAYERS, 0.3f, 1f);
        // прежний фокус уходит в сумку, а если места нет — в инвентарь
        if (!current.isEmpty() && (FocusPouchItem.addFocus(player, current) || player.getInventory().add(current.copy()))) {
            WandItem.setFocus(wand, ItemStack.EMPTY);
        }
        if (WandItem.getFocusStack(wand).isEmpty()) {
            WandItem.setFocus(wand, item);
        } else if (!FocusPouchItem.addFocus(player, item)) {
            player.getInventory().add(item);
        }
    }

    /** Точка, откуда вылетают заклинания: вершина жезла чуть впереди и сбоку от глаз. */
    public static Vec3 wandTip(Player player) {
        Vec3 look = player.getLookAngle();
        Vec3 side = look.cross(new Vec3(0, 1, 0));
        if (side.lengthSqr() > 1.0e-4) {
            side = side.normalize();
        }
        return player.getEyePosition().add(look.scale(0.5)).add(side.scale(0.25)).add(0, -0.2, 0);
    }

    /** Куда упирается взгляд: в блок не дальше range или в точку на таком расстоянии. */
    public static Vec3 lookTarget(Player player, double range) {
        Vec3 eye = player.getEyePosition();
        BlockHitResult hit = player.level().clip(new ClipContext(eye, eye.add(player.getLookAngle().scale(range)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getLocation();
    }

    /** Существо, на которое смотрит игрок (EntityUtils.getPointedEntity оригинала): луч не проходит сквозь блоки. */
    @Nullable
    public static Entity getPointedEntity(Player player, double range, double padding) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(range));
        BlockHitResult block = player.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (block.getType() != HitResult.Type.MISS) {
            end = block.getLocation();
        }
        Entity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Entity entity : player.level().getEntities(player, new AABB(eye, end).inflate(1 + padding),
                e -> e.isPickable() && e.isAlive())) {
            AABB box = entity.getBoundingBox().inflate(Math.max(0.8f, entity.getPickRadius()) * padding / 2);
            Optional<Vec3> hit = box.clip(eye, end);
            double distance = box.contains(eye) ? 0 : hit.map(eye::distanceToSqr).orElse(Double.MAX_VALUE);
            if (distance < bestDistance) {
                best = entity;
                bestDistance = distance;
            }
        }
        return best;
    }
}
