package com.mahghuuuls.foodtimer.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Client-side renderer for variant-specific food cooldown overlays on hotbar and inventory GUI slots.
 */
public class ClientCooldownRenderer {

    public static void drawCooldownOverlay(int x, int y, float fraction) {
        if (fraction <= 0.0F) {
            return;
        }

        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO
        );

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        int yStart = y + MathHelper.floor(16.0F * (1.0F - fraction));
        int height = MathHelper.ceil(16.0F * fraction);

        buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(x, yStart + height, 0.0D).color(255, 255, 255, 127).endVertex();
        buffer.pos(x + 16, yStart + height, 0.0D).color(255, 255, 255, 127).endVertex();
        buffer.pos(x + 16, yStart, 0.0D).color(255, 255, 255, 127).endVertex();
        buffer.pos(x, yStart, 0.0D).color(255, 255, 255, 127).endVertex();
        tessellator.draw();

        GlStateManager.enableTexture2D();
        GlStateManager.enableDepth();
        GlStateManager.disableLighting();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @SubscribeEvent
    public void onRenderHotbar(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.HOTBAR) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) {
            return;
        }

        ScaledResolution resolution = event.getResolution();
        int width = resolution.getScaledWidth();
        int height = resolution.getScaledHeight();
        int midX = width / 2;

        boolean renderedAny = false;

        // Main hand hotbar slots 0 to 8:
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.inventory.mainInventory.get(i);
            if (!stack.isEmpty() && ClientCooldownTracker.hasCooldown(stack)) {
                float fraction = ClientCooldownTracker.getCooldownFraction(stack, event.getPartialTicks());
                int slotX = midX - 90 + i * 20 + 3;
                int slotY = height - 16 - 3;
                drawCooldownOverlay(slotX, slotY, fraction);
                renderedAny = true;
            }
        }

        // Offhand slot:
        ItemStack offhand = mc.player.getHeldItemOffhand();
        if (!offhand.isEmpty() && ClientCooldownTracker.hasCooldown(offhand)) {
            float fraction = ClientCooldownTracker.getCooldownFraction(offhand, event.getPartialTicks());
            EnumHandSide primaryHand = mc.player.getPrimaryHand();
            int offhandX = (primaryHand == EnumHandSide.RIGHT) ? (midX - 90 - 29 + 3) : (midX + 90 + 13 + 3);
            int offhandY = height - 16 - 3;
            drawCooldownOverlay(offhandX, offhandY, fraction);
            renderedAny = true;
        }

        if (renderedAny) {
            GlStateManager.disableLighting();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    @SubscribeEvent
    public void onDrawScreen(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (event.getGui() instanceof GuiContainer) {
            GuiContainer gui = (GuiContainer) event.getGui();
            int guiLeft = gui.getGuiLeft();
            int guiTop = gui.getGuiTop();

            for (Slot slot : gui.inventorySlots.inventorySlots) {
                if (slot != null && slot.getHasStack()) {
                    ItemStack stack = slot.getStack();
                    if (ClientCooldownTracker.hasCooldown(stack)) {
                        float fraction = ClientCooldownTracker.getCooldownFraction(stack, Minecraft.getMinecraft().getRenderPartialTicks());
                        drawCooldownOverlay(guiLeft + slot.xPos, guiTop + slot.yPos, fraction);
                    }
                }
            }

            GlStateManager.disableLighting();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    @SubscribeEvent
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getWorld().isRemote) {
            if (ClientCooldownTracker.hasCooldown(event.getItemStack())) {
                event.setCancellationResult(net.minecraft.util.EnumActionResult.FAIL);
                event.setCanceled(true);
            }
        }
    }
}
