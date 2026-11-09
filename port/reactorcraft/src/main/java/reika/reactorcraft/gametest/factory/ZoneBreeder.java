/*******************************************************************************
 * Practice factory zone (see Factory): breeder cores, sodium heaters and a tritizer around a small ReactorCraft fission reactor, built
 * by a simulated survival player. Not part of the original mods.
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
import reika.reactorcraft.reactor.BreederCoreBlockEntity;
import reika.reactorcraft.reactor.FuelRodBlockEntity;
import reika.reactorcraft.reactor.SodiumHeaterBlockEntity;
import reika.reactorcraft.reactor.TritizerBlockEntity;
import reika.reactorcraft.registry.ReactorBlocks;
import reika.reactorcraft.registry.ReactorFluids;
import reika.reactorcraft.registry.ReactorItems;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.lib.world.AmbientTemperature;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneBreeder {

	public static final List<String> BLOCKS = List.of("reactorcraft:breeder_core", "reactorcraft:sodium_heater", "reactorcraft:tritizer");

	private static final String PLATFORM = "platform_9x8x9";

	/**
	 * Eleven uranium fuel cores (four pellets each through the GUI, as in ZoneFission): a 3x3 and two more flanking the west breeder core,
	 * are the neutron source. Beside them two breeder cores given four breeder fuel pellets each through the GUI, a tritizer fed with
	 * water by a pipe from a barrel on top, and, west of one breeder core, a column of two sodium heaters fed with sodium by pipes under
	 * the floor from a barrel. Decay neutrons start fission; within 30 s the fission neutrons make the breeder cores react (active,
	 * 60 C or more above ambient), and the west one warms the sodium heater beside it (50 C or more above ambient); the lower heater
	 * passes its sodium up to the one above; fission neutrons turn the tritizer's water into heavy water. (Hot sodium needs a 301 C
	 * heater; this small reactor levels out at about 200 C.)
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 1500, batch = "factory")
	public static void breederAndTritizerAroundFission(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8);
		Factory.infiniteChest(h, chest, new ItemStack(ReactorBlocks.FUEL_ROD.get()), new ItemStack(ReactorItems.FUEL.get()),
				new ItemStack(ReactorBlocks.BREEDER_CORE.get()), new ItemStack(ReactorItems.BREEDER_FUEL.get()), new ItemStack(ReactorBlocks.SODIUM_HEATER.get()),
				new ItemStack(ReactorBlocks.TRITIZER.get()), new ItemStack(RotaryBlocks.PIPE.get()), new ItemStack(Items.IRON_PICKAXE));
		Factory.take(p, chest, ReactorBlocks.FUEL_ROD.get().asItem(), 11);
		Factory.take(p, chest, ReactorItems.FUEL.get(), 44);
		List<BlockPos> cores = new ArrayList<>();
		for (int i = 0; i < 9; i++) {
			BlockPos c = new BlockPos(3+i%3, 2, 3+i/3);
			p.place(c, ReactorBlocks.FUEL_ROD.get(), Direction.NORTH);
			cores.add(c);
		}
		for (BlockPos c : List.of(new BlockPos(2, 2, 3), new BlockPos(2, 2, 5))) {
			p.placeSneaking(c, new ItemStack(ReactorBlocks.FUEL_ROD.get()), Direction.NORTH);
			cores.add(c);
		}
		for (BlockPos c : cores) {
			p.open(c);
			for (int s = 0; s < 4; s++)
				p.putInto(s, ReactorItems.FUEL.get(), 1);
			p.closeMenu();
		}

		// breeder cores west and north of the reactor
		BlockPos breederW = new BlockPos(2, 2, 4), breederN = new BlockPos(4, 2, 2);
		Factory.takeExactly(p, chest, ReactorBlocks.BREEDER_CORE.get().asItem(), 2);
		Factory.takeExactly(p, chest, ReactorItems.BREEDER_FUEL.get(), 8);
		for (BlockPos b : List.of(breederW, breederN)) {
			p.placeSneaking(b, new ItemStack(ReactorBlocks.BREEDER_CORE.get()), Direction.NORTH);
			p.open(b);
			for (int s = 0; s < 4; s++)
				p.putInto(s, ReactorItems.BREEDER_FUEL.get(), 1);
			p.closeMenu();
		}

		// sodium heaters west of the west breeder core, sodium from a barrel through pipes under the floor
		BlockPos heater = breederW.west(), barrelNa = new BlockPos(1, 2, 5);
		Factory.takeExactly(p, chest, Items.IRON_PICKAXE, 1);
		Factory.takeExactly(p, chest, RotaryBlocks.PIPE.get().asItem(), 3);
		Factory.takeExactly(p, chest, ReactorBlocks.SODIUM_HEATER.get().asItem(), 2);
		for (BlockPos q : List.of(barrelNa.below(), heater.below())) {
			p.mine(q, Items.IRON_PICKAXE);
			p.placeSneaking(q, new ItemStack(RotaryBlocks.PIPE.get()), Direction.NORTH);
		}
		p.placeSneaking(heater, new ItemStack(ReactorBlocks.SODIUM_HEATER.get()), Direction.NORTH);
		p.placeSneaking(heater.above(), new ItemStack(ReactorBlocks.SODIUM_HEATER.get()), Direction.NORTH);
		Factory.fluidBarrel(h, barrelNa, ReactorFluids.get("sodium"));

		// tritizer south of the reactor, water from a barrel through a pipe on top
		BlockPos trit = new BlockPos(4, 2, 6);
		Factory.takeExactly(p, chest, ReactorBlocks.TRITIZER.get().asItem(), 1);
		p.placeSneaking(trit, new ItemStack(ReactorBlocks.TRITIZER.get()), Direction.NORTH);
		p.placeSneaking(trit.above(), new ItemStack(RotaryBlocks.PIPE.get()), Direction.NORTH);
		Factory.fluidBarrel(h, trit.above(2), Fluids.WATER);

		FuelRodBlockEntity mid = (FuelRodBlockEntity)h.getBlockEntity(cores.get(4));
		BreederCoreBlockEntity bw = (BreederCoreBlockEntity)h.getBlockEntity(breederW), bn = (BreederCoreBlockEntity)h.getBlockEntity(breederN);
		SodiumHeaterBlockEntity lower = (SodiumHeaterBlockEntity)h.getBlockEntity(heater), upper = (SodiumHeaterBlockEntity)h.getBlockEntity(heater.above());
		TritizerBlockEntity tr = (TritizerBlockEntity)h.getBlockEntity(trit);
		//reactor parts start at 0 C and settle to the ambient temperature; heating is measured against it
		int amb = AmbientTemperature.getAmbientTemperatureAt(h.getLevel(), h.absolutePos(heater));
		int[] peak = {0, 0, 0, 0}; //centre core, west breeder, north breeder, lower heater
		h.onEachTick(() -> {
			peak[0] = Math.max(peak[0], mid.getTemperature());
			peak[1] = Math.max(peak[1], bw.getTemperature());
			peak[2] = Math.max(peak[2], bn.getTemperature());
			peak[3] = Math.max(peak[3], lower.getTemperature());
		});
		h.succeedWhen(() -> {
			String state = "ambient "+amb+" C, centre core peak "+peak[0]+" C, breeders peak "+peak[1]+"/"+peak[2]+" C, heater peak "+peak[3]+" C, sodium "
					+lower.getTank().getFluidAmount()+"/"+upper.getTank().getFluidAmount()+" mB, tritizer "+tr.getInput().getFluidAmount()+" mB in, "
					+tr.getOutput().getFluidAmount()+" mB out";
			h.assertTrue(h.getTick() >= 600, "watching the temperatures for 30 s: "+state); //fission is random: the peaks over a while
			h.assertTrue(mid.isActive() && peak[0] >= 40, state);
			h.assertTrue(bw.isActive() && bn.isActive() && peak[1] >= amb+60 && peak[2] >= amb+60, "breeders: "+state);
			h.assertTrue(lower.getTank().getFluidAmount() > 0 && upper.getTank().getFluidAmount() > 0, "sodium not moved: "+state);
			h.assertTrue(peak[3] >= amb+50, "heater not warmed: "+state);
			h.assertTrue(tr.getOutput().getFluidAmount() >= 100 && tr.getOutput().getFluid().getFluid().isSame(ReactorFluids.get("heavy_water")), "no heavy water: "+state);
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 3, 8));
			p.leave();
		});
	}
}
