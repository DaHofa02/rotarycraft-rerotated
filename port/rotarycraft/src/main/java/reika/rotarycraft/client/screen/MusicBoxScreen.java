/*******************************************************************************
 * Private NeoForge 1.21.1 port of RotaryCraft; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.screen;

import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import reika.rotarycraft.content.blockentity.MusicBoxBlockEntity;
import reika.rotarycraft.content.blockentity.MusicBoxBlockEntity.Instrument;
import reika.rotarycraft.content.blockentity.MusicBoxBlockEntity.NoteLength;
import reika.rotarycraft.content.menu.MusicBoxMenu;

/**
 * Own layout (drawn): channel, instrument, length and octave selectors, a one-octave keyboard, rest/delete/clear buttons and the end of the
 * current channel's notes.
 */
public class MusicBoxScreen extends AbstractContainerScreen<MusicBoxMenu> {

	private static final String[] NAMES = {"C", "C#", "D", "Eb", "E", "F", "F#", "G", "G#", "A", "Bb", "B"};

	private int channel;
	private int voice = 1;
	private int length = 2;
	private int octave = 2;

	public MusicBoxScreen(MusicBoxMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		imageWidth = 236;
		imageHeight = 132;
	}

	private void click(int id) {
		if (minecraft != null && minecraft.gameMode != null)
			minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
	}

	@Override
	protected void init() {
		super.init();
		int x = leftPos+8, y = topPos+18;
		this.addRenderableWidget(Button.builder(Component.literal("Ch"), b -> channel = (channel+1)%MusicBoxBlockEntity.CHANNELS).bounds(x, y, 54, 16).build());
		this.addRenderableWidget(Button.builder(Component.literal("Voice"), b -> voice = 1+voice%6).bounds(x+56, y, 54, 16).build());
		this.addRenderableWidget(Button.builder(Component.literal("Len"), b -> length = (length+1)%5).bounds(x+112, y, 54, 16).build());
		this.addRenderableWidget(Button.builder(Component.literal("Oct"), b -> octave = (octave+1)%4).bounds(x+168, y, 52, 16).build());
		for (int i = 0; i < 12; i++) {
			int semi = i;
			this.addRenderableWidget(Button.builder(Component.literal(NAMES[i]), b -> this.note(semi)).bounds(x+i*18, y+40, 18, 18).build());
		}
		this.addRenderableWidget(Button.builder(Component.literal("Rest"), b -> this.click(MusicBoxMenu.noteButton(channel, NoteLength.values()[length], 0, Instrument.REST)))
				.bounds(x, y+62, 54, 16).build());
		this.addRenderableWidget(Button.builder(Component.literal("Delete"), b -> this.click(channel << 16 | 1)).bounds(x+56, y+62, 54, 16).build());
		this.addRenderableWidget(Button.builder(Component.literal("Clear ch"), b -> this.click(channel << 16 | 2)).bounds(x+112, y+62, 54, 16).build());
		this.addRenderableWidget(Button.builder(Component.literal("Clear all"), b -> this.click(3)).bounds(x+168, y+62, 52, 16).build());
	}

	private void note(int semi) {
		int pitch = Math.min(MusicBoxBlockEntity.MAX_PITCH, octave*12+semi);
		this.click(MusicBoxMenu.noteButton(channel, NoteLength.values()[length], pitch, Instrument.values()[voice]));
	}

	@Override
	protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
		int x = leftPos, y = topPos;
		g.fill(x, y, x+imageWidth, y+imageHeight, 0xff2b2d33);
		g.fill(x+2, y+2, x+imageWidth-2, y+imageHeight-2, 0xff7a5a40);
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mx, int my) {
		g.drawString(font, title, 8, 5, 0xfff0e0c0, false);
		g.drawString(font, "Ch "+(channel+1)+"  "+Instrument.values()[voice].name().toLowerCase()+"  "+NoteLength.values()[length].name().toLowerCase()+"  octave "+octave,
				8, 38, 0xfff0e0c0, false);
		List<MusicBoxBlockEntity.Note> notes = menu.tile.getChannel(channel);
		StringBuilder sb = new StringBuilder(notes.size()+" notes: ");
		for (int i = Math.max(0, notes.size()-8); i < notes.size(); i++) {
			MusicBoxBlockEntity.Note n = notes.get(i);
			sb.append(n.isRest() ? "-" : NAMES[n.pitch()%12]+n.pitch()/12).append(' ');
		}
		g.drawString(font, sb.toString(), 8, 102, 0xffffffff, false);
		g.drawString(font, menu.tile.isPlaying() ? "Playing" : "Needs 1024 W or a redstone pulse", 8, 116, 0xffd0c0a0, false);
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float partial) {
		super.render(g, mx, my, partial);
		this.renderTooltip(g, mx, my);
	}
}
