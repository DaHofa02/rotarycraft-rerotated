/*******************************************************************************
 * Practice factory zone (see Factory): RotaryCraft service machines - filling station, refrigerator, pump, wind engine and Van de Graaff
 * generator - built and run by a simulated survival player. Not part of the original mods.
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
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.rotarycraft.content.block.GearboxBlock;
import reika.rotarycraft.content.block.ShaftBlock;
import reika.rotarycraft.content.blockentity.FillingStationBlockEntity;
import reika.rotarycraft.content.blockentity.PumpBlockEntity;
import reika.rotarycraft.content.blockentity.RefrigeratorBlockEntity;
import reika.rotarycraft.content.blockentity.VanDeGraffBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.content.item.IntegratedGearboxItem;
import reika.rotarycraft.content.piping.PipingBlockEntity;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.GearboxTypes;
import reika.rotarycraft.registry.MaterialRegistry;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;
import reika.rotarycraft.registry.RotaryItems;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneUtilities {

	public static final List<String> BLOCKS = List.of("rotarycraft:filling_station", "rotarycraft:refrigerator", "rotarycraft:pump",
			"rotarycraft:wind_engine", "rotarycraft:van_de_graff");

	private static final String PLATFORM = "platform_9x8x9";

	/**
	 * A DC engine (on a redstone block) drives a filling station that gets lubricant through a hose from a barrel; the player puts a 2x
	 * gear upgrade into its GUI, and it comes out filled (a torque gear). The player takes it out and sets it into a gas engine that is
	 * not running yet (its ethanol barrel is set down only afterwards): the engine then gives 256 rad/s @ 256 Nm, and through a bedrock
	 * 8:1 gearbox the refrigerator gets the 2048 Nm it needs. Ice put in through its GUI becomes liquid nitrogen, which a pipe at its
	 * west side draws off.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 2000, batch = "factory")
	public static void fillingStationAndRefrigerator(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8);
		Item gear = RotaryItems.GEAR_UPGRADES.get(1).get();
		ItemStack gbx = GearboxBlock.gearboxItem(GearboxTypes.BEDROCK, 8);
		Factory.infiniteChest(h, chest, new ItemStack(RotaryBlocks.DC_ENGINE.get()), new ItemStack(Items.REDSTONE_BLOCK),
				new ItemStack(RotaryBlocks.FILLING_STATION.get()), new ItemStack(RotaryBlocks.HOSE.get()), new ItemStack(gear),
				new ItemStack(RotaryBlocks.FUEL_LINE.get()), new ItemStack(RotaryBlocks.GAS_ENGINE.get()), gbx,
				new ItemStack(RotaryBlocks.REFRIGERATOR.get()), new ItemStack(RotaryBlocks.PIPE.get()), new ItemStack(Items.ICE));

		// filling station along z = 2, looking east
		Factory.take(p, chest, RotaryBlocks.DC_ENGINE.get().asItem(), 1);
		Factory.take(p, chest, Items.REDSTONE_BLOCK, 1);
		Factory.take(p, chest, RotaryBlocks.FILLING_STATION.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.HOSE.get().asItem(), 1);
		Factory.takeExactly(p, chest, gear, 1);
		BlockPos fs = new BlockPos(6, 2, 2);
		p.place(new BlockPos(7, 2, 2), RotaryBlocks.DC_ENGINE.get(), Direction.EAST);
		p.place(new BlockPos(8, 2, 2), Items.REDSTONE_BLOCK, Direction.EAST);
		p.placeSneaking(fs, new ItemStack(RotaryBlocks.FILLING_STATION.get()), Direction.EAST);
		Factory.fluidBarrel(h, fs.north().above(), RotaryFluids.LUBRICANT.get());
		p.placeSneaking(fs.north(), new ItemStack(RotaryBlocks.HOSE.get()), Direction.EAST);
		p.open(fs);
		p.putInto(FillingStationBlockEntity.INPUT_SLOT, gear, 1); //menu slots = machine slots
		p.closeMenu();

		// refrigerator along z = 5: the engine stands on its fuel line, but without fuel yet
		Factory.take(p, chest, RotaryBlocks.FUEL_LINE.get().asItem(), 2);
		Factory.take(p, chest, RotaryBlocks.GAS_ENGINE.get().asItem(), 1);
		Factory.take(p, chest, gbx, 1);
		Factory.take(p, chest, RotaryBlocks.REFRIGERATOR.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.PIPE.get().asItem(), 1);
		Factory.take(p, chest, Items.ICE, 3);
		BlockPos eng = new BlockPos(7, 3, 5), fr = new BlockPos(5, 3, 5);
		p.place(new BlockPos(8, 2, 5), RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		p.place(new BlockPos(7, 2, 5), RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		p.placeSneaking(eng, new ItemStack(RotaryBlocks.GAS_ENGINE.get()), Direction.EAST);
		p.placeSneaking(new BlockPos(6, 3, 5), gbx, Direction.EAST);
		p.placeSneaking(fr, new ItemStack(RotaryBlocks.REFRIGERATOR.get()), Direction.EAST);
		p.placeSneaking(fr.west(), new ItemStack(RotaryBlocks.PIPE.get()), Direction.EAST);
		p.open(fr);
		p.putInto(0, Items.ICE, 3);
		p.closeMenu();

		FillingStationBlockEntity f = (FillingStationBlockEntity)h.getBlockEntity(fs);
		EngineBlockEntity e = (EngineBlockEntity)h.getBlockEntity(eng);
		RefrigeratorBlockEntity r = (RefrigeratorBlockEntity)h.getBlockEntity(fr);
		PipingBlockEntity pipe = (PipingBlockEntity)h.getBlockEntity(fr.west());
		boolean[] fuelled = {false};
		h.runAtTickTime(100, () -> {
			ItemStack out = f.getInventory().getStackInSlot(FillingStationBlockEntity.OUTPUT_SLOT);
			h.assertTrue(out.is(gear) && IntegratedGearboxItem.getRatioFromIntegratedGearItem(out, true) == 2,
					"filling station: output "+out+", filling "+f.getInventory().getStackInSlot(FillingStationBlockEntity.FILLING_SLOT)+", tank "+f.getTank().getFluidAmount()+" mB, "+f.omega+" rad/s");
			p.open(fs);
			p.shiftClick(FillingStationBlockEntity.OUTPUT_SLOT);
			p.closeMenu();
			p.hold(gear);
			p.player.setShiftKeyDown(true); //sneaking: the gear goes into the engine instead of the click opening it
			try {
				p.useOn(eng, Direction.UP);
			}
			finally {
				p.player.setShiftKeyDown(false);
			}
			h.assertTrue(e.getIntegratedGear() == 2 && p.count(gear) == 0, "torque gear not set into the engine: "+e.getIntegratedGear());
			Factory.fluidBarrel(h, new BlockPos(8, 3, 5), RotaryFluids.ETHANOL.get());
			fuelled[0] = true;
		});
		h.succeedWhen(() -> {
			h.assertTrue(fuelled[0], "engine not fuelled yet");
			h.assertTrue(r.omega == 32 && r.torque == 2048, "refrigerator input "+r.omega+" rad/s @ "+r.torque+" Nm");
			h.assertTrue(pipe.getFluidType() == RotaryFluids.LIQUID_NITROGEN.get() && pipe.getFluidLevel() > 0,
					"no liquid nitrogen in the pipe: "+pipe.getFluidLevel()+" mB, fridge tank "+r.getTank().getFluidAmount()+" mB, progress "+r.time+"/"+r.getOperationTime());
			h.assertTrue(r.getInventory().getStackInSlot(0).getCount() < 3, "ice not used: "+r.getInventory().getStackInSlot(0));
			Factory.assertBuilt(h, List.of("rotarycraft:filling_station", "rotarycraft:refrigerator"), new BlockPos(0, 2, 0), new BlockPos(8, 3, 8));
			p.leave();
		});
	}

	/**
	 * Pump: the player walls in a 3x3 pool on the floor and fills it with four water buckets from a barrel (it stays full: infinite
	 * water); above its middle stands the pump, driven through steel shafts over the wall by a gas engine on a fuel line north of the pool. A pipe
	 * at the pump's side takes the water up.
	 *
	 * Wind engine on a dirt block with nothing in front of its rotor (and nothing in the 32 blocks behind it that would slow it): it gives
	 * its full 1024 rad/s @ 8 Nm, which bevel gears turn up into a Van de Graaff generator: it charges by 4*sqrt(8192) = 362 C per tick.
	 * Then the player sets an iron block beside it, and it discharges into the block.
	 */
	@GameTest(template = Factory.BIG, timeoutTicks = 1500, batch = "factory")
	public static void pumpWindEngineAndVanDeGraaff(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 47, 47);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(1, 2, 1);
		ItemStack shaft = ShaftBlock.shaftItem(MaterialRegistry.STEEL, false);
		Factory.infiniteChest(h, chest, new ItemStack(Items.STONE), new ItemStack(RotaryBlocks.FUEL_LINE.get()), new ItemStack(RotaryBlocks.GAS_ENGINE.get()),
				shaft, new ItemStack(RotaryBlocks.PUMP.get()), new ItemStack(RotaryBlocks.PIPE.get()), new ItemStack(Items.BUCKET),
				new ItemStack(Items.DIRT), new ItemStack(Items.IRON_SHOVEL), new ItemStack(RotaryBlocks.WIND_ENGINE.get()),
				new ItemStack(RotaryBlocks.BEVEL_GEARS.get()), new ItemStack(RotaryBlocks.VAN_DE_GRAFF.get()), new ItemStack(Items.IRON_BLOCK));

		// pump: pool inside x = 3..5, z = 6..8, walled in at y = 2; the engine stands north of the pool, its shaft reaches over the wall
		Factory.take(p, chest, Items.STONE, 16);
		Factory.take(p, chest, RotaryBlocks.FUEL_LINE.get().asItem(), 2);
		Factory.take(p, chest, RotaryBlocks.GAS_ENGINE.get().asItem(), 1);
		Factory.take(p, chest, shaft, 3);
		Factory.take(p, chest, RotaryBlocks.PUMP.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.PIPE.get().asItem(), 1);
		Factory.takeExactly(p, chest, Items.BUCKET, 1);
		for (int x = 2; x <= 6; x++)
			for (int z = 5; z <= 9; z++)
				if (x == 2 || x == 6 || z == 5 || z == 9)
					p.place(new BlockPos(x, 2, z), Items.STONE, Direction.NORTH);
		p.place(new BlockPos(4, 2, 2), RotaryBlocks.FUEL_LINE.get(), Direction.NORTH);
		p.placeSneaking(new BlockPos(4, 2, 3), new ItemStack(RotaryBlocks.FUEL_LINE.get()), Direction.NORTH);
		Factory.fluidBarrel(h, new BlockPos(4, 3, 2), RotaryFluids.ETHANOL.get());
		BlockPos pumpPos = new BlockPos(4, 3, 7);
		//looking north: the engine drives south, along the shafts into the pump (which reads north and south)
		p.placeSneaking(new BlockPos(4, 3, 3), new ItemStack(RotaryBlocks.GAS_ENGINE.get()), Direction.NORTH);
		for (int z = 4; z <= 6; z++)
			p.placeSneaking(new BlockPos(4, 3, z), shaft, Direction.NORTH);
		p.placeSneaking(pumpPos, new ItemStack(RotaryBlocks.PUMP.get()), Direction.NORTH);
		p.placeSneaking(pumpPos.east(), new ItemStack(RotaryBlocks.PIPE.get()), Direction.NORTH);
		BlockPos water = new BlockPos(8, 2, 2);
		Factory.fluidBarrel(h, water, Fluids.WATER);
		for (int[] c : new int[][] {{3, 6}, {5, 6}, {3, 8}, {5, 8}}) {
			p.use(water, Items.BUCKET);
			pour(h, p, new Vec3(c[0]+0.5, 4.5, c[1]+0.5)); //looking straight down into the pool's corner
		}
		h.assertTrue(h.getBlockState(new BlockPos(3, 2, 6)).getFluidState().isSource(), "no water in the pool: "+h.getBlockState(new BlockPos(3, 2, 6)));

		// wind engine and Van de Graaff generator around (5, *, 30); the wind engine looks east (it drives west, its rotor is east of it)
		Factory.take(p, chest, Items.DIRT, 3);
		Factory.take(p, chest, Items.IRON_SHOVEL, 1);
		Factory.take(p, chest, RotaryBlocks.WIND_ENGINE.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.BEVEL_GEARS.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.VAN_DE_GRAFF.get().asItem(), 1);
		Factory.take(p, chest, Items.IRON_BLOCK, 1);
		BlockPos wind = new BlockPos(6, 3, 30), vdgPos = new BlockPos(5, 4, 30);
		p.place(wind.below(), Items.DIRT, Direction.EAST);
		p.place(wind, RotaryBlocks.WIND_ENGINE.get(), Direction.EAST);
		p.place(new BlockPos(5, 2, 30), Items.DIRT, Direction.EAST);
		p.place(new BlockPos(5, 3, 30), Items.DIRT, Direction.EAST);
		p.place(vdgPos, RotaryBlocks.VAN_DE_GRAFF.get(), Direction.EAST);
		p.mine(new BlockPos(5, 3, 30), Items.IRON_SHOVEL);
		p.placeSneaking(new BlockPos(5, 3, 30), new ItemStack(RotaryBlocks.BEVEL_GEARS.get()), Direction.EAST); //last: finds east -> up
		p.moveNear(new BlockPos(38, 2, 1)); //well out of the generator's 16-block reach (it would strike the player)

		PumpBlockEntity pump = (PumpBlockEntity)h.getBlockEntity(pumpPos);
		PipingBlockEntity pipe = (PipingBlockEntity)h.getBlockEntity(pumpPos.east());
		VanDeGraffBlockEntity vdg = (VanDeGraffBlockEntity)h.getBlockEntity(vdgPos);
		int[] last = {-1};
		int[] stage = {0};
		h.succeedWhen(() -> {
			h.assertTrue(pump.omega == 512 && pump.torque == 128, "pump input "+pump.omega+" rad/s @ "+pump.torque+" Nm");
			h.assertTrue(pipe.getFluidType() == Fluids.WATER && pipe.getFluidLevel() > 0, "pump: tank "+pump.getFluidLevel()+" mB, pipe "+pipe.getFluidLevel()+" mB");
			int c = vdg.getCharge();
			if (stage[0] == 0) {
				h.assertTrue(vdg.getPower() == 8192, "Van de Graaff at "+vdg.getPower()+" W (wind engine "+((EngineBlockEntity)h.getBlockEntity(wind)).omega+" rad/s)");
				if (last[0] < 0 || c-last[0] != 362) {
					last[0] = c;
					h.fail("not yet two readings 362 C apart: "+c);
				}
				//charging as it should: now a lightning rod for it - the iron block beside it (it would detonate at 2 MC otherwise)
				p.placeSneaking(vdgPos.north(), new ItemStack(Items.IRON_BLOCK), Direction.EAST);
				p.moveNear(new BlockPos(38, 2, 1));
				last[0] = c;
				stage[0] = 1;
				h.fail("iron block just set down");
			}
			h.assertTrue(c < last[0], "Van de Graaff did not discharge into the iron block: "+c+" C (was "+last[0]+")");
			Factory.assertBuilt(h, BLOCKS.subList(2, 5), new BlockPos(0, 2, 0), new BlockPos(10, 4, 32));
			p.leave();
		});
	}

	/** Pours the held water bucket the way a player does: from that eye position (relative), looking straight down. */
	private static void pour(GameTestHelper h, SimPlayer p, Vec3 eye) {
		Vec3 a = Vec3.atLowerCornerOf(h.absolutePos(BlockPos.ZERO)).add(eye);
		p.hold(Items.WATER_BUCKET);
		p.player.moveTo(a.x, a.y-p.player.getEyeHeight(), a.z, 0, 90);
		p.player.setYHeadRot(0);
		p.useItem();
	}
}
