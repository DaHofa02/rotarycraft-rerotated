/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.SpyCamBlockEntity;
import reika.rotarycraft.content.menu.SpyCamViewMenu;

/**
 * Original: Reika.RotaryCraft.GUIs.Machine.Inventory.GuiSpyCam (222x222, spycamgui.png) - the camera's top-down map in 4x4 pixel cells,
 * darker the deeper the surface lies below the camera, and an 8x8 icon (mobicons.png, by 1.7.10 entity id) for each mob on it.
 */
public class SpyCamViewScreen extends AbstractContainerScreen<SpyCamViewMenu> {

	private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/gui/spycamgui.png");
	private static final ResourceLocation ICONS = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/gui/mobicons.png");

	public SpyCamViewScreen(SpyCamViewMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		imageWidth = 222;
		imageHeight = 222;
		inventoryLabelY = imageHeight+100;
	}

	@Override
	protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
		g.blit(TEX, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
		SpyCamBlockEntity cam = menu.tile;
		if (cam == null)
			return;
		int a = leftPos, b = topPos+1;
		int[] bounds = cam.getBounds();
		int max = bounds[1]*4;
		int y = cam.getBlockPos().getY();
		for (int i = bounds[0]; i <= bounds[1]; i++) {
			for (int j = bounds[0]; j <= bounds[1]; j++) {
				float br = y == 0 ? 1 : 1-(float)(y-cam.getHeightAt(i, j))/y*1.25F;
				br = Math.max(0, Math.min(1, br));
				int c = cam.getTopBlockColorAt(i, j);
				int r = (int)(((c >> 16) & 255)*br), gr = (int)(((c >> 8) & 255)*br), bl = (int)((c & 255)*br);
				g.fill(a+17+max-(4+4*j), b+19+4*i, a+17+max-4*j, b+19+4*i+4, 0xFF000000 | r << 16 | gr << 8 | bl);
			}
		}
		for (int i = bounds[0]; i <= bounds[1]; i++)
			for (int j = bounds[0]; j <= bounds[1]; j++) {
				int id = cam.getMobAt(i, j);
				if (id == 0)
					continue;
				int v = 8*(id/16), u = 8*(id%16);
				g.blit(ICONS, a+13+max-4*i-2, b+19+4*j-2, u, v, 8, 8);
			}
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mx, int my) {
		g.drawString(font, title, imageWidth/2-font.width(title)/2, 5, 4210752, false);
	}
}
