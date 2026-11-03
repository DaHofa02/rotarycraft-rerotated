/*******************************************************************************
 * Optional Just Enough Items integration of the private NeoForge 1.21.1 port of RotaryCraft (by Reika Kalseki). Only loaded with JEI.
 ******************************************************************************/
package reika.rotarycraft.compat.jei;

import java.util.List;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * The one JEI category layout all ported machines share, in the style of GregTech CEu Modern's machine pages (so the two read alike in
 * one pack): the machine's whole slot grid on each side - item slots, below them its fluid slots (dark) - sized to the largest recipe of
 * the machine, unused slots shown empty; an animated arrow between; chances in gold at the top of their slot; the requirements as white
 * lines with shadow below.
 */
public class MachineCategory implements IRecipeCategory<MachineRecipe> {

	public static final int WIDTH = 176;
	private static final int ARROW_X = 76, GAP = 8;
	private static final int FLUID_SLOT = 0xFF373737;

	private final RecipeType<MachineRecipe> type;
	private final Component title;
	private final IDrawable icon;
	private final IDrawableStatic slot;
	private final IDrawableStatic arrow;
	private final IDrawableAnimated progress;
	private final int lines;

	/** The machine's grid: item slots and fluid slots per side (set from its recipes). */
	private int itemsIn = 1, itemsOut = 1, fluidsIn, fluidsOut;
	private boolean grid;
	/** Text rows needed by the longest requirement text once wrapped to the width (at least the declared line count). */
	private int textRows;

	public MachineCategory(IGuiHelper gui, RecipeType<MachineRecipe> type, Component title, ItemStack icon, int maxLines) {
		this.type = type;
		this.title = title;
		this.icon = gui.createDrawableItemStack(icon);
		slot = gui.getSlotDrawable();
		arrow = gui.getRecipeArrow();
		progress = gui.createAnimatedRecipeArrow(80);
		lines = maxLines;
	}

	/** Sizes the slot grid to the largest of the machine's recipes. */
	public void layout(List<MachineRecipe> recipes) {
		for (MachineRecipe r : recipes) {
			itemsIn = Math.max(itemsIn, Math.min(9, r.inputs().size()));
			itemsOut = Math.max(itemsOut, Math.min(9, r.outputs().size()));
			fluidsIn = Math.max(fluidsIn, Math.min(3, r.fluidsIn().size()));
			fluidsOut = Math.max(fluidsOut, Math.min(3, r.fluidsOut().size()));
			grid |= r.grid();
		}
		if (grid)
			itemsIn = 9;
		textRows = lines;
		Font font = Minecraft.getInstance().font;
		if (font != null)
			for (MachineRecipe r : recipes) {
				int n = 0;
				for (Component c : r.lines())
					n += font.split(c, WIDTH-4).size();
				textRows = Math.max(textRows, n);
			}
	}

	@Override
	public RecipeType<MachineRecipe> getRecipeType() {
		return type;
	}

	@Override
	public Component getTitle() {
		return title;
	}

	@Override
	public IDrawable getIcon() {
		return icon;
	}

	@Override
	public int getWidth() {
		return WIDTH;
	}

	@Override
	public int getHeight() {
		return this.slotsHeight()+GAP+Math.max(lines, textRows)*10;
	}

	/** Columns of a grid of n slots, as GregTech arranges them (1-3 in a row, 4 as 2x2, then rows of 3). */
	private static int cols(int n) {
		return n == 4 ? 2 : Math.min(3, Math.max(1, n));
	}

	private static int rows(int n, int cols) {
		return n <= 0 ? 0 : (n+cols-1)/cols;
	}

	private int sideHeight(int items, int fluids) {
		return 18*(rows(items, cols(items))+rows(fluids, cols(fluids)));
	}

	private int slotsHeight() {
		return Math.max(18, Math.max(this.sideHeight(itemsIn, fluidsIn), this.sideHeight(itemsOut, fluidsOut)));
	}

	/** Top-left of slot i of a side's grid: items first, the fluid rows under them; inputs end left of the arrow, outputs start right of it. */
	private int[] pos(boolean input, boolean fluid, int i) {
		int items = input ? itemsIn : itemsOut, fluids = input ? fluidsIn : fluidsOut;
		int ci = cols(items), cf = cols(fluids), w = 18*Math.max(ci, fluids > 0 ? cf : 0);
		int top = (this.slotsHeight()-this.sideHeight(items, fluids))/2;
		int c = fluid ? cf : ci;
		int y = top+18*(i/c)+(fluid ? 18*rows(items, ci) : 0);
		int x = input ? ARROW_X-10-w+18*(i%c)+(w-18*c) : ARROW_X+34+18*(i%c);
		return new int[] {x, y};
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder b, MachineRecipe r, IFocusGroup focuses) {
		List<List<ItemStack>> in = r.inputs();
		for (int i = 0; i < Math.min(9, in.size()); i++) {
			int[] p = this.pos(true, false, i);
			IRecipeSlotBuilder s = b.addSlot(RecipeIngredientRole.INPUT, p[0]+1, p[1]+1);
			if (!in.get(i).isEmpty())
				s.addIngredients(VanillaTypes.ITEM_STACK, in.get(i));
		}
		for (int i = 0; i < Math.min(3, r.fluidsIn().size()); i++) {
			int[] p = this.pos(true, true, i);
			FluidStack fs = r.fluidsIn().get(i);
			b.addSlot(RecipeIngredientRole.INPUT, p[0]+1, p[1]+1).setFluidRenderer(Math.max(1, fs.getAmount()), false, 16, 16)
					.addIngredient(NeoForgeTypes.FLUID_STACK, fs);
		}
		List<MachineRecipe.Output> out = r.outputs();
		for (int i = 0; i < Math.min(9, out.size()); i++) {
			int[] p = this.pos(false, false, i);
			MachineRecipe.Output o = out.get(i);
			IRecipeSlotBuilder s = b.addSlot(RecipeIngredientRole.OUTPUT, p[0]+1, p[1]+1).addItemStack(o.item());
			if (o.note() != null)
				s.addRichTooltipCallback((view, tip) -> tip.add(Component.literal(o.note()).withStyle(ChatFormatting.GOLD)));
		}
		for (int i = 0; i < Math.min(3, r.fluidsOut().size()); i++) {
			int[] p = this.pos(false, true, i);
			FluidStack fs = r.fluidsOut().get(i);
			b.addSlot(RecipeIngredientRole.OUTPUT, p[0]+1, p[1]+1).setFluidRenderer(Math.max(1, fs.getAmount()), false, 16, 16)
					.addIngredient(NeoForgeTypes.FLUID_STACK, fs);
		}
	}

	private static String amount(int mB) {
		return mB >= 1000 && mB%1000 == 0 ? mB/1000+"B" : mB >= 1000 ? String.format("%.1fB", mB/1000D) : String.valueOf(mB);
	}

	@Override
	public void draw(MachineRecipe r, IRecipeSlotsView view, GuiGraphics g, double mx, double my) {
		Font font = Minecraft.getInstance().font;
		for (int side = 0; side < 2; side++) {
			boolean input = side == 0;
			int items = input ? itemsIn : itemsOut, fluids = input ? fluidsIn : fluidsOut;
			for (int i = 0; i < rows(items, cols(items))*cols(items); i++) { //whole rows, as GregTech shows its grids
				int[] p = this.pos(input, false, i);
				slot.draw(g, p[0], p[1]);
			}
			for (int i = 0; i < rows(fluids, cols(fluids))*cols(fluids); i++) {
				int[] p = this.pos(input, true, i);
				slot.draw(g, p[0], p[1]);
				g.fill(p[0]+1, p[1]+1, p[0]+17, p[1]+17, FLUID_SLOT);
			}
		}
		int ay = (this.slotsHeight()-17)/2;
		arrow.draw(g, ARROW_X, ay);
		progress.draw(g, ARROW_X, ay);
		var pose = g.pose();
		for (int i = 0; i < r.outputs().size() && i < 9; i++) {
			String n = r.outputs().get(i).note();
			if (n != null && n.length() <= 5) {
				int[] p = this.pos(false, false, i);
				pose.pushPose();
				pose.translate(p[0]+1, p[1]+1, 200);
				pose.scale(0.5F, 0.5F, 1);
				g.drawString(font, n, 0, 0, 0xFFFFAA00, true);
				pose.popPose();
			}
		}
		for (int k = 0; k < 2; k++) {
			List<FluidStack> fl = k == 0 ? r.fluidsIn() : r.fluidsOut();
			for (int i = 0; i < Math.min(3, fl.size()); i++) {
				int[] p = this.pos(k == 0, true, i);
				String a = amount(fl.get(i).getAmount());
				pose.pushPose();
				pose.translate(p[0]+17-font.width(a)*0.5F, p[1]+12.5F, 200);
				pose.scale(0.5F, 0.5F, 1);
				g.drawString(font, a, 0, 0, 0xFFFFFFFF, true);
				pose.popPose();
			}
		}
		int y = this.slotsHeight()+GAP;
		for (Component c : r.lines())
			for (var row : font.split(c, WIDTH-4)) {
				g.drawString(font, row, 2, y, 0xFFFFFFFF, true);
				y += 10;
			}
	}
}
