/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.screen;

import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.network.PacketDistributor;

import reika.rotarycraft.content.blockentity.BevelGearBlockEntity;
import reika.rotarycraft.content.blockentity.BevelGearBlockEntity.IOPair;
import reika.rotarycraft.content.menu.MachineMenu;
import reika.rotarycraft.network.RotaryNetwork;

/**
 * Original: Reika.RotaryCraft.GUIs.Machine.GuiBevel - pick input and output side; the neighbouring blocks are shown next to the buttons.
 * The coloured side buttons are drawn from bevelgui2.png like the original ImagedGuiButtons; colour-blind mode is not ported yet.
 */
public class BevelScreen extends MachineScreen {

	private Direction in;
	private Direction out;
	private int posn;

	public BevelScreen(MachineMenu m, Inventory inv, Component t) {
		super(m, inv, t, "bevelgui2", 176, 192);
	}

	private BevelGearBlockEntity bevel() {
		return (BevelGearBlockEntity)menu.tile;
	}

	@Override
	protected void init() {
		super.init();
		if (!(menu.tile instanceof BevelGearBlockEntity))
			return;
		posn = bevel().direction;
		IOPair dirs = BevelGearBlockEntity.getDirectionMap().get(posn);
		in = dirs.read();
		out = dirs.write();
		this.rebuild();
	}

	private void rebuild() {
		this.clearWidgets();
		int j = leftPos-2;
		int k = topPos-12;
		for (int i = 0; i < 6; i++) {
			Direction d = Direction.from3DDataValue(i);
			String s = d.name().substring(0, 1);
			final int id = i;
			this.addRenderableWidget(new SideButton(j+40, k+8+48+i*22, in == d ? 194 : 176, i*18, s, () -> this.pick(id)));
			boolean valid = BevelGearBlockEntity.isValid(in, d);
			this.addRenderableWidget(new SideButton(j+imageWidth-40-18, k+8+48+i*22, valid ? (out == d ? 194 : 176) : 212, valid ? i*18 : 0, s, () -> this.pick(id+6)));
		}
	}

	private void pick(int id) {
		if (id < 6) {
			in = Direction.from3DDataValue(id);
			if (!BevelGearBlockEntity.isValid(in, out))
				out = in.getStepY() != 0 ? Direction.EAST : Direction.DOWN;
		}
		else {
			Direction d = Direction.from3DDataValue(id-6);
			if (!BevelGearBlockEntity.isValid(in, d))
				return;
			out = d;
		}
		List<IOPair> map = BevelGearBlockEntity.getDirectionMap();
		int idx = map.indexOf(new IOPair(in, out));
		if (idx < 0)
			return;
		posn = idx;
		bevel().direction = posn;
		PacketDistributor.sendToServer(new RotaryNetwork.BevelDirection(bevel().getBlockPos(), posn));
		this.rebuild();
	}

	/** Original: DragonAPI ImagedGuiButton - an 18x18 image from bevelgui2.png with the side's initial on it. */
	private class SideButton extends AbstractButton {

		private final int u, v;
		private final Runnable action;

		SideButton(int x, int y, int u, int v, String label, Runnable action) {
			super(x, y, 18, 18, Component.literal(label));
			this.u = u;
			this.v = v;
			this.action = action;
		}

		@Override
		public void onPress() {
			action.run();
		}

		@Override
		protected void renderWidget(GuiGraphics g, int mx, int my, float partial) {
			BevelScreen.this.blitTex(g, this.getX(), this.getY(), u, v, 18, 18);
			g.drawString(BevelScreen.this.font, this.getMessage(), this.getX()+9-BevelScreen.this.font.width(this.getMessage())/2, this.getY()+5, 0, false);
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput out) {
			this.defaultButtonNarrationText(out);
		}
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mx, int my) {
		super.renderLabels(g, mx, my);
		g.drawString(font, "Input Side", 24, 32, 4210752, false);
		g.drawString(font, "Output Side", 99, 32, 4210752, false);
		if (!(menu.tile instanceof BevelGearBlockEntity bev) || bev.getLevel() == null)
			return;
		for (int i = 0; i < 6; i++) {
			BlockPos p = bev.getBlockPos().relative(Direction.from3DDataValue(i));
			BlockState bs = bev.getLevel().getBlockState(p);
			if (!bs.isAir()) {
				ItemStack drop = bs.getCloneItemStack(null, bev.getLevel(), p, minecraft.player);
				if (!drop.isEmpty())
					g.renderItem(drop, 15, 45+i*22);
			}
		}
	}
}
