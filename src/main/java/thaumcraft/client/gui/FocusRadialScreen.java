package thaumcraft.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import thaumcraft.Thaumcraft;
import thaumcraft.common.items.wands.FocusItem;
import thaumcraft.common.items.wands.WandItem;
import thaumcraft.common.network.ChangeFocusPacket;
import thaumcraft.common.network.ModNetwork;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Круговое меню выбора фокуса (REHWandHandler.renderFocusRadialHUD оригинала). Открыто, пока зажата клавиша
 * смены фокуса: фокусы из инвентаря стоят по кругу, в середине — надетый сейчас. Фокус выбирается щелчком
 * или отпусканием клавиши над ним.
 */
public class FocusRadialScreen extends Screen {
    private static final ResourceLocation RADIAL = Thaumcraft.id("textures/misc/radial.png");
    private static final ResourceLocation RADIAL2 = Thaumcraft.id("textures/misc/radial2.png");

    private final KeyMapping key;
    private final TreeMap<String, ItemStack> foci = new TreeMap<>();
    private final Map<String, Float> scales = new HashMap<>();
    private float hudScale;
    @Nullable
    private String hovered;

    private final boolean pinned;

    /** pinned — меню не закрывается само, когда клавиша отпущена (для снимков). */
    public FocusRadialScreen(KeyMapping key, boolean pinned) {
        super(Component.empty());
        this.key = key;
        this.pinned = pinned;
    }

    /** Фокусы, которые игрок может надеть прямо сейчас, по ключу сортировки. */
    public static TreeMap<String, ItemStack> collect(Player player) {
        TreeMap<String, ItemStack> out = new TreeMap<>();
        for (ItemStack pouch : thaumcraft.common.items.wands.FocusPouchItem.findPouches(player)) {
            for (ItemStack item : thaumcraft.common.items.wands.FocusPouchItem.getInventory(pouch)) {
                if (item.getItem() instanceof FocusItem focus) {
                    out.put(focus.getSortingHelper(item), item.copy());
                }
            }
        }
        for (ItemStack item : player.getInventory().items) {
            if (item.getItem() instanceof FocusItem focus) {
                out.put(focus.getSortingHelper(item), item.copy());
            }
        }
        return out;
    }

    @Override
    protected void init() {
        foci.clear();
        scales.clear();
        if (minecraft != null && minecraft.player != null) {
            foci.putAll(collect(minecraft.player));
        }
        for (String id : foci.keySet()) {
            scales.put(id, 1f);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private boolean keyHeld() {
        return pinned || key.isUnbound() || InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), key.getKey().getValue());
    }

    @Override
    public void tick() {
        if (minecraft == null || minecraft.player == null || !(minecraft.player.getMainHandItem().getItem() instanceof WandItem)) {
            onClose();
            return;
        }
        hudScale = Math.min(1f, hudScale + 0.2f);
        for (Map.Entry<String, Float> entry : scales.entrySet()) {
            boolean over = entry.getKey().equals(hovered);
            entry.setValue(Mth.clamp(entry.getValue() + (over ? 0.1f : -0.1f), 1f, 1.3f));
        }
        if (!keyHeld()) {
            choose();
        }
    }

    private void choose() {
        if (hovered != null) {
            ModNetwork.CHANNEL.sendToServer(new ChangeFocusPacket(hovered));
        }
        onClose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && hovered != null) {
            choose();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (minecraft == null || minecraft.player == null) {
            return;
        }
        ItemStack wand = minecraft.player.getMainHandItem();
        ItemStack current = wand.getItem() instanceof WandItem ? WandItem.getFocusStack(wand) : ItemStack.EMPTY;
        float cx = width / 2f;
        float cy = height / 2f;
        float ring = 16f + foci.size() * 2.5f;
        float spin = partialTick + minecraft.player.tickCount % 720 / 2f;
        float shown = Math.min(1f, hudScale + partialTick * 0.2f);

        // два полупрозрачных кольца, вращающихся навстречу друг другу
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(0.5f, 0.5f, 0.5f, 0.5f);
        drawRing(graphics, RADIAL, cx, cy, ring * 2.75f * shown, spin);
        drawRing(graphics, RADIAL2, cx, cy, ring * 2.55f * shown, -spin);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();

        ItemStack tooltip = ItemStack.EMPTY;
        if (!current.isEmpty()) {
            graphics.renderItem(current, (int) cx - 8, (int) cy - 8);
            if (Math.abs(mouseX - cx) <= 10 && Math.abs(mouseY - cy) <= 10) {
                tooltip = current;
            }
        }
        hovered = null;
        float angle = -90f * shown;
        float slice = 360f / Math.max(1, foci.size());
        for (Map.Entry<String, ItemStack> entry : foci.entrySet()) {
            float x = Mth.cos(angle * Mth.DEG_TO_RAD) * ring * shown;
            float y = Mth.sin(angle * Mth.DEG_TO_RAD) * ring * shown;
            angle += slice;
            float scale = scales.getOrDefault(entry.getKey(), 1f) * shown;
            graphics.pose().pushPose();
            graphics.pose().translate(cx + x, cy + y, 100f);
            graphics.pose().scale(scale, scale, 1f);
            graphics.renderItem(entry.getValue(), -8, -8);
            graphics.pose().popPose();
            if (Math.abs(mouseX - cx - x) <= 10 && Math.abs(mouseY - cy - y) <= 10) {
                hovered = entry.getKey();
                tooltip = entry.getValue();
            }
        }
        if (!tooltip.isEmpty()) {
            graphics.pose().pushPose();
            graphics.pose().translate(0f, 0f, 200f);
            graphics.renderTooltip(font, tooltip, mouseX, mouseY);
            graphics.pose().popPose();
        }
    }

    private static void drawRing(GuiGraphics graphics, ResourceLocation texture, float cx, float cy, float size, float degrees) {
        graphics.pose().pushPose();
        graphics.pose().translate(cx, cy, 0f);
        graphics.pose().mulPose(Axis.ZP.rotationDegrees(degrees));
        graphics.pose().scale(size / 256f, size / 256f, 1f);
        graphics.blit(texture, -128, -128, 0, 0f, 0f, 256, 256, 256, 256);
        graphics.pose().popPose();
    }
}
