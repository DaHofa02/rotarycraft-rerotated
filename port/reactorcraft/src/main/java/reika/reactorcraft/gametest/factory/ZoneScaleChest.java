/*******************************************************************************
 * Practice factory zone (see Factory): the RotaryCraft scaleable chest built and used by a simulated survival player. Not part of the
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

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.rotarycraft.content.blockentity.ScaleableChestBlockEntity;
import reika.rotarycraft.content.menu.ScaleableChestMenu;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneScaleChest {

	public static final List<String> BLOCKS = List.of("rotarycraft:scaleable_chest");

	private static final String PLATFORM = "platform_9x8x9";

	/**
	 * Ethanol barrel -> fuel lines -> gas engine -> scaleable chest (489 slots, 10 pages). The player puts cobblestone on page 0, turns
	 * to page 1 with the Next button and puts dirt there.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 300, batch = "factory")
	public static void chestHoldsPages(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8);
		Factory.infiniteChest(h, chest, new ItemStack(RotaryBlocks.FUEL_LINE.get()), new ItemStack(RotaryBlocks.GAS_ENGINE.get()),
				new ItemStack(RotaryBlocks.SCALEABLE_CHEST.get()), new ItemStack(Items.DIRT), new ItemStack(Items.COBBLESTONE));
		Factory.fluidBarrel(h, new BlockPos(8, 3, 5), RotaryFluids.ETHANOL.get());
		Factory.take(p, chest, RotaryBlocks.FUEL_LINE.get().asItem(), 3);
		Factory.take(p, chest, RotaryBlocks.GAS_ENGINE.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.SCALEABLE_CHEST.get().asItem(), 1);
		Factory.takeExactly(p, chest, Items.DIRT, 9);
		Factory.takeExactly(p, chest, Items.COBBLESTONE, 16);
		for (int x = 8; x >= 6; x--)
			p.place(new BlockPos(x, 2, 5), RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		p.placeSneaking(new BlockPos(6, 3, 5), new ItemStack(RotaryBlocks.GAS_ENGINE.get()), Direction.EAST);
		BlockPos pos = new BlockPos(5, 3, 5);
		p.place(pos.below(), Items.DIRT, Direction.EAST);
		p.place(pos, RotaryBlocks.SCALEABLE_CHEST.get(), Direction.EAST); //looking east: takes its power from the east
		ScaleableChestBlockEntity te = (ScaleableChestBlockEntity)h.getBlockEntity(pos);
		boolean[] done = {false};

		h.succeedWhen(() -> {
			h.assertTrue(te.getNumberSlots() == 489, "chest has "+te.getNumberSlots()+" slots at "+te.power+" W");
			h.assertTrue(te.getNumPowerChanges() == 0, "power came on less than a second ago: the chest still refuses");
			if (!done[0]) {
				h.assertTrue(p.open(pos) instanceof ScaleableChestMenu m && m.page == 0, "chest opens no GUI");
				p.putInto(0, Items.COBBLESTONE, 16);
				p.player.containerMenu.clickMenuButton(p.player, 0);
				h.assertTrue(p.player.containerMenu instanceof ScaleableChestMenu m && m.page == 1, "not on page 1: "+p.player.containerMenu);
				p.putInto(0, Items.DIRT, 8);
				p.closeMenu();
				done[0] = true;
			}
			h.assertTrue(te.getInventory().getStackInSlot(0).is(Items.COBBLESTONE) && te.getInventory().getStackInSlot(54).is(Items.DIRT),
					"slot 0 "+te.getInventory().getStackInSlot(0)+", slot 54 "+te.getInventory().getStackInSlot(54));
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 4, 8));
			p.leave();
		});
	}
}
