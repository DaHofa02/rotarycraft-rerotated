/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.screen;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import net.neoforged.neoforge.network.PacketDistributor;

import reika.rotarycraft.content.blockentity.SplitterBlockEntity;
import reika.rotarycraft.content.menu.MachineMenu;
import reika.rotarycraft.network.RotaryNetwork;

/** Original: Reika.RotaryCraft.GUIs.Machine.GuiSplitter - the nine split ratios of a junction in split mode. */
public class SplitterScreen extends MachineScreen {

	public SplitterScreen(MachineMenu m, Inventory inv, Component t) {
		super(m, inv, t, "splittergui", 176, 140);
	}

	@Override
	protected void init() {
		super.init();
		int j = leftPos;
		int k = topPos;
		this.mode(32, j+8, k+31, "31:1 Inline");
		this.mode(16, j+8, k+51, "15:1 Inline");
		this.mode(8, j+8, k+71, "7:1 Inline");
		this.mode(4, j+8, k+91, "3:1 Inline");
		this.mode(1, j+52, k+113, "1:1 Even");
		this.mode(-32, j+96, k+31, "1:31 Bend");
		this.mode(-16, j+96, k+51, "1:15 Bend");
		this.mode(-8, j+96, k+71, "1:7 Bend");
		this.mode(-4, j+96, k+91, "1:3 Bend");
	}

	private void mode(int mode, int x, int y, String label) {
		this.addRenderableWidget(Button.builder(Component.literal(label), b -> {
			if (menu.tile instanceof SplitterBlockEntity spl) {
				PacketDistributor.sendToServer(new RotaryNetwork.SplitterMode(spl.getBlockPos(), mode));
				spl.setMode(mode);
			}
		}).bounds(x, y, 72, 20).build());
	}
}
