/*******************************************************************************
 * Tests for the ported RotaryCraft spillway (original TileEntitySpillway). Private NeoForge 1.21.1 port of RotaryCraft by Reika Kalseki;
 * not for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.SpillwayBlockEntity;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class SpillwayTests {

	private static final String PLATFORM = "platform_9x8x9";
	private static final BlockPos POS = new BlockPos(4, 2, 4);

	/** A spillway at (4, 2, 4) facing east (meta 0). */
	private static SpillwayBlockEntity spillway(GameTestHelper h) {
		h.setBlock(POS, RotaryBlocks.SPILLWAY.get());
		SpillwayBlockEntity te = (SpillwayBlockEntity)h.getBlockEntity(POS);
		te.setBlockMetadata(0);
		return te;
	}

	/** Water from a source on a stone pillar falls down beside the spillway: 50 mB a tick. */
	@GameTest(template = PLATFORM, timeoutTicks = 300)
	public static void spillwayCatchesAWaterfall(GameTestHelper h) {
		SpillwayBlockEntity te = spillway(h);
		h.assertTrue(te.getDrainSide() == Direction.EAST, "faces "+te.getDrainSide());
		for (int y = 2; y <= 5; y++)
			h.setBlock(new BlockPos(6, y, 4), Blocks.STONE);
		h.setBlock(new BlockPos(6, 6, 4), Blocks.WATER);
		int[] first = {-1, 0};
		h.succeedWhen(() -> {
			int lvl = te.getWaterLevel();
			if (first[0] < 0) {
				h.assertTrue(lvl > 0 && te.isActive(), "no water yet at "+h.getBlockState(new BlockPos(5, 2, 4)));
				first[0] = (int)h.getTick();
				first[1] = lvl;
				h.fail("first water");
			}
			h.assertTrue(h.getTick()-first[0] >= 20, "waiting");
			int per = (lvl-first[1])/(int)(h.getTick()-first[0]);
			h.assertTrue(per == 50, per+" mB a tick");
		});
	}

	/**
	 * A channel of three still water blocks beside it (one wide, so no new sources form) is drained block by block, farthest first, 1000 mB
	 * each, and the emptied spots are kept empty; the tank empties downwards only. (A wider pool keeps refilling itself through vanilla's
	 * infinite water, which feeds the spillway on and on as in the original.)
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 200)
	public static void spillwayDrainsAPool(GameTestHelper h) {
		SpillwayBlockEntity te = spillway(h);
		for (int x = 4; x <= 8; x++)
			for (int z = 3; z <= 5; z++)
				if (x > 4 || z != 4)
					h.setBlock(new BlockPos(x, 2, z), x <= 7 && z == 4 ? Blocks.WATER : Blocks.STONE);
		h.runAtTickTime(40, () -> {
			h.assertTrue(te.getWaterLevel() == 3000, te.getWaterLevel()+" mB");
			for (int x = 5; x <= 7; x++)
				h.assertFalse(h.getBlockState(new BlockPos(x, 2, 4)).getFluidState().isSource(), "source left at x = "+x);
			IFluidHandler side = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(POS), Direction.NORTH);
			IFluidHandler down = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(POS), Direction.DOWN);
			h.assertTrue(side.drain(1000, IFluidHandler.FluidAction.EXECUTE).isEmpty(), "water came out at the side");
			h.assertTrue(side.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE) == 0, "water went in");
			h.assertTrue(down.drain(2000, IFluidHandler.FluidAction.EXECUTE).getAmount() == 2000, "water did not come out below");
			h.succeed();
		});
	}

	/** Water put right on top of it is removed; without water beside it nothing comes in. */
	@GameTest(template = PLATFORM, timeoutTicks = 100)
	public static void spillwayClearsWaterAboveIt(GameTestHelper h) {
		SpillwayBlockEntity te = spillway(h);
		h.setBlock(POS.above(), Blocks.WATER);
		h.runAtTickTime(40, () -> {
			h.assertBlockNotPresent(Blocks.WATER, POS.above());
			h.assertTrue(te.getWaterLevel() == 0 && !te.isActive(), te.getWaterLevel()+" mB from nowhere");
			h.succeed();
		});
	}
}
