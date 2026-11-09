/*******************************************************************************
 * Practice factory zone (see Factory): ReactorCraft high temperature gas reactors (pebble beds), one heating a CO2 heater, one heating
 * a thorium core over an emergency fuel dump, built and started by a simulated survival player. Not part of the original mods.
 ******************************************************************************/
package reika.reactorcraft.gametest.factory;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.reactorcraft.reactor.CO2HeaterBlockEntity;
import reika.reactorcraft.reactor.FuelDumpBlockEntity;
import reika.reactorcraft.reactor.PebbleBedBlockEntity;
import reika.reactorcraft.reactor.ThoriumCoreBlockEntity;
import reika.reactorcraft.registry.ReactorBlocks;
import reika.reactorcraft.registry.ReactorFluids;
import reika.reactorcraft.registry.ReactorItems;
import reika.rotarycraft.content.piping.PipingBlockEntity;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneHTGR {

	public static final List<String> BLOCKS = List.of("reactorcraft:pebble_bed", "reactorcraft:co2_heater", "reactorcraft:thorium_core",
			"reactorcraft:fuel_dump", "reactorcraft:thorium_fuel");

	private static final String PLATFORM = "platform_16x8x9";

	/** The reactor layer at y = 2: x 3-8, z 2-6, walled in at x 2 / 9 and z 1 / 7 and roofed at y = 3. */
	private static final int X0 = 3, X1 = 8, Z0 = 2, Z1 = 6;

	/**
	 * A pebble bed reactor: the 6x5 layer full of pebble beds but for a CO2 heater on its north edge (29 beds, each given one fuel pebble
	 * through its GUI; beds within 3 blocks of each other are one reactor, which with 24-35 beds reacts with 6 % per tick and bed, +20 C
	 * each time), walled in and roofed with stone so that every bed is enclosed (it then loses heat four times slower than in the open).
	 * CO2 comes from a barrel through bedrock pipes laid into the floor up into the heater (the original pipe does not carry gases).
	 * The beds heat up; the heater beside three of them passes 800 C and turns CO2 into hot CO2.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 4000, batch = "factory")
	public static void pebbleBedsHeatCO2(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 15, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8);
		Factory.infiniteChest(h, chest, new ItemStack(ReactorBlocks.PEBBLE_BED.get()), new ItemStack(ReactorItems.PELLET.get()),
				new ItemStack(ReactorBlocks.CO2_HEATER.get()), new ItemStack(RotaryBlocks.BEDROCK_PIPE.get()), new ItemStack(Items.STONE),
				new ItemStack(Items.IRON_PICKAXE));
		BlockPos heater = new BlockPos(5, 2, Z0);
		Factory.takeExactly(p, chest, Items.IRON_PICKAXE, 1);
		Factory.takeExactly(p, chest, RotaryBlocks.BEDROCK_PIPE.get().asItem(), 3);
		Factory.takeExactly(p, chest, ReactorBlocks.CO2_HEATER.get().asItem(), 1);
		for (int z = 0; z <= Z0; z++) {
			p.mine(new BlockPos(5, 1, z), Items.IRON_PICKAXE);
			p.placeSneaking(new BlockPos(5, 1, z), new ItemStack(RotaryBlocks.BEDROCK_PIPE.get()), Direction.SOUTH);
		}
		p.placeSneaking(heater, new ItemStack(ReactorBlocks.CO2_HEATER.get()), Direction.SOUTH);
		PebbleBedBlockEntity mid = buildReactor(h, p, chest, List.of(heater), List.of());
		Factory.fluidBarrel(h, new BlockPos(5, 2, 0), ReactorFluids.get("co2"));

		CO2HeaterBlockEntity co2 = (CO2HeaterBlockEntity)h.getBlockEntity(heater);
		int[] peak = {0, 0};
		h.onEachTick(() -> {
			peak[0] = Math.max(peak[0], mid.getTemperature());
			peak[1] = Math.max(peak[1], co2.getTemperature());
		});
		h.succeedWhen(() -> {
			String state = "beds peak "+peak[0]+" C (reactor of "+mid.getReactorSize()+"), CO2 heater peak "+peak[1]+" C with "+co2.getTank().getFluidAmount()
					+" mB CO2, "+co2.getOutputTank().getFluidAmount()+" mB hot CO2";
			h.assertTrue(mid.getReactorSize() == 29, state);
			h.assertTrue(co2.getOutputTank().getFluidAmount() >= 100 && co2.getOutputTank().getFluid().getFluid().isSame(ReactorFluids.get("hot_co2")), "no hot CO2: "+state);
			Factory.assertBuilt(h, List.of("reactorcraft:pebble_bed", "reactorcraft:co2_heater"), new BlockPos(0, 1, 0), new BlockPos(15, 3, 8));
			p.leave();
		});
	}

	/**
	 * The same pebble bed reactor with a thorium core in its middle (four beds around it, 29 in all), standing on an emergency fuel dump
	 * set into the floor with a bedrock pipe under the dump. A fuel line from a barrel on the roof fills the core with LiFBe fuel; then
	 * the player takes the fuel line off and closes the roof (a core on an endless supply would be dumped without end). Seven uranium
	 * fuel cores (four pellets each through the GUI) outside the walls, in line with the thorium core, send decay neutrons through the
	 * stone and the beds into it. HTGR heat reaches another reactor type at 1/16 only: the beds warm the core past 400 C, where it starts
	 * reacting and burns its fuel into hot LiFBe (+50 C each time), which the pipe under the fuel dump draws off through it (the dump
	 * passes drains on to its core). Past 1100 C the fuel dump drains the core's fuel and lets it out below as thorium fuel, which
	 * replaces the pipe and spreads out under the floor.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 5000, batch = "factory")
	public static void pebbleBedsHeatThoriumCore(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 15, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 0);
		Factory.infiniteChest(h, chest, new ItemStack(ReactorBlocks.PEBBLE_BED.get()), new ItemStack(ReactorItems.PELLET.get()),
				new ItemStack(ReactorBlocks.THORIUM_CORE.get()), new ItemStack(ReactorBlocks.FUEL_DUMP.get()), new ItemStack(ReactorBlocks.FUEL_ROD.get()),
				new ItemStack(ReactorItems.FUEL.get()), new ItemStack(RotaryBlocks.BEDROCK_PIPE.get()), new ItemStack(RotaryBlocks.FUEL_LINE.get()),
				new ItemStack(Items.STONE), new ItemStack(Items.IRON_PICKAXE));
		BlockPos thorium = new BlockPos(5, 2, 4), dump = thorium.below(), drainPipe = dump.below();
		Factory.takeExactly(p, chest, Items.IRON_PICKAXE, 1);
		Factory.takeExactly(p, chest, ReactorBlocks.FUEL_DUMP.get().asItem(), 1);
		Factory.takeExactly(p, chest, RotaryBlocks.BEDROCK_PIPE.get().asItem(), 1);
		Factory.takeExactly(p, chest, ReactorBlocks.THORIUM_CORE.get().asItem(), 1);
		p.mine(dump, Items.IRON_PICKAXE);
		p.placeSneaking(dump, new ItemStack(ReactorBlocks.FUEL_DUMP.get()), Direction.SOUTH);
		p.placeSneaking(thorium, new ItemStack(ReactorBlocks.THORIUM_CORE.get()), Direction.SOUTH);
		p.placeSneaking(drainPipe, new ItemStack(RotaryBlocks.BEDROCK_PIPE.get()), Direction.SOUTH); //after the core: the dump connects to pipes only with a core on it
		PebbleBedBlockEntity mid = buildReactor(h, p, chest, List.of(thorium), List.of(thorium.above()));
		Factory.takeExactly(p, chest, RotaryBlocks.FUEL_LINE.get().asItem(), 1);
		p.placeSneaking(thorium.above(), new ItemStack(RotaryBlocks.FUEL_LINE.get()), Direction.SOUTH);
		Factory.fluidBarrel(h, thorium.above(2), ReactorFluids.get("lifbe_fuel"));

		List<BlockPos> rods = List.of(new BlockPos(X0-2, 2, thorium.getZ()), new BlockPos(X0-3, 2, thorium.getZ()), new BlockPos(X1+2, 2, thorium.getZ()),
				new BlockPos(X1+3, 2, thorium.getZ()), new BlockPos(X1+4, 2, thorium.getZ()), new BlockPos(thorium.getX(), 2, Z0-2), new BlockPos(thorium.getX(), 2, Z1+2));
		Factory.takeExactly(p, chest, ReactorBlocks.FUEL_ROD.get().asItem(), rods.size());
		Factory.takeExactly(p, chest, ReactorItems.FUEL.get(), 4*rods.size());
		for (BlockPos r : rods) {
			p.placeSneaking(r, new ItemStack(ReactorBlocks.FUEL_ROD.get()), Direction.SOUTH);
			p.open(r);
			for (int s = 0; s < 4; s++)
				p.putInto(s, ReactorItems.FUEL.get(), 1);
			p.closeMenu();
		}

		ThoriumCoreBlockEntity tc = (ThoriumCoreBlockEntity)h.getBlockEntity(thorium);
		FuelDumpBlockEntity fd = (FuelDumpBlockEntity)h.getBlockEntity(dump);
		h.runAtTickTime(100, () -> {
			h.assertTrue(tc.getFuelTank().getFluidAmount() == 4000, "the fuel line filled the core with only "+tc.getFuelTank().getFluidAmount()+" mB");
			p.mine(thorium.above(), Items.IRON_PICKAXE);
			p.placeSneaking(thorium.above(), new ItemStack(Items.STONE), Direction.SOUTH);
		});
		int[] peak = {0, 0, 0, 0}; //beds, thorium core, hot LiFBe in the drain pipe, fuel in the dump
		int[] t400 = {-1};
		h.onEachTick(() -> {
			peak[0] = Math.max(peak[0], mid.getTemperature());
			peak[1] = Math.max(peak[1], tc.getTemperature());
			//the pipe is gone once the dump spills onto it
			if (h.getLevel().getBlockEntity(h.absolutePos(drainPipe)) instanceof PipingBlockEntity pipe && pipe.getFluidType() != null && pipe.getFluidType().isSame(ReactorFluids.get("hot_lifbe")))
				peak[2] = Math.max(peak[2], pipe.getFluidLevel());
			peak[3] = Math.max(peak[3], fd.getTank().getFluidAmount());
			if (t400[0] < 0 && tc.getTemperature() >= ThoriumCoreBlockEntity.MIN_TEMPERATURE)
				t400[0] = (int)h.getTick();
		});
		h.succeedWhen(() -> {
			int spilled = 0;
			for (BlockPos q : BlockPos.betweenClosed(new BlockPos(0, 0, 0), new BlockPos(15, 0, 8)))
				if (h.getBlockState(q).is(ReactorBlocks.THORIUM_FUEL.get()))
					spilled++;
			String state = "beds peak "+peak[0]+" C (reactor of "+mid.getReactorSize()+"), thorium core peak "+peak[1]+" C (400 C at tick "+t400[0]+"), fuel "
					+tc.getFuelTank().getFluidAmount()+" mB, hot LiFBe "+tc.getOutputTank().getFluidAmount()+" mB, at most "+peak[2]+" mB of it in the drain pipe, dump held up to "
					+peak[3]+" mB, thorium fuel in "+spilled+" blocks";
			h.assertTrue(mid.getReactorSize() == 29, state);
			h.assertTrue(tc.isActive() && peak[1] >= ThoriumCoreBlockEntity.FUEL_DUMP_TEMPERATURE, "thorium core: "+state);
			h.assertTrue(peak[2] > 0, "no hot LiFBe drawn through the fuel dump: "+state);
			h.assertTrue(peak[3] > 0 && spilled > 0 && tc.getFuelTank().getFluidAmount() < 4000, "the hot core was not dumped: "+state);
			Factory.assertBuilt(h, List.of("reactorcraft:pebble_bed", "reactorcraft:thorium_core", "reactorcraft:fuel_dump", "reactorcraft:thorium_fuel"),
					new BlockPos(0, 0, 0), new BlockPos(15, 3, 8));
			p.leave();
		});
	}

	/**
	 * Fills the reactor layer with pebble beds (but for the blocks already set at {@code taken}), puts one fuel pebble into each through
	 * its GUI, and walls and roofs it in with stone (but for {@code roofHoles}). Returns a bed in the middle.
	 */
	private static PebbleBedBlockEntity buildReactor(GameTestHelper h, SimPlayer p, BlockPos chest, List<BlockPos> taken, List<BlockPos> roofHoles) {
		int n = (X1-X0+1)*(Z1-Z0+1)-taken.size();
		Factory.takeExactly(p, chest, ReactorBlocks.PEBBLE_BED.get().asItem(), n);
		Factory.takeExactly(p, chest, ReactorItems.PELLET.get(), n);
		List<BlockPos> beds = new ArrayList<>();
		for (int x = X0; x <= X1; x++)
			for (int z = Z0; z <= Z1; z++) {
				BlockPos b = new BlockPos(x, 2, z);
				if (taken.contains(b))
					continue;
				p.placeSneaking(b, new ItemStack(ReactorBlocks.PEBBLE_BED.get()), Direction.SOUTH);
				beds.add(b);
			}
		for (BlockPos b : beds) {
			p.open(b);
			p.putInto(0, ReactorItems.PELLET.get(), 1); //the bed slides it down to its last slot
			p.closeMenu();
		}
		List<BlockPos> shell = new ArrayList<>();
		for (int z = Z0; z <= Z1; z++) {
			shell.add(new BlockPos(X0-1, 2, z));
			shell.add(new BlockPos(X1+1, 2, z));
		}
		for (int x = X0; x <= X1; x++) {
			shell.add(new BlockPos(x, 2, Z0-1));
			shell.add(new BlockPos(x, 2, Z1+1));
		}
		for (int x = X0; x <= X1; x++)
			for (int z = Z0; z <= Z1; z++)
				if (!roofHoles.contains(new BlockPos(x, 3, z)))
					shell.add(new BlockPos(x, 3, z));
		Factory.takeExactly(p, chest, Items.STONE, 64);
		for (BlockPos s : shell)
			p.placeSneaking(s, new ItemStack(Items.STONE), Direction.SOUTH);
		return (PebbleBedBlockEntity)h.getBlockEntity(new BlockPos(4, 2, 4));
	}
}
