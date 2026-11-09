/*******************************************************************************
 * Practice factory zone (see Factory): the ReactorCraft pressurizer, driven from below by a gas engine through a reduction gearbox and
 * bevel gears, turning low pressure water from a barrel back into water that a pipe carries into a reservoir; built and run by a
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
import reika.reactorcraft.powergen.ReactorPumpBlockEntity;
import reika.reactorcraft.registry.ReactorBlocks;
import reika.reactorcraft.registry.ReactorFluids;
import reika.rotarycraft.content.block.GearboxBlock;
import reika.rotarycraft.content.blockentity.ReservoirBlockEntity;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.GearboxTypes;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZonePressurizer {

	public static final List<String> BLOCKS = List.of("reactorcraft:pressurizer");

	private static final String PLATFORM = "platform_9x8x9";

	/**
	 * Ethanol barrel -> fuel lines -> gas engine (512 rad/s @ 128 Nm) -> bedrock gearbox 16:1 (reduction, as placed) -> bevel gears turning
	 * the power up into the pressurizer standing on them: 32 rad/s @ 2048 Nm, above its minimum of 1024 Nm and 16384 W. A barrel of low
	 * pressure water (what turbines leave behind) feeds it from the top through a pipe; it pushes the water out of its north side into a
	 * pipe that runs down and into a reservoir the player set on the floor.
	 *
	 * Plan (z = 2..4, x = 5..8; the pressurizer at (5, 4, 4) on bevel gears at (5, 3, 4), which stand on a dirt block):
	 *
	 *   y=6  (5,6,4) low pressure water barrel
	 *   y=5  (5,5,4) pipe
	 *   y=4  (5,4,3) pipe <- (5,4,4) pressurizer
	 *   y=3  (5,3,3) pipe     (5,3,4) bevel gears <- (6,3,4) gearbox <- (7,3,4) gas engine   (8,3,4) ethanol barrel
	 *   y=2  (5,2,2) reservoir <- (5,2,3) pipe   (5,2,4) dirt   (7,2,4), (8,2,4) fuel lines
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 1200, batch = "factory")
	public static void pressurizerRefillsWater(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8);
		ItemStack gbx16 = GearboxBlock.gearboxItem(GearboxTypes.BEDROCK, 16); //bedrock gears need no lubricant and do not wear
		Factory.infiniteChest(h, chest, new ItemStack(RotaryBlocks.FUEL_LINE.get()), new ItemStack(RotaryBlocks.GAS_ENGINE.get()), gbx16,
				new ItemStack(RotaryBlocks.BEVEL_GEARS.get()), new ItemStack(ReactorBlocks.PRESSURIZER.get()), new ItemStack(RotaryBlocks.PIPE.get()),
				new ItemStack(RotaryBlocks.RESERVOIR.get()), new ItemStack(Items.DIRT), new ItemStack(Items.IRON_SHOVEL));
		Factory.take(p, chest, RotaryBlocks.FUEL_LINE.get().asItem(), 2);
		Factory.take(p, chest, RotaryBlocks.GAS_ENGINE.get().asItem(), 1);
		Factory.take(p, chest, gbx16, 1);
		Factory.take(p, chest, RotaryBlocks.BEVEL_GEARS.get().asItem(), 1);
		Factory.take(p, chest, ReactorBlocks.PRESSURIZER.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.PIPE.get().asItem(), 4);
		Factory.take(p, chest, RotaryBlocks.RESERVOIR.get().asItem(), 1);
		Factory.take(p, chest, Items.DIRT, 2);
		Factory.take(p, chest, Items.IRON_SHOVEL, 1);

		// the drive: barrel, fuel lines, gas engine looking east (output west), gearbox
		Factory.fluidBarrel(h, new BlockPos(8, 3, 4), RotaryFluids.ETHANOL.get());
		p.place(new BlockPos(8, 2, 4), RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		p.place(new BlockPos(7, 2, 4), RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		p.placeSneaking(new BlockPos(7, 3, 4), new ItemStack(RotaryBlocks.GAS_ENGINE.get()), Direction.EAST);
		p.placeSneaking(new BlockPos(6, 3, 4), gbx16, Direction.EAST);
		// the pressurizer on two dirt blocks; the upper one is dug away and replaced by the bevel gears (last, so they find east -> up)
		BlockPos pump = new BlockPos(5, 4, 4);
		p.place(new BlockPos(5, 2, 4), Items.DIRT, Direction.EAST);
		p.place(new BlockPos(5, 3, 4), Items.DIRT, Direction.EAST);
		p.place(pump, ReactorBlocks.PRESSURIZER.get().asItem(), Direction.EAST);
		p.mine(new BlockPos(5, 3, 4), Items.IRON_SHOVEL);
		p.placeSneaking(new BlockPos(5, 3, 4), new ItemStack(RotaryBlocks.BEVEL_GEARS.get()), Direction.EAST);

		// output: pipe out of the north side, down to a reservoir on the floor (entered from its side)
		BlockPos res = new BlockPos(5, 2, 2);
		p.placeSneaking(res, new ItemStack(RotaryBlocks.RESERVOIR.get()), Direction.NORTH);
		p.placeSneaking(new BlockPos(5, 2, 3), new ItemStack(RotaryBlocks.PIPE.get()), Direction.NORTH);
		p.placeSneaking(new BlockPos(5, 3, 3), new ItemStack(RotaryBlocks.PIPE.get()), Direction.NORTH);
		p.placeSneaking(new BlockPos(5, 4, 3), new ItemStack(RotaryBlocks.PIPE.get()), Direction.NORTH);
		// input: pipe on top, the low pressure water barrel above it
		p.placeSneaking(new BlockPos(5, 5, 4), new ItemStack(RotaryBlocks.PIPE.get()), Direction.NORTH);
		Factory.fluidBarrel(h, new BlockPos(5, 6, 4), ReactorFluids.get("low_pressure_water"));

		ReactorPumpBlockEntity te = (ReactorPumpBlockEntity)h.getBlockEntity(pump);
		h.succeedWhen(() -> {
			h.assertTrue(te.getOmega() == 32 && te.getTorque() == 2048 && te.sufficientPower(), "pressurizer input "+te.getOmega()+" rad/s @ "+te.getTorque()+" Nm");
			ReservoirBlockEntity r = (ReservoirBlockEntity)h.getBlockEntity(res);
			h.assertTrue(r.getFluid().isSame(Fluids.WATER) && r.getFluidAmount() >= 1000,
					"reservoir "+r.getFluid()+" "+r.getFluidAmount()+" mB; pressurizer in "+te.getTank().getFluidAmount()+" / out "+te.getOutputTank().getFluidAmount()+" mB");
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 7, 8));
			p.leave();
		});
	}
}
