/*******************************************************************************
 * Practice factory zone (see Factory): the RotaryCraft lava smeltery built and run by a simulated survival player. Not part of the
 * original mods.
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
import reika.rotarycraft.content.blockentity.BigFurnaceBlockEntity;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneSmeltery {

	public static final List<String> BLOCKS = List.of("rotarycraft:big_furnace");

	private static final String PLATFORM = "platform_9x8x9";

	/**
	 * Ethanol barrel -> fuel lines -> gas engine -> bevel gears -> lava smeltery above them; a lava barrel over a pipe feeds it from the
	 * side. The player loads cobblestone through the GUI. The lava heats it (reaching the 400 deg C to smelt takes about four minutes,
	 * so the zone checks the heating: past 100 deg C with power and lava flowing in).
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 2400, batch = "factory")
	public static void smelteryHeatsUpWithLava(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8);
		Factory.infiniteChest(h, chest, new ItemStack(RotaryBlocks.FUEL_LINE.get()), new ItemStack(RotaryBlocks.GAS_ENGINE.get()),
				new ItemStack(RotaryBlocks.BEVEL_GEARS.get()), new ItemStack(RotaryBlocks.BIG_FURNACE.get()), new ItemStack(RotaryBlocks.PIPE.get()),
				new ItemStack(Items.DIRT), new ItemStack(Items.IRON_SHOVEL), new ItemStack(Items.COBBLESTONE));
		Factory.fluidBarrel(h, new BlockPos(8, 3, 5), RotaryFluids.ETHANOL.get());
		Factory.take(p, chest, RotaryBlocks.FUEL_LINE.get().asItem(), 3);
		Factory.take(p, chest, RotaryBlocks.GAS_ENGINE.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.BEVEL_GEARS.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.BIG_FURNACE.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.PIPE.get().asItem(), 1);
		Factory.takeExactly(p, chest, Items.DIRT, 4);
		Factory.takeExactly(p, chest, Items.IRON_SHOVEL, 1);
		Factory.takeExactly(p, chest, Items.COBBLESTONE, 16);
		for (int x = 8; x >= 6; x--)
			p.place(new BlockPos(x, 2, 5), RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		p.placeSneaking(new BlockPos(6, 3, 5), new ItemStack(RotaryBlocks.GAS_ENGINE.get()), Direction.EAST);
		BlockPos pos = new BlockPos(5, 4, 5), pipe = new BlockPos(4, 4, 5);
		p.place(new BlockPos(5, 2, 5), Items.DIRT, Direction.EAST);
		p.place(new BlockPos(5, 3, 5), Items.DIRT, Direction.EAST);
		p.place(pos, RotaryBlocks.BIG_FURNACE.get(), Direction.EAST);
		p.place(new BlockPos(4, 2, 5), Items.DIRT, Direction.EAST);
		p.place(new BlockPos(4, 3, 5), Items.DIRT, Direction.EAST);
		p.place(pipe, RotaryBlocks.PIPE.get(), Direction.EAST);
		Factory.fluidBarrel(h, pipe.above(), Fluids.LAVA); //a reservoir drains downwards only
		p.mine(new BlockPos(5, 3, 5), Items.IRON_SHOVEL);
		p.placeSneaking(new BlockPos(5, 3, 5), new ItemStack(RotaryBlocks.BEVEL_GEARS.get()), Direction.EAST); //last: finds east -> up
		BigFurnaceBlockEntity te = (BigFurnaceBlockEntity)h.getBlockEntity(pos);

		h.assertTrue(p.open(pos) != null, "smeltery opens no GUI");
		p.putInto(0, Items.COBBLESTONE, 16);
		p.closeMenu();
		h.assertTrue(te.getInventory().getStackInSlot(0).getCount() == 16, "smeltery holds "+te.getInventory().getStackInSlot(0));

		h.succeedWhen(() -> {
			h.assertTrue(te.power == 65536, "smeltery at "+te.power+" W");
			h.assertTrue(te.getTank().getFluid().is(Fluids.LAVA) && te.getTank().getFluidAmount() > 0, "no lava in the smeltery");
			h.assertTrue(te.getTemperature() > 100, "smeltery only at "+te.getTemperature()+" deg C");
			h.assertTrue(!te.canSmelt() && te.getInventory().getStackInSlot(18).isEmpty(), "smelting below 400 deg C");
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 6, 8));
			p.leave();
		});
	}
}
