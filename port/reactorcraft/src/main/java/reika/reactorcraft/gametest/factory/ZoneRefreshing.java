/*******************************************************************************
 * Practice factory zone (see Factory): the RotaryCraft item refresher built and run by a simulated survival player. Not part of the
 * original mods.
 ******************************************************************************/
package reika.reactorcraft.gametest.factory;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.rotarycraft.content.blockentity.ItemRefresherBlockEntity;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneRefreshing {

	public static final List<String> BLOCKS = List.of("rotarycraft:item_refresher");

	private static final String PLATFORM = "platform_9x8x9";

	/**
	 * Ethanol barrel -> fuel lines -> gas engine -> item refresher west of it. The player throws a diamond on the ground (its lifespan cut
	 * to 100 ticks for the test): the refresher keeps it from despawning.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 400, batch = "factory")
	public static void refresherKeepsAThrownItem(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8);
		Factory.infiniteChest(h, chest, new ItemStack(RotaryBlocks.FUEL_LINE.get()), new ItemStack(RotaryBlocks.GAS_ENGINE.get()),
				new ItemStack(RotaryBlocks.ITEM_REFRESHER.get()), new ItemStack(Items.DIRT), new ItemStack(Items.DIAMOND));
		Factory.fluidBarrel(h, new BlockPos(8, 3, 5), RotaryFluids.ETHANOL.get());
		Factory.take(p, chest, RotaryBlocks.FUEL_LINE.get().asItem(), 3);
		Factory.take(p, chest, RotaryBlocks.GAS_ENGINE.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.ITEM_REFRESHER.get().asItem(), 1);
		Factory.takeExactly(p, chest, Items.DIRT, 1);
		Factory.takeExactly(p, chest, Items.DIAMOND, 1);
		for (int x = 8; x >= 6; x--)
			p.place(new BlockPos(x, 2, 5), RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		p.placeSneaking(new BlockPos(6, 3, 5), new ItemStack(RotaryBlocks.GAS_ENGINE.get()), Direction.EAST);
		BlockPos pos = new BlockPos(5, 3, 5);
		p.place(pos.below(), Items.DIRT, Direction.EAST);
		p.place(pos, RotaryBlocks.ITEM_REFRESHER.get(), Direction.EAST);
		ItemRefresherBlockEntity te = (ItemRefresherBlockEntity)h.getBlockEntity(pos);
		p.hold(Items.DIAMOND);
		ItemEntity thrown = p.player.drop(p.player.getMainHandItem().copy(), false);
		p.player.getMainHandItem().setCount(0);
		h.assertTrue(thrown != null, "nothing thrown");
		thrown.lifespan = 100;
		thrown.setPickUpDelay(32767);
		long t0 = h.getTick();

		h.succeedWhen(() -> {
			h.assertTrue(te.power == 65536, "refresher at "+te.power+" W");
			h.assertTrue(h.getTick()-t0 > 200, "waiting");
			h.assertTrue(thrown.isAlive() && thrown.lifespan > 100, "diamond despawned (lifespan "+thrown.lifespan+")");
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 4, 8));
			p.leave();
		});
	}
}
