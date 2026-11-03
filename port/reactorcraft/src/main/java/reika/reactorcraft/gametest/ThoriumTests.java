/*******************************************************************************
 * Tests for the ported ReactorCraft thorium fuel chain (original ReactorRecipes LiFBe fuel conversion in the RotaryCraft Fuel Enhancer).
 * Private NeoForge 1.21.1 port of ReactorCraft by Reika Kalseki; not for distribution.
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
import reika.reactorcraft.registry.ReactorFluids;
import reika.reactorcraft.registry.ReactorItems;
import reika.rotarycraft.content.blockentity.FuelEnhancerBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ThoriumTests {

	private static final String PLATFORM = "platform_9x4x9";

	/**
	 * Microturbine (131072 rad/s @ 16 Nm) -> bevel gears up -> Fuel Enhancer with LiFBe and thorium dust: it makes LiFBe fuel
	 * (speed 10 * (1 + log2(131072/16384)/2) = 20 mB per tick, 1:1), without thorium dust nothing.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 800)
	public static void fuelEnhancerMakesLiFBeFuel(GameTestHelper h) {
		h.setBlock(new BlockPos(5, 1, 4), RotaryBlocks.MICRO_ENGINE.get());
		EngineBlockEntity eng = (EngineBlockEntity)h.getBlockEntity(new BlockPos(5, 1, 4));
		eng.setBlockMetadata(0);
		eng.addFuel(50000);
		h.setBlock(new BlockPos(4, 1, 4), RotaryBlocks.BEVEL_GEARS.get());
		var bev = (reika.rotarycraft.content.blockentity.BevelGearBlockEntity)h.getBlockEntity(new BlockPos(4, 1, 4));
		bev.direction = reika.rotarycraft.content.blockentity.BevelGearBlockEntity.getDirectionMap().indexOf(
				new reika.rotarycraft.content.blockentity.BevelGearBlockEntity.IOPair(Direction.EAST, Direction.UP));
		h.setBlock(new BlockPos(4, 2, 4), RotaryBlocks.FUEL_ENHANCER.get());
		FuelEnhancerBlockEntity fe = (FuelEnhancerBlockEntity)h.getBlockEntity(new BlockPos(4, 2, 4));
		fe.fill(Direction.UP, new FluidStack(ReactorFluids.get("lifbe"), 4000), IFluidHandler.FluidAction.EXECUTE);
		h.runAtTickTime(60, () -> {
			h.assertTrue(fe.getOutputTank().isEmpty(), "made fuel without thorium dust");
			fe.getInventory().setStackInSlot(0, new ItemStack(ReactorItems.raw(ReactorItems.Raw.THORDUST).get(), 16));
		});
		h.runAfterDelay(61, () -> h.succeedWhen(() -> h.assertTrue(fe.getOutputTank().getFluid().getFluid().isSame(ReactorFluids.get("lifbe_fuel"))
				&& fe.getOutputTank().getFluidAmount() > 0, "no LiFBe fuel ("+fe.getOmega()+" rad/s, "+fe.getPower()+" W)"))); //the microturbine needs a while to reach speed
	}

	private static reika.reactorcraft.reactor.ThoriumCoreBlockEntity core(GameTestHelper h, BlockPos p, int fuel, int temp) {
		h.setBlock(p, reika.reactorcraft.registry.ReactorBlocks.THORIUM_CORE.get());
		var te = (reika.reactorcraft.reactor.ThoriumCoreBlockEntity)h.getBlockEntity(p);
		te.fill(Direction.UP, new FluidStack(ReactorFluids.get("lifbe_fuel"), fuel), IFluidHandler.FluidAction.EXECUTE);
		te.setTemperature(temp);
		return te;
	}

	/** Below 400 C the thorium core does not react; at 800 C fission neutrons burn its fuel into hot LiFBe (+50 C per reaction). */
	@GameTest(template = PLATFORM)
	public static void thoriumCoreReacts(GameTestHelper h) {
		var cold = core(h, new BlockPos(2, 2, 4), 4000, 300);
		var hot = core(h, new BlockPos(6, 2, 4), 4000, 800);
		var n = new reika.reactorcraft.content.entity.NeutronEntity(h.getLevel(), h.absolutePos(new BlockPos(4, 2, 4)), Direction.EAST,
				reika.reactorcraft.content.entity.NeutronEntity.NeutronType.FISSION);
		for (int i = 0; i < 200; i++) {
			cold.onNeutron(n, h.getLevel(), h.absolutePos(new BlockPos(2, 2, 4)));
			hot.onNeutron(n, h.getLevel(), h.absolutePos(new BlockPos(6, 2, 4)));
		}
		h.assertTrue(cold.getFuelTank().getFluidAmount() == 4000 && cold.getOutputTank().isEmpty(), "cold core reacted");
		h.assertTrue(hot.getFuelTank().getFluidAmount() < 4000 && hot.getOutputTank().getFluid().getFluid().isSame(ReactorFluids.get("hot_lifbe")), "hot core did not react");
		h.assertTrue(hot.getTemperature() > 800, "hot core at "+hot.getTemperature()+" C");
		h.succeed();
	}

	/** From 1100 C a fuel dump under the core drains its fuel and lets it out below as thorium fuel. */
	@GameTest(template = PLATFORM, timeoutTicks = 100)
	public static void fuelDumpDrainsHotCore(GameTestHelper h) {
		var tc = core(h, new BlockPos(4, 3, 4), 2000, 1150);
		h.setBlock(new BlockPos(4, 2, 4), reika.reactorcraft.registry.ReactorBlocks.FUEL_DUMP.get());
		h.setBlock(new BlockPos(4, 1, 4), net.minecraft.world.level.block.Blocks.AIR);
		h.succeedWhen(() -> {
			h.assertTrue(tc.getFuelTank().getFluidAmount() < 2000, "fuel not drained");
			h.assertBlockPresent(reika.reactorcraft.registry.ReactorBlocks.THORIUM_FUEL.get(), new BlockPos(4, 1, 4));
		});
	}

	/** Liquid waste leaves a thorium core only into a waste pipe beside it. */
	@GameTest(template = PLATFORM, timeoutTicks = 60)
	public static void wastePipeTakesLiquidWaste(GameTestHelper h) {
		var tc = core(h, new BlockPos(3, 2, 4), 0, 25);
		tc.getWasteTank().fill(new FluidStack(ReactorFluids.get("nuclear_waste"), 1000), IFluidHandler.FluidAction.EXECUTE);
		h.setBlock(new BlockPos(4, 2, 4), reika.reactorcraft.registry.ReactorBlocks.WASTE_PIPE.get());
		h.setBlock(new BlockPos(5, 2, 4), reika.reactorcraft.registry.ReactorBlocks.WASTE_PIPE.get());
		var pipe = (reika.reactorcraft.waste.WastePipeBlockEntity)h.getBlockEntity(new BlockPos(5, 2, 4));
		h.succeedWhen(() -> {
			h.assertTrue(h.getBlockState(new BlockPos(4, 2, 4)).getValue(net.minecraft.world.level.block.PipeBlock.WEST), "pipe not connected to the core");
			h.assertTrue(pipe.getFluidLevel() > 0 && tc.getWasteTank().getFluidAmount() < 1000, "waste not in the pipe");
		});
	}

	/** Spilled thorium fuel falls into free space and levels out (Forge finite fluid rules). */
	@GameTest(template = PLATFORM, timeoutTicks = 200)
	public static void thoriumFuelFlows(GameTestHelper h) {
		h.setBlock(new BlockPos(4, 3, 4), reika.reactorcraft.registry.ReactorBlocks.THORIUM_FUEL.get().defaultBlockState());
		h.succeedWhen(() -> {
			h.assertBlockNotPresent(reika.reactorcraft.registry.ReactorBlocks.THORIUM_FUEL.get(), new BlockPos(4, 3, 4));
			int blocks = 0;
			for (int x = 2; x <= 6; x++)
				for (int z = 2; z <= 6; z++)
					if (h.getBlockState(new BlockPos(x, 2, z)).is(reika.reactorcraft.registry.ReactorBlocks.THORIUM_FUEL.get()))
						blocks++;
			h.assertTrue(blocks >= 4, "fuel spread to only "+blocks+" blocks");
		});
	}
}
