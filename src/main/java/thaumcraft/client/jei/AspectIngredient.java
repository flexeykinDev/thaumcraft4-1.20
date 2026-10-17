package thaumcraft.client.jei;

import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientRenderer;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.subtypes.UidContext;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.TooltipFlag;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.client.ClientKnowledge;
import thaumcraft.client.gui.AspectRenderer;
import thaumcraft.common.Res;

import java.util.ArrayList;
import java.util.List;

/**
 * Аспект как ингредиент JEI (как в Thaumic JEI для оригинала): у него есть своя иконка в списке, а клавиши R и U
 * показывают, где его взять (предметы, состав) и на что он тратится (верстак, тигель, наполнение).
 */
public final class AspectIngredient {
    /** Аспект и количество; 0 — без подписи числа (так аспекты лежат в общем списке). */
    public record AspectStack(Aspect aspect, int amount) {
    }

    public static final IIngredientType<AspectStack> TYPE = () -> AspectStack.class;

    static List<AspectStack> all() {
        List<AspectStack> list = new ArrayList<>();
        for (Aspect aspect : Aspect.all()) {
            list.add(new AspectStack(aspect, 0));
        }
        return list;
    }

    static List<AspectStack> of(AspectList aspects) {
        List<AspectStack> list = new ArrayList<>();
        for (Aspect aspect : aspects.getAspectsSortedByAmount()) {
            list.add(new AspectStack(aspect, aspects.getAmount(aspect)));
        }
        return list;
    }

    static final class Helper implements IIngredientHelper<AspectStack> {
        @Override
        public IIngredientType<AspectStack> getIngredientType() {
            return TYPE;
        }

        @Override
        public String getDisplayName(AspectStack stack) {
            return stack.aspect().getName().getString();
        }

        @Override
        public String getUniqueId(AspectStack stack, UidContext context) {
            return "thaumcraft:aspect/" + stack.aspect().getTag();
        }

        @Override
        public ResourceLocation getResourceLocation(AspectStack stack) {
            return Res.of("thaumcraft", stack.aspect().getTag());
        }

        @Override
        public AspectStack copyIngredient(AspectStack stack) {
            return stack;
        }

        @Override
        public long getAmount(AspectStack stack) {
            return Math.max(1, stack.amount());
        }

        @Override
        public AspectStack copyWithAmount(AspectStack stack, long amount) {
            return new AspectStack(stack.aspect(), (int) amount);
        }

        @Override
        public Iterable<Integer> getColors(AspectStack stack) {
            return List.of(stack.aspect().getColor());
        }

        @Override
        public String getErrorInfo(AspectStack stack) {
            return stack == null ? "null" : "aspect " + stack.aspect().getTag() + " x" + stack.amount();
        }
    }

    static final class Renderer implements IIngredientRenderer<AspectStack> {
        @Override
        public void render(GuiGraphics graphics, AspectStack stack) {
            AspectRenderer.draw(graphics, Minecraft.getInstance().font, stack.aspect(), stack.amount(), 0, 0, 1f);
        }

        @Override
        @SuppressWarnings("removal")
        public List<Component> getTooltip(AspectStack stack, TooltipFlag flag) {
            Aspect aspect = stack.aspect();
            List<Component> lines = new ArrayList<>();
            lines.add(aspect.getName().copy().withStyle(ChatFormatting.AQUA));
            lines.add(aspect.getDescription().copy().withStyle(ChatFormatting.GRAY));
            if (aspect.isPrimal()) {
                lines.add(Component.translatable("jei.thaumcraft.primal").withStyle(ChatFormatting.DARK_AQUA));
            } else {
                lines.add(aspect.getFirstComponent().getName().copy().append(" + ").append(aspect.getSecondComponent().getName())
                        .withStyle(ChatFormatting.DARK_AQUA));
            }
            if (!ClientKnowledge.get().hasDiscovered(aspect)) {
                lines.add(Component.translatable("jei.thaumcraft.undiscovered").withStyle(ChatFormatting.DARK_RED));
            }
            return lines;
        }
    }

    private AspectIngredient() {
    }
}
