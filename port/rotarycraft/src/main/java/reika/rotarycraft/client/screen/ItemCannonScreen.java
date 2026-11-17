/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.menu.ItemCannonMenu;
import reika.rotarycraft.network.RotaryNetwork;

/** Original: GuiItemCannon (targetgui.png) - fields for the target's x, y, z and dimension above the 9 slots, with the power tab. */
public class ItemCannonScreen extends AbstractContainerScreen<ItemCannonMenu> {

	private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/gui/targetgui.png");
	private static final ResourceLocation POWERTAB = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/gui/powertab.png");

	private EditBox[] inputs;

	public ItemCannonScreen(ItemCannonMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		imageWidth = 176;
		imageHeight = 170;
		inventoryLabelY = 78;
	}

	@Override
	protected void init() {
		super.init();
		BlockPos t = menu.tile.getTarget();
		int[] vals = t == null ? new int[] {0, 0, 0, 0} : new int[] {t.getX(), t.getY(), t.getZ(), menu.tile.getTargetDimensionId()};
		int[][] at = {{imageWidth/2+25, 26}, {imageWidth/2+25, 42}, {imageWidth/2+25, 58}, {imageWidth/4-36, 50}};
		inputs = new EditBox[4];
		for (int i = 0; i < 4; i++) {
			EditBox e = new EditBox(font, leftPos+at[i][0], topPos+at[i][1]-12, i == 3 ? 26 : 46, 16, Component.empty());
			e.setMaxLength(i == 3 ? 3 : 8);
			e.setFilter(s -> s.matches("-?[0-9]*"));
			e.setValue(t == null ? "" : String.valueOf(vals[i]));
			e.setResponder(s -> this.send());
			inputs[i] = this.addRenderableWidget(e);
		}
	}

	/** Like the original: sent once every field holds a number. */
	private void send() {
		int[] v = new int[4];
		for (int i = 0; i < 4; i++) {
			try {
				v[i] = Integer.parseInt(inputs[i].getValue());
			}
			catch (NumberFormatException e) {
				return;
			}
		}
		PacketDistributor.sendToServer(new RotaryNetwork.CannonTarget(menu.tile.getBlockPos(), v[3], new BlockPos(v[0], v[1], v[2])));
	}

	@Override
	public boolean keyPressed(int key, int scan, int mods) {
		for (EditBox e : inputs)
			if (e.isFocused() && key != 256) //not Escape: typing must not close the GUI with the inventory key
				return e.keyPressed(key, scan, mods) || e.canConsumeInput() || super.keyPressed(key, scan, mods);
		return super.keyPressed(key, scan, mods);
	}

	@Override
	protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
		int j = leftPos, k = topPos;
		g.blit(TEX, j, k, 0, 0, imageWidth, imageHeight);
		g.blit(POWERTAB, j+imageWidth, k+4, 0, 4, 42, 24);
		g.blit(POWERTAB, j+imageWidth, k+4+23, 0, 157, 42, 6);
		g.blit(POWERTAB, j+imageWidth+5, k+22, 0, 0, (int)menu.tile.getScaledPower(29), 4);
		g.drawString(font, "Power:", j+imageWidth+20-font.width("Power:")/2, k+9, 0xff000000, false);
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mx, int my) {
		super.renderLabels(g, mx, my);
		g.drawString(font, "Target X", imageWidth/3+10, 18, 0x404040, false);
		g.drawString(font, "Target Y", imageWidth/3+10, 34, 0x404040, false);
		g.drawString(font, "Target Z", imageWidth/3+10, 51, 0x404040, false);
		g.drawString(font, "Target Dim", imageWidth/4-38, 26, 0x404040, false);
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float partial) {
		super.render(g, mx, my, partial);
		this.renderTooltip(g, mx, my);
	}
}
