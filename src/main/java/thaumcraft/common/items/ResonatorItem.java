package thaumcraft.common.items;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.EssentiaTransport;
import thaumcraft.common.blockentity.EssentiaBufferBlockEntity;
import thaumcraft.common.registry.ModSounds;

/**
 * Резонатор эссенции (ItemResonator оригинала): стукнув им по трубе или сосуду, узнаёшь, что внутри
 * и с какой силой и какой аспект туда всасывается.
 */
public class ResonatorItem extends Item {
    public ResonatorItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.hasTag();
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (player == null || !(level.getBlockEntity(pos) instanceof EssentiaTransport transport)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        Direction face = context.getClickedFace();
        if (transport instanceof EssentiaBufferBlockEntity buffer) {
            // в буфере может лежать сразу несколько аспектов
            AspectList stored = buffer.getContainedAspects();
            for (Aspect aspect : stored.getAspectsSortedByName()) {
                player.sendSystemMessage(Component.translatable("tc.resonator1", "" + stored.getAmount(aspect), aspect.getName()));
            }
        } else if (transport.getEssentiaType(face) != null) {
            player.sendSystemMessage(Component.translatable("tc.resonator1", "" + transport.getEssentiaAmount(face),
                    transport.getEssentiaType(face).getName()));
        }
        Aspect suction = transport.getSuctionType(face);
        player.sendSystemMessage(Component.translatable("tc.resonator2", "" + transport.getSuctionAmount(face),
                suction != null ? suction.getName() : Component.translatable("tc.resonator3")));
        level.playSound(null, pos, ModSounds.ALEMBIC_KNOCK.get(), SoundSource.BLOCKS, 0.5f, 1.9f + level.random.nextFloat() * 0.1f);
        return InteractionResult.SUCCESS;
    }
}
