package thaumcraft.common.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import thaumcraft.common.registry.ModSounds;
import thaumcraft.common.research.PlayerKnowledge;
import thaumcraft.common.research.ResearchHelper;

import java.util.List;

/**
 * Предметы Элдрича (ItemEldritchObject оригинала): око Элдрича (открывает путь в Запредельные земли), багровые
 * обряды (прочитав книгу, узнаёшь о культе — исследование CRIMSON) и рунная скрижаль (ключ от запертых дверей
 * в Запредельных землях); установщик обелиска — только для творческого режима: ставит алтарь с парящим обелиском.
 */
public class EldritchObjectItem extends Item {
    public enum Kind { EYE, RITES, TABLET, PLACER }

    private final Kind kind;

    public EldritchObjectItem(Kind kind) {
        super(new Item.Properties().stacksTo(1).rarity(kind == Kind.TABLET ? Rarity.RARE : Rarity.UNCOMMON));
        this.kind = kind;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        switch (kind) {
            case EYE -> tooltip.add(Component.translatable("item.ItemEldritchObject.text.1").withStyle(ChatFormatting.DARK_PURPLE));
            case RITES -> {
                tooltip.add(Component.translatable("item.ItemEldritchObject.text.2").withStyle(ChatFormatting.DARK_PURPLE));
                tooltip.add(Component.translatable("item.ItemEldritchObject.text.3").withStyle(ChatFormatting.DARK_BLUE));
            }
            case TABLET -> tooltip.add(Component.translatable("item.ItemEldritchObject.text.4").withStyle(ChatFormatting.DARK_PURPLE));
            case PLACER -> tooltip.add(Component.translatable("item.thaumcraft.obelisk_placer.creative").withStyle(ChatFormatting.ITALIC));
        }
    }

    /** onItemUseFirst оригинала: по верху блока, если над ним шесть блоков воздуха, — алтарь и обелиск над ним. */
    @Override
    public net.minecraft.world.InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
        if (kind != Kind.PLACER || context.getClickedFace() != net.minecraft.core.Direction.UP) {
            return super.useOn(context);
        }
        Level level = context.getLevel();
        net.minecraft.core.BlockPos pos = context.getClickedPos();
        for (int a = 1; a <= 6; a++) {
            if (!level.isEmptyBlock(pos.above(a))) {
                return net.minecraft.world.InteractionResult.FAIL;
            }
        }
        if (!level.isClientSide) {
            level.setBlock(pos.above(1), thaumcraft.common.registry.ModBlocks.ELDRITCH_ALTAR.get().defaultBlockState(), 3);
            thaumcraft.common.blocks.EldritchObeliskBlock.place(level, pos.above(3));
        }
        return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (kind == Kind.RITES && !level.isClientSide && player instanceof ServerPlayer server
                && !PlayerKnowledge.get(player).isResearchComplete("CRIMSON")) {
            ResearchHelper.complete(server, "CRIMSON");
            level.playSound(null, player, ModSounds.LEARN.get(), SoundSource.PLAYERS, 0.75f, 1f);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
