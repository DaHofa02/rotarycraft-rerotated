/*******************************************************************************
 * Tests for the ported RotaryCraft lawn sprinkler (original TileEntityLawnSprinkler). Private NeoForge 1.21.1 port of RotaryCraft by
 * Reika Kalseki; not for distribution.
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
import reika.rotarycraft.content.blockentity.LawnSprinklerBlockEntity;
import reika.rotarycraft.content.piping.PipingBlockEntity;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class LawnSprinklerTests {

	private static final String PLATFORM = "platform_9x8x9";
	private static final BlockPos POS = new BlockPos(4, 4, 4);

	/** A lawn sprinkler at (4, 4, 4) on a pipe (4, 3, 4) on stone, the pipe kept topped up with water. */
	private static LawnSprinklerBlockEntity sprinkler(GameTestHelper h, boolean water) {
		h.setBlock(new BlockPos(4, 2, 4), Blocks.STONE);
		h.setBlock(POS.below(), RotaryBlocks.PIPE.get());
		h.setBlock(POS, RotaryBlocks.LAWN_SPRINKLER.get());
		PipingBlockEntity pipe = (PipingBlockEntity)h.getBlockEntity(POS.below());
		if (water)
			h.onEachTick(() -> {
				if (pipe.getFluidLevel() < 500)
					pipe.addFluid(Fluids.WATER, 1000);
			});
		return (LawnSprinklerBlockEntity)h.getBlockEntity(POS);
	}

	private static int moisture(GameTestHelper h, int x, int z) {
		return h.getBlockState(new BlockPos(x, 2, z)).getValue(FarmBlock.MOISTURE);
	}

	/** Water pressure (101300 kPa and more) gives the full range 8; farmland around it gets wet, a fire within range goes out. */
	@GameTest(template = PLATFORM, timeoutTicks = 2000)
	public static void sprinklerWetsFarmlandAndPutsOutFire(GameTestHelper h) {
		LawnSprinklerBlockEntity te = sprinkler(h, true);
		int[][] spots = {{3, 4}, {5, 5}, {4, 2}};
		for (int[] s : spots)
			h.setBlock(new BlockPos(s[0], 2, s[1]), Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 0));
		h.setBlock(new BlockPos(1, 2, 7), Blocks.NETHERRACK);
		h.setBlock(new BlockPos(7, 2, 1), Blocks.OAK_PLANKS);
		h.setBlock(new BlockPos(7, 3, 1), Blocks.FIRE);
		h.succeedWhen(() -> {
			h.assertTrue(te.getRange() == 8 && te.canPerformEffects(), "range "+te.getRange()+", water "+te.getWater()+", pressure "+te.getPressure());
			for (int[] s : spots)
				h.assertTrue(moisture(h, s[0], s[1]) == FarmBlock.MAX_MOISTURE, "farmland at "+s[0]+" "+s[1]+" at "+moisture(h, s[0], s[1]));
			h.assertBlockNotPresent(Blocks.FIRE, new BlockPos(7, 3, 1));
		});
	}

	/** Without water in the pipe it does nothing. */
	@GameTest(template = PLATFORM, timeoutTicks = 300)
	public static void sprinklerNeedsWater(GameTestHelper h) {
		LawnSprinklerBlockEntity te = sprinkler(h, false);
		h.setBlock(new BlockPos(3, 2, 4), Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 0));
		h.runAtTickTime(250, () -> {
			h.assertTrue(!te.canPerformEffects() && te.getRange() == 0, "works without water");
			h.assertTrue(moisture(h, 3, 4) <= 0, "farmland wet");
			h.succeed();
		});
	}
}
