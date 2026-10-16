package thaumcraft.common.items;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import thaumcraft.common.registry.ModSounds;
import thaumcraft.common.research.PlayerKnowledge;
import thaumcraft.common.research.ScanManager;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Таумометр: зажать ПКМ, глядя на блок, предмет или существо, и удерживать чуть больше секунды.
 * Вся логика на сервере; взгляд нужно держать на одной и той же цели до конца.
 */
public class ThaumometerItem extends Item {
    public static final int SCAN_TICKS = 25;

    /** Что каждый игрок начал сканировать. Только сервер. */
    private static final Map<Player, String> SCANNING = new WeakHashMap<>();

    public ThaumometerItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return SCAN_TICKS;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide) {
            ScanManager.Target target = unscannedTarget(player);
            if (target != null) {
                SCANNING.put(player, target.key());
            } else {
                SCANNING.remove(player);
            }
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (level.isClientSide || !(entity instanceof ServerPlayer player)) {
            return;
        }
        String started = SCANNING.get(player);
        ScanManager.Target target = unscannedTarget(player);
        if (started == null || target == null || !started.equals(target.key())) {
            SCANNING.remove(player);
            return;
        }
        if (remaining <= 5) {
            SCANNING.remove(player);
            player.stopUsingItem();
            ScanManager.completeScan(player, target);
        } else if (remaining % 2 == 0) {
            player.playNotifySound(ModSounds.CAMERA_TICKS.get(), SoundSource.PLAYERS,
                    0.2f, 0.45f + level.random.nextFloat() * 0.1f);
            // руны, плывущие по изучаемому блоку (blockRunes оригинала)
            Vec3 pos = target.pos();
            thaumcraft.common.network.FxPacket.send((ServerLevel) level, thaumcraft.common.network.FxPacket.RUNES,
                    new Vec3(Math.floor(pos.x), Math.floor(pos.y), Math.floor(pos.z)), Vec3.ZERO, 2, 0);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        SCANNING.remove(entity);
    }

    @Nullable
    private static ScanManager.Target unscannedTarget(Player player) {
        ScanManager.Target target = ScanManager.findTarget(player);
        return target != null && !PlayerKnowledge.get(player).hasScanned(target.key()) ? target : null;
    }
}
