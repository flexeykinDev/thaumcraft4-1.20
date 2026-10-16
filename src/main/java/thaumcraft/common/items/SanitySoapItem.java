package thaumcraft.common.items;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import thaumcraft.Thaumcraft;
import thaumcraft.client.fx.Fx;
import thaumcraft.common.registry.ModEffects;
import thaumcraft.common.registry.ModSounds;
import thaumcraft.common.research.PlayerKnowledge;
import thaumcraft.common.research.Warp;

/**
 * Мыло здравомыслия (ItemSanitySoap оригинала): десять секунд намыливания смывают всё временное искажение и
 * с шансом в треть — единицу «липкого». Шанс выше под защитой от искажения и стоя в очищающей жидкости.
 */
public class SanitySoapItem extends Item {
    /** Жидкости, в которых мыло действует сильнее (blockFluidPure оригинала). */
    public static final TagKey<Fluid> PURIFYING = TagKey.create(Registries.FLUID, Thaumcraft.id("purifying"));

    public SanitySoapItem() {
        super(new Item.Properties());
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 200;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BLOCK;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        if (getUseDuration(stack) - remaining > 195) {
            user.releaseUsingItem();
            return;
        }
        if (level.isClientSide) {
            if (level.random.nextFloat() < 0.2f) {
                level.playLocalSound(user.getX(), user.getY(), user.getZ(), ModSounds.ROOTS.get(), SoundSource.PLAYERS, 0.1f,
                        1.5f + level.random.nextFloat() * 0.2f, false);
            }
            for (int i = 0; i < 5; i++) {
                Fx.crucibleBubble(user.getX() - 0.5 + level.random.nextFloat(), user.getY() + level.random.nextFloat() * user.getBbHeight(),
                        user.getZ() - 0.5 + level.random.nextFloat(), 1f, 0.8f, 0.9f);
            }
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int timeLeft) {
        if (getUseDuration(stack) - timeLeft <= 195) {
            return;
        }
        stack.shrink(1);
        if (user instanceof ServerPlayer player) {
            float chance = 0.33f;
            if (player.hasEffect(ModEffects.WARP_WARD.get())) {
                chance += 0.25f;
            }
            if (level.getFluidState(BlockPos.containing(player.position())).is(PURIFYING)) {
                chance += 0.25f;
            }
            PlayerKnowledge knowledge = PlayerKnowledge.get(player);
            if (level.random.nextFloat() < chance && knowledge.getWarpSticky() > 0) {
                Warp.addSticky(player, -1);
            }
            if (knowledge.getWarpTemp() > 0) {
                Warp.add(player, -knowledge.getWarpTemp(), true);
            }
        } else if (level.isClientSide) {
            level.playLocalSound(user.getX(), user.getY(), user.getZ(), ModSounds.CRAFT_START.get(), SoundSource.PLAYERS, 0.25f, 1f, false);
            for (int i = 0; i < 20; i++) {
                Fx.crucibleBubble(user.getX() - 0.5 + level.random.nextFloat() * 1.5f,
                        user.getY() + level.random.nextFloat() * user.getBbHeight(),
                        user.getZ() - 0.5 + level.random.nextFloat() * 1.5f, 1f, 0.7f, 0.9f);
            }
        }
    }
}
