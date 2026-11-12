/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.ParticleEmitterBlockEntity;
import reika.rotarycraft.content.menu.ParticleEmitterMenu;

/**
 * Original: GuiParticle (particlegui.png, 194 high) - one 18x18 button (buttons.png at 0,36) per valid particle kind with its icon from
 * particles.png: two rows of six with a gap for the coil slot, then rows of eight.
 */
public class ParticleEmitterScreen extends AbstractContainerScreen<ParticleEmitterMenu> {

	private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/gui/particlegui.png");
	private static final ResourceLocation BUTTONS = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/gui/buttons.png");
	private static final ResourceLocation ICONS = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/gui/particles.png");

	public ParticleEmitterScreen(ParticleEmitterMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		imageWidth = 176;
		imageHeight = 194;
		inventoryLabelY = imageHeight-94;
	}

	/** Original button layout: {x, y} relative to the GUI for button i. */
	public static int[] buttonPos(int i) {
		int dx = i%6;
		int dy = i/6;
		int x = 8+dx*20;
		if (dx >= 3 && dy < 2)
			x += 41;
		if (dy >= 2) {
			dx = (i-12)%8;
			x = 8+dx*20;
			dy = 2+(i-12)/8;
		}
		return new int[] {x, 19+dy*20};
	}

	@Override
	protected void init() {
		super.init();
		for (int i = 0; i < ParticleEmitterMenu.KINDS.size(); i++) {
			int[] p = buttonPos(i);
			int id = i;
			ParticleEmitterBlockEntity.Kind k = ParticleEmitterMenu.KINDS.get(i);
			this.addRenderableWidget(new AbstractButton(leftPos+p[0], topPos+p[1], 18, 18, Component.literal(k.name())) {
				@Override
				public void onPress() {
					minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
				}

				@Override
				protected void renderWidget(GuiGraphics g, int mx, int my, float partial) {
					g.blit(BUTTONS, this.getX(), this.getY(), 0, 36, 18, 18);
					g.blit(ICONS, this.getX()+2, this.getY()+2, 16*(k.ordinal()%16), 16*(k.ordinal()/16), 16, 16);
					if (menu.tile.particleType == k || this.isHovered())
						g.renderOutline(this.getX(), this.getY(), 18, 18, menu.tile.particleType == k ? 0xffffffff : 0x80ffffff);
				}

				@Override
				protected void updateWidgetNarration(NarrationElementOutput out) {
					this.defaultButtonNarrationText(out);
				}
			});
		}
	}

	@Override
	protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
		g.blit(TEX, leftPos, topPos, 0, 0, imageWidth, imageHeight);
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float partial) {
		super.render(g, mx, my, partial);
		this.renderTooltip(g, mx, my);
	}
}
