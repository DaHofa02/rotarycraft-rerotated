/*******************************************************************************
 * Practice factory zone (see Factory): the RotaryCraft item cannon built, aimed and filled by a simulated survival player. Not part of the
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
import reika.rotarycraft.content.blockentity.ItemCannonBlockEntity;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;
import reika.rotarycraft.network.RotaryNetwork;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneItemCannon {

	public static final List<String> BLOCKS = List.of("rotarycraft:item_cannon");

	private static final String PLATFORM = "platform_9x8x9";

	/**
	 * Ethanol barrel -> fuel lines -> gas engine -> bevel gears (east -> up) -> item cannon on top; a second cannon on the floor across the
	 * zone. The player types the second cannon's position into the first one's GUI and puts cobblestone in: it arrives over there.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 300, batch = "factory")
	public static void itemCannonDeliversCobblestone(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8);
		Factory.infiniteChest(h, chest, new ItemStack(RotaryBlocks.FUEL_LINE.get()), new ItemStack(RotaryBlocks.GAS_ENGINE.get()),
				new ItemStack(RotaryBlocks.BEVEL_GEARS.get()), new ItemStack(RotaryBlocks.ITEM_CANNON.get()), new ItemStack(Items.DIRT),
				new ItemStack(Items.IRON_SHOVEL), new ItemStack(Items.COBBLESTONE));
		Factory.fluidBarrel(h, new BlockPos(8, 3, 5), RotaryFluids.ETHANOL.get());
		Factory.take(p, chest, RotaryBlocks.FUEL_LINE.get().asItem(), 3);
		Factory.take(p, chest, RotaryBlocks.GAS_ENGINE.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.BEVEL_GEARS.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.ITEM_CANNON.get().asItem(), 2);
		Factory.takeExactly(p, chest, Items.DIRT, 2);
		Factory.takeExactly(p, chest, Items.IRON_SHOVEL, 1);
		Factory.takeExactly(p, chest, Items.COBBLESTONE, 3);
		for (int x = 8; x >= 6; x--)
			p.place(new BlockPos(x, 2, 5), RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		p.placeSneaking(new BlockPos(6, 3, 5), new ItemStack(RotaryBlocks.GAS_ENGINE.get()), Direction.EAST);
		BlockPos bevel = new BlockPos(5, 3, 5), pos = bevel.above(), far = new BlockPos(1, 2, 1);
		p.place(bevel.below(), Items.DIRT, Direction.EAST);
		p.place(bevel, Items.DIRT, Direction.EAST); //a support, dug out again
		p.place(pos, RotaryBlocks.ITEM_CANNON.get(), Direction.EAST);
		p.mine(bevel, Items.IRON_SHOVEL);
		p.placeSneaking(bevel, new ItemStack(RotaryBlocks.BEVEL_GEARS.get()), Direction.EAST); //last: finds east -> up
		p.place(far, RotaryBlocks.ITEM_CANNON.get(), Direction.EAST);
		h.assertTrue(p.open(pos) != null, "item cannon opens no GUI");
		BlockPos t = h.absolutePos(far);
		RotaryNetwork.applyCannonTarget(p.player, new RotaryNetwork.CannonTarget(h.absolutePos(pos), 0, t)); //what the GUI's fields send
		p.putInto(0, Items.COBBLESTONE, 3);
		p.closeMenu();
		ItemCannonBlockEntity te = (ItemCannonBlockEntity)h.getBlockEntity(pos);
		ItemCannonBlockEntity there = (ItemCannonBlockEntity)h.getBlockEntity(far);

		h.succeedWhen(() -> {
			h.assertTrue(t.equals(te.getTarget()) && te.getTargetDimensionId() == 0, "target "+te.getTarget());
			h.assertTrue(there.getInventory().getStackInSlot(0).is(Items.COBBLESTONE) && there.getInventory().getStackInSlot(0).getCount() == 3,
					"arrived "+there.getInventory().getStackInSlot(0)+" at "+te.getPower()+" W");
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 4, 8));
			p.leave();
		});
	}
}
