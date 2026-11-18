/*******************************************************************************
 * Tests for the port's CCTV camera. Private NeoForge 1.21.1 port of RotaryCraft; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.BevelGearBlockEntity;
import reika.rotarycraft.content.blockentity.CctvBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.ScreenBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.content.item.CoilItem;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryItems;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class CctvTests {

	private static final String PLATFORM = "platform_9x8x9";

	private static <T extends BlockEntity> T place(GameTestHelper h, Block block, int x, int y, int z, int meta, Class<T> cls) {
		BlockPos p = new BlockPos(x, y, z);
		h.setBlock(p, block);
		BlockEntity be = h.getBlockEntity(p);
		if (be instanceof RotaryCraftBlockEntity r)
			r.setBlockMetadata(meta);
		return cls.cast(be);
	}

	/** Screwdriver aiming: 5 degrees to the side, or 5 down, wrapping from -60 to 60. */
	@GameTest(template = PLATFORM)
	public static void cctvAims(GameTestHelper h) {
		CctvBlockEntity te = place(h, RotaryBlocks.CCTV.get(), 4, 2, 4, 0, CctvBlockEntity.class);
		te.turn(false);
		te.turn(false);
		h.assertTrue(te.yaw == 10, "yaw "+te.yaw);
		for (int i = 0; i < 12; i++)
			te.turn(true);
		h.assertTrue(te.theta == -60, "pitch "+te.theta);
		te.turn(true);
		h.assertTrue(te.theta == 60, "pitch wrapped to "+te.theta);
		h.succeed();
	}

	/**
	 * A powered screen with red, green and blue dyes finds the CCTV with the same dyes and shows its view to the player - only while the
	 * CCTV has a wound spring.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 100)
	public static void screenShowsTheCctv(GameTestHelper h) {
		place(h, RotaryBlocks.GAS_ENGINE.get(), 3, 2, 4, 1, EngineBlockEntity.class).addFuel(50000);
		BevelGearBlockEntity bev = place(h, RotaryBlocks.BEVEL_GEARS.get(), 4, 2, 4, 0, BevelGearBlockEntity.class);
		bev.direction = BevelGearBlockEntity.getDirectionMap().indexOf(new BevelGearBlockEntity.IOPair(Direction.WEST, Direction.UP));
		ScreenBlockEntity screen = place(h, RotaryBlocks.SCREEN.get(), 4, 3, 4, 0, ScreenBlockEntity.class);
		screen.getInventory().setStackInSlot(0, new ItemStack(Items.PURPLE_DYE));
		screen.getInventory().setStackInSlot(1, new ItemStack(Items.CYAN_DYE));
		screen.getInventory().setStackInSlot(2, new ItemStack(Items.LIME_DYE));
		CctvBlockEntity cam = place(h, RotaryBlocks.CCTV.get(), 7, 2, 7, 0, CctvBlockEntity.class);
		cam.getDyes().setStackInSlot(0, new ItemStack(Items.PURPLE_DYE));
		cam.getDyes().setStackInSlot(1, new ItemStack(Items.CYAN_DYE));
		cam.getDyes().setStackInSlot(2, new ItemStack(Items.LIME_DYE));
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		boolean[] wound = {false};
		h.succeedWhen(() -> {
			h.assertTrue(screen.canRun() && screen.findCamera() == cam, "camera "+screen.findCamera()+" at "+screen.getPower()+" W");
			if (!wound[0]) {
				screen.activate(p.player);
				h.assertTrue(cam.views == 0 && !cam.isOn(), "view without a spring");
				cam.getInventory().setStackInSlot(0, CoilItem.withCharge(new ItemStack(RotaryItems.SPRING.get()), 50));
				wound[0] = true;
				h.fail("wound");
			}
			h.assertTrue(cam.isOn(), "not on with a spring");
			screen.activate(p.player);
			h.assertTrue(cam.views == 1, "views "+cam.views);
			p.leave();
		});
	}
}
