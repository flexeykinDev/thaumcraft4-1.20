package thaumcraft.common.items.tools;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.ForgeTier;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;
import thaumcraft.api.WarpingGear;
import thaumcraft.common.registry.ModItems;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Первозданный дробитель — перенос ItemPrimalCrusher оригинала: кирка и лопата разом, из первозданной жемчужины,
 * пустотных и стихийных инструментов. Ломает сразу квадрат 3×3 перед гранью, по которой ударили (крадучись — один
 * блок); добыча сама летит к хозяину. Сам чинится и даёт 2 единицы искажения.
 */
public class PrimalCrusherItem extends DiggerItem implements WarpingGear {
    /** Материал PRIMALVOID: уровень 5, прочность 500, скорость 8, урон +4, зачаровываемость 20. */
    public static final Tier TIER = new ForgeTier(5, 500, 8f, 4f, 20, net.minecraftforge.common.Tags.Blocks.NEEDS_NETHERITE_TOOL,
            () -> net.minecraft.world.item.crafting.Ingredient.of(ModItems.PRIMAL_CHARM.get()));
    /** Что дробитель берёт: всё, что берут кирка и лопата. */
    public static final TagKey<Block> MINEABLE = BlockTags.create(new ResourceLocation("thaumcraft", "mineable/primal_crusher"));
    private static final Map<UUID, Direction> SIDES = new HashMap<>();

    public PrimalCrusherItem() {
        super(3.5f, -2.8f, TIER, MINEABLE, new Item.Properties().rarity(Rarity.EPIC));
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ToolAction action) {
        return ToolActions.DEFAULT_PICKAXE_ACTIONS.contains(action) || ToolActions.DEFAULT_SHOVEL_ACTIONS.contains(action);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        VoidGear.selfRepair(stack, level, entity);
    }

    @Override
    public int getWarp(ItemStack stack, Player player) {
        return 2;
    }

    @Override
    public boolean onBlockStartBreak(ItemStack stack, BlockPos pos, Player player) {
        HitResult hit = getPlayerPOVHitResult(player.level(), player, ClipContext.Fluid.NONE);
        if (hit instanceof BlockHitResult block && hit.getType() == HitResult.Type.BLOCK) {
            SIDES.put(player.getUUID(), block.getDirection());
        }
        return super.onBlockStartBreak(stack, pos, player);
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
        if (miner.isShiftKeyDown() || level.isClientSide || !(miner instanceof Player player)) {
            return super.mineBlock(stack, level, state, pos, miner);
        }
        if (!state.is(MINEABLE)) {
            return true;
        }
        Direction side = SIDES.getOrDefault(player.getUUID(), Direction.UP);
        for (int aa = -1; aa <= 1; aa++) {
            for (int bb = -1; bb <= 1; bb++) {
                BlockPos near = switch (side.getAxis()) {
                    case Y -> pos.offset(aa, 0, bb);
                    case Z -> pos.offset(aa, bb, 0);
                    case X -> pos.offset(0, bb, aa);
                };
                if (stack.isEmpty() || !level.mayInteract(player, near)) {
                    continue;
                }
                BlockState there = level.getBlockState(near);
                if (there.getDestroySpeed(level, near) >= 0f && there.is(MINEABLE)) {
                    stack.hurtAndBreak(1, player, broken -> broken.broadcastBreakEvent(EquipmentSlot.MAINHAND));
                    ToolHelper.harvestBlock(level, player, near, true, 2);
                }
            }
        }
        return true;
    }
}
