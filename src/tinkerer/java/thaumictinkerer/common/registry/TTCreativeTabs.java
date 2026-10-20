package thaumictinkerer.common.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import thaumictinkerer.ThaumicTinkerer;

/** Своя вкладка творческого режима: все предметы аддона в порядке регистрации. */
public final class TTCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ThaumicTinkerer.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.thaumictinkerer"))
            .withTabsBefore(thaumcraft.Thaumcraft.id("main"))
            .icon(() -> TTItems.ITEMS.getEntries().isEmpty() ? new ItemStack(thaumcraft.common.registry.ModItems.THAUMONOMICON.get())
                    : new ItemStack(TTItems.ITEMS.getEntries().iterator().next().get()))
            .displayItems((parameters, output) -> TTItems.ITEMS.getEntries().forEach(item -> {
                if (item.get() instanceof thaumictinkerer.common.item.InfusedSeedsItem) {
                    thaumcraft.api.aspects.Aspect.primals().forEach(aspect -> output.accept(thaumictinkerer.common.item.InfusedSeedsItem.of(aspect,
                            new thaumcraft.api.aspects.AspectList())));
                } else {
                    output.accept(item.get());
                }
            }))
            .build());

    private TTCreativeTabs() {
    }
}
