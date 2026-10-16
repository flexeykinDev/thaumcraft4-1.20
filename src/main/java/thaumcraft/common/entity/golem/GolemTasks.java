package thaumcraft.common.entity.golem;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import thaumcraft.common.items.golem.GolemCoreItem;

/** Поведения ядер голема: что он делает с тем или иным ядром и что происходит по щелчку по нему. */
public final class GolemTasks {
    private GolemTasks() {
    }

    /** Задачи ядра; страж и мясник заданы прямо в {@link Golem#setupGolem()}. */
    public static void addCoreGoals(Golem golem, int core) {
        switch (core) {
            case GolemCoreItem.FILL -> {
                golem.goalSelector.addGoal(0, new GolemCarryGoals.HomeReplace(golem));
                golem.goalSelector.addGoal(1, new GolemCarryGoals.HomePlace(golem));
                golem.goalSelector.addGoal(2, new GolemCarryGoals.HomeDrop(golem));
                golem.goalSelector.addGoal(3, new GolemCarryGoals.FillTake(golem));
                golem.goalSelector.addGoal(4, new GolemCarryGoals.FillGoto(golem));
            }
            case GolemCoreItem.EMPTY -> {
                golem.goalSelector.addGoal(0, new GolemCarryGoals.HomeReplace(golem));
                golem.goalSelector.addGoal(1, new GolemCarryGoals.EmptyPlace(golem));
                golem.goalSelector.addGoal(2, new GolemCarryGoals.EmptyDrop(golem));
                golem.goalSelector.addGoal(3, new GolemCarryGoals.EmptyGoto(golem));
                golem.goalSelector.addGoal(4, new GolemCarryGoals.HomeTake(golem));
            }
            case GolemCoreItem.GATHER -> {
                golem.goalSelector.addGoal(0, new GolemCarryGoals.HomeReplace(golem));
                golem.goalSelector.addGoal(1, new GolemCarryGoals.HomePlace(golem));
                golem.goalSelector.addGoal(2, new GolemCarryGoals.ItemPickup(golem));
            }
            case GolemCoreItem.HARVEST -> golem.goalSelector.addGoal(2, new GolemWorkGoals.HarvestCrops(golem));
            case GolemCoreItem.LUMBER -> golem.goalSelector.addGoal(2, new GolemWorkGoals.HarvestLogs(golem));
            case GolemCoreItem.FISHING -> golem.goalSelector.addGoal(2, new GolemFishGoal(golem));
            case GolemCoreItem.LIQUID -> {
                golem.goalSelector.addGoal(1, new GolemLiquidGoals.Empty(golem));
                golem.goalSelector.addGoal(2, new GolemLiquidGoals.Gather(golem));
                golem.goalSelector.addGoal(3, new GolemLiquidGoals.Goto(golem));
            }
            case GolemCoreItem.ALCHEMY -> {
                golem.goalSelector.addGoal(1, new GolemEssentiaGoals.Empty(golem));
                golem.goalSelector.addGoal(2, new GolemEssentiaGoals.Gather(golem));
                golem.goalSelector.addGoal(3, new GolemEssentiaGoals.Goto(golem));
            }
            case GolemCoreItem.SORTING -> {
                golem.goalSelector.addGoal(0, new GolemCarryGoals.HomeReplace(golem));
                golem.goalSelector.addGoal(1, new GolemSortGoals.Place(golem));
                golem.goalSelector.addGoal(3, new GolemSortGoals.Goto(golem));
                golem.goalSelector.addGoal(4, new GolemSortGoals.HomeTake(golem));
            }
            case GolemCoreItem.USE -> {
                golem.goalSelector.addGoal(0, new GolemCarryGoals.HomeReplace(golem));
                golem.goalSelector.addGoal(0, new GolemUseGoal(golem));
                golem.goalSelector.addGoal(4, new GolemCarryGoals.HomeTake(golem));
            }
            default -> {
            }
        }
    }

    /** Щелчок по голему пустой рукой или посторонним предметом. */
    public static InteractionResult interact(Golem golem, Player player, InteractionHand hand) {
        int core = golem.getCore();
        if (core < 0 || !GolemCoreItem.hasGUI(core) || player.getItemInHand(hand).getItem() instanceof thaumcraft.common.items.wands.WandItem
                || player.getItemInHand(hand).getItem() instanceof thaumcraft.common.items.golem.GolemBellItem) {
            return InteractionResult.PASS;
        }
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            net.minecraftforge.network.NetworkHooks.openScreen(serverPlayer, new net.minecraft.world.SimpleMenuProvider(
                    (id, inventory, opener) -> new thaumcraft.common.menu.GolemMenu(id, inventory, golem), golem.getDisplayName()),
                    buf -> buf.writeVarInt(golem.getId()));
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }
}
