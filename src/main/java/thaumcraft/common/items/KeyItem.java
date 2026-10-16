package thaumcraft.common.items;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.jetbrains.annotations.Nullable;
import thaumcraft.common.blockentity.OwnedBlockEntity;
import thaumcraft.common.blocks.ArcaneDoorBlock;
import thaumcraft.common.blocks.ArcanePressurePlateBlock;
import thaumcraft.common.registry.ModSounds;

import java.util.List;

/**
 * Чародейский ключ — перенос ItemKey оригинала. Хозяин двери или плиты щелчком чистого ключа «нарезает» ключ к
 * ней; тот, кто щёлкнет нарезанным ключом по своей двери или плите, получает к ней доступ, а ключ исчезает.
 * Железный ключ даёт право пользоваться, золотой — ещё и нарезать ключи и менять настройки плиты.
 */
public class KeyItem extends Item {
    /** 0 — железный, 1 — золотой. */
    private final int grade;

    public KeyItem(int grade) {
        super(new Item.Properties().rarity(Rarity.UNCOMMON));
        this.grade = grade;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.hasTag();
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        boolean door = state.getBlock() instanceof ArcaneDoorBlock;
        if (player == null || !door && !(state.getBlock() instanceof ArcanePressurePlateBlock)) {
            return InteractionResult.PASS;
        }
        BlockPos main = door && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
        if (!(level.getBlockEntity(main) instanceof OwnedBlockEntity owned)) {
            return InteractionResult.PASS;
        }
        player.swing(context.getHand());
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        String location = main.getX() + "," + main.getY() + "," + main.getZ();
        if (!stack.hasTag()) {
            // нарезать ключ может хозяин, а железный — ещё и тот, у кого золотой ключ
            if (owned.isOwner(player) || owned.hasAccess(player, 1) && grade == 0) {
                ItemStack key = new ItemStack(this);
                key.getOrCreateTag().putString("location", location);
                key.getTag().putByte("type", (byte) (door ? 0 : 1));
                key.getTag().putString("dimension", level.dimension().location().toString());
                if (!player.getInventory().add(key)) {
                    player.drop(key, false);
                }
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                message(player, door ? "tc.key1" : "tc.key2");
                level.playSound(null, pos, ModSounds.KEY.get(), SoundSource.PLAYERS, 1f, 0.9f);
            }
            return InteractionResult.SUCCESS;
        }
        boolean here = location.equals(stack.getTag().getString("location"));
        if (here && !owned.isOwner(player) && !owned.hasAccess(player, grade) && !owned.hasAccess(player, 1)) {
            owned.grant(player, grade);
            if (door && level.getBlockEntity(main.above()) instanceof OwnedBlockEntity upper) {
                upper.grant(player, grade);
            }
            player.displayClientMessage(Component.translatable(door ? "tc.key3" : "tc.key5")
                    .append(grade == 0 ? Component.empty() : Component.translatable(door ? "tc.key4" : "tc.key6"))
                    .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC), false);
            level.playSound(null, pos, ModSounds.KEY.get(), SoundSource.PLAYERS, 1f, 1.1f);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        } else {
            message(player, here ? "tc.key8" : "tc.key7");
        }
        return InteractionResult.SUCCESS;
    }

    private static void message(Player player, String key) {
        player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC), false);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (stack.hasTag() && stack.getTag().contains("location")) {
            String location = stack.getTag().getString("location");
            String[] parts = location.split(",");
            if (parts.length == 3) {
                location = "x " + parts[0] + ", z " + parts[2] + ", y " + parts[1];
            }
            tooltip.add(Component.translatable("tc.key9").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
            tooltip.add(Component.translatable(stack.getTag().getByte("type") == 0 ? "tc.key10" : "tc.key11")
                    .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
            tooltip.add(Component.literal(location).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        }
    }
}
