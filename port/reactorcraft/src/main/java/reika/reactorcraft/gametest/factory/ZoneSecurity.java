/*******************************************************************************
 * Practice factory zone (see Factory): the RotaryCraft Player Detector built and run by a simulated survival player. Not part of the
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
import net.minecraft.world.level.block.RedstoneLampBlock;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.rotarycraft.content.blockentity.PlayerDetectorBlockEntity;
import reika.rotarycraft.content.menu.PlayerDetectorMenu;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneSecurity {

	public static final List<String> BLOCKS = List.of("rotarycraft:player_detector");

	private static final String PLATFORM = "platform_9x8x9";

	/** The player opens the detector's GUI and types the range in (the GUI sends it as a menu button click). */
	private static void setRange(GameTestHelper h, SimPlayer p, BlockPos det, int range) {
		h.assertTrue(p.open(det) instanceof PlayerDetectorMenu, "detector opens no range GUI");
		h.assertTrue(p.menu().clickMenuButton(p.player, range), "GUI does not take the range "+range);
		p.closeMenu();
	}

	/**
	 * DC engine on a redstone block (256 rad/s @ 4 Nm = 1024 W) -> bevel gears (east -> up) -> player detector, a redstone lamp beside
	 * it. The power allows a range of 8 and gives a reaction time of 92 ticks. With the range set to 1 the player, standing 2.5 blocks
	 * away, is not seen; with 3 the lamp lights after the reaction time; when the player walks away it goes out.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 800, batch = "factory")
	public static void detectorSeesItsBuilder(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8);
		Factory.infiniteChest(h, chest, new ItemStack(Items.REDSTONE_BLOCK), new ItemStack(RotaryBlocks.DC_ENGINE.get()),
				new ItemStack(RotaryBlocks.BEVEL_GEARS.get()), new ItemStack(RotaryBlocks.PLAYER_DETECTOR.get()), new ItemStack(Items.REDSTONE_LAMP),
				new ItemStack(Items.DIRT), new ItemStack(Items.IRON_SHOVEL));

		Factory.take(p, chest, Items.REDSTONE_BLOCK, 1);
		Factory.take(p, chest, RotaryBlocks.DC_ENGINE.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.BEVEL_GEARS.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.PLAYER_DETECTOR.get().asItem(), 1);
		Factory.take(p, chest, Items.REDSTONE_LAMP, 1);
		Factory.takeExactly(p, chest, Items.DIRT, 4);
		Factory.takeExactly(p, chest, Items.IRON_SHOVEL, 1);
		p.place(new BlockPos(6, 2, 4), Items.REDSTONE_BLOCK, Direction.EAST);
		p.place(new BlockPos(6, 3, 4), RotaryBlocks.DC_ENGINE.get(), Direction.EAST);
		// the detector on a dirt pillar, whose top block is dug away again and replaced by the bevel gears (placed last: east -> up)
		BlockPos det = new BlockPos(5, 4, 4);
		p.place(new BlockPos(5, 2, 4), Items.DIRT, Direction.EAST);
		p.place(det.below(), Items.DIRT, Direction.EAST);
		p.place(det, RotaryBlocks.PLAYER_DETECTOR.get(), Direction.EAST);
		p.mine(det.below(), Items.IRON_SHOVEL);
		p.placeSneaking(det.below(), new ItemStack(RotaryBlocks.BEVEL_GEARS.get()), Direction.EAST);
		// a lamp west of it, on its own dirt pillar
		BlockPos lamp = new BlockPos(4, 4, 4);
		p.place(new BlockPos(4, 2, 4), Items.DIRT, Direction.EAST);
		p.place(new BlockPos(4, 3, 4), Items.DIRT, Direction.EAST);
		p.place(lamp, Items.REDSTONE_LAMP, Direction.EAST);

		PlayerDetectorBlockEntity te = (PlayerDetectorBlockEntity)h.getBlockEntity(det);
		p.join(); //from here on the player is in the world for the detector to find
		int[] stage = {0};
		long[] since = {0};
		h.succeedWhen(() -> {
			boolean lit = h.getBlockState(lamp).getValue(RedstoneLampBlock.LIT);
			h.assertTrue(te.omega == 256 && te.torque == 4, "detector at "+te.omega+" rad/s @ "+te.torque+" Nm");
			h.assertTrue(te.getMaxRange() == 8 && te.getReactionTime() == 92, "max range "+te.getMaxRange()+", reaction time "+te.getReactionTime());
			if (stage[0] == 0) {
				setRange(h, p, det, 1);
				h.assertTrue(te.getRange() == 1, "range "+te.getRange());
				since[0] = h.getTick();
				stage[0] = 1;
			}
			if (stage[0] == 1) {
				h.assertTrue(!te.isActive() && !lit, "detector sees the player outside its range");
				h.assertTrue(h.getTick()-since[0] >= 120, "waiting to see that the detector stays off");
				setRange(h, p, det, 3);
				h.assertTrue(te.getRange() == 3, "range "+te.getRange());
				since[0] = h.getTick();
				stage[0] = 2;
			}
			if (stage[0] == 2) {
				if (h.getTick()-since[0] < 90)
					h.assertTrue(!te.isActive() && !lit, "detector reacts after "+(h.getTick()-since[0])+" ticks");
				h.assertTrue(te.isActive() && lit, "detector does not see the player yet");
				p.moveNear(new BlockPos(0, 2, -2)); //walks to the far edge of the platform
				stage[0] = 3;
				h.fail("player walked away; checking that the detector stops");
			}
			h.assertTrue(!te.isActive() && !lit, "detector stays on: "+te.isActive()+", lamp "+lit);
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 5, 8));
			p.leave();
		});
	}
}
