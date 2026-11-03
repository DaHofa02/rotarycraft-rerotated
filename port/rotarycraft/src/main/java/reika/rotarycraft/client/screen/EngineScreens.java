/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import net.neoforged.neoforge.network.PacketDistributor;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.content.blockentity.engine.JetEngineBlockEntity;
import reika.rotarycraft.network.RotaryNetwork;
import reika.rotarycraft.content.blockentity.engine.PerformanceEngineBlockEntity;
import reika.rotarycraft.content.menu.MachineMenu;

/** Original: GuiSteam, GuiEthanol, GuiPerformance, GuiJet (also used by the microturbine) and GuiOneSlotInv (AC engine). */
public final class EngineScreens {

	private EngineScreens() {}

	public static class Steam extends MachineScreen {
		public Steam(MachineMenu m, Inventory inv, Component t) {
			super(m, inv, t, "steamgui", 176, 79);
		}

		@Override
		protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
			super.renderBg(g, partial, mx, my);
			if (!(menu.tile instanceof EngineBlockEntity e))
				return;
			int i2 = e.getWaterScaled(54);
			int i3 = e.getTempScaled(54);
			blitTex(g, leftPos+49, topPos+71-i2, 193, 55-i2, 5, i2);
			blitTex(g, leftPos+119, topPos+71-i3, 177, 99-i3, 9, i3);
		}

		@Override
		protected int[] getFuelBar() {
			return new int[] {48, 16, 6, 55};
		}
	}

	public static class Ethanol extends MachineScreen {
		public Ethanol(MachineMenu m, Inventory inv, Component t) {
			super(m, inv, t, "ethanolgui", 176, 166);
		}

		@Override
		protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
			super.renderBg(g, partial, mx, my);
			if (menu.tile instanceof EngineBlockEntity e) {
				int i1 = e.getFuelScaled(54);
				blitTex(g, leftPos+85, topPos+71-i1, 200, 55-i1, 5, i1);
			}
		}

		@Override
		protected int[] getFuelBar() {
			return new int[] {84, 16, 6, 55};
		}
	}

	public static class Performance extends MachineScreen {
		public Performance(MachineMenu m, Inventory inv, Component t) {
			super(m, inv, t, "perfgui", 176, 166);
		}

		@Override
		protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
			super.renderBg(g, partial, mx, my);
			if (!(menu.tile instanceof PerformanceEngineBlockEntity e))
				return;
			int i2 = e.getWaterScaled(54);
			int i3 = e.getTempScaled(54);
			blitTex(g, leftPos+41, topPos+71-i2, 193, 55-i2, 5, i2);
			blitTex(g, leftPos+128, topPos+71-i3, 177, 99-i3, 9, i3);
			int i1 = e.getFuelScaled(54);
			blitTex(g, leftPos+82, topPos+71-i1, 200, 55-i1, 6, i1);
			int i4 = e.getAdditivesScaled(54);
			blitTex(g, leftPos+89, topPos+71-i4, 207, 55-i4, 6, i4);
		}

		@Override
		protected int[] getFuelBar() {
			return new int[] {81, 16, 7, 55};
		}
	}

	/** GuiJet: the afterburner toggle (an ImagedGuiButton from buttons.png) is shown for gas turbines, not for the microturbine. */
	public static class Jet extends MachineScreen {

		private static final ResourceLocation BUTTONS = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/gui/buttons.png");
		private boolean burn;

		public Jet(MachineMenu m, Inventory inv, Component t) {
			super(m, inv, t, "jetgui", 176, 166);
			burn = menu.tile instanceof JetEngineBlockEntity jet && jet.canAfterBurn() && jet.burnerActive();
		}

		@Override
		protected void init() {
			super.init();
			this.clearWidgets();
			if (menu.tile instanceof JetEngineBlockEntity jet) {
				int u = jet.burnerActive() ? 36 : 0;
				int v = jet.canAfterBurn() ? 72 : 90;
				this.addRenderableWidget(new AbstractButton(leftPos+32, topPos+36, 36, 18, Component.empty()) {
					@Override
					public void onPress() {
						if (jet.canAfterBurn()) {
							burn = !burn;
							PacketDistributor.sendToServer(new RotaryNetwork.Afterburner(jet.getBlockPos(), burn));
							jet.setBurnerActive(burn);
							Jet.this.init();
						}
					}

					@Override
					protected void renderWidget(GuiGraphics g, int mx, int my, float partial) {
						g.blit(BUTTONS, this.getX(), this.getY(), u, v, 36, 18);
					}

					@Override
					protected void updateWidgetNarration(NarrationElementOutput out) {
						this.defaultButtonNarrationText(out);
					}
				});
			}
		}

		@Override
		protected void renderExtraTooltips(GuiGraphics g, int mx, int my) {
			super.renderExtraTooltips(g, mx, my);
			if (menu.tile instanceof JetEngineBlockEntity && mx >= leftPos+32 && mx <= leftPos+68 && my >= topPos+36 && my <= topPos+54)
				g.renderTooltip(font, Component.literal("Afterburner"), mx, my);
		}

		@Override
		protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
			super.renderBg(g, partial, mx, my);
			if (menu.tile instanceof EngineBlockEntity e) {
				int i2 = e.getFuelScaled(54);
				blitTex(g, leftPos+85, topPos+71-i2, 207, 55-i2, 5, i2);
			}
		}

		@Override
		protected int[] getFuelBar() {
			return new int[] {84, 16, 6, 55};
		}
	}

	public static class OneSlot extends MachineScreen {
		public OneSlot(MachineMenu m, Inventory inv, Component t) {
			super(m, inv, t, "basic_gui_oneslot", 176, 166);
		}
	}
}
