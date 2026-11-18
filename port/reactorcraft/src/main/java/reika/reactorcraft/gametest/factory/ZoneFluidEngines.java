/*******************************************************************************
 * Practice factory zone (see Factory): the port's fuel engine, distillery, boiler and steam turbine set up by a simulated survival player.
 * Not part of the original mods.
 ******************************************************************************/
package reika.reactorcraft.gametest.factory;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.rotarycraft.content.blockentity.BoilerBlockEntity;
import reika.rotarycraft.content.blockentity.DistilleryBlockEntity;
import reika.rotarycraft.content.blockentity.FuelEngineBlockEntity;
import reika.rotarycraft.content.blockentity.SteamTurbineBlockEntity;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneFluidEngines {

	public static final List<String> BLOCKS = List.of("rotarycraft:fuel_engine", "rotarycraft:distillery", "rotarycraft:boiler", "rotarycraft:steam_turbine");

	private static final String PLATFORM = "platform_9x8x9";

	/** The player sets the fuel engine (looking east: drives west) and fills it the way pipes would (lava stands in for a mod's fuel). */
	private static FuelEngineBlockEntity engine(GameTestHelper h, SimPlayer p, BlockPos pos) {
		FuelEngineBlockEntity.TEST_FUELS.add(Fluids.LAVA);
		p.place(pos, RotaryBlocks.FUEL_ENGINE.get(), Direction.EAST);
		FuelEngineBlockEntity e = (FuelEngineBlockEntity)h.getBlockEntity(pos);
		e.fill(Direction.EAST, new FluidStack(Fluids.LAVA, 20000), IFluidHandler.FluidAction.EXECUTE);
		e.fill(Direction.EAST, new FluidStack(RotaryFluids.LUBRICANT.get(), 4000), IFluidHandler.FluidAction.EXECUTE);
		return e;
	}

	/** Fuel engine -> bevel gears (placed last: west -> up... found by themselves) -> distillery on top making lubricant from "oil". */
	@GameTest(template = PLATFORM, timeoutTicks = 400, batch = "factory")
	public static void fuelEngineDrivesADistillery(GameTestHelper h) {
		DistilleryBlockEntity.TEST_INPUTS.put(Fluids.LAVA, DistilleryBlockEntity.Conversion.OIL);
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.CREATIVE);
		BlockPos eng = new BlockPos(5, 2, 4), bev = new BlockPos(4, 2, 4), dist = new BlockPos(4, 3, 4);
		engine(h, p, eng);
		p.place(new BlockPos(4, 2, 5), net.minecraft.world.item.Items.STONE, Direction.EAST); //something to set the distillery against
		p.place(new BlockPos(4, 3, 5), net.minecraft.world.item.Items.STONE, Direction.EAST);
		p.placeSneaking(dist.above(0), new ItemStack(RotaryBlocks.DISTILLERY.get()), Direction.EAST);
		p.placeSneaking(bev, new ItemStack(RotaryBlocks.BEVEL_GEARS.get()), Direction.EAST); //last: finds east -> up
		DistilleryBlockEntity d = (DistilleryBlockEntity)h.getBlockEntity(dist);
		d.fill(Direction.NORTH, new FluidStack(Fluids.LAVA, 200), IFluidHandler.FluidAction.EXECUTE); //what a pipe would bring
		h.succeedWhen(() -> {
			h.assertTrue(d.getOutputTank().getFluid().is(RotaryFluids.LUBRICANT.get()) && d.made >= 30, "lubricant "+d.made+", "+d.getPower()+" W "+d.getTorque()+" Nm");
			Factory.assertBuilt(h, List.of("rotarycraft:fuel_engine", "rotarycraft:distillery"), new BlockPos(0, 2, 0), new BlockPos(8, 4, 8));
			p.leave();
		});
	}

	/** Fuel engine -> bevel gears -> boiler -> steam turbine on top, set to 256 rad/s in its GUI: steam power. */
	@GameTest(template = PLATFORM, timeoutTicks = 900, batch = "factory")
	public static void boilerDrivesASteamTurbine(GameTestHelper h) {
		h.assertTrue(BoilerBlockEntity.steamFluid() != null, "no fluid in c:steam");
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.CREATIVE);
		BlockPos eng = new BlockPos(5, 2, 4), bev = new BlockPos(4, 2, 4), boil = new BlockPos(4, 3, 4), turb = new BlockPos(4, 4, 4);
		engine(h, p, eng);
		for (int y = 2; y <= 4; y++)
			p.place(new BlockPos(4, y, 5), net.minecraft.world.item.Items.STONE, Direction.EAST);
		p.placeSneaking(boil, new ItemStack(RotaryBlocks.BOILER.get()), Direction.EAST);
		p.placeSneaking(turb, new ItemStack(RotaryBlocks.STEAM_TURBINE.get()), Direction.EAST); //steam in from below, drives west
		p.placeSneaking(bev, new ItemStack(RotaryBlocks.BEVEL_GEARS.get()), Direction.EAST);
		BoilerBlockEntity b = (BoilerBlockEntity)h.getBlockEntity(boil);
		b.fill(Direction.NORTH, new FluidStack(Fluids.WATER, 20000), IFluidHandler.FluidAction.EXECUTE); //what a pipe would bring
		h.assertTrue(p.open(turb) != null, "turbine opens no GUI");
		for (int i = 0; i < 9; i++)
			p.player.containerMenu.clickMenuButton(p.player, 1);
		p.closeMenu();
		SteamTurbineBlockEntity t = (SteamTurbineBlockEntity)h.getBlockEntity(turb);
		h.succeedWhen(() -> {
			h.assertTrue(t.getOmega() == 256, "turbine "+t.getOmega()+" rad/s, steam "+t.getStoredEnergy()+"; boiler "+b.getTemperature()+" C, "+b.getPower()+" W, made "+b.steamMade);
			Factory.assertBuilt(h, List.of("rotarycraft:boiler", "rotarycraft:steam_turbine"), new BlockPos(0, 2, 0), new BlockPos(8, 5, 8));
			p.leave();
		});
	}
}
