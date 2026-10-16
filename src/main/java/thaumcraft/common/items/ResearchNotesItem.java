package thaumcraft.common.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import thaumcraft.common.research.PlayerKnowledge;
import thaumcraft.common.research.ResearchEntry;
import thaumcraft.common.research.ResearchHelper;
import thaumcraft.common.research.ResearchNote;
import thaumcraft.common.research.ResearchRegistry;

import java.util.List;

/**
 * Заметки об исследовании (ItemResearchNotes оригинала). Незавершённые решаются за столом исследований;
 * решённые становятся «открытием», и ПКМ по нему завершает исследование.
 */
public class ResearchNotesItem extends Item {

    public ResearchNotesItem() {
        super(new Item.Properties().stacksTo(1));
    }

    public static boolean isComplete(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean("complete");
    }

    /** «Неизвестное знание» (заметки оригинала с метой 42): щелчок превращает его в заметку по скрытому исследованию. */
    public static ItemStack unknown(Item item) {
        ItemStack stack = new ItemStack(item);
        stack.getOrCreateTag().putBoolean("unknown", true);
        return stack;
    }

    public static boolean isUnknown(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean("unknown");
    }

    /**
     * ResearchManager.findHiddenResearch оригинала: случайное скрытое исследование, которое игрок ещё не изучил,
     * но уже мог бы найти сам (выполнены условия, есть предметы или аспекты, наводящие на него).
     */
    @Nullable
    private static ResearchEntry findHiddenResearch(Player player) {
        PlayerKnowledge knowledge = PlayerKnowledge.get(player);
        List<ResearchEntry> found = new java.util.ArrayList<>();
        for (ResearchEntry entry : ResearchRegistry.entries()) {
            if (entry.hidden && !entry.aspects.isEmpty() && !knowledge.isResearchComplete(entry.key)
                    && ResearchHelper.canUnlock(knowledge, entry) && (!entry.itemTriggers.isEmpty() || !entry.aspectTriggers.isEmpty())) {
                found.add(entry);
            }
        }
        if (found.isEmpty()) {
            return null;
        }
        found.sort(java.util.Comparator.comparing(entry -> entry.key));
        return found.get(new java.util.Random(player.level().getGameTime() / 50L).nextInt(found.size()));
    }

    private static void reveal(ItemStack stack, Level level, Player player) {
        ResearchEntry entry = findHiddenResearch(player);
        ResearchNote note = entry == null ? null : ResearchNote.create(entry, level.random);
        if (note == null) {
            // раскрывать нечего: знание рассыпается обратно на 7–9 фрагментов
            stack.shrink(1);
            level.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(level, player.getX(), player.getY() + player.getEyeHeight() / 2f,
                    player.getZ(), new ItemStack(thaumcraft.common.registry.ModItems.KNOWLEDGE_FRAGMENT.get(), 7 + level.random.nextInt(3))));
            level.playSound(null, player, thaumcraft.common.registry.ModSounds.ERASE.get(), net.minecraft.sounds.SoundSource.PLAYERS, 0.75f, 1f);
        } else {
            stack.getTag().remove("unknown");
            note.write(stack);
            level.playSound(null, player, thaumcraft.common.registry.ModSounds.WRITE.get(), net.minecraft.sounds.SoundSource.PLAYERS, 0.75f, 1f);
        }
    }

    public static ItemStack create(Item item, ResearchNote note) {
        ItemStack stack = new ItemStack(item);
        note.write(stack);
        return stack;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (isUnknown(stack)) {
            if (!level.isClientSide) {
                reveal(stack, level, player);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        ResearchNote note = ResearchNote.read(stack);
        if (note == null || !note.complete) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            PlayerKnowledge knowledge = PlayerKnowledge.get(player);
            ResearchEntry research = ResearchRegistry.get(note.key);
            if (research != null && !knowledge.isResearchComplete(note.key) && ResearchHelper.canUnlock(knowledge, research)) {
                ResearchHelper.complete(serverPlayer, note.key);
                stack.shrink(1);
            } else {
                player.displayClientMessage(Component.translatable("tc.researcherror"), true);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(isComplete(stack) ? "item.discovery.name" : "item.researchnotes.name");
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (isUnknown(stack)) {
            tooltip.add(Component.translatable("item.researchnotes.unknown.1").withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.translatable("item.researchnotes.unknown.2").withStyle(ChatFormatting.BLUE));
        }
        ResearchNote note = ResearchNote.read(stack);
        ResearchEntry research = note == null ? null : ResearchRegistry.get(note.key);
        if (research != null) {
            tooltip.add(research.getName().copy().withStyle(ChatFormatting.GOLD));
            tooltip.add(research.getText().copy().withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));
        }
    }
}
