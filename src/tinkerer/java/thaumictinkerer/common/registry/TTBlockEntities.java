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
    public static final RegistryObject<BlockEntityType<thaumictinkerer.common.block.InfusedGrainBlock.Tile>> INFUSED_GRAIN = BLOCK_ENTITIES.register(
            "infused_grain", () -> BlockEntityType.Builder.of(thaumictinkerer.common.block.InfusedGrainBlock.Tile::new, TTBlocks.INFUSED_GRAIN.get()).build(null));
    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<thaumictinkerer.common.block.InfusedFarmlandBlock.Tile>> INFUSED_FARMLAND = BLOCK_ENTITIES.register(
            "infused_farmland", () -> BlockEntityType.Builder.of(thaumictinkerer.common.block.InfusedFarmlandBlock.Tile::new, TTBlocks.INFUSED_FARMLAND.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<thaumictinkerer.common.block.WarpGateBlock.Tile>> WARP_GATE = BLOCK_ENTITIES.register(
            "warp_gate", () -> BlockEntityType.Builder.of(thaumictinkerer.common.block.WarpGateBlock.Tile::new, TTBlocks.WARP_GATE.get()).build(null));

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

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<thaumictinkerer.common.block.MagnetBlock.Tile>> MAGNET = BLOCK_ENTITIES.register("magnet",
            () -> BlockEntityType.Builder.of(thaumictinkerer.common.block.MagnetBlock.Tile::new, TTBlocks.MAGNET.get(), TTBlocks.MOB_MAGNET.get()).build(null));
    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<thaumictinkerer.common.block.TransvectorBlock.Interface.Tile>> TRANSVECTOR_INTERFACE = BLOCK_ENTITIES.register(
            "transvector_interface", () -> BlockEntityType.Builder.of(thaumictinkerer.common.block.TransvectorBlock.Interface.Tile::new,
                    TTBlocks.TRANSVECTOR_INTERFACE.get()).build(null));
    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<thaumictinkerer.common.block.TransvectorBlock.Dislocator.Tile>> TRANSVECTOR_DISLOCATOR = BLOCK_ENTITIES.register(
            "transvector_dislocator", () -> BlockEntityType.Builder.of(thaumictinkerer.common.block.TransvectorBlock.Dislocator.Tile::new,
                    TTBlocks.TRANSVECTOR_DISLOCATOR.get()).build(null));
    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<thaumictinkerer.common.block.MobilizerBlock.Mobilizer>> MOBILIZER = BLOCK_ENTITIES.register("mobilizer",
            () -> BlockEntityType.Builder.of(thaumictinkerer.common.block.MobilizerBlock.Mobilizer::new, TTBlocks.MOBILIZER.get()).build(null));
    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<thaumictinkerer.common.block.MobilizerBlock.Relay>> RELAY = BLOCK_ENTITIES.register("mobilizer_relay",
            () -> BlockEntityType.Builder.of(thaumictinkerer.common.block.MobilizerBlock.Relay::new, TTBlocks.RELAY.get()).build(null));
    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<thaumictinkerer.common.block.RemotePlacerBlock.Tile>> REMOTE_PLACER = BLOCK_ENTITIES.register("remote_placer",
            () -> BlockEntityType.Builder.of(thaumictinkerer.common.block.RemotePlacerBlock.Tile::new, TTBlocks.REMOTE_PLACER.get()).build(null));
    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<thaumictinkerer.common.block.AnimationTabletBlock.Tile>> ANIMATION_TABLET = BLOCK_ENTITIES.register(
            "animation_tablet", () -> BlockEntityType.Builder.of(thaumictinkerer.common.block.AnimationTabletBlock.Tile::new, TTBlocks.ANIMATION_TABLET.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<thaumictinkerer.common.block.SummonBlock.Tile>> SUMMON = BLOCK_ENTITIES.register("summon",
            () -> BlockEntityType.Builder.of(thaumictinkerer.common.block.SummonBlock.Tile::new, TTBlocks.SUMMON.get()).build(null));

    private TTBlockEntities() {
    }
}
