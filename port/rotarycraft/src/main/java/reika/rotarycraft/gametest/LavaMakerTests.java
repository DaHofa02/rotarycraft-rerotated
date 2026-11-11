/*******************************************************************************
 * Tests for the ported RotaryCraft rock melter (original TileEntityLavaMaker). Private NeoForge 1.21.1 port of RotaryCraft by Reika
 * Kalseki; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.BevelGearBlockEntity;
import reika.rotarycraft.content.blockentity.LavaMakerBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.content.piping.PipeConnector;
import reika.rotarycraft.content.piping.PipeType;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;
import reika.rotarycraft.registry.RotaryItems;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class LavaMakerTests {

	private static final String PLATFORM = "platform_9x8x9";

	private static <T extends BlockEntity> T place(GameTestHelper h, Block block, int x, int y, int z, int meta, Class<T> cls) {
		BlockPos p = new BlockPos(x, y, z);
		h.setBlock(p, block);
		BlockEntity be = h.getBlockEntity(p);
		if (be instanceof RotaryCraftBlockEntity r)
			r.setBlockMetadata(meta);
		return cls.cast(be);
	}

	/** Gas engine (512 rad/s @ 128 Nm = 65536 W, output east) -> bevel gears (west -> up) -> rock melter at (4, 3, 4). */
	private static LavaMakerBlockEntity rig(GameTestHelper h) {
		place(h, RotaryBlocks.GAS_ENGINE.get(), 3, 2, 4, 1, EngineBlockEntity.class).addFuel(50000);
		BevelGearBlockEntity bev = place(h, RotaryBlocks.BEVEL_GEARS.get(), 4, 2, 4, 0, BevelGearBlockEntity.class);
		bev.direction = BevelGearBlockEntity.getDirectionMap().indexOf(new BevelGearBlockEntity.IOPair(Direction.WEST, Direction.UP));
		return place(h, RotaryBlocks.LAVA_MAKER.get(), 4, 3, 4, 0, LavaMakerBlockEntity.class);
	}

	/**
	 * Netherrack needs 600 deg C and 480000 J: at 65536 W the first of three melts after 480000*20/65536 = 147 ticks (not within 100),
	 * each giving 2000 mB of lava. Hot (above 50 deg C), the melter also thaws a snow block and an ice block beside it. Empty, it
	 * signals 15.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 900)
	public static void melterTurnsNetherrackIntoLava(GameTestHelper h) {
		LavaMakerBlockEntity te = rig(h);
		te.setTemperature(700);
		te.getInventory().setStackInSlot(3, new ItemStack(Items.NETHERRACK, 3));
		BlockPos snow = new BlockPos(4, 3, 3), ice = new BlockPos(4, 3, 5);
		h.setBlock(snow, Blocks.SNOW_BLOCK);
		for (BlockPos p : new BlockPos[]{ice.below(), ice.west(), ice.east(), ice.south()}) //a basin for the melt water
			h.setBlock(p, Blocks.STONE);
		h.setBlock(ice, Blocks.ICE);
		h.assertTrue(te.getRedstoneOverride() == 0 && te.getMeltingTemperature() == 600, "melting point "+te.getMeltingTemperature());
		h.runAtTickTime(100, () -> {
			h.assertTrue(te.getTank().isEmpty() && te.getEnergy() > 0, "after 100 ticks: "+te.getTank().getFluidAmount()+" mB, "+te.getEnergy()+" J");
			h.assertBlockPresent(Blocks.AIR, snow);
			h.assertBlockPresent(Blocks.WATER, ice);
		});
		h.succeedWhen(() -> {
			h.assertTrue(h.getTick() > 100, "too early");
			h.assertTrue(te.getTank().getFluid().is(Fluids.LAVA) && te.getTank().getFluidAmount() == 6000, "tank holds "+te.getTank().getFluidAmount()+" mB");
			h.assertTrue(!te.hasStone() && te.getRedstoneOverride() == 15, "netherrack left: "+te.getInventory().getStackInSlot(3));
			h.assertTrue(te.getTemperature() >= 700 && te.getTemperature() <= LavaMakerBlockEntity.MAXTEMP, "temperature "+te.getTemperature());
		});
	}

	/**
	 * Stone needs 1000 deg C: at 300 the melter collects no energy; it only heats by log2(65536) = 16 deg C per second (less the loss of
	 * 1/64 of what it is above ambient). Once hot enough it stores its full power every tick (stone takes 5200000*20 J).
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 400)
	public static void melterNeedsItsTemperature(GameTestHelper h) {
		LavaMakerBlockEntity te = rig(h);
		te.setTemperature(300);
		te.getInventory().setStackInSlot(0, new ItemStack(Items.STONE, 2));
		h.runAtTickTime(100, () -> {
			h.assertTrue(te.power == 65536, "melter at "+te.omega+" rad/s @ "+te.torque+" Nm");
			h.assertTrue(te.getEnergy() == 0 && te.getTank().isEmpty(), "cold melter collected "+te.getEnergy()+" J");
			h.assertTrue(te.getTemperature() > 300 && te.getTemperature() <= 300+5*16, "temperature "+te.getTemperature());
			h.assertTrue(te.getMeltingTemperature() == 1000 && te.getRedstoneOverride() == 0, "melting point "+te.getMeltingTemperature());
			te.setTemperature(1100);
		});
		h.runAtTickTime(150, () -> {
			long e = te.getEnergy();
			h.assertTrue(e >= 45*65536L && e <= 50*65536L && e%65536 == 0, "hot melter collected "+e+" J in 50 ticks");
			h.assertTrue(te.getTank().isEmpty() && te.getInventory().getStackInSlot(0).getCount() == 2, "stone melted too early");
			h.succeed();
		});
	}

	/**
	 * Ethanol crystals melt at 180 deg C into 1000 mB of ethanol each (6000*20 J: two ticks). The melter never mixes liquids: netherrack
	 * waits while there is ethanol in the tank (signal 15). Only meltable items go in and none come out; the liquid leaves through the
	 * four sides only, into pipes and fuel lines.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 400)
	public static void melterMakesEthanolAndOnlyDrainsSideways(GameTestHelper h) {
		LavaMakerBlockEntity te = rig(h);
		te.setTemperature(700);
		te.getInventory().setStackInSlot(0, new ItemStack(RotaryItems.ETHANOL.get(), 2));
		te.getInventory().setStackInSlot(1, new ItemStack(Items.NETHERRACK));
		h.succeedWhen(() -> {
			h.assertTrue(te.getTank().getFluid().is(RotaryFluids.ETHANOL.get()) && te.getTank().getFluidAmount() == 2000, "tank holds "+te.getTank().getFluid());
			h.assertTrue(te.getInventory().getStackInSlot(0).isEmpty() && te.getInventory().getStackInSlot(1).getCount() == 1, "wrong items left");
			h.assertTrue(te.getRedstoneOverride() == 15, "signal "+te.getRedstoneOverride()+" with netherrack waiting for an ethanol tank");
			var items = te.getItemHandler(Direction.UP);
			h.assertTrue(items.insertItem(5, new ItemStack(Items.DIRT), true).getCount() == 1, "dirt accepted");
			h.assertTrue(items.insertItem(5, new ItemStack(Items.COBBLESTONE), true).isEmpty(), "cobblestone refused");
			h.assertTrue(items.extractItem(1, 1, true).isEmpty(), "items can be taken out");
			h.assertTrue(te.getFlowForSide(Direction.UP) == PipeConnector.Flow.NONE && te.getFlowForSide(Direction.NORTH) == PipeConnector.Flow.OUTPUT, "wrong flow");
			h.assertTrue(te.canConnectToPipeOnSide(PipeType.PIPE, Direction.WEST) && te.canConnectToPipeOnSide(PipeType.FUEL_LINE, Direction.WEST)
					&& !te.canConnectToPipeOnSide(PipeType.HOSE, Direction.WEST) && !te.canConnectToPipeOnSide(PipeType.PIPE, Direction.UP), "wrong pipe sides");
			h.assertTrue(te.getFluidHandler(Direction.UP).drain(1000, IFluidHandler.FluidAction.EXECUTE).isEmpty(), "drained from above");
			h.assertTrue(te.getFluidHandler(Direction.NORTH).fill(new FluidStack(RotaryFluids.ETHANOL.get(), 100), IFluidHandler.FluidAction.EXECUTE) == 0, "filled");
			FluidStack fs = te.getFluidHandler(Direction.NORTH).drain(1500, IFluidHandler.FluidAction.EXECUTE);
			h.assertTrue(fs.is(RotaryFluids.ETHANOL.get()) && fs.getAmount() == 1500 && te.getTank().getFluidAmount() == 500, "drained "+fs);
		});
	}
}
