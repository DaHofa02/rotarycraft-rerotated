/*******************************************************************************
 * Optional Just Enough Items integration of the private NeoForge 1.21.1 port of ReactorCraft (by Reika Kalseki): the multiblock viewer,
 * in place of the original handbook's structure pages (DragonAPI StructureRenderer). Only loaded with JEI.
 ******************************************************************************/
package reika.reactorcraft.compat.jei;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

import reika.reactorcraft.ReactorCraft;
import reika.reactorcraft.registry.ReactorBlocks;
import reika.reactorcraft.structure.Blueprints.Blueprint;

/**
 * Shows a multiblock as a slowly turning 3D view of its blocks (machines drawn by their renderers, as their items), laid out like GregTech
 * CEu Modern's "Multiblock Info" page: the name centred above, the machine at its heart (the recipe output) at the top left, buttons on the
 * right - the layer ("L:all", "L:1" ...; click: next layer from the bottom) and "ALL" (whole structure) - and the parts it needs with counts
 * in one row of slots below (with more than nine kinds the last slot cycles through the rest). Clicking the view also steps the layers.
 */
public class MultiblockCategory implements IRecipeCategory<Blueprint> {

	public static final RecipeType<Blueprint> TYPE = RecipeType.create(ReactorCraft.MOD_ID, "multiblock", Blueprint.class);

	private static final int WIDTH = 176, VIEW = 100, PARTS = 9, BUTTON_X = WIDTH-24, BUTTON_W = 24, BUTTON_H = 16;

	private final IDrawable icon;
	private final Map<String, Integer> layer = new HashMap<>();

	public MultiblockCategory(IGuiHelper gui) {
		icon = gui.createDrawableItemStack(new ItemStack(ReactorBlocks.TURBINE_HOUSING.get()));
	}

	@Override
	public RecipeType<Blueprint> getRecipeType() {
		return TYPE;
	}

	@Override
	public Component getTitle() {
		return Component.literal("Multiblock Structures");
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
		return 12+VIEW+24; //room for the part counts under the slot row (the page must fit JEI's window: the view is kept small)
	}

	/** How many of each block the structure takes. */
	public static Map<Block, Integer> parts(Blueprint bp) {
		Map<Block, Integer> m = new LinkedHashMap<>();
		for (Block b : bp.blocks().values())
			m.merge(b, 1, Integer::sum);
		return m;
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder b, Blueprint bp, IFocusGroup focuses) {
		List<Map.Entry<Block, Integer>> parts = new ArrayList<>(parts(bp).entrySet());
		for (int i = 0; i < Math.min(PARTS, parts.size()); i++) {
			List<Map.Entry<Block, Integer>> here = i < PARTS-1 || parts.size() <= PARTS ? List.of(parts.get(i)) : parts.subList(i, parts.size());
			List<ItemStack> stacks = new ArrayList<>();
			for (Map.Entry<Block, Integer> e : here)
				stacks.add(new ItemStack(e.getKey(), Math.min(99, e.getValue())));
			b.addSlot(RecipeIngredientRole.INPUT, (WIDTH-18*PARTS)/2+1+18*i, 12+VIEW+3).setStandardSlotBackground().addItemStacks(stacks)
					.addRichTooltipCallback((view, tip) -> view.getDisplayedItemStack().ifPresent(is -> {
						for (Map.Entry<Block, Integer> e : here)
							if (is.is(e.getKey().asItem()))
								tip.add(Component.literal(e.getValue()+" needed"));
					}));
		}
		b.addSlot(RecipeIngredientRole.OUTPUT, 1, 13).setOutputSlotBackground().addItemStack(new ItemStack(bp.machine()));
	}

	@Override
	public boolean handleInput(Blueprint bp, double mx, double my, InputConstants.Key key) {
		if (key.getType() != InputConstants.Type.MOUSE || my < 12 || my > 12+VIEW)
			return false;
		int[] yr = yRange(bp);
		int n = yr[1]-yr[0]+1;
		if (mx >= BUTTON_X && my >= 32 && my < 32+BUTTON_H) { //"ALL"
			layer.put(bp.id(), -1);
			return true;
		}
		int l = layer.getOrDefault(bp.id(), -1)+1;
		layer.put(bp.id(), l >= n ? -1 : l);
		return true;
	}

	/** A raised button as GregTech draws them: light face, white top-left and dark bottom-right edges. */
	private static void button(GuiGraphics g, Minecraft mc, int y, String text, boolean hover) {
		int x = BUTTON_X;
		g.fill(x, y, x+BUTTON_W, y+BUTTON_H, 0xFF373737);
		g.fill(x, y, x+BUTTON_W-1, y+BUTTON_H-1, 0xFFFFFFFF);
		g.fill(x+1, y+1, x+BUTTON_W-1, y+BUTTON_H-1, hover ? 0xFFA0A0A0 : 0xFF8B8B8B);
		g.drawString(mc.font, text, x+(BUTTON_W-mc.font.width(text))/2, y+4, 0xFFFFFFFF, true);
	}

	private static int[] yRange(Blueprint bp) {
		int lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE;
		for (BlockPos p : bp.blocks().keySet()) {
			lo = Math.min(lo, p.getY());
			hi = Math.max(hi, p.getY());
		}
		return new int[]{lo, hi};
	}

	private static boolean hasQuads(BakedModel m, BlockState s, RandomSource r) {
		if (!m.getQuads(s, null, r, ModelData.EMPTY, null).isEmpty())
			return true;
		for (Direction d : Direction.values())
			if (!m.getQuads(s, d, r, ModelData.EMPTY, null).isEmpty())
				return true;
		return false;
	}

	@Override
	public void draw(Blueprint bp, IRecipeSlotsView view, GuiGraphics g, double mx, double my) {
		Minecraft mc = Minecraft.getInstance();
		int[] yr = yRange(bp);
		int lay = layer.getOrDefault(bp.id(), -1);
		g.drawString(mc.font, bp.name(), (WIDTH-mc.font.width(bp.name()))/2, 1, 0xFF404040, false);
		button(g, mc, 13, lay < 0 ? "L:all" : "L:"+(lay+1), mx >= BUTTON_X && my >= 13 && my < 13+BUTTON_H);
		button(g, mc, 32, "ALL", mx >= BUTTON_X && my >= 32 && my < 32+BUTTON_H);
		int x0 = Integer.MAX_VALUE, x1 = Integer.MIN_VALUE, z0 = Integer.MAX_VALUE, z1 = Integer.MIN_VALUE;
		for (BlockPos p : bp.blocks().keySet()) {
			x0 = Math.min(x0, p.getX());
			x1 = Math.max(x1, p.getX());
			z0 = Math.min(z0, p.getZ());
			z1 = Math.max(z1, p.getZ());
		}
		float size = Math.max(Math.max(x1-x0+1, z1-z0+1), yr[1]-yr[0]+1);
		float scale = VIEW/(size*1.45F);
		PoseStack pose = g.pose();
		pose.pushPose();
		pose.translate(WIDTH/2F, 12+VIEW/2F, 200);
		pose.scale(scale, -scale, scale);
		pose.mulPose(Axis.XP.rotationDegrees(30));
		pose.mulPose(Axis.YP.rotationDegrees((System.currentTimeMillis()/40)%360));
		pose.translate(-(x0+x1+1)/2F, -(yr[0]+yr[1]+1)/2F, -(z0+z1+1)/2F);
		MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
		RenderSystem.enableDepthTest();
		Lighting.setupFor3DItems();
		RandomSource rand = RandomSource.create(42);
		for (Map.Entry<BlockPos, Block> e : bp.blocks().entrySet()) {
			BlockPos p = e.getKey();
			if (lay >= 0 && p.getY() != yr[0]+lay)
				continue;
			BlockState s = e.getValue().defaultBlockState();
			pose.pushPose();
			pose.translate(p.getX(), p.getY(), p.getZ());
			BakedModel m = mc.getBlockRenderer().getBlockModel(s);
			if (hasQuads(m, s, rand)) {
				mc.getBlockRenderer().renderSingleBlock(s, pose, buf, 0xF000F0, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
			}
			else { //modelled machines: drawn by their block entity renderer, here as their item
				pose.translate(0.5, 0.5, 0.5);
				mc.getItemRenderer().renderStatic(new ItemStack(e.getValue()), ItemDisplayContext.NONE, 0xF000F0, OverlayTexture.NO_OVERLAY, pose, buf, mc.level, 0);
			}
			pose.popPose();
		}
		buf.endBatch();
		pose.popPose();
		Lighting.setupForFlatItems();
	}
}
