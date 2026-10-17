package thaumcraft.common.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import thaumcraft.Thaumcraft;
import thaumcraft.api.wands.WandCap;
import thaumcraft.api.wands.WandRod;
import thaumcraft.common.items.ResearchNotesItem;
import thaumcraft.common.items.wands.WandItem;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Thaumcraft.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.thaumcraft"))
            .icon(() -> new ItemStack(ModItems.SHARD_BALANCED.get()))
            .displayItems((parameters, output) -> {
                for (RegistryObject<Item> item : ModItems.ITEMS.getEntries()) {
                    if (item.get() instanceof WandItem) {
                        // как в оригинале: три заряженных жезла разной силы
                        output.accept(WandItem.create(item.get(), WandRod.WOOD, WandCap.IRON, true));
                        output.accept(WandItem.create(item.get(), WandRod.GREATWOOD, WandCap.GOLD, true));
                        output.accept(WandItem.create(item.get(), WandRod.SILVERWOOD, WandCap.THAUMIUM, true));
                        output.accept(WandItem.create(item.get(), WandRod.BLAZE, WandCap.THAUMIUM, true));
                        // как в оригинале: скипетр и посох
                        ItemStack sceptre = WandItem.create(item.get(), WandRod.SILVERWOOD, WandCap.THAUMIUM, false);
                        WandItem.setSceptre(sceptre);
                        for (thaumcraft.api.aspects.Aspect primal : thaumcraft.api.aspects.Aspect.primals()) {
                            WandItem.storeVis(sceptre, primal, WandItem.getMaxVis(sceptre));
                        }
                        output.accept(sceptre);
                        output.accept(WandItem.create(item.get(), WandRod.SILVERWOOD_STAFF, WandCap.THAUMIUM, true));
                        output.accept(WandItem.create(item.get(), WandRod.PRIMAL_STAFF, WandCap.THAUMIUM, true));
                    } else if (!metalPresent(item.getId().getPath())) {
                        // олова, серебра или свинца в этой игре нет — как в оригинале, их вещей во вкладке нет
                        continue;
                    } else if (item.get() instanceof thaumcraft.common.items.NodeJarItem) {
                        // как в оригинале: пустой банки с узлом во вкладке нет — её получают, запирая узел
                        continue;
                    } else if (item.get() instanceof ResearchNotesItem) {
                        // пустая заметка бессмысленна; во вкладке лежит «неизвестное знание» из девяти фрагментов
                        output.accept(ResearchNotesItem.unknown(item.get()));
                    } else {
                        output.accept(item.get());
                    }
                }
            })
            .build());

    public static boolean metalPresent(String name) {
        for (String metal : new String[]{"tin", "silver", "lead"}) {
            if (name.equals("cluster_" + metal) || name.equals(metal + "_nugget") || name.startsWith("wand_cap_" + metal)) {
                return thaumcraft.common.crafting.MetalCompat.hasMetal(metal);
            }
        }
        return true;
    }

    private ModCreativeTabs() {
    }
}
