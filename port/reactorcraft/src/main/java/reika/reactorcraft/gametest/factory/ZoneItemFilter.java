/*******************************************************************************
 * Practice factory zone (see Factory): the port's item filter built and set by a simulated survival player. Not part of the original mods.
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
import net.minecraft.world.level.block.entity.HopperBlockEntity;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.rotarycraft.content.blockentity.ItemFilterBlockEntity;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneItemFilter {

	public static final List<String> BLOCKS = List.of("rotarycraft:item_filter");

	private static final String PLATFORM = "platform_9x8x9";

	/**
	 * Ethanol barrel -> fuel lines -> gas engine, the item filter west of it with a hopper on top. The player puts an iron ingot in as the
	 * template through the GUI and fills the hopper with gold and iron: only the iron gets into the buffer.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 300, batch = "factory")
	public static void itemFilterSortsIngots(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8);
		Factory.infiniteChest(h, chest, new ItemStack(RotaryBlocks.FUEL_LINE.get()), new ItemStack(RotaryBlocks.GAS_ENGINE.get()),
				new ItemStack(RotaryBlocks.ITEM_FILTER.get()), new ItemStack(Items.HOPPER), new ItemStack(Items.DIRT), new ItemStack(Items.IRON_INGOT),
				new ItemStack(Items.GOLD_INGOT));
		Factory.fluidBarrel(h, new BlockPos(8, 3, 5), RotaryFluids.ETHANOL.get());
		Factory.take(p, chest, RotaryBlocks.FUEL_LINE.get().asItem(), 3);
		Factory.take(p, chest, RotaryBlocks.GAS_ENGINE.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.ITEM_FILTER.get().asItem(), 1);
		Factory.takeExactly(p, chest, Items.HOPPER, 1);
		Factory.takeExactly(p, chest, Items.DIRT, 1);
		Factory.takeExactly(p, chest, Items.IRON_INGOT, 5);
		Factory.takeExactly(p, chest, Items.GOLD_INGOT, 4);
		for (int x = 8; x >= 6; x--)
			p.place(new BlockPos(x, 2, 5), RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		p.placeSneaking(new BlockPos(6, 3, 5), new ItemStack(RotaryBlocks.GAS_ENGINE.get()), Direction.EAST); //looking east: drives west
		BlockPos pos = new BlockPos(5, 3, 5), hop = pos.above();
		p.place(pos.below(), Items.DIRT, Direction.EAST);
		p.place(pos, RotaryBlocks.ITEM_FILTER.get(), Direction.EAST);
		h.assertTrue(p.open(pos) != null, "item filter opens no GUI");
		p.putInto(0, Items.IRON_INGOT, 1);
		p.closeMenu();
		p.placeSneaking(hop, new ItemStack(Items.HOPPER), Direction.EAST); //sneaking: a plain click opens the filter's GUI
		h.assertTrue(p.open(hop) != null, "hopper opens no GUI");
		p.putInto(0, Items.GOLD_INGOT, 4);
		p.putInto(1, Items.IRON_INGOT, 4);
		p.closeMenu();
		ItemFilterBlockEntity te = (ItemFilterBlockEntity)h.getBlockEntity(pos);
		HopperBlockEntity hb = (HopperBlockEntity)h.getBlockEntity(hop);

		h.succeedWhen(() -> {
			h.assertTrue(te.getInventory().getStackInSlot(1).is(Items.IRON_INGOT) && te.getInventory().getStackInSlot(1).getCount() == 4,
					"buffer "+te.getInventory().getStackInSlot(1)+" at "+te.getPower()+" W");
			h.assertTrue(hb.getItem(0).is(Items.GOLD_INGOT) && hb.getItem(0).getCount() == 4, "gold went through");
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 4, 8));
			p.leave();
		});
	}
}
