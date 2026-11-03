/*******************************************************************************
 * Regression tests for the ported RotaryCraft piping (TileEntityPiping and subclasses). Expected behaviour follows the original
 * code: quarter-of-the-difference flow, connection rules per pipe type and per machine side.
 * Private NeoForge 1.21.1 port of RotaryCraft by Reika Kalseki; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.block.GearboxBlock;
import reika.rotarycraft.content.blockentity.EngineControllerBlockEntity;
import reika.rotarycraft.content.blockentity.ExtractorBlockEntity;
import reika.rotarycraft.content.blockentity.GearboxBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.content.piping.PipingBlockEntity;
import reika.rotarycraft.registry.GearboxTypes;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class PipeTests {

	private static final String PLATFORM = "platform_9x4x9";

	private static <T extends BlockEntity> T place(GameTestHelper h, Block block, int x, int y, int z, int meta, Class<T> cls) {
		BlockPos p = new BlockPos(x, y, z);
		h.setBlock(p, block);
		BlockEntity be = h.getBlockEntity(p);
		if (!cls.isInstance(be))
			throw new IllegalStateException("no "+cls.getSimpleName()+" at "+p);
		if (be instanceof RotaryCraftBlockEntity r)
			r.setBlockMetadata(meta);
		return cls.cast(be);
	}

	private static PipingBlockEntity pipe(GameTestHelper h, Block block, int x, int y, int z) {
		return place(h, block, x, y, z, 0, PipingBlockEntity.class);
	}

	private static boolean connected(GameTestHelper h, int x, int y, int z, Direction d) {
		return h.getBlockState(new BlockPos(x, y, z)).getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(d));
	}

	/**
	 * Water put into a pipe run reaches the back of a steam engine standing over fire and the engine runs (512 rad/s, 32 Nm). The
	 * engine (meta 0) has its back to the east, which is the only side it accepts a pipe on. It starts at 95 °C: fire adds only about
	 * 1 °C per second, the test is about the water supply.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 600)
	public static void pipeFeedsSteamEngine(GameTestHelper h) {
		h.setBlock(new BlockPos(2, 0, 4), Blocks.NETHERRACK);
		h.setBlock(new BlockPos(2, 1, 4), Blocks.FIRE);
		EngineBlockEntity eng = place(h, RotaryBlocks.STEAM_ENGINE.get(), 2, 2, 4, 0, EngineBlockEntity.class);
		((reika.rotarycraft.content.blockentity.TemperatureMachine)eng).setTemperature(95);
		pipe(h, RotaryBlocks.PIPE.get(), 3, 2, 4);
		pipe(h, RotaryBlocks.PIPE.get(), 4, 2, 4);
		PipingBlockEntity src = pipe(h, RotaryBlocks.PIPE.get(), 5, 2, 4);
		h.runAfterDelay(2, () -> src.addFluid(Fluids.WATER, 20000));
		h.succeedWhen(() -> {
			h.assertTrue(connected(h, 3, 2, 4, Direction.WEST) && connected(h, 3, 2, 4, Direction.EAST), "pipe next to the engine not connected");
			h.assertTrue(eng.getWater() > 0, "no water in the engine");
			h.assertTrue(eng.omega == 512 && eng.torque == 32, "steam engine "+eng.omega+" rad/s @ "+eng.torque+" Nm, "+eng.getTemperature()+" C");
		});
	}

	/** A pipe does not connect to the front or the sides of an engine. */
	@GameTest(template = PLATFORM, timeoutTicks = 40)
	public static void pipeOnlyAtEngineBack(GameTestHelper h) {
		place(h, RotaryBlocks.STEAM_ENGINE.get(), 4, 2, 4, 0, EngineBlockEntity.class);
		pipe(h, RotaryBlocks.PIPE.get(), 3, 2, 4);
		pipe(h, RotaryBlocks.PIPE.get(), 4, 2, 5);
		pipe(h, RotaryBlocks.PIPE.get(), 5, 2, 4);
		h.runAtTickTime(20, () -> {
			h.assertFalse(connected(h, 3, 2, 4, Direction.EAST), "pipe connected to the engine's front");
			h.assertFalse(connected(h, 4, 2, 5, Direction.NORTH), "pipe connected to the engine's side");
			h.assertTrue(connected(h, 5, 2, 4, Direction.WEST), "pipe not connected to the engine's back");
			h.succeed();
		});
	}

	/**
	 * Flow between two pipes: each step moves a quarter of the level difference, so 1000 mB in one pipe spread to two
	 * equal halves (within the rounding of the integer quarter).
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 100)
	public static void pipesEqualize(GameTestHelper h) {
		PipingBlockEntity a = pipe(h, RotaryBlocks.PIPE.get(), 3, 1, 4);
		PipingBlockEntity b = pipe(h, RotaryBlocks.PIPE.get(), 4, 1, 4);
		h.runAfterDelay(2, () -> a.addFluid(Fluids.WATER, 1000));
		h.runAtTickTime(60, () -> {
			h.assertTrue(a.getFluidLevel()+b.getFluidLevel() == 1000, "water not conserved: "+a.getFluidLevel()+" + "+b.getFluidLevel());
			h.assertTrue(Math.abs(a.getFluidLevel()-b.getFluidLevel()) <= 4, "not equalized: "+a.getFluidLevel()+" / "+b.getFluidLevel());
			h.succeed();
		});
	}

	/** Pipe types do not mix: a liquid pipe next to a fuel line or a hose stays unconnected; a bedrock pipe joins a liquid pipe. */
	@GameTest(template = PLATFORM, timeoutTicks = 40)
	public static void pipeTypesDoNotMix(GameTestHelper h) {
		pipe(h, RotaryBlocks.PIPE.get(), 3, 1, 4);
		pipe(h, RotaryBlocks.FUEL_LINE.get(), 4, 1, 4);
		pipe(h, RotaryBlocks.HOSE.get(), 3, 1, 5);
		pipe(h, RotaryBlocks.BEDROCK_PIPE.get(), 2, 1, 4);
		h.runAtTickTime(20, () -> {
			h.assertFalse(connected(h, 3, 1, 4, Direction.EAST), "pipe connected to a fuel line");
			h.assertFalse(connected(h, 3, 1, 4, Direction.SOUTH), "pipe connected to a hose");
			h.assertTrue(connected(h, 3, 1, 4, Direction.WEST) && connected(h, 2, 1, 4, Direction.EAST), "pipe and bedrock pipe not connected");
			h.succeed();
		});
	}

	/** A pipe does not take fuel or lubricant; a fuel line does not take water. */
	@GameTest(template = PLATFORM, timeoutTicks = 20)
	public static void pipeFluidRules(GameTestHelper h) {
		PipingBlockEntity p = pipe(h, RotaryBlocks.PIPE.get(), 3, 1, 4);
		PipingBlockEntity f = pipe(h, RotaryBlocks.FUEL_LINE.get(), 5, 1, 4);
		PipingBlockEntity hose = pipe(h, RotaryBlocks.HOSE.get(), 7, 1, 4);
		h.assertFalse(p.canIntakeFluid(RotaryFluids.JET_FUEL.get()) || p.canIntakeFluid(RotaryFluids.LUBRICANT.get()) || p.canIntakeFluid(RotaryFluids.ETHANOL.get()), "pipe takes fuel/lubricant");
		h.assertTrue(p.canIntakeFluid(Fluids.WATER) && p.canIntakeFluid(Fluids.LAVA), "pipe refuses water/lava");
		h.assertFalse(f.canIntakeFluid(Fluids.WATER), "fuel line takes water");
		h.assertTrue(f.canIntakeFluid(RotaryFluids.ETHANOL.get()) && f.canIntakeFluid(RotaryFluids.JET_FUEL.get()), "fuel line refuses fuel");
		h.assertTrue(hose.canIntakeFluid(RotaryFluids.LUBRICANT.get()) && !hose.canIntakeFluid(Fluids.WATER), "hose fluid rule");
		h.succeed();
	}

	/**
	 * Fuel line from an ECU to the bottom of a gas engine: the ECU (flow DUAL) gives ethanol, the engine (fuel from below) takes it.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 300)
	public static void fuelLineFeedsEngine(GameTestHelper h) {
		EngineControllerBlockEntity ecu = place(h, RotaryBlocks.ECU.get(), 2, 1, 4, 0, EngineControllerBlockEntity.class);
		ecu.getFluidHandler(Direction.EAST).fill(new FluidStack(RotaryFluids.ETHANOL.get(), 4000), IFluidHandler.FluidAction.EXECUTE);
		pipe(h, RotaryBlocks.FUEL_LINE.get(), 3, 1, 4);
		pipe(h, RotaryBlocks.FUEL_LINE.get(), 4, 1, 4);
		EngineBlockEntity eng = place(h, RotaryBlocks.GAS_ENGINE.get(), 4, 2, 4, 0, EngineBlockEntity.class);
		h.succeedWhen(() -> {
			h.assertTrue(connected(h, 4, 1, 4, Direction.UP), "fuel line not connected to the engine's bottom");
			h.assertTrue(eng.getFuelLevel() > 0, "no fuel in the engine");
		});
	}

	/** A hose filled with lubricant feeds a gearbox from the side; the hose does not connect to the gearbox top. */
	@GameTest(template = PLATFORM, timeoutTicks = 300)
	public static void hoseLubricatesGearbox(GameTestHelper h) {
		GearboxBlockEntity gbx = place(h, RotaryBlocks.GEARBOX.get(), 4, 1, 4, 0, GearboxBlockEntity.class);
		gbx.applyComponentsFromItemStack(GearboxBlock.gearboxItem(GearboxTypes.STEEL, 2));
		gbx.setLubricant(0);
		PipingBlockEntity hose = pipe(h, RotaryBlocks.HOSE.get(), 4, 1, 3);
		pipe(h, RotaryBlocks.HOSE.get(), 4, 2, 4);
		h.runAfterDelay(2, () -> hose.addFluid(RotaryFluids.LUBRICANT.get(), 2000));
		h.succeedWhen(() -> {
			h.assertFalse(connected(h, 4, 2, 4, Direction.DOWN), "hose connected to the gearbox top");
			h.assertTrue(gbx.getLubricant() > 0, "gearbox got no lubricant");
		});
	}

	/** A pipe feeds a generic fluid handler (the Extractor's water tank) from the side. */
	@GameTest(template = PLATFORM, timeoutTicks = 300)
	public static void pipeFillsExtractor(GameTestHelper h) {
		ExtractorBlockEntity ex = place(h, RotaryBlocks.EXTRACTOR.get(), 4, 1, 4, 0, ExtractorBlockEntity.class);
		PipingBlockEntity p = pipe(h, RotaryBlocks.PIPE.get(), 5, 1, 4);
		h.runAfterDelay(2, () -> p.addFluid(Fluids.WATER, 4000));
		h.succeedWhen(() -> h.assertTrue(ex.getWater() > 0, "extractor got no water"));
	}

	/**
	 * Pump over a walled 3x3 water pool (y=1), gas engine to its north (512 rad/s @ 128 Nm = 64x the minimum power, so water is
	 * doubled twice: 4000 mB per source block); a liquid pipe on its east side takes water only up to the back pressure
	 * 1000*(1+log2(128/8)) = 5000 mB.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 600)
	public static void pumpFillsPipe(GameTestHelper h) {
		for (int x = 2; x <= 6; x++)
			for (int z = 2; z <= 6; z++)
				h.setBlock(new BlockPos(x, 1, z), x == 2 || x == 6 || z == 2 || z == 6 ? Blocks.STONE : Blocks.WATER);
		place(h, RotaryBlocks.GAS_ENGINE.get(), 4, 2, 3, 3, EngineBlockEntity.class).addFuel(50000);
		reika.rotarycraft.content.blockentity.PumpBlockEntity pump = place(h, RotaryBlocks.PUMP.get(), 4, 2, 4, 0, reika.rotarycraft.content.blockentity.PumpBlockEntity.class);
		PipingBlockEntity p = pipe(h, RotaryBlocks.PIPE.get(), 5, 2, 4);
		h.succeedWhen(() -> {
			h.assertTrue(pump.omega == 512 && pump.torque == 128, "pump input "+pump.omega+" rad/s @ "+pump.torque+" Nm");
			int total = pump.getFluidLevel()+p.getFluidLevel();
			h.assertTrue(total >= 8000 && total%4000 == 0, "pumped "+total+" mB (4000 per source block)");
			h.assertTrue(p.getFluidLevel() > 0 && p.getFluidLevel() <= 5000+5000/4, "pipe at "+p.getFluidLevel()+" mB");
		});
	}

	/** Two joined reservoirs with water level out (half the difference per step) and leave the wall between them out. */
	@GameTest(template = PLATFORM, timeoutTicks = 200)
	public static void reservoirsLevelOut(GameTestHelper h) {
		var a = place(h, RotaryBlocks.RESERVOIR.get(), 3, 1, 4, 0, reika.rotarycraft.content.blockentity.ReservoirBlockEntity.class);
		var b = place(h, RotaryBlocks.RESERVOIR.get(), 4, 1, 4, 0, reika.rotarycraft.content.blockentity.ReservoirBlockEntity.class);
		a.addLiquid(8000, Fluids.WATER);
		h.succeedWhen(() -> {
			h.assertTrue(a.isConnectedOnSide(Direction.EAST) && b.isConnectedOnSide(Direction.WEST), "reservoirs not joined");
			h.assertTrue(a.getFluidAmount()+b.getFluidAmount() == 8000 && Math.abs(a.getFluidAmount()-b.getFluidAmount()) <= 2,
					"levels "+a.getFluidAmount()+" / "+b.getFluidAmount());
		});
	}

	/** Different fluids do not mix: lava next to water stays apart, and the wall stays. */
	@GameTest(template = PLATFORM, timeoutTicks = 60)
	public static void reservoirsKeepFluidsApart(GameTestHelper h) {
		var a = place(h, RotaryBlocks.RESERVOIR.get(), 3, 1, 4, 0, reika.rotarycraft.content.blockentity.ReservoirBlockEntity.class);
		var b = place(h, RotaryBlocks.RESERVOIR.get(), 4, 1, 4, 0, reika.rotarycraft.content.blockentity.ReservoirBlockEntity.class);
		a.addLiquid(8000, Fluids.WATER);
		b.addLiquid(1000, RotaryFluids.LUBRICANT.get());
		h.runAtTickTime(40, () -> {
			h.assertTrue(a.getFluidAmount() == 8000 && b.getFluidAmount() == 1000, "levels "+a.getFluidAmount()+" / "+b.getFluidAmount());
			h.assertFalse(a.isConnectedOnSide(Direction.EAST), "wall left out between different fluids");
			h.succeed();
		});
	}

	/** A pipe under a reservoir drains it (the reservoir gives only downwards); the item of a filled reservoir keeps the contents. */
	@GameTest(template = PLATFORM, timeoutTicks = 200)
	public static void reservoirDrainsDown(GameTestHelper h) {
		var r = place(h, RotaryBlocks.RESERVOIR.get(), 4, 2, 4, 0, reika.rotarycraft.content.blockentity.ReservoirBlockEntity.class);
		r.addLiquid(8000, Fluids.WATER);
		var drop = r.collectComponents().get(reika.rotarycraft.registry.RotaryComponents.RESERVOIR_DATA.get());
		h.assertTrue(drop != null && drop.fluid().getAmount() == 8000, "item data "+drop);
		PipingBlockEntity p = pipe(h, RotaryBlocks.PIPE.get(), 4, 1, 4);
		PipingBlockEntity side = pipe(h, RotaryBlocks.PIPE.get(), 5, 2, 4);
		h.succeedWhen(() -> {
			h.assertTrue(p.getFluidLevel() > 0, "pipe below got nothing");
			h.assertTrue(r.getFluidAmount()+p.getFluidLevel()+side.getFluidLevel() == 8000, "water not conserved");
		});
	}

	/** A valve under a reservoir takes nothing until it gets a redstone signal. */
	@GameTest(template = PLATFORM, timeoutTicks = 200)
	public static void valveNeedsRedstone(GameTestHelper h) {
		var r = place(h, RotaryBlocks.RESERVOIR.get(), 4, 2, 4, 0, reika.rotarycraft.content.blockentity.ReservoirBlockEntity.class);
		r.addLiquid(8000, Fluids.WATER);
		PipingBlockEntity v = pipe(h, RotaryBlocks.VALVE.get(), 4, 1, 4);
		h.runAtTickTime(40, () -> {
			h.assertTrue(v.getFluidLevel() == 0, "closed valve took "+v.getFluidLevel());
			h.setBlock(new BlockPos(5, 1, 4), Blocks.REDSTONE_BLOCK);
		});
		h.runAtTickTime(120, () -> {
			h.assertTrue(v.getFluidLevel() > 0, "open valve took nothing");
			h.succeed();
		});
	}

	/** A separator takes from the side and gives only downwards; with a redstone signal only upwards. */
	@GameTest(template = PLATFORM, timeoutTicks = 200)
	public static void separatorGivesDown(GameTestHelper h) {
		PipingBlockEntity in = pipe(h, RotaryBlocks.PIPE.get(), 3, 2, 4);
		pipe(h, RotaryBlocks.SEPARATION.get(), 4, 2, 4);
		PipingBlockEntity down = pipe(h, RotaryBlocks.PIPE.get(), 4, 1, 4);
		PipingBlockEntity up = pipe(h, RotaryBlocks.PIPE.get(), 4, 3, 4);
		h.runAfterDelay(2, () -> in.addFluid(Fluids.WATER, 8000));
		h.runAtTickTime(100, () -> {
			h.assertTrue(down.getFluidLevel() > 0, "nothing below");
			h.assertTrue(up.getFluidLevel() == 0, "separator gave "+up.getFluidLevel()+" upwards");
			h.succeed();
		});
	}

	/** Two bypasses next to each other stay apart, unless one was placed against the other. */
	@GameTest(template = PLATFORM, timeoutTicks = 40)
	public static void bypassesStayApart(GameTestHelper h) {
		pipe(h, RotaryBlocks.BYPASS.get(), 3, 1, 4);
		pipe(h, RotaryBlocks.BYPASS.get(), 4, 1, 4);
		var c = (reika.rotarycraft.content.piping.FlowPipeBlockEntity)pipe(h, RotaryBlocks.BYPASS.get(), 3, 1, 6);
		pipe(h, RotaryBlocks.BYPASS.get(), 4, 1, 6);
		h.runAtTickTime(10, () -> {
			h.assertFalse(connected(h, 3, 1, 4, Direction.EAST), "bypasses joined without being placed against each other");
			c.onPlacedAgainst(Direction.EAST);
			c.recomputeConnections(h.getLevel(), c.getBlockPos());
		});
		h.runAtTickTime(30, () -> {
			h.assertTrue(connected(h, 3, 1, 6, Direction.EAST) && connected(h, 4, 1, 6, Direction.WEST), "placed-against bypasses not joined");
			h.succeed();
		});
	}

	/**
	 * A suction pipe takes nothing from pipes (it only pulls from other mods' fluid handlers; no RotaryCraft machine accepts it, as in
	 * the original) and does not join a reservoir.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 100)
	public static void suctionPipeIgnoresPipes(GameTestHelper h) {
		PipingBlockEntity full = pipe(h, RotaryBlocks.PIPE.get(), 3, 1, 4);
		PipingBlockEntity s = pipe(h, RotaryBlocks.SUCTION.get(), 4, 1, 4);
		place(h, RotaryBlocks.RESERVOIR.get(), 4, 2, 4, 0, reika.rotarycraft.content.blockentity.ReservoirBlockEntity.class).addLiquid(8000, Fluids.WATER);
		h.runAfterDelay(2, () -> full.addFluid(Fluids.WATER, 4000));
		h.runAtTickTime(60, () -> {
			h.assertTrue(connected(h, 4, 1, 4, Direction.WEST), "suction pipe not joined to the liquid pipe");
			h.assertFalse(connected(h, 4, 1, 4, Direction.UP), "suction pipe joined the reservoir");
			h.assertTrue(s.getFluidLevel() == 0 && full.getFluidLevel() == 4000, "suction pipe took "+s.getFluidLevel()+" from the pipe");
			h.succeed();
		});
	}

	/**
	 * Pipe pump (metadata 0: input side east) between two otherwise unconnected pipes, a microturbine (131072 rad/s, 16 Nm) feeding it
	 * from the south: it moves the east pipe's water into the west pipe (up to omega/4 mB per tick); both pipes connect to it.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 300)
	public static void pipePumpMovesWater(GameTestHelper h) {
		PipingBlockEntity dst = pipe(h, RotaryBlocks.PIPE.get(), 3, 1, 4);
		PipingBlockEntity src = pipe(h, RotaryBlocks.PIPE.get(), 5, 1, 4);
		reika.rotarycraft.content.blockentity.PipePumpBlockEntity pump = place(h, RotaryBlocks.PIPE_PUMP.get(), 4, 1, 4, 0,
				reika.rotarycraft.content.blockentity.PipePumpBlockEntity.class);
		place(h, RotaryBlocks.MICRO_ENGINE.get(), 4, 1, 5, 2, EngineBlockEntity.class).addFuel(50000);
		h.runAfterDelay(2, () -> src.addFluid(Fluids.WATER, 1000));
		h.succeedWhen(() -> {
			h.assertTrue(connected(h, 3, 1, 4, Direction.EAST) && connected(h, 5, 1, 4, Direction.WEST), "pipes not connected to the pump");
			h.assertTrue(dst.getFluidLevel() > 0 && dst.getFluidType() == Fluids.WATER, "west pipe "+dst.getFluidLevel()+" mB, pump "+pump.getOmega()+" rad/s "+pump.getPower()+" W");
		});
	}

	/** Without shaft power the pump moves nothing, and a pipe beside it (off its axis) does not connect. */
	@GameTest(template = PLATFORM, timeoutTicks = 60)
	public static void pipePumpNeedsPower(GameTestHelper h) {
		PipingBlockEntity dst = pipe(h, RotaryBlocks.PIPE.get(), 3, 1, 4);
		PipingBlockEntity src = pipe(h, RotaryBlocks.PIPE.get(), 5, 1, 4);
		pipe(h, RotaryBlocks.PIPE.get(), 4, 1, 3);
		place(h, RotaryBlocks.PIPE_PUMP.get(), 4, 1, 4, 0, reika.rotarycraft.content.blockentity.PipePumpBlockEntity.class);
		h.runAfterDelay(2, () -> src.addFluid(Fluids.WATER, 1000));
		h.runAtTickTime(50, () -> {
			h.assertTrue(dst.getFluidLevel() == 0, "moved "+dst.getFluidLevel()+" mB without power");
			h.assertFalse(connected(h, 4, 1, 3, Direction.SOUTH), "side pipe connected");
			h.succeed();
		});
	}
}
