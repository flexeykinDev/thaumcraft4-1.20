package thaumcraft.common.registry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.common.SoundActions;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import thaumcraft.Thaumcraft;

import java.util.function.Consumer;

/** Жидкости мода. Очищающая жидкость (FLUIDPURE оригинала) — настоящая жидкость: течёт как вода, льётся по чужим трубам. */
public final class ModFluids {
    public static final DeferredRegister<FluidType> TYPES = DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, Thaumcraft.MODID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(ForgeRegistries.FLUIDS, Thaumcraft.MODID);

    private static final ResourceLocation PURE_TEXTURE = Thaumcraft.id("block/fluidpure");

    public static final RegistryObject<FluidType> PURIFYING_TYPE = TYPES.register("purifying_fluid",
            () -> new FluidType(FluidType.Properties.create().descriptionId("block.thaumcraft.purifying_fluid").lightLevel(10)
                    .viscosity(1000).density(1000).rarity(Rarity.RARE).canSwim(true).canDrown(true).canExtinguish(true)
                    .supportsBoating(true).fallDistanceModifier(0f)
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL).sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)) {
                @Override
                public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                    consumer.accept(new IClientFluidTypeExtensions() {
                        @Override
                        public ResourceLocation getStillTexture() {
                            return PURE_TEXTURE;
                        }

                        @Override
                        public ResourceLocation getFlowingTexture() {
                            return PURE_TEXTURE;
                        }
                    });
                }
            });

    public static final RegistryObject<FlowingFluid> PURIFYING = FLUIDS.register("purifying_fluid",
            () -> new ForgeFlowingFluid.Source(purifying()));
    public static final RegistryObject<FlowingFluid> PURIFYING_FLOWING = FLUIDS.register("flowing_purifying_fluid",
            () -> new ForgeFlowingFluid.Flowing(purifying()));

    private static ForgeFlowingFluid.Properties purifying() {
        return new ForgeFlowingFluid.Properties(PURIFYING_TYPE, PURIFYING, PURIFYING_FLOWING)
                .block(() -> (LiquidBlock) ModBlocks.PURIFYING_FLUID.get()).bucket(ModItems.BUCKET_PURE);
    }

    private ModFluids() {
    }
}
