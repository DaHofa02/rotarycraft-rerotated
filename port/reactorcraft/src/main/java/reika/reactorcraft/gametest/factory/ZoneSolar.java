/*******************************************************************************
 * Practice factory zone (see Factory): a solar plant - RotaryCraft tower and mirrors with the ReactorCraft receiver on top - built by a
 * simulated survival player. Not part of the original mods.
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
import net.minecraft.world.level.material.Fluids;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.reactorcraft.powergen.SolarTopBlockEntity;
import reika.reactorcraft.registry.ReactorBlocks;
import reika.rotarycraft.content.blockentity.DynamometerBlockEntity;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.lib.world.AmbientTemperature;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneSolar {

	public static final List<String> BLOCKS = List.of("rotarycraft:solar_tower", "rotarycraft:solar_mirror", "reactorcraft:solar_top");

	private static final String PLATFORM = "platform_9x8x9";

	/**
	 * At noon (the time is set, as the solar tests of the ports do): a tower three blocks high on the floor, the two-block sodium
	 * receiver on top, eight mirrors joined to it on the floor and water piped from a barrel into the tower's foot. The tower gives its
	 * power downwards: the player digs two floor blocks out for bevel gears under the tower and a dynamometer beside them, which reads
	 * 512 rad/s. With eight mirrors the receiver heats up (fewer than eight do not heat it at all, as in the original).
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 400, batch = "factory")
	public static void solarPlantRunsAtNoon(GameTestHelper h) {
		h.getLevel().setDayTime(6000);
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8);
		Factory.infiniteChest(h, chest, new ItemStack(RotaryBlocks.SOLAR_TOWER.get()), new ItemStack(RotaryBlocks.MIRROR.get()),
				new ItemStack(ReactorBlocks.SOLAR_TOP.get()), new ItemStack(RotaryBlocks.PIPE.get()), new ItemStack(RotaryBlocks.BEVEL_GEARS.get()),
				new ItemStack(RotaryBlocks.DYNAMOMETER.get()), new ItemStack(Items.IRON_PICKAXE));
		Factory.take(p, chest, RotaryBlocks.SOLAR_TOWER.get().asItem(), 3);
		Factory.take(p, chest, RotaryBlocks.MIRROR.get().asItem(), 8);
		Factory.take(p, chest, ReactorBlocks.SOLAR_TOP.get().asItem(), 2);
		Factory.take(p, chest, RotaryBlocks.PIPE.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.BEVEL_GEARS.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.DYNAMOMETER.get().asItem(), 1);
		Factory.take(p, chest, Items.IRON_PICKAXE, 1);
		BlockPos foot = new BlockPos(4, 2, 4), bevel = foot.below(), dyna = bevel.west();
		p.mine(bevel, Items.IRON_PICKAXE);
		p.mine(dyna, Items.IRON_PICKAXE);
		p.place(dyna, RotaryBlocks.DYNAMOMETER.get(), Direction.EAST);
		//the pipe first: the tower's foot stands over the pit and is set against it
		Factory.fluidBarrel(h, new BlockPos(4, 3, 3), Fluids.WATER);
		p.placeSneaking(new BlockPos(4, 2, 3), new ItemStack(RotaryBlocks.PIPE.get()), Direction.NORTH);
		for (int i = 0; i < 3; i++)
			p.placeSneaking(foot.above(i), new ItemStack(RotaryBlocks.SOLAR_TOWER.get()), Direction.NORTH);
		p.placeSneaking(foot.above(3), new ItemStack(ReactorBlocks.SOLAR_TOP.get()), Direction.NORTH);
		p.placeSneaking(foot.above(4), new ItemStack(ReactorBlocks.SOLAR_TOP.get()), Direction.NORTH);
		//a plant joins only mirrors touching the tower or each other: the ring around the foot (the pipe takes its north spot) and one more
		int[][] mirrors = {{3, 3}, {5, 3}, {3, 4}, {5, 4}, {3, 5}, {4, 5}, {5, 5}, {2, 4}};
		for (int[] m : mirrors)
			p.place(new BlockPos(m[0], 2, m[1]), RotaryBlocks.MIRROR.get(), Direction.NORTH);
		p.placeSneaking(bevel, new ItemStack(RotaryBlocks.BEVEL_GEARS.get()), Direction.NORTH);
		DynamometerBlockEntity d = (DynamometerBlockEntity)h.getBlockEntity(dyna);
		SolarTopBlockEntity top = (SolarTopBlockEntity)h.getBlockEntity(foot.above(3));
		int amb = AmbientTemperature.getAmbientTemperatureAt(h.getLevel(), h.absolutePos(foot.above(3)));
		h.succeedWhen(() -> {
			h.assertTrue(d.omega == 512 && d.torque > 0, "dynamometer under the tower: "+d.omega+" rad/s @ "+d.torque+" Nm; tower "+tw(h, foot)+", receiver "+top.getTemperature());
			h.assertTrue(top.getTemperature() > amb+50, "receiver at "+top.getTemperature()+" C (ambient "+amb+")");
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 1, 0), new BlockPos(8, 7, 8));
			p.leave();
		});
	}

	private static String tw(GameTestHelper h, BlockPos foot) {
		var t = (reika.rotarycraft.content.blockentity.SolarTowerBlockEntity)h.getBlockEntity(foot);
		var pl = t.getPlant();
		return t.getOmega()+"@"+t.getTorque()+" plant "+(pl == null ? "none" : pl.mirrorCount()+"m/"+pl.towerCount()+"t x"+pl.getTowerMultiplier())+" use "+t.getCurrentConsumption();
	}
}
