/*******************************************************************************
 * Tests for the ported RotaryCraft Liquid Spiller (original TileEntityFlooder / TileEntityAreaFiller). Private NeoForge 1.21.1 port of
 * RotaryCraft by Reika Kalseki; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.FlooderBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class FlooderTests {

	private static final String PLATFORM = "platform_9x8x9";

	private static <T extends BlockEntity> T place(GameTestHelper h, Block block, int x, int y, int z, int meta, Class<T> cls) {
		BlockPos p = new BlockPos(x, y, z);
		h.setBlock(p, block);
		BlockEntity be = h.getBlockEntity(p);
		if (be instanceof RotaryCraftBlockEntity r)
			r.setBlockMetadata(meta);
		return cls.cast(be);
	}

	/** A stone basin on the template floor (y = 1): 3x3x3 hollow at x, z = 3..5, y = 2..4, open at the top. */
	private static void basin(GameTestHelper h) {
		for (int x = 2; x <= 6; x++)
			for (int z = 2; z <= 6; z++)
				if (x == 2 || x == 6 || z == 2 || z == 6)
					for (int y = 2; y <= 4; y++)
						h.setBlock(new BlockPos(x, y, z), Blocks.STONE);
	}

	/** The spiller above the middle of the basin at (4, 5, 4), driven from the west (engine meta 1: output east). */
	private static FlooderBlockEntity rig(GameTestHelper h, boolean gas) {
		if (gas) {
			place(h, RotaryBlocks.GAS_ENGINE.get(), 3, 5, 4, 1, EngineBlockEntity.class).addFuel(50000); //512 rad/s @ 128 Nm = 65536 W
		}
		else {
			place(h, RotaryBlocks.DC_ENGINE.get(), 3, 5, 4, 1, EngineBlockEntity.class); //256 rad/s @ 4 Nm = 1024 W
			h.setBlock(new BlockPos(3, 5, 3), Blocks.REDSTONE_BLOCK);
		}
		return place(h, RotaryBlocks.SPILLER.get(), 4, 5, 4, 0, FlooderBlockEntity.class);
	}

	private static boolean isSource(GameTestHelper h, BlockPos p, Fluid f) {
		BlockState s = h.getBlockState(p);
		return s.getFluidState().isSource() && s.getFluidState().getType().isSame(f);
	}

	private static int sources(GameTestHelper h, Fluid f, int y0, int y1) {
		int n = 0;
		for (int x = 3; x <= 5; x++)
			for (int z = 3; z <= 5; z++)
				for (int y = y0; y <= y1; y++)
					if (isSource(h, new BlockPos(x, y, z), f))
						n++;
		return n;
	}

	/**
	 * 512 rad/s: one block every max(1, (int)(22 - 2*log2(513))) = 3 ticks; water (viscosity 1000) needs 512 W. Kept supplied, the
	 * spiller turns the whole hollow below it (27 blocks) into water sources and then has nothing left to fill.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 400)
	public static void spillerFillsTheHollowWithWater(GameTestHelper h) {
		basin(h);
		FlooderBlockEntity f = rig(h, true);
		h.assertTrue(f.fill(Direction.DOWN, new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE) == 0, "takes water from below");
		h.onEachTick(() -> f.fill(Direction.UP, new FluidStack(Fluids.WATER, 4000), FluidAction.EXECUTE));
		h.succeedWhen(() -> {
			h.assertTrue(f.getPower() == 65536 && f.getOperationTime() == 3 && f.getRequiredPower() == 512,
					"spiller "+f.getPower()+" W, time "+f.getOperationTime()+", needs "+f.getRequiredPower());
			h.assertTrue(sources(h, Fluids.WATER, 2, 4) == 27, "only "+sources(h, Fluids.WATER, 2, 4)+" water sources");
			h.assertTrue(f.getRemainingArea() == 0, f.getRemainingArea()+" blocks left to fill");
		});
	}

	/**
	 * Lava (viscosity 6000) needs 3072 W and takes clamp(3 * 6, 0, 12) = 12 ticks a block. One full tank (4000 mB) gives exactly four
	 * sources, 1000 mB each: bottom layer first, and there the block under the machine and three of its neighbours.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 400)
	public static void spillerPoursBottomUpFromTheMiddle(GameTestHelper h) {
		basin(h);
		FlooderBlockEntity f = rig(h, true);
		h.assertTrue(f.fill(Direction.UP, new FluidStack(Fluids.LAVA, 5000), FluidAction.EXECUTE) == FlooderBlockEntity.CAPACITY, "tank is not 4000 mB");
		h.assertTrue(f.fill(Direction.UP, new FluidStack(Fluids.WATER, 1000), FluidAction.SIMULATE) == 0, "mixes water into lava");
		h.runAtTickTime(20, () -> h.assertTrue(f.getOperationTime() == 12 && f.getRequiredPower() == 3072, "time "+f.getOperationTime()+", needs "+f.getRequiredPower()));
		h.succeedWhen(() -> {
			h.assertTrue(f.getFluidLevel() == 0, f.getFluidLevel()+" mB left");
			h.assertTrue(sources(h, Fluids.LAVA, 2, 2) == 4 && sources(h, Fluids.LAVA, 3, 4) == 0,
					sources(h, Fluids.LAVA, 2, 2)+" lava sources at the bottom, "+sources(h, Fluids.LAVA, 3, 4)+" above");
			int near = 0;
			for (Direction d : Direction.Plane.HORIZONTAL)
				if (isSource(h, new BlockPos(4, 2, 4).relative(d), Fluids.LAVA))
					near++;
			h.assertTrue(isSource(h, new BlockPos(4, 2, 4), Fluids.LAVA) && near == 3, "sources not around the middle: "+near);
		});
	}

	/** A DC engine gives the 1024 W minimum - enough for water, not for lava (3072 W): nothing is poured. */
	@GameTest(template = PLATFORM, timeoutTicks = 200)
	public static void spillerNeedsMorePowerForLava(GameTestHelper h) {
		basin(h);
		FlooderBlockEntity f = rig(h, false);
		f.fill(Direction.UP, new FluidStack(Fluids.LAVA, 4000), FluidAction.EXECUTE);
		h.runAtTickTime(150, () -> {
			h.assertTrue(f.getPower() == 1024 && f.getOperationTime() == 20, "spiller "+f.getPower()+" W, time "+f.getOperationTime()); //(int)(22 - 2*log2(257)) = 5, times 6, at most 4 * 5
			h.assertTrue(f.getFluidLevel() == 4000 && sources(h, Fluids.LAVA, 2, 4) == 0, f.getFluidLevel()+" mB, "+sources(h, Fluids.LAVA, 2, 4)+" sources");
			h.succeed();
		});
	}

	/** Standing on solid ground there is no hollow to fill: the tank stays full. */
	@GameTest(template = PLATFORM, timeoutTicks = 200)
	public static void spillerNeedsAirBelow(GameTestHelper h) {
		place(h, RotaryBlocks.GAS_ENGINE.get(), 3, 2, 4, 1, EngineBlockEntity.class).addFuel(50000);
		FlooderBlockEntity f = place(h, RotaryBlocks.SPILLER.get(), 4, 2, 4, 0, FlooderBlockEntity.class);
		f.fill(Direction.UP, new FluidStack(Fluids.WATER, 4000), FluidAction.EXECUTE);
		h.runAtTickTime(100, () -> {
			h.assertTrue(f.getPower() == 65536, "spiller "+f.getPower()+" W");
			h.assertTrue(f.getFluidLevel() == 4000 && f.getRemainingArea() == 0, f.getFluidLevel()+" mB, area "+f.getRemainingArea());
			h.succeed();
		});
	}
}
