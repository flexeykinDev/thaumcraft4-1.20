package thaumictinkerer.common.item.kami;

import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thaumcraft.api.wands.WandCap;
import thaumcraft.api.wands.WandRod;
import thaumictinkerer.ThaumicTinkerer;
import thaumictinkerer.common.registry.TTItems;

import java.util.List;

/** Общее для раздела Ками: редкость, ихоровые набалдашник и стержень, осколки измерений (DimensionalShardDropHandler). */
@Mod.EventBusSubscriber(modid = ThaumicTinkerer.MODID)
public final class Kami {
    public static final Rarity RARITY = Rarity.create("THAUMICTINKERER_KAMI", ChatFormatting.LIGHT_PURPLE);

    /** CapIchor: вис на 30% дешевле. */
    public static final WandCap CAP = new WandCap("ichor", 0.7f, List.of(), 1f, 10, ThaumicTinkerer.MODID + ":ichorium_cap");
    /** RodIchorcloth: 1000 вис, светится. */
    public static final WandRod ROD = new WandRod("ichorcloth", 1000, 10, ThaumicTinkerer.MODID + ":ichorcloth_rod").glowing();

    private Kami() {
    }

    /** Вызывается при создании мода, чтобы набалдашник и стержень попали в списки Thaumcraft до загрузки рецептов. */
    public static void init() {
    }

    /** Эндермен в Энде роняет осколок Края с шансом 1/32, зомби-пиглин в Незере — осколок Незера с шансом 1/16. */
    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (!(event.getSource().getEntity() instanceof Player)) {
            return;
        }
        var entity = event.getEntity();
        ItemStack shard = ItemStack.EMPTY;
        if (entity instanceof EnderMan && entity.level().dimension() == Level.END && Math.random() <= 0.03125) {
            shard = new ItemStack(TTItems.ENDER_SHARD.get());
        } else if (entity instanceof ZombifiedPiglin && entity.level().dimension() == Level.NETHER && Math.random() <= 0.0625) {
            shard = new ItemStack(TTItems.NETHER_SHARD.get());
        }
        if (!shard.isEmpty()) {
            event.getDrops().add(new ItemEntity(entity.level(), entity.getX(), entity.getY(), entity.getZ(), shard));
        }
    }
}
