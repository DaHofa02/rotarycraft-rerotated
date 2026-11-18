/*******************************************************************************
 * Private NeoForge 1.21.1 port of RotaryCraft; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.network.PacketDistributor;

import reika.rotarycraft.content.blockentity.AimedCannonBlockEntity;
import reika.rotarycraft.content.menu.AmmoMenu;
import reika.rotarycraft.network.RotaryNetwork;

/** Own drawn layout: the ammo slots; for cannons also x/y/z target fields with a Set button. */
public class AmmoScreen extends AbstractContainerScreen<AmmoMenu> {

	private EditBox[] fields;

	public AmmoScreen(AmmoMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		imageWidth = 176;
		imageHeight = 166;
		inventoryLabelY = 72;
	}

	@Override
	protected void init() {
		super.init();
		if (!(menu.tile instanceof AimedCannonBlockEntity c))
			return;
		BlockPos t = c.getTarget();
		int[] v = t == null ? new int[] {c.getBlockPos().getX(), c.getBlockPos().getY(), c.getBlockPos().getZ()} : new int[] {t.getX(), t.getY(), t.getZ()};
		fields = new EditBox[3];
		for (int i = 0; i < 3; i++) {
			fields[i] = new EditBox(font, leftPos+8+i*44, topPos+22, 40, 14, Component.literal("xyz".substring(i, i+1)));
			fields[i].setFilter(s -> s.matches("-?\\d{0,8}"));
			fields[i].setValue(String.valueOf(v[i]));
			this.addRenderableWidget(fields[i]);
		}
		this.addRenderableWidget(Button.builder(Component.literal("Set"), b -> this.send()).bounds(leftPos+142, topPos+20, 26, 18).build());
	}

	private void send() {
		int[] v = new int[3];
		for (int i = 0; i < 3; i++) {
			try {
				v[i] = Integer.parseInt(fields[i].getValue());
			}
			catch (NumberFormatException e) {
				return;
			}
		}
		PacketDistributor.sendToServer(new RotaryNetwork.CannonTarget(menu.tile.getBlockPos(), 0, new BlockPos(v[0], v[1], v[2])));
	}

	@Override
	public boolean keyPressed(int key, int scan, int mods) {
		if (fields != null && key != 256)
			for (EditBox f : fields)
				if (f.isFocused())
					return f.keyPressed(key, scan, mods) || true;
		return super.keyPressed(key, scan, mods);
	}

	@Override
	protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
		int x = leftPos, y = topPos;
		g.fill(x, y, x+imageWidth, y+imageHeight, 0xff2b2d33);
		g.fill(x+2, y+2, x+imageWidth-2, y+imageHeight-2, 0xff8e8c86);
		for (Slot s : menu.slots)
			g.fill(x+s.x-1, y+s.y-1, x+s.x+17, y+s.y+17, 0xff6e727a);
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float partial) {
		super.render(g, mx, my, partial);
		this.renderTooltip(g, mx, my);
	}
}
