/*******************************************************************************
 * Practice factory zone (see Factory): the RotaryCraft item sorter built and run by a simulated survival player. Not part of the
 * original mods.
 ******************************************************************************/
package reika.reactorcraft.gametest.factory;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.rotarycraft.content.blockentity.SortingBlockEntity;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneSorting {

	public static final List<String> BLOCKS = List.of("rotarycraft:sorting");

	private static final String PLATFORM = "platform_9x8x9";

	/**
	 * Ethanol barrel -> fuel lines -> gas engine -> item sorter (input east) with a chest on top and chests north and south of it. In the
	 * sorter's GUI the player maps cobblestone to the first row (north) by clicking the ghost slot with cobblestone on the cursor, then
	 * fills the top chest with cobblestone and dirt: cobblestone goes north, dirt (unmapped) down into the chest below.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 400, batch = "factory")
	public static void sorterSplitsAChest(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8);
		Factory.infiniteChest(h, chest, new ItemStack(RotaryBlocks.FUEL_LINE.get()), new ItemStack(RotaryBlocks.GAS_ENGINE.get()),
				new ItemStack(RotaryBlocks.SORTING.get()), new ItemStack(Items.CHEST), new ItemStack(Items.COBBLESTONE), new ItemStack(Items.DIRT));
		Factory.fluidBarrel(h, new BlockPos(8, 3, 5), RotaryFluids.ETHANOL.get());
		Factory.take(p, chest, RotaryBlocks.FUEL_LINE.get().asItem(), 3);
		Factory.take(p, chest, RotaryBlocks.GAS_ENGINE.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.SORTING.get().asItem(), 1);
		Factory.takeExactly(p, chest, Items.CHEST, 3);
		Factory.takeExactly(p, chest, Items.COBBLESTONE, 9);
		Factory.takeExactly(p, chest, Items.DIRT, 4);
		for (int x = 8; x >= 6; x--)
			p.place(new BlockPos(x, 2, 5), RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		p.placeSneaking(new BlockPos(6, 3, 5), new ItemStack(RotaryBlocks.GAS_ENGINE.get()), Direction.EAST);
		BlockPos pos = new BlockPos(5, 3, 5), below = new BlockPos(5, 2, 5), north = new BlockPos(5, 3, 4), top = new BlockPos(5, 4, 5);
		p.place(below, Items.CHEST, Direction.EAST);
		p.placeSneaking(pos, new ItemStack(RotaryBlocks.SORTING.get()), Direction.EAST); //looking east: the input faces east, towards the engine
		SortingBlockEntity te = (SortingBlockEntity)h.getBlockEntity(pos);
		h.assertTrue(te.getFacing() == Direction.EAST && te.getDirection(0) == Direction.NORTH, "sorter faces "+te.getFacing()+" (meta "+te.getBlockMetadata()+")");
		p.place(new BlockPos(5, 2, 4), Items.DIRT, Direction.EAST);
		p.placeSneaking(north, new ItemStack(Items.CHEST), Direction.SOUTH);

		AbstractContainerMenu m = p.open(pos);
		int cobbleSlot = -1;
		for (int i = 0; i < m.slots.size(); i++)
			if (m.slots.get(i).getItem().is(Items.COBBLESTONE))
				cobbleSlot = i;
		p.click(cobbleSlot, 0, ClickType.PICKUP);
		h.assertTrue(m.clickMenuButton(p.player, 0), "ghost slot refused");
		p.click(cobbleSlot, 0, ClickType.PICKUP);
		p.closeMenu();
		h.assertTrue(te.getMapping(0).is(Items.COBBLESTONE) && p.count(Items.COBBLESTONE) == 9, "mapping "+te.getMapping(0));

		p.placeSneaking(top, new ItemStack(Items.CHEST), Direction.EAST);
		p.open(top);
		p.putInto(0, Items.COBBLESTONE, 9);
		p.putInto(1, Items.DIRT, 3);
		p.closeMenu();

		h.succeedWhen(() -> {
			Container n = (Container)h.getBlockEntity(north), d = (Container)h.getBlockEntity(below);
			h.assertTrue(n.countItem(Items.COBBLESTONE) == 9 && d.countItem(Items.DIRT) == 3, "north "+n.countItem(Items.COBBLESTONE)+" cobblestone, below "+d.countItem(Items.DIRT)+" dirt");
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 4, 8));
			p.leave();
		});
	}
}
