/*******************************************************************************
 * Private NeoForge 1.21.1 ports of RotaryCraft, ReactorCraft and ElectriCraft (not part of the original mods): after the models are
 * baked, logs every item and block state of the three mods whose model is missing or shows the missing texture (the purple-black
 * checkerboard), and every item with a builtin/entity model but no item renderer (drawn as nothing), so gaps are found without looking
 * at each one in game.
 ******************************************************************************/
package reika.rotarycraft.client;

import java.util.Set;
import java.util.TreeSet;

import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import reika.rotarycraft.RotaryCraft;

@EventBusSubscriber(modid = RotaryCraft.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class TextureAudit {

	private static final Set<String> MODS = Set.of("rotarycraft", "reactorcraft", "electricraft");

	private TextureAudit() {}

	private static boolean broken(BakedModel m, BakedModel missing, BlockState s) {
		if (m == null || m == missing)
			return true;
		if (m.getParticleIcon().contents().name().equals(MissingTextureAtlasSprite.getLocation()) && !m.isCustomRenderer())
			return true;
		RandomSource r = RandomSource.create(42);
		for (Direction d : new Direction[] {null, Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST})
			for (BakedQuad q : m.getQuads(s, d, r))
				if (q.getSprite().contents().name().equals(MissingTextureAtlasSprite.getLocation()))
					return true;
		return false;
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void audit(ModelEvent.ModifyBakingResult e) {
		var models = e.getModels();
		BakedModel missing = models.get(ModelBakery.MISSING_MODEL_VARIANT);
		Set<String> bad = new TreeSet<>();
		var vanilla = net.minecraft.client.Minecraft.getInstance().getItemRenderer() != null
				? net.minecraft.client.Minecraft.getInstance().getItemRenderer().getBlockEntityRenderer() : null;
		int items = 0, custom = 0;
		for (Item i : BuiltInRegistries.ITEM) {
			ResourceLocation id = BuiltInRegistries.ITEM.getKey(i);
			if (!MODS.contains(id.getNamespace()))
				continue;
			items++;
			BakedModel m = models.get(ModelResourceLocation.inventory(id));
			if (broken(m, missing, null))
				bad.add("item "+id);
			else if (m.isCustomRenderer()) {
				custom++;
				if (IClientItemExtensions.of(i).getCustomRenderer() == vanilla)
					bad.add("item "+id+" (builtin/entity model, no item renderer: draws nothing)");
			}
		}
		for (Block b : BuiltInRegistries.BLOCK) {
			ResourceLocation id = BuiltInRegistries.BLOCK.getKey(b);
			if (!MODS.contains(id.getNamespace()))
				continue;
			for (BlockState s : b.getStateDefinition().getPossibleStates())
				if (broken(models.get(BlockModelShaper.stateToModelLocation(s)), missing, s)) {
					bad.add("block "+id);
					break;
				}
		}
		RotaryCraft.LOGGER.info("Texture audit: {} items checked ({} drawn by renderers, renderer check {}), {} without texture: {}", items, custom,
				vanilla != null ? "on" : "off", bad.size(), bad);
	}
}
