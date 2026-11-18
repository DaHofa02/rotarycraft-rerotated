/*******************************************************************************
 * Tests for the port's fuel engine, distillery, boiler and steam turbine. Private NeoForge 1.21.1 port of RotaryCraft; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.BevelGearBlockEntity;
import reika.rotarycraft.content.blockentity.BoilerBlockEntity;
import reika.rotarycraft.content.blockentity.DistilleryBlockEntity;
import reika.rotarycraft.content.blockentity.FuelEngineBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.SteamTurbineBlockEntity;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class FluidMachineTests {

	private static final String PLATFORM = "platform_9x8x9";

	private static <T extends BlockEntity> T place(GameTestHelper h, Block block, int x, int y, int z, int meta, Class<T> cls) {
		BlockPos p = new BlockPos(x, y, z);
		h.setBlock(p, block);
		BlockEntity be = h.getBlockEntity(p);
		if (be instanceof RotaryCraftBlockEntity r)
			r.setBlockMetadata(meta);
		return cls.cast(be);
	}

	/** A fuel engine at (x, 2, 4) writing east (lava stands in for a fuel mod's fuel), with fuel and lubricant. */
	static FuelEngineBlockEntity fuelEngine(GameTestHelper h, int x) {
		FuelEngineBlockEntity.TEST_FUELS.add(Fluids.LAVA);
		FuelEngineBlockEntity e = place(h, RotaryBlocks.FUEL_ENGINE.get(), x, 2, 4, 1, FuelEngineBlockEntity.class);
		e.fill(null, new FluidStack(Fluids.LAVA, 20000), IFluidHandler.FluidAction.EXECUTE);
		e.fill(null, new FluidStack(RotaryFluids.LUBRICANT.get(), 4000), IFluidHandler.FluidAction.EXECUTE);
		return e;
	}

	/** Bevel gears at (x, 2, 4) taking from the west and driving up. */
	static void bevelUp(GameTestHelper h, int x) {
		BevelGearBlockEntity bev = place(h, RotaryBlocks.BEVEL_GEARS.get(), x, 2, 4, 0, BevelGearBlockEntity.class);
		bev.direction = BevelGearBlockEntity.getDirectionMap().indexOf(new BevelGearBlockEntity.IOPair(Direction.WEST, Direction.UP));
	}

	/**
	 * Fuel engine: no lubricant, no power; with it 2048 Nm at 256 rad/s; fuel goes down by 4 mB per 36 ticks; hot (600 C) it only reaches
	 * 706 - 600 = 106 rad/s; water is a separate tank.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 400)
	public static void fuelEngineBurnsFuel(GameTestHelper h) {
		FuelEngineBlockEntity.TEST_FUELS.add(Fluids.LAVA);
		FuelEngineBlockEntity e = place(h, RotaryBlocks.FUEL_ENGINE.get(), 4, 2, 4, 1, FuelEngineBlockEntity.class);
		e.fill(null, new FluidStack(Fluids.LAVA, 10000), IFluidHandler.FluidAction.EXECUTE);
		e.fill(null, new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
		h.assertTrue(e.getFuel() == 10000 && e.getWater() == 1000, "tanks "+e.getFuel()+"/"+e.getWater());
		int[] stage = {0};
		h.succeedWhen(() -> {
			switch (stage[0]) {
				case 0 -> {
					h.assertTrue(e.getOmega() == 0 && e.getFluidHandler(Direction.EAST) == null, "runs without lubricant, or takes fluid at the front");
					e.fill(Direction.WEST, new FluidStack(RotaryFluids.LUBRICANT.get(), 1000), IFluidHandler.FluidAction.EXECUTE);
					stage[0] = 1;
					h.fail("lubricated");
				}
				case 1 -> {
					h.assertTrue(e.getOmega() == 256 && e.getTorque() == 2048 && e.getFuel() < 10000, "running "+e.getOmega()+" rad/s "+e.getTorque()+" Nm, fuel "+e.getFuel());
					FuelEngineBlockEntity.allowOverheat = false;
					e.setTemperature(600);
					stage[0] = 2;
					h.fail("hot");
				}
				default -> {
					FuelEngineBlockEntity.allowOverheat = true;
					h.assertTrue(e.getGenOmega() == 106, "hot speed "+e.getGenOmega()+" at "+e.getTemperature());
					h.setBlock(new BlockPos(4, 2, 4), net.minecraft.world.level.block.Blocks.AIR);
				}
			}
		});
	}

	/** Fuel engine -> bevel gears -> distillery: "oil" (lava stands in) becomes 6 times as much lubricant, in the top tank. */
	@GameTest(template = PLATFORM, timeoutTicks = 400)
	public static void distilleryMakesLubricantFromOil(GameTestHelper h) {
		DistilleryBlockEntity.TEST_INPUTS.put(Fluids.LAVA, DistilleryBlockEntity.Conversion.OIL);
		fuelEngine(h, 2);
		bevelUp(h, 3);
		DistilleryBlockEntity d = place(h, RotaryBlocks.DISTILLERY.get(), 3, 3, 4, 0, DistilleryBlockEntity.class);
		h.assertTrue(d.fill(Direction.UP, new FluidStack(Fluids.LAVA, 100), IFluidHandler.FluidAction.EXECUTE) == 0, "filled from the top");
		d.fill(Direction.NORTH, new FluidStack(Fluids.LAVA, 100), IFluidHandler.FluidAction.EXECUTE);
		h.succeedWhen(() -> {
			h.assertTrue(d.getPower() >= 8192 && d.getTorque() >= 2048, "power "+d.getPower()+" W "+d.getTorque()+" Nm");
			FluidStack out = d.getOutputTank().getFluid();
			h.assertTrue(out.is(RotaryFluids.LUBRICANT.get()) && out.getAmount() >= 60 && out.getAmount() == 6*(100-d.getInputTank().getFluidAmount()), "lubricant "+out.getAmount()+", oil left "+d.getInputTank().getFluidAmount());
		});
	}

	/**
	 * Fuel engine -> bevel gears -> boiler (hot, with water) -> steam turbine on top (tier 0, 256 rad/s): the boiler's c:steam (from
	 * Mekanism in the test runs) drives the turbine.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 600)
	public static void boilerSteamDrivesTheTurbine(GameTestHelper h) {
		Fluid steam = BoilerBlockEntity.steamFluid();
		h.assertTrue(steam != null, "no fluid in c:steam (Mekanism not loaded?)");
		fuelEngine(h, 2);
		bevelUp(h, 3);
		BoilerBlockEntity b = place(h, RotaryBlocks.BOILER.get(), 3, 3, 4, 0, BoilerBlockEntity.class);
		b.fill(Direction.NORTH, new FluidStack(Fluids.WATER, 8000), IFluidHandler.FluidAction.EXECUTE);
		b.setTemperature(150);
		SteamTurbineBlockEntity t = place(h, RotaryBlocks.STEAM_TURBINE.get(), 3, 4, 4, 1, SteamTurbineBlockEntity.class);
		t.setSpeedStep(8);
		h.succeedWhen(() -> {
			h.assertTrue(b.getPower() > 0 && b.getTemperature() > 100, "boiler "+b.getPower()+" W at "+b.getTemperature()+" C");
			h.assertTrue(b.steamMade > 0 && b.getWaterTank().getFluidAmount() < 8000, "steam made "+b.steamMade);
			h.assertTrue(t.getOmega() == 256 && t.getTorque() == 8, "turbine "+t.getOmega()+" rad/s "+t.getTorque()+" Nm, steam "+t.getStoredEnergy()+", uses "+t.getConsumption());
		});
	}
}
