package thaumcraft.common.registry;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import thaumcraft.Thaumcraft;
import thaumcraft.common.blockentity.AlchemicalFurnaceBlockEntity;
import thaumcraft.common.blockentity.ArcaneWorkbenchBlockEntity;
import thaumcraft.common.blockentity.EssentiaBlockEntity;
import thaumcraft.common.blockentity.CrucibleBlockEntity;
import thaumcraft.common.blockentity.InfusionMatrixBlockEntity;
import thaumcraft.common.blockentity.NodeBlockEntity;
import thaumcraft.common.blockentity.PedestalBlockEntity;
import thaumcraft.common.blockentity.ResearchTableBlockEntity;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Thaumcraft.MODID);

    public static final RegistryObject<BlockEntityType<NodeBlockEntity>> NODE = BLOCK_ENTITIES.register("node",
            () -> BlockEntityType.Builder.of(NodeBlockEntity::new,
                    ModBlocks.AURA_NODE.get(), ModBlocks.SILVERWOOD_KNOT.get(),
                    ModBlocks.CHARGED_OBSIDIAN_TOTEM.get()).build(null));

    public static final RegistryObject<BlockEntityType<ArcaneWorkbenchBlockEntity>> ARCANE_WORKBENCH =
            BLOCK_ENTITIES.register("arcane_workbench", () -> BlockEntityType.Builder.of(
                    ArcaneWorkbenchBlockEntity::new, ModBlocks.ARCANE_WORKBENCH.get()).build(null));

    public static final RegistryObject<BlockEntityType<ResearchTableBlockEntity>> RESEARCH_TABLE =
            BLOCK_ENTITIES.register("research_table", () -> BlockEntityType.Builder.of(
                    ResearchTableBlockEntity::new, ModBlocks.RESEARCH_TABLE.get()).build(null));

    public static final RegistryObject<BlockEntityType<CrucibleBlockEntity>> CRUCIBLE =
            BLOCK_ENTITIES.register("crucible", () -> BlockEntityType.Builder.of(
                    CrucibleBlockEntity::new, ModBlocks.CRUCIBLE.get()).build(null));

    public static final RegistryObject<BlockEntityType<AlchemicalFurnaceBlockEntity>> ALCHEMICAL_FURNACE =
            BLOCK_ENTITIES.register("alchemical_furnace", () -> BlockEntityType.Builder.of(
                    AlchemicalFurnaceBlockEntity::new, ModBlocks.ALCHEMICAL_FURNACE.get()).build(null));

    public static final RegistryObject<BlockEntityType<EssentiaBlockEntity>> ALEMBIC = BLOCK_ENTITIES.register("alembic",
            () -> BlockEntityType.Builder.of(EssentiaBlockEntity::alembic, ModBlocks.ALEMBIC.get()).build(null));

    public static final RegistryObject<BlockEntityType<EssentiaBlockEntity>> JAR = BLOCK_ENTITIES.register("warded_jar",
            () -> BlockEntityType.Builder.of(EssentiaBlockEntity::jar, ModBlocks.WARDED_JAR.get(), ModBlocks.VOID_JAR.get()).build(null));

    public static final RegistryObject<BlockEntityType<PedestalBlockEntity>> PEDESTAL = BLOCK_ENTITIES.register("pedestal",
            () -> BlockEntityType.Builder.of(PedestalBlockEntity::new, ModBlocks.PEDESTAL.get()).build(null));

    public static final RegistryObject<BlockEntityType<InfusionMatrixBlockEntity>> INFUSION_MATRIX =
            BLOCK_ENTITIES.register("infusion_matrix", () -> BlockEntityType.Builder.of(
                    InfusionMatrixBlockEntity::new, ModBlocks.INFUSION_MATRIX.get()).build(null));

    public static final RegistryObject<BlockEntityType<thaumcraft.common.blocks.NitorBlock.Flame>> NITOR =
            BLOCK_ENTITIES.register("nitor", () -> BlockEntityType.Builder.of(
                    thaumcraft.common.blocks.NitorBlock.Flame::new, ModBlocks.NITOR.get()).build(null));

    public static final RegistryObject<BlockEntityType<thaumcraft.common.blockentity.TubeBlockEntity>> TUBE =
            BLOCK_ENTITIES.register("tube", () -> BlockEntityType.Builder.of(thaumcraft.common.blockentity.TubeBlockEntity::new,
                    ModBlocks.TUBE.get(), ModBlocks.TUBE_VALVE.get(), ModBlocks.TUBE_FILTER.get(), ModBlocks.TUBE_RESTRICT.get(),
                    ModBlocks.TUBE_ONEWAY.get()).build(null));

    public static final RegistryObject<BlockEntityType<thaumcraft.common.blockentity.EssentiaBufferBlockEntity>> ESSENTIA_BUFFER =
            BLOCK_ENTITIES.register("essentia_buffer", () -> BlockEntityType.Builder.of(
                    thaumcraft.common.blockentity.EssentiaBufferBlockEntity::new, ModBlocks.TUBE_BUFFER.get()).build(null));

    public static final RegistryObject<BlockEntityType<thaumcraft.common.blockentity.CentrifugeBlockEntity>> CENTRIFUGE =
            BLOCK_ENTITIES.register("centrifuge", () -> BlockEntityType.Builder.of(
                    thaumcraft.common.blockentity.CentrifugeBlockEntity::new, ModBlocks.CENTRIFUGE.get()).build(null));

    public static final RegistryObject<BlockEntityType<thaumcraft.common.blocks.BellowsBlock.Bellows>> BELLOWS =
            BLOCK_ENTITIES.register("bellows", () -> BlockEntityType.Builder.of(
                    thaumcraft.common.blocks.BellowsBlock.Bellows::new, ModBlocks.BELLOWS.get()).build(null));

    public static final RegistryObject<BlockEntityType<thaumcraft.common.blocks.NodeStabilizerBlock.Stabilizer>> NODE_STABILIZER =
            BLOCK_ENTITIES.register("node_stabilizer", () -> BlockEntityType.Builder.of(
                    thaumcraft.common.blocks.NodeStabilizerBlock.Stabilizer::new, ModBlocks.NODE_STABILIZER.get(),
                    ModBlocks.ADVANCED_NODE_STABILIZER.get()).build(null));

    public static final RegistryObject<BlockEntityType<thaumcraft.common.blocks.NodeTransducerBlock.Transducer>> NODE_TRANSDUCER =
            BLOCK_ENTITIES.register("node_transducer", () -> BlockEntityType.Builder.of(
                    thaumcraft.common.blocks.NodeTransducerBlock.Transducer::new, ModBlocks.NODE_TRANSDUCER.get()).build(null));

    public static final RegistryObject<BlockEntityType<thaumcraft.common.blocks.VisRelayBlock.Relay>> VIS_RELAY =
            BLOCK_ENTITIES.register("vis_relay", () -> BlockEntityType.Builder.of(
                    thaumcraft.common.blocks.VisRelayBlock.Relay::new, ModBlocks.VIS_RELAY.get()).build(null));

    public static final RegistryObject<BlockEntityType<thaumcraft.common.blocks.FocalManipulatorBlock.Manipulator>> FOCAL_MANIPULATOR =
            BLOCK_ENTITIES.register("focal_manipulator", () -> BlockEntityType.Builder.of(
                    thaumcraft.common.blocks.FocalManipulatorBlock.Manipulator::new, ModBlocks.FOCAL_MANIPULATOR.get()).build(null));

    public static final RegistryObject<BlockEntityType<thaumcraft.common.blocks.WardedBlock.Warded>> WARDED =
            BLOCK_ENTITIES.register("warded", () -> BlockEntityType.Builder.of(
                    thaumcraft.common.blocks.WardedBlock.Warded::new, ModBlocks.WARDED.get()).build(null));

    public static final RegistryObject<BlockEntityType<thaumcraft.common.blocks.HoleBlock.Hole>> HOLE =
            BLOCK_ENTITIES.register("hole", () -> BlockEntityType.Builder.of(
                    thaumcraft.common.blocks.HoleBlock.Hole::new, ModBlocks.HOLE.get()).build(null));

    public static final RegistryObject<BlockEntityType<thaumcraft.common.blocks.CrystallizerBlock.Crystallizer>> CRYSTALLIZER =
            BLOCK_ENTITIES.register("crystallizer", () -> BlockEntityType.Builder.of(
                    thaumcraft.common.blocks.CrystallizerBlock.Crystallizer::new, ModBlocks.CRYSTALLIZER.get()).build(null));

    public static final RegistryObject<BlockEntityType<thaumcraft.common.blocks.EtherealBloomBlock.Bloom>> ETHEREAL_BLOOM =
            BLOCK_ENTITIES.register("ethereal_bloom", () -> BlockEntityType.Builder.of(
                    thaumcraft.common.blocks.EtherealBloomBlock.Bloom::new, ModBlocks.ETHEREAL_BLOOM.get()).build(null));

    public static final RegistryObject<BlockEntityType<thaumcraft.common.blocks.ArcaneSpaBlock.Spa>> ARCANE_SPA =
            BLOCK_ENTITIES.register("arcane_spa", () -> BlockEntityType.Builder.of(
                    thaumcraft.common.blocks.ArcaneSpaBlock.Spa::new, ModBlocks.ARCANE_SPA.get()).build(null));

    public static final RegistryObject<BlockEntityType<thaumcraft.common.blocks.DeconstructionTableBlock.Table>> DECONSTRUCTION_TABLE =
            BLOCK_ENTITIES.register("deconstruction_table", () -> BlockEntityType.Builder.of(
                    thaumcraft.common.blocks.DeconstructionTableBlock.Table::new, ModBlocks.DECONSTRUCTION_TABLE.get()).build(null));

    public static final RegistryObject<BlockEntityType<thaumcraft.common.blocks.ThaumatoriumBlock.Core>> THAUMATORIUM =
            BLOCK_ENTITIES.register("thaumatorium", () -> BlockEntityType.Builder.of(
                    thaumcraft.common.blocks.ThaumatoriumBlock.Core::new, ModBlocks.THAUMATORIUM.get()).build(null));
    public static final RegistryObject<BlockEntityType<thaumcraft.common.blocks.ThaumatoriumBlock.Top>> THAUMATORIUM_TOP =
            BLOCK_ENTITIES.register("thaumatorium_top", () -> BlockEntityType.Builder.of(
                    thaumcraft.common.blocks.ThaumatoriumBlock.Top::new, ModBlocks.THAUMATORIUM.get()).build(null));

    public static final RegistryObject<BlockEntityType<thaumcraft.common.blocks.ArcaneLampBlock.Lamp>> ARCANE_LAMP =
            BLOCK_ENTITIES.register("arcane_lamp", () -> BlockEntityType.Builder.of(
                    thaumcraft.common.blocks.ArcaneLampBlock.Lamp::new, ModBlocks.ARCANE_LAMP.get()).build(null));

    public static final RegistryObject<BlockEntityType<thaumcraft.common.blocks.EssentiaLampBlock.Lamp>> ESSENTIA_LAMP =
            BLOCK_ENTITIES.register("essentia_lamp", () -> BlockEntityType.Builder.of(
                    thaumcraft.common.blocks.EssentiaLampBlock.Lamp::new, ModBlocks.LAMP_GROWTH.get(), ModBlocks.LAMP_FERTILITY.get())
                    .build(null));

    public static final RegistryObject<BlockEntityType<thaumcraft.common.blocks.PavingStoneBlock.Stone>> WARDING_STONE =
            BLOCK_ENTITIES.register("warding_stone", () -> BlockEntityType.Builder.of(
                    thaumcraft.common.blocks.PavingStoneBlock.Stone::new, ModBlocks.WARDING_PAVING.get()).build(null));

    public static final RegistryObject<BlockEntityType<thaumcraft.common.blocks.ArcaneEarBlock.Sensor>> ARCANE_EAR =
            BLOCK_ENTITIES.register("arcane_ear", () -> BlockEntityType.Builder.of(
                    thaumcraft.common.blocks.ArcaneEarBlock.Sensor::new, ModBlocks.ARCANE_EAR.get()).build(null));

    public static final RegistryObject<BlockEntityType<thaumcraft.common.blocks.LevitatorBlock.Lifter>> LEVITATOR =
            BLOCK_ENTITIES.register("levitator", () -> BlockEntityType.Builder.of(
                    thaumcraft.common.blocks.LevitatorBlock.Lifter::new, ModBlocks.LEVITATOR.get()).build(null));

    public static final RegistryObject<BlockEntityType<thaumcraft.common.blocks.MirrorBlock.Mirror>> MIRROR =
            BLOCK_ENTITIES.register("mirror", () -> BlockEntityType.Builder.of(
                    thaumcraft.common.blocks.MirrorBlock.Mirror::new, ModBlocks.MAGIC_MIRROR.get(), ModBlocks.ESSENTIA_MIRROR.get()).build(null));

    public static final RegistryObject<BlockEntityType<thaumcraft.common.blocks.BrainJarBlock.Brain>> BRAIN_JAR =
            BLOCK_ENTITIES.register("brain_jar", () -> BlockEntityType.Builder.of(
                    thaumcraft.common.blocks.BrainJarBlock.Brain::new, ModBlocks.BRAIN_JAR.get()).build(null));

    public static final RegistryObject<BlockEntityType<thaumcraft.common.blockentity.OwnedBlockEntity>> OWNED =
            BLOCK_ENTITIES.register("owned", () -> BlockEntityType.Builder.of(
                    thaumcraft.common.blockentity.OwnedBlockEntity::new, ModBlocks.ARCANE_DOOR.get(), ModBlocks.ARCANE_PRESSURE_PLATE.get())
                    .build(null));

    public static final RegistryObject<BlockEntityType<thaumcraft.common.blocks.FluxScrubberBlock.Scrubber>> FLUX_SCRUBBER =
            BLOCK_ENTITIES.register("flux_scrubber", () -> BlockEntityType.Builder.of(
                    thaumcraft.common.blocks.FluxScrubberBlock.Scrubber::new, ModBlocks.FLUX_SCRUBBER.get()).build(null));

    public static final RegistryObject<BlockEntityType<thaumcraft.common.blocks.ThaumBannerBlock.Banner>> THAUM_BANNER =
            BLOCK_ENTITIES.register("thaum_banner", () -> BlockEntityType.Builder.of(
                    thaumcraft.common.blocks.ThaumBannerBlock.Banner::new, ModBlocks.THAUM_BANNER.get()).build(null));

    public static final RegistryObject<BlockEntityType<thaumcraft.common.blocks.ArcaneBoreBaseBlock.Base>> ARCANE_BORE_BASE =
            BLOCK_ENTITIES.register("arcane_bore_base", () -> BlockEntityType.Builder.of(
                    thaumcraft.common.blocks.ArcaneBoreBaseBlock.Base::new, ModBlocks.ARCANE_BORE_BASE.get()).build(null));

    public static final RegistryObject<BlockEntityType<thaumcraft.common.blocks.EssentiaReservoirBlock.Reservoir>> ESSENTIA_RESERVOIR =
            BLOCK_ENTITIES.register("essentia_reservoir", () -> BlockEntityType.Builder.of(
                    thaumcraft.common.blocks.EssentiaReservoirBlock.Reservoir::new, ModBlocks.ESSENTIA_RESERVOIR.get()).build(null));
    public static final RegistryObject<BlockEntityType<thaumcraft.common.blocks.AdvancedAlchemyFurnaceBlock.Core>> ADVANCED_ALCHEMY_FURNACE =
            BLOCK_ENTITIES.register("advanced_alchemy_furnace", () -> BlockEntityType.Builder.of(
                    thaumcraft.common.blocks.AdvancedAlchemyFurnaceBlock.Core::new, ModBlocks.ADVANCED_ALCHEMY_FURNACE.get()).build(null));
    public static final RegistryObject<BlockEntityType<thaumcraft.common.blocks.AdvancedAlchemyFurnaceBlock.Nozzle>> ADVANCED_ALCHEMY_FURNACE_NOZZLE =
            BLOCK_ENTITIES.register("advanced_alchemy_furnace_nozzle", () -> BlockEntityType.Builder.of(
                    thaumcraft.common.blocks.AdvancedAlchemyFurnaceBlock.Nozzle::new, ModBlocks.ADVANCED_ALCHEMY_FURNACE.get()).build(null));
    public static final RegistryObject<BlockEntityType<thaumcraft.common.blocks.EldritchAltarBlock.Altar>> ELDRITCH_ALTAR =
            BLOCK_ENTITIES.register("eldritch_altar", () -> BlockEntityType.Builder.of(
                    thaumcraft.common.blocks.EldritchAltarBlock.Altar::new, ModBlocks.ELDRITCH_ALTAR.get()).build(null));
    public static final RegistryObject<BlockEntityType<thaumcraft.common.blocks.EldritchObeliskBlock.Obelisk>> ELDRITCH_OBELISK =
            BLOCK_ENTITIES.register("eldritch_obelisk", () -> BlockEntityType.Builder.of(
                    thaumcraft.common.blocks.EldritchObeliskBlock.Obelisk::new, ModBlocks.ELDRITCH_OBELISK.get()).build(null));
    public static final RegistryObject<BlockEntityType<thaumcraft.common.blocks.EldritchObeliskBlock.Cap>> ELDRITCH_CAP =
            BLOCK_ENTITIES.register("eldritch_capstone", () -> BlockEntityType.Builder.of(
                    thaumcraft.common.blocks.EldritchObeliskBlock.Cap::new, ModBlocks.ELDRITCH_CAPSTONE.get()).build(null));
    public static final RegistryObject<BlockEntityType<thaumcraft.common.blocks.CrustedOpeningBlock.Spawner>> CRUSTED_OPENING =
            BLOCK_ENTITIES.register("crusted_opening", () -> BlockEntityType.Builder.of(
                    thaumcraft.common.blocks.CrustedOpeningBlock.Spawner::new, ModBlocks.CRUSTED_OPENING.get()).build(null));
    public static final RegistryObject<BlockEntityType<thaumcraft.common.blocks.RunedStoneBlock.Trap>> RUNED_STONE =
            BLOCK_ENTITIES.register("runed_stone", () -> BlockEntityType.Builder.of(
                    thaumcraft.common.blocks.RunedStoneBlock.Trap::new, ModBlocks.RUNED_STONE.get()).build(null));
    public static final RegistryObject<BlockEntityType<thaumcraft.common.blocks.EldritchVoidBlock.Void>> ELDRITCH_VOID =
            BLOCK_ENTITIES.register("eldritch_void", () -> BlockEntityType.Builder.of(
                    thaumcraft.common.blocks.EldritchVoidBlock.Void::new, ModBlocks.ELDRITCH_VOID_OPEN.get()).build(null));
    public static final RegistryObject<BlockEntityType<thaumcraft.common.blocks.EldritchPortalBlock.Portal>> ELDRITCH_PORTAL =
            BLOCK_ENTITIES.register("eldritch_portal", () -> BlockEntityType.Builder.of(
                    thaumcraft.common.blocks.EldritchPortalBlock.Portal::new, ModBlocks.ELDRITCH_PORTAL.get()).build(null));
    public static final RegistryObject<BlockEntityType<thaumcraft.common.blocks.AncientLockBlock.Lock>> ANCIENT_LOCK =
            BLOCK_ENTITIES.register("ancient_lock", () -> BlockEntityType.Builder.of(
                    thaumcraft.common.blocks.AncientLockBlock.Lock::new, ModBlocks.ANCIENT_LOCK.get()).build(null));
    public static final RegistryObject<BlockEntityType<thaumcraft.common.blocks.StrangeCrystalsBlock.Crystals>> STRANGE_CRYSTALS =
            BLOCK_ENTITIES.register("strange_crystals", () -> BlockEntityType.Builder.of(
                    thaumcraft.common.blocks.StrangeCrystalsBlock.Crystals::new, ModBlocks.STRANGE_CRYSTALS.get()).build(null));
    public static final RegistryObject<BlockEntityType<thaumcraft.common.blocks.NodeJarBlock.Jar>> NODE_JAR =
            BLOCK_ENTITIES.register("node_jar", () -> BlockEntityType.Builder.of(
                    thaumcraft.common.blocks.NodeJarBlock.Jar::new, ModBlocks.NODE_JAR.get()).build(null));
    public static final RegistryObject<BlockEntityType<thaumcraft.common.blocks.InfernalFurnaceBlock.Core>> INFERNAL_FURNACE =
            BLOCK_ENTITIES.register("infernal_furnace", () -> BlockEntityType.Builder.of(
                    thaumcraft.common.blocks.InfernalFurnaceBlock.Core::new, ModBlocks.INFERNAL_FURNACE.get()).build(null));
    public static final RegistryObject<BlockEntityType<thaumcraft.common.blocks.InfernalFurnaceBlock.Nozzle>> INFERNAL_FURNACE_NOZZLE =
            BLOCK_ENTITIES.register("infernal_furnace_nozzle", () -> BlockEntityType.Builder.of(
                    thaumcraft.common.blocks.InfernalFurnaceBlock.Nozzle::new, ModBlocks.INFERNAL_FURNACE.get()).build(null));
    public static final RegistryObject<BlockEntityType<thaumcraft.common.blocks.ArcaneBoreBlock.Bore>> ARCANE_BORE =
            BLOCK_ENTITIES.register("arcane_bore", () -> BlockEntityType.Builder.of(
                    thaumcraft.common.blocks.ArcaneBoreBlock.Bore::new, ModBlocks.ARCANE_BORE.get()).build(null));

    public static final RegistryObject<BlockEntityType<thaumcraft.common.blocks.GrateBlock.Grate>> GRATE =
            BLOCK_ENTITIES.register("grate", () -> BlockEntityType.Builder.of(
                    thaumcraft.common.blocks.GrateBlock.Grate::new, ModBlocks.GRATE.get()).build(null));

    public static final RegistryObject<BlockEntityType<thaumcraft.common.blocks.ArcaneLampBlock.Light>> LAMP_LIGHT =
            BLOCK_ENTITIES.register("lamp_light", () -> BlockEntityType.Builder.of(
                    thaumcraft.common.blocks.ArcaneLampBlock.Light::new, ModBlocks.LAMP_LIGHT.get()).build(null));

    public static final RegistryObject<BlockEntityType<thaumcraft.common.blocks.HungryChestBlock.Chest>> HUNGRY_CHEST =
            BLOCK_ENTITIES.register("hungry_chest", () -> BlockEntityType.Builder.of(
                    thaumcraft.common.blocks.HungryChestBlock.Chest::new, ModBlocks.HUNGRY_CHEST.get()).build(null));

    public static final RegistryObject<BlockEntityType<thaumcraft.common.blocks.ManaPodBlock.Pod>> MANA_POD =
            BLOCK_ENTITIES.register("mana_pod", () -> BlockEntityType.Builder.of(
                    thaumcraft.common.blocks.ManaPodBlock.Pod::new, ModBlocks.MANA_POD.get()).build(null));

    public static final RegistryObject<BlockEntityType<thaumcraft.common.blocks.CrystalClusterBlock.Crystal>> CRYSTAL =
            BLOCK_ENTITIES.register("crystal", () -> BlockEntityType.Builder.of(
                    thaumcraft.common.blocks.CrystalClusterBlock.Crystal::new, ModBlocks.CRYSTAL_AIR.get(),
                    ModBlocks.CRYSTAL_FIRE.get(),
                    ModBlocks.CRYSTAL_WATER.get(),
                    ModBlocks.CRYSTAL_EARTH.get(),
                    ModBlocks.CRYSTAL_ORDER.get(),
                    ModBlocks.CRYSTAL_ENTROPY.get(),
                    ModBlocks.CRYSTAL_MIXED.get()).build(null));

    public static final RegistryObject<BlockEntityType<thaumcraft.common.blocks.WandPedestalBlock.Charger>> WAND_PEDESTAL =
            BLOCK_ENTITIES.register("wand_pedestal", () -> BlockEntityType.Builder.of(
                    thaumcraft.common.blocks.WandPedestalBlock.Charger::new, ModBlocks.WAND_PEDESTAL.get()).build(null));

    public static final RegistryObject<BlockEntityType<thaumcraft.common.blocks.VisChargeRelayBlock.ChargeRelay>> VIS_CHARGE_RELAY =
            BLOCK_ENTITIES.register("vis_charge_relay", () -> BlockEntityType.Builder.of(
                    thaumcraft.common.blocks.VisChargeRelayBlock.ChargeRelay::new, ModBlocks.VIS_CHARGE_RELAY.get()).build(null));

    private ModBlockEntities() {
    }
}
