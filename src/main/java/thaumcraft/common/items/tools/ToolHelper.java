package thaumcraft.common.items.tools;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import thaumcraft.common.entity.FollowingItem;
import thaumcraft.common.registry.ModItems;

/** Общие приёмы волшебных инструментов — перенос нужных частей BlockUtils и Utils оригинала. */
public final class ToolHelper {
    private ToolHelper() {
    }

    /**
     * BlockUtils.harvestBlock: игрок ломает блок «на расстоянии» — с добычей, но без износа инструмента и без
     * повторного вызова его особых свойств. При follow выпавшие предметы сами летят к игроку; trail — вид их следа
     * (10 — пузыри топора, иначе искры этого цвета).
     */
    public static boolean harvestBlock(Level level, Player player, BlockPos pos, boolean follow, int trail) {
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.getDestroySpeed(level, pos) < 0f) {
            return false;
        }
        // событие Forge: чужие приваты и защита спавна запрещают ломать и таким способом
        int experience = ForgeHooks.onBlockBreakEvent(level, serverPlayer.gameMode.getGameModeForPlayer(), serverPlayer, pos);
        if (experience == -1) {
            return false;
        }
        level.levelEvent(2001, pos, Block.getId(state));
        BlockEntity entity = level.getBlockEntity(pos);
        if (player.getAbilities().instabuild) {
            if (state.onDestroyedByPlayer(level, pos, player, false, level.getFluidState(pos))) {
                state.getBlock().destroy(level, pos, state);
            }
            return true;
        }
        boolean canHarvest = state.canHarvestBlock(level, pos, player);
        if (state.onDestroyedByPlayer(level, pos, player, canHarvest, level.getFluidState(pos))) {
            state.getBlock().destroy(level, pos, state);
            if (canHarvest) {
                state.getBlock().playerDestroy(level, player, pos, state, entity, player.getMainHandItem().copy());
                if (experience > 0) {
                    state.getBlock().popExperience(server, pos, experience);
                }
                if (follow) {
                    for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2.0))) {
                        if (item.isAlive() && item.tickCount == 0 && !(item instanceof FollowingItem)) {
                            FollowingItem flying = new FollowingItem(level, item.getX(), item.getY(), item.getZ(), item.getItem().copy(),
                                    player, trail);
                            flying.setDeltaMovement(item.getDeltaMovement());
                            level.addFreshEntity(flying);
                            item.discard();
                        }
                    }
                }
            }
        }
        return true;
    }

    /**
     * Utils.findSpecialMiningResult: с вероятностью chance руда выпадает самородной гроздью. Списку оригинала
     * (железная и золотая руда, киноварь, медь) в 1.20 соответствуют куски сырого металла и сама киноварная руда.
     */
    public static ItemStack specialMiningResult(ItemStack drop, float chance, RandomSource random) {
        Item cluster = null;
        float weight = 1f;
        if (drop.is(Items.RAW_IRON)) {
            cluster = ModItems.CLUSTER_IRON.get();
        } else if (drop.is(Items.RAW_GOLD)) {
            cluster = ModItems.CLUSTER_GOLD.get();
            weight = 0.9f;
        } else if (drop.is(Items.RAW_COPPER)) {
            cluster = ModItems.CLUSTER_COPPER.get();
        } else if (drop.is(ModItems.CINNABAR_ORE.get()) || drop.is(ModItems.DEEPSLATE_CINNABAR_ORE.get())) {
            cluster = ModItems.CLUSTER_CINNABAR.get();
            weight = 0.9f;
        } else if (metal(drop, "tin")) {
            cluster = ModItems.CLUSTER_TIN.get();
        } else if (metal(drop, "silver")) {
            cluster = ModItems.CLUSTER_SILVER.get();
        } else if (metal(drop, "lead")) {
            cluster = ModItems.CLUSTER_LEAD.get();
        }
        if (cluster == null || random.nextFloat() > chance * weight) {
            return drop;
        }
        return new ItemStack(cluster, drop.getCount());
    }

    /** Руда или сырой металл олова, серебра или свинца из другого мода. */
    private static boolean metal(ItemStack drop, String metal) {
        return drop.is(thaumcraft.common.crafting.MetalCompat.tag("ores/" + metal))
                || drop.is(thaumcraft.common.crafting.MetalCompat.tag("raw_materials/" + metal));
    }

    /** Utils.resetFloatCounter: сервер не должен принимать полёт на волшебной тяге за взлом и выгонять игрока. */
    public static void resetFloatCounter(ServerPlayer player) {
        try {
            // aboveGroundTickCount
            ObfuscationReflectionHelper.setPrivateValue(ServerGamePacketListenerImpl.class, player.connection, 0, "f_9737_");
        } catch (RuntimeException ignored) {
            // счётчик недоступен — полёт просто останется под надзором сервера
        }
    }
}
