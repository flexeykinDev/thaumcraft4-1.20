package thaumcraft.common.research;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.items.ResearchNotesItem;
import thaumcraft.common.items.ScribingToolsItem;
import thaumcraft.common.registry.ModItems;

import java.util.ArrayList;
import java.util.List;
import thaumcraft.common.network.SyncKnowledgePacket;
import thaumcraft.common.registry.ModSounds;

/** Правила открытия исследований — общие для сервера и книги на клиенте. */
public final class ResearchHelper {

    /** Все предпосылки (явные и скрытые) завершены. */
    /** ResearchManager.reduceToPrimals оригинала: составные аспекты раскладываются на первичные, количества складываются. */
    public static thaumcraft.api.aspects.AspectList reduceToPrimals(thaumcraft.api.aspects.AspectList aspects) {
        thaumcraft.api.aspects.AspectList out = new thaumcraft.api.aspects.AspectList();
        for (thaumcraft.api.aspects.Aspect aspect : aspects.getAspects()) {
            addPrimals(aspect, aspects.getAmount(aspect), out);
        }
        return out;
    }

    private static void addPrimals(thaumcraft.api.aspects.Aspect aspect, int amount, thaumcraft.api.aspects.AspectList out) {
        if (aspect.isPrimal()) {
            out.add(aspect, amount);
        } else {
            addPrimals(aspect.getFirstComponent(), amount, out);
            addPrimals(aspect.getSecondComponent(), amount, out);
        }
    }

    public static boolean canUnlock(PlayerKnowledge knowledge, ResearchEntry entry) {
        for (String parent : entry.parents) {
            if (ResearchRegistry.get(parent) != null && !knowledge.isResearchComplete(parent)) {
                return false;
            }
        }
        for (String parent : entry.parentsHidden) {
            if (ResearchRegistry.get(parent) != null && !knowledge.isResearchComplete(parent)) {
                return false;
            }
        }
        return true;
    }

    /** Хватает ли очков аспектов на покупку. */
    public static boolean canAfford(PlayerKnowledge knowledge, ResearchEntry entry) {
        for (Aspect aspect : entry.aspects.getAspects()) {
            if (knowledge.getPool(aspect) < entry.aspects.getAmount(aspect)) {
                return false;
            }
        }
        return true;
    }

    /** Исследования, которые известны каждому с самого начала. */
    public static boolean grantAutoUnlocks(PlayerKnowledge knowledge) {
        boolean changed = false;
        for (ResearchEntry entry : ResearchRegistry.entries()) {
            if (entry.autoUnlock && knowledge.completeResearch(entry.key)) {
                changed = true;
            }
        }
        return changed;
    }

    /** Покупка вторичного исследования за очки аспектов (нажатие в книге). */
    public static void purchase(ServerPlayer player, String key) {
        PlayerKnowledge knowledge = PlayerKnowledge.get(player);
        ResearchEntry entry = ResearchRegistry.get(key);
        if (entry == null || knowledge.isResearchComplete(key) || !entry.isPurchasable()
                || !canUnlock(knowledge, entry) || !canAfford(knowledge, entry)) {
            return;
        }
        for (Aspect aspect : entry.aspects.getAspects()) {
            knowledge.addPool(aspect, -entry.aspects.getAmount(aspect));
        }
        complete(player, key);
    }

    /** Завершает исследование и вместе с ним его «родственные», если у них выполнены предпосылки. */
    public static void complete(ServerPlayer player, String key) {
        PlayerKnowledge knowledge = PlayerKnowledge.get(player);
        ResearchEntry entry = ResearchRegistry.get(key);
        if (entry == null || !knowledge.completeResearch(key)) {
            return;
        }
        for (String sibling : entry.siblings) {
            ResearchEntry other = ResearchRegistry.get(sibling);
            if (other != null && !knowledge.isResearchComplete(sibling) && canUnlock(knowledge, other)) {
                knowledge.completeResearch(sibling);
            }
        }
        // запретные исследования навсегда оставляют искажение (addWarpToResearch оригинала)
        if (entry.warp > 0) {
            Warp.add(player, entry.warp, false);
        }
        player.playNotifySound(ModSounds.LEARN.get(), SoundSource.PLAYERS, 0.75f, 1f);
        SyncKnowledgePacket.send(player);
    }

    /**
     * Нажатие на первичное исследование в книге: игрок получает заметку для стола исследований.
     * Нужны лист бумаги и письменные принадлежности с чернилами; уже имеющаяся заметка не дублируется.
     */
    public static void requestNote(ServerPlayer player, String key) {
        PlayerKnowledge knowledge = PlayerKnowledge.get(player);
        ResearchEntry entry = ResearchRegistry.get(key);
        if (entry == null || knowledge.isResearchComplete(key) || entry.isPurchasable() || !canUnlock(knowledge, entry)
                || findNote(player, key) >= 0) {
            return;
        }
        int tools = findScribingTools(player);
        int paper = findPaper(player);
        if (entry.aspects.isEmpty()) {
            // заметку без аспектов не составить: обычно такие открываются сами вместе с родительским, а если это
            // почему-то не случилось — открываем по щелчку. Скрытые (культ, Древние) открываются только событием.
            if (!entry.hidden && !entry.lost && !entry.parents.isEmpty()) {
                complete(player, key);
            }
            return;
        }
        ResearchNote note = ResearchNote.create(entry, player.getRandom());
        if (tools < 0 || paper < 0 || note == null) {
            return;
        }
        player.getInventory().getItem(paper).shrink(1);
        ItemStack ink = player.getInventory().getItem(tools);
        ink.setDamageValue(ink.getDamageValue() + 1);
        ItemStack stack = ResearchNotesItem.create(ModItems.RESEARCH_NOTES.get(), note);
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        player.playNotifySound(ModSounds.LEARN.get(), SoundSource.PLAYERS, 0.75f, 1f);
    }

    /**
     * Слот с бумагой во всём инвентаре, включая вторую руку и переименованные листы, либо -1.
     * Inventory.findSlotMatchingItem смотрит только основные 36 ячеек и требует совпадения меток — бумага в левой руке
     * не находилась, и книга писала, что нужны письменные принадлежности.
     */
    public static int findPaper(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).is(Items.PAPER)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Добавляет очки аспектов в запас игрока, показывает всплывающие уведомления (как PacketAspectPool оригинала)
     * и отправляет знания клиенту — без этого книга и стол исследований не видели прибавки.
     */
    public static void giveAspects(ServerPlayer player, thaumcraft.api.aspects.AspectList aspects) {
        PlayerKnowledge knowledge = PlayerKnowledge.get(player);
        java.util.List<thaumcraft.common.network.NotificationPacket.Entry> notifications = new java.util.ArrayList<>();
        for (Aspect aspect : aspects.getAspects()) {
            int amount = aspects.getAmount(aspect);
            if (knowledge.discover(aspect)) {
                notifications.add(new thaumcraft.common.network.NotificationPacket.Entry(aspect,
                        net.minecraft.network.chat.Component.translatable("tc.addaspectdiscovery", aspect.getName())
                                .withStyle(net.minecraft.ChatFormatting.GOLD)));
            }
            knowledge.addPool(aspect, amount);
            notifications.add(new thaumcraft.common.network.NotificationPacket.Entry(aspect,
                    net.minecraft.network.chat.Component.translatable("tc.addaspectpool", amount, aspect.getName())));
        }
        thaumcraft.common.network.ModNetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                new thaumcraft.common.network.NotificationPacket(notifications));
        player.playNotifySound(ModSounds.LEARN.get(), SoundSource.PLAYERS, 0.5f, 1f);
        SyncKnowledgePacket.send(player);
    }

    /** Слот с заметкой по этому исследованию, либо -1. */
    public static int findNote(Player player, String key) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof ResearchNotesItem && stack.hasTag() && key.equals(stack.getTag().getString("key"))) {
                return i;
            }
        }
        return -1;
    }

    /** Слот с письменными принадлежностями, в которых остались чернила, либо -1. */
    public static int findScribingTools(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (ScribingToolsItem.hasInk(player.getInventory().getItem(i))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Подсказка от сканирования (createClue оригинала): если изученный предмет или один из его аспектов
     * значится у скрытого исследования, игрок узнаёт о нём, и оно появляется в книге.
     */
    public static boolean createClue(ServerPlayer player, String scanKey, AspectList aspects) {
        PlayerKnowledge knowledge = PlayerKnowledge.get(player);
        List<String> found = new ArrayList<>();
        for (ResearchEntry entry : ResearchRegistry.entries()) {
            if (entry.aspects.isEmpty() || !(entry.lost || entry.hidden) || knowledge.isResearchComplete(entry.key)
                    || knowledge.isResearchComplete("@" + entry.key)) {
                continue;
            }
            boolean match = false;
            for (ResourceLocation item : entry.itemTriggers) {
                if (scanKey.equals("item:" + item)) {
                    match = true;
                }
            }
            for (ResourceLocation entity : entry.entityTriggers) {
                // у ключа существа бывает уточнение после черты: детёныш, заряженный крипер
                if (scanKey.equals("entity:" + entity) || scanKey.startsWith("entity:" + entity + "|")) {
                    match = true;
                }
            }
            for (Aspect aspect : entry.aspectTriggers) {
                if (aspects.getAmount(aspect) > 0) {
                    match = true;
                }
            }
            if (match) {
                found.add(entry.key);
            }
        }
        if (found.isEmpty()) {
            return false;
        }
        knowledge.completeResearch("@" + found.get(player.getRandom().nextInt(found.size())));
        player.playNotifySound(ModSounds.LEARN.get(), SoundSource.PLAYERS, 0.75f, 1f);
        return true;
    }

    private ResearchHelper() {
    }
}
