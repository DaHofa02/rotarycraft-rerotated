/*******************************************************************************
 * Tests for the ported RotaryCraft sprinkler (original TileEntitySprinkler). Private NeoForge 1.21.1 port of RotaryCraft by Reika
 * Kalseki; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.SprinklerBlockEntity;
import reika.rotarycraft.content.piping.PipingBlockEntity;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class SprinklerTests {

	private static final String PLATFORM = "platform_9x8x9";
	private static final BlockPos POS = new BlockPos(4, 5, 4);

	/** A sprinkler at (4, 5, 4) under a pipe (4, 6, 4) kept topped up with water. */
	private static SprinklerBlockEntity sprinkler(GameTestHelper h) {
		h.setBlock(POS.above(), RotaryBlocks.PIPE.get());
		h.setBlock(POS, RotaryBlocks.SPRINKLER.get());
		PipingBlockEntity pipe = (PipingBlockEntity)h.getBlockEntity(POS.above());
		h.onEachTick(() -> {
			if (pipe.getFluidLevel() < 500)
				pipe.addFluid(Fluids.WATER, 1000);
		});
		return (SprinklerBlockEntity)h.getBlockEntity(POS);
	}

	private static int moisture(GameTestHelper h, int x, int z) {
		return h.getBlockState(new BlockPos(x, 2, z)).getValue(FarmBlock.MOISTURE);
	}

	/** Farmland in the square below gets wet, a fire goes out, and sugar cane grows. */
	@GameTest(template = PLATFORM, timeoutTicks = 1200)
	public static void sprinklerWatersTheSquareBelow(GameTestHelper h) {
		SprinklerBlockEntity te = sprinkler(h);
		int[][] spots = {{3, 4}, {6, 6}, {1, 2}};
		for (int[] s : spots)
			h.setBlock(new BlockPos(s[0], 2, s[1]), Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 0));
		h.setBlock(new BlockPos(6, 2, 2), Blocks.OAK_PLANKS);
		h.setBlock(new BlockPos(6, 3, 2), Blocks.FIRE);
		h.succeedWhen(() -> {
			h.assertTrue(te.getRange() == 8 && te.canPerformEffects(), "range "+te.getRange()+", water "+te.getWater());
			for (int[] s : spots)
				h.assertTrue(moisture(h, s[0], s[1]) == FarmBlock.MAX_MOISTURE, "farmland at "+s[0]+" "+s[1]+" at "+moisture(h, s[0], s[1]));
			h.assertBlockNotPresent(Blocks.FIRE, new BlockPos(6, 3, 2));
		});
	}

	/** A stone roof over a column keeps the farmland under it dry. */
	@GameTest(template = PLATFORM, timeoutTicks = 400)
	public static void sprinklerIsStoppedByOpaqueBlocks(GameTestHelper h) {
		SprinklerBlockEntity te = sprinkler(h);
		h.setBlock(new BlockPos(2, 2, 4), Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 0));
		h.setBlock(new BlockPos(2, 4, 4), Blocks.STONE);
		h.setBlock(new BlockPos(3, 2, 4), Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 0));
		h.runAtTickTime(350, () -> {
			h.assertTrue(te.canPerformEffects(), "sprinkler idle");
			h.assertTrue(moisture(h, 3, 4) == FarmBlock.MAX_MOISTURE, "open farmland at "+moisture(h, 3, 4));
			h.assertTrue(moisture(h, 2, 4) == 0, "covered farmland at "+moisture(h, 2, 4));
			h.succeed();
		});
	}
}
