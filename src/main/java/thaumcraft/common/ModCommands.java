package thaumcraft.common;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.nodes.NodeType;
import thaumcraft.common.blockentity.NodeBlockEntity;
import thaumcraft.common.world.NodeGenerator;
import thaumcraft.common.network.SyncKnowledgePacket;
import thaumcraft.common.research.PlayerKnowledge;
import thaumcraft.common.research.ResearchEntry;
import thaumcraft.common.research.ResearchHelper;
import thaumcraft.common.research.ResearchRegistry;

/**
 * Команды для проверки и администрирования (нужны права оператора):
 * /thaumcraft discover all — открыть все аспекты; /thaumcraft discover &lt;аспект&gt; — открыть один;
 * /thaumcraft reset — забыть всё изученное; /thaumcraft ores — посчитать блоки мода вокруг игрока;
 * /thaumcraft node [тип] — поставить узел ауры перед собой;
 * /thaumcraft research <ключ|all|reset> — завершить исследование, все сразу или сбросить.
 */
@Mod.EventBusSubscriber(modid = Thaumcraft.MODID)
public final class ModCommands {

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("thaumcraft")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("discover")
                        .then(Commands.argument("aspect", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        java.util.stream.Stream.concat(java.util.stream.Stream.of("all"),
                                                Aspect.all().stream().map(Aspect::getTag)), builder))
                                .executes(ModCommands::discover)))
                .then(Commands.literal("reset").executes(ModCommands::reset))
                .then(Commands.literal("ores").executes(ModCommands::countOres))
                .then(Commands.literal("research")
                        .then(Commands.argument("key", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        java.util.stream.Stream.concat(java.util.stream.Stream.of("all", "reset"),
                                                ResearchRegistry.entries().stream().map(e -> e.key)), builder))
                                .executes(ModCommands::research)))
                .then(Commands.literal("taint").executes(ModCommands::taint))
                .then(Commands.literal("biomes").executes(ModCommands::biomes))
                .then(Commands.literal("champion").executes(context -> champion(context, -1))
                        .then(Commands.argument("mod", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0,
                                        thaumcraft.common.champion.Champions.MODS.length - 1))
                                .executes(context -> champion(context,
                                        com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "mod")))))
                .then(Commands.literal("node").executes(context -> createNode(context, null))
                        .then(Commands.argument("type", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        java.util.Arrays.stream(NodeType.values()).map(t -> t.name().toLowerCase()), builder))
                                .executes(context -> createNode(context, StringArgumentType.getString(context, "type"))))));
    }

    private static int discover(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        PlayerKnowledge knowledge = PlayerKnowledge.get(player);
        String name = StringArgumentType.getString(context, "aspect");
        int count = 0;
        for (Aspect aspect : Aspect.all()) {
            if ((name.equals("all") || aspect.getTag().equals(name)) && knowledge.discover(aspect)) {
                count++;
            }
        }
        SyncKnowledgePacket.send(player);
        int discovered = count;
        context.getSource().sendSuccess(() -> Component.literal("Thaumcraft: открыто аспектов: " + discovered), false);
        return count;
    }

    private static int reset(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        PlayerKnowledge.get(player).load(new net.minecraft.nbt.CompoundTag());
        SyncKnowledgePacket.send(player);
        context.getSource().sendSuccess(() -> Component.literal("Thaumcraft: знания сброшены"), false);
        return 1;
    }

    /** Считает блоки мода в 81 чанке вокруг игрока: быстрый способ проверить генерацию мира. */
    private static int countOres(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ServerLevel level = player.serverLevel();
        Map<Block, Integer> counts = new LinkedHashMap<>();
        ChunkPos center = player.chunkPosition();
        int chunks = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int cx = center.x - 4; cx <= center.x + 4; cx++) {
            for (int cz = center.z - 4; cz <= center.z + 4; cz++) {
                LevelChunk chunk = level.getChunk(cx, cz);
                chunks++;
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y++) {
                            Block block = chunk.getBlockState(pos.set(x, y, z)).getBlock();
                            if (Thaumcraft.MODID.equals(ForgeRegistries.BLOCKS.getKey(block).getNamespace())) {
                                counts.merge(block, 1, Integer::sum);
                            }
                        }
                    }
                }
            }
        }
        int total = chunks;
        context.getSource().sendSuccess(() -> Component.literal("Thaumcraft: блоки мода в " + total + " чанках:"), false);
        counts.forEach((block, count) -> context.getSource().sendSuccess(() -> Component.literal(
                String.format("  %s: %d (%.1f на чанк)", block.getName().getString(), count, count / (float) total)), false));
        return counts.size();
    }

    /** Завершает одно исследование, все сразу (all) или сбрасывает их (reset). */
    private static int research(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        PlayerKnowledge knowledge = PlayerKnowledge.get(player);
        String key = StringArgumentType.getString(context, "key");
        int count = 0;
        if (key.equals("reset")) {
            knowledge.clearResearch();
            ResearchHelper.grantAutoUnlocks(knowledge);
        } else {
            for (ResearchEntry entry : ResearchRegistry.entries()) {
                if ((key.equals("all") || entry.key.equals(key)) && knowledge.completeResearch(entry.key)) {
                    count++;
                }
            }
        }
        SyncKnowledgePacket.send(player);
        int completed = count;
        context.getSource().sendSuccess(() -> Component.literal(key.equals("reset")
                ? "Thaumcraft: исследования сброшены" : "Thaumcraft: завершено исследований: " + completed), false);
        return count;
    }

    /** Ставит узел ауры на уровне глаз игрока: случайный или заданного типа. */
    /** Земля вокруг игрока (9x9 блоков) становится порченой, под ногами вырастают волокна. */
    private static int taint(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        net.minecraft.server.level.ServerLevel level = player.serverLevel();
        for (int dx = -4; dx <= 4; dx += 4) {
            for (int dz = -4; dz <= 4; dz += 4) {
                thaumcraft.common.taint.Taint.setTainted(level, player.blockPosition().offset(dx, 0, dz));
            }
        }
        thaumcraft.common.taint.Taint.spreadFibres(level, player.blockPosition());
        context.getSource().sendSuccess(() -> Component.literal("Thaumcraft: земля вокруг заражена порчей"), false);
        return 1;
    }

    /**
     * Доля биомов на карте: опрашивает генератор мира по сетке 16 на 16 тысяч блоков вокруг начала координат.
     * Нужна, чтобы проверять, насколько часто встречаются порченые земли.
     */
    private static int biomes(CommandContext<CommandSourceStack> context) {
        net.minecraft.server.level.ServerLevel level = context.getSource().getLevel();
        net.minecraft.world.level.biome.BiomeSource source = level.getChunkSource().getGenerator().getBiomeSource();
        net.minecraft.world.level.biome.Climate.Sampler sampler = level.getChunkSource().randomState().sampler();
        java.util.Map<String, Integer> counts = new java.util.TreeMap<>();
        int total = 0;
        for (int x = -8192; x < 8192; x += 64) {
            for (int z = -8192; z < 8192; z += 64) {
                String name = source.getNoiseBiome(x >> 2, 80 >> 2, z >> 2, sampler).unwrapKey()
                        .map(key -> key.location().toString()).orElse("?");
                counts.merge(name, 1, Integer::sum);
                total++;
            }
        }
        StringBuilder line = new StringBuilder();
        int all = total;
        counts.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue()).forEach(entry ->
                line.append(String.format(java.util.Locale.ROOT, " %s %.2f%%;", entry.getKey().replace("minecraft:", ""),
                        entry.getValue() * 100.0 / all)));
        double taint = counts.getOrDefault("thaumcraft:tainted_land", 0) * 100.0 / total;
        thaumcraft.Thaumcraft.LOGGER.info("BIOMES порченые земли {}%; все:{}", String.format(java.util.Locale.ROOT, "%.2f", taint), line);
        context.getSource().sendSuccess(() -> Component.literal(String.format(java.util.Locale.ROOT,
                "Thaumcraft: порченые земли занимают %.2f%% карты", taint)), false);
        return 1;
    }

    /** Ближайший монстр в 16 блоках становится особым; без номера свойство случайное. */
    private static int champion(CommandContext<CommandSourceStack> context, int type) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        net.minecraft.world.entity.monster.Monster nearest = null;
        for (net.minecraft.world.entity.monster.Monster mob : player.level().getEntitiesOfClass(
                net.minecraft.world.entity.monster.Monster.class, player.getBoundingBox().inflate(16))) {
            if (nearest == null || mob.distanceToSqr(player) < nearest.distanceToSqr(player)) {
                nearest = mob;
            }
        }
        if (nearest == null) {
            context.getSource().sendFailure(Component.literal("Thaumcraft: рядом нет монстров"));
            return 0;
        }
        if (type < 0) {
            thaumcraft.common.champion.Champions.makeChampion(nearest, true);
        } else {
            thaumcraft.common.champion.Champions.makeChampion(nearest, type, true);
        }
        net.minecraft.world.entity.monster.Monster made = nearest;
        context.getSource().sendSuccess(() -> Component.literal("Thaumcraft: ").append(made.getDisplayName()), false);
        return 1;
    }

    private static int createNode(CommandContext<CommandSourceStack> context, String typeName) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        BlockPos pos = BlockPos.containing(player.getEyePosition().add(player.getLookAngle().scale(2)));
        if (!player.level().getBlockState(pos).isAir()) {
            context.getSource().sendFailure(Component.literal("Thaumcraft: перед тобой нет свободного места"));
            return 0;
        }
        NodeGenerator.createRandomNode(player.level(), pos, player.getRandom(), false);
        if (typeName != null && player.level().getBlockEntity(pos) instanceof NodeBlockEntity node) {
            for (NodeType type : NodeType.values()) {
                if (type.name().equalsIgnoreCase(typeName)) {
                    node.setNodeType(type);
                    node.nodeChange();
                }
            }
        }
        context.getSource().sendSuccess(() -> Component.literal("Thaumcraft: узел создан"), false);
        return 1;
    }

    private ModCommands() {
    }
}
