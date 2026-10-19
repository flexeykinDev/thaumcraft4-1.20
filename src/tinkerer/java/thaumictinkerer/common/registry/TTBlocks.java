package thaumictinkerer.common.registry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import thaumictinkerer.ThaumicTinkerer;
import thaumictinkerer.common.block.AspectAnalyzerBlock;
import thaumictinkerer.common.block.FunnelBlock;
import thaumictinkerer.common.block.GasBlock;
import thaumictinkerer.common.block.PlatformBlock;
import thaumictinkerer.common.block.RepairerBlock;

/** Блоки Thaumic Tinkerer. */
public final class TTBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, ThaumicTinkerer.MODID);

    // закоптившийся кварц (BlockDarkQuartz оригинала: обычный, резной и столб — виды одного блока)
    private static BlockBehaviour.Properties quartz() {
        return BlockBehaviour.Properties.copy(Blocks.QUARTZ_BLOCK).mapColor(MapColor.COLOR_BLACK);
    }

    public static final RegistryObject<Block> DARK_QUARTZ_BLOCK = BLOCKS.register("dark_quartz_block", () -> new Block(quartz()));
    public static final RegistryObject<Block> CHISELED_DARK_QUARTZ_BLOCK = BLOCKS.register("chiseled_dark_quartz_block",
            () -> new Block(quartz()));
    public static final RegistryObject<Block> DARK_QUARTZ_PILLAR = BLOCKS.register("dark_quartz_pillar",
            () -> new RotatedPillarBlock(quartz()));
    public static final RegistryObject<Block> DARK_QUARTZ_SLAB = BLOCKS.register("dark_quartz_slab", () -> new SlabBlock(quartz()));
    public static final RegistryObject<Block> DARK_QUARTZ_STAIRS = BLOCKS.register("dark_quartz_stairs",
            () -> new StairBlock(() -> DARK_QUARTZ_BLOCK.get().defaultBlockState(), quartz()));

    // газы: светящийся, теневой и свет от гиперзаряженного нитора
    public static final RegistryObject<Block> GASEOUS_LIGHT = BLOCKS.register("gaseous_light",
            () -> new GasBlock(GasBlock.Kind.LIGHT, GasBlock.properties().lightLevel(state -> 12)));
    public static final RegistryObject<Block> GASEOUS_SHADOW = BLOCKS.register("gaseous_shadow",
            () -> new GasBlock(GasBlock.Kind.SHADOW, GasBlock.properties()));
    public static final RegistryObject<Block> NITOR_GAS = BLOCKS.register("nitor_gas",
            () -> new GasBlock(GasBlock.Kind.NITOR, GasBlock.properties().lightLevel(state -> 12)));

    public static final RegistryObject<Block> FUNNEL = BLOCKS.register("funnel", () -> new FunnelBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3f, 8f).sound(SoundType.STONE).noOcclusion()));
    public static final RegistryObject<Block> REPAIRER = BLOCKS.register("repairer", () -> new RepairerBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(3f, 8f).sound(SoundType.METAL).noOcclusion()));
    public static final RegistryObject<Block> ASPECT_ANALYZER = BLOCKS.register("aspect_analyzer", () -> new AspectAnalyzerBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2f, 5f).sound(SoundType.WOOD)));
    public static final RegistryObject<Block> ENCHANTER = BLOCKS.register("enchanter", () -> new thaumictinkerer.common.block.EnchanterBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(5f, 2000f).sound(SoundType.STONE).noOcclusion()));
    public static final RegistryObject<Block> PLATFORM = BLOCKS.register("platform", () -> new PlatformBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2f, 5f).sound(SoundType.WOOD).noOcclusion()
                    .isViewBlocking((state, level, pos) -> false).isSuffocating((state, level, pos) -> false)));

    private TTBlocks() {
    }
}
