package thaumcraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/** Рисование иконок аспектов в интерфейсе. */
public final class AspectRenderer {
    public static final int ICON = 16;
    public static final int STEP = 18;

    /** Иконка 16x16 в цвете аспекта; amount &gt; 0 подписывается в правом нижнем углу. */
    public static void draw(GuiGraphics graphics, Font font, Aspect aspect, int amount, int x, int y, float alpha) {
        draw(graphics, font, aspect, amount > 0 ? Integer.toString(amount) : "", x, y, alpha);
    }

    /** То же с готовой подписью: для дробных количеств. */
    public static void draw(GuiGraphics graphics, Font font, Aspect aspect, String text, int x, int y, float alpha) {
        int color = aspect.getColor();
        RenderSystem.enableBlend();
        graphics.setColor((color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f, alpha);
        graphics.blit(aspect.getImage(), x, y, ICON, ICON, 0f, 0f, 1, 1, 1, 1);
        graphics.setColor(1f, 1f, 1f, 1f);

        if (!text.isEmpty()) {
            PoseStack pose = graphics.pose();
            pose.pushPose();
            pose.translate(x + ICON, y + ICON, 10f);
            pose.scale(0.5f, 0.5f, 1f);
            int textColor = 0xffffff | (int) (alpha * 255f) << 24;
            graphics.drawString(font, text, -font.width(text), -font.lineHeight, textColor, true);
            pose.popPose();
        }
    }

    /** Ряд иконок, начиная с x. */
    public static void drawRow(GuiGraphics graphics, Font font, AspectList aspects, int x, int y) {
        for (Aspect aspect : aspects.getAspectsSortedByName()) {
            draw(graphics, font, aspect, aspects.getAmount(aspect), x, y, 1f);
            x += STEP;
        }
    }

    public static int rowWidth(AspectList aspects) {
        return aspects.size() * STEP;
    }

    private AspectRenderer() {
    }
}
