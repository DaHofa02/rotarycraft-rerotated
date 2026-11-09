/*******************************************************************************
 * Practice factory zone (see Factory): a small ReactorCraft fission reactor, built and started by a simulated survival player. Not part
 * of the original mods.
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
import net.minecraft.world.level.material.Fluids;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.reactorcraft.reactor.ControlRodBlockEntity;
import reika.reactorcraft.reactor.CpuBlockEntity;
import reika.reactorcraft.reactor.FuelRodBlockEntity;
import reika.reactorcraft.reactor.LiquidStates;
import reika.reactorcraft.reactor.ReactorBoilerBlockEntity;
import reika.reactorcraft.reactor.WaterCellBlockEntity;
import reika.reactorcraft.registry.ReactorBlocks;
import reika.reactorcraft.registry.ReactorItems;
import reika.rotarycraft.gametest.SimPlayer;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneFission {

	public static final List<String> BLOCKS = List.of("reactorcraft:fuel_rod", "reactorcraft:coolant_cell", "reactorcraft:reactor_boiler",
			"reactorcraft:steam_line", "reactorcraft:control_rod", "reactorcraft:cpu", "reactorcraft:neutron_reflector");

	private static final String PLATFORM = "platform_9x8x9";

	/**
	 * Nine fuel cores in a 3x3, each given four uranium pellets through its GUI; around them a reactor boiler (a steam line on top) and a
	 * coolant cell, both filled with water buckets from a barrel, a neutron reflector, and a control rod linked through the reactor to a
	 * CPU. Decay neutrons start fission: the centre core becomes active and heats up. The CPU has no shaft power, so it SCRAMs after its
	 * first second - the rod drops in and the CPU's comparator output shows all rods lowered (15).
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 1500, batch = "factory")
	public static void fissionReactorStarts(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8), water = new BlockPos(8, 2, 8);
		Factory.infiniteChest(h, chest, new ItemStack(ReactorBlocks.FUEL_ROD.get()), new ItemStack(ReactorItems.FUEL.get()),
				new ItemStack(ReactorBlocks.WATER_CELL.get()), new ItemStack(ReactorBlocks.BOILER.get()), new ItemStack(ReactorBlocks.STEAM_LINE.get()),
				new ItemStack(ReactorBlocks.CONTROL_ROD.get()), new ItemStack(ReactorBlocks.CPU.get()), new ItemStack(ReactorBlocks.NEUTRON_REFLECTOR.get()),
				new ItemStack(Items.BUCKET));
		Factory.fluidBarrel(h, water, Fluids.WATER);
		Factory.take(p, chest, ReactorBlocks.FUEL_ROD.get().asItem(), 9);
		Factory.take(p, chest, ReactorItems.FUEL.get(), 36);
		List<BlockPos> cores = new ArrayList<>();
		for (int i = 0; i < 9; i++) {
			BlockPos c = new BlockPos(3+i%3, 2, 3+i/3);
			p.place(c, ReactorBlocks.FUEL_ROD.get(), Direction.NORTH);
			cores.add(c);
		}
		for (BlockPos c : cores) {
			p.open(c);
			for (int s = 0; s < 4; s++)
				p.putInto(s, ReactorItems.FUEL.get(), 1);
			p.closeMenu();
		}
		Factory.take(p, chest, ReactorBlocks.BOILER.get().asItem(), 1);
		Factory.take(p, chest, ReactorBlocks.STEAM_LINE.get().asItem(), 1);
		Factory.take(p, chest, ReactorBlocks.WATER_CELL.get().asItem(), 1);
		Factory.take(p, chest, ReactorBlocks.NEUTRON_REFLECTOR.get().asItem(), 1);
		Factory.take(p, chest, ReactorBlocks.CONTROL_ROD.get().asItem(), 1);
		Factory.take(p, chest, ReactorBlocks.CPU.get().asItem(), 1);
		Factory.takeExactly(p, chest, Items.BUCKET, 1);
		BlockPos boiler = new BlockPos(6, 2, 4), cell = new BlockPos(2, 2, 4), rod = new BlockPos(4, 2, 6), cpu = new BlockPos(4, 2, 7);
		p.placeSneaking(boiler, new ItemStack(ReactorBlocks.BOILER.get()), Direction.NORTH);
		p.placeSneaking(boiler.above(), new ItemStack(ReactorBlocks.STEAM_LINE.get()), Direction.NORTH);
		p.placeSneaking(cell, new ItemStack(ReactorBlocks.WATER_CELL.get()), Direction.NORTH);
		p.placeSneaking(new BlockPos(4, 2, 2), new ItemStack(ReactorBlocks.NEUTRON_REFLECTOR.get()), Direction.NORTH);
		p.placeSneaking(rod, new ItemStack(ReactorBlocks.CONTROL_ROD.get()), Direction.NORTH);
		p.placeSneaking(cpu, new ItemStack(ReactorBlocks.CPU.get()), Direction.NORTH);
		p.use(water, Items.BUCKET);
		p.use(boiler, Items.WATER_BUCKET);
		p.use(water, Items.BUCKET);
		p.use(cell, Items.WATER_BUCKET);
		ReactorBoilerBlockEntity b = (ReactorBoilerBlockEntity)h.getBlockEntity(boiler);
		h.assertTrue(b.getTank().getFluidAmount() == 1000, "boiler holds "+b.getTank().getFluidAmount()+" mB after a bucket");
		h.assertTrue(((WaterCellBlockEntity)h.getBlockEntity(cell)).getLiquidState() == LiquidStates.WATER, "coolant cell not filled");
		FuelRodBlockEntity mid = (FuelRodBlockEntity)h.getBlockEntity(cores.get(4));
		CpuBlockEntity c = (CpuBlockEntity)h.getBlockEntity(cpu);
		ControlRodBlockEntity r = (ControlRodBlockEntity)h.getBlockEntity(rod);
		int[] peak = {0};
		h.onEachTick(() -> peak[0] = Math.max(peak[0], mid.getTemperature()));
		h.succeedWhen(() -> {
			h.assertTrue(mid.isActive() && peak[0] >= 40, "centre core: active "+mid.isActive()+", peak "+peak[0]+" C");
			h.assertTrue(r.isActive() && c.getRedstoneOverride() == 15, "rod inserted "+r.isActive()+", CPU comparator "+c.getRedstoneOverride());
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 3, 8));
			p.leave();
		});
	}
}
