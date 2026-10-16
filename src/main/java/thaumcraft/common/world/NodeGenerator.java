package thaumcraft.common.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.nodes.NodeModifier;
import thaumcraft.api.nodes.NodeType;
import thaumcraft.common.blockentity.NodeBlockEntity;
import thaumcraft.common.registry.ModBlocks;

import java.util.List;

/** Создание узлов ауры — перенос createRandomNodeAt из ThaumcraftWorldGenerator оригинала. */
public final class NodeGenerator {
    /** Config.specialNodeRarity оригинала: раз в столько узлов выпадает особый тип. */
    private static final int SPECIAL_NODE_RARITY = 18;

    /**
     * Ставит свободный узел со случайными свойствами.
     *
     * @param small узел вчетверо слабее обычного
     */
    public static boolean createRandomNode(LevelAccessor level, BlockPos pos, RandomSource random, boolean small) {
        return createRandomNode(level, pos, random, small, false);
    }

    /** @param eerie узел тёмный — такие стоят у курганов, тотемов и каменных кругов */
    public static boolean createRandomNode(LevelAccessor level, BlockPos pos, RandomSource random, boolean small, boolean eerie) {
        level.setBlock(pos, ModBlocks.AURA_NODE.get().defaultBlockState(), 3);
        return fill(level, pos, random, false, eerie, small);
    }

    /** Заполняет уже поставленный блок узла (свободный узел или сучок серебряного дерева). */
    public static boolean fill(LevelAccessor level, BlockPos pos, RandomSource random, boolean silverwood, boolean small) {
        return fill(level, pos, random, silverwood, false, small);
    }

    public static boolean fill(LevelAccessor level, BlockPos pos, RandomSource random, boolean silverwood, boolean eerie, boolean small) {
        if (!(level.getBlockEntity(pos) instanceof NodeBlockEntity node)) {
            return false;
        }
        NodeType type = NodeType.NORMAL;
        if (silverwood) {
            type = NodeType.PURE;
        } else if (eerie) {
            type = NodeType.DARK;
        } else if (random.nextInt(SPECIAL_NODE_RARITY) == 0) {
            int roll = random.nextInt(10);
            type = roll < 3 ? NodeType.DARK : roll < 6 ? NodeType.UNSTABLE : roll < 9 ? NodeType.PURE : NodeType.HUNGRY;
        }
        NodeModifier modifier = null;
        if (random.nextInt(SPECIAL_NODE_RARITY / 2) == 0) {
            modifier = NodeModifier.values()[random.nextInt(3)];
        }
        Holder<Biome> biome = level.getBiome(pos);
        int aura = BiomeAura.getAura(biome);
        // в порченых землях узлы в полтора раза сильнее, а каждый второй ещё и заражён — и от этого сильнее ещё в полтора
        if (type != NodeType.PURE && biome.is(thaumcraft.common.taint.Taint.BIOME)) {
            aura = (int) (aura * 1.5f);
            if (random.nextBoolean()) {
                type = NodeType.TAINTED;
                aura = (int) (aura * 1.5f);
            }
        }
        if (silverwood || small) {
            aura /= 4;
        }
        int value = random.nextInt(Math.max(1, aura / 2)) + aura / 2;

        List<Aspect> primals = Aspect.primals();
        List<Aspect> compounds = Aspect.compounds();
        AspectList weights = new AspectList();
        Aspect biomeAspect = BiomeAura.getRandomAspect(biome, random);
        if (biomeAspect != null) {
            weights.add(biomeAspect, 2);
        } else {
            weights.add(compounds.get(random.nextInt(compounds.size())), 1);
            weights.add(primals.get(random.nextInt(primals.size())), 1);
        }
        for (int i = 0; i < 3; i++) {
            if (random.nextBoolean()) {
                if (random.nextInt(SPECIAL_NODE_RARITY) == 0) {
                    weights.merge(compounds.get(random.nextInt(compounds.size())), 1);
                } else {
                    weights.merge(primals.get(random.nextInt(primals.size())), 1);
                }
            }
        }
        if (type == NodeType.HUNGRY) {
            weights.merge(Aspect.HUNGER, 2);
            if (random.nextBoolean()) {
                weights.merge(Aspect.GREED, 1);
            }
        } else if (type == NodeType.PURE) {
            weights.merge(random.nextBoolean() ? Aspect.LIFE : Aspect.ORDER, 2);
        } else if (type == NodeType.DARK) {
            for (Aspect aspect : new Aspect[]{Aspect.DEATH, Aspect.UNDEAD, Aspect.ENTROPY, Aspect.DARKNESS}) {
                if (random.nextBoolean()) {
                    weights.merge(aspect, 1);
                }
            }
        }
        addSurroundingAspects(level, pos, weights);

        // общий запас делится между аспектами: главный аспект получает долю побольше
        List<Aspect> sorted = weights.getAspectsSortedByName();
        int[] spread = new int[sorted.size()];
        float total = 0f;
        for (int i = 0; i < spread.length; i++) {
            spread[i] = weights.getAmount(sorted.get(i)) == 2 ? 50 + random.nextInt(25) : 25 + random.nextInt(50);
            total += spread[i];
        }
        AspectList aspects = new AspectList();
        for (int i = 0; i < spread.length; i++) {
            aspects.add(sorted.get(i), Math.max(weights.getAmount(sorted.get(i)), (int) (spread[i] / total * value)));
        }
        setNode(node, type, modifier, aspects);
        return true;
    }

    public static void setNode(NodeBlockEntity node, NodeType type, @Nullable NodeModifier modifier, AspectList aspects) {
        node.setNodeType(type);
        node.setNodeModifier(modifier);
        node.setAspects(aspects);
    }

    /** Узел перенимает стихии окружения: рядом с водой — вода, с лавой — огонь и земля, и так далее. */
    private static void addSurroundingAspects(LevelAccessor level, BlockPos pos, AspectList weights) {
        int water = 0;
        int lava = 0;
        int stone = 0;
        int foliage = 0;
        for (BlockPos near : BlockPos.betweenClosed(pos.offset(-5, -5, -5), pos.offset(5, 5, 5))) {
            if (!level.hasChunkAt(near)) {
                continue;
            }
            BlockState state = level.getBlockState(near);
            if (state.getFluidState().is(FluidTags.WATER)) {
                water++;
            } else if (state.getFluidState().is(FluidTags.LAVA)) {
                lava++;
            } else if (state.is(Blocks.STONE)) {
                stone++;
            }
            if (state.is(BlockTags.LEAVES)) {
                foliage++;
            }
        }
        if (water > 100) {
            weights.merge(Aspect.WATER, 1);
        }
        if (lava > 100) {
            weights.merge(Aspect.FIRE, 1);
            weights.merge(Aspect.EARTH, 1);
        }
        if (stone > 500) {
            weights.merge(Aspect.EARTH, 1);
        }
        if (foliage > 100) {
            weights.merge(Aspect.PLANT, 1);
        }
    }

    private NodeGenerator() {
    }
}
