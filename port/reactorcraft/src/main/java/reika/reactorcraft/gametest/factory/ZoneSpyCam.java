/*******************************************************************************
 * Practice factory zone (see Factory): a RotaryCraft spy cam watched through a CCTV screen by a simulated survival player. Not part of
 * the original mods.
 ******************************************************************************/
package reika.reactorcraft.gametest.factory;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.rotarycraft.content.blockentity.SpyCamBlockEntity;
import reika.rotarycraft.content.item.CoilItem;
import reika.rotarycraft.content.menu.SpyCamViewMenu;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;
import reika.rotarycraft.registry.RotaryItems;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneSpyCam {

	public static final List<String> BLOCKS = List.of("rotarycraft:cctv_screen", "rotarycraft:spy_cam");

	private static final String PLATFORM = "platform_9x8x9";

	/**
	 * Ethanol barrel -> fuel lines -> gas engine -> bevel gears (east -> up) -> CCTV screen on top. The player sets down a spy cam in the
	 * far corner, puts a wound coil and red, green and blue dyes into it and the same dyes into the screen, then sneak-right-clicks the
	 * screen: the camera's map opens.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 300, batch = "factory")
	public static void screenShowsTheSpyCam(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8);
		ItemStack coil = CoilItem.withCharge(new ItemStack(RotaryItems.SPRING.get()), 500);
		Factory.infiniteChest(h, chest, new ItemStack(RotaryBlocks.FUEL_LINE.get()), new ItemStack(RotaryBlocks.GAS_ENGINE.get()),
				new ItemStack(RotaryBlocks.BEVEL_GEARS.get()), new ItemStack(RotaryBlocks.SCREEN.get()), new ItemStack(RotaryBlocks.SPY_CAM.get()),
				coil, new ItemStack(Items.DIRT), new ItemStack(Items.IRON_SHOVEL), new ItemStack(Items.RED_DYE), new ItemStack(Items.GREEN_DYE), new ItemStack(Items.BLUE_DYE));
		Factory.fluidBarrel(h, new BlockPos(8, 3, 5), RotaryFluids.ETHANOL.get());
		Factory.take(p, chest, RotaryBlocks.FUEL_LINE.get().asItem(), 3);
		Factory.take(p, chest, RotaryBlocks.GAS_ENGINE.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.BEVEL_GEARS.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.SCREEN.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.SPY_CAM.get().asItem(), 1);
		Factory.takeExactly(p, chest, RotaryItems.SPRING.get(), 1);
		Factory.takeExactly(p, chest, Items.DIRT, 2);
		Factory.takeExactly(p, chest, Items.IRON_SHOVEL, 1);
		for (var dye : new net.minecraft.world.item.Item[] {Items.RED_DYE, Items.GREEN_DYE, Items.BLUE_DYE})
			Factory.takeExactly(p, chest, dye, 2);
		for (int x = 8; x >= 6; x--)
			p.place(new BlockPos(x, 2, 5), RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		p.placeSneaking(new BlockPos(6, 3, 5), new ItemStack(RotaryBlocks.GAS_ENGINE.get()), Direction.EAST);
		BlockPos bevel = new BlockPos(5, 3, 5), screen = bevel.above(), camPos = new BlockPos(1, 2, 1);
		p.place(bevel.below(), Items.DIRT, Direction.EAST);
		p.place(bevel, Items.DIRT, Direction.EAST); //a support for the screen, dug out again
		p.placeSneaking(screen, new ItemStack(RotaryBlocks.SCREEN.get()), Direction.EAST);
		p.mine(bevel, Items.IRON_SHOVEL);
		p.placeSneaking(bevel, new ItemStack(RotaryBlocks.BEVEL_GEARS.get()), Direction.EAST); //last: finds east -> up
		p.place(camPos, RotaryBlocks.SPY_CAM.get(), Direction.EAST);
		h.assertTrue(p.open(camPos) != null, "spy cam opens no GUI");
		p.putInto(0, RotaryItems.SPRING.get(), 1);
		p.putInto(1, Items.RED_DYE, 1);
		p.putInto(2, Items.GREEN_DYE, 1);
		p.putInto(3, Items.BLUE_DYE, 1);
		p.closeMenu();
		h.assertTrue(p.open(screen) != null, "screen opens no GUI");
		p.putInto(0, Items.RED_DYE, 1);
		p.putInto(1, Items.GREEN_DYE, 1);
		p.putInto(2, Items.BLUE_DYE, 1);
		p.closeMenu();
		SpyCamBlockEntity cam = (SpyCamBlockEntity)h.getBlockEntity(camPos);

		h.succeedWhen(() -> {
			h.assertTrue(cam.isOn(), "camera off");
			p.player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			p.player.setShiftKeyDown(true);
			try {
				p.use(screen);
			}
			finally {
				p.player.setShiftKeyDown(false);
			}
			h.assertTrue(p.player.containerMenu instanceof SpyCamViewMenu m && m.tile == cam, "no camera view: "+p.player.containerMenu+"; screen "+((reika.rotarycraft.content.blockentity.ScreenBlockEntity)h.getBlockEntity(screen)).getPower()+" W, runs "+((reika.rotarycraft.content.blockentity.ScreenBlockEntity)h.getBlockEntity(screen)).canRun()+", camera "+((reika.rotarycraft.content.blockentity.ScreenBlockEntity)h.getBlockEntity(screen)).findCamera());
			p.closeMenu();
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 4, 8));
			p.leave();
		});
	}
}
