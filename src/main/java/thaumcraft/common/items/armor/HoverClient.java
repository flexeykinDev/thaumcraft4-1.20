package thaumcraft.common.items.armor;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import thaumcraft.common.registry.ModSounds;

/** Клиентская часть парения: потрескивание упряжи раз в 1.2 секунды. */
final class HoverClient {
    private static long next;

    private HoverClient() {
    }

    static void tick(Player player) {
        long now = System.currentTimeMillis();
        if (next < now) {
            next = now + 1200L;
            player.level().playLocalSound(player.getX(), player.getY(), player.getZ(), ModSounds.JACOBS.get(), SoundSource.PLAYERS, 0.05f,
                    1f + player.getRandom().nextFloat() * 0.05f, false);
        }
    }
}
