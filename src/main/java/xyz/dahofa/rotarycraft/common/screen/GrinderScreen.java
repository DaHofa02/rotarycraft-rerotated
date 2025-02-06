package xyz.dahofa.rotarycraft.common.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import xyz.dahofa.rotarycraft.api.lib.Names;

public class GrinderScreen extends AbstractContainerScreen<GrinderMenu> {

    private static final ResourceLocation GUI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Names.MOD_ID, "textures/gui/grinder.png");

    private static final ResourceLocation POWER_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Names.MOD_ID, "textures/gui/powertab.png");

    private int x;
    private int y;
    private int powerTabX;
    private int powerTabY;
    private int powerTabWidth;
    private int powerTabHeight;
    private Component powerTabPowerText;
    private Component powerTabSpeedText;
    private Component powerTabTorqueText;

    public GrinderScreen(GrinderMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        super.init();


        this.x = (width - imageWidth) / 2;
        this.y = (height - imageHeight) / 2;
        this.titleLabelX = imageHeight / 2;
        this.inventoryLabelX = 116;
        this.powerTabX = x + imageWidth;
        this.powerTabY = y + 5;
        this.powerTabWidth = 42;
        this.powerTabHeight = 159;
        this.powerTabPowerText = Component.literal("Power:");
        this.powerTabSpeedText = Component.literal("Speed: ");
        this.powerTabTorqueText = Component.literal("Torque: ");
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float v, int i, int i1) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, GUI_TEXTURE);

        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, imageWidth, imageHeight);
        guiGraphics.blit(POWER_TEXTURE, powerTabX, powerTabY, 0, 4, powerTabWidth, powerTabHeight, 256, 256);

        renderProgressArrow(guiGraphics, x, y);
    }

    private void renderProgressArrow(GuiGraphics guiGraphics, int x, int y) {
        if (menu.isCrafting()) {
            guiGraphics.blit(GUI_TEXTURE, x + 99, y + 34, 176, 13, menu.getScaledArrowProgress(), 17, 256, 256);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, centerText(this.title, 0), this.titleLabelY, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);

        guiGraphics.drawString(this.font, this.powerTabPowerText, centerText(this.powerTabPowerText, (imageWidth / 2) + (powerTabWidth / 2)), 10, 4210752, false);
        guiGraphics.drawString(this.font, this.powerTabSpeedText, centerText(this.powerTabSpeedText, (imageWidth / 2) + (powerTabWidth / 2)), this.inventoryLabelY - 4, 4210752, false);
        guiGraphics.drawString(this.font, this.powerTabTorqueText, centerText(this.powerTabTorqueText, (imageWidth / 2) + (powerTabWidth / 2)), (this.inventoryLabelY * 2) - 14, 4210752, false);

    }

    public int centerText(Component text, int offset) {
        int textWidth = font.width(text);

        return ((imageWidth - textWidth) / 2) + offset;
    }
}
