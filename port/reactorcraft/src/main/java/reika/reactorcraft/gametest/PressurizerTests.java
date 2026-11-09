/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.material.Fluids;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.reactorcraft.powergen.ReactorPumpBlockEntity;
import reika.reactorcraft.registry.ReactorBlocks;
import reika.reactorcraft.registry.ReactorFluids;
import reika.rotarycraft.content.block.GearboxBlock;
import reika.rotarycraft.content.blockentity.BevelGearBlockEntity;
import reika.rotarycraft.content.blockentity.GearboxBlockEntity;
import reika.rotarycraft.content.blockentity.ReservoirBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.content.piping.PipingBlockEntity;
import reika.rotarycraft.registry.GearboxTypes;
import reika.rotarycraft.registry.RotaryBlocks;

/**
 * Original TileEntityReactorPump, the Pressurizer (see ReactorPumpBlockEntity): it needs 16384 W, 1024 Nm and 1 rad/s from below and then
 * turns low pressure water/ammonia into water/ammonia, as much per tick as the 12000 mB output tank takes. Power here: a gas engine
 * (512 rad/s @ 128 Nm) -> bedrock gearbox (reduction) -> bevel gears turning it up into the pressurizer standing on them.
 */
@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class PressurizerTests {

	private static final String PLATFORM = "platform_9x4x9";
	private static final BlockPos PUMP = new BlockPos(4, 2, 4);

	/** Engine (6,1,4) -> gearbox 1:ratio (5,1,4) -> bevel gears east -> up (4,1,4) -> pressurizer (4,2,4) with the given input. */
	private static ReactorPumpBlockEntity setup(GameTestHelper h, int ratio, String fluid, int amount) {
		h.setBlock(new BlockPos(6, 1, 4), RotaryBlocks.GAS_ENGINE.get());
		EngineBlockEntity eng = (EngineBlockEntity)h.getBlockEntity(new BlockPos(6, 1, 4));
		eng.setBlockMetadata(0);
		eng.addFuel(50000);
		h.setBlock(new BlockPos(5, 1, 4), RotaryBlocks.GEARBOX.get());
		GearboxBlockEntity gbx = (GearboxBlockEntity)h.getBlockEntity(new BlockPos(5, 1, 4));
		gbx.applyComponentsFromItemStack(GearboxBlock.gearboxItem(GearboxTypes.BEDROCK, ratio));
		gbx.setBlockMetadata((Integer.numberOfTrailingZeros(ratio)-1)*4); //ratio index * 4 + direction 0
		h.setBlock(new BlockPos(4, 1, 4), RotaryBlocks.BEVEL_GEARS.get());
		BevelGearBlockEntity bev = (BevelGearBlockEntity)h.getBlockEntity(new BlockPos(4, 1, 4));
		bev.direction = BevelGearBlockEntity.getDirectionMap().indexOf(new BevelGearBlockEntity.IOPair(Direction.EAST, Direction.UP));
		h.setBlock(PUMP, ReactorBlocks.PRESSURIZER.get());
		ReactorPumpBlockEntity te = (ReactorPumpBlockEntity)h.getBlockEntity(PUMP);
		h.assertTrue(te.fill(Direction.UP, new FluidStack(ReactorFluids.get(fluid), amount), IFluidHandler.FluidAction.EXECUTE) == amount, "input not filled");
		return te;
	}

	/**
	 * 16:1 -> 32 rad/s @ 2048 Nm (65536 W): the 8000 mB of low pressure water become 8000 mB of water in one tick. Then low pressure
	 * ammonia is let in: the output tank holds water, so the ammonia waits in the input tank (the original converts only into an empty
	 * output tank or one with the matching fluid).
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 400)
	public static void pressurizerTurnsLowPressureWaterIntoWater(GameTestHelper h) {
		ReactorPumpBlockEntity te = setup(h, 16, "low_pressure_water", 8000);
		h.startSequence().thenWaitUntil(() -> {
			h.assertTrue(te.getOmega() == 32 && te.getTorque() == 2048 && te.sufficientPower(), "pressurizer input "+te.getOmega()+" rad/s @ "+te.getTorque()+" Nm");
			h.assertTrue(te.getOutputTank().getFluid().getFluid().isSame(Fluids.WATER) && te.getOutputTank().getFluidAmount() == 8000,
					"output "+te.getOutputTank().getFluid().getFluid()+" "+te.getOutputTank().getFluidAmount()+" mB");
			h.assertTrue(te.getTank().isEmpty(), "input left "+te.getTank().getFluidAmount()+" mB");
		}).thenExecute(() -> te.fill(Direction.UP, new FluidStack(ReactorFluids.get("low_pressure_ammonia"), 4000), IFluidHandler.FluidAction.EXECUTE))
				.thenExecuteAfter(20, () -> {
					h.assertTrue(te.getTank().getFluidAmount() == 4000, "ammonia converted into a water tank: input "+te.getTank().getFluidAmount()+" mB");
					h.assertTrue(te.getOutputTank().getFluidAmount() == 8000 && te.getOutputTank().getFluid().getFluid().isSame(Fluids.WATER), "output changed");
				}).thenSucceed();
	}

	/** 4:1 -> 128 rad/s @ 512 Nm: 65536 W, but under the 1024 Nm minimum - nothing is converted. */
	@GameTest(template = PLATFORM, timeoutTicks = 400)
	public static void pressurizerNeedsTorque(GameTestHelper h) {
		ReactorPumpBlockEntity te = setup(h, 4, "low_pressure_water", 8000);
		h.succeedWhen(() -> {
			h.assertTrue(te.getOmega() == 128 && te.getTorque() == 512 && te.getPower() == 65536, "pressurizer input "+te.getOmega()+" rad/s @ "+te.getTorque()+" Nm");
			h.assertTrue(!te.sufficientPower() && te.getOutputTank().isEmpty() && te.getTank().getFluidAmount() == 8000,
					"converted without enough torque: output "+te.getOutputTank().getFluidAmount()+" mB");
		});
	}

	/**
	 * Low pressure ammonia becomes ammonia and leaves through the sides: the pipe on the north side gets a quarter (1500 mB of 6000), the
	 * reservoir on the south side, a plain fluid handler, the remaining 4500 mB. Pipes connect on the top and the sides, not below.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 400)
	public static void pressurizerFeedsPipeAndTankBeside(GameTestHelper h) {
		h.setBlock(PUMP.north(), RotaryBlocks.PIPE.get());
		h.setBlock(PUMP.south(), RotaryBlocks.RESERVOIR.get());
		ReactorPumpBlockEntity te = setup(h, 16, "low_pressure_ammonia", 6000);
		PipingBlockEntity pipe = (PipingBlockEntity)h.getBlockEntity(PUMP.north());
		ReservoirBlockEntity res = (ReservoirBlockEntity)h.getBlockEntity(PUMP.south());
		h.succeedWhen(() -> {
			h.assertTrue(te.sufficientPower(), "pressurizer input "+te.getOmega()+" rad/s @ "+te.getTorque()+" Nm");
			h.assertTrue(te.getTank().isEmpty() && te.getOutputTank().isEmpty(), "pressurizer still holds "+te.getTank().getFluidAmount()+" / "+te.getOutputTank().getFluidAmount()+" mB");
			h.assertTrue(pipe.getFluidType() != null && pipe.getFluidType().isSame(ReactorFluids.get("ammonia")) && pipe.getFluidLevel() == 1500,
					"pipe: "+pipe.getFluidType()+" "+pipe.getFluidLevel()+" mB");
			h.assertTrue(res.getFluid() != null && res.getFluid().isSame(ReactorFluids.get("ammonia")) && res.getFluidAmount() == 4500,
					"reservoir: "+res.getFluid()+" "+res.getFluidAmount()+" mB");
			h.assertTrue(te.canConnectToPipeOnSide(reika.rotarycraft.content.piping.PipeType.PIPE, Direction.UP)
					&& te.canConnectToPipeOnSide(reika.rotarycraft.content.piping.PipeType.PIPE, Direction.NORTH)
					&& !te.canConnectToPipeOnSide(reika.rotarycraft.content.piping.PipeType.PIPE, Direction.DOWN), "pipe sides");
		});
	}

	/**
	 * Without power: only low pressure water/ammonia, only from the top, is taken in (12000 mB); the output can be drained from the sides
	 * only while the input tank holds fluid (original canDrain looked at the input tank) - and with an empty output nothing comes out.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 20)
	public static void pressurizerInputRules(GameTestHelper h) {
		h.setBlock(PUMP, ReactorBlocks.PRESSURIZER.get());
		ReactorPumpBlockEntity te = (ReactorPumpBlockEntity)h.getBlockEntity(PUMP);
		FluidStack lpw = new FluidStack(ReactorFluids.get("low_pressure_water"), 20000);
		h.assertTrue(te.fill(Direction.NORTH, lpw, IFluidHandler.FluidAction.EXECUTE) == 0, "filled from the side");
		h.assertTrue(te.fill(Direction.DOWN, lpw, IFluidHandler.FluidAction.EXECUTE) == 0, "filled from below");
		h.assertTrue(te.fill(Direction.UP, new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE) == 0, "took plain water");
		h.assertTrue(te.drain(Direction.NORTH, 1000, IFluidHandler.FluidAction.EXECUTE).isEmpty(), "drained from an empty pressurizer");
		h.assertTrue(te.fill(Direction.UP, lpw, IFluidHandler.FluidAction.EXECUTE) == ReactorPumpBlockEntity.CAPACITY, "input capacity");
		h.assertTrue(te.getFlowForSide(Direction.UP) == reika.rotarycraft.content.piping.PipeConnector.Flow.INPUT
				&& te.getFlowForSide(Direction.EAST) == reika.rotarycraft.content.piping.PipeConnector.Flow.NONE, "pipe flows");
		h.runAtTickTime(10, () -> {
			h.assertTrue(te.getTank().getFluidAmount() == ReactorPumpBlockEntity.CAPACITY && te.getOutputTank().isEmpty(), "converted without power");
			h.succeed();
		});
	}
}
