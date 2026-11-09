/*******************************************************************************
 * Practice factory zone (see Factory): the RotaryCraft machines a microturbine drives - heat ray, compactor, pulse jet furnace and fuel
 * enhancer - built and run by a simulated survival player. Not part of the original mods.
 ******************************************************************************/
package reika.reactorcraft.gametest.factory;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.reactorcraft.registry.ReactorFluids;
import reika.reactorcraft.registry.ReactorItems;
import reika.rotarycraft.content.block.GearboxBlock;
import reika.rotarycraft.content.blockentity.CompactorBlockEntity;
import reika.rotarycraft.content.blockentity.FuelEnhancerBlockEntity;
import reika.rotarycraft.content.blockentity.HeatRayBlockEntity;
import reika.rotarycraft.content.blockentity.PulseJetFurnaceBlockEntity;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.GearboxTypes;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneTurbines {

	public static final List<String> BLOCKS = List.of("rotarycraft:heat_ray", "rotarycraft:compactor", "rotarycraft:pulse_jet_furnace",
			"rotarycraft:fuel_enhancer");

	/**
	 * Four lines along x, each fed by a microturbine (131072 rad/s @ 16 Nm = 2 MW once spun up) that stands on a fuel line the player lays
	 * into a trench dug in the floor, under a jet fuel barrel (it gives only downwards). The player looks east, so every turbine drives
	 * west.
	 *  - z = 4: heat ray (2 MW, range 8): its beam turns the sand two blocks ahead into glass; an obsidian block behind stops the beam.
	 *  - z = 8: two bedrock 16:1 gearboxes (512 rad/s @ 4096 Nm) into a compactor with four ice put in through its GUI: the pressure rises
	 *    to 24000 kPa and it presses two packed ice, which the player takes out.
	 *  - z = 12: bevel gears turn the power up into a pulse jet furnace (it needs the turbine's full 131072 rad/s); jet fuel from a fuel line
	 *    at its west side, water (cooling: without it the furnace explodes at 1000 C) from a pipe at its south side; obsidian put in
	 *    through its GUI becomes blast glass at 850 C.
	 *  - z = 16: bevel gears into a fuel enhancer; LiFBe from a barrel through a pipe on its top, thorium dust through a hopper the player
	 *    sets against its west side: it makes LiFBe fuel.
	 */
	@GameTest(template = Factory.BIG, timeoutTicks = 5000, batch = "factory")
	public static void microturbineMachines(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 47, 47);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos a = new BlockPos(1, 2, 1), b = new BlockPos(2, 2, 1);
		ItemStack gbx = GearboxBlock.gearboxItem(GearboxTypes.BEDROCK, 16);
		Item thor = ReactorItems.raw(ReactorItems.Raw.THORDUST).get();
		Factory.infiniteChest(h, a, new ItemStack(Items.IRON_PICKAXE), new ItemStack(Items.IRON_SHOVEL), new ItemStack(RotaryBlocks.FUEL_LINE.get()),
				new ItemStack(RotaryBlocks.MICRO_ENGINE.get()), new ItemStack(RotaryBlocks.HEAT_RAY.get()), new ItemStack(Items.SAND),
				new ItemStack(Items.OBSIDIAN), gbx, new ItemStack(RotaryBlocks.COMPACTOR.get()), new ItemStack(Items.ICE),
				new ItemStack(RotaryBlocks.BEVEL_GEARS.get()), new ItemStack(RotaryBlocks.PULSE_JET.get()), new ItemStack(RotaryBlocks.PIPE.get()),
				new ItemStack(Items.DIRT));
		Factory.infiniteChest(h, b, new ItemStack(RotaryBlocks.FUEL_ENHANCER.get()), new ItemStack(Items.HOPPER), new ItemStack(thor));
		Factory.take(p, a, Items.IRON_PICKAXE, 1);
		Factory.take(p, a, Items.IRON_SHOVEL, 1);
		Factory.take(p, a, RotaryBlocks.FUEL_LINE.get().asItem(), 9);
		Factory.take(p, a, RotaryBlocks.MICRO_ENGINE.get().asItem(), 4);
		Factory.take(p, a, Items.DIRT, 4);

		// heat ray, z = 4
		turbine(h, p, 4);
		Factory.take(p, a, RotaryBlocks.HEAT_RAY.get().asItem(), 1);
		Factory.take(p, a, Items.SAND, 1);
		Factory.take(p, a, Items.OBSIDIAN, 1);
		BlockPos ray = new BlockPos(8, 2, 4), sand = new BlockPos(6, 2, 4);
		p.placeSneaking(ray, new ItemStack(RotaryBlocks.HEAT_RAY.get()), Direction.EAST);
		p.place(sand, Items.SAND, Direction.EAST);
		p.place(sand.west(), Items.OBSIDIAN, Direction.EAST);

		// compactor, z = 8
		turbine(h, p, 8);
		Factory.take(p, a, gbx, 2);
		Factory.take(p, a, RotaryBlocks.COMPACTOR.get().asItem(), 1);
		Factory.take(p, a, Items.ICE, 4);
		BlockPos comp = new BlockPos(6, 2, 8);
		p.placeSneaking(new BlockPos(8, 2, 8), gbx, Direction.EAST);
		p.placeSneaking(new BlockPos(7, 2, 8), gbx, Direction.EAST);
		p.placeSneaking(comp, new ItemStack(RotaryBlocks.COMPACTOR.get()), Direction.EAST);
		p.open(comp);
		for (int i = 0; i < 4; i++)
			p.putInto(i, Items.ICE, 1);
		p.closeMenu();

		// pulse jet furnace, z = 12
		turbine(h, p, 12);
		Factory.take(p, a, RotaryBlocks.BEVEL_GEARS.get().asItem(), 2);
		Factory.take(p, a, RotaryBlocks.PULSE_JET.get().asItem(), 1);
		Factory.take(p, a, RotaryBlocks.PIPE.get().asItem(), 2);
		Factory.take(p, a, Items.OBSIDIAN, 2);
		BlockPos pj = new BlockPos(8, 3, 12);
		onBevelGears(p, pj, RotaryBlocks.PULSE_JET.get().asItem());
		Factory.fluidBarrel(h, pj.west().above(), RotaryFluids.JET_FUEL.get());
		p.placeSneaking(pj.west(), new ItemStack(RotaryBlocks.FUEL_LINE.get()), Direction.EAST);
		Factory.fluidBarrel(h, pj.south().above(), Fluids.WATER);
		p.placeSneaking(pj.south(), new ItemStack(RotaryBlocks.PIPE.get()), Direction.EAST);
		p.open(pj);
		p.putInto(0, Items.OBSIDIAN, 2);
		p.closeMenu();

		// fuel enhancer, z = 16
		turbine(h, p, 16);
		Factory.take(p, b, RotaryBlocks.FUEL_ENHANCER.get().asItem(), 1);
		Factory.take(p, b, Items.HOPPER, 1);
		Factory.take(p, b, thor, 16);
		BlockPos fe = new BlockPos(8, 3, 16);
		onBevelGears(p, fe, RotaryBlocks.FUEL_ENHANCER.get().asItem());
		Factory.fluidBarrel(h, fe.above(2), ReactorFluids.get("lifbe"));
		p.placeSneaking(fe.above(), new ItemStack(RotaryBlocks.PIPE.get()), Direction.EAST);
		p.hold(Items.HOPPER); //sneak-right-click on the enhancer's west face: the hopper sits there, facing into it
		p.player.setShiftKeyDown(true);
		try {
			p.useOn(fe, Direction.WEST);
		}
		finally {
			p.player.setShiftKeyDown(false);
		}
		h.assertBlockPresent(Blocks.HOPPER, fe.west());
		p.open(fe.west());
		p.putInto(0, thor, 16);
		p.closeMenu();
		p.moveNear(new BlockPos(1, 2, 20)); //out of the way of the machines

		HeatRayBlockEntity hr = (HeatRayBlockEntity)h.getBlockEntity(ray);
		CompactorBlockEntity cp = (CompactorBlockEntity)h.getBlockEntity(comp);
		PulseJetFurnaceBlockEntity pf = (PulseJetFurnaceBlockEntity)h.getBlockEntity(pj);
		FuelEnhancerBlockEntity en = (FuelEnhancerBlockEntity)h.getBlockEntity(fe);
		boolean[] taken = {false};
		h.succeedWhen(() -> {
			h.assertTrue(hr.getPower() >= 2097152, "heat ray at "+hr.getPower()+" W");
			h.assertBlockPresent(Blocks.GLASS, sand);
			h.assertBlockPresent(Blocks.OBSIDIAN, sand.west());
			h.assertTrue(pf.getInventory().getStackInSlot(2).is(RotaryBlocks.BLAST_GLASS.get().asItem()),
					"pulse jet: output "+pf.getInventory().getStackInSlot(2)+" at "+pf.getTemperature()+" C, "+pf.omega+" rad/s, fuel "+pf.getFuel()+", water "+pf.getWater());
			h.assertTrue(pf.getTemperature() >= 850 && pf.getTemperature() < 1000, "pulse jet at "+pf.getTemperature()+" C");
			h.assertTrue(en.getOutputTank().getFluid().getFluid().isSame(ReactorFluids.get("lifbe_fuel")) && en.getOutputTank().getFluidAmount() > 0,
					"fuel enhancer: "+en.getOmega()+" rad/s, "+en.getPower()+" W, in "+en.getInputTank().getFluidAmount()+" mB, items "+en.getInventory().getStackInSlot(0));
			if (!taken[0]) {
				ItemStack out = cp.getInventory().getStackInSlot(4);
				h.assertTrue(out.is(Items.PACKED_ICE) && out.getCount() == 2, "compactor: "+cp.omega+" rad/s @ "+cp.torque+" Nm, "+cp.getPressure()+" kPa, output "+out);
				p.open(comp);
				p.shiftClick(4);
				p.closeMenu();
				taken[0] = true;
			}
			h.assertTrue(p.count(Items.PACKED_ICE) == 2, "player holds "+p.count(Items.PACKED_ICE)+" packed ice");
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(20, 4, 20));
			p.leave();
		});
	}

	/**
	 * A microturbine at (9, 2, z): the player digs two floor blocks out, lays fuel lines into the trench, a jet fuel barrel stands over the
	 * east one and the turbine (looking east: it drives west) over the west one.
	 */
	private static void turbine(GameTestHelper h, SimPlayer p, int z) {
		for (int x = 10; x >= 9; x--) {
			BlockPos t = new BlockPos(x, 1, z);
			p.mine(t, Items.IRON_PICKAXE);
			p.place(t, RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		}
		Factory.fluidBarrel(h, new BlockPos(10, 2, z), RotaryFluids.JET_FUEL.get());
		p.placeSneaking(new BlockPos(9, 2, z), new ItemStack(RotaryBlocks.MICRO_ENGINE.get()), Direction.EAST);
	}

	/**
	 * The machine at {@code m} (reading its power from below) on bevel gears west of the turbine: set on a dirt block, which is dug away
	 * again and replaced by the bevel gears - placed last, so they find their route east -> up.
	 */
	private static void onBevelGears(SimPlayer p, BlockPos m, Item machine) {
		p.place(m.below(), Items.DIRT, Direction.EAST);
		p.place(m, machine, Direction.EAST);
		p.mine(m.below(), Items.IRON_SHOVEL);
		p.placeSneaking(m.below(), new ItemStack(RotaryBlocks.BEVEL_GEARS.get()), Direction.EAST);
	}
}
