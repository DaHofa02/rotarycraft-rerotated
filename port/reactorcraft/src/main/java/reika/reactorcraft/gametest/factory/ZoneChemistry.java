/*******************************************************************************
 * Practice factory zone (see Factory): ReactorCraft chemistry - electrolyzer, synthesizer, isotope centrifuge and fluid extractor - built
 * and run by a simulated survival player. Not part of the original mods.
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
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.reactorcraft.content.blockentity.ElectrolyzerBlockEntity;
import reika.reactorcraft.content.blockentity.HeavyPumpBlockEntity;
import reika.reactorcraft.content.blockentity.IsoCentrifugeBlockEntity;
import reika.reactorcraft.content.blockentity.SynthesizerBlockEntity;
import reika.reactorcraft.content.blockentity.UProcessorBlockEntity;
import reika.reactorcraft.registry.FluoriteTypes;
import reika.reactorcraft.registry.ReactorBlocks;
import reika.reactorcraft.registry.ReactorFluids;
import reika.reactorcraft.registry.ReactorItems;
import reika.rotarycraft.content.block.GearboxBlock;
import reika.rotarycraft.content.blockentity.FrictionHeaterBlockEntity;
import reika.rotarycraft.content.blockentity.GearboxBlockEntity;
import reika.rotarycraft.content.blockentity.VanDeGraffBlockEntity;
import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.content.piping.PipingBlockEntity;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.GearboxTypes;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;
import reika.rotarycraft.registry.RotaryItems;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneChemistry {

	public static final List<String> BLOCKS = List.of("reactorcraft:electrolyzer", "reactorcraft:synthesizer", "reactorcraft:isocentrifuge",
			"reactorcraft:fluid_extractor");

	private static final String PLATFORM = "platform_9x8x9";

	/**
	 * Two electrolyzers on either side of a Van de Graaff generator (it sparks into them, 1000 weight each). The generator gets 65536 W
	 * from below: gas engine on a fuel line in a floor trench -> bevel gears turning the power up; it sparks every 4 ticks with 4096 C.
	 *  - north: heavy water from a barrel through two pipes into its west side; it makes deuterium (drawn off upwards by a pipe on its top)
	 *    and oxygen.
	 *  - south: salt put in through its GUI; a friction heater on its south side, driven by a microturbine through a bedrock 4:1 gearbox
	 *    (32768 rad/s @ 64 Nm: the heater settles at 1110 C), heats it past salt's 801 C melting point: chlorine (pipe on top) and sodium.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 3000, batch = "factory")
	public static void electrolyzersOnVanDeGraaff(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos a = new BlockPos(0, 2, 0), b = new BlockPos(1, 2, 0);
		ItemStack gbx = GearboxBlock.gearboxItem(GearboxTypes.BEDROCK, 4);
		Item salt = RotaryItems.part(CraftingPart.SALT);
		Factory.infiniteChest(h, a, new ItemStack(Items.IRON_PICKAXE), new ItemStack(Items.IRON_SHOVEL), new ItemStack(Items.DIRT),
				new ItemStack(RotaryBlocks.FUEL_LINE.get()), new ItemStack(RotaryBlocks.GAS_ENGINE.get()), new ItemStack(RotaryBlocks.BEVEL_GEARS.get()),
				new ItemStack(RotaryBlocks.VAN_DE_GRAFF.get()), new ItemStack(RotaryBlocks.PIPE.get()));
		Factory.infiniteChest(h, b, new ItemStack(ReactorBlocks.ELECTROLYZER.get()), new ItemStack(RotaryBlocks.MICRO_ENGINE.get()), gbx,
				new ItemStack(RotaryBlocks.FRICTION_HEATER.get()), new ItemStack(salt));
		Factory.take(p, a, Items.IRON_PICKAXE, 1);
		Factory.take(p, a, Items.IRON_SHOVEL, 1);
		Factory.take(p, a, Items.DIRT, 7);
		Factory.take(p, a, RotaryBlocks.FUEL_LINE.get().asItem(), 4);
		Factory.take(p, a, RotaryBlocks.GAS_ENGINE.get().asItem(), 1);
		Factory.take(p, a, RotaryBlocks.BEVEL_GEARS.get().asItem(), 1);
		Factory.take(p, a, RotaryBlocks.VAN_DE_GRAFF.get().asItem(), 1);
		Factory.take(p, a, RotaryBlocks.PIPE.get().asItem(), 4);
		Factory.take(p, b, ReactorBlocks.ELECTROLYZER.get().asItem(), 2);
		Factory.take(p, b, RotaryBlocks.MICRO_ENGINE.get().asItem(), 1);
		Factory.take(p, b, gbx, 1);
		Factory.take(p, b, RotaryBlocks.FRICTION_HEATER.get().asItem(), 1);
		Factory.take(p, b, salt, 4);

		// the generator (4, 3, 4) on bevel gears, the gas engine east of them on a fuel line laid into a trench, the ethanol barrel over it
		for (int x = 6; x >= 5; x--) {
			BlockPos t = new BlockPos(x, 1, 4);
			p.mine(t, Items.IRON_PICKAXE);
			p.place(t, RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		}
		Factory.fluidBarrel(h, new BlockPos(6, 2, 4), RotaryFluids.ETHANOL.get());
		p.placeSneaking(new BlockPos(5, 2, 4), new ItemStack(RotaryBlocks.GAS_ENGINE.get()), Direction.EAST);
		BlockPos vdgPos = new BlockPos(4, 3, 4);
		p.place(vdgPos.below(), Items.DIRT, Direction.EAST);
		p.place(vdgPos, RotaryBlocks.VAN_DE_GRAFF.get(), Direction.EAST);

		// the electrolyzers north and south of it, each on a dirt block
		BlockPos heavy = new BlockPos(4, 3, 3), saltEl = new BlockPos(4, 3, 5);
		for (BlockPos e : new BlockPos[] {heavy, saltEl}) {
			p.place(e.below(), Items.DIRT, Direction.EAST);
			p.placeSneaking(e, new ItemStack(ReactorBlocks.ELECTROLYZER.get()), Direction.EAST);
		}

		// heavy water: barrel (2, 4, 3) over a pipe, a second pipe into the electrolyzer's west side; a pipe on top takes the deuterium
		Factory.fluidBarrel(h, new BlockPos(2, 4, 3), ReactorFluids.get("heavy_water"));
		p.placeSneaking(new BlockPos(3, 3, 3), new ItemStack(RotaryBlocks.PIPE.get()), Direction.EAST);
		p.placeSneaking(new BlockPos(2, 3, 3), new ItemStack(RotaryBlocks.PIPE.get()), Direction.EAST);
		BlockPos dOut = heavy.above(), clOut = saltEl.above();
		p.placeSneaking(dOut, new ItemStack(RotaryBlocks.PIPE.get()), Direction.EAST);
		p.placeSneaking(clOut, new ItemStack(RotaryBlocks.PIPE.get()), Direction.EAST);

		// salt through the GUI; looking south, the microturbine (4, 3, 8) drives north through the gearbox into the friction heater
		p.open(saltEl);
		p.putInto(0, salt, 4);
		p.closeMenu();
		BlockPos heater = new BlockPos(4, 3, 6);
		p.place(heater.below(), Items.DIRT, Direction.SOUTH);
		p.place(new BlockPos(4, 2, 7), Items.DIRT, Direction.SOUTH);
		p.place(new BlockPos(4, 2, 8), RotaryBlocks.FUEL_LINE.get(), Direction.SOUTH);
		p.place(new BlockPos(3, 2, 8), RotaryBlocks.FUEL_LINE.get(), Direction.SOUTH);
		Factory.fluidBarrel(h, new BlockPos(3, 3, 8), RotaryFluids.JET_FUEL.get());
		p.placeSneaking(new BlockPos(4, 3, 8), new ItemStack(RotaryBlocks.MICRO_ENGINE.get()), Direction.SOUTH);
		p.placeSneaking(new BlockPos(4, 3, 7), gbx, Direction.SOUTH);
		p.placeSneaking(heater, new ItemStack(RotaryBlocks.FRICTION_HEATER.get()), Direction.SOUTH);

		// last the bevel gears under the generator (they find their route east -> up), then out of its reach
		p.mine(vdgPos.below(), Items.IRON_SHOVEL);
		p.placeSneaking(vdgPos.below(), new ItemStack(RotaryBlocks.BEVEL_GEARS.get()), Direction.EAST);
		p.moveNear(new BlockPos(0, 2, 30));

		VanDeGraffBlockEntity vdg = (VanDeGraffBlockEntity)h.getBlockEntity(vdgPos);
		ElectrolyzerBlockEntity eh = (ElectrolyzerBlockEntity)h.getBlockEntity(heavy);
		ElectrolyzerBlockEntity es = (ElectrolyzerBlockEntity)h.getBlockEntity(saltEl);
		FrictionHeaterBlockEntity fh = (FrictionHeaterBlockEntity)h.getBlockEntity(heater);
		PipingBlockEntity dPipe = (PipingBlockEntity)h.getBlockEntity(dOut);
		PipingBlockEntity clPipe = (PipingBlockEntity)h.getBlockEntity(clOut);
		h.succeedWhen(() -> {
			h.assertTrue(vdg.getPower() == 65536, "Van de Graaff at "+vdg.getPower()+" W, "+vdg.getCharge()+" C");
			int oxygen = eh.getHeavyTank().getFluidAmount();
			h.assertTrue(oxygen >= 100 && eh.getHeavyTank().getFluid().getFluid().isSame(ReactorFluids.get("oxygen")),
					"heavy water electrolyzer: oxygen "+oxygen+" mB, input "+eh.getInputTank().getFluidAmount()+" mB, timer "+eh.getTimer()+", at "+eh.getTemperature()+" C");
			int deut = eh.getLightTank().getFluidAmount()+(dPipe.getFluidType() == null ? 0 : dPipe.getFluidLevel());
			h.assertTrue(dPipe.getFluidType() != null && dPipe.getFluidType().isSame(ReactorFluids.get("deuterium")) && deut >= 2*oxygen-10,
					"deuterium: pipe "+dPipe.getFluidType()+" "+dPipe.getFluidLevel()+" mB, tank "+eh.getLightTank().getFluidAmount()+" mB");
			h.assertTrue(es.getTemperature() >= ElectrolyzerBlockEntity.SALT_MELT, "salt electrolyzer at "+es.getTemperature()+" C, heater "+fh.getTemperature()
					+" C ("+fh.omega+" rad/s @ "+fh.torque+" Nm)");
			int sodium = es.getHeavyTank().getFluidAmount();
			h.assertTrue(sodium >= 200 && es.getHeavyTank().getFluid().getFluid().isSame(ReactorFluids.get("sodium")),
					"salt electrolyzer: sodium "+sodium+" mB, salt "+es.getInventory().getStackInSlot(0)+", timer "+es.getTimer()+", at "+es.getTemperature()+" C");
			h.assertTrue(es.getInventory().getStackInSlot(0).getCount() == 4-sodium/100, "salt left "+es.getInventory().getStackInSlot(0)+" for "+sodium+" mB sodium");
			h.assertTrue(clPipe.getFluidType() != null && clPipe.getFluidType().isSame(ReactorFluids.get("chlorine")), "chlorine pipe: "+clPipe.getFluidType());
			Factory.assertBuilt(h, List.of("reactorcraft:electrolyzer"), new BlockPos(0, 2, 0), new BlockPos(8, 4, 8));
			p.leave();
		});
	}

	/**
	 * Synthesizer heated by a friction heater (gas engine on an ethanol fuel line -> friction heater, which settles at 786 C), water from a
	 * barrel through two pipes into its west side, quicklime and ammonium chloride through its GUI: from 220 C it makes 1000 mB ammonia
	 * per 250 mB water and one of each item; a pipe on its top draws the ammonia off.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 1500, batch = "factory")
	public static void synthesizerMakesAmmonia(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos a = new BlockPos(0, 2, 8);
		Item lime = ReactorItems.raw(ReactorItems.Raw.LIME).get(), ammonium = ReactorItems.raw(ReactorItems.Raw.AMMONIUM).get();
		Factory.infiniteChest(h, a, new ItemStack(RotaryBlocks.FUEL_LINE.get()), new ItemStack(RotaryBlocks.GAS_ENGINE.get()),
				new ItemStack(RotaryBlocks.FRICTION_HEATER.get()), new ItemStack(ReactorBlocks.SYNTHESIZER.get()), new ItemStack(RotaryBlocks.PIPE.get()),
				new ItemStack(Items.DIRT), new ItemStack(lime), new ItemStack(ammonium));
		Factory.take(p, a, RotaryBlocks.FUEL_LINE.get().asItem(), 3);
		Factory.take(p, a, RotaryBlocks.GAS_ENGINE.get().asItem(), 1);
		Factory.take(p, a, RotaryBlocks.FRICTION_HEATER.get().asItem(), 1);
		Factory.take(p, a, ReactorBlocks.SYNTHESIZER.get().asItem(), 1);
		Factory.take(p, a, RotaryBlocks.PIPE.get().asItem(), 3);
		Factory.take(p, a, Items.DIRT, 2);
		Factory.take(p, a, lime, 4);
		Factory.take(p, a, ammonium, 4);

		// looking east: ethanol barrel (8, 3, 2) over the fuel line, gas engine (6, 3, 2), friction heater (5, 3, 2), synthesizer (4, 3, 2)
		Factory.fluidBarrel(h, new BlockPos(8, 3, 2), RotaryFluids.ETHANOL.get());
		for (int x = 8; x >= 6; x--)
			p.place(new BlockPos(x, 2, 2), RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		BlockPos heater = new BlockPos(5, 3, 2), syn = new BlockPos(4, 3, 2);
		p.placeSneaking(new BlockPos(6, 3, 2), new ItemStack(RotaryBlocks.GAS_ENGINE.get()), Direction.EAST);
		p.place(heater.below(), Items.DIRT, Direction.EAST);
		p.placeSneaking(heater, new ItemStack(RotaryBlocks.FRICTION_HEATER.get()), Direction.EAST);
		p.place(syn.below(), Items.DIRT, Direction.EAST);
		p.placeSneaking(syn, new ItemStack(ReactorBlocks.SYNTHESIZER.get()), Direction.EAST);

		// water: barrel (2, 4, 2) over a pipe, a second pipe into the west side; a pipe on top for the ammonia
		Factory.fluidBarrel(h, new BlockPos(2, 4, 2), Fluids.WATER);
		p.placeSneaking(new BlockPos(3, 3, 2), new ItemStack(RotaryBlocks.PIPE.get()), Direction.EAST);
		p.placeSneaking(new BlockPos(2, 3, 2), new ItemStack(RotaryBlocks.PIPE.get()), Direction.EAST);
		BlockPos out = syn.above();
		p.placeSneaking(out, new ItemStack(RotaryBlocks.PIPE.get()), Direction.EAST);
		p.open(syn);
		p.putInto(1, lime, 4);
		p.putInto(2, ammonium, 4);
		p.closeMenu();
		p.moveNear(new BlockPos(0, 2, 6));

		SynthesizerBlockEntity te = (SynthesizerBlockEntity)h.getBlockEntity(syn);
		FrictionHeaterBlockEntity fh = (FrictionHeaterBlockEntity)h.getBlockEntity(heater);
		PipingBlockEntity pipe = (PipingBlockEntity)h.getBlockEntity(out);
		h.succeedWhen(() -> {
			h.assertTrue(te.getTemperature() >= SynthesizerBlockEntity.AMMONIATEMP, "synthesizer at "+te.getTemperature()+" C, heater "+fh.getTemperature()+" C");
			int used = 4-te.getInventory().getStackInSlot(1).getCount();
			h.assertTrue(used >= 2 && te.getInventory().getStackInSlot(2).getCount() == 4-used, "used "+used+" quicklime, ammonium left "+te.getInventory().getStackInSlot(2)
					+", water "+te.getInputTank().getFluidAmount()+" mB, ammonia "+te.getOutputTank().getFluidAmount()+" mB");
			h.assertTrue(pipe.getFluidType() != null && pipe.getFluidType().isSame(ReactorFluids.get("ammonia")) && pipe.getFluidLevel() > 0,
					"ammonia pipe: "+pipe.getFluidType()+" "+pipe.getFluidLevel()+" mB; tank "+te.getOutputTank().getFluidAmount()+" mB");
			Factory.assertBuilt(h, List.of("reactorcraft:synthesizer"), new BlockPos(0, 2, 0), new BlockPos(8, 4, 8));
			p.leave();
		});
	}

	/**
	 * Isotope centrifuge fed by a uranium processor: looking east, a microturbine (7, 3, 4) on a jet fuel line drives a bedrock 2:1
	 * gearbox switched to speed-up with the screwdriver (262144 rad/s @ 8 Nm, the centrifuge's minimum), bevel gears turn it up into the
	 * centrifuge (5, 4, 4). On a dirt block over the gearbox stands the processor (water by pipe from a barrel on its north side;
	 * fluorite, a water bucket and uranium ingots through its GUI); its front (west) gives the uranium hexafluoride into a pipe on the
	 * centrifuge's top. Every 900 ticks 50 mB UF6 become a dust (enriched or depleted uranium), which the player takes out of the GUI.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 4000, batch = "factory")
	public static void processorFeedsIsoCentrifuge(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos a = new BlockPos(0, 2, 8), b = new BlockPos(1, 2, 8);
		ItemStack gbx = GearboxBlock.gearboxItem(GearboxTypes.BEDROCK, 2);
		ItemStack fluorite = new ItemStack(ReactorItems.fluorite(FluoriteTypes.GREEN).get());
		ItemStack uranium = new ItemStack(ReactorItems.ingot(ReactorItems.Ingot.URANIUM).get());
		Factory.infiniteChest(h, a, new ItemStack(RotaryBlocks.FUEL_LINE.get()), new ItemStack(RotaryBlocks.MICRO_ENGINE.get()), gbx,
				new ItemStack(RotaryItems.SCREWDRIVER.get()), new ItemStack(Items.DIRT), new ItemStack(Items.IRON_SHOVEL),
				new ItemStack(RotaryBlocks.BEVEL_GEARS.get()), new ItemStack(ReactorBlocks.ISO_CENTRIFUGE.get()), new ItemStack(RotaryBlocks.PIPE.get()));
		Factory.infiniteChest(h, b, new ItemStack(ReactorBlocks.PROCESSOR.get()), fluorite, uranium, new ItemStack(Items.BUCKET));
		Factory.take(p, a, RotaryBlocks.FUEL_LINE.get().asItem(), 2);
		Factory.take(p, a, RotaryBlocks.MICRO_ENGINE.get().asItem(), 1);
		Factory.take(p, a, gbx, 1);
		Factory.take(p, a, RotaryItems.SCREWDRIVER.get(), 1);
		Factory.take(p, a, Items.DIRT, 3);
		Factory.take(p, a, Items.IRON_SHOVEL, 1);
		Factory.take(p, a, RotaryBlocks.BEVEL_GEARS.get().asItem(), 1);
		Factory.take(p, a, ReactorBlocks.ISO_CENTRIFUGE.get().asItem(), 1);
		Factory.take(p, a, RotaryBlocks.PIPE.get().asItem(), 2);
		Factory.take(p, b, ReactorBlocks.PROCESSOR.get().asItem(), 1);
		Factory.take(p, b, fluorite, 4);
		Factory.take(p, b, uranium, 2);
		Factory.takeExactly(p, b, Items.BUCKET, 1);

		// power line along z = 4
		Factory.fluidBarrel(h, new BlockPos(8, 3, 4), RotaryFluids.JET_FUEL.get());
		p.place(new BlockPos(8, 2, 4), RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		p.place(new BlockPos(7, 2, 4), RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		p.placeSneaking(new BlockPos(7, 3, 4), new ItemStack(RotaryBlocks.MICRO_ENGINE.get()), Direction.EAST);
		BlockPos g = new BlockPos(6, 3, 4), cen = new BlockPos(5, 4, 4);
		p.placeSneaking(g, gbx, Direction.EAST);
		p.hold(RotaryItems.SCREWDRIVER.get());
		p.player.setShiftKeyDown(true); //sneak-right-click with the screwdriver: reduction <-> speed-up
		try {
			p.useOn(g, Direction.UP);
		}
		finally {
			p.player.setShiftKeyDown(false);
		}
		h.assertTrue(!((GearboxBlockEntity)h.getBlockEntity(g)).reduction, "gearbox not switched to speed-up");
		p.place(new BlockPos(5, 2, 4), Items.DIRT, Direction.EAST);
		p.place(new BlockPos(5, 3, 4), Items.DIRT, Direction.EAST);
		p.place(cen, ReactorBlocks.ISO_CENTRIFUGE.get(), Direction.EAST);

		// the processor (6, 5, 4) on a dirt block over the gearbox, its front (west) at the pipe on the centrifuge's top
		BlockPos proc = new BlockPos(6, 5, 4);
		p.placeSneaking(proc.below(), new ItemStack(Items.DIRT), Direction.EAST);
		p.place(proc, ReactorBlocks.PROCESSOR.get(), Direction.EAST);
		p.placeSneaking(cen.above(), new ItemStack(RotaryBlocks.PIPE.get()), Direction.EAST);
		BlockPos water = new BlockPos(6, 6, 3);
		Factory.fluidBarrel(h, water, Fluids.WATER);
		p.placeSneaking(proc.north(), new ItemStack(RotaryBlocks.PIPE.get()), Direction.EAST);
		p.use(water, Items.BUCKET);
		p.open(proc);
		p.putInto(0, fluorite.getItem(), 4);
		p.putInto(1, Items.WATER_BUCKET, 1);
		p.putInto(2, uranium.getItem(), 2);
		p.closeMenu();

		// last the bevel gears under the centrifuge (east -> up)
		p.mine(new BlockPos(5, 3, 4), Items.IRON_SHOVEL);
		p.placeSneaking(new BlockPos(5, 3, 4), new ItemStack(RotaryBlocks.BEVEL_GEARS.get()), Direction.EAST);
		p.moveNear(new BlockPos(0, 2, 0));

		IsoCentrifugeBlockEntity te = (IsoCentrifugeBlockEntity)h.getBlockEntity(cen);
		UProcessorBlockEntity up = (UProcessorBlockEntity)h.getBlockEntity(proc);
		Item fuel = ReactorItems.raw(ReactorItems.Raw.FUELDUST).get(), dep = ReactorItems.raw(ReactorItems.Raw.DEPDUST).get();
		int[] taken = {-1};
		h.succeedWhen(() -> {
			if (taken[0] < 0) {
				h.assertTrue(te.getOmega() == 262144 && te.getTorque() == 8, "centrifuge input "+te.getOmega()+" rad/s @ "+te.getTorque()+" Nm");
				int dust = te.getInventory().getStackInSlot(0).getCount()+te.getInventory().getStackInSlot(1).getCount();
				h.assertTrue(dust >= 1, "no dust yet: centrifuge UF6 "+te.getTank().getFluidAmount()+" mB, timer "+te.split+"; processor UF6 "
						+up.getOutputTank().getFluidAmount()+" mB, acid "+up.getIntermediateTank().getFluidAmount()+", water "+up.getInputTank().getFluidAmount());
				p.open(cen);
				for (int i = 0; i < 2; i++)
					if (!p.menu().getSlot(i).getItem().isEmpty())
						p.shiftClick(i);
				p.closeMenu();
				p.moveNear(new BlockPos(0, 2, 0));
				taken[0] = dust;
			}
			h.assertTrue(p.count(fuel)+p.count(dep) == taken[0], "player took "+p.count(fuel)+" enriched and "+p.count(dep)+" depleted dust of "+taken[0]);
			Factory.assertBuilt(h, List.of("reactorcraft:isocentrifuge"), new BlockPos(0, 2, 0), new BlockPos(8, 6, 8));
			p.leave();
		});
	}

	/**
	 * Fluid extractor on the overworld lava surface (y -54, here about seven blocks over the floor): the player builds a stone block up to
	 * just below that level with a hole in its top layer and pours lava (buckets filled at a lava barrel) into the hole; sets the extractor
	 * over it and pours lava on three of its sides. A gas engine on an ethanol fuel line drives through a bedrock 4:1 gearbox (128 rad/s @
	 * 512 Nm = 65536 W, the minimum) and bevel gears turning the power down into it. It collects molten lithium, which the player fills into
	 * an empty canister. (Heavy water needs an ocean biome: the test world is plains.)
	 */
	@GameTest(template = Factory.BIG, timeoutTicks = 1500, batch = "factory")
	public static void extractorCollectsLithium(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 47, 47);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		int s = -54-h.absolutePos(BlockPos.ZERO).getY();
		h.assertTrue(s >= 4 && s <= 9, "lava surface at relative y "+s);
		int X = 10, Z = 10;
		BlockPos chest = new BlockPos(40, 2, 40), lavaBarrel = new BlockPos(20, 2, 10);
		ItemStack gbx = GearboxBlock.gearboxItem(GearboxTypes.BEDROCK, 4);
		Factory.infiniteChest(h, chest, new ItemStack(Items.STONE), new ItemStack(Items.BUCKET), new ItemStack(ReactorBlocks.FLUID_EXTRACTOR.get()),
				new ItemStack(RotaryBlocks.FUEL_LINE.get()), new ItemStack(RotaryBlocks.GAS_ENGINE.get()), gbx, new ItemStack(RotaryBlocks.BEVEL_GEARS.get()),
				new ItemStack(ReactorItems.EMPTY_CANISTER.get()));
		Factory.fluidBarrel(h, lavaBarrel, Fluids.LAVA);
		Factory.take(p, chest, Items.STONE, (s-2)*25-1+(s-2)+20); //the block below the lava surface, its column and its top layer
		Factory.takeExactly(p, chest, Items.BUCKET, 1);
		Factory.take(p, chest, ReactorBlocks.FLUID_EXTRACTOR.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.FUEL_LINE.get().asItem(), 2);
		Factory.take(p, chest, RotaryBlocks.GAS_ENGINE.get().asItem(), 1);
		Factory.take(p, chest, gbx, 1);
		Factory.take(p, chest, RotaryBlocks.BEVEL_GEARS.get().asItem(), 1);
		Factory.takeExactly(p, chest, ReactorItems.EMPTY_CANISTER.get(), 1);

		// the stone block 5 x 5 from the floor up to the lava surface; one hole under the extractor, its top layer open on three sides
		BlockPos ext = new BlockPos(X, s, Z);
		List<BlockPos> lava = List.of(ext.north(), ext.south(), ext.west());
		for (int y = 2; y <= s; y++) {
			for (int x = X-2; x <= X+2; x++)
				for (int z = Z-2; z <= Z+2; z++) {
					BlockPos q = new BlockPos(x, y, z);
					if (q.equals(ext.below()) || (y == s && (q.equals(ext) || lava.contains(q) || q.equals(new BlockPos(X+2, s, Z)))))
						continue;
					p.place(q, Items.STONE, Direction.EAST);
				}
			if (y < s)
				p.place(new BlockPos(X+3, y, Z), Items.STONE, Direction.EAST); //the column under the outer fuel line
		}

		// lava into the hole, the extractor over it (set against the stone east of it), lava on its other three sides
		pourLava(h, p, lavaBarrel, ext);
		p.hold(ReactorBlocks.FLUID_EXTRACTOR.get().asItem());
		p.look(Direction.EAST);
		p.useOn(ext.east(), Direction.WEST);
		h.assertBlockPresent(ReactorBlocks.FLUID_EXTRACTOR.get(), ext);
		for (BlockPos l : lava)
			pourLava(h, p, lavaBarrel, l.above());
		h.assertTrue(h.getBlockState(ext.below()).getFluidState().isSource(), "no lava under the extractor: "+h.getBlockState(ext.below()));
		for (BlockPos l : lava)
			h.assertTrue(h.getBlockState(l).is(Blocks.LAVA) && h.getBlockState(l).getFluidState().isSource(), "no lava source at "+l+": "+h.getBlockState(l));

		// power from above: looking east, gas engine (X+2, s+1) on the fuel line, gearbox, bevel gears over the extractor (placed last)
		p.place(new BlockPos(X+3, s, Z), RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		p.place(new BlockPos(X+2, s, Z), RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		Factory.fluidBarrel(h, new BlockPos(X+3, s+1, Z), RotaryFluids.ETHANOL.get());
		p.placeSneaking(new BlockPos(X+2, s+1, Z), new ItemStack(RotaryBlocks.GAS_ENGINE.get()), Direction.EAST);
		p.placeSneaking(new BlockPos(X+1, s+1, Z), gbx, Direction.EAST);
		p.placeSneaking(ext.above(), new ItemStack(RotaryBlocks.BEVEL_GEARS.get()), Direction.EAST);
		p.moveNear(new BlockPos(30, 2, 30));

		HeavyPumpBlockEntity te = (HeavyPumpBlockEntity)h.getBlockEntity(ext);
		Item lithium = ReactorItems.canisterFor(ReactorFluids.get("lithium"));
		boolean[] filled = {false};
		h.succeedWhen(() -> {
			h.assertTrue(te.getOmega() == 128 && te.getTorque() == 512, "extractor input "+te.getOmega()+" rad/s @ "+te.getTorque()+" Nm");
			if (!filled[0]) {
				h.assertTrue(te.getTankLevel() >= 1000 && te.getTank().getFluid().getFluid().isSame(ReactorFluids.get("lithium")),
						"extractor tank "+te.getTankLevel()+" mB "+te.getTank().getFluid().getFluid());
				int before = te.getTankLevel();
				p.hold(ReactorItems.EMPTY_CANISTER.get());
				p.useOn(ext, Direction.NORTH); //through the lava north of it
				p.moveNear(new BlockPos(30, 2, 30));
				filled[0] = true;
				h.assertTrue(te.getTankLevel() <= before-1000+40, "the canister took "+(before-te.getTankLevel())+" mB");
			}
			h.assertTrue(p.count(lithium) == 1 && p.count(ReactorItems.EMPTY_CANISTER.get()) == 0, "player holds "+p.count(lithium)+" lithium canisters");
			Factory.assertBuilt(h, List.of("reactorcraft:fluid_extractor"), new BlockPos(0, 2, 0), new BlockPos(47, 11, 47));
			p.leave();
		});
	}

	/**
	 * Fills the bucket at the lava barrel and pours it the way a player does: from three blocks above the target, looking straight down
	 * (the lava lands on the block under the target and fills it).
	 */
	private static void pourLava(GameTestHelper h, SimPlayer p, BlockPos barrel, BlockPos target) {
		p.use(barrel, Items.BUCKET);
		h.assertTrue(p.count(Items.LAVA_BUCKET) == 1, "no lava bucket from the barrel");
		Vec3 eye = Vec3.atLowerCornerOf(h.absolutePos(target)).add(0.5, 3, 0.5);
		p.hold(Items.LAVA_BUCKET);
		p.player.moveTo(eye.x, eye.y-p.player.getEyeHeight(), eye.z, 0, 90);
		p.player.setYHeadRot(0);
		p.useItem();
		h.assertTrue(p.count(Items.LAVA_BUCKET) == 0, "lava not poured at "+target);
	}
}
