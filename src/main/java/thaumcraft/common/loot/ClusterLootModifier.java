package thaumcraft.common.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import thaumcraft.common.items.tools.ElementalPickaxeItem;
import thaumcraft.common.items.tools.ToolHelper;

import java.util.function.Supplier;

/**
 * Самородные грозди из-под кирки ядра — перенос EventHandlerWorld.harvestEvent оригинала: каждая выпавшая из блока
 * руда с шансом 20% (плюс 7.5% за уровень «Удачи») заменяется гроздью, из которой выплавляется два слитка.
 */
public class ClusterLootModifier extends LootModifier {
    public static final Supplier<Codec<ClusterLootModifier>> CODEC = () ->
            RecordCodecBuilder.create(inst -> codecStart(inst).apply(inst, ClusterLootModifier::new));

    public ClusterLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        ItemStack tool = context.getParamOrNull(LootContextParams.TOOL);
        if (tool == null || !(tool.getItem() instanceof ElementalPickaxeItem) || loot.isEmpty()
                || context.getParamOrNull(LootContextParams.BLOCK_STATE) == null
                || !(context.getParamOrNull(LootContextParams.THIS_ENTITY) instanceof Player)) {
            return loot;
        }
        float chance = 0.2f + tool.getEnchantmentLevel(Enchantments.BLOCK_FORTUNE) * 0.075f;
        for (int i = 0; i < loot.size(); i++) {
            ItemStack drop = loot.get(i);
            ItemStack special = ToolHelper.specialMiningResult(drop, chance, context.getRandom());
            if (special != drop) {
                loot.set(i, special);
                Vec3 origin = context.getParamOrNull(LootContextParams.ORIGIN);
                if (origin != null) {
                    context.getLevel().playSound(null, BlockPos.containing(origin), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS,
                            0.2f, 0.7f + context.getRandom().nextFloat() * 0.2f);
                }
            }
        }
        return loot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC.get();
    }
}
