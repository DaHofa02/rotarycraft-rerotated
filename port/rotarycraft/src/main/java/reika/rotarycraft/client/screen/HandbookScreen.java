/*******************************************************************************
 * Private NeoForge 1.21.1 port of RotaryCraft; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.screen;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import reika.rotarycraft.client.HandbookClient;

/**
 * The handbook screen, own drawn layout: on the left the entries under their category headings (mouse wheel scrolls), on the right the
 * chosen entry's icon, title and text (mouse wheel over it scrolls the text).
 */
public class HandbookScreen extends Screen {

	private static final int W = 320, H = 200, LIST_W = 110, ROW = 11;

	private final List<HandbookClient.Entry> entries;
	/** Rows of the list: a String is a category heading, an Integer an entry index. */
	private final List<Object> rows = new ArrayList<>();
	private int selected;
	private int listScroll;
	private int textScroll;
	private List<FormattedCharSequence> lines = List.of();

	public HandbookScreen(Component title, List<HandbookClient.Entry> entries, @Nullable String at) {
		super(title);
		this.entries = entries;
		String cat = null;
		for (int i = 0; i < entries.size(); i++) {
			if (!entries.get(i).category().equals(cat)) {
				cat = entries.get(i).category();
				rows.add(cat);
			}
			rows.add(i);
			if (entries.get(i).id().equals(at))
				selected = i;
		}
	}

	private int left() {
		return (width-W)/2;
	}

	private int top() {
		return (height-H)/2;
	}

	@Override
	protected void init() {
		super.init();
		this.select(selected);
		int row = rows.indexOf(selected);
		listScroll = Math.max(0, Math.min(row-4, rows.size()-this.visibleRows()));
	}

	private int visibleRows() {
		return (H-12)/ROW;
	}

	private void select(int i) {
		selected = i;
		textScroll = 0;
		lines = entries.isEmpty() ? List.of() : font.split(Component.literal(entries.get(i).text()), W-LIST_W-24);
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float partial) {
		super.render(g, mx, my, partial);
		int x = this.left(), y = this.top();
		g.fill(x, y, x+W, y+H, 0xff3a2c1e);
		g.fill(x+3, y+3, x+LIST_W, y+H-3, 0xffe8dcc0);
		g.fill(x+LIST_W+3, y+3, x+W-3, y+H-3, 0xfff4ead2);
		int vis = this.visibleRows();
		for (int r = 0; r < vis && r+listScroll < rows.size(); r++) {
			Object o = rows.get(r+listScroll);
			int ry = y+6+r*ROW;
			if (o instanceof String cat)
				g.drawString(font, cat, x+6, ry, 0xff6a3010, false);
			else {
				int i = (Integer)o;
				boolean sel = i == selected;
				if (sel)
					g.fill(x+5, ry-1, x+LIST_W-2, ry+ROW-2, 0xffc8b48c);
				String t = font.plainSubstrByWidth(entries.get(i).title(), LIST_W-14);
				g.drawString(font, t, x+10, ry, sel ? 0xff000000 : 0xff303030, false);
			}
		}
		if (entries.isEmpty()) {
			g.drawString(font, "The handbook is empty.", x+LIST_W+12, y+12, 0xff303030, false);
			return;
		}
		HandbookClient.Entry e = entries.get(selected);
		g.renderItem(new ItemStack(BuiltInRegistries.ITEM.get(e.icon())), x+LIST_W+10, y+8);
		g.drawString(font, e.title(), x+LIST_W+32, y+12, 0xff202020, false);
		int ty = y+30, maxLines = (H-36)/10;
		for (int l = 0; l < maxLines && l+textScroll < lines.size(); l++)
			g.drawString(font, lines.get(l+textScroll), x+LIST_W+12, ty+l*10, 0xff303030, false);
		if (lines.size() > maxLines)
			g.drawString(font, (textScroll+maxLines < lines.size() ? "v" : "^"), x+W-12, y+H-14, 0xff806040, false);
	}

	@Override
	public boolean mouseClicked(double mx, double my, int button) {
		int x = this.left(), y = this.top();
		if (mx >= x+3 && mx < x+LIST_W && my >= y+6) {
			int r = (int)((my-y-6)/ROW)+listScroll;
			if (r >= 0 && r < rows.size() && rows.get(r) instanceof Integer i) {
				this.select(i);
				return true;
			}
		}
		return super.mouseClicked(mx, my, button);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double dx, double dy) {
		int x = this.left();
		if (mx < x+LIST_W)
			listScroll = Math.max(0, Math.min(rows.size()-this.visibleRows(), listScroll-(int)Math.signum(dy)*3));
		else
			textScroll = Math.max(0, Math.min(Math.max(0, lines.size()-(H-36)/10), textScroll-(int)Math.signum(dy)*2));
		return true;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
