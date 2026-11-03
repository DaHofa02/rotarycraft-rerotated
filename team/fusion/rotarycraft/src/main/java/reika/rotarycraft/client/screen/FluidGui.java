/*******************************************************************************
 * @author Reika Kalseki (original DragonAPI). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.Fluid;

import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

/** Original: ReikaLiquidRenderer.bindFluidTexture + drawTexturedModelRectFromIcon - a fluid's still icon drawn into a GUI. */
public final class FluidGui {

	private FluidGui() {}

	/** Draws the fluid's still texture (tinted with its colour) into the rectangle, cut to the given height from the bottom. */
	public static void drawFluid(GuiGraphics g, Fluid f, int x, int y, int w, int h) {
		if (h <= 0)
			return;
		IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(f);
		TextureAtlasSprite ico = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ext.getStillTexture());
		int c = ext.getTintColor();
		g.blit(x, y, 0, w, h, ico, ((c >> 16) & 255)/255F, ((c >> 8) & 255)/255F, (c & 255)/255F, 1F);
	}
}
