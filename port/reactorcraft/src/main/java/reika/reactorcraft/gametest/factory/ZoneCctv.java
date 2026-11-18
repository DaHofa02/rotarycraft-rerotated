/*******************************************************************************
 * Practice factory zone (see Factory): the port's CCTV camera set up and watched by a simulated survival player. Not part of the original mods.
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
import reika.rotarycraft.content.blockentity.CctvBlockEntity;
import reika.rotarycraft.content.item.CoilItem;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;
import reika.rotarycraft.registry.RotaryItems;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneCctv {

	public static final List<String> BLOCKS = List.of("rotarycraft:cctv");

	private static final String PLATFORM = "platform_9x8x9";

	/**
	 * Ethanol barrel -> fuel lines -> gas engine -> bevel gears (east -> up) -> CCTV screen with three dyes. In a corner the player sets up
	 * a CCTV with the same dyes and a wound spring, aims it with the screwdriver and sneak-clicks the screen: the camera's view is called up.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 300, batch = "factory")
	public static void cctvWatchesTheCorner(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8);
		ItemStack spring = CoilItem.withCharge(new ItemStack(RotaryItems.SPRING.get()), 200);
		Factory.infiniteChest(h, chest, new ItemStack(RotaryBlocks.FUEL_LINE.get()), new ItemStack(RotaryBlocks.GAS_ENGINE.get()),
				new ItemStack(RotaryBlocks.BEVEL_GEARS.get()), new ItemStack(RotaryBlocks.SCREEN.get()), new ItemStack(RotaryBlocks.CCTV.get()),
				new ItemStack(Items.DIRT), new ItemStack(Items.IRON_SHOVEL), new ItemStack(Items.RED_DYE), new ItemStack(Items.GREEN_DYE),
				new ItemStack(Items.BLUE_DYE), spring, new ItemStack(RotaryItems.SCREWDRIVER.get()));
		Factory.fluidBarrel(h, new BlockPos(8, 3, 5), RotaryFluids.ETHANOL.get());
		Factory.take(p, chest, RotaryBlocks.FUEL_LINE.get().asItem(), 3);
		Factory.take(p, chest, RotaryBlocks.GAS_ENGINE.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.BEVEL_GEARS.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.SCREEN.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.CCTV.get().asItem(), 1);
		Factory.takeExactly(p, chest, Items.DIRT, 2);
		Factory.takeExactly(p, chest, Items.IRON_SHOVEL, 1);
		for (net.minecraft.world.item.Item d : new net.minecraft.world.item.Item[] {Items.RED_DYE, Items.GREEN_DYE, Items.BLUE_DYE})
			Factory.takeExactly(p, chest, d, 2);
		Factory.take(p, chest, spring, 1);
		Factory.take(p, chest, RotaryItems.SCREWDRIVER.get(), 1);
		for (int x = 8; x >= 6; x--)
			p.place(new BlockPos(x, 2, 5), RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		p.placeSneaking(new BlockPos(6, 3, 5), new ItemStack(RotaryBlocks.GAS_ENGINE.get()), Direction.EAST);
		BlockPos bevel = new BlockPos(5, 3, 5), screen = bevel.above(), cam = new BlockPos(1, 2, 1);
		p.place(bevel.below(), Items.DIRT, Direction.EAST);
		p.place(bevel, Items.DIRT, Direction.EAST); //a support, dug out again
		p.place(screen, RotaryBlocks.SCREEN.get(), Direction.EAST);
		p.mine(bevel, Items.IRON_SHOVEL);
		p.placeSneaking(bevel, new ItemStack(RotaryBlocks.BEVEL_GEARS.get()), Direction.EAST); //last: finds east -> up
		h.assertTrue(p.open(screen) != null, "screen opens no GUI");
		p.putInto(0, Items.RED_DYE, 1);
		p.putInto(1, Items.GREEN_DYE, 1);
		p.putInto(2, Items.BLUE_DYE, 1);
		p.closeMenu();
		p.place(cam, RotaryBlocks.CCTV.get(), Direction.EAST);
		h.assertTrue(p.open(cam) != null, "CCTV opens no GUI");
		p.putInto(0, RotaryItems.SPRING.get(), 1);
		p.putInto(1, Items.RED_DYE, 1);
		p.putInto(2, Items.GREEN_DYE, 1);
		p.putInto(3, Items.BLUE_DYE, 1);
		p.closeMenu();
		CctvBlockEntity te = (CctvBlockEntity)h.getBlockEntity(cam);
		p.use(cam, RotaryItems.SCREWDRIVER.get());
		h.assertTrue(te.yaw == 5, "screwdriver did not turn the CCTV: "+te.yaw);

		h.succeedWhen(() -> {
			h.assertTrue(te.isOn(), "CCTV not running");
			//retried each tick: the screen only calls up the camera once its engine has spun up
			p.player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			p.player.setShiftKeyDown(true);
			try {
				p.use(screen);
			}
			finally {
				p.player.setShiftKeyDown(false);
			}
			var sc = (reika.rotarycraft.content.blockentity.ScreenBlockEntity)h.getBlockEntity(screen);
			h.assertTrue(te.views >= 1, "view not called up: screen "+sc.getPower()+" W, runs "+sc.canRun()+", camera "+sc.findCamera());
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 4, 8));
			p.leave();
		});
	}
}
