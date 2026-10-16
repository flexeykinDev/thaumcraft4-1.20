package thaumcraft.common.registry;

import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.blocks.AlchemicalFurnaceBlock;
import thaumcraft.common.blocks.ArcaneWorkbenchBlock;
import thaumcraft.common.blocks.EssentiaVesselBlock;
import thaumcraft.common.blocks.CrucibleBlock;
import thaumcraft.common.blocks.FluxBlock;
import thaumcraft.common.blocks.InfusionMatrixBlock;
import thaumcraft.common.blocks.PedestalBlock;
import thaumcraft.common.blocks.NitorBlock;
import thaumcraft.common.blocks.MagicalPlantBlock;
import thaumcraft.common.blocks.ResearchTableBlock;
import thaumcraft.common.blocks.TableBlock;
import thaumcraft.common.blocks.NodeBlock;
import thaumcraft.common.blocks.SilverwoodKnotBlock;
import thaumcraft.common.blocks.MagicalSaplingBlock;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import thaumcraft.Thaumcraft;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Thaumcraft.MODID);

    // Руды (в оригинале BlockCustomOre, мета 0-7)
    public static final RegistryObject<Block> CINNABAR_ORE = ore("cinnabar_ore", ConstantInt.of(0));
    public static final RegistryObject<Block> INFUSED_STONE_AIR = ore("infused_stone_air", UniformInt.of(0, 3));
    public static final RegistryObject<Block> INFUSED_STONE_FIRE = ore("infused_stone_fire", UniformInt.of(0, 3));
    public static final RegistryObject<Block> INFUSED_STONE_WATER = ore("infused_stone_water", UniformInt.of(0, 3));
    public static final RegistryObject<Block> INFUSED_STONE_EARTH = ore("infused_stone_earth", UniformInt.of(0, 3));
    public static final RegistryObject<Block> INFUSED_STONE_ORDER = ore("infused_stone_order", UniformInt.of(0, 3));
    public static final RegistryObject<Block> INFUSED_STONE_ENTROPY = ore("infused_stone_entropy", UniformInt.of(0, 3));
    public static final RegistryObject<Block> AMBER_ORE = ore("amber_ore", UniformInt.of(1, 4));

    // Те же руды в глубинном сланце (новая часть мира в 1.18+): прочнее в полтора раза, как у ванильных руд
    public static final RegistryObject<Block> DEEPSLATE_CINNABAR_ORE = deepslateOre("deepslate_cinnabar_ore", ConstantInt.of(0));
    public static final RegistryObject<Block> DEEPSLATE_INFUSED_STONE_AIR = deepslateOre("deepslate_infused_stone_air", UniformInt.of(0, 3));
    public static final RegistryObject<Block> DEEPSLATE_INFUSED_STONE_FIRE = deepslateOre("deepslate_infused_stone_fire", UniformInt.of(0, 3));
    public static final RegistryObject<Block> DEEPSLATE_INFUSED_STONE_WATER = deepslateOre("deepslate_infused_stone_water", UniformInt.of(0, 3));
    public static final RegistryObject<Block> DEEPSLATE_INFUSED_STONE_EARTH = deepslateOre("deepslate_infused_stone_earth", UniformInt.of(0, 3));
    public static final RegistryObject<Block> DEEPSLATE_INFUSED_STONE_ORDER = deepslateOre("deepslate_infused_stone_order", UniformInt.of(0, 3));
    public static final RegistryObject<Block> DEEPSLATE_INFUSED_STONE_ENTROPY = deepslateOre("deepslate_infused_stone_entropy", UniformInt.of(0, 3));
    public static final RegistryObject<Block> DEEPSLATE_AMBER_ORE = deepslateOre("deepslate_amber_ore", UniformInt.of(1, 4));

    // Деревья (в оригинале BlockMagicalLog, BlockMagicalLeaves, доски из BlockWoodenDevice, саженцы из BlockCustomPlant)
    public static final RegistryObject<Block> GREATWOOD_LOG = log("greatwood_log");
    public static final RegistryObject<Block> SILVERWOOD_LOG = log("silverwood_log");
    public static final RegistryObject<Block> GREATWOOD_LEAVES = leaves("greatwood_leaves", 0);
    public static final RegistryObject<Block> SILVERWOOD_LEAVES = leaves("silverwood_leaves", 7);
    public static final RegistryObject<Block> GREATWOOD_PLANKS = planks("greatwood_planks");
    public static final RegistryObject<Block> SILVERWOOD_PLANKS = planks("silverwood_planks");
    // ступени и плиты (BlockCosmeticStairs и BlockCosmeticWoodSlab оригинала)
    public static final RegistryObject<Block> GREATWOOD_STAIRS = stairs("greatwood_stairs", GREATWOOD_PLANKS, true);
    public static final RegistryObject<Block> SILVERWOOD_STAIRS = stairs("silverwood_stairs", SILVERWOOD_PLANKS, true);
    public static final RegistryObject<Block> GREATWOOD_SLAB = woodSlab("greatwood_slab");
    public static final RegistryObject<Block> SILVERWOOD_SLAB = woodSlab("silverwood_slab");
    public static final RegistryObject<Block> GREATWOOD_SAPLING = BLOCKS.register("greatwood_sapling",
            () -> new MagicalSaplingBlock(ModFeatures.GREATWOOD_GROWER, 25, BlockBehaviour.Properties.copy(Blocks.OAK_SAPLING)));
    public static final RegistryObject<Block> SILVERWOOD_SAPLING = BLOCKS.register("silverwood_sapling",
            () -> new MagicalSaplingBlock(ModFeatures.SILVERWOOD_GROWER, 50, BlockBehaviour.Properties.copy(Blocks.OAK_SAPLING)));

    // Столы (в оригинале BlockTable, мета 0/1 и 15)
    public static final RegistryObject<Block> TABLE = BLOCKS.register("table",
            () -> new TableBlock(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS).strength(2.5f).noOcclusion()));
    public static final RegistryObject<Block> ARCANE_WORKBENCH = BLOCKS.register("arcane_workbench",
            () -> new ArcaneWorkbenchBlock(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS).strength(2.5f).noOcclusion()));

    public static final RegistryObject<Block> DECONSTRUCTION_TABLE = BLOCKS.register("deconstruction_table",
            () -> new thaumcraft.common.blocks.DeconstructionTableBlock(
                    BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS).strength(2.5f).noOcclusion()));
    public static final RegistryObject<Block> RESEARCH_TABLE = BLOCKS.register("research_table",
            () -> new ResearchTableBlock(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS).strength(2.5f).noOcclusion()));

    // Алхимия (в оригинале BlockMetalDevice мета 0, BlockAiry мета 1, BlockCosmeticSolid мета 4)
    public static final RegistryObject<Block> CRUCIBLE = BLOCKS.register("crucible",
            () -> new CrucibleBlock(BlockBehaviour.Properties.copy(Blocks.CAULDRON).noOcclusion()));
    public static final RegistryObject<Block> NITOR = BLOCKS.register("nitor",
            () -> new NitorBlock(BlockBehaviour.Properties.of().noCollission().noOcclusion().instabreak()
                    .lightLevel(state -> 15).sound(SoundType.WOOL)
                    .pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY)));
    public static final RegistryObject<Block> THAUMIUM_BLOCK = BLOCKS.register("thaumium_block",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)));

    // Перегонка эссенции (в оригинале BlockStoneDevice мета 0, BlockMetalDevice мета 1, BlockJar мета 0)
    public static final RegistryObject<Block> ARCANE_STONE = BLOCKS.register("arcane_stone",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(2f, 10f)));
    public static final RegistryObject<Block> ALCHEMICAL_FURNACE = BLOCKS.register("alchemical_furnace",
            () -> new AlchemicalFurnaceBlock(BlockBehaviour.Properties.copy(Blocks.FURNACE).strength(3f, 25f)
                    .lightLevel(state -> state.getValue(AlchemicalFurnaceBlock.LIT) ? 13 : 0)));
    public static final RegistryObject<Block> ALEMBIC = BLOCKS.register("alembic",
            () -> new EssentiaVesselBlock(false, BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(3f, 17f).noOcclusion()));
    public static final RegistryObject<Block> WARDED_JAR = BLOCKS.register("warded_jar",
            () -> new EssentiaVesselBlock(true, BlockBehaviour.Properties.copy(Blocks.GLASS).strength(0.3f).noOcclusion()));
    /** Банка пустоты (BlockJar оригинала, мета 3): всё, что не поместилось, бесследно исчезает. */
    public static final RegistryObject<Block> VOID_JAR = BLOCKS.register("void_jar",
            () -> new EssentiaVesselBlock(true, BlockBehaviour.Properties.copy(Blocks.GLASS).strength(0.3f).noOcclusion()));

    // Инфузия (в оригинале BlockCosmeticSolid мета 7, BlockStoneDevice мета 1 и 2)
    public static final RegistryObject<Block> ARCANE_STONE_BRICKS = BLOCKS.register("arcane_stone_bricks",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(2f, 10f)));
    public static final RegistryObject<Block> INFUSION_PILLAR = BLOCKS.register("infusion_pillar",
            () -> new thaumcraft.common.blocks.InfusionPillarBlock(BlockBehaviour.Properties.copy(Blocks.STONE)
                    .strength(2f, 10f).noOcclusion()));
    public static final RegistryObject<Block> PEDESTAL = BLOCKS.register("pedestal",
            () -> new PedestalBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(3f, 25f).noOcclusion()));
    public static final RegistryObject<Block> INFUSION_MATRIX = BLOCKS.register("infusion_matrix",
            () -> new InfusionMatrixBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(3f, 25f).noOcclusion()
                    .lightLevel(state -> state.getValue(InfusionMatrixBlock.ACTIVE) ? 10 : 0)));

    // Трубы для эссенции (в оригинале BlockTube, мета 0, 1, 3, 5, 6)
    public static final RegistryObject<Block> TUBE = BLOCKS.register("tube",
            () -> new thaumcraft.common.blocks.TubeBlock(thaumcraft.common.blocks.TubeBlock.Kind.NORMAL,
                    BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(0.5f, 5f).noOcclusion()));
    public static final RegistryObject<Block> TUBE_VALVE = BLOCKS.register("tube_valve",
            () -> new thaumcraft.common.blocks.TubeBlock(thaumcraft.common.blocks.TubeBlock.Kind.VALVE,
                    BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(0.5f, 5f).noOcclusion()));
    public static final RegistryObject<Block> TUBE_FILTER = BLOCKS.register("tube_filter",
            () -> new thaumcraft.common.blocks.TubeBlock(thaumcraft.common.blocks.TubeBlock.Kind.FILTER,
                    BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(0.5f, 5f).noOcclusion()));
    public static final RegistryObject<Block> TUBE_RESTRICT = BLOCKS.register("tube_restrict",
            () -> new thaumcraft.common.blocks.TubeBlock(thaumcraft.common.blocks.TubeBlock.Kind.RESTRICT,
                    BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(0.5f, 5f).noOcclusion()));
    public static final RegistryObject<Block> TUBE_ONEWAY = BLOCKS.register("tube_oneway",
            () -> new thaumcraft.common.blocks.TubeBlock(thaumcraft.common.blocks.TubeBlock.Kind.ONEWAY,
                    BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(0.5f, 5f).noOcclusion()));
    public static final RegistryObject<Block> TUBE_BUFFER = BLOCKS.register("tube_buffer",
            () -> new thaumcraft.common.blocks.TubeBlock(thaumcraft.common.blocks.TubeBlock.Kind.BUFFER,
                    BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(0.5f, 5f).noOcclusion()));
    // Остальная алхимическая машинерия (BlockTube мета 2, BlockMetalDevice мета 9, BlockWoodenDevice мета 0)
    public static final RegistryObject<Block> CENTRIFUGE = BLOCKS.register("centrifuge",
            () -> new thaumcraft.common.blocks.CentrifugeBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(3f, 17f).noOcclusion()));
    public static final RegistryObject<Block> CRYSTALLIZER = BLOCKS.register("crystallizer",
            () -> new thaumcraft.common.blocks.CrystallizerBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .strength(3f, 17f).noOcclusion().lightLevel(state -> 5)));
    public static final RegistryObject<Block> ALCHEMICAL_CONSTRUCT = BLOCKS.register("alchemical_construct",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(3f, 17f)));
    public static final RegistryObject<Block> HUNGRY_CHEST = BLOCKS.register("hungry_chest",
            () -> new thaumcraft.common.blocks.HungryChestBlock(BlockBehaviour.Properties.copy(Blocks.CHEST).strength(2.5f).noOcclusion()));
    /** Чародейская лампа (blockMetalDevice:7) и её невидимые огоньки (blockAiry:3). */
    public static final RegistryObject<Block> ARCANE_LAMP = BLOCKS.register("arcane_lamp",
            () -> new thaumcraft.common.blocks.ArcaneLampBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(3f, 17f)
                    .noOcclusion().lightLevel(state -> 15)));
    public static final RegistryObject<Block> LAMP_LIGHT = BLOCKS.register("lamp_light",
            () -> new thaumcraft.common.blocks.ArcaneLampBlock.LightBlock(BlockBehaviour.Properties.copy(Blocks.AIR).lightLevel(state -> 15)));
    /** Лампы роста и плодородия (blockMetalDevice:8 и 13). */
    public static final RegistryObject<Block> LAMP_GROWTH = BLOCKS.register("lamp_growth",
            () -> new thaumcraft.common.blocks.EssentiaLampBlock(true, BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(3f, 17f)
                    .noOcclusion()));
    public static final RegistryObject<Block> LAMP_FERTILITY = BLOCKS.register("lamp_fertility",
            () -> new thaumcraft.common.blocks.EssentiaLampBlock(false, BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(3f, 17f)
                    .noOcclusion()));
    /** Мостовые путника и защиты (blockCosmeticSolid:2 и 3) и невидимая стена над второй (blockAiry:4). */
    public static final RegistryObject<Block> TRAVEL_PAVING = BLOCKS.register("travel_paving",
            () -> new thaumcraft.common.blocks.PavingStoneBlock.Travel(BlockBehaviour.Properties.copy(Blocks.STONE_BRICKS).strength(2f, 10f)
                    .lightLevel(state -> 9)));
    public static final RegistryObject<Block> WARDING_PAVING = BLOCKS.register("warding_paving",
            () -> new thaumcraft.common.blocks.PavingStoneBlock.Ward(BlockBehaviour.Properties.copy(Blocks.STONE_BRICKS).strength(2f, 10f)));
    public static final RegistryObject<Block> WARD_AURA = BLOCKS.register("ward_aura",
            () -> new thaumcraft.common.blocks.PavingStoneBlock.Aura(BlockBehaviour.Properties.of().replaceable().noLootTable()
                    .noOcclusion().strength(-1f, 3600000f).pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY)));
    /** Чародейское ухо (blockWoodenDevice:1) и чародейский левитатор (blockLifter). */
    public static final RegistryObject<Block> ARCANE_EAR = BLOCKS.register("arcane_ear",
            () -> new thaumcraft.common.blocks.ArcaneEarBlock(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS).strength(2.5f).noOcclusion()));
    public static final RegistryObject<Block> LEVITATOR = BLOCKS.register("levitator",
            () -> new thaumcraft.common.blocks.LevitatorBlock(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS).strength(2.5f).noOcclusion()));
    /** Магическое зеркало и зеркало эссенции (blockMirror:0 и 6). */
    public static final RegistryObject<Block> MAGIC_MIRROR = BLOCKS.register("magic_mirror",
            () -> new thaumcraft.common.blocks.MirrorBlock(false, BlockBehaviour.Properties.copy(Blocks.GLASS).strength(1f, 10f).noOcclusion()));
    public static final RegistryObject<Block> ESSENTIA_MIRROR = BLOCKS.register("essentia_mirror",
            () -> new thaumcraft.common.blocks.MirrorBlock(true, BlockBehaviour.Properties.copy(Blocks.GLASS).strength(1f, 10f).noOcclusion()));
    /** Мозг в банке (blockJar:1). */
    public static final RegistryObject<Block> BRAIN_JAR = BLOCKS.register("brain_jar",
            () -> new thaumcraft.common.blocks.BrainJarBlock(BlockBehaviour.Properties.copy(Blocks.GLASS).strength(0.3f).noOcclusion()));
    /** Защищённая магия: чародейская дверь, нажимная плита (blockWoodenDevice:2) и защищённое стекло (blockCosmeticOpaque:2). */
    public static final RegistryObject<Block> ARCANE_DOOR = BLOCKS.register("arcane_door",
            () -> new thaumcraft.common.blocks.ArcaneDoorBlock(BlockBehaviour.Properties.copy(Blocks.IRON_DOOR).strength(15f, 999f)
                    .noOcclusion().pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK)));
    public static final RegistryObject<Block> ARCANE_PRESSURE_PLATE = BLOCKS.register("arcane_pressure_plate",
            () -> new thaumcraft.common.blocks.ArcanePressurePlateBlock(BlockBehaviour.Properties.copy(Blocks.OAK_PRESSURE_PLATE)
                    .strength(2f, 999f).noCollission().pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK)));
    public static final RegistryObject<Block> WARDED_GLASS = BLOCKS.register("warded_glass",
            () -> new thaumcraft.common.blocks.WardedGlassBlock(BlockBehaviour.Properties.copy(Blocks.GLASS).strength(5f, 999f)
                    .pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK)));
    /** Очиститель скверны (blockStoneDevice:14). */
    public static final RegistryObject<Block> FLUX_SCRUBBER = BLOCKS.register("flux_scrubber",
            () -> new thaumcraft.common.blocks.FluxScrubberBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(2f, 10f).noOcclusion()));
    /** Знамя (blockWoodenDevice:8). */
    public static final RegistryObject<Block> THAUM_BANNER = BLOCKS.register("thaum_banner",
            () -> new thaumcraft.common.blocks.ThaumBannerBlock(BlockBehaviour.Properties.copy(Blocks.WHITE_BANNER).strength(1f).noOcclusion()));
    /** Чародейский бур и его основание (blockWoodenDevice:5 и 4). */
    public static final RegistryObject<Block> ARCANE_BORE_BASE = BLOCKS.register("arcane_bore_base",
            () -> new thaumcraft.common.blocks.ArcaneBoreBaseBlock(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS).strength(2.5f).noOcclusion()));
    public static final RegistryObject<Block> ARCANE_BORE = BLOCKS.register("arcane_bore",
            () -> new thaumcraft.common.blocks.ArcaneBoreBlock(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS).strength(2.5f).noOcclusion()));
    /** Адская печь (blockArcaneFurnace): собирается жезлом из куба незерского кирпича, обсидиана и лавы. */
    public static final RegistryObject<Block> INFERNAL_FURNACE = BLOCKS.register("infernal_furnace",
            () -> new thaumcraft.common.blocks.InfernalFurnaceBlock(BlockBehaviour.Properties.copy(Blocks.OBSIDIAN).strength(10f, 500f)
                    .requiresCorrectToolForDrops().lightLevel(thaumcraft.common.blocks.InfernalFurnaceBlock::light)
                    .isValidSpawn((state, level, pos, type) -> false)));
    /** Обсидиановая плитка, тотем и заряженный тотем с узлом (blockCosmeticSolid:1, 0 и 8). */
    public static final RegistryObject<Block> OBSIDIAN_TILE = BLOCKS.register("obsidian_tile",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.OBSIDIAN).strength(30f, 999f)));
    public static final RegistryObject<Block> OBSIDIAN_TOTEM = BLOCKS.register("obsidian_totem",
            () -> new thaumcraft.common.blocks.ObsidianTotemBlock(BlockBehaviour.Properties.copy(Blocks.OBSIDIAN).strength(30f, 999f)));
    public static final RegistryObject<Block> CHARGED_OBSIDIAN_TOTEM = BLOCKS.register("charged_obsidian_totem",
            () -> new thaumcraft.common.blocks.ChargedTotemBlock(BlockBehaviour.Properties.copy(Blocks.OBSIDIAN).strength(30f, 999f)));
    /** Старые урны и брошенные ящики трёх редкостей (blockLootUrn и blockLootCrate). */
    public static final RegistryObject<Block> LOOT_URN_COMMON = lootBlock("loot_urn_common", 0, true);
    public static final RegistryObject<Block> LOOT_URN_UNCOMMON = lootBlock("loot_urn_uncommon", 1, true);
    public static final RegistryObject<Block> LOOT_URN_RARE = lootBlock("loot_urn_rare", 2, true);
    public static final RegistryObject<Block> LOOT_CRATE_COMMON = lootBlock("loot_crate_common", 0, false);
    public static final RegistryObject<Block> LOOT_CRATE_UNCOMMON = lootBlock("loot_crate_uncommon", 1, false);
    public static final RegistryObject<Block> LOOT_CRATE_RARE = lootBlock("loot_crate_rare", 2, false);

    private static RegistryObject<Block> lootBlock(String name, int tier, boolean urn) {
        return BLOCKS.register(name, () -> new thaumcraft.common.blocks.LootBlock(BlockBehaviour.Properties.of()
                .mapColor(urn ? net.minecraft.world.level.material.MapColor.CLAY : net.minecraft.world.level.material.MapColor.WOOD)
                .strength(0.15f, 0f).noOcclusion().sound(urn ? new net.minecraftforge.common.util.ForgeSoundType(1f, 0.7f,
                        thaumcraft.common.registry.ModSounds.URN_BREAK, thaumcraft.common.registry.ModSounds.URN_BREAK,
                        thaumcraft.common.registry.ModSounds.URN_BREAK, thaumcraft.common.registry.ModSounds.URN_BREAK,
                        thaumcraft.common.registry.ModSounds.URN_BREAK) : SoundType.WOOD), tier, urn));
    }

    /** Резервуар эссенции (blockEssentiaReservoir). */
    public static final RegistryObject<Block> ESSENTIA_RESERVOIR = BLOCKS.register("essentia_reservoir",
            () -> new thaumcraft.common.blocks.EssentiaReservoirBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(2f, 17f)
                    .noOcclusion()));
    /** Продвинутая алхимическая конструкция (blockMetalDevice:3) и собранная из неё продвинутая печь (blockAlchemyFurnace). */
    public static final RegistryObject<Block> ADVANCED_ALCHEMICAL_CONSTRUCT = BLOCKS.register("advanced_alchemical_construct",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(3f, 17f)));
    public static final RegistryObject<Block> ADVANCED_ALCHEMY_FURNACE = BLOCKS.register("advanced_alchemy_furnace",
            () -> new thaumcraft.common.blocks.AdvancedAlchemyFurnaceBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(3f, 17f)
                    .noOcclusion().lightLevel(thaumcraft.common.blocks.AdvancedAlchemyFurnaceBlock::light)));
    // Алтарь, обелиск и колпаки Элдрича (BlockEldritch меты 0–3): почти неразрушимы, ничего не дают
    public static final RegistryObject<Block> ELDRITCH_ALTAR = BLOCKS.register("eldritch_altar",
            () -> new thaumcraft.common.blocks.EldritchAltarBlock(eldritch().strength(50f, 20000f).lightLevel(state -> 8).noOcclusion()));
    public static final RegistryObject<Block> ELDRITCH_OBELISK = BLOCKS.register("eldritch_obelisk",
            () -> new thaumcraft.common.blocks.EldritchObeliskBlock(eldritch().strength(50f, 20000f).lightLevel(state -> 8).noOcclusion(),
                    thaumcraft.common.blocks.EldritchObeliskBlock.Part.BASE));
    public static final RegistryObject<Block> ELDRITCH_OBELISK_SEGMENT = BLOCKS.register("eldritch_obelisk_segment",
            () -> new thaumcraft.common.blocks.EldritchObeliskBlock(eldritch().strength(50f, 20000f).lightLevel(state -> 8).noOcclusion(),
                    thaumcraft.common.blocks.EldritchObeliskBlock.Part.SEGMENT));
    public static final RegistryObject<Block> ELDRITCH_CAPSTONE = BLOCKS.register("eldritch_capstone",
            () -> new thaumcraft.common.blocks.EldritchObeliskBlock(eldritch().strength(50f, 20000f).lightLevel(state -> 8).noOcclusion(),
                    thaumcraft.common.blocks.EldritchObeliskBlock.Part.CAP));
    // Камни Запредельных земель (BlockEldritch меты 4–10 и BlockCosmeticSolid меты 11–15)
    public static final RegistryObject<Block> GLOWING_CRUSTED_STONE = BLOCKS.register("glowing_crusted_stone",
            () -> new Block(eldritch().strength(2f, 50f).lightLevel(state -> 12).noOcclusion().requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> GLYPHED_STONE = BLOCKS.register("glyphed_stone",
            () -> new net.minecraft.world.level.block.DropExperienceBlock(eldritch().strength(2f, 50f).lightLevel(state -> 12).noOcclusion()
                    .requiresCorrectToolForDrops(), UniformInt.of(1, 4)));
    public static final RegistryObject<Block> ANCIENT_DOORWAY = BLOCKS.register("ancient_doorway",
            () -> new Block(eldritch().strength(-1f, 3600000f).lightLevel(state -> 12).noLootTable()
                    .isValidSpawn((state, level, pos, type) -> false)));
    public static final RegistryObject<Block> CRUSTED_OPENING = BLOCKS.register("crusted_opening",
            () -> new thaumcraft.common.blocks.CrustedOpeningBlock(eldritch().strength(15f, 50f).lightLevel(state -> 4).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> RUNED_STONE = BLOCKS.register("runed_stone",
            () -> new thaumcraft.common.blocks.RunedStoneBlock(eldritch().strength(15f, 50f).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> ANCIENT_STONE = BLOCKS.register("ancient_stone",
            () -> new Block(eldritch().strength(2f, 10f).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> ANCIENT_ROCK = BLOCKS.register("ancient_rock",
            () -> new Block(eldritch().strength(2f, 10f).requiresCorrectToolForDrops()));
    /** Мета 13: тот же древний камень, но на нём никто не появляется — им выложены залы лабиринта. */
    public static final RegistryObject<Block> ANCIENT_STONE_SEALED = BLOCKS.register("ancient_stone_sealed",
            () -> new Block(eldritch().strength(2f, 10f).requiresCorrectToolForDrops().isValidSpawn((state, level, pos, type) -> false)));
    public static final RegistryObject<Block> CRUSTED_STONE = BLOCKS.register("crusted_stone",
            () -> new Block(eldritch().strength(2f, 10f).lightLevel(state -> 4).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> ANCIENT_PEDESTAL = BLOCKS.register("ancient_pedestal",
            () -> new Block(eldritch().strength(2f, 10f).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> ANCIENT_STONE_STAIRS = stairs("ancient_stone_stairs", ANCIENT_STONE, false);
    public static final RegistryObject<Block> ANCIENT_STONE_SLAB = BLOCKS.register("ancient_stone_slab",
            () -> new net.minecraft.world.level.block.SlabBlock(eldritch().strength(2f, 10f).requiresCorrectToolForDrops()));

    /** Пустота (BlockEldritchNothing): неразрушима, светится едва-едва. */
    public static final RegistryObject<Block> ELDRITCH_VOID = BLOCKS.register("eldritch_void",
            () -> new thaumcraft.common.blocks.EldritchVoidBlock(BlockBehaviour.Properties.of().strength(-1f, 6000000f).noLootTable().noOcclusion()
                    .sound(SoundType.WOOL).lightLevel(state -> 3).isValidSpawn((state, level, pos, type) -> false)
                    .pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK)));
    /** Открытая Пустота: через её открытые грани видна бездна. */
    public static final RegistryObject<Block> ELDRITCH_VOID_OPEN = BLOCKS.register("eldritch_void_open",
            () -> new thaumcraft.common.blocks.EldritchVoidBlock.Open(BlockBehaviour.Properties.of().strength(-1f, 6000000f).noLootTable().noOcclusion()
                    .sound(SoundType.WOOL).lightLevel(state -> 3).isValidSpawn((state, level, pos, type) -> false)
                    .pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK)));
    /** Портал Элдрича (BlockEldritchPortal). */
    public static final RegistryObject<Block> ELDRITCH_PORTAL = BLOCKS.register("eldritch_portal",
            () -> new thaumcraft.common.blocks.EldritchPortalBlock(BlockBehaviour.Properties.of().strength(-1f, 200000f).noLootTable().noOcclusion()
                    .noCollission().lightLevel(state -> 15).pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK)));
    /** Древний запирающий механизм (BlockEldritch мета 8). */
    public static final RegistryObject<Block> ANCIENT_LOCK = BLOCKS.register("ancient_lock",
            () -> new thaumcraft.common.blocks.AncientLockBlock(eldritch().strength(-1f, 3600000f).lightLevel(state -> 5).noLootTable()));
    /** Непроходимое (BlockAiry мета 12): невидимая стена в проёме зала хранителя. */
    public static final RegistryObject<Block> IMPASSABLE = BLOCKS.register("impassable",
            () -> new thaumcraft.common.blocks.EldritchFieldBlock(BlockBehaviour.Properties.of().strength(-1f, 3600000f).noLootTable().noOcclusion()
                    .isValidSpawn((state, level, pos, type) -> false).pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK), false));
    /** Истощающее поле (BlockAiry мета 11). */
    public static final RegistryObject<Block> SAPPING_FIELD = BLOCKS.register("sapping_field",
            () -> new thaumcraft.common.blocks.EldritchFieldBlock(BlockBehaviour.Properties.of().strength(100f, 50f).noLootTable().noOcclusion()
                    .noCollission().replaceable().randomTicks().pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY), true));
    /** Странные кристаллы (blockCrystal мета 7). */
    public static final RegistryObject<Block> STRANGE_CRYSTALS = BLOCKS.register("strange_crystals",
            () -> new thaumcraft.common.blocks.StrangeCrystalsBlock(BlockBehaviour.Properties.copy(Blocks.GLASS).strength(0.7f, 1f).noOcclusion()
                    .lightLevel(state -> 7).sound(ModBlocks.CRYSTAL_SOUND)));

    private static BlockBehaviour.Properties eldritch() {
        return BlockBehaviour.Properties.copy(Blocks.STONE);
    }

    /** Узел в банке (blockJar:2). */
    public static final RegistryObject<Block> NODE_JAR = BLOCKS.register("node_jar",
            () -> new thaumcraft.common.blocks.NodeJarBlock(BlockBehaviour.Properties.copy(Blocks.GLASS).strength(0.3f).noOcclusion()
                    .lightLevel(state -> 11)));
    /** Решётка (blockMetalDevice:5 и 6). */
    public static final RegistryObject<Block> GRATE = BLOCKS.register("grate",
            () -> new thaumcraft.common.blocks.GrateBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BARS).strength(3f, 17f).noOcclusion()));
    /** Янтарный блок и янтарный кирпич (blockCosmeticOpaque:0 и 1). */
    public static final RegistryObject<Block> AMBER_BLOCK = BLOCKS.register("amber_block",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.GLASS).strength(0.5f).noOcclusion()));
    public static final RegistryObject<Block> AMBER_BRICKS = BLOCKS.register("amber_bricks",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.GLASS).strength(0.5f).noOcclusion()));
    public static final RegistryObject<Block> GOLEM_FETTER = BLOCKS.register("golem_fetter",
            () -> new thaumcraft.common.blocks.GolemFetterBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(2f, 10f)));
    /** Блок сала (BlockCosmeticSolid оригинала, мета 5). */
    public static final RegistryObject<Block> TALLOW_BLOCK = BLOCKS.register("tallow_block",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.CLAY).mapColor(MapColor.SAND).strength(2f, 10f).sound(SoundType.SLIME_BLOCK)));
    public static final RegistryObject<Block> THAUMATORIUM = BLOCKS.register("thaumatorium",
            () -> new thaumcraft.common.blocks.ThaumatoriumBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(3f, 17f)
                    .noOcclusion()));
    public static final RegistryObject<Block> MNEMONIC_MATRIX = BLOCKS.register("mnemonic_matrix",
            () -> new thaumcraft.common.blocks.MnemonicMatrixBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(3f, 17f)
                    .noOcclusion()));
    public static final RegistryObject<Block> BELLOWS = BLOCKS.register("bellows",
            () -> new thaumcraft.common.blocks.BellowsBlock(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS).strength(1f).noOcclusion()));

    // Дыра от фокуса «Портативная дыра»: неразрушима, проходима, слегка светится
    public static final RegistryObject<Block> HOLE = BLOCKS.register("hole",
            () -> new thaumcraft.common.blocks.HoleBlock(BlockBehaviour.Properties.of().noCollission().noOcclusion()
                    .strength(-1f, 3600000f).noLootTable().lightLevel(state -> 11)
                    .pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK)));

    // Защищённый блок фокуса «Защита»: неразрушим, выглядит как блок, который он заменил
    public static final RegistryObject<Block> WARDED = BLOCKS.register("warded",
            () -> new thaumcraft.common.blocks.WardedBlock(BlockBehaviour.Properties.copy(Blocks.STONE)
                    .strength(-1f, 3600000f).noLootTable().pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK)));

    // Сеть вис: стабилизаторы узла, преобразователь узла и реле (BlockStoneDevice 9-11, BlockMetalDevice 14)
    public static final RegistryObject<Block> NODE_STABILIZER = BLOCKS.register("node_stabilizer",
            () -> new thaumcraft.common.blocks.NodeStabilizerBlock(false, BlockBehaviour.Properties.copy(Blocks.STONE).strength(3f, 25f).noOcclusion()));
    public static final RegistryObject<Block> ADVANCED_NODE_STABILIZER = BLOCKS.register("advanced_node_stabilizer",
            () -> new thaumcraft.common.blocks.NodeStabilizerBlock(true, BlockBehaviour.Properties.copy(Blocks.STONE).strength(3f, 25f).noOcclusion()));
    public static final RegistryObject<Block> NODE_TRANSDUCER = BLOCKS.register("node_transducer",
            () -> new thaumcraft.common.blocks.NodeTransducerBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(3f, 25f).noOcclusion()));
    public static final RegistryObject<Block> VIS_RELAY = BLOCKS.register("vis_relay",
            () -> new thaumcraft.common.blocks.VisRelayBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(1f, 10f).noOcclusion()
                    .noCollission().lightLevel(state -> 7)));

    // Кристаллические гроздья (BlockCrystal, виды 0–6): светятся, ломаются рукой, звучат как кристалл
    private static final SoundType CRYSTAL_SOUND = new net.minecraftforge.common.util.ForgeSoundType(1f, 1f,
            thaumcraft.common.registry.ModSounds.CRYSTAL, thaumcraft.common.registry.ModSounds.CRYSTAL,
            thaumcraft.common.registry.ModSounds.CRYSTAL, thaumcraft.common.registry.ModSounds.CRYSTAL,
            thaumcraft.common.registry.ModSounds.CRYSTAL);

    private static RegistryObject<Block> crystal(String name, int kind) {
        return BLOCKS.register("crystal_" + name, () -> new thaumcraft.common.blocks.CrystalClusterBlock(
                BlockBehaviour.Properties.copy(Blocks.GLASS).strength(0.7f, 0.6f).noOcclusion().lightLevel(state -> 7)
                        .sound(CRYSTAL_SOUND), kind));
    }

    public static final RegistryObject<Block> CRYSTAL_AIR = crystal("air", 0);
    public static final RegistryObject<Block> CRYSTAL_FIRE = crystal("fire", 1);
    public static final RegistryObject<Block> CRYSTAL_WATER = crystal("water", 2);
    public static final RegistryObject<Block> CRYSTAL_EARTH = crystal("earth", 3);
    public static final RegistryObject<Block> CRYSTAL_ORDER = crystal("order", 4);
    public static final RegistryObject<Block> CRYSTAL_ENTROPY = crystal("entropy", 5);
    public static final RegistryObject<Block> CRYSTAL_MIXED = crystal("mixed", 6);

    // Порча (BlockTaint и BlockTaintFibres): корка, порченая почва, плоть, волокна и порченые растения
    private static final SoundType GORE_SOUND = new net.minecraftforge.common.util.ForgeSoundType(0.5f, 0.8f,
            thaumcraft.common.registry.ModSounds.GORE, thaumcraft.common.registry.ModSounds.GORE,
            thaumcraft.common.registry.ModSounds.GORE, thaumcraft.common.registry.ModSounds.GORE,
            thaumcraft.common.registry.ModSounds.GORE);

    private static BlockBehaviour.Properties taint() {
        return BlockBehaviour.Properties.of().mapColor(net.minecraft.world.level.material.MapColor.COLOR_PURPLE).sound(GORE_SOUND);
    }

    public static final RegistryObject<Block> TAINT_CRUST = BLOCKS.register("taint_crust",
            () -> new thaumcraft.common.taint.TaintBlock(taint().strength(1.75f, 6f).randomTicks().noLootTable(), true));
    public static final RegistryObject<Block> TAINTED_SOIL = BLOCKS.register("tainted_soil",
            () -> new thaumcraft.common.taint.TaintBlock(taint().strength(1.5f, 6f).randomTicks(), false));
    public static final RegistryObject<Block> FLESH_BLOCK = BLOCKS.register("flesh_block", () -> new Block(taint().strength(0.2f, 6f)));
    public static final RegistryObject<Block> TAINT_FIBRES = BLOCKS.register("taint_fibres",
            () -> new thaumcraft.common.taint.TaintFibresBlock(taint().strength(1f, 3f).randomTicks().noCollission().noOcclusion()
                    .replaceable().noLootTable()));
    public static final RegistryObject<Block> TAINT_GRASS = BLOCKS.register("taint_grass",
            () -> new thaumcraft.common.taint.TaintPlantBlock(taint().strength(1f, 3f).randomTicks().noCollission().noOcclusion()
                    .noLootTable().offsetType(BlockBehaviour.OffsetType.XZ), false));
    public static final RegistryObject<Block> TAINT_PLANT = BLOCKS.register("taint_plant",
            () -> new thaumcraft.common.taint.TaintPlantBlock(taint().strength(1f, 3f).randomTicks().noCollission().noOcclusion()
                    .noLootTable().offsetType(BlockBehaviour.OffsetType.XZ).lightLevel(state -> 8), false));
    public static final RegistryObject<Block> SPORE_STALK = BLOCKS.register("spore_stalk",
            () -> new thaumcraft.common.taint.TaintPlantBlock(taint().strength(1f, 3f).randomTicks().noCollission().noOcclusion()
                    .noLootTable().lightLevel(state -> state.getValue(thaumcraft.common.taint.TaintPlantBlock.RIPE) ? 10 : 0), true));

    // Зарядное реле верстака (BlockMetalDevice мета 2)
    public static final RegistryObject<Block> VIS_CHARGE_RELAY = BLOCKS.register("vis_charge_relay",
            () -> new thaumcraft.common.blocks.VisChargeRelayBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(1f, 10f)
                    .noOcclusion().lightLevel(state -> 7)));

    // Пьедестал подзарядки и его фокус (BlockStoneDevice меты 5 и 8)
    public static final RegistryObject<Block> WAND_PEDESTAL = BLOCKS.register("wand_pedestal",
            () -> new thaumcraft.common.blocks.WandPedestalBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(3f, 25f).noOcclusion()));
    public static final RegistryObject<Block> WAND_PEDESTAL_FOCUS = BLOCKS.register("wand_pedestal_focus",
            () -> new thaumcraft.common.blocks.WandPedestalFocusBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(3f, 25f).noOcclusion()));

    // Фокальный манипулятор (BlockStoneDevice мета 13) и плита из магического камня для его рецепта
    public static final RegistryObject<Block> FOCAL_MANIPULATOR = BLOCKS.register("focal_manipulator",
            () -> new thaumcraft.common.blocks.FocalManipulatorBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(3f, 25f).noOcclusion()));
    public static final RegistryObject<Block> ARCANE_STONE_STAIRS = stairs("arcane_stone_stairs", ARCANE_STONE_BRICKS, false);
    public static final RegistryObject<Block> ARCANE_STONE_SLAB = BLOCKS.register("arcane_stone_slab",
            () -> new net.minecraft.world.level.block.SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(2f, 10f)));

    // Наэлектризованная земля после «Земного разряда» (BlockAiry мета 10)
    public static final RegistryObject<Block> ELECTRIFIED_GROUND = BLOCKS.register("electrified_ground",
            () -> new thaumcraft.common.blocks.ElectrifiedGroundBlock(BlockBehaviour.Properties.of().noCollission().noOcclusion()
                    .instabreak().noLootTable().replaceable().randomTicks().lightLevel(state -> 6)
                    .pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY)));

    // Флюс (в оригинале BlockFluxGoo и BlockFluxGas)
    public static final RegistryObject<Block> FLUX_GOO = BLOCKS.register("flux_goo",
            () -> new FluxBlock(false, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).noCollission()
                    .noOcclusion().noLootTable().randomTicks().strength(100f).sound(SoundType.SLIME_BLOCK)
                    .pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY)));
    /** Жидкая смерть: конечная жидкость, как флюс, но не испаряется. */
    public static final RegistryObject<Block> LIQUID_DEATH = BLOCKS.register("liquid_death",
            () -> new thaumcraft.common.blocks.LiquidDeathBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).noCollission()
                    .noOcclusion().noLootTable().strength(100f).lightLevel(state -> 8).sound(SoundType.SLIME_BLOCK)
                    .pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY)));
    public static final RegistryObject<Block> PURIFYING_FLUID = BLOCKS.register("purifying_fluid",
            () -> new thaumcraft.common.blocks.PurifyingFluidBlock(thaumcraft.common.registry.ModFluids.PURIFYING,
                    BlockBehaviour.Properties.copy(Blocks.WATER).mapColor(MapColor.SNOW).lightLevel(state -> 10)));
    public static final RegistryObject<Block> ARCANE_SPA = BLOCKS.register("arcane_spa",
            () -> new thaumcraft.common.blocks.ArcaneSpaBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(2f, 10f)));
    public static final RegistryObject<Block> FLUX_GAS = BLOCKS.register("flux_gas",
            () -> new FluxBlock(true, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).noCollission()
                    .noOcclusion().noLootTable().randomTicks().strength(100f).replaceable()
                    .pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY)));

    // Узлы ауры (в оригинале BlockAiry мета 0 и BlockMagicalLog мета 2)
    public static final RegistryObject<Block> AURA_NODE = BLOCKS.register("aura_node",
            () -> new NodeBlock(BlockBehaviour.Properties.of().noCollission().noOcclusion().noLootTable()
                    .strength(2f, 200f).lightLevel(state -> 8).sound(SoundType.WOOL)));
    public static final RegistryObject<Block> SILVERWOOD_KNOT = BLOCKS.register("silverwood_knot",
            () -> new SilverwoodKnotBlock(BlockBehaviour.Properties.copy(Blocks.OAK_LOG).strength(2.5f)));

    // Растения (в оригинале BlockCustomPlant, мета 2 и 3)
    public static final RegistryObject<Block> SHIMMERLEAF = BLOCKS.register("shimmerleaf",
            () -> new MagicalPlantBlock(MagicalPlantBlock.Kind.SHIMMERLEAF,
                    BlockBehaviour.Properties.copy(Blocks.DANDELION).lightLevel(state -> 8)));
    public static final RegistryObject<Block> CINDERPEARL = BLOCKS.register("cinderpearl",
            () -> new MagicalPlantBlock(MagicalPlantBlock.Kind.CINDERPEARL,
                    BlockBehaviour.Properties.copy(Blocks.DANDELION).lightLevel(state -> 8)));
    public static final RegistryObject<Block> VISHROOM = BLOCKS.register("vishroom",
            () -> new MagicalPlantBlock(MagicalPlantBlock.Kind.VISHROOM,
                    BlockBehaviour.Properties.copy(Blocks.DANDELION).lightLevel(state -> 8)));
    public static final RegistryObject<Block> ETHEREAL_BLOOM = BLOCKS.register("ethereal_bloom",
            () -> new thaumcraft.common.blocks.EtherealBloomBlock(BlockBehaviour.Properties.copy(Blocks.DANDELION).lightLevel(state -> 15)));
    /** Сальные свечи шестнадцати цветов, в порядке красителей от белого до чёрного. */
    public static final java.util.List<RegistryObject<Block>> TALLOW_CANDLES = java.util.Arrays.stream(net.minecraft.world.item.DyeColor.values())
            .map(color -> BLOCKS.register("tallow_candle_" + color.getName(), () -> (Block) new thaumcraft.common.blocks.TallowCandleBlock(color,
                    BlockBehaviour.Properties.of().mapColor(color).noCollission().noOcclusion().strength(0.1f).sound(SoundType.WOOL)
                            .lightLevel(state -> 14).pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY))))
            .toList();

    /** Стручок маны: без своего предмета, сажается зерном маны. */
    public static final RegistryObject<Block> MANA_POD = BLOCKS.register("mana_pod",
            () -> new thaumcraft.common.blocks.ManaPodBlock(BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noOcclusion()
                    .strength(0.2f).randomTicks().sound(SoundType.GRASS).pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY)
                    .lightLevel(state -> state.getValue(thaumcraft.common.blocks.ManaPodBlock.AGE))));

    private static RegistryObject<Block> log(String name) {
        return BLOCKS.register(name, () -> new RotatedPillarBlock(
                BlockBehaviour.Properties.copy(Blocks.OAK_LOG).strength(2.5f)) {
            @Override
            public boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
                return true;
            }

            @Override
            public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
                return 5;
            }

            @Override
            public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
                return 5;
            }
        });
    }

    private static RegistryObject<Block> leaves(String name, int light) {
        return BLOCKS.register(name, () -> new LeavesBlock(
                BlockBehaviour.Properties.copy(Blocks.OAK_LEAVES).lightLevel(state -> light)) {
            @Override
            public boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
                return true;
            }

            @Override
            public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
                return 60;
            }

            @Override
            public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
                return 30;
            }
        });
    }

    private static RegistryObject<Block> stairs(String name, RegistryObject<Block> base, boolean wood) {
        return BLOCKS.register(name, () -> new net.minecraft.world.level.block.StairBlock(() -> base.get().defaultBlockState(),
                BlockBehaviour.Properties.copy(base.get())) {
            @Override
            public boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
                return wood;
            }

            @Override
            public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
                return wood ? 20 : 0;
            }

            @Override
            public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
                return wood ? 5 : 0;
            }
        });
    }

    private static RegistryObject<Block> woodSlab(String name) {
        return BLOCKS.register(name, () -> new net.minecraft.world.level.block.SlabBlock(BlockBehaviour.Properties.copy(Blocks.OAK_SLAB)) {
            @Override
            public boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
                return true;
            }

            @Override
            public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
                return 20;
            }

            @Override
            public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
                return 5;
            }
        });
    }

    private static RegistryObject<Block> planks(String name) {
        return BLOCKS.register(name, () -> new Block(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS)) {
            @Override
            public boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
                return true;
            }

            @Override
            public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
                return 20;
            }

            @Override
            public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
                return 5;
            }
        });
    }

    private static RegistryObject<Block> deepslateOre(String name, IntProvider experience) {
        return BLOCKS.register(name, () -> new DropExperienceBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.DEEPSLATE)
                .requiresCorrectToolForDrops()
                .strength(2.25f, 5f)
                .sound(SoundType.DEEPSLATE), experience));
    }

    private static RegistryObject<Block> ore(String name, IntProvider experience) {
        return BLOCKS.register(name, () -> new DropExperienceBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .requiresCorrectToolForDrops()
                .strength(1.5f, 5f)
                .sound(SoundType.STONE), experience));
    }

    private ModBlocks() {
    }
}
