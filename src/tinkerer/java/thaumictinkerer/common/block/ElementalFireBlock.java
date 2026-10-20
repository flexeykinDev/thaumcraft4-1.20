package thaumictinkerer.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import thaumictinkerer.common.registry.TTBlocks;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Стихийный огонь (BlockFireBase и его шесть видов в оригинале). Горит, только пока рядом есть «его» блоки, каждые
 * десять секунд с шансом превращает их по своей таблице (воздух — дерево в песок, земля — камень в землю, огонь —
 * землю в адский камень, вода — лаву в обсидиан, порядок — руду в блок металла, энтропия гасит прочие стихийные
 * огни) и перекидывается на соседние клетки возле таких блоков. Дождь его тушит.
 */
public class ElementalFireBlock extends Block {
    public enum Kind { AIR, FIRE, WATER, EARTH, ORDER, ENTROPY }

    private final Kind kind;
    private Map<Block, Block> table;
    /** Для огня порядка: что из какого блока сгущается (вычисляется по рецептам и кешируется). */
    private final Map<Block, Block> orderCache = new HashMap<>();

    public ElementalFireBlock(Kind kind) {
        super(BlockBehaviour.Properties.of().replaceable().noCollission().instabreak().lightLevel(state -> 15)
                .sound(net.minecraft.world.level.block.SoundType.WOOL).pushReaction(PushReaction.DESTROY).noLootTable());
        this.kind = kind;
    }

    private Map<Block, Block> table() {
        if (table == null) {
            Map<Block, Block> map = new HashMap<>();
            switch (kind) {
                case AIR -> {
                    for (Block log : List.of(Blocks.OAK_LOG, Blocks.SPRUCE_LOG, Blocks.BIRCH_LOG, Blocks.JUNGLE_LOG, Blocks.ACACIA_LOG,
                            Blocks.DARK_OAK_LOG, Blocks.MANGROVE_LOG, Blocks.CHERRY_LOG)) {
                        map.put(log, Blocks.SAND);
                    }
                    for (Block leaves : List.of(Blocks.OAK_LEAVES, Blocks.SPRUCE_LEAVES, Blocks.BIRCH_LEAVES, Blocks.JUNGLE_LEAVES,
                            Blocks.ACACIA_LEAVES, Blocks.DARK_OAK_LEAVES, Blocks.MANGROVE_LEAVES, Blocks.CHERRY_LEAVES)) {
                        map.put(leaves, Blocks.SANDSTONE);
                    }
                    map.put(Blocks.ICE, Blocks.GLASS);
                    map.put(Blocks.WATER, Blocks.CAKE);
                    map.put(Blocks.DIRT, Blocks.SAND);
                    map.put(Blocks.GRASS_BLOCK, Blocks.SAND);
                }
                case EARTH -> {
                    map.put(Blocks.SAND, Blocks.DIRT);
                    map.put(Blocks.GRAVEL, Blocks.CLAY);
                    map.put(Blocks.NETHER_BRICKS, Blocks.OAK_PLANKS);
                    map.put(Blocks.NETHER_BRICK_FENCE, Blocks.OAK_FENCE);
                    map.put(Blocks.NETHER_BRICK_STAIRS, Blocks.OAK_STAIRS);
                    map.put(Blocks.CACTUS, Blocks.OAK_LOG);
                    map.put(Blocks.SNOW, Blocks.GRASS_BLOCK);
                    map.put(Blocks.STONE, Blocks.DIRT);
                    map.put(Blocks.SPAWNER, Blocks.IRON_BLOCK);
                    map.put(Blocks.OAK_LOG, Blocks.DIRT);
                    map.put(Blocks.SPRUCE_LOG, Blocks.DIRT);
                    map.put(Blocks.BIRCH_LOG, Blocks.DIRT);
                    map.put(Blocks.OAK_LEAVES, Blocks.DIRT);
                    map.put(Blocks.COBBLESTONE, Blocks.DIRT);
                    map.put(Blocks.OAK_PLANKS, Blocks.DIRT);
                    map.put(Blocks.GLASS, Blocks.DIRT);
                }
                case FIRE -> {
                    map.put(Blocks.GRASS_BLOCK, Blocks.NETHERRACK);
                    map.put(Blocks.DIRT, Blocks.NETHERRACK);
                    map.put(Blocks.SAND, Blocks.SOUL_SAND);
                    map.put(Blocks.GRAVEL, Blocks.SOUL_SAND);
                    map.put(Blocks.CLAY, Blocks.GLOWSTONE);
                    for (Block ore : List.of(Blocks.COAL_ORE, Blocks.IRON_ORE, Blocks.DIAMOND_ORE, Blocks.EMERALD_ORE, Blocks.GOLD_BLOCK,
                            Blocks.LAPIS_ORE, Blocks.REDSTONE_ORE)) {
                        map.put(ore, Blocks.NETHER_QUARTZ_ORE);
                    }
                    map.put(Blocks.WATER, Blocks.LAVA);
                    map.put(Blocks.WHEAT, Blocks.NETHER_WART);
                    map.put(Blocks.POTATOES, Blocks.NETHER_WART);
                    map.put(Blocks.CARROTS, Blocks.NETHER_WART);
                    map.put(Blocks.POPPY, Blocks.BROWN_MUSHROOM);
                }
                case WATER -> {
                    map.put(Blocks.SAND, Blocks.ICE);
                    map.put(Blocks.NETHERRACK, Blocks.SNOW_BLOCK);
                    map.put(Blocks.SOUL_SAND, Blocks.ICE);
                    map.put(Blocks.GLOWSTONE, Blocks.ICE);
                    map.put(Blocks.LAVA, Blocks.OBSIDIAN);
                }
                case ENTROPY -> {
                    for (Supplier<Block> fire : List.<Supplier<Block>>of(TTBlocks.FIRE_AIR, TTBlocks.FIRE_WATER, TTBlocks.FIRE_EARTH,
                            TTBlocks.FIRE_IGNIS, TTBlocks.FIRE_ORDER)) {
                        map.put(fire.get(), Blocks.FIRE);
                    }
                }
                case ORDER -> {
                }
            }
            table = map;
        }
        return table;
    }

    /** Во что превратится блок; null — не цель этого огня. */
    @Nullable
    private Block result(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (kind == Kind.ORDER) {
            return state.isAir() || state.getBlock() instanceof ElementalFireBlock ? null : orderResult(level, pos, state);
        }
        return table().get(state.getBlock());
    }

    /** Огонь порядка: добыча блока, девятью уложенная в сетку крафта (или сначала переплавленная), даёт блок. */
    @Nullable
    private Block orderResult(Level level, BlockPos pos, BlockState state) {
        if (state.is(Blocks.STONE) || state.is(Blocks.GLASS) || state.is(Blocks.SAND) || state.is(Blocks.GRAVEL)
                || !(level instanceof ServerLevel server)) {
            return null;
        }
        Block block = state.getBlock();
        if (orderCache.containsKey(block)) {
            return orderCache.get(block);
        }
        Block out = null;
        List<ItemStack> drops = state.getDrops(new LootParams.Builder(server).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .withParameter(LootContextParams.TOOL, ItemStack.EMPTY));
        if (!drops.isEmpty() && drops.stream().allMatch(d -> ItemStack.isSameItemSameTags(d, drops.get(0)))) {
            // в 1.7 руда роняла саму себя и шла в печь; теперь она роняет сырьё, поэтому переплавка пробуется первой
            ItemStack smelted = server.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SimpleContainer(drops.get(0)), server)
                    .map(r -> r.getResultItem(server.registryAccess())).orElse(ItemStack.EMPTY);
            ItemStack result = smelted.isEmpty() ? ItemStack.EMPTY : nine(server, smelted);
            if (!(result.getItem() instanceof BlockItem)) {
                result = nine(server, drops.get(0));
            }
            if (result.getItem() instanceof BlockItem item && item.getBlock() != block) {
                out = item.getBlock();
            }
        }
        orderCache.put(block, out);
        return out;
    }

    private static ItemStack nine(ServerLevel level, ItemStack stack) {
        TransientCraftingContainer grid = new TransientCraftingContainer(new net.minecraft.world.inventory.AbstractContainerMenu(null, -1) {
            @Override
            public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player, int index) {
                return ItemStack.EMPTY;
            }

            @Override
            public boolean stillValid(net.minecraft.world.entity.player.Player player) {
                return false;
            }
        }, 3, 3);
        for (int i = 0; i < 9; i++) {
            grid.setItem(i, stack.copyWithCount(1));
        }
        return level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level).map(r -> r.assemble(grid, level.registryAccess()))
                .orElse(ItemStack.EMPTY);
    }

    /** Шанс, что блок станет результатом, а не просто сгорит: у огня порядка — по числу выпадающих вещей. */
    private int decayChance(Level level, BlockPos pos) {
        if (kind != Kind.ORDER || !(level instanceof ServerLevel server)) {
            return 1;
        }
        int drops = level.getBlockState(pos).getDrops(new LootParams.Builder(server)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos)).withParameter(LootContextParams.TOOL, ItemStack.EMPTY))
                .stream().mapToInt(ItemStack::getCount).sum();
        return drops == 2 ? 2 : drops >= 3 ? 1 : 3;
    }

    private boolean nearTarget(Level level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (result(level, pos.relative(direction)) != null) {
                return true;
            }
        }
        return false;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        level.scheduleTick(pos, this, 200 + level.random.nextInt(10));
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOFIRETICK)) {
            level.scheduleTick(pos, this, 200);
            return;
        }
        if (level.isRainingAt(pos) || !nearTarget(level, pos)) {
            level.removeBlock(pos, false);
            return;
        }
        // превращение соседних блоков
        if (random.nextInt(20) == 0) {
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                Block to = result(level, next);
                if (to != null) {
                    BlockState converted = random.nextInt(decayChance(level, next)) == 0 ? to.defaultBlockState() : Blocks.AIR.defaultBlockState();
                    if (to instanceof CropBlock crop && converted.getBlock() == crop) {
                        converted = crop.getStateForAge(0);
                    }
                    level.setBlock(next, converted, Block.UPDATE_ALL);
                }
            }
        }
        // огонь перекидывается на пустые клетки возле «своих» блоков
        for (BlockPos near : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 2, 1))) {
            if (!near.equals(pos) && level.isEmptyBlock(near) && random.nextInt(100) < 70 && nearTarget(level, near)) {
                level.setBlock(near.immutable(), defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        level.scheduleTick(pos, this, 200 + random.nextInt(3));
    }

    /** Жжёт, как обычный огонь. */
    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, net.minecraft.world.entity.Entity entity) {
        if (!entity.fireImmune()) {
            entity.setRemainingFireTicks(entity.getRemainingFireTicks() + 1);
            if (entity.getRemainingFireTicks() == 0) {
                entity.setSecondsOnFire(8);
            }
        }
        entity.hurt(level.damageSources().inFire(), 1f);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(24) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS,
                    1f + random.nextFloat(), random.nextFloat() * 0.7f + 0.3f, false);
        }
    }
}
