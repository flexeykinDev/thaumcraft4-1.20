package thaumictinkerer.common.registry;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import thaumictinkerer.ThaumicTinkerer;
import thaumictinkerer.common.block.AspectAnalyzerBlock;
import thaumictinkerer.common.block.FunnelBlock;
import thaumictinkerer.common.block.PlatformBlock;
import thaumictinkerer.common.block.RepairerBlock;

/** Сущности блоков Thaumic Tinkerer. */
public final class TTBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ThaumicTinkerer.MODID);

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<FunnelBlock.Tile>> FUNNEL = BLOCK_ENTITIES.register("funnel",
            () -> BlockEntityType.Builder.of(FunnelBlock.Tile::new, TTBlocks.FUNNEL.get()).build(null));
    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<RepairerBlock.Tile>> REPAIRER = BLOCK_ENTITIES.register("repairer",
            () -> BlockEntityType.Builder.of(RepairerBlock.Tile::new, TTBlocks.REPAIRER.get()).build(null));
    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<AspectAnalyzerBlock.Tile>> ASPECT_ANALYZER = BLOCK_ENTITIES.register(
            "aspect_analyzer", () -> BlockEntityType.Builder.of(AspectAnalyzerBlock.Tile::new, TTBlocks.ASPECT_ANALYZER.get()).build(null));
    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<PlatformBlock.Tile>> PLATFORM = BLOCK_ENTITIES.register("platform",
            () -> BlockEntityType.Builder.of(PlatformBlock.Tile::new, TTBlocks.PLATFORM.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<thaumictinkerer.common.block.EnchanterBlock.Tile>> ENCHANTER = BLOCK_ENTITIES.register(
            "enchanter", () -> BlockEntityType.Builder.of(thaumictinkerer.common.block.EnchanterBlock.Tile::new, TTBlocks.ENCHANTER.get()).build(null));

    private TTBlockEntities() {
    }
}
