package thaumictinkerer.common.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import thaumcraft.common.network.FxPacket;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

/**
 * Талисман удержания (ItemXPTalisman оригинала): включённый и надетый на шею, втягивает сферы опыта в радиусе трёх
 * блоков — до 1500 единиц. Щелчок с пустой склянкой в инвентаре разливает 10 единиц в пузырёк опыта;
 * Shift+щелчок включает и выключает поглощение.
 */
public class XpTalismanItem extends Item implements ICurioItem {
    public static final int CAPACITY = 1500;

    public XpTalismanItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    public static int getXp(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().getInt("xp") : 0;
    }

    public static void setXp(ItemStack stack, int xp) {
        stack.getOrCreateTag().putInt("xp", xp);
    }

    public static boolean isAbsorbing(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean("absorbing");
    }

    private static void setAbsorbing(ItemStack stack, boolean absorbing) {
        stack.getOrCreateTag().putBoolean("absorbing", absorbing);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (getXp(stack) < CAPACITY) {
                setAbsorbing(stack, !isAbsorbing(stack));
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP,
                        SoundSource.PLAYERS, 0.3f, 0.1f);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (getXp(stack) >= 10) {
            int bottle = player.getInventory().findSlotMatchingItem(new ItemStack(Items.GLASS_BOTTLE));
            if (bottle >= 0) {
                if (!level.isClientSide) {
                    player.getInventory().getItem(bottle).shrink(1);
                    ItemStack filled = new ItemStack(Items.EXPERIENCE_BOTTLE);
                    if (!player.getInventory().add(filled)) {
                        player.drop(filled, false);
                    }
                    setXp(stack, getXp(stack) - 10);
                    FxPacket.send((ServerLevel) level, FxPacket.SPARKLES, player.position().add(0, 1, 0), Vec3.ZERO, 3, 50);
                }
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP,
                        SoundSource.PLAYERS, 0.1f, (float) (0.1 + Math.random() / 2));
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            }
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void curioTick(SlotContext context, ItemStack stack) {
        LivingEntity wearer = context.entity();
        if (!isAbsorbing(stack) || !(wearer.level() instanceof ServerLevel level)) {
            return;
        }
        int stored = getXp(stack);
        int room = CAPACITY - stored;
        if (room <= 0) {
            setAbsorbing(stack, false);
            return;
        }
        int taken = 0;
        for (ExperienceOrb orb : level.getEntitiesOfClass(ExperienceOrb.class, wearer.getBoundingBox().inflate(3))) {
            if (orb.isAlive() && taken + orb.getValue() <= room) {
                taken += orb.getValue();
                level.playSound(null, orb.getX(), orb.getY(), orb.getZ(), thaumcraft.common.registry.ModSounds.ZAP.get(),
                        SoundSource.PLAYERS, orb.getValue() / 10f, 1f);
                FxPacket.send(level, FxPacket.SPARKLES, orb.position(), Vec3.ZERO, 3, 20);
                orb.discard();
            }
        }
        if (taken > 0) {
            setXp(stack, Math.min(CAPACITY, stored + taken));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("XP: " + getXp(stack)).withStyle(ChatFormatting.GRAY));
        String state = getXp(stack) >= CAPACITY ? "ttmisc.full" : isAbsorbing(stack) ? "ttmisc.absorbing" : "ttmisc.notAbsorbing";
        tooltip.add(Component.translatable(state).withStyle(ChatFormatting.GRAY));
    }
}
