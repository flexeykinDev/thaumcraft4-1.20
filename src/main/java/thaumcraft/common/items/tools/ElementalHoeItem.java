package thaumcraft.common.items.tools;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.network.FxPacket;
import thaumcraft.common.registry.ModBlocks;
import thaumcraft.common.registry.ModMaterials;
import thaumcraft.common.registry.ModSounds;

/**
 * Мотыга роста — перенос ItemElementalHoe оригинала. Вспахивает сразу девять блоков, а там, где пахать нечего,
 * действует как костная мука; саженцы великого и серебряного дерева она растит сразу, но это дорого ей обходится.
 */
public class ElementalHoeItem extends HoeItem {
    public ElementalHoeItem() {
        super(ModMaterials.ELEMENTAL_TOOL, -3, 0f, new Item.Properties().rarity(Rarity.RARE));
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return 5;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || player.isShiftKeyDown()) {
            return super.useOn(context);
        }
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();
        boolean did = false;
        for (int xx = -1; xx <= 1; xx++) {
            for (int zz = -1; zz <= 1; zz++) {
                BlockPos near = pos.offset(xx, 0, zz);
                if (stack.isEmpty()) {
                    break;
                }
                UseOnContext there = new UseOnContext(level, player, context.getHand(), stack,
                        new BlockHitResult(Vec3.atCenterOf(near), context.getClickedFace(), near, false));
                if (super.useOn(there).consumesAction()) {
                    if (level instanceof ServerLevel server) {
                        FxPacket.blockSparkle(server, near, 8401408, 2);
                    }
                    did = true;
                }
            }
        }
        if (did) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        BlockState state = level.getBlockState(pos);
        int left = stack.getMaxDamage() - stack.getDamageValue();
        boolean greatwood = state.is(ModBlocks.GREATWOOD_SAPLING.get());
        boolean silverwood = state.is(ModBlocks.SILVERWOOD_SAPLING.get());
        if (greatwood || silverwood) {
            // свои саженцы мотыга растит наверняка: великое дерево за 5 единиц прочности, серебряное за 25
            if (left < (greatwood ? 20 : 150)) {
                return InteractionResult.PASS;
            }
            if (level instanceof ServerLevel server) {
                ((SaplingBlock) state.getBlock()).advanceTree(server, pos, state, level.random);
                stack.hurtAndBreak(greatwood ? 5 : 25, player, broken -> broken.broadcastBreakEvent(context.getHand()));
                FxPacket.blockSparkle(server, pos, 0, 2);
            }
            did = true;
        } else if (BoneMealItem.applyBonemeal(new ItemStack(Items.BONE_MEAL), level, pos, player)) {
            if (level instanceof ServerLevel server) {
                stack.hurtAndBreak(1, player, broken -> broken.broadcastBreakEvent(context.getHand()));
                FxPacket.blockSparkle(server, pos, 0, 3);
                level.levelEvent(1505, pos, 0);
            }
            did = true;
        }
        if (did && !level.isClientSide) {
            level.playSound(null, pos, ModSounds.WAND.get(), SoundSource.PLAYERS, 0.75f, 0.9f + level.random.nextFloat() * 0.2f);
        }
        return did ? InteractionResult.sidedSuccess(level.isClientSide) : InteractionResult.PASS;
    }
}
