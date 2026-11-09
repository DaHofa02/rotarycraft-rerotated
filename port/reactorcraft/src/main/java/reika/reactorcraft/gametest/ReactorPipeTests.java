/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.material.Fluids;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.reactorcraft.content.piping.GasDuctBlockEntity;
import reika.reactorcraft.content.piping.HeatPipeBlockEntity;
import reika.reactorcraft.reactor.ReactorBoilerBlockEntity;
import reika.reactorcraft.reactor.WaterCellBlockEntity;
import reika.reactorcraft.registry.ReactorBlocks;
import reika.reactorcraft.registry.ReactorFluids;
import reika.reactorcraft.waste.WastePipeBlockEntity;
import reika.rotarycraft.content.blockentity.ReservoirBlockEntity;
import reika.rotarycraft.lib.world.AmbientTemperature;
import reika.rotarycraft.registry.RotaryBlocks;

/** Original TileEntityGasDuct (with TileEntityReactorPiping) and TileEntityHeatPipe (see GasDuctBlockEntity, HeatPipeBlockEntity). */
@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ReactorPipeTests {

	private static final String PLATFORM = "platform_9x4x9";

	/** Original 0.481 J/(g K) * 7800 g for a full block. */
	private static final double BASE_HEAT = 0.481*7800;

	/**
	 * Original forced quarter: a duct with 1000 mB oxygen beside an empty one. In the first tick, whichever ticks first, a quarter (+1) of
	 * the difference goes over (1000 -> 749 / 251), then a quarter (+1) of the new difference 498 (125) -> 624 / 376.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 40)
	public static void gasDuctMovesAQuarter(GameTestHelper h) {
		BlockPos a = new BlockPos(3, 2, 4), b = new BlockPos(4, 2, 4);
		h.setBlock(a, ReactorBlocks.GAS_DUCT.get());
		h.setBlock(b, ReactorBlocks.GAS_DUCT.get());
		GasDuctBlockEntity da = (GasDuctBlockEntity)h.getBlockEntity(a), db = (GasDuctBlockEntity)h.getBlockEntity(b);
		da.setFluid(ReactorFluids.get("oxygen"), 1000);
		h.succeedWhen(() -> {
			h.assertTrue(da.getFluidLevel() != 1000, "nothing moved yet");
			h.assertTrue(da.getFluidLevel() == 624 && db.getFluidLevel() == 376, "after the first tick "+da.getFluidLevel()+" / "+db.getFluidLevel()+", expected 624 / 376");
			h.assertTrue(db.getFluidType() == ReactorFluids.get("oxygen"), "second duct holds "+db.getFluidType());
		});
	}

	/**
	 * Oxygen from a reservoir above (reservoirs give only downwards) runs through three gas ducts into a reservoir beside the last one
	 * (plain fluid handlers are filled sideways); water from a reservoir above another duct is not taken (not gaseous), although the duct
	 * connects to that reservoir.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 200)
	public static void gasDuctCarriesOnlyGas(GameTestHelper h) {
		h.setBlock(new BlockPos(2, 2, 4), ReactorBlocks.GAS_DUCT.get());
		h.setBlock(new BlockPos(3, 2, 4), ReactorBlocks.GAS_DUCT.get());
		h.setBlock(new BlockPos(4, 2, 4), ReactorBlocks.GAS_DUCT.get());
		h.setBlock(new BlockPos(2, 3, 4), RotaryBlocks.RESERVOIR.get());
		((ReservoirBlockEntity)h.getBlockEntity(new BlockPos(2, 3, 4))).setLevel(8000, ReactorFluids.get("oxygen"));
		h.setBlock(new BlockPos(5, 2, 4), RotaryBlocks.RESERVOIR.get());
		ReservoirBlockEntity out = (ReservoirBlockEntity)h.getBlockEntity(new BlockPos(5, 2, 4));
		h.setBlock(new BlockPos(6, 2, 1), ReactorBlocks.GAS_DUCT.get());
		h.setBlock(new BlockPos(6, 3, 1), RotaryBlocks.RESERVOIR.get());
		((ReservoirBlockEntity)h.getBlockEntity(new BlockPos(6, 3, 1))).setLevel(8000, Fluids.WATER);
		GasDuctBlockEntity wet = (GasDuctBlockEntity)h.getBlockEntity(new BlockPos(6, 2, 1));
		h.succeedWhen(() -> {
			h.assertTrue(h.getBlockState(new BlockPos(2, 2, 4)).getValue(PipeBlock.UP) && h.getBlockState(new BlockPos(2, 2, 4)).getValue(PipeBlock.EAST),
					"first duct not connected to the reservoir and the next duct");
			h.assertTrue(h.getBlockState(new BlockPos(4, 2, 4)).getValue(PipeBlock.EAST), "last duct not connected to the reservoir beside it");
			h.assertTrue(out.getFluidAmount() >= 500 && out.getFluid() == ReactorFluids.get("oxygen"), "reservoir got "+out.getFluidAmount()+" mB of "+out.getFluid());
			h.assertTrue(h.getBlockState(new BlockPos(6, 2, 1)).getValue(PipeBlock.UP), "duct not connected to the water reservoir");
			h.assertTrue(wet.getFluidLevel() == 0, "gas duct took "+wet.getFluidLevel()+" mB of "+wet.getFluidType());
		});
	}

	/** Original PumpablePipe: a pipe pump moves fluid only between ducts of the same kind; the source keeps no fluid type when emptied. */
	@GameTest(template = PLATFORM, timeoutTicks = 20)
	public static void ductsArePumpable(GameTestHelper h) {
		h.setBlock(new BlockPos(2, 2, 2), ReactorBlocks.GAS_DUCT.get());
		h.setBlock(new BlockPos(6, 2, 2), ReactorBlocks.GAS_DUCT.get());
		h.setBlock(new BlockPos(6, 2, 6), ReactorBlocks.WASTE_PIPE.get());
		GasDuctBlockEntity a = (GasDuctBlockEntity)h.getBlockEntity(new BlockPos(2, 2, 2)), b = (GasDuctBlockEntity)h.getBlockEntity(new BlockPos(6, 2, 2));
		WastePipeBlockEntity w = (WastePipeBlockEntity)h.getBlockEntity(new BlockPos(6, 2, 6));
		a.setFluid(ReactorFluids.get("deuterium"), 100);
		h.assertTrue(a.canTransferTo(b, net.minecraft.core.Direction.EAST), "gas duct cannot pump into a gas duct");
		h.assertTrue(!a.canTransferTo(w, net.minecraft.core.Direction.EAST), "gas duct can pump into a waste pipe");
		b.transferFrom(a, 100);
		h.assertTrue(b.getFluidLevel() == 100 && b.getFluidType() == ReactorFluids.get("deuterium"), "pumped duct holds "+b.getFluidLevel()+" of "+b.getFluidType());
		h.assertTrue(a.getFluidLevel() == 0 && a.getFluidType() == null, "source duct holds "+a.getFluidLevel()+" of "+a.getFluidType());
		h.succeed();
	}

	/** Two heat pipes: the cooler one takes half the difference from the hotter one in one tick (original balanceWith), heat is conserved. */
	@GameTest(template = PLATFORM, timeoutTicks = 40)
	public static void heatPipesShareHeat(GameTestHelper h) {
		BlockPos a = new BlockPos(3, 2, 4), b = new BlockPos(4, 2, 4);
		h.setBlock(a, ReactorBlocks.HEAT_PIPE.get());
		h.setBlock(b, ReactorBlocks.HEAT_PIPE.get());
		HeatPipeBlockEntity pa = (HeatPipeBlockEntity)h.getBlockEntity(a), pb = (HeatPipeBlockEntity)h.getBlockEntity(b);
		double[] start = new double[2];
		h.runAtTickTime(4, () -> {
			double amb = AmbientTemperature.getAmbientTemperatureAt(h.getLevel(), pa.getBlockPos())*HeatPipeBlockEntity.HEAT_CAPACITY;
			//within a degree: the ambient temperature is sampled per block (the two pipes may start a degree apart and then share half of it)
			h.assertTrue(Math.abs(pa.getHeatEnergy()-amb) <= HeatPipeBlockEntity.HEAT_CAPACITY, "first tick did not set the ambient heat: "+pa.getHeatEnergy()+" vs "+amb);
			h.assertTrue(h.getBlockState(a).getValue(PipeBlock.EAST), "heat pipes not connected");
			pa.setHeatEnergy(pa.getHeatEnergy()+100*HeatPipeBlockEntity.HEAT_CAPACITY);
			start[0] = pa.getHeatEnergy();
			start[1] = pb.getHeatEnergy();
		});
		h.succeedWhen(() -> {
			h.assertTrue(start[0] != 0, "not started");
			h.assertTrue(pa.getHeatEnergy() != start[0], "no heat moved yet");
			double half = (start[0]-start[1])/2;
			h.assertTrue(Math.abs(pa.getHeatEnergy()-(start[0]-half)) < 1e-6 && Math.abs(pb.getHeatEnergy()-(start[1]+half)) < 1e-6,
					"after one tick "+pa.getHeatEnergy()+" / "+pb.getHeatEnergy()+", expected both "+(start[0]-half)+" / "+(start[1]+half));
		});
	}

	/**
	 * A water cell (a reactor core: 50 x 0.481 x 7800 J per degree) at 500 C beside a heat pipe at ambient: in one tick the pipe gains a
	 * quarter of the cell's heat above ambient and the cell is set to ambient + 1 (original quirk: getTemperatureForHeat is at least 1).
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 40)
	public static void heatPipeDrainsReactorPart(GameTestHelper h) {
		BlockPos pipe = new BlockPos(3, 2, 4), cell = new BlockPos(4, 2, 4);
		h.setBlock(pipe, ReactorBlocks.HEAT_PIPE.get());
		HeatPipeBlockEntity hp = (HeatPipeBlockEntity)h.getBlockEntity(pipe);
		WaterCellBlockEntity[] wc = new WaterCellBlockEntity[1];
		int[] tamb = new int[1];
		h.runAtTickTime(4, () -> {
			h.setBlock(cell, ReactorBlocks.WATER_CELL.get());
			wc[0] = (WaterCellBlockEntity)h.getBlockEntity(cell);
			wc[0].setTemperature(500);
			tamb[0] = wc[0].getAmbientTemperature();
		});
		h.succeedWhen(() -> {
			h.assertTrue(wc[0] != null, "not started");
			h.assertTrue(Math.abs(wc[0].heatEnergyPerDegree()-50*BASE_HEAT) < 1e-6, "water cell heat capacity "+wc[0].heatEnergyPerDegree()+", expected "+50*BASE_HEAT);
			h.assertTrue(wc[0].getTemperature() != 500, "pipe did not act yet");
			h.assertTrue(wc[0].getTemperature() == tamb[0]+1, "cell at "+wc[0].getTemperature()+" C, expected ambient+1 = "+(tamb[0]+1));
			double expect = 50*BASE_HEAT*(500-tamb[0])/4;
			h.assertTrue(Math.abs(hp.getNetHeatEnergy()-expect) < 1e-3, "pipe holds "+hp.getNetHeatEnergy()+" J above ambient, expected "+expect);
			h.assertTrue(h.getBlockState(pipe).getValue(PipeBlock.EAST), "heat pipe not connected to the water cell");
		});
	}

	/**
	 * A heat pipe holding Q = 4 x 100.5 x C_boiler joules above ambient beside a reactor boiler at ambient: the boiler is set to ambient +
	 * (int)(Q/4 / C_boiler) = ambient + 100, the pipe keeps 3/4 Q. A pebble bed (HTGR: no external heating) beside another hot pipe stays
	 * as it is.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 40)
	public static void heatPipeHeatsBoilerNotHTGR(GameTestHelper h) {
		BlockPos pipe = new BlockPos(3, 2, 4), boiler = new BlockPos(4, 2, 4), pipe2 = new BlockPos(3, 2, 7), bed = new BlockPos(4, 2, 7);
		h.setBlock(pipe, ReactorBlocks.HEAT_PIPE.get());
		h.setBlock(pipe2, ReactorBlocks.HEAT_PIPE.get());
		HeatPipeBlockEntity hp = (HeatPipeBlockEntity)h.getBlockEntity(pipe), hp2 = (HeatPipeBlockEntity)h.getBlockEntity(pipe2);
		ReactorBoilerBlockEntity[] rb = new ReactorBoilerBlockEntity[1];
		reika.reactorcraft.reactor.PebbleBedBlockEntity[] pb = new reika.reactorcraft.reactor.PebbleBedBlockEntity[1];
		int[] tamb = new int[2];
		double[] q = new double[2];
		h.runAtTickTime(4, () -> {
			h.setBlock(boiler, ReactorBlocks.BOILER.get());
			rb[0] = (ReactorBoilerBlockEntity)h.getBlockEntity(boiler);
			tamb[0] = rb[0].getAmbientTemperature();
			rb[0].setTemperature(tamb[0]);
			q[0] = 4*100.5*rb[0].heatEnergyPerDegree();
			hp.setHeatEnergy(hp.getHeatEnergy()+q[0]);
			h.setBlock(bed, ReactorBlocks.PEBBLE_BED.get());
			pb[0] = (reika.reactorcraft.reactor.PebbleBedBlockEntity)h.getBlockEntity(bed);
			tamb[1] = pb[0].getAmbientTemperature();
			pb[0].setTemperature(tamb[1]);
			q[1] = hp2.getHeatEnergy();
			hp2.setHeatEnergy(q[1]+1e6);
		});
		h.succeedWhen(() -> {
			h.assertTrue(rb[0] != null, "not started");
			h.assertTrue(Math.abs(rb[0].heatEnergyPerDegree()-50*BASE_HEAT) < 1e-6, "boiler heat capacity "+rb[0].heatEnergyPerDegree());
			h.assertTrue(rb[0].getTemperature() != tamb[0], "pipe did not act yet");
			h.assertTrue(rb[0].getTemperature() == tamb[0]+100, "boiler at "+rb[0].getTemperature()+" C, expected ambient+100 = "+(tamb[0]+100));
			h.assertTrue(Math.abs(hp.getNetHeatEnergy()-0.75*q[0]) < 1e-3, "pipe holds "+hp.getNetHeatEnergy()+" J, expected "+0.75*q[0]);
			h.assertTrue(!pb[0].allowExternalHeating() && pb[0].getTemperature() == tamb[1], "pebble bed heated to "+pb[0].getTemperature());
			h.assertTrue(hp2.getHeatEnergy() == q[1]+1e6, "pipe beside the pebble bed lost heat");
		});
	}
}
