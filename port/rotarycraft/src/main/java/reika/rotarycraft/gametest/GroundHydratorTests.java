/*******************************************************************************
 * Tests for the ported RotaryCraft ground hydrator (original TileEntityGroundHydrator). Private NeoForge 1.21.1 port of RotaryCraft by
 * Reika Kalseki; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.GroundHydratorBlockEntity;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class GroundHydratorTests {

	private static final String PLATFORM = "platform_9x8x9";

	private static GroundHydratorBlockEntity hydrator(GameTestHelper h, int x, int z) {
		BlockPos p = new BlockPos(x, 2, z);
		h.setBlock(p, RotaryBlocks.HYDRATOR.get());
		return (GroundHydratorBlockEntity)h.getBlockEntity(p);
	}

	/** Dry farmland with young wheat on it (the wheat keeps vanilla from turning dry farmland back into dirt). */
	private static BlockPos field(GameTestHelper h, int x, int z) {
		BlockPos p = new BlockPos(x, 2, z);
		h.setBlock(p, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 0));
		h.setBlock(p.above(), ((CropBlock)Blocks.WHEAT).getStateForAge(0));
		return p;
	}

	private static int moisture(GameTestHelper h, BlockPos p) {
		return h.getBlockState(p).is(Blocks.FARMLAND) ? h.getBlockState(p).getValue(FarmBlock.MOISTURE) : -1;
	}

	/** Original AREA: 13x13 around it, 9 next to it, 3 at the ends of the axes, 1 in the corners, 0 for itself and outside. */
	@GameTest(template = PLATFORM)
	public static void hydratorAreaFollowsTheOriginal(GameTestHelper h) {
		h.assertTrue(GroundHydratorBlockEntity.getRange() == 6, "range "+GroundHydratorBlockEntity.getRange());
		int[][] cases = {{0, 0, 0}, {1, 0, 9}, {1, 1, 9}, {0, -6, 3}, {6, 0, 3}, {6, 6, 1}, {-6, -6, 1}, {7, 0, 0}, {-2, 3, 6}};
		for (int[] c : cases)
			h.assertTrue(GroundHydratorBlockEntity.getWeight(c[0], c[1]) == c[2], "weight at "+c[0]+" "+c[1]+": "+GroundHydratorBlockEntity.getWeight(c[0], c[1]));
		h.succeed();
	}

	/**
	 * Farmland next to it gets wet step by step up to 7, for 25 mB of water per step; farmland 7 blocks away (outside the area) and
	 * farmland one level lower stay dry.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 4000)
	public static void hydratorWetsTheFarmlandAroundIt(GameTestHelper h) {
		GroundHydratorBlockEntity te = hydrator(h, 1, 4);
		te.addWater(1000);
		BlockPos[] near = {field(h, 2, 4), field(h, 1, 5), field(h, 2, 3)};
		BlockPos far = field(h, 8, 4);
		BlockPos low = new BlockPos(1, 1, 3); //one level lower, beside the hydrator's own floor block
		h.setBlock(low, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 0));
		h.succeedWhen(() -> {
			for (BlockPos p : near)
				h.assertTrue(moisture(h, p) == 7, "farmland at "+p+" at moisture "+moisture(h, p)+", "+te.getWaterLevel()+" mB left");
			int used = 1000-te.getWaterLevel();
			h.assertTrue(used >= 3*7*25 && used%25 == 0, used+" mB used");
			h.assertTrue(moisture(h, far) == 0, "farmland outside the area wet: "+moisture(h, far));
			h.assertTrue(moisture(h, low) <= 0, "farmland below wet: "+moisture(h, low));
		});
	}

	/** Below 25 mB nothing happens; only water goes in (up to 1000 mB), through pipes or any fluid handler. */
	@GameTest(template = PLATFORM, timeoutTicks = 400)
	public static void hydratorNeedsWater(GameTestHelper h) {
		GroundHydratorBlockEntity te = hydrator(h, 4, 4);
		IFluidHandler fh = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(new BlockPos(4, 2, 4)), Direction.UP);
		h.assertTrue(fh != null, "hydrator has no fluid handler");
		h.assertTrue(fh.fill(new FluidStack(Fluids.LAVA, 500), IFluidHandler.FluidAction.EXECUTE) == 0, "lava went in");
		h.assertTrue(fh.fill(new FluidStack(RotaryFluids.ETHANOL.get(), 500), IFluidHandler.FluidAction.EXECUTE) == 0, "ethanol went in");
		h.assertTrue(fh.fill(new FluidStack(Fluids.WATER, 24), IFluidHandler.FluidAction.EXECUTE) == 24, "water refused");
		h.assertTrue(fh.drain(10, IFluidHandler.FluidAction.EXECUTE).isEmpty(), "water came out");
		BlockPos[] near = {field(h, 5, 4), field(h, 3, 4), field(h, 4, 5), field(h, 4, 3)};
		h.runAtTickTime(300, () -> {
			for (BlockPos p : near)
				h.assertTrue(moisture(h, p) == 0, "farmland at "+p+" wet with 24 mB: "+moisture(h, p));
			h.assertTrue(te.getWaterLevel() == 24, te.getWaterLevel()+" mB left");
			h.assertTrue(fh.fill(new FluidStack(Fluids.WATER, 5000), IFluidHandler.FluidAction.EXECUTE) == 976, "tank not 1000 mB");
			h.succeed();
		});
	}
}
