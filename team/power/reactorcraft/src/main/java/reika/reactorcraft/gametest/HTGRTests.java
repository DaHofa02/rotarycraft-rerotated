/*******************************************************************************
 * Tests for the ported ReactorCraft high temperature gas reactor (original TileEntityPebbleBed, PebbleBedArrangement,
 * TileEntityCO2Heater). Private NeoForge 1.21.1 port of ReactorCraft by Reika Kalseki; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.reactorcraft.reactor.CO2HeaterBlockEntity;
import reika.reactorcraft.reactor.PebbleBedBlockEntity;
import reika.reactorcraft.registry.ReactorBlocks;
import reika.reactorcraft.registry.ReactorFluids;
import reika.reactorcraft.registry.ReactorItems;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class HTGRTests {

	private static final String PLATFORM = "platform_9x4x9";

	private static PebbleBedBlockEntity bed(GameTestHelper h, BlockPos p, int pellets) {
		h.setBlock(p, ReactorBlocks.PEBBLE_BED.get());
		PebbleBedBlockEntity te = (PebbleBedBlockEntity)h.getBlockEntity(p);
		for (int i = 0; i < pellets; i++)
			te.getInventory().setStackInSlot(46-i, new ItemStack(ReactorItems.PELLET.get()));
		return te;
	}

	/** Beds within 3 blocks of each other form one reactor (here 6 beds: 2 % per tick); one 4 blocks away stays on its own. */
	@GameTest(template = PLATFORM, timeoutTicks = 100)
	public static void pebbleBedArrangement(GameTestHelper h) {
		PebbleBedBlockEntity[] beds = new PebbleBedBlockEntity[6];
		for (int i = 0; i < 6; i++)
			beds[i] = bed(h, new BlockPos(1+(i%3)*3, 2, 1+(i/3)*3), 1);
		PebbleBedBlockEntity lone = bed(h, new BlockPos(7, 2, 8), 1);
		h.runAtTickTime(80, () -> {
			h.assertTrue(beds[0].getReactorSize() == 6 && beds[5].getReactorSize() == 6, "reactor size "+beds[0].getReactorSize());
			h.assertTrue(beds[0].getFissionChance() == 2, "fission chance "+beds[0].getFissionChance());
			h.assertTrue(lone.getReactorSize() == 1, "lone bed in a reactor of "+lone.getReactorSize());
			h.succeed();
		});
	}

	/** A 3x2 reactor of beds full of pebbles heats up (+20 C per reaction). */
	@GameTest(template = PLATFORM, timeoutTicks = 600)
	public static void pebbleBedHeats(GameTestHelper h) {
		PebbleBedBlockEntity[] beds = new PebbleBedBlockEntity[6];
		for (int i = 0; i < 6; i++)
			beds[i] = bed(h, new BlockPos(3+i%3, 2, 3+i/3), 20);
		int[] peak = {0};
		h.onEachTick(() -> {
			for (PebbleBedBlockEntity b : beds)
				peak[0] = Math.max(peak[0], b.getTemperature());
		});
		h.succeedWhen(() -> h.assertTrue(peak[0] >= 80, "beds peaked at only "+peak[0]+" C"));
	}

	/** Only the first slot can be taken out, in a 40 of 80 ticks window; spent pebbles at any time. */
	@GameTest(template = PLATFORM, timeoutTicks = 100)
	public static void pebbleBedRemoval(GameTestHelper h) {
		PebbleBedBlockEntity te = bed(h, new BlockPos(4, 2, 4), 0);
		te.getInventory().setStackInSlot(0, new ItemStack(ReactorItems.PELLET.get()));
		te.getInventory().setStackInSlot(46, new ItemStack(ReactorItems.OLD_PELLET.get()));
		var out = te.getItemHandler(net.minecraft.core.Direction.DOWN);
		h.assertTrue(out.extractItem(46, 1, false).is(ReactorItems.OLD_PELLET.get()), "spent pebble not removable");
		h.assertTrue(out.extractItem(5, 1, false).isEmpty(), "middle slot removable");
		h.succeedWhen(() -> h.assertTrue(!out.extractItem(0, 1, false).isEmpty(), "first slot never removable"));
	}

	/** A CO2 heater at 900 C (minimum 800 C) turns CO2 into hot CO2 once a second. */
	@GameTest(template = PLATFORM, timeoutTicks = 100)
	public static void co2HeaterMakesHotCO2(GameTestHelper h) {
		h.setBlock(new BlockPos(4, 2, 4), ReactorBlocks.CO2_HEATER.get());
		CO2HeaterBlockEntity te = (CO2HeaterBlockEntity)h.getBlockEntity(new BlockPos(4, 2, 4));
		te.fill(net.minecraft.core.Direction.DOWN, new FluidStack(ReactorFluids.get("co2"), 4000), IFluidHandler.FluidAction.EXECUTE);
		te.setTemperature(900);
		h.succeedWhen(() -> h.assertTrue(te.getOutputTank().getFluidAmount() >= 100 && te.getOutputTank().getFluid().getFluid().isSame(ReactorFluids.get("hot_co2")),
				"no hot CO2 at "+te.getTemperature()+" C"));
	}
}
