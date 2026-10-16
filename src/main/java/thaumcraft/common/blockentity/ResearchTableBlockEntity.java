package thaumcraft.common.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.items.ResearchNotesItem;
import thaumcraft.common.items.ScribingToolsItem;
import thaumcraft.common.menu.ResearchTableMenu;
import thaumcraft.common.network.SyncKnowledgePacket;
import thaumcraft.common.blocks.ResearchTableBlock;
import thaumcraft.common.registry.ModBlockEntities;
import thaumcraft.common.registry.ModBlocks;
import thaumcraft.common.research.PlayerKnowledge;
import thaumcraft.common.research.ResearchNote;

import java.util.List;

/**
 * Стол исследований (TileResearchTable оригинала): в нём лежат письменные принадлежности и заметка,
 * а окружение время от времени дарит «бесплатные» аспекты.
 */
public class ResearchTableBlockEntity extends BlockEntity implements MenuProvider {
    public static final int TOOLS_SLOT = 0;
    public static final int NOTES_SLOT = 1;

    private final NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);
    /** Аспекты, которые можно один раз потратить бесплатно: их даёт то, что стоит вокруг стола. */
    private AspectList bonusAspects = new AspectList();
    private int nextRecalc;

    public ResearchTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RESEARCH_TABLE.get(), pos, state);
    }

    public NonNullList<ItemStack> getItems() {
        return items;
    }

    public AspectList getBonusAspects() {
        return bonusAspects;
    }

    public void contentsChanged() {
        setChanged();
        if (level != null && !level.isClientSide) {
            // чернильница и свиток на модели стола показывают, что в нём лежит
            BlockState state = getBlockState();
            if (state.hasProperty(ResearchTableBlock.INK)) {
                BlockState shown = state.setValue(ResearchTableBlock.INK, !items.get(TOOLS_SLOT).isEmpty())
                        .setValue(ResearchTableBlock.NOTE, !items.get(NOTES_SLOT).isEmpty());
                if (shown != state) {
                    level.setBlock(worldPosition, shown, 3);
                }
            }
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64.0;
    }

    private boolean hasInk() {
        ItemStack tools = items.get(TOOLS_SLOT);
        return tools.getItem() instanceof ScribingToolsItem && tools.getDamageValue() < tools.getMaxDamage();
    }

    private void useInk() {
        ItemStack tools = items.get(TOOLS_SLOT);
        tools.setDamageValue(tools.getDamageValue() + 1);
    }

    /** Тратит одну единицу аспекта: из запаса игрока, а если там пусто — из бесплатных аспектов стола. */
    private boolean spend(PlayerKnowledge knowledge, Aspect aspect) {
        if (knowledge.getPool(aspect) > 0) {
            knowledge.addPool(aspect, -1);
            return true;
        }
        if (bonusAspects.getAmount(aspect) > 0) {
            bonusAspects.reduce(aspect, 1);
            return true;
        }
        return false;
    }

    private boolean canSpend(PlayerKnowledge knowledge, Aspect aspect) {
        return knowledge.getPool(aspect) > 0 || bonusAspects.getAmount(aspect) > 0;
    }

    /**
     * Кладёт аспект на клетку заметки или стирает уже положенный (aspect == null). Каждое действие стоит чернил.
     */
    public void placeAspect(ServerPlayer player, int q, int r, @Nullable Aspect aspect) {
        ItemStack notes = items.get(NOTES_SLOT);
        ResearchNote note = notes.getItem() instanceof ResearchNotesItem ? ResearchNote.read(notes) : null;
        if (note == null || note.complete || !hasInk()) {
            return;
        }
        ResearchNote.Hex hex = new ResearchNote.Hex(q, r);
        ResearchNote.Entry current = note.hexes.get(hex);
        PlayerKnowledge knowledge = PlayerKnowledge.get(player);
        // «Опытный исследователь»: первая ступень возвращает стёртый аспект в четверти случаев, вторая — в половине,
        // а ещё в одном случае из десяти не тратит аспект, когда его кладут
        boolean expertise = knowledge.isResearchComplete("RESEARCHER1");
        boolean mastery = knowledge.isResearchComplete("RESEARCHER2");
        if (aspect != null) {
            if (current == null || current.type() != ResearchNote.EMPTY || !knowledge.hasDiscovered(aspect)
                    || !canSpend(knowledge, aspect)) {
                return;
            }
            if (mastery && level.random.nextFloat() < 0.1f) {
                level.playSound(null, worldPosition, net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP,
                        net.minecraft.sounds.SoundSource.PLAYERS, 0.2f, 0.9f + level.random.nextFloat() * 0.2f);
            } else {
                spend(knowledge, aspect);
            }
            note.hexes.put(hex, new ResearchNote.Entry(aspect, ResearchNote.PLACED));
        } else {
            if (current == null || current.type() != ResearchNote.PLACED) {
                return;
            }
            float roll = level.random.nextFloat();
            if (current.aspect() != null && (expertise && roll < 0.25f || mastery && roll < 0.5f)) {
                level.playSound(null, worldPosition, net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP,
                        net.minecraft.sounds.SoundSource.PLAYERS, 0.2f, 0.9f + level.random.nextFloat() * 0.2f);
                knowledge.addPool(current.aspect(), 1);
            }
            note.hexes.put(hex, new ResearchNote.Entry(null, ResearchNote.EMPTY));
        }
        useInk();
        if (note.checkCompletion(knowledge)) {
            level.playSound(null, worldPosition, thaumcraft.common.registry.ModSounds.LEARN.get(), SoundSource.BLOCKS, 0.75f, 1f);
        }
        note.write(notes);
        SyncKnowledgePacket.send(player);
        contentsChanged();
    }

    /** Соединяет два аспекта в составной: тратит по единице каждого и даёт единицу результата, открывая его. */
    public void combineAspects(ServerPlayer player, Aspect first, Aspect second) {
        Aspect result = Aspect.combine(first, second);
        PlayerKnowledge knowledge = PlayerKnowledge.get(player);
        if (result == null || !knowledge.hasDiscovered(first) || !knowledge.hasDiscovered(second)
                || !canSpend(knowledge, first) || !canSpend(knowledge, second)) {
            return;
        }
        if (first == second && knowledge.getPool(first) + bonusAspects.getAmount(first) < 2) {
            return;
        }
        spend(knowledge, first);
        spend(knowledge, second);
        knowledge.discover(result);
        knowledge.addPool(result, 1);
        SyncKnowledgePacket.send(player);
        contentsChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ResearchTableBlockEntity table) {
        if (table.nextRecalc++ > 600) {
            table.nextRecalc = 0;
            table.recalculateBonus(level);
            table.contentsChanged();
        }
    }

    /**
     * Раз в 30 секунд стол может получить бесплатный аспект от окружения (recalculateBonus оригинала):
     * темнота даёт энтропию, высота — воздух, огонь, земля и вода рядом — свои стихии, заряженный камень —
     * свою, механизмы — порядок, книжные полки изредка — любой аспект. Каждого первичного не больше одного.
     */
    private void recalculateBonus(Level level) {
        RandomSource random = level.random;
        BlockPos above = worldPosition.above();
        if (!level.isDay() && level.getMaxLocalRawBrightness(above) < 4 && !level.canSeeSky(above) && random.nextInt(20) == 0) {
            bonusAspects.merge(Aspect.ENTROPY, 1);
        }
        int height = level.getMaxBuildHeight();
        for (float fraction : new float[]{0.5f, 0.66f, 0.75f}) {
            if (worldPosition.getY() > height * fraction && random.nextInt(20) == 0) {
                bonusAspects.merge(Aspect.AIR, 1);
            }
        }
        List<BlockState> infused = new java.util.ArrayList<>();
        for (var block : List.of(ModBlocks.INFUSED_STONE_AIR, ModBlocks.INFUSED_STONE_FIRE, ModBlocks.INFUSED_STONE_WATER,
                ModBlocks.INFUSED_STONE_EARTH, ModBlocks.INFUSED_STONE_ORDER, ModBlocks.INFUSED_STONE_ENTROPY,
                ModBlocks.DEEPSLATE_INFUSED_STONE_AIR, ModBlocks.DEEPSLATE_INFUSED_STONE_FIRE, ModBlocks.DEEPSLATE_INFUSED_STONE_WATER,
                ModBlocks.DEEPSLATE_INFUSED_STONE_EARTH, ModBlocks.DEEPSLATE_INFUSED_STONE_ORDER, ModBlocks.DEEPSLATE_INFUSED_STONE_ENTROPY)) {
            infused.add(block.get().defaultBlockState());
        }
        // стихии заряженного камня в том же порядке, что и блоки выше: сначала каменные, потом сланцевые
        Aspect[] infusedAspects = {Aspect.AIR, Aspect.FIRE, Aspect.WATER, Aspect.EARTH, Aspect.ORDER, Aspect.ENTROPY,
                Aspect.AIR, Aspect.FIRE, Aspect.WATER, Aspect.EARTH, Aspect.ORDER, Aspect.ENTROPY};

        for (BlockPos near : BlockPos.betweenClosed(worldPosition.offset(-8, -8, -8), worldPosition.offset(8, 8, 8))) {
            if (!level.isLoaded(near)) {
                continue;
            }
            BlockState state = level.getBlockState(near);
            Aspect found = null;
            int chance = 20;
            for (int i = 0; i < infused.size(); i++) {
                if (state.is(infused.get(i).getBlock())) {
                    found = infusedAspects[i];
                }
            }
            // кристаллы стихий действуют так же, как заряженный камень, но вдвое чаще
            if (state.getBlock() instanceof thaumcraft.common.blocks.CrystalClusterBlock crystal && crystal.kind < 6) {
                found = infusedAspects[crystal.kind];
                chance = 10;
            }
            if (found == null) {
                if (state.is(BlockTags.FIRE) || state.getFluidState().is(FluidTags.LAVA)) {
                    found = Aspect.FIRE;
                } else if (state.is(BlockTags.DIRT)) {
                    found = Aspect.EARTH;
                } else if (state.getFluidState().is(FluidTags.WATER)) {
                    found = Aspect.WATER;
                    chance = 15;
                } else if (state.is(Blocks.PISTON) || state.is(Blocks.STICKY_PISTON) || state.is(Blocks.REPEATER)
                        || state.is(Blocks.COMPARATOR) || state.is(Blocks.REDSTONE_WIRE) || state.is(Blocks.LEVER)) {
                    found = Aspect.ORDER;
                }
            }
            if (found != null && bonusAspects.getAmount(found) < 1 && random.nextInt(chance) == 0) {
                bonusAspects.merge(found, 1);
                return;
            }
            if (state.is(Blocks.BOOKSHELF) && random.nextInt(300) == 0) {
                List<Aspect> all = List.copyOf(Aspect.all());
                bonusAspects.merge(all.get(random.nextInt(all.size())), 1);
                return;
            }
        }
        // TODO: банка с мозгом рядом тоже даёт случайный аспект (1 из 200) — когда появится сама банка
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, items);
        tag.put("Bonus", bonusAspects.save(new CompoundTag()));
        tag.putInt("NextRecalc", nextRecalc);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        items.clear();
        ContainerHelper.loadAllItems(tag, items);
        bonusAspects = AspectList.load(tag.getCompound("Bonus"));
        nextRecalc = tag.getInt("NextRecalc");
    }

    /** Цвет ленты на свитке: цвет главного аспекта исследования из заметки. */
    public int getNoteColor() {
        ItemStack notes = items.get(NOTES_SLOT);
        return notes.hasTag() && notes.getTag().contains("color") ? notes.getTag().getInt("color") : 0x999999;
    }

    /** Лента красится по данным стола, поэтому после их прихода блок надо перерисовать. */
    @Override
    public void onDataPacket(net.minecraft.network.Connection connection, ClientboundBlockEntityDataPacket packet) {
        super.onDataPacket(connection, packet);
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 8);
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ResearchTableMenu(id, inventory, this);
    }
}
