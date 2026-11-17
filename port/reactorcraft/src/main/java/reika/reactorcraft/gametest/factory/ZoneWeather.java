/*******************************************************************************
 * Practice factory zone (see Factory): the RotaryCraft weather controller built and filled by a simulated survival player. Not part of the
 * original mods.
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

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.rotarycraft.content.blockentity.WeatherControllerBlockEntity;
import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;
import reika.rotarycraft.registry.RotaryItems;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneWeather {

	public static final List<String> BLOCKS = List.of("rotarycraft:weather_controller");

	private static final String PLATFORM = "platform_9x8x9";

	/**
	 * Ethanol barrel -> fuel lines -> gas engine -> bevel gears (east -> up) -> weather controller on top. Under a clear sky the player puts
	 * silver iodide in through its GUI: it fires it into the sky and it starts to rain.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 300, batch = "weatherzone")
	public static void weatherControllerSeedsClouds(GameTestHelper h) {
		h.getLevel().setWeatherParameters(24000, 0, false, false);
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8);
		Item iodide = RotaryItems.part(CraftingPart.SILVERIODIDE);
		Factory.infiniteChest(h, chest, new ItemStack(RotaryBlocks.FUEL_LINE.get()), new ItemStack(RotaryBlocks.GAS_ENGINE.get()),
				new ItemStack(RotaryBlocks.BEVEL_GEARS.get()), new ItemStack(RotaryBlocks.WEATHER_CONTROLLER.get()), new ItemStack(Items.DIRT),
				new ItemStack(Items.IRON_SHOVEL), new ItemStack(iodide));
		Factory.fluidBarrel(h, new BlockPos(8, 3, 5), RotaryFluids.ETHANOL.get());
		Factory.take(p, chest, RotaryBlocks.FUEL_LINE.get().asItem(), 3);
		Factory.take(p, chest, RotaryBlocks.GAS_ENGINE.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.BEVEL_GEARS.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.WEATHER_CONTROLLER.get().asItem(), 1);
		Factory.takeExactly(p, chest, Items.DIRT, 2);
		Factory.takeExactly(p, chest, Items.IRON_SHOVEL, 1);
		Factory.takeExactly(p, chest, iodide, 2);
		for (int x = 8; x >= 6; x--)
			p.place(new BlockPos(x, 2, 5), RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		p.placeSneaking(new BlockPos(6, 3, 5), new ItemStack(RotaryBlocks.GAS_ENGINE.get()), Direction.EAST);
		BlockPos bevel = new BlockPos(5, 3, 5), pos = bevel.above();
		p.place(bevel.below(), Items.DIRT, Direction.EAST);
		p.place(bevel, Items.DIRT, Direction.EAST); //a support, dug out again
		p.place(pos, RotaryBlocks.WEATHER_CONTROLLER.get(), Direction.EAST);
		p.mine(bevel, Items.IRON_SHOVEL);
		p.placeSneaking(bevel, new ItemStack(RotaryBlocks.BEVEL_GEARS.get()), Direction.EAST); //last: finds east -> up
		h.assertTrue(p.open(pos) != null, "weather controller opens no GUI");
		p.putInto(0, iodide, 2);
		p.closeMenu();
		WeatherControllerBlockEntity te = (WeatherControllerBlockEntity)h.getBlockEntity(pos);

		h.succeedWhen(() -> {
			h.assertTrue(h.getLevel().isRaining() && !h.getLevel().isThundering(), "no rain at "+te.getPower()+" W");
			h.assertTrue(te.getInventory().getStackInSlot(0).getCount() == 1, "silver iodide not used");
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 4, 8));
			//the structure stays and the controller would set its last mode again after its cooldown: take it away
			for (int i = 0; i < te.getInventory().getSlots(); i++)
				te.getInventory().setStackInSlot(i, ItemStack.EMPTY);
			h.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR);
			h.getLevel().setWeatherParameters(24000, 0, false, false);
			h.getLevel().setRainLevel(0); //otherwise the rain fades out over 100 ticks, into later tests
			p.leave();
		});
	}
}
