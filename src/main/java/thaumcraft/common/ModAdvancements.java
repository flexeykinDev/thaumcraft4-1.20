package thaumcraft.common;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import thaumcraft.Thaumcraft;

/**
 * Достижения-обучение (data/thaumcraft/advancements): то, что нельзя описать ванильными триггерами (первое изучение
 * таумометром, верстак из стола, вис из узла, первый ритуал), выдаётся отсюда — у таких достижений один критерий
 * «done» с триггером minecraft:impossible.
 */
public final class ModAdvancements {
    public static final String SCAN = "scan";
    public static final String RESEARCH_TABLE = "research_table";
    public static final String DISCOVERY = "discovery";
    public static final String ARCANE_WORKBENCH = "arcane_workbench";
    public static final String ARCANE_CRAFT = "arcane_craft";
    public static final String CRUCIBLE = "crucible";
    public static final String ALCHEMY = "alchemy";
    public static final String NODE = "node";
    public static final String INFUSION = "infusion";
    public static final String GOLEM = "golem";
    public static final String WARP = "warp";
    public static final String OCULUS = "oculus";

    private ModAdvancements() {
    }

    public static void grant(Player player, String name) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        Advancement advancement = serverPlayer.server.getAdvancements().getAdvancement(Thaumcraft.id(name));
        if (advancement == null) {
            return;
        }
        AdvancementProgress progress = serverPlayer.getAdvancements().getOrStartProgress(advancement);
        if (!progress.isDone()) {
            for (String criterion : progress.getRemainingCriteria()) {
                serverPlayer.getAdvancements().award(advancement, criterion);
            }
        }
    }
}
