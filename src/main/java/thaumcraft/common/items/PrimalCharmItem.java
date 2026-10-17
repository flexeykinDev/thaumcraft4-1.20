package thaumcraft.common.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.entity.AspectOrbEntity;
import thaumcraft.common.network.SyncKnowledgePacket;
import thaumcraft.common.research.PlayerKnowledge;

import java.util.List;

/**
 * Изначальный талисман (ItemResource оригинала, вид 15). В инвентаре изредка роняет сферу первичного аспекта,
 * а однажды «вдохновляет» владельца — даёт подсказку к исследованию изначального фокуса. Подсказка в описании
 * временами меняется.
 */
public class PrimalCharmItem extends Item {
    private static final Aspect[] PRIMALS = {Aspect.AIR, Aspect.EARTH, Aspect.FIRE, Aspect.WATER, Aspect.ORDER, Aspect.ENTROPY};

    public PrimalCharmItem() {
        super(new Item.Properties());
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide) {
            return;
        }
        int roll = level.random.nextInt(20000);
        if (roll < 20) {
            AspectOrbEntity.spawn(level, entity.getX(), entity.getY(), entity.getZ(), PRIMALS[level.random.nextInt(6)], 1);
        } else if (roll == 42 && entity instanceof ServerPlayer player) {
            PlayerKnowledge knowledge = PlayerKnowledge.get(player);
            if (!knowledge.isResearchComplete("FOCUSPRIMAL") && knowledge.completeResearch("@FOCUSPRIMAL")) {
                player.sendSystemMessage(Component.translatable("tc.primalcharm.trigger")
                        .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
                SyncKnowledgePacket.send(player);
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        // одна и та же фраза держится шесть секунд, и показывается лишь иногда
        long tick = level == null ? 0 : level.getGameTime() / 120;
        RandomSource random = RandomSource.create(System.identityHashCode(stack) + tick);
        if (random.nextInt(200) < 25) {
            tooltip.add(Component.translatable("tc.primalcharm." + random.nextInt(5)).withStyle(ChatFormatting.GOLD));
        }
    }
}
