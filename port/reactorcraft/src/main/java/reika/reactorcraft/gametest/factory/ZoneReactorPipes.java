/*******************************************************************************
 * Practice factory zone (see Factory): ReactorCraft gas duct and heat pipe, set up by a simulated survival player. Not part of the original mods.
 ******************************************************************************/
package reika.reactorcraft.gametest.factory;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.PipeBlock;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.reactorcraft.content.item.NuclearWasteItem;
import reika.reactorcraft.content.piping.HeatPipeBlockEntity;
import reika.reactorcraft.fusion.GasCollectorBlockEntity;
import reika.reactorcraft.radiation.Isotopes;
import reika.reactorcraft.registry.ReactorBlocks;
import reika.reactorcraft.registry.ReactorFluids;
import reika.reactorcraft.waste.WasteContainerBlockEntity;
import reika.rotarycraft.content.blockentity.FermenterBlockEntity;
import reika.rotarycraft.content.blockentity.ReservoirBlockEntity;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneReactorPipes {

	public static final List<String> BLOCKS = List.of("reactorcraft:gas_duct", "reactorcraft:heat_pipe");

	private static final String PLATFORM = "platform_9x8x9";

	/**
	 * Gas: the player lights a furnace (raw iron, coal), sets a gas collector on its east side and lays three gas ducts from the
	 * collector's output side to a reservoir - the CO2 the collector gathers ends up in the reservoir.
	 * Heat: the player fills a waste container with 27 short-lived waste items (Cs-134, together about 86 C of decay heat per second; on
	 * its own it would melt down at 600 C within about 7 s) and lays three heat pipes from it to a fermenter. The pipes keep drawing the
	 * container's heat (it stays far below meltdown for 15 s), carry it and warm the fermenter above ambient.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 400, batch = "factory")
	public static void ductsCarryGasAndHeat(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8);
		ItemStack waste = NuclearWasteItem.of(Isotopes.Cs134);
		Factory.infiniteChest(h, chest, new ItemStack(ReactorBlocks.GAS_DUCT.get()), new ItemStack(ReactorBlocks.HEAT_PIPE.get()),
				new ItemStack(ReactorBlocks.GAS_COLLECTOR.get()), new ItemStack(Items.FURNACE), new ItemStack(Items.COAL), new ItemStack(Items.RAW_IRON),
				new ItemStack(RotaryBlocks.RESERVOIR.get()), new ItemStack(ReactorBlocks.WASTE_CONTAINER.get()), new ItemStack(RotaryBlocks.FERMENTER.get()), waste);

		//gas: furnace -> collector -> 3 gas ducts -> reservoir
		Factory.take(p, chest, Items.FURNACE, 1);
		Factory.take(p, chest, ReactorBlocks.GAS_COLLECTOR.get().asItem(), 1);
		Factory.take(p, chest, Items.COAL, 1);
		Factory.take(p, chest, Items.RAW_IRON, 1);
		Factory.takeExactly(p, chest, ReactorBlocks.GAS_DUCT.get().asItem(), 3);
		Factory.take(p, chest, RotaryBlocks.RESERVOIR.get().asItem(), 1);
		BlockPos furnace = new BlockPos(1, 2, 2), collector = furnace.east(), res = new BlockPos(6, 2, 2);
		p.place(furnace, Items.FURNACE, Direction.NORTH);
		p.open(furnace);
		p.putInto(0, Items.RAW_IRON, 8);
		p.putInto(1, Items.COAL, 4);
		p.closeMenu();
		p.hold(new ItemStack(ReactorBlocks.GAS_COLLECTOR.get()));
		p.player.setShiftKeyDown(true);
		p.useOn(furnace, Direction.EAST); //onto the furnace's east side
		p.player.setShiftKeyDown(false);
		for (int x = 3; x <= 5; x++)
			p.placeSneaking(new BlockPos(x, 2, 2), new ItemStack(ReactorBlocks.GAS_DUCT.get()), Direction.EAST);
		p.placeSneaking(res, new ItemStack(RotaryBlocks.RESERVOIR.get()), Direction.EAST);
		GasCollectorBlockEntity gc = (GasCollectorBlockEntity)h.getBlockEntity(collector);
		ReservoirBlockEntity r = (ReservoirBlockEntity)h.getBlockEntity(res);

		//heat: waste container -> 3 heat pipes -> fermenter
		Factory.take(p, chest, ReactorBlocks.WASTE_CONTAINER.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.FERMENTER.get().asItem(), 1);
		Factory.takeExactly(p, chest, ReactorBlocks.HEAT_PIPE.get().asItem(), 3);
		Factory.take(p, chest, waste, 27);
		BlockPos cont = new BlockPos(2, 2, 6), ferm = new BlockPos(6, 2, 6);
		p.place(cont, ReactorBlocks.WASTE_CONTAINER.get(), Direction.NORTH);
		for (int x = 3; x <= 5; x++)
			p.placeSneaking(new BlockPos(x, 2, 6), new ItemStack(ReactorBlocks.HEAT_PIPE.get()), Direction.EAST);
		p.placeSneaking(ferm, new ItemStack(RotaryBlocks.FERMENTER.get()), Direction.EAST);
		p.open(cont);
		for (int i = 0; i < 27; i++)
			Factory.putExactly(p, i, waste, 1);
		p.closeMenu();
		WasteContainerBlockEntity c = (WasteContainerBlockEntity)h.getBlockEntity(cont);
		FermenterBlockEntity f = (FermenterBlockEntity)h.getBlockEntity(ferm);
		HeatPipeBlockEntity[] pipes = new HeatPipeBlockEntity[3];
		for (int i = 0; i < 3; i++)
			pipes[i] = (HeatPipeBlockEntity)h.getBlockEntity(new BlockPos(3+i, 2, 6));

		int[] maxCont = {Integer.MIN_VALUE}, maxFermExcess = {Integer.MIN_VALUE};
		double[] maxPipe = {0};
		h.onEachTick(() -> {
			if (c.isRemoved() || f.isRemoved())
				return;
			maxCont[0] = Math.max(maxCont[0], c.getTemperature());
			maxFermExcess[0] = Math.max(maxFermExcess[0], f.getTemperature()-f.getAmbientTemperature());
			for (HeatPipeBlockEntity hp : pipes)
				maxPipe[0] = Math.max(maxPipe[0], hp.getNetHeatEnergy());
		});
		h.runAtTickTime(320, () -> {
			h.assertTrue(gc.hasFurnace(), "gas collector has no lit furnace");
			for (int x = 3; x <= 5; x++)
				h.assertTrue(h.getBlockState(new BlockPos(x, 2, 2)).getValue(PipeBlock.WEST) && h.getBlockState(new BlockPos(x, 2, 2)).getValue(PipeBlock.EAST),
						"gas duct at x="+x+" not connected on both sides");
			h.assertTrue(r.getFluid() == ReactorFluids.get("co2") && r.getFluidAmount() > 0, "reservoir holds "+r.getFluidAmount()+" mB of "+r.getFluid());
			for (int x = 3; x <= 5; x++)
				h.assertTrue(h.getBlockState(new BlockPos(x, 2, 6)).getValue(PipeBlock.WEST) && h.getBlockState(new BlockPos(x, 2, 6)).getValue(PipeBlock.EAST),
						"heat pipe at x="+x+" not connected on both sides");
			//waste decays at random (as in the original): one or two of the 27 may be gone by now
			h.assertTrue(c.countWaste() >= 25 && c.countWaste() <= 27, "container holds "+c.countWaste()+" waste items");
			h.assertTrue(maxCont[0] < 300, "waste container reached "+maxCont[0]+" C despite the heat pipes");
			h.assertTrue(maxPipe[0] > 0, "the heat pipes never held heat above ambient");
			h.assertTrue(maxFermExcess[0] >= 1, "fermenter never warmed above ambient (max "+maxFermExcess[0]+")");
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 2, 8));
			p.leave();
			h.succeed();
		});
	}
}
