/*******************************************************************************
 * Tests for the ported RotaryCraft player detector (original TileEntityPlayerDetector). Private NeoForge 1.21.1 port of RotaryCraft by
 * Reika Kalseki; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.BevelGearBlockEntity;
import reika.rotarycraft.content.blockentity.PlayerDetectorBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.menu.PlayerDetectorMenu;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class PlayerDetectorTests {

	private static final String PLATFORM = "platform_9x8x9";
	private static final BlockPos DET = new BlockPos(4, 3, 4);
	private static final BlockPos LAMP = new BlockPos(5, 3, 4);

	private static <T extends BlockEntity> T place(GameTestHelper h, Block block, int x, int y, int z, int meta, Class<T> cls) {
		BlockPos p = new BlockPos(x, y, z);
		h.setBlock(p, block);
		BlockEntity be = h.getBlockEntity(p);
		if (be instanceof RotaryCraftBlockEntity r)
			r.setBlockMetadata(meta);
		return cls.cast(be);
	}

	/**
	 * DC engine (256 rad/s @ 4 Nm = 1024 W, meta 1: output east) -> bevel gears (west -> up) -> detector at (4, 3, 4), a redstone lamp
	 * beside it.
	 */
	private static PlayerDetectorBlockEntity rig(GameTestHelper h) {
		place(h, RotaryBlocks.DC_ENGINE.get(), 3, 2, 4, 1, RotaryCraftBlockEntity.class);
		h.setBlock(new BlockPos(3, 2, 3), Blocks.REDSTONE_BLOCK);
		BevelGearBlockEntity bev = place(h, RotaryBlocks.BEVEL_GEARS.get(), 4, 2, 4, 0, BevelGearBlockEntity.class);
		bev.direction = BevelGearBlockEntity.getDirectionMap().indexOf(new BevelGearBlockEntity.IOPair(Direction.WEST, Direction.UP));
		h.setBlock(LAMP, Blocks.REDSTONE_LAMP);
		return place(h, RotaryBlocks.PLAYER_DETECTOR.get(), DET.getX(), DET.getY(), DET.getZ(), 0, PlayerDetectorBlockEntity.class);
	}

	private static boolean lit(GameTestHelper h) {
		return h.getBlockState(LAMP).getValue(RedstoneLampBlock.LIT);
	}

	/**
	 * 1024 W: range 1024/128 = 8 at most; 256 rad/s: reaction time 100 - 256/32 = 92 ticks. A player standing 2.5 blocks away is seen
	 * with the range set to 3 - after 92 ticks, not before - and the detector gives redstone power 15 (lamp, comparator); when the
	 * player is gone it stops at once.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 300)
	public static void detectorReactsAfterItsReactionTime(GameTestHelper h) {
		PlayerDetectorBlockEntity te = rig(h);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		h.runAtTickTime(20, () -> {
			h.assertTrue(te.omega == 256 && te.torque == 4, "detector at "+te.omega+" rad/s @ "+te.torque+" Nm");
			h.assertTrue(te.getMaxRange() == 8 && te.getReactionTime() == 92, "max range "+te.getMaxRange()+", reaction time "+te.getReactionTime());
			h.assertTrue(te.getRange() == 0, "range "+te.getRange()+" before anything is set");
			h.assertTrue(new PlayerDetectorMenu(0, p.player.getInventory(), te).clickMenuButton(p.player, 3), "GUI does not take the range");
			h.assertTrue(te.selectedrange == 3 && te.getRange() == 3, "range "+te.getRange());
			p.moveNear(DET);
			p.join();
		});
		h.runAtTickTime(100, () -> h.assertTrue(!te.isActive() && !lit(h), "detector reacts after 80 ticks"));
		h.runAtTickTime(120, () -> {
			h.assertTrue(te.isActive() && lit(h), "detector does not react: active "+te.isActive()+", lamp "+lit(h));
			h.assertTrue(h.getLevel().getSignal(h.absolutePos(DET), Direction.WEST) == 15 && te.getRedstoneOverride() == 15, "no redstone signal");
			p.leave();
		});
		h.runAtTickTime(125, () -> {
			h.assertTrue(!te.isActive() && !lit(h), "detector stays on without a player");
			h.succeed();
		});
	}

	/**
	 * The range is the smaller of the one typed in and power/128: 999 typed in gives 8 with a DC engine. With the range at 1 the same
	 * player (2.5 blocks away) is not seen. Without power the range is 0.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 300)
	public static void detectorRangeFollowsPowerAndSetting(GameTestHelper h) {
		PlayerDetectorBlockEntity te = rig(h);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		PlayerDetectorMenu menu = new PlayerDetectorMenu(0, p.player.getInventory(), te);
		h.runAtTickTime(20, () -> {
			h.assertFalse(menu.clickMenuButton(p.player, 1000) || menu.clickMenuButton(p.player, -1), "GUI takes a range out of bounds");
			h.assertTrue(menu.clickMenuButton(p.player, 999) && te.selectedrange == 999 && te.getRange() == 8, "range "+te.getRange()+" of "+te.selectedrange);
			menu.clickMenuButton(p.player, 1);
			p.moveNear(DET);
			p.join();
		});
		h.runAtTickTime(140, () -> {
			h.assertTrue(te.getRange() == 1 && !te.isActive() && !lit(h), "detector sees a player outside its range");
			h.setBlock(DET.below(), Blocks.AIR); //the bevel gears gone: no power
			menu.clickMenuButton(p.player, 3);
		});
		h.runAtTickTime(160, () -> {
			h.assertTrue(te.power == 0 && te.getMaxRange() == 0 && te.getRange() == 0, "detector still at "+te.power+" W");
			h.assertTrue(!te.isActive() && !lit(h), "detector works without power");
			p.leave();
			h.succeed();
		});
	}
}
