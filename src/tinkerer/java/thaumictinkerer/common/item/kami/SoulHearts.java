package thaumictinkerer.common.item.kami;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thaumictinkerer.ThaumicTinkerer;
import thaumictinkerer.common.network.TTNetwork;

/**
 * Сердца души (SoulHeartHandler): до 20 единиц здоровья сверх обычного, которые даёт пробуждённый меч в режиме души.
 * Они первыми принимают на себя урон и пропадают; видны металлическими сердцами над полоской сытости.
 */
@Mod.EventBusSubscriber(modid = ThaumicTinkerer.MODID)
public final class SoulHearts {
    public static final int MAX = 20;
    private static final String COMPOUND = "ThaumicTinkerer";
    private static final String TAG = "soulHearts";
    /** Сколько сердец у игрока этого клиента (приходит пакетом). */
    public static int clientHearts;

    private SoulHearts() {
    }

    private static CompoundTag data(Player player) {
        CompoundTag root = player.getPersistentData();
        if (!root.contains(COMPOUND)) {
            root.put(COMPOUND, new CompoundTag());
        }
        return root.getCompound(COMPOUND);
    }

    public static int get(Player player) {
        return data(player).getInt(TAG);
    }

    public static void set(Player player, int hearts) {
        data(player).putInt(TAG, Math.max(0, Math.min(MAX, hearts)));
        if (player instanceof ServerPlayer server) {
            TTNetwork.sendSoulHearts(server, get(player));
        }
    }

    public static void add(Player player, int hearts) {
        if (get(player) < MAX) {
            set(player, get(player) + hearts);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide && event.getAmount() > 0) {
            int stored = get(player);
            if (stored > 0) {
                int taken = Math.min(stored, (int) Math.ceil(event.getAmount()));
                event.setAmount(Math.max(0, event.getAmount() - taken));
                set(player, stored - taken);
            }
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TTNetwork.sendSoulHearts(player, get(player));
            TTNetwork.sendArmorStatus(player, IchorArmor.enabled(player));
        }
    }
}
