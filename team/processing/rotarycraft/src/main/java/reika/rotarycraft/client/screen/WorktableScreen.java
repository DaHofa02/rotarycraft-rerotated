/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.WorktableBlockEntity;
import reika.rotarycraft.content.menu.WorktableMenu;
import reika.rotarycraft.content.recipe.WorktableRecipe;

/**
 * Original: GuiWorktable (worktablegui2.png). While the outputs are empty, the arrow is shown and the result of the current recipe is
 * drawn in the main output slot (click it to craft).
 */
public class WorktableScreen extends AbstractContainerScreen<WorktableMenu> {

	private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/gui/worktablegui2.png");

	public WorktableScreen(WorktableMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		imageWidth = 176;
		imageHeight = 166;
	}

	private boolean isReadyToCraft() {
		for (int i = 9; i < 18; i++)
			if (menu.slots.get(i).hasItem())
				return false;
		return true;
	}

	@Override
	protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
		int j = leftPos, k = topPos;
		g.blit(TEX, j, k, 0, 0, imageWidth, imageHeight);
		if (!this.isReadyToCraft())
			return;
		g.blit(TEX, j+79, k+35, 176, 35, 18, 15);
		WorktableRecipe wr = WorktableBlockEntity.findMatchingRecipe(minecraft.level, menu.tile.getInventory());
		if (wr != null) {
			ItemStack out = wr.getResultItem(minecraft.level.registryAccess());
			g.renderItem(out, j+116, k+35);
			g.renderItemDecorations(font, out, j+116, k+35);
			if (mx >= j+116 && mx < j+132 && my >= k+35 && my < k+51)
				g.renderTooltip(font, out, mx, my);
		}
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mx, int my) {
		g.drawString(font, title, imageWidth/2-font.width(title)/2, 5, 4210752, false);
		g.drawString(font, playerInventoryTitle, imageWidth-58, imageHeight-96+3, 4210752, false);
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float partial) {
		super.render(g, mx, my, partial);
		this.renderTooltip(g, mx, my);
	}
}
