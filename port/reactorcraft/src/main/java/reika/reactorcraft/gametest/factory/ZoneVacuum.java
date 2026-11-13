/*******************************************************************************
 * Practice factory zone (see Factory): the RotaryCraft item vacuum built and run by a simulated survival player. Not part of the
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
import reika.rotarycraft.content.blockentity.VacuumBlockEntity;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneVacuum {

	public static final List<String> BLOCKS = List.of("rotarycraft:vacuum");

	private static final String PLATFORM = "platform_9x8x9";

	/**
	 * Ethanol barrel -> fuel lines -> gas engine -> item vacuum. The player throws a stack of cobblestone across the zone: the vacuum
	 * sucks it in, and the player takes it back out through the GUI. (In a batch of its own: its 24-block reach would take the items of
	 * neighbouring zones.)
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 400, batch = "vacuum")
	public static void vacuumCatchesThrownItems(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8);
		Factory.infiniteChest(h, chest, new ItemStack(RotaryBlocks.FUEL_LINE.get()), new ItemStack(RotaryBlocks.GAS_ENGINE.get()),
				new ItemStack(RotaryBlocks.VACUUM.get()), new ItemStack(Items.DIRT), new ItemStack(Items.COBBLESTONE));
		Factory.fluidBarrel(h, new BlockPos(8, 3, 5), RotaryFluids.ETHANOL.get());
		Factory.take(p, chest, RotaryBlocks.FUEL_LINE.get().asItem(), 3);
		Factory.take(p, chest, RotaryBlocks.GAS_ENGINE.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.VACUUM.get().asItem(), 1);
		Factory.takeExactly(p, chest, Items.DIRT, 1);
		Factory.takeExactly(p, chest, Items.COBBLESTONE, 32);
		for (int x = 8; x >= 6; x--)
			p.place(new BlockPos(x, 2, 5), RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		p.placeSneaking(new BlockPos(6, 3, 5), new ItemStack(RotaryBlocks.GAS_ENGINE.get()), Direction.EAST);
		BlockPos pos = new BlockPos(5, 3, 5);
		p.place(pos.below(), Items.DIRT, Direction.EAST);
		p.place(pos, RotaryBlocks.VACUUM.get(), Direction.EAST);
		VacuumBlockEntity te = (VacuumBlockEntity)h.getBlockEntity(pos);
		p.hold(Items.COBBLESTONE);
		h.assertTrue(p.player.drop(p.player.getMainHandItem().copy(), true) != null, "nothing thrown");
		p.player.getMainHandItem().setCount(0);
		boolean[] done = {false};

		h.succeedWhen(() -> {
			int cob = 0;
			for (int i = 0; i < 54; i++)
				cob += te.getInventory().getStackInSlot(i).is(Items.COBBLESTONE) ? te.getInventory().getStackInSlot(i).getCount() : 0;
			h.assertTrue(done[0] || cob == 32, "vacuum holds "+cob+" cobblestone at "+te.power+" W");
			if (!done[0]) {
				p.open(pos);
				for (int i = 0; i < 54; i++)
					p.shiftClick(i);
				p.closeMenu();
				done[0] = true;
			}
			h.assertTrue(p.count(Items.COBBLESTONE) == 32, "player has "+p.count(Items.COBBLESTONE));
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 4, 8));
			p.leave();
		});
	}
}
