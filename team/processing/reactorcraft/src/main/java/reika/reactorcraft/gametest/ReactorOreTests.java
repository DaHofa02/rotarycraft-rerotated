/*******************************************************************************
 * Tests for the ported ReactorCraft ores and materials: drops (original BlockReactorOre/BlockFluoriteOre), smelting, fluorite
 * activation and the vein placement.
 * Private NeoForge 1.21.1 port of ReactorCraft by Reika Kalseki; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.gametest;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.phys.AABB;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.reactorcraft.content.block.FluoriteBlock;
import reika.reactorcraft.registry.FluoriteTypes;
import reika.reactorcraft.registry.ReactorBlocks;
import reika.reactorcraft.registry.ReactorItems;
import reika.reactorcraft.registry.ReactorOres;
import reika.reactorcraft.world.ReactorOrePlacement;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ReactorOreTests {

	private static final String PLATFORM = "platform_9x4x9";

	private static int dropped(GameTestHelper h, BlockPos p, Item item) {
		int n = 0;
		for (ItemEntity ie : h.getLevel().getEntities(EntityType.ITEM, new AABB(h.absolutePos(p)).inflate(1.5), e -> true))
			if (ie.getItem().is(item))
				n += ie.getItem().getCount();
		return n;
	}

	private static void breakAt(GameTestHelper h, BlockPos p, Block b) {
		h.setBlock(p, b);
		h.getLevel().destroyBlock(h.absolutePos(p), true);
	}

	/** Self-dropping ores drop themselves; calcite, magnetite and ammonium chloride (plus a netherrack) drop one mineral without fortune. */
	@GameTest(template = PLATFORM)
	public static void oreDrops(GameTestHelper h) {
		int x = 0;
		for (ReactorOres ore : ReactorOres.oreList) {
			if (ore == ReactorOres.FLUORITE)
				continue;
			BlockPos p = new BlockPos(x%9, 2, 1+4*(x/9));
			Block b = ReactorBlocks.ore(ore).get();
			breakAt(h, p, b);
			switch (ore) {
				case CALCITE -> h.assertTrue(dropped(h, p, ReactorItems.raw(ReactorItems.Raw.CALCITE).get()) == 1, "calcite drops");
				case MAGNETITE -> h.assertTrue(dropped(h, p, ReactorItems.raw(ReactorItems.Raw.MAGNETITE).get()) == 1, "magnetite drops");
				case AMMONIUM -> h.assertTrue(dropped(h, p, ReactorItems.raw(ReactorItems.Raw.AMMONIUM).get()) == 1 && dropped(h, p, Items.NETHERRACK) == 1, "ammonium drops");
				default -> h.assertTrue(dropped(h, p, b.asItem()) == 1, ore+" does not drop itself");
			}
			x += 2;
		}
		h.succeed();
	}

	/** Smelting: uraninite (both) -> uranium, cadmium/indium/silver ingots, thorite -> thorium dust, fluorite ore -> crystal, calcite -> lime. */
	@GameTest(template = PLATFORM)
	public static void smelting(GameTestHelper h) {
		ServerLevel w = h.getLevel();
		Object[][] cases = {
				{ReactorBlocks.ore(ReactorOres.PITCHBLENDE).get(), ReactorItems.ingot(ReactorItems.Ingot.URANIUM).get(), 1F},
				{ReactorBlocks.ore(ReactorOres.ENDBLENDE).get(), ReactorItems.ingot(ReactorItems.Ingot.URANIUM).get(), 1F},
				{ReactorBlocks.ore(ReactorOres.CADMIUM).get(), ReactorItems.ingot(ReactorItems.Ingot.CADMIUM).get(), 0.7F},
				{ReactorBlocks.ore(ReactorOres.INDIUM).get(), ReactorItems.ingot(ReactorItems.Ingot.INDIUM).get(), 1F},
				{ReactorBlocks.ore(ReactorOres.SILVER).get(), ReactorItems.ingot(ReactorItems.Ingot.SILVER).get(), 0.5F},
				{ReactorBlocks.ore(ReactorOres.THORIUM).get(), ReactorItems.raw(ReactorItems.Raw.THORDUST).get(), 0.8F},
				{ReactorBlocks.fluoriteOre(FluoriteTypes.RED).get(), ReactorItems.fluorite(FluoriteTypes.RED).get(), 0.4F},
				{ReactorItems.raw(ReactorItems.Raw.CALCITE).get(), ReactorItems.raw(ReactorItems.Raw.LIME).get(), 0.2F},
		};
		for (Object[] c : cases) {
			ItemStack in = new ItemStack((net.minecraft.world.level.ItemLike)c[0]);
			var r = w.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(in), w);
			h.assertTrue(r.isPresent(), "no smelting for "+in);
			h.assertTrue(r.get().value().getResultItem(w.registryAccess()).is((Item)c[1]), in+" smelts into "+r.get().value().getResultItem(w.registryAccess()));
			h.assertTrue(r.get().value().getExperience() == (Float)c[2], in+" XP "+r.get().value().getExperience());
		}
		h.succeed();
	}

	/** Fluorite ore drops 1-5 crystals of its own colour (no fortune); it glows 6, activated 12 (block: 0/15); a random tick resets it. */
	@GameTest(template = PLATFORM)
	public static void fluorite(GameTestHelper h) {
		for (int i = 0; i < 8; i++) {
			FluoriteTypes f = FluoriteTypes.colorList[i];
			BlockPos p = new BlockPos(i, 2, 2);
			breakAt(h, p, ReactorBlocks.fluoriteOre(f).get());
			int n = dropped(h, p, ReactorItems.fluorite(f).get());
			h.assertTrue(n >= 1 && n <= 5, f+" fluorite ore dropped "+n);
		}
		BlockPos p = new BlockPos(4, 2, 6);
		h.setBlock(p, ReactorBlocks.fluoriteOre(FluoriteTypes.GREEN).get().defaultBlockState().setValue(FluoriteBlock.ACTIVATED, true));
		h.assertTrue(h.getBlockState(p).getLightEmission() == 12, "activated ore light "+h.getBlockState(p).getLightEmission());
		h.getBlockState(p).randomTick(h.getLevel(), h.absolutePos(p), h.getLevel().random);
		h.assertTrue(h.getBlockState(p).getLightEmission() == 6 && !h.getBlockState(p).getValue(FluoriteBlock.ACTIVATED), "ore not reset by random tick");
		h.setBlock(p, ReactorBlocks.fluoriteBlock(FluoriteTypes.GREEN).get().defaultBlockState().setValue(FluoriteBlock.ACTIVATED, true));
		h.assertTrue(h.getBlockState(p).getLightEmission() == 15, "activated block light");
		h.succeed();
	}

	/** Builds a stone column (absolute y 0..70) over this chunk, places the feature, counts the ore, removes the column. */
	private static int placeAndCount(GameTestHelper h, String feature, Block ore, int minY, int maxY, List<String> errors) {
		ServerLevel world = h.getLevel();
		BlockPos origin = h.absolutePos(BlockPos.ZERO);
		int cx = origin.getX() & ~15, cz = origin.getZ() & ~15;
		BlockPos a = new BlockPos(cx-8, 0, cz-8), b = new BlockPos(cx+23, 70, cz+23);
		for (BlockPos p : BlockPos.betweenClosed(a, b))
			world.setBlock(p, Blocks.STONE.defaultBlockState(), 2);
		PlacedFeature f = world.registryAccess().registryOrThrow(Registries.PLACED_FEATURE).get(ResourceLocation.fromNamespaceAndPath(ReactorCraft.MOD_ID, feature));
		h.assertTrue(f != null, "no placed feature "+feature);
		f.place(world, world.getChunkSource().getGenerator(), RandomSource.create(99), new BlockPos(cx, 0, cz));
		int n = 0;
		for (BlockPos p : BlockPos.betweenClosed(a, b)) {
			if (world.getBlockState(p).is(ore)) {
				n++;
				if (p.getY() < minY-4 || p.getY() > maxY+4)
					errors.add(feature+" at y "+p.getY());
			}
		}
		for (BlockPos p : BlockPos.betweenClosed(a, b))
			world.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
		return n;
	}

	/** Calcite veins (12 of 4, y 32-60) in stone; fluorite only in this chunk's colour. */
	@GameTest(template = PLATFORM, timeoutTicks = 100)
	public static void veinsGenerate(GameTestHelper h) {
		List<String> errors = new java.util.ArrayList<>();
		int calc = placeAndCount(h, "calcite_ore", ReactorBlocks.ore(ReactorOres.CALCITE).get(), 32, 60, errors);
		h.assertTrue(calc > 6, "only "+calc+" calcite ore");
		BlockPos origin = h.absolutePos(BlockPos.ZERO);
		FluoriteTypes col = ReactorOrePlacement.chunkColor(h.getLevel().getSeed(), new ChunkPos(new BlockPos(origin.getX() & ~15, 0, origin.getZ() & ~15)));
		FluoriteTypes other = FluoriteTypes.colorList[(col.ordinal()+1)%8];
		int fl = placeAndCount(h, col.getColorName()+"_fluorite_ore", ReactorBlocks.fluoriteOre(col).get(), 32, 60, errors);
		int fl2 = placeAndCount(h, other.getColorName()+"_fluorite_ore", ReactorBlocks.fluoriteOre(other).get(), 32, 60, errors);
		h.assertTrue(fl > 12 && fl2 == 0, "fluorite of the chunk colour "+fl+", other colour "+fl2);
		h.assertTrue(errors.isEmpty(), "ore outside its band: "+errors);
		h.succeed();
	}

	/** A diamond pickaxe mines every ore with drops (the pickaxe tag must hold all ReactorCraft ores, fluorite and machine blocks). */
	@GameTest(template = "platform_9x4x9")
	public static void pickaxeHarvests(GameTestHelper h) {
		ItemStack pick = new ItemStack(Items.DIAMOND_PICKAXE);
		for (ReactorOres ore : ReactorOres.oreList) {
			if (ore == ReactorOres.FLUORITE)
				continue;
			var state = ReactorBlocks.ore(ore).get().defaultBlockState();
			h.assertTrue(pick.isCorrectToolForDrops(state), ore+" not harvestable with a pickaxe");
		}
		for (FluoriteTypes f : FluoriteTypes.colorList)
			h.assertTrue(pick.isCorrectToolForDrops(ReactorBlocks.fluoriteOre(f).get().defaultBlockState()), f+" fluorite ore not harvestable");
		for (Block b : List.of(ReactorBlocks.CALCITE_BLOCK.get(), ReactorBlocks.CONCRETE.get(), ReactorBlocks.GRAPHITE_BLOCK.get(), ReactorBlocks.FUEL_ROD.get(),
				ReactorBlocks.CPU.get(), ReactorBlocks.CONTROL_ROD.get()))
			h.assertTrue(b.defaultBlockState().is(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE), b+" not in the pickaxe tag");
		h.succeed();
	}
}
