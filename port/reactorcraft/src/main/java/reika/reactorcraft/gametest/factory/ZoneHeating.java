/*******************************************************************************
 * Practice factory zone (see Factory): the RotaryCraft heater built and run by a simulated survival player. Not part of the original
 * mods.
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

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.rotarycraft.content.blockentity.BlastFurnaceBlockEntity;
import reika.rotarycraft.content.blockentity.HeaterBlockEntity;
import reika.rotarycraft.content.menu.HeaterMenu;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneHeating {

	public static final List<String> BLOCKS = List.of("rotarycraft:heater");

	private static final String PLATFORM = "platform_9x8x9";

	/**
	 * Ethanol barrel -> fuel line -> gas engine (65536 W) -> bevel gears -> heater with a blast furnace on top. The player puts coal
	 * into the heater's GUI (cobblestone is refused) and sets 400 deg C: the heater burns coal (96 deg C per piece) and the blast
	 * furnace heats up with it.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 3000, batch = "factory")
	public static void heaterHeatsABlastFurnace(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8);
		Factory.infiniteChest(h, chest, new ItemStack(RotaryBlocks.FUEL_LINE.get()), new ItemStack(RotaryBlocks.GAS_ENGINE.get()),
				new ItemStack(RotaryBlocks.BEVEL_GEARS.get()), new ItemStack(RotaryBlocks.HEATER.get()), new ItemStack(RotaryBlocks.BLAST_FURNACE.get()),
				new ItemStack(Items.DIRT), new ItemStack(Items.IRON_SHOVEL), new ItemStack(Items.COAL), new ItemStack(Items.COBBLESTONE));
		Factory.fluidBarrel(h, new BlockPos(8, 3, 5), RotaryFluids.ETHANOL.get());

		Factory.take(p, chest, RotaryBlocks.FUEL_LINE.get().asItem(), 3);
		Factory.take(p, chest, RotaryBlocks.GAS_ENGINE.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.BEVEL_GEARS.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.HEATER.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.BLAST_FURNACE.get().asItem(), 1);
		Factory.takeExactly(p, chest, Items.DIRT, 2);
		Factory.takeExactly(p, chest, Items.IRON_SHOVEL, 1);
		Factory.takeExactly(p, chest, Items.COAL, 8);
		Factory.takeExactly(p, chest, Items.COBBLESTONE, 1);
		for (int x = 8; x >= 6; x--)
			p.place(new BlockPos(x, 2, 5), RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		p.placeSneaking(new BlockPos(6, 3, 5), new ItemStack(RotaryBlocks.GAS_ENGINE.get()), Direction.EAST);
		// the heater on a dirt pillar, whose top block is dug away again and replaced by the bevel gears (placed last: east -> up)
		BlockPos pos = new BlockPos(5, 4, 5);
		p.place(new BlockPos(5, 2, 5), Items.DIRT, Direction.EAST);
		p.place(pos.below(), Items.DIRT, Direction.EAST);
		p.place(pos, RotaryBlocks.HEATER.get(), Direction.EAST);
		p.placeSneaking(pos.above(), new ItemStack(RotaryBlocks.BLAST_FURNACE.get()), Direction.EAST);
		p.mine(pos.below(), Items.IRON_SHOVEL);
		p.placeSneaking(pos.below(), new ItemStack(RotaryBlocks.BEVEL_GEARS.get()), Direction.EAST);

		HeaterBlockEntity te = (HeaterBlockEntity)h.getBlockEntity(pos);
		BlastFurnaceBlockEntity furnace = (BlastFurnaceBlockEntity)h.getBlockEntity(pos.above());
		h.assertTrue(p.open(pos) instanceof HeaterMenu, "heater opens no GUI");
		HeaterMenu menu = (HeaterMenu)p.player.containerMenu;
		p.putInto(0, Items.COAL, 8);
		p.putInto(1, Items.COBBLESTONE, 1);
		h.assertTrue(menu.clickMenuButton(p.player, 400) && !menu.clickMenuButton(p.player, 10000), "temperature field");
		p.closeMenu();
		h.assertTrue(te.getInventory().getStackInSlot(0).getCount() == 8 && p.count(Items.COAL) == 0, "coal not put in");
		h.assertTrue(te.getInventory().getStackInSlot(1).isEmpty() && p.count(Items.COBBLESTONE) == 1, "the heater took cobblestone");
		h.assertTrue(te.getSetTemperature() == 400, "set temperature "+te.getSetTemperature());

		h.succeedWhen(() -> {
			h.assertTrue(te.power == 65536, "heater at "+te.omega+" rad/s @ "+te.torque+" Nm");
			h.assertTrue(te.getTemperature() >= 300 && te.getTemperature() <= 400, "heater at "+te.getTemperature()+" deg C");
			h.assertTrue(te.getInventory().getStackInSlot(0).getCount() <= 5, te.getInventory().getStackInSlot(0).getCount()+" coal left");
			h.assertTrue(furnace.getTemperature() >= 250, "blast furnace at "+furnace.getTemperature()+" deg C");
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 6, 8));
			p.leave();
		});
	}
}
