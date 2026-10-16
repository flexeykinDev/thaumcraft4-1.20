package thaumcraft.common.items.tools;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import thaumcraft.common.registry.ModMaterials;
import thaumcraft.common.registry.ModSounds;

/**
 * Кирка ядра — перенос ItemElementalPickaxe оригинала. Поджигает тех, кого ею бьют; с руды иногда выбивает
 * самородную гроздь (см. ClusterLootModifier); а если щёлкнуть ею по блоку, на пять секунд показывает сквозь
 * камень руды, воду и лаву вокруг — ценой пяти единиц прочности.
 */
public class ElementalPickaxeItem extends PickaxeItem {
    public ElementalPickaxeItem() {
        super(ModMaterials.ELEMENTAL_TOOL, 1, -2.8f, new Item.Properties().rarity(Rarity.RARE));
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        if (!player.level().isClientSide && (!(entity instanceof Player other) || player.canHarmPlayer(other))) {
            entity.setSecondsOnFire(2);
        }
        return super.onLeftClickEntity(stack, player, entity);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        context.getItemInHand().hurtAndBreak(5, player, broken -> broken.broadcastBreakEvent(context.getHand()));
        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    thaumcraft.client.render.OreScanOverlay.start(level, context.getClickedPos(), 5000L, 8));
        } else {
            level.playSound(null, context.getClickedPos(), ModSounds.WAND_FAIL.get(), SoundSource.PLAYERS, 0.2f,
                    0.2f + level.random.nextFloat() * 0.2f);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
