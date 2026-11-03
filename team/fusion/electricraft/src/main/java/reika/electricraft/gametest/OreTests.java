/*******************************************************************************
 * Tests for the ported ElectriCraft ores: drops, smelting and the vein placement (original ElectriOres/BasicElectriOreGenerator).
 * Private NeoForge 1.21.1 port of ElectriCraft by Reika Kalseki; not for distribution.
 ******************************************************************************/
package reika.electricraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.electricraft.ElectriCraft;
import reika.electricraft.registry.ElectriBlocks;
import reika.electricraft.registry.ElectriOres;
import reika.electricraft.registry.WireType;

@GameTestHolder(ElectriCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class OreTests {

	private static final String PLATFORM = "platform_9x4x9";

	/** Every ore drops itself and smelts into its ingot with the original XP. */
	@GameTest(template = PLATFORM)
	public static void oresDropAndSmelt(GameTestHelper h) {
		ServerLevel world = h.getLevel();
		int x = 0;
		for (ElectriOres ore : ElectriOres.oreList) {
			if (!ore.isPorted())
				continue;
			BlockPos p = new BlockPos(x, 2, 1);
			h.setBlock(p, ElectriBlocks.ore(ore).get());
			h.getLevel().destroyBlock(h.absolutePos(p), true);
			h.assertItemEntityPresent(ElectriBlocks.ore(ore).get().asItem(), p, 1.5);
			var r = world.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(new ItemStack(ElectriBlocks.ore(ore).get())), world);
			h.assertTrue(r.isPresent(), "no smelting recipe for "+ore);
			ItemStack out = r.get().value().getResultItem(world.registryAccess());
			h.assertTrue(out.is(ElectriBlocks.ingot(ore).get()), ore+" smelts into "+out);
			h.assertTrue(r.get().value().getExperience() == ore.xpDropped, ore+" XP "+r.get().value().getExperience());
			x += 2;
		}
		h.succeed();
	}

	/** Wires drop their item (wire loot tables). */
	@GameTest(template = PLATFORM)
	public static void wiresDropThemselves(GameTestHelper h) {
		BlockPos p = new BlockPos(4, 2, 4);
		h.setBlock(p, ElectriBlocks.getWire(WireType.COPPER, true));
		h.getLevel().destroyBlock(h.absolutePos(p), true);
		h.assertItemEntityPresent(ElectriBlocks.getWire(WireType.COPPER, true).asItem(), p, 1.5);
		h.succeed();
	}

	/**
	 * The placed tin feature (8 veins of 8 between y 48 and 72, stone only) in a stone column of this chunk: some tin ore appears, all of
	 * it inside the original height band. The column is built and removed in absolute coordinates, outside the test structure.
	 */
	@GameTest(template = PLATFORM)
	public static void tinVeinsGenerateInStone(GameTestHelper h) {
		ServerLevel world = h.getLevel();
		BlockPos origin = h.absolutePos(BlockPos.ZERO);
		int cx = origin.getX() & ~15, cz = origin.getZ() & ~15;
		BlockPos a = new BlockPos(cx-8, 40, cz-8), b = new BlockPos(cx+23, 80, cz+23);
		for (BlockPos p : BlockPos.betweenClosed(a, b))
			world.setBlock(p, Blocks.STONE.defaultBlockState(), 2);
		PlacedFeature f = world.registryAccess().registryOrThrow(Registries.PLACED_FEATURE).get(ResourceLocation.fromNamespaceAndPath(ElectriCraft.MOD_ID, "tin_ore"));
		h.assertTrue(f != null, "no placed feature electricraft:tin_ore");
		f.place(world, world.getChunkSource().getGenerator(), RandomSource.create(1234), new BlockPos(cx, 0, cz));
		int n = 0, outside = 0;
		for (BlockPos p : BlockPos.betweenClosed(a, b)) {
			if (world.getBlockState(p).is(ElectriBlocks.ore(ElectriOres.TIN).get())) {
				n++;
				if (p.getY() < 48-3 || p.getY() > 72+3) //a vein reaches a few blocks around its centre
					outside++;
			}
		}
		for (BlockPos p : BlockPos.betweenClosed(a, b))
			world.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
		h.assertTrue(n > 8, "only "+n+" tin ore placed");
		h.assertTrue(outside == 0, outside+" tin ore outside y 45..75");
		h.succeed();
	}
}
