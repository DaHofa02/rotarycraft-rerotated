/*******************************************************************************
 * Tests for the ported ReactorCraft breeder reactor and plutonium fuel (original TileEntityBreederCore, TileEntitySodiumHeater,
 * ReactorFuel.PLUTONIUM). Private NeoForge 1.21.1 port of ReactorCraft by Reika Kalseki; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.reactorcraft.content.entity.NeutronEntity;
import reika.reactorcraft.content.item.FuelItem;
import reika.reactorcraft.reactor.BreederCoreBlockEntity;
import reika.reactorcraft.reactor.FuelRodBlockEntity;
import reika.reactorcraft.reactor.SodiumHeaterBlockEntity;
import reika.reactorcraft.registry.ReactorBlocks;
import reika.reactorcraft.registry.ReactorComponents;
import reika.reactorcraft.registry.ReactorFluids;
import reika.reactorcraft.registry.ReactorItems;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class BreederTests {

	private static final String PLATFORM = "platform_9x4x9";

	/** Plutonium is fuel for the fuel core; its last depletion step uses it up (uranium becomes depleted uranium). */
	@GameTest(template = PLATFORM)
	public static void plutoniumFuel(GameTestHelper h) {
		h.setBlock(new BlockPos(4, 2, 4), ReactorBlocks.FUEL_ROD.get());
		FuelRodBlockEntity te = (FuelRodBlockEntity)h.getBlockEntity(new BlockPos(4, 2, 4));
		te.getInventory().setStackInSlot(3, new ItemStack(ReactorItems.PLUTONIUM.get()));
		h.assertTrue(te.isFissile(), "plutonium not fissile");
		ItemStack last = new ItemStack(ReactorItems.PLUTONIUM.get());
		last.set(ReactorComponents.DEPLETION.get(), 99);
		h.assertTrue(FuelRodBlockEntity.ReactorFuel.PLUTONIUM.getFissionProduct(last).isEmpty(), "plutonium not used up");
		ItemStack lastU = new ItemStack(ReactorItems.FUEL.get());
		lastU.set(ReactorComponents.DEPLETION.get(), 99);
		h.assertTrue(FuelRodBlockEntity.ReactorFuel.URANIUM.getFissionProduct(lastU).is(ReactorItems.DEPLETED.get()), "uranium not depleted");
		h.succeed();
	}

	/**
	 * Breeder neutrons convert breeder fuel step by step (5 % per reaction, +50 C), each reaction heats the core. The waste the reactions
	 * leave (10 %) is taken out after every neutron, as a hopper would: waste poisons the core (lost neutrons, 1/(9-n)) and with 8 pieces
	 * stops it, which made a fixed 400 neutrons fail by chance now and then (no conversion before the core was poisoned).
	 */
	@GameTest(template = PLATFORM)
	public static void breederConvertsFuel(GameTestHelper h) {
		h.setBlock(new BlockPos(4, 2, 4), ReactorBlocks.BREEDER_CORE.get());
		BreederCoreBlockEntity te = (BreederCoreBlockEntity)h.getBlockEntity(new BlockPos(4, 2, 4));
		te.getInventory().setStackInSlot(3, new ItemStack(ReactorItems.BREEDER_FUEL.get()));
		int t0 = te.getTemperature();
		NeutronEntity n = new NeutronEntity(h.getLevel(), h.absolutePos(new BlockPos(3, 2, 4)), Direction.EAST, NeutronEntity.NeutronType.BREEDER);
		ItemStack fuel = te.getInventory().getStackInSlot(3);
		for (int i = 0; i < 4000 && fuel.is(ReactorItems.BREEDER_FUEL.get()) && (FuelItem.getDepletion(fuel) == 0 || te.getTemperature() <= t0+500); i++) {
			te.onNeutron(n, h.getLevel(), h.absolutePos(new BlockPos(4, 2, 4)));
			for (int s = 4; s < 12; s++)
				te.getInventory().setStackInSlot(s, ItemStack.EMPTY);
			fuel = te.getInventory().getStackInSlot(3);
		}
		h.assertTrue(fuel.is(ReactorItems.BREEDER_FUEL.get()) && FuelItem.getDepletion(fuel) > 0 || fuel.isEmpty(), "no conversion: "+fuel);
		h.assertTrue(te.getTemperature() > t0+500, "core only at "+te.getTemperature()+" C");
		h.succeed();
	}

	/** A hot breeder core heats the sodium heater beside it, which then turns sodium into hot sodium (100 mB per second from 301 C). */
	@GameTest(template = PLATFORM, timeoutTicks = 200)
	public static void sodiumHeaterMakesHotSodium(GameTestHelper h) {
		h.setBlock(new BlockPos(3, 2, 4), ReactorBlocks.BREEDER_CORE.get());
		BreederCoreBlockEntity core = (BreederCoreBlockEntity)h.getBlockEntity(new BlockPos(3, 2, 4));
		core.setTemperature(1200);
		h.setBlock(new BlockPos(4, 2, 4), ReactorBlocks.SODIUM_HEATER.get());
		SodiumHeaterBlockEntity heater = (SodiumHeaterBlockEntity)h.getBlockEntity(new BlockPos(4, 2, 4));
		heater.getTank().fill(new FluidStack(ReactorFluids.get("sodium"), 4000), IFluidHandler.FluidAction.EXECUTE);
		h.succeedWhen(() -> {
			h.assertTrue(heater.getOutputTank().getFluidAmount() > 0 && heater.getOutputTank().getFluid().getFluid().isSame(ReactorFluids.get("hot_sodium")),
					"no hot sodium, heater at "+heater.getTemperature()+" C");
			h.assertTrue(heater.getTank().getFluidAmount() < 4000, "sodium not used");
		});
	}

	/**
	 * Gas engine (512 rad/s @ 128 Nm) -> bevel gears up -> heat exchanger with hot sodium: it cools 100 mB per tick into sodium,
	 * warms up and passes heat to the reactor boiler beside it, which then counts the heat as breeder heat.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 300)
	public static void heatExchangerHeatsBoiler(GameTestHelper h) {
		h.setBlock(new BlockPos(5, 1, 4), reika.rotarycraft.registry.RotaryBlocks.GAS_ENGINE.get());
		var eng = (reika.rotarycraft.content.blockentity.engine.EngineBlockEntity)h.getBlockEntity(new BlockPos(5, 1, 4));
		eng.setBlockMetadata(0);
		eng.addFuel(50000);
		h.setBlock(new BlockPos(4, 1, 4), reika.rotarycraft.registry.RotaryBlocks.BEVEL_GEARS.get());
		var bev = (reika.rotarycraft.content.blockentity.BevelGearBlockEntity)h.getBlockEntity(new BlockPos(4, 1, 4));
		bev.direction = reika.rotarycraft.content.blockentity.BevelGearBlockEntity.getDirectionMap().indexOf(
				new reika.rotarycraft.content.blockentity.BevelGearBlockEntity.IOPair(Direction.EAST, Direction.UP));
		h.setBlock(new BlockPos(4, 2, 4), ReactorBlocks.HEAT_EXCHANGER.get());
		var ex = (reika.reactorcraft.powergen.HeatExchangerBlockEntity)h.getBlockEntity(new BlockPos(4, 2, 4));
		ex.fill(Direction.UP, new FluidStack(ReactorFluids.get("hot_sodium"), 2000), IFluidHandler.FluidAction.EXECUTE);
		h.setBlock(new BlockPos(3, 2, 4), ReactorBlocks.BOILER.get());
		var boiler = (reika.reactorcraft.reactor.ReactorBoilerBlockEntity)h.getBlockEntity(new BlockPos(3, 2, 4));
		int b0 = boiler.getTemperature();
		h.succeedWhen(() -> {
			h.assertTrue(ex.sufficientPower(), "exchanger power "+ex.getPower()+" W @ "+ex.getOmega()+" rad/s");
			h.assertTrue(ex.getOutputTank().getFluid().getFluid().isSame(ReactorFluids.get("sodium")) && ex.getOutputTank().getFluidAmount() >= 100, "no cold sodium");
			h.assertTrue(boiler.getTemperature() > b0+20, "boiler at "+boiler.getTemperature()+" C (exchanger "+ex.getTemperature()+" C)");
			h.assertTrue(boiler.getReactorTypeFractions().getOrDefault(reika.reactorcraft.reactor.ReactorType.BREEDER, 0D) > 0, "heat not counted as breeder");
		});
	}
}
