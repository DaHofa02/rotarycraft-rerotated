/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ComparatorBlockEntity;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.reactorcraft.powergen.TurbineCoreBlockEntity;
import reika.reactorcraft.powergen.TurbineMeterBlockEntity;
import reika.reactorcraft.powergen.WorkingFluid;
import reika.reactorcraft.registry.ReactorBlocks;

/** Original TileEntityTurbineMeter (see TurbineMeterBlockEntity). */
@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class TurbineMeterTests {

	/** The turbine's largest blade circle (radius 3.5) needs more height than the small platform's barrier ceiling allows. */
	private static final String TALL = "platform_9x8x9";

	/** Loads a steam store into a grate the way a saved world would (as in SteamTests). */
	private static void giveSteam(GameTestHelper h, BlockEntity te, int steam) {
		CompoundTag t = te.saveWithoutMetadata(h.getLevel().registryAccess());
		t.putInt("energy", steam);
		WorkingFluid.WATER.saveToNBT(t);
		te.loadWithComponents(t, h.getLevel().registryAccess());
	}

	/**
	 * The five-stage steam turbine of SteamTests.turbineRunsOnSteam with a meter on the floor under the last stage (outside its blade
	 * circle): while the turbine spins up the meter gives 15*omega/maxSpeed of that stage, and a comparator next to it passes the level on.
	 */
	@GameTest(template = TALL, timeoutTicks = 400)
	public static void meterReadsTurbineSpeed(GameTestHelper h) {
		h.setBlock(new BlockPos(7, 3, 4), ReactorBlocks.STEAM_GRATE.get());
		giveSteam(h, h.getBlockEntity(new BlockPos(7, 3, 4)), 100000);
		TurbineCoreBlockEntity[] t = new TurbineCoreBlockEntity[5];
		for (int i = 0; i < 5; i++) {
			BlockPos p = new BlockPos(7-i, 5, 4);
			h.setBlock(p, ReactorBlocks.TURBINE_CORE.get());
			t[i] = (TurbineCoreBlockEntity)h.getBlockEntity(p);
			t[i].setBlockMetadata(0);
			t[i].addLubricant(10000);
		}
		BlockPos mp = new BlockPos(3, 1, 4);
		h.setBlock(mp, ReactorBlocks.TURBINE_METER.get());
		TurbineMeterBlockEntity meter = (TurbineMeterBlockEntity)h.getBlockEntity(mp);
		// a comparator reads the block on its FACING side; it needs a block under it (the platform has no floor under y = 1)
		BlockPos cp = new BlockPos(3, 1, 5);
		h.setBlock(cp.below(), Blocks.STONE);
		h.setBlock(cp, Blocks.COMPARATOR.defaultBlockState().setValue(ComparatorBlock.FACING, Direction.NORTH));
		h.succeedWhen(() -> {
			TurbineCoreBlockEntity last = t[4];
			h.assertTrue(last.getStage() == 4 && last.getOmega() > 0, "turbine not running: stage "+last.getStage()+", "+last.getOmega()+" rad/s");
			h.assertTrue(h.absolutePos(new BlockPos(3, 5, 4)).equals(meter.getTurbinePos()), "meter sees "+meter.getTurbinePos());
			int expected = 15*last.getOmega()/last.getMaxSpeed();
			int lvl = meter.getRedstoneOverride();
			// the meter and the turbine tick in either order, so the meter may be one turbine tick behind
			h.assertTrue(lvl > 0 && Math.abs(lvl-expected) <= 1, "meter "+lvl+", expected "+expected+" ("+last.getOmega()+"/"+last.getMaxSpeed()+" rad/s)");
			int out = ((ComparatorBlockEntity)h.getBlockEntity(cp)).getOutputSignal();
			h.assertTrue(out == lvl, "comparator "+out+", meter "+lvl);
		});
	}

	/**
	 * Original scan rule: the meter looks straight up and takes the topmost turbine, but any opaque block above that turbine clears it
	 * again; glass (opacity 0) does not. The turbine here stands alone (not spinning) - only the found position is checked. The scan
	 * repeats every 32 ticks.
	 */
	@GameTest(template = TALL, timeoutTicks = 200)
	public static void meterNeedsClearSkyAboveTurbine(GameTestHelper h) {
		BlockPos mp = new BlockPos(4, 1, 4);
		h.setBlock(mp, ReactorBlocks.TURBINE_METER.get());
		TurbineMeterBlockEntity meter = (TurbineMeterBlockEntity)h.getBlockEntity(mp);
		h.setBlock(new BlockPos(4, 3, 4), ReactorBlocks.TURBINE_CORE.get());
		h.setBlock(new BlockPos(4, 2, 4), Blocks.GLASS);
		h.setBlock(new BlockPos(4, 5, 4), Blocks.GLASS);
		BlockPos turbine = h.absolutePos(new BlockPos(4, 3, 4));
		h.runAtTickTime(40, () -> {
			h.assertTrue(turbine.equals(meter.getTurbinePos()), "glass hides the turbine: "+meter.getTurbinePos());
			h.setBlock(new BlockPos(4, 6, 4), Blocks.STONE);
		});
		h.runAtTickTime(80, () -> {
			h.assertTrue(meter.getTurbinePos() == null && meter.getRedstoneOverride() == 0, "stone above the turbine, meter still sees "+meter.getTurbinePos());
			h.setBlock(new BlockPos(4, 7, 4), ReactorBlocks.TURBINE_CORE.get());
		});
		h.runAtTickTime(120, () -> {
			h.assertTrue(h.absolutePos(new BlockPos(4, 7, 4)).equals(meter.getTurbinePos()), "upper turbine not found: "+meter.getTurbinePos());
			h.succeed();
		});
	}
}
