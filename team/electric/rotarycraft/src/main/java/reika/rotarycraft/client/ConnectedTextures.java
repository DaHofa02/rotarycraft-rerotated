/*******************************************************************************
 * Private NeoForge 1.21.1 ports of RotaryCraft, ReactorCraft and ElectriCraft (not part of the original mods): connected textures. Plain
 * blocks of the three mods (no block entity: casings, metal blocks, multiblock parts) whose textures have a frame join with the same block
 * next to them - the frame opens on that side, so a wall or floor of them reads as one panel. The variants are made by
 * tools/modernize_textures.py (textures/<dir>/ctm/<name>/<mask>.png, mask: 1 top, 2 right, 4 bottom, 8 left of the texture); this picks
 * one per face from the neighbours while the chunk is built. Which world side is the "top" or "left" of a face is read from the quad's
 * own UVs, so rotated models work as well.
 ******************************************************************************/
package reika.rotarycraft.client;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;

import reika.rotarycraft.RotaryCraft;

@EventBusSubscriber(modid = RotaryCraft.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ConnectedTextures {

	private static final Set<String> MODS = Set.of("rotarycraft", "reactorcraft", "electricraft");
	/** Bit d (Direction.get3DDataValue) set: the same block is on that side. */
	private static final ModelProperty<Integer> SAME = new ModelProperty<>();
	private static final int TOP = 1, RIGHT = 2, BOTTOM = 4, LEFT = 8;

	private ConnectedTextures() {}

	private static ResourceLocation variant(ResourceLocation sprite, int mask) {
		String p = sprite.getPath();
		int i = p.lastIndexOf('/');
		return ResourceLocation.fromNamespaceAndPath(sprite.getNamespace(), p.substring(0, i)+"/ctm/"+p.substring(i+1)+"/"+mask);
	}

	@SubscribeEvent
	public static void bake(ModelEvent.ModifyBakingResult e) {
		Map<ModelResourceLocation, BakedModel> models = e.getModels();
		Map<TextureAtlasSprite, TextureAtlasSprite[]> variants = new IdentityHashMap<>();
		int wrapped = 0;
		java.util.Set<ResourceLocation> blocks = new java.util.TreeSet<>();
		for (Block b : BuiltInRegistries.BLOCK) {
			if (!MODS.contains(BuiltInRegistries.BLOCK.getKey(b).getNamespace()) || b instanceof EntityBlock)
				continue;
			for (BlockState s : b.getStateDefinition().getPossibleStates()) {
				ModelResourceLocation loc = BlockModelShaper.stateToModelLocation(s);
				BakedModel m = models.get(loc);
				if (m == null || m instanceof Model)
					continue;
				boolean any = false;
				for (Direction d : Direction.values())
					for (BakedQuad q : m.getQuads(s, d, RandomSource.create(42)))
						any |= variants.computeIfAbsent(q.getSprite(), sp -> {
							ResourceLocation n = sp.contents().name();
							if (!MODS.contains(n.getNamespace()))
								return null;
							TextureAtlasSprite[] v = new TextureAtlasSprite[16];
							for (int k = 1; k < 16; k++) {
								v[k] = e.getTextureGetter().apply(new Material(InventoryMenu.BLOCK_ATLAS, variant(n, k)));
								if (v[k] == null || v[k].contents().name().equals(MissingTextureAtlasSprite.getLocation()))
									return null;
							}
							v[0] = sp;
							return v;
						}) != null;
				if (any) {
					models.put(loc, new Model(m, variants));
					wrapped++;
					blocks.add(BuiltInRegistries.BLOCK.getKey(b));
				}
			}
		}
		RotaryCraft.LOGGER.info("Connected textures: {} block states of {}", wrapped, blocks);
	}

	/** World direction in which the quad's texture coordinate (u: 4, v: 5) grows. */
	private static Direction axis(int[] vx, int uv) {
		int stride = vx.length/4;
		float[][] p = new float[4][3];
		float[] t = new float[4], o = new float[4];
		for (int i = 0; i < 4; i++) {
			for (int c = 0; c < 3; c++)
				p[i][c] = Float.intBitsToFloat(vx[i*stride+c]);
			t[i] = Float.intBitsToFloat(vx[i*stride+uv]);
			o[i] = Float.intBitsToFloat(vx[i*stride+(uv == 4 ? 5 : 4)]);
		}
		for (int i = 0; i < 4; i++)
			for (int j = 0; j < 4; j++)
				if (Math.abs(o[i]-o[j]) < 1e-5F && t[j]-t[i] > 1e-5F)
					return Direction.getNearest(p[j][0]-p[i][0], p[j][1]-p[i][1], p[j][2]-p[i][2]);
		return null;
	}

	private static final class Model extends BakedModelWrapper<BakedModel> {

		private final Map<TextureAtlasSprite, TextureAtlasSprite[]> variants;
		/** Per quad: the world sides of its texture's right (+u) and bottom (+v), and its variants by mask (made when first needed). */
		private final Map<BakedQuad, Direction[]> axes = new IdentityHashMap<>();
		private final Map<BakedQuad, BakedQuad[]> made = new IdentityHashMap<>();

		Model(BakedModel m, Map<TextureAtlasSprite, TextureAtlasSprite[]> variants) {
			super(m);
			this.variants = variants;
		}

		@Override
		public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData data) {
			int same = 0;
			for (Direction d : Direction.values())
				if (level.getBlockState(pos.relative(d)).getBlock() == state.getBlock())
					same |= 1 << d.get3DDataValue();
			return super.getModelData(level, pos, state, data).derive().with(SAME, same).build();
		}

		@Override
		public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand, ModelData data, RenderType rt) {
			List<BakedQuad> base = super.getQuads(state, side, rand, data, rt);
			Integer same = data.get(SAME);
			if (same == null || same == 0)
				return base;
			List<BakedQuad> li = new ArrayList<>(base.size());
			for (BakedQuad q : base)
				li.add(this.connect(q, same));
			return li;
		}

		private BakedQuad connect(BakedQuad q, int same) {
			TextureAtlasSprite[] v = variants.get(q.getSprite());
			if (v == null)
				return q;
			Direction[] ax;
			synchronized (axes) {
				ax = axes.computeIfAbsent(q, k -> new Direction[] {axis(k.getVertices(), 4), axis(k.getVertices(), 5)});
			}
			if (ax[0] == null || ax[1] == null)
				return q;
			int mask = 0;
			if ((same & 1 << ax[1].getOpposite().get3DDataValue()) != 0)
				mask |= TOP;
			if ((same & 1 << ax[0].get3DDataValue()) != 0)
				mask |= RIGHT;
			if ((same & 1 << ax[1].get3DDataValue()) != 0)
				mask |= BOTTOM;
			if ((same & 1 << ax[0].getOpposite().get3DDataValue()) != 0)
				mask |= LEFT;
			if (mask == 0)
				return q;
			synchronized (made) {
				BakedQuad[] arr = made.computeIfAbsent(q, k -> new BakedQuad[16]);
				if (arr[mask] == null)
					arr[mask] = remap(q, q.getSprite(), v[mask]);
				return arr[mask];
			}
		}

		private static BakedQuad remap(BakedQuad q, TextureAtlasSprite from, TextureAtlasSprite to) {
			int[] vx = q.getVertices().clone();
			int stride = vx.length/4;
			for (int i = 0; i < 4; i++) {
				float u = Float.intBitsToFloat(vx[i*stride+4]), w = Float.intBitsToFloat(vx[i*stride+5]);
				float fu = (u-from.getU0())/(from.getU1()-from.getU0()), fv = (w-from.getV0())/(from.getV1()-from.getV0());
				vx[i*stride+4] = Float.floatToRawIntBits(to.getU0()+fu*(to.getU1()-to.getU0()));
				vx[i*stride+5] = Float.floatToRawIntBits(to.getV0()+fv*(to.getV1()-to.getV0()));
			}
			return new BakedQuad(vx, q.getTintIndex(), q.getDirection(), to, q.isShade(), q.hasAmbientOcclusion());
		}
	}
}
